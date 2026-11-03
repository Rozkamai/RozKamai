-- =============================================================================
-- Migration: 001_initial_schema.sql
-- Description: Core schema for RozKamai Watch-to-Earn PKR platform
-- Tables: users, devices, wallets, ledger_entries, ad_sessions, ad_callbacks,
--         ad_impressions, withdrawals, kyc_records, fraud_events, admin_users, settings
-- Constraints: Integer currency representation, row-level locks, idempotent unique keys
-- =============================================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 1. USERS TABLE
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    phone VARCHAR(20) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    is_age_verified BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_blocked BOOLEAN NOT NULL DEFAULT FALSE,
    block_reason TEXT,
    risk_score INT NOT NULL DEFAULT 0,
    first_withdrawal_eligible_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() + INTERVAL '48 hours'),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_users_phone ON users(phone);
CREATE INDEX IF NOT EXISTS idx_users_is_blocked ON users(is_blocked);

-- 2. DEVICES TABLE (Enforces 1 account per physical device fingerprint)
CREATE TABLE IF NOT EXISTS devices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    device_fingerprint VARCHAR(128) NOT NULL UNIQUE,
    platform VARCHAR(30) NOT NULL DEFAULT 'android',
    os_version VARCHAR(30),
    app_version VARCHAR(30),
    is_rooted BOOLEAN NOT NULL DEFAULT FALSE,
    play_integrity_status VARCHAR(50) NOT NULL DEFAULT 'UNEVALUATED',
    last_seen_ip VARCHAR(45),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_devices_user_id ON devices(user_id);
CREATE INDEX IF NOT EXISTS idx_devices_fingerprint ON devices(device_fingerprint);

-- 3. WALLETS TABLE (Integer coin accounting, strictly >= 0)
CREATE TABLE IF NOT EXISTS wallets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE RESTRICT,
    total_earned_coins BIGINT NOT NULL DEFAULT 0 CHECK (total_earned_coins >= 0),
    available_balance_coins BIGINT NOT NULL DEFAULT 0 CHECK (available_balance_coins >= 0),
    pending_withdrawal_coins BIGINT NOT NULL DEFAULT 0 CHECK (pending_withdrawal_coins >= 0),
    version INT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_wallets_user_id ON wallets(user_id);

-- 4. LEDGER ENTRIES TABLE (Strictly append-only double-entry audit trail)
CREATE TABLE IF NOT EXISTS ledger_entries (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    wallet_id UUID NOT NULL REFERENCES wallets(id) ON DELETE RESTRICT,
    entry_type VARCHAR(50) NOT NULL CHECK (
        entry_type IN ('REWARD_USER_70', 'REWARD_PLATFORM_30', 'WITHDRAWAL_HOLD', 'WITHDRAWAL_PAID', 'WITHDRAWAL_REFUND', 'MANUAL_ADJUSTMENT')
    ),
    amount_coins BIGINT NOT NULL,
    balance_before_coins BIGINT NOT NULL,
    balance_after_coins BIGINT NOT NULL,
    reference_type VARCHAR(50) NOT NULL CHECK (
        reference_type IN ('AD_SESSION', 'WITHDRAWAL', 'ADMIN')
    ),
    reference_id VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    metadata JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ledger_user_created ON ledger_entries(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_ledger_wallet_id ON ledger_entries(wallet_id);
CREATE INDEX IF NOT EXISTS idx_ledger_reference ON ledger_entries(reference_type, reference_id);

-- 5. AD SESSIONS TABLE (Tracks state for server-to-server reward reconciliation)
CREATE TABLE IF NOT EXISTS ad_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    device_id UUID NOT NULL REFERENCES devices(id) ON DELETE RESTRICT,
    network VARCHAR(30) NOT NULL CHECK (network IN ('unity', 'applovin')),
    ad_unit_id VARCHAR(100) NOT NULL,
    nonce VARCHAR(64) NOT NULL UNIQUE,
    status VARCHAR(30) NOT NULL DEFAULT 'CREATED' CHECK (
        status IN ('CREATED', 'PENDING_CALLBACK', 'COMPLETED', 'EXPIRED', 'FRAUD_REJECTED')
    ),
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ad_sessions_nonce ON ad_sessions(nonce);
CREATE INDEX IF NOT EXISTS idx_ad_sessions_user_status ON ad_sessions(user_id, status);
CREATE INDEX IF NOT EXISTS idx_ad_sessions_created ON ad_sessions(created_at DESC);

-- 6. AD CALLBACKS TABLE (Guarantees idempotency via unique network transaction ID)
CREATE TABLE IF NOT EXISTS ad_callbacks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ad_session_id UUID REFERENCES ad_sessions(id) ON DELETE SET NULL,
    network VARCHAR(30) NOT NULL CHECK (network IN ('unity', 'applovin')),
    transaction_id VARCHAR(128) NOT NULL UNIQUE,
    raw_payload JSONB NOT NULL,
    signature VARCHAR(255) NOT NULL,
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    processed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ad_callbacks_tx ON ad_callbacks(transaction_id);
CREATE INDEX IF NOT EXISTS idx_ad_callbacks_session ON ad_callbacks(ad_session_id);

-- 7. AD IMPRESSIONS TABLE (Audits banner, interstitial, and rewarded impressions)
CREATE TABLE IF NOT EXISTS ad_impressions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    device_id UUID REFERENCES devices(id) ON DELETE SET NULL,
    ad_type VARCHAR(30) NOT NULL CHECK (ad_type IN ('BANNER', 'INTERSTITIAL', 'REWARDED')),
    network VARCHAR(30) NOT NULL,
    placement_name VARCHAR(100) NOT NULL,
    client_timestamp TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_impressions_user ON ad_impressions(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_impressions_type ON ad_impressions(ad_type, created_at DESC);

-- 8. WITHDRAWALS TABLE (JazzCash, EasyPaisa, Bank Transfer in PKR)
CREATE TABLE IF NOT EXISTS withdrawals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    wallet_id UUID NOT NULL REFERENCES wallets(id) ON DELETE RESTRICT,
    amount_coins BIGINT NOT NULL CHECK (amount_coins > 0),
    amount_paisa BIGINT NOT NULL CHECK (amount_paisa > 0),
    amount_pkr_display NUMERIC(12, 2) NOT NULL CHECK (amount_pkr_display > 0),
    payment_method VARCHAR(30) NOT NULL CHECK (payment_method IN ('JAZZ_CASH', 'EASY_PAISA', 'BANK_TRANSFER')),
    account_number_encrypted TEXT NOT NULL,
    account_number_hash VARCHAR(64) NOT NULL,
    account_title VARCHAR(120) NOT NULL,
    bank_name VARCHAR(100),
    bank_iban_encrypted TEXT,
    bank_iban_hash VARCHAR(64),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'APPROVED', 'PAID', 'REJECTED')),
    admin_notes TEXT,
    paid_reference VARCHAR(100),
    paid_at TIMESTAMPTZ,
    rejected_at TIMESTAMPTZ,
    rejected_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_withdrawals_user ON withdrawals(user_id, status);
CREATE INDEX IF NOT EXISTS idx_withdrawals_status ON withdrawals(status, created_at DESC);

-- 9. KYC RECORDS TABLE (AES-256-GCM encrypted CNIC with HMAC blind index)
CREATE TABLE IF NOT EXISTS kyc_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE RESTRICT,
    cnic_encrypted TEXT NOT NULL,
    cnic_hash VARCHAR(64) NOT NULL UNIQUE,
    cnic_masked VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'VERIFIED', 'REJECTED')),
    verified_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_kyc_hash ON kyc_records(cnic_hash);
CREATE INDEX IF NOT EXISTS idx_kyc_status ON kyc_records(status);

-- 10. FRAUD EVENTS TABLE (Automated risk & abuse telemetry)
CREATE TABLE IF NOT EXISTS fraud_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    device_id UUID REFERENCES devices(id) ON DELETE SET NULL,
    event_type VARCHAR(50) NOT NULL CHECK (
        event_type IN ('VPN_DETECTED', 'RATE_LIMIT_EXCEEDED', 'INTEGRITY_FAIL', 'FAST_WATCH_TIME', 'UNUSUAL_CADENCE', 'MULTI_ACCOUNT_DEVICE', 'MULTI_ACCOUNT_CNIC', 'SIMULATOR_DETECTED')
    ),
    severity VARCHAR(20) NOT NULL CHECK (severity IN ('INFO', 'LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    action_taken VARCHAR(30) NOT NULL CHECK (action_taken IN ('ALLOW', 'THROTTLE', 'HOLD_REWARDS', 'BLOCK')),
    ip_address VARCHAR(45),
    details JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_fraud_user ON fraud_events(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_fraud_ip ON fraud_events(ip_address, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_fraud_severity ON fraud_events(severity);

-- 11. ADMIN USERS TABLE
CREATE TABLE IF NOT EXISTS admin_users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(120) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role VARCHAR(30) NOT NULL CHECK (role IN ('SUPER_ADMIN', 'FINANCE', 'FRAUD_OFFICER')),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_admin_email ON admin_users(email);

-- 12. SETTINGS TABLE (Configurable platform economics and fraud parameters)
CREATE TABLE IF NOT EXISTS settings (
    key VARCHAR(64) PRIMARY KEY,
    value JSONB NOT NULL,
    description TEXT NOT NULL,
    updated_by UUID REFERENCES admin_users(id) ON DELETE SET NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- SEED INITIAL SETTINGS
INSERT INTO settings (key, value, description) VALUES
('reward_gross_coins_per_ad', '100'::jsonb, 'Gross coins generated per completed rewarded video ad'),
('user_share_percentage', '70'::jsonb, 'Percentage of ad value allocated to the user (70%)'),
('platform_share_percentage', '30'::jsonb, 'Percentage of ad value retained by the platform (30%)'),
('coins_per_pkr', '100'::jsonb, 'Conversion rate: 100 coins equal 1 Pakistani Rupee (PKR 1.00)'),
('min_withdrawal_pkr', '500'::jsonb, 'Minimum withdrawal threshold in PKR'),
('max_daily_rewarded_ads', '60'::jsonb, 'Maximum rewarded ads allowed per user/device in a 24-hour cycle'),
('max_hourly_rewarded_ads', '10'::jsonb, 'Maximum rewarded ads allowed per hour'),
('min_ad_interval_seconds', '30'::jsonb, 'Minimum compulsory cooldown period between ad session requests'),
('new_user_cooldown_hours', '48'::jsonb, 'Cooldown period before new account can request first withdrawal'),
('max_daily_ads_new_user', '30'::jsonb, 'Restricted daily ad cap during first 7 days of registration')
ON CONFLICT (key) DO NOTHING;
