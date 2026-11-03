/**
 * @module db/seed
 * @description Database seeding script with realistic Pakistani demo data.
 * 
 * Purpose:
 *   Prepopulates the database with an admin user, test users, devices, wallets,
 *   ledger history, and sample withdrawal requests for development & integration tests.
 */

import bcrypt from 'bcrypt';
import { pool, withTransaction } from '../config/database';
import { logger } from '../config/logger';
import { encryptData, computeBlindIndex, maskCnic } from '../utils/crypto';

async function seed() {
  logger.info('Starting database seed...');

  await withTransaction(async (client) => {
    // 1. Seed Super Admin
    const adminPassHash = await bcrypt.hash('AdminPass123!', 12);
    await client.query(`
      INSERT INTO admin_users (email, password_hash, full_name, role)
      VALUES ('admin@rozkamai.pk', $1, 'RozKamai System Admin', 'SUPER_ADMIN')
      ON CONFLICT (email) DO NOTHING
    `, [adminPassHash]);

    // 2. Seed Demo User
    const userPassHash = await bcrypt.hash('UserPass123!', 12);
    const userRes = await client.query(`
      INSERT INTO users (phone, password_hash, full_name, is_age_verified, first_withdrawal_eligible_at)
      VALUES ('03001234567', $1, 'Hamza Ali', true, NOW() - INTERVAL '10 days')
      ON CONFLICT (phone) DO UPDATE SET full_name = EXCLUDED.full_name
      RETURNING id
    `, [userPassHash]);

    const userId = userRes.rows[0].id;

    // 3. Seed Device
    await client.query(`
      INSERT INTO devices (user_id, device_fingerprint, platform, os_version, app_version, is_rooted, play_integrity_status, last_seen_ip)
      VALUES ($1, 'demo_android_fingerprint_001', 'android', '14.0', '1.0.0', false, 'MEETS_DEVICE_INTEGRITY', '127.0.0.1')
      ON CONFLICT (device_fingerprint) DO NOTHING
    `, [userId]);

    // 4. Seed Wallet
    const walletRes = await client.query(`
      INSERT INTO wallets (user_id, total_earned_coins, available_balance_coins, pending_withdrawal_coins)
      VALUES ($1, 70000, 20000, 50000)
      ON CONFLICT (user_id) DO UPDATE SET
        total_earned_coins = 70000,
        available_balance_coins = 20000,
        pending_withdrawal_coins = 50000
      RETURNING id
    `, [userId]);

    const walletId = walletRes.rows[0].id;

    // 5. Seed KYC Record
    const demoCnic = '4210112345671';
    const cnicEncrypted = encryptData(demoCnic);
    const cnicHash = computeBlindIndex(demoCnic);
    const cnicMasked = maskCnic(demoCnic);

    await client.query(`
      INSERT INTO kyc_records (user_id, cnic_encrypted, cnic_hash, cnic_masked, status, verified_at)
      VALUES ($1, $2, $3, $4, 'VERIFIED', NOW() - INTERVAL '5 days')
      ON CONFLICT (user_id) DO NOTHING
    `, [userId, cnicEncrypted, cnicHash, cnicMasked]);

    // 6. Seed Sample Ledger Entries
    await client.query(`
      INSERT INTO ledger_entries (user_id, wallet_id, entry_type, amount_coins, balance_before_coins, balance_after_coins, reference_type, reference_id, description)
      VALUES 
      ($1, $2, 'REWARD_USER_70', 70, 0, 70, 'AD_SESSION', gen_random_uuid()::text, 'Rewarded Video 70% share (+70 coins)'),
      ($1, $2, 'REWARD_USER_70', 70, 70, 140, 'AD_SESSION', gen_random_uuid()::text, 'Rewarded Video 70% share (+70 coins)'),
      ($1, $2, 'WITHDRAWAL_HOLD', -50000, 70000, 20000, 'WITHDRAWAL', gen_random_uuid()::text, 'Withdrawal request PKR 500 via JAZZ_CASH')
    `, [userId, walletId]);

    // 7. Seed Sample Pending Withdrawal
    const jazzAccount = '03001234567';
    await client.query(`
      INSERT INTO withdrawals (
        user_id, wallet_id, amount_coins, amount_paisa, amount_pkr_display,
        payment_method, account_number_encrypted, account_number_hash,
        account_title, status
      )
      VALUES (
        $1, $2, 50000, 50000, 500.00,
        'JAZZ_CASH', $3, $4,
        'Hamza Ali', 'PENDING'
      )
    `, [userId, walletId, encryptData(jazzAccount), computeBlindIndex(jazzAccount)]);

    logger.info('Database seeded successfully with demo users, wallet, and sample withdrawal.');
  });

  await pool.end();
}

seed().catch((err) => {
  logger.error({ err }, 'Seed failed');
  process.exit(1);
});
