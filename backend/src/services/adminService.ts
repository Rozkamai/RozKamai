/**
 * @module services/adminService
 * @description Administrative control plane for financial reconciliations, CSV exports, and fraud management.
 * 
 * Purpose:
 *   Enables finance admins to review pending withdrawals, export CSVs, mark payments as PAID or REJECTED
 *   (with automatic ledger refund), view 30% platform retained pool revenue, and adjust settings.
 * 
 * Inputs:
 *   - Admin credentials, withdrawal IDs, resolution notes, transaction references
 * 
 * Outputs:
 *   - Financial summaries, CSV streams, ledger adjustments
 * 
 * Edge Cases Handled:
 *   - Rejecting a withdrawal restores the user's available balance in one atomic transaction.
 *   - Decrypts account numbers securely during CSV generation without exposing them in logs.
 */

import { withTransaction, query } from '../config/database';
import { withdrawalRepository, WithdrawalRow } from '../repositories/withdrawalRepository';
import { walletRepository } from '../repositories/walletRepository';
import { ledgerRepository } from '../repositories/ledgerRepository';
import { userRepository } from '../repositories/userRepository';
import { fraudRepository } from '../repositories/fraudRepository';
import { settingsRepository, PlatformSettings } from '../repositories/settingsRepository';
import { decryptData } from '../utils/crypto';
import { logger } from '../config/logger';

export const adminService = {
  async getPendingWithdrawals(limit: number = 50, offset: number = 0) {
    const rows = await withdrawalRepository.listPending(limit, offset);
    return rows.map((r) => ({
      id: r.id,
      userId: r.user_id,
      amountPkr: parseFloat(r.amount_pkr_display),
      amountCoins: parseInt(r.amount_coins, 10),
      paymentMethod: r.payment_method,
      accountTitle: r.account_title,
      accountNumber: decryptData(r.account_number_encrypted),
      bankName: r.bank_name,
      bankIban: r.bank_iban_encrypted ? decryptData(r.bank_iban_encrypted) : null,
      status: r.status,
      createdAt: r.created_at,
    }));
  },

  async exportWithdrawalsCsv(): Promise<string> {
    const rows = await withdrawalRepository.listPending(1000, 0);
    const headers = [
      'Withdrawal_ID',
      'User_ID',
      'Amount_PKR',
      'Payment_Method',
      'Account_Title',
      'Account_Number',
      'Bank_Name',
      'Bank_IBAN',
      'Created_At',
    ];

    const lines = [headers.join(',')];

    for (const r of rows) {
      const decAccount = `"${decryptData(r.account_number_encrypted)}"`;
      const decIban = r.bank_iban_encrypted ? `"${decryptData(r.bank_iban_encrypted)}"` : '""';
      const rowData = [
        r.id,
        r.user_id,
        r.amount_pkr_display,
        r.payment_method,
        `"${r.account_title.replace(/"/g, '""')}"`,
        decAccount,
        `"${(r.bank_name || '').replace(/"/g, '""')}"`,
        decIban,
        r.created_at.toISOString(),
      ];
      lines.push(rowData.join(','));
    }

    return lines.join('\n');
  },

  async markWithdrawalPaid(withdrawalId: string, paidReference: string, adminNotes?: string) {
    const withdrawal = await withdrawalRepository.findById(withdrawalId);
    if (!withdrawal) throw new Error('Withdrawal not found');

    return withTransaction(async (client) => {
      // 1. Mark paid in DB
      const updated = await withdrawalRepository.markPaid(client, withdrawalId, paidReference, adminNotes);

      // 2. Lock wallet and finalize debit
      const coins = parseInt(withdrawal.amount_coins, 10);
      const wallet = await walletRepository.lockWalletByUserId(client, withdrawal.user_id);
      await walletRepository.finalizeWithdrawal(client, wallet.id, coins);

      // 3. Append ledger entry
      await ledgerRepository.appendEntry(
        client,
        withdrawal.user_id,
        wallet.id,
        'WITHDRAWAL_PAID',
        0, // available balance was already debited on HOLD
        parseInt(wallet.available_balance_coins, 10),
        parseInt(wallet.available_balance_coins, 10),
        'WITHDRAWAL',
        withdrawal.id,
        `Withdrawal paid via reference ${paidReference}`,
        { paidReference, adminNotes }
      );

      logger.info({ withdrawalId, paidReference }, 'Admin marked withdrawal as PAID');
      return updated;
    });
  },

  async rejectWithdrawal(withdrawalId: string, reason: string, adminNotes?: string) {
    const withdrawal = await withdrawalRepository.findById(withdrawalId);
    if (!withdrawal) throw new Error('Withdrawal not found');

    return withTransaction(async (client) => {
      // 1. Mark rejected in DB
      const updated = await withdrawalRepository.markRejected(client, withdrawalId, reason, adminNotes);

      // 2. Lock wallet and refund held balance
      const coins = parseInt(withdrawal.amount_coins, 10);
      const wallet = await walletRepository.lockWalletByUserId(client, withdrawal.user_id);
      const balanceBefore = parseInt(wallet.available_balance_coins, 10);
      const balanceAfter = balanceBefore + coins;

      await walletRepository.refundWithdrawal(client, wallet.id, coins);

      // 3. Append ledger refund entry
      await ledgerRepository.appendEntry(
        client,
        withdrawal.user_id,
        wallet.id,
        'WITHDRAWAL_REFUND',
        coins,
        balanceBefore,
        balanceAfter,
        'WITHDRAWAL',
        withdrawal.id,
        `Withdrawal rejected: ${reason}. Funds refunded to available balance.`,
        { reason, adminNotes }
      );

      logger.info({ withdrawalId, reason }, 'Admin rejected withdrawal and refunded balance');
      return updated;
    });
  },

  async getPlatformRevenueStats() {
    const res = await query<{
      platform_coins: string;
      user_coins: string;
      total_paid_pkr: string;
      total_pending_pkr: string;
      total_ads_completed: string;
    }>(`
      SELECT
        COALESCE(SUM(CASE WHEN entry_type = 'REWARD_PLATFORM_30' THEN amount_coins ELSE 0 END), 0)::text as platform_coins,
        COALESCE(SUM(CASE WHEN entry_type = 'REWARD_USER_70' THEN amount_coins ELSE 0 END), 0)::text as user_coins,
        (SELECT COALESCE(SUM(amount_pkr_display), 0)::text FROM withdrawals WHERE status = 'PAID') as total_paid_pkr,
        (SELECT COALESCE(SUM(amount_pkr_display), 0)::text FROM withdrawals WHERE status IN ('PENDING', 'APPROVED')) as total_pending_pkr,
        (SELECT COUNT(*)::text FROM ad_sessions WHERE status = 'COMPLETED') as total_ads_completed
      FROM ledger_entries
    `);

    const row = res.rows[0];
    const platformCoins = parseInt(row.platform_coins, 10);
    const userCoins = parseInt(row.user_coins, 10);
    const grossCoins = platformCoins + userCoins;

    return {
      platformRetainedPoolCoins: platformCoins,
      platformRetainedPkr: (platformCoins / 100).toFixed(2),
      userDistributedCoins: userCoins,
      userDistributedPkr: (userCoins / 100).toFixed(2),
      grossCoins,
      grossPkr: (grossCoins / 100).toFixed(2),
      totalPaidPkr: parseFloat(row.total_paid_pkr),
      totalPendingPkr: parseFloat(row.total_pending_pkr),
      totalAdsCompleted: parseInt(row.total_ads_completed, 10),
    };
  },

  async getFraudEvents(limit: number = 50, offset: number = 0) {
    return fraudRepository.listRecent(limit, offset);
  },

  async setBlockUser(userId: string, isBlocked: boolean, reason?: string) {
    await userRepository.setBlocked(userId, isBlocked, reason || null);
  },

  async updateSettings(key: keyof PlatformSettings, value: number, adminId?: string) {
    await settingsRepository.update(key, value, adminId);
  },
};
