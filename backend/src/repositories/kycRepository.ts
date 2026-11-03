/**
 * @module repositories/kycRepository
 * @description SQL data access layer for Pakistani CNIC identity records.
 * 
 * Purpose:
 *   Stores encrypted CNIC values, keyed HMAC hashes for duplicate check, and masked CNICs.
 * 
 * Inputs:
 *   - User ID, encrypted CNIC, blind hash, masked CNIC
 * 
 * Outputs:
 *   - KYC status and audit record
 * 
 * Edge Cases Handled:
 *   - Unique constraint on cnic_hash prevents multi-accounting with same CNIC.
 *   - Never stores or returns raw plaintext CNIC.
 */

import { query } from '../config/database';

export interface KycRecordRow {
  id: string;
  user_id: string;
  cnic_encrypted: string;
  cnic_hash: string;
  cnic_masked: string;
  status: 'PENDING' | 'VERIFIED' | 'REJECTED';
  verified_at: Date | null;
  created_at: Date;
  updated_at: Date;
}

export const kycRepository = {
  async findByUserId(userId: string): Promise<KycRecordRow | null> {
    const res = await query<KycRecordRow>(
      'SELECT * FROM kyc_records WHERE user_id = $1 LIMIT 1',
      [userId]
    );
    return res.rows[0] || null;
  },

  async findByCnicHash(cnicHash: string): Promise<KycRecordRow | null> {
    const res = await query<KycRecordRow>(
      'SELECT * FROM kyc_records WHERE cnic_hash = $1 LIMIT 1',
      [cnicHash]
    );
    return res.rows[0] || null;
  },

  async saveKyc(
    userId: string,
    cnicEncrypted: string,
    cnicHash: string,
    cnicMasked: string,
    status: KycRecordRow['status'] = 'VERIFIED'
  ): Promise<KycRecordRow> {
    const res = await query<KycRecordRow>(
      `INSERT INTO kyc_records (user_id, cnic_encrypted, cnic_hash, cnic_masked, status, verified_at)
       VALUES ($1, $2, $3, $4, $5, CASE WHEN $5 = 'VERIFIED' THEN NOW() ELSE NULL END)
       ON CONFLICT (user_id) DO UPDATE
       SET cnic_encrypted = EXCLUDED.cnic_encrypted,
           cnic_hash = EXCLUDED.cnic_hash,
           cnic_masked = EXCLUDED.cnic_masked,
           status = EXCLUDED.status,
           verified_at = EXCLUDED.verified_at,
           updated_at = NOW()
       RETURNING *`,
      [userId, cnicEncrypted, cnicHash, cnicMasked, status]
    );
    return res.rows[0];
  },
};
