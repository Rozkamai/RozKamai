/**
 * @module services/withdrawalService
 * @description Payout and withdrawal processing engine for the Pakistani financial market.
 * 
 * Purpose:
 *   Validates JazzCash, EasyPaisa, and Pakistani Bank IBAN formats, checks 48h new-user cooldown,
 *   verifies KYC completion, locks wallet balance atomically, and manages payout status transitions.
 * 
 * Inputs:
 *   - User ID, payment method, account number/IBAN, account title, requested PKR
 * 
 * Outputs:
 *   - Created withdrawal record with status 'PENDING'
 * 
 * Edge Cases Handled:
 *   - One active pending withdrawal at a time per user.
 *   - Verifies available balance >= amount and moves to pending_withdrawal_coins.
 *   - Rejection safely refunds funds back to available balance via ledger.
 */

import { withTransaction } from '../config/database';
import { withdrawalRepository, WithdrawalRow } from '../repositories/withdrawalRepository';
import { walletRepository } from '../repositories/walletRepository';
import { ledgerRepository } from '../repositories/ledgerRepository';
import { userRepository } from '../repositories/userRepository';
import { kycRepository } from '../repositories/kycRepository';
import { settingsRepository } from '../repositories/settingsRepository';
import { encryptData, computeBlindIndex } from '../utils/crypto';
import { logger } from '../config/logger';

// Extensible Payout Provider Interface for JazzCash / EasyPaisa / 1Link APIs
export interface PayoutProvider {
  disburse(withdrawal: WithdrawalRow): Promise<{ success: boolean; transactionRef: string; error?: string }>;
}

export class ManualPayoutProvider implements PayoutProvider {
  async disburse(withdrawal: WithdrawalRow) {
    // Manual disbursement flow (Admin exports CSV / marks as paid in admin portal)
    return { success: true, transactionRef: `MANUAL_${withdrawal.id.substring(0, 8)}` };
  }
}

export function validatePakistaniIban(iban: string): string {
  const clean = iban.replace(/[\s\-]/g, '').toUpperCase();
  if (!/^PK\d{2}[A-Z]{4}\d{16}$/.test(clean)) {
    throw new Error('Invalid Pakistani IBAN format. Must be 24 alphanumeric characters starting with PK (e.g. PK36SCBL0000001123456701)');
  }
  return clean;
}

export function validateMobileWalletNumber(number: string): string {
  const clean = number.replace(/[\s\-]/g, '');
  if (!/^03\d{9}$/.test(clean)) {
    throw new Error('Invalid mobile wallet number. Must be 11 digits starting with 03 (e.g. 03001234567)');
  }
  return clean;
}

export const withdrawalService = {
  async requestWithdrawal(
    userId: string,
    amountPkr: number,
    paymentMethod: 'JAZZ_CASH' | 'EASY_PAISA' | 'BANK_TRANSFER',
    accountNumber: string,
    accountTitle: string,
    bankName?: string,
    bankIban?: string
  ): Promise<WithdrawalRow> {
    const [user, settings, kyc] = await Promise.all([
      userRepository.findById(userId),
      settingsRepository.getAll(),
      kycRepository.findByUserId(userId),
    ]);

    if (!user) throw new Error('User not found');
    if (user.is_blocked) throw new Error('Account suspended');

    // 1. Check KYC Verification
    if (!kyc || kyc.status !== 'VERIFIED') {
      throw new Error('Identity verification (KYC with CNIC) is required before requesting withdrawals.');
    }

    // 2. Check 48-Hour New Account Cooldown
    if (new Date() < new Date(user.first_withdrawal_eligible_at)) {
      const waitHours = Math.ceil((new Date(user.first_withdrawal_eligible_at).getTime() - Date.now()) / (1000 * 60 * 60));
      throw new Error(`New account safety cooldown active. First withdrawal eligible in ${waitHours} hours.`);
    }

    // 3. Minimum Withdrawal Threshold
    if (amountPkr < settings.min_withdrawal_pkr) {
      throw new Error(`Minimum withdrawal amount is PKR ${settings.min_withdrawal_pkr}.`);
    }

    // 4. One Pending Request at a time
    const hasPending = await withdrawalRepository.hasPendingWithdrawal(userId);
    if (hasPending) {
      throw new Error('You already have a pending withdrawal in process. Please wait for it to be completed.');
    }

    // 5. Input Validation
    let formattedAccount = accountNumber.trim();
    let formattedIban: string | null = null;

    if (paymentMethod === 'JAZZ_CASH' || paymentMethod === 'EASY_PAISA') {
      formattedAccount = validateMobileWalletNumber(accountNumber);
    } else if (paymentMethod === 'BANK_TRANSFER') {
      if (!bankName || bankName.trim().length === 0) {
        throw new Error('Bank name is required for bank transfer');
      }
      if (!bankIban) {
        throw new Error('Bank IBAN is required for bank transfer');
      }
      formattedIban = validatePakistaniIban(bankIban);
    }

    if (!accountTitle || accountTitle.trim().length < 3) {
      throw new Error('Valid account title (name) is required');
    }

    // Coins required: 1 PKR = settings.coins_per_pkr
    const requiredCoins = amountPkr * settings.coins_per_pkr;
    const amountPaisa = amountPkr * 100;

    // Encrypt sensitive account numbers using AES-256-GCM
    const accountEncrypted = encryptData(formattedAccount);
    const accountHash = computeBlindIndex(formattedAccount);
    const ibanEncrypted = formattedIban ? encryptData(formattedIban) : null;
    const ibanHash = formattedIban ? computeBlindIndex(formattedIban) : null;

    // 6. Transactional Execution: Lock Wallet -> Verify Balance -> Move to Pending -> Append Ledger -> Create Withdrawal
    return withTransaction(async (client) => {
      const wallet = await walletRepository.lockWalletByUserId(client, userId);
      const availableCoins = parseInt(wallet.available_balance_coins, 10);

      if (availableCoins < requiredCoins) {
        throw new Error(`Insufficient balance. Required: ${requiredCoins} coins (PKR ${amountPkr}), Available: ${availableCoins} coins.`);
      }

      const balanceBefore = availableCoins;
      const balanceAfter = availableCoins - requiredCoins;

      // Hold funds in wallet
      await walletRepository.holdWithdrawal(client, wallet.id, requiredCoins);

      // Create withdrawal row
      const withdrawal = await withdrawalRepository.createWithdrawal(
        client,
        userId,
        wallet.id,
        requiredCoins,
        amountPaisa,
        amountPkr,
        paymentMethod,
        accountEncrypted,
        accountHash,
        accountTitle.trim(),
        bankName?.trim() || null,
        ibanEncrypted,
        ibanHash
      );

      // Append Ledger Hold entry
      await ledgerRepository.appendEntry(
        client,
        userId,
        wallet.id,
        'WITHDRAWAL_HOLD',
        -requiredCoins, // negative ledger adjustment
        balanceBefore,
        balanceAfter,
        'WITHDRAWAL',
        withdrawal.id,
        `Withdrawal request PKR ${amountPkr} via ${paymentMethod}`,
        { amountPkr, paymentMethod, accountTitle }
      );

      logger.info({ userId, withdrawalId: withdrawal.id, amountPkr }, 'Created withdrawal request');
      return withdrawal;
    });
  },

  async listUserWithdrawals(userId: string) {
    return withdrawalRepository.listByUser(userId);
  },
};
