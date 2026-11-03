/**
 * @module repositories/walletRepository
 * @description SQL data access layer for wallet balances with row-level locking.
 * 
 * Purpose:
 *   Performs balance querying, atomic credit, atomic debit/holding, and ledger reconciliation.
 * 
 * Inputs:
 *   - User IDs, coin amounts
 * 
 * Outputs:
 *   - Wallet state with integer coins (total_earned, available, pending)
 * 
 * Edge Cases Handled:
 *   - Uses `FOR UPDATE` to serialize concurrent credit/debit transactions on the same user.
 *   - Checks that balances never drop below 0.
 */

import { PoolClient } from 'pg';
import { query } from '../config/database';

export interface WalletRow {
  id: string;
  user_id: string;
  total_earned_coins: string; // BIGINT as string in pg
  available_balance_coins: string;
  pending_withdrawal_coins: string;
  version: number;
  created_at: Date;
  updated_at: Date;
}

export const walletRepository = {
  async createWallet(client: PoolClient, userId: string): Promise<WalletRow> {
    const res = await client.query<WalletRow>(
      `INSERT INTO wallets (user_id, total_earned_coins, available_balance_coins, pending_withdrawal_coins)
       VALUES ($1, 0, 0, 0)
       RETURNING *`,
      [userId]
    );
    return res.rows[0];
  },

  async findByUserId(userId: string): Promise<WalletRow | null> {
    const res = await query<WalletRow>(
      'SELECT * FROM wallets WHERE user_id = $1 LIMIT 1',
      [userId]
    );
    return res.rows[0] || null;
  },

  /**
   * Locks wallet row for atomic transactional update.
   */
  async lockWalletByUserId(client: PoolClient, userId: string): Promise<WalletRow> {
    const res = await client.query<WalletRow>(
      'SELECT * FROM wallets WHERE user_id = $1 FOR UPDATE',
      [userId]
    );
    if (res.rows.length === 0) {
      throw new Error(`Wallet not found for user ${userId}`);
    }
    return res.rows[0];
  },

  /**
   * Adds coins to available and total earned.
   */
  async creditUserShare(
    client: PoolClient,
    walletId: string,
    coins: number
  ): Promise<WalletRow> {
    const res = await client.query<WalletRow>(
      `UPDATE wallets
       SET available_balance_coins = available_balance_coins + $1,
           total_earned_coins = total_earned_coins + $1,
           version = version + 1,
           updated_at = NOW()
       WHERE id = $2
       RETURNING *`,
      [coins, walletId]
    );
    return res.rows[0];
  },

  /**
   * Moves amount from available to pending during withdrawal request.
   */
  async holdWithdrawal(
    client: PoolClient,
    walletId: string,
    coins: number
  ): Promise<WalletRow> {
    const res = await client.query<WalletRow>(
      `UPDATE wallets
       SET available_balance_coins = available_balance_coins - $1,
           pending_withdrawal_coins = pending_withdrawal_coins + $1,
           version = version + 1,
           updated_at = NOW()
       WHERE id = $2 AND available_balance_coins >= $1
       RETURNING *`,
      [coins, walletId]
    );
    if (res.rowCount === 0) {
      throw new Error('Insufficient balance or concurrent modification');
    }
    return res.rows[0];
  },

  /**
   * Deducts from pending once withdrawal is marked as PAID.
   */
  async finalizeWithdrawal(
    client: PoolClient,
    walletId: string,
    coins: number
  ): Promise<WalletRow> {
    const res = await client.query<WalletRow>(
      `UPDATE wallets
       SET pending_withdrawal_coins = pending_withdrawal_coins - $1,
           version = version + 1,
           updated_at = NOW()
       WHERE id = $2 AND pending_withdrawal_coins >= $1
       RETURNING *`,
      [coins, walletId]
    );
    return res.rows[0];
  },

  /**
   * Restores pending balance back to available when withdrawal is REJECTED.
   */
  async refundWithdrawal(
    client: PoolClient,
    walletId: string,
    coins: number
  ): Promise<WalletRow> {
    const res = await client.query<WalletRow>(
      `UPDATE wallets
       SET available_balance_coins = available_balance_coins + $1,
           pending_withdrawal_coins = pending_withdrawal_coins - $1,
           version = version + 1,
           updated_at = NOW()
       WHERE id = $2 AND pending_withdrawal_coins >= $1
       RETURNING *`,
      [coins, walletId]
    );
    return res.rows[0];
  },

  /**
   * Reconciles current wallet balance with sum of user ledger entries.
   */
  async reconcileWithLedger(userId: string): Promise<{ isReconciled: boolean; ledgerSum: number; walletBalance: number }> {
    const res = await query<{ ledger_sum: string; available_balance: string }>(
      `SELECT
         COALESCE(SUM(amount_coins), 0)::text as ledger_sum,
         (SELECT available_balance_coins FROM wallets WHERE user_id = $1)::text as available_balance
       FROM ledger_entries
       WHERE user_id = $1 AND entry_type NOT IN ('REWARD_PLATFORM_30', 'WITHDRAWAL_PAID')`,
      [userId]
    );

    const ledgerSum = parseInt(res.rows[0]?.ledger_sum || '0', 10);
    const walletBalance = parseInt(res.rows[0]?.available_balance || '0', 10);

    return {
      isReconciled: ledgerSum === walletBalance,
      ledgerSum,
      walletBalance,
    };
  },
};
