/**
 * @module repositories/ledgerRepository
 * @description SQL data access layer for append-only double-entry financial ledger.
 * 
 * Purpose:
 *   Inserts immutable audit log entries for every balance movement and queries transaction history.
 * 
 * Inputs:
 *   - User, wallet, amount, entry_type, balance before/after, reference type/id
 * 
 * Outputs:
 *   - Ledger records
 * 
 * Edge Cases Handled:
 *   - Enforces append-only design (NO update or delete operations).
 *   - Serializes JSON metadata for rich audit trails.
 */

import { PoolClient } from 'pg';
import { query } from '../config/database';

export interface LedgerRow {
  id: string;
  user_id: string;
  wallet_id: string;
  entry_type: 'REWARD_USER_70' | 'REWARD_PLATFORM_30' | 'WITHDRAWAL_HOLD' | 'WITHDRAWAL_PAID' | 'WITHDRAWAL_REFUND' | 'MANUAL_ADJUSTMENT';
  amount_coins: string;
  balance_before_coins: string;
  balance_after_coins: string;
  reference_type: 'AD_SESSION' | 'WITHDRAWAL' | 'ADMIN';
  reference_id: string;
  description: string;
  metadata: Record<string, any>;
  created_at: Date;
}

export const ledgerRepository = {
  async appendEntry(
    client: PoolClient,
    userId: string,
    walletId: string,
    entryType: LedgerRow['entry_type'],
    amountCoins: number,
    balanceBefore: number,
    balanceAfter: number,
    referenceType: LedgerRow['reference_type'],
    referenceId: string,
    description: string,
    metadata: Record<string, any> = {}
  ): Promise<LedgerRow> {
    const res = await client.query<LedgerRow>(
      `INSERT INTO ledger_entries (
         user_id, wallet_id, entry_type, amount_coins, balance_before_coins,
         balance_after_coins, reference_type, reference_id, description, metadata
       )
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10)
       RETURNING *`,
      [
        userId,
        walletId,
        entryType,
        amountCoins,
        balanceBefore,
        balanceAfter,
        referenceType,
        referenceId,
        description,
        JSON.stringify(metadata),
      ]
    );
    return res.rows[0];
  },

  async listByUser(
    userId: string,
    limit: number = 50,
    offset: number = 0
  ): Promise<{ entries: LedgerRow[]; total: number }> {
    const [dataRes, countRes] = await Promise.all([
      query<LedgerRow>(
        `SELECT * FROM ledger_entries
         WHERE user_id = $1
         ORDER BY created_at DESC
         LIMIT $2 OFFSET $3`,
        [userId, limit, offset]
      ),
      query<{ count: string }>(
        'SELECT COUNT(*) as count FROM ledger_entries WHERE user_id = $1',
        [userId]
      ),
    ]);

    return {
      entries: dataRes.rows,
      total: parseInt(countRes.rows[0]?.count || '0', 10),
    };
  },
};
