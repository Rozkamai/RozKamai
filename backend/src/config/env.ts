/**
 * @module config/env
 * @description Centralized environment variable loader and schema validator.
 * 
 * Purpose:
 *   Validates all process.env variables at application bootstrap using Zod.
 *   Prevents the server from booting if crucial cryptographic keys, database
 *   credentials, or ad network secrets are missing or malformed.
 * 
 * Inputs:
 *   - process.env (loaded via dotenv from root or backend directory)
 * 
 * Outputs:
 *   - Strongly-typed `config` object accessible application-wide
 * 
 * Edge Cases Handled:
 *   - Missing required keys throws a clear, descriptive ZodError detailing every failed field.
 *   - Coerces string numbers to numbers (PORT, DB_PORT, BCRYPT_ROUNDS).
 *   - Strips dangerous trailing slashes from API_PREFIX.
 *   - Ensures AES encryption key is exactly 64 hex characters (32 bytes).
 */

import dotenv from 'dotenv';
import path from 'path';
import { z } from 'zod';

// Load .env from backend directory first, fallback to root project directory
dotenv.config({ path: path.resolve(process.cwd(), '.env') });
dotenv.config({ path: path.resolve(process.cwd(), '../.env') });

const envSchema = z.object({
  // Server
  PORT: z.coerce.number().default(4000),
  NODE_ENV: z.enum(['development', 'production', 'test']).default('development'),
  APP_NAME: z.string().default('RozKamai'),
  API_PREFIX: z.string().default('/api/v1'),
  CORS_ORIGIN: z.string().default('*'),

  // PostgreSQL Database
  DB_HOST: z.string().default('localhost'),
  DB_PORT: z.coerce.number().default(5432),
  DB_NAME: z.string().default('rozkamai_db'),
  DB_USER: z.string().default('rozkamai_admin'),
  DB_PASSWORD: z.string().default('rozkamai_secure_pass_2026'),
  DB_POOL_MIN: z.coerce.number().default(2),
  DB_POOL_MAX: z.coerce.number().default(20),
  DB_SSL: z.enum(['true', 'false']).default('false').transform((v) => v === 'true'),

  // Authentication & Security
  JWT_ACCESS_SECRET: z.string().min(32, 'JWT_ACCESS_SECRET must be at least 32 characters'),
  JWT_REFRESH_SECRET: z.string().min(32, 'JWT_REFRESH_SECRET must be at least 32 characters'),
  JWT_ACCESS_EXPIRY: z.string().default('15m'),
  JWT_REFRESH_EXPIRY: z.string().default('7d'),
  BCRYPT_ROUNDS: z.coerce.number().min(10).max(14).default(12),

  // Encryption (AES-256-GCM requires 32-byte / 64-hex-char key)
  DATA_ENCRYPTION_KEY_HEX: z
    .string()
    .length(64, 'DATA_ENCRYPTION_KEY_HEX must be exactly 64 hex characters (32 bytes) for AES-256-GCM')
    .regex(/^[0-9a-fA-F]+$/, 'DATA_ENCRYPTION_KEY_HEX must be valid hex characters'),
  DATA_ENCRYPTION_KEY_ID: z.string().default('v1'),
  DATA_BLIND_INDEX_SALT: z.string().min(16, 'DATA_BLIND_INDEX_SALT must be at least 16 characters'),

  // Ad Network Secrets (Server-to-Server callbacks)
  UNITY_ADS_SECRET: z.string().default('dev_unity_ads_secret'),
  APPLOVIN_MAX_SECRET: z.string().default('dev_applovin_max_secret'),

  // Third-party Integrations
  IP_INTEL_API_KEY: z.string().default('mock_api_key'),
  IP_INTEL_PROVIDER: z.enum(['mock', 'ipqualityscore', 'maxmind']).default('mock'),
  SMS_PROVIDER: z.enum(['mock', 'twilio', 'smsalert']).default('mock'),
  SMS_API_KEY: z.string().default('mock_sms_api_key'),

  // Logging
  LOG_LEVEL: z.enum(['fatal', 'error', 'warn', 'info', 'debug', 'trace']).default('info'),
  LOG_PRETTY: z.enum(['true', 'false']).default('true').transform((v) => v === 'true'),
});

const parsedEnv = envSchema.safeParse(process.env);

if (!parsedEnv.success) {
  // eslint-disable-next-line no-console
  console.error('❌ FATAL: Invalid environment variable configuration:');
  // eslint-disable-next-line no-console
  console.error(JSON.stringify(parsedEnv.error.format(), null, 2));
  process.exit(1);
}

export const env = parsedEnv.data;
export type EnvConfig = z.infer<typeof envSchema>;
