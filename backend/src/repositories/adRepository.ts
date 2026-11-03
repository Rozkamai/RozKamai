/**
 * @module repositories/adRepository
 * @description SQL data access layer for ad sessions, S2S callbacks, and impressions.
 * 
 * Purpose:
 *   Manages ad session lifecycles, ensures callback idempotency via transaction IDs,
 *   and tracks ad impressions for audit & frequency capping.
 * 
 * Inputs:
 *   - User, device, session nonces, S2S callback payloads
 * 
 * Outputs:
 *   - Session records, callback verification status, impressions
 * 
 * Edge Cases Handled:
 *   - Duplicate transaction_id from ad networks is caught via unique constraint.
 *   - Expired sessions are prevented from completion.
 */

import { PoolClient } from 'pg';
import { query } from '../config/database';

export interface AdSessionRow {
  id: string;
  user_id: string;
  device_id: string;
  network: 'unity' | 'applovin';
  ad_unit_id: string;
  nonce: string;
  status: 'CREATED' | 'PENDING_CALLBACK' | 'COMPLETED' | 'EXPIRED' | 'FRAUD_REJECTED';
  expires_at: Date;
  created_at: Date;
  updated_at: Date;
}

export interface AdCallbackRow {
  id: string;
  ad_session_id: string | null;
  network: 'unity' | 'applovin';
  transaction_id: string;
  raw_payload: any;
  signature: string;
  is_verified: boolean;
  processed_at: Date | null;
  created_at: Date;
}

export const adRepository = {
  async createSession(
    userId: string,
    deviceId: string,
    network: 'unity' | 'applovin',
    adUnitId: string,
    nonce: string,
    durationMinutes: number = 10
  ): Promise<AdSessionRow> {
    const res = await query<AdSessionRow>(
      `INSERT INTO ad_sessions (user_id, device_id, network, ad_unit_id, nonce, status, expires_at)
       VALUES ($1, $2, $3, $4, $5, 'CREATED', NOW() + ($6 || ' minutes')::interval)
       RETURNING *`,
      [userId, deviceId, network, adUnitId, nonce, durationMinutes.toString()]
    );
    return res.rows[0];
  },

  async findSessionById(id: string): Promise<AdSessionRow | null> {
    const res = await query<AdSessionRow>(
      'SELECT * FROM ad_sessions WHERE id = $1 LIMIT 1',
      [id]
    );
    return res.rows[0] || null;
  },

  async findSessionByNonce(nonce: string): Promise<AdSessionRow | null> {
    const res = await query<AdSessionRow>(
      'SELECT * FROM ad_sessions WHERE nonce = $1 LIMIT 1',
      [nonce]
    );
    return res.rows[0] || null;
  },

  async updateSessionStatus(
    client: PoolClient,
    sessionId: string,
    status: AdSessionRow['status']
  ): Promise<void> {
    await client.query(
      'UPDATE ad_sessions SET status = $1, updated_at = NOW() WHERE id = $2',
      [status, sessionId]
    );
  },

  async findCallbackByTxId(transactionId: string): Promise<AdCallbackRow | null> {
    const res = await query<AdCallbackRow>(
      'SELECT * FROM ad_callbacks WHERE transaction_id = $1 LIMIT 1',
      [transactionId]
    );
    return res.rows[0] || null;
  },

  async recordCallback(
    client: PoolClient,
    adSessionId: string | null,
    network: 'unity' | 'applovin',
    transactionId: string,
    rawPayload: any,
    signature: string,
    isVerified: boolean
  ): Promise<AdCallbackRow> {
    const res = await client.query<AdCallbackRow>(
      `INSERT INTO ad_callbacks (
         ad_session_id, network, transaction_id, raw_payload, signature, is_verified, processed_at
       )
       VALUES ($1, $2, $3, $4, $5, $6, NOW())
       RETURNING *`,
      [adSessionId, network, transactionId, JSON.stringify(rawPayload), signature, isVerified]
    );
    return res.rows[0];
  },

  async recordImpression(
    userId: string | null,
    deviceId: string | null,
    adType: 'BANNER' | 'INTERSTITIAL' | 'REWARDED',
    network: string,
    placementName: string,
    clientTimestamp: Date
  ): Promise<void> {
    await query(
      `INSERT INTO ad_impressions (user_id, device_id, ad_type, network, placement_name, client_timestamp)
       VALUES ($1, $2, $3, $4, $5, $6)`,
      [userId, deviceId, adType, network, placementName, clientTimestamp]
    );
  },

  async countRecentRewardedAds(
    userId: string,
    intervalHours: number
  ): Promise<number> {
    const res = await query<{ count: string }>(
      `SELECT COUNT(*) as count FROM ad_sessions
       WHERE user_id = $1 AND status = 'COMPLETED'
         AND created_at >= NOW() - ($2 || ' hours')::interval`,
      [userId, intervalHours.toString()]
    );
    return parseInt(res.rows[0]?.count || '0', 10);
  },

  async getLastCompletedAdTime(userId: string): Promise<Date | null> {
    const res = await query<{ updated_at: Date }>(
      `SELECT updated_at FROM ad_sessions
       WHERE user_id = $1 AND status = 'COMPLETED'
       ORDER BY updated_at DESC LIMIT 1`,
      [userId]
    );
    return res.rows[0]?.updated_at || null;
  },
};
