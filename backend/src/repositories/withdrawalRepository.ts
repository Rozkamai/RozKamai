/**
 * @module repositories/withdrawalRepository
 * @description SQL data access layer for withdrawal requests and status transitions.
 * 
 * Purpose:
 *   Tracks life cycle of withdrawal requests (PENDING -> APPROVED -> PAID / REJECTED)
 *   and enforces that users can only have one pending withdrawal at a time.
 * 
 * Inputs:
 *   - User, wallet, coin amount, encrypted account details, bank details
 * 
 * Outputs:
 *   - Withdrawal records with encrypted data
 * 
 * Edge Cases Handled:
 *   - Rejection requires recorded reason.
 *   - Payment requires reference ID.
 */

import { PoolClient } from 'pg';
import { query } from '../config/database';

export interface WithdrawalRow {
  id: string;
  user_id: string;
  wallet_id: string;
  amount_coins: string;
  amount_paisa: string;
  amount_pkr_display: string;
  payment_method: 'JAZZ_CASH' | 'EASY_PAISA' | 'BANK_TRANSFER';
  account_number_encrypted: string;
  account_number_hash: string;
  account_title: string;
  bank_name: string | null;
  bank_iban_encrypted: string | null;
  bank_iban_hash: string | null;
  status: 'PENDING' | 'APPROVED' | 'PAID' | 'REJECTED';
  admin_notes: string | null;
  paid_reference: string | null;
  paid_at: Date | null;
  rejected_at: Date | null;
  rejected_reason: string | null;
  created_at: Date;
  updated_at: Date;
}

export const withdrawalRepository = {
  async hasPendingWithdrawal(userId: string): Promise<boolean> {
    const res = await query(
      "SELECT 1 FROM withdrawals WHERE user_id = $1 AND status IN ('PENDING', 'APPROVED') LIMIT 1",
      [userId]
    );
    return res.rows.length > 0;
  },

  async createWithdrawal(
    client: PoolClient,
    userId: string,
    walletId: string,
    amountCoins: number,
    amountPaisa: number,
    amountPkrDisplay: number,
    paymentMethod: WithdrawalRow['payment_method'],
    accountNumberEncrypted: string,
    accountNumberHash: string,
    accountTitle: string,
    bankName: string | null,
    bankIbanEncrypted: string | null,
    bankIbanHash: string | null
  ): Promise<WithdrawalRow> {
    const res = await client.query<WithdrawalRow>(
      `INSERT INTO withdrawals (
         user_id, wallet_id, amount_coins, amount_paisa, amount_pkr_display,
         payment_method, account_number_encrypted, account_number_hash,
         account_title, bank_name, bank_iban_encrypted, bank_iban_hash, status
       )
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, 'PENDING')
       RETURNING *`,
      [
        userId,
        walletId,
        amountCoins,
        amountPaisa,
        amountPkrDisplay,
        paymentMethod,
        accountNumberEncrypted,
        accountNumberHash,
        accountTitle,
        bankName,
        bankIbanEncrypted,
        bankIbanHash,
      ]
    );
    return res.rows[0];
  },

  async findById(id: string): Promise<WithdrawalRow | null> {
    const res = await query<WithdrawalRow>(
      'SELECT * FROM withdrawals WHERE id = $1 LIMIT 1',
      [id]
    );
    return res.rows[0] || null;
  },

  async listByUser(userId: string): Promise<WithdrawalRow[]> {
    const res = await query<WithdrawalRow>(
      'SELECT * FROM withdrawals WHERE user_id = $1 ORDER BY created_at DESC',
      [userId]
    );
    return res.rows;
  },

  async listPending(limit: number = 100, offset: number = 0): Promise<WithdrawalRow[]> {
    const res = await query<WithdrawalRow>(
      `SELECT * FROM withdrawals WHERE status IN ('PENDING', 'APPROVED')
       ORDER BY created_at ASC LIMIT $1 OFFSET $2`,
      [limit, offset]
    );
    return res.rows;
  },

  async markPaid(
    client: PoolClient,
    id: string,
    paidReference: string,
    adminNotes?: string
  ): Promise<WithdrawalRow> {
    const res = await client.query<WithdrawalRow>(
      `UPDATE withdrawals
       SET status = 'PAID', paid_reference = $1, admin_notes = COALESCE($2, admin_notes),
           paid_at = NOW(), updated_at = NOW()
       WHERE id = $3 AND status IN ('PENDING', 'APPROVED')
       RETURNING *`,
      [paidReference, adminNotes || null, id]
    );
    if (res.rowCount === 0) {
      throw new Error(`Withdrawal ${id} is not in a payable status`);
    }
    return res.rows[0];
  },

  async markRejected(
    client: PoolClient,
    id: string,
    reason: string,
    adminNotes?: string
  ): Promise<WithdrawalRow> {
    const res = await client.query<WithdrawalRow>(
      `UPDATE withdrawals
       SET status = 'REJECTED', rejected_reason = $1, admin_notes = COALESCE($2, admin_notes),
           rejected_at = NOW(), updated_at = NOW()
       WHERE id = $3 AND status IN ('PENDING', 'APPROVED')
       RETURNING *`,
      [reason, adminNotes || null, id]
    );
    if (res.rowCount === 0) {
      throw new Error(`Withdrawal ${id} is not in a rejectable status`);
    }
    return res.rows[0];
  },
};
