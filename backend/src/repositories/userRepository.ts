/**
 * @module repositories/userRepository
 * @description SQL data access layer for user and device entities.
 * 
 * Purpose:
 *   Handles persistence, querying, device binding, and account status management.
 * 
 * Inputs:
 *   - User registration parameters, credentials, device fingerprints
 * 
 * Outputs:
 *   - User domain records, device linkages, risk score updates
 * 
 * Edge Cases Handled:
 *   - Soft-deleted users are excluded from active queries.
 *   - Duplicate phone or device registration triggers explicit database conflict handling.
 */

import { PoolClient } from 'pg';
import { query } from '../config/database';

export interface UserRow {
  id: string;
  phone: string;
  password_hash: string;
  full_name: string;
  is_age_verified: boolean;
  is_active: boolean;
  is_blocked: boolean;
  block_reason: string | null;
  risk_score: number;
  first_withdrawal_eligible_at: Date;
  created_at: Date;
  updated_at: Date;
  deleted_at: Date | null;
}

export interface DeviceRow {
  id: string;
  user_id: string;
  device_fingerprint: string;
  platform: string;
  os_version: string | null;
  app_version: string | null;
  is_rooted: boolean;
  play_integrity_status: string;
  last_seen_ip: string | null;
  created_at: Date;
  updated_at: Date;
}

export const userRepository = {
  async findByPhone(phone: string): Promise<UserRow | null> {
    const res = await query<UserRow>(
      'SELECT * FROM users WHERE phone = $1 AND deleted_at IS NULL LIMIT 1',
      [phone]
    );
    return res.rows[0] || null;
  },

  async findById(id: string): Promise<UserRow | null> {
    const res = await query<UserRow>(
      'SELECT * FROM users WHERE id = $1 AND deleted_at IS NULL LIMIT 1',
      [id]
    );
    return res.rows[0] || null;
  },

  async createUser(
    client: PoolClient,
    phone: string,
    passwordHash: string,
    fullName: string,
    isAgeVerified: boolean
  ): Promise<UserRow> {
    const res = await client.query<UserRow>(
      `INSERT INTO users (phone, password_hash, full_name, is_age_verified, first_withdrawal_eligible_at)
       VALUES ($1, $2, $3, $4, NOW() + INTERVAL '48 hours')
       RETURNING *`,
      [phone, passwordHash, fullName, isAgeVerified]
    );
    return res.rows[0];
  },

  async findDeviceByFingerprint(fingerprint: string): Promise<DeviceRow | null> {
    const res = await query<DeviceRow>(
      'SELECT * FROM devices WHERE device_fingerprint = $1 LIMIT 1',
      [fingerprint]
    );
    return res.rows[0] || null;
  },

  async registerDevice(
    client: PoolClient,
    userId: string,
    fingerprint: string,
    platform: string,
    osVersion: string | null,
    appVersion: string | null,
    isRooted: boolean,
    playIntegrityStatus: string,
    ip: string
  ): Promise<DeviceRow> {
    const res = await client.query<DeviceRow>(
      `INSERT INTO devices (user_id, device_fingerprint, platform, os_version, app_version, is_rooted, play_integrity_status, last_seen_ip)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
       ON CONFLICT (device_fingerprint) DO UPDATE
       SET last_seen_ip = EXCLUDED.last_seen_ip,
           updated_at = NOW()
       RETURNING *`,
      [userId, fingerprint, platform, osVersion, appVersion, isRooted, playIntegrityStatus, ip]
    );
    return res.rows[0];
  },

  async updateRiskScore(userId: string, delta: number): Promise<void> {
    await query(
      'UPDATE users SET risk_score = GREATEST(0, risk_score + $1), updated_at = NOW() WHERE id = $2',
      [delta, userId]
    );
  },

  async setBlocked(userId: string, isBlocked: boolean, reason: string | null): Promise<void> {
    await query(
      'UPDATE users SET is_blocked = $1, block_reason = $2, updated_at = NOW() WHERE id = $3',
      [isBlocked, reason, userId]
    );
  },

  async softDelete(userId: string): Promise<void> {
    await query(
      'UPDATE users SET deleted_at = NOW(), is_active = FALSE, updated_at = NOW() WHERE id = $1',
      [userId]
    );
  },
};
