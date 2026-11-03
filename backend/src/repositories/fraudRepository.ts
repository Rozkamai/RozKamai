/**
 * @module repositories/fraudRepository
 * @description SQL data access layer for fraud telemetry and risk events.
 * 
 * Purpose:
 *   Logs security events (VPN detection, emulator/root detection, frequency abuse)
 *   and queries user risk history.
 * 
 * Inputs:
 *   - User, device, event_type, severity, action_taken, ip, details
 * 
 * Outputs:
 *   - Fraud event records
 * 
 * Edge Cases Handled:
 *   - Accepts null user_id/device_id for unauthenticated exploit attempts.
 */

import { query } from '../config/database';

export interface FraudEventRow {
  id: string;
  user_id: string | null;
  device_id: string | null;
  event_type: string;
  severity: 'INFO' | 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  action_taken: 'ALLOW' | 'THROTTLE' | 'HOLD_REWARDS' | 'BLOCK';
  ip_address: string | null;
  details: Record<string, any>;
  created_at: Date;
}

export const fraudRepository = {
  async logEvent(
    userId: string | null,
    deviceId: string | null,
    eventType: FraudEventRow['event_type'],
    severity: FraudEventRow['severity'],
    actionTaken: FraudEventRow['action_taken'],
    ipAddress: string | null,
    details: Record<string, any> = {}
  ): Promise<FraudEventRow> {
    const res = await query<FraudEventRow>(
      `INSERT INTO fraud_events (user_id, device_id, event_type, severity, action_taken, ip_address, details)
       VALUES ($1, $2, $3, $4, $5, $6, $7)
       RETURNING *`,
      [userId, deviceId, eventType, severity, actionTaken, ipAddress, JSON.stringify(details)]
    );
    return res.rows[0];
  },

  async listRecent(limit: number = 50, offset: number = 0): Promise<FraudEventRow[]> {
    const res = await query<FraudEventRow>(
      'SELECT * FROM fraud_events ORDER BY created_at DESC LIMIT $1 OFFSET $2',
      [limit, offset]
    );
    return res.rows;
  },
};
