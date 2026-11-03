-- =============================================================================
-- Migration Rollback: 001_initial_schema.down.sql
-- Description: Reverses the initial schema migrations safely in reverse dependency order
-- =============================================================================

DROP TABLE IF EXISTS settings CASCADE;
DROP TABLE IF EXISTS admin_users CASCADE;
DROP TABLE IF EXISTS fraud_events CASCADE;
DROP TABLE IF EXISTS kyc_records CASCADE;
DROP TABLE IF EXISTS withdrawals CASCADE;
DROP TABLE IF EXISTS ad_impressions CASCADE;
DROP TABLE IF EXISTS ad_callbacks CASCADE;
DROP TABLE IF EXISTS ad_sessions CASCADE;
DROP TABLE IF EXISTS ledger_entries CASCADE;
DROP TABLE IF EXISTS wallets CASCADE;
DROP TABLE IF EXISTS devices CASCADE;
DROP TABLE IF EXISTS users CASCADE;
