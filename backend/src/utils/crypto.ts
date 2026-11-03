/**
 * @module utils/crypto
 * @description Cryptographic utilities for AES-256-GCM encryption, HMAC blind indexing, and PII masking.
 * 
 * Purpose:
 *   Provides enterprise-grade encryption for sensitive data (Pakistani CNIC, bank accounts)
 *   and keyed blind indexes for duplicate detection without exposing plaintext.
 * 
 * Inputs:
 *   - Plaintext strings (CNIC, account numbers, IBANs)
 *   - Encryption keys from environment config
 * 
 * Outputs:
 *   - Encrypted ciphertext in format: `${keyId}:${ivHex}:${tagHex}:${ciphertextHex}`
 *   - Decrypted plaintext
 *   - Keyed HMAC-SHA256 blind index hash
 *   - Masked CNIC string (e.g., "42101-*******-1")
 * 
 * Edge Cases Handled:
 *   - Authentication tag mismatch throws an explicit integrity error.
 *   - Corrupted ciphertext format throws invalid payload error.
 *   - Masking safely handles malformed length inputs.
 */

import crypto from 'crypto';
import { env } from '../config/env';

const ALGORITHM = 'aes-256-gcm';
const IV_LENGTH = 12; // 96 bits for GCM

/**
 * Encrypts a string using AES-256-GCM.
 * Output format: `keyId:iv:authTag:ciphertext` (all hex encoded)
 */
export function encryptData(plaintext: string): string {
  const iv = crypto.randomBytes(IV_LENGTH);
  const key = Buffer.from(env.DATA_ENCRYPTION_KEY_HEX, 'hex');
  const cipher = crypto.createCipheriv(ALGORITHM, key, iv);

  const encrypted = Buffer.concat([cipher.update(plaintext, 'utf8'), cipher.final()]);
  const tag = cipher.getAuthTag();

  return [
    env.DATA_ENCRYPTION_KEY_ID,
    iv.toString('hex'),
    tag.toString('hex'),
    encrypted.toString('hex'),
  ].join(':');
}

/**
 * Decrypts an AES-256-GCM encrypted string.
 */
export function decryptData(encryptedPayload: string): string {
  const parts = encryptedPayload.split(':');
  if (parts.length !== 4) {
    throw new Error('Invalid encrypted payload format');
  }

  const [, ivHex, tagHex, ciphertextHex] = parts;
  const key = Buffer.from(env.DATA_ENCRYPTION_KEY_HEX, 'hex');
  const iv = Buffer.from(ivHex, 'hex');
  const tag = Buffer.from(tagHex, 'hex');
  const ciphertext = Buffer.from(ciphertextHex, 'hex');

  const decipher = crypto.createDecipheriv(ALGORITHM, key, iv);
  decipher.setAuthTag(tag);

  const decrypted = Buffer.concat([decipher.update(ciphertext), decipher.final()]);
  return decrypted.toString('utf8');
}

/**
 * Generates a keyed HMAC-SHA256 blind index for deterministic duplicate detection.
 * Explanation: Hashing with a secret salt allows exact-match database queries (e.g. checking
 * if a CNIC is already registered) without storing or decrypting raw PII in index tables.
 */
export function computeBlindIndex(value: string): string {
  const normalized = value.trim().toLowerCase().replace(/[^a-zA-Z0-9]/g, '');
  return crypto.createHmac('sha256', env.DATA_BLIND_INDEX_SALT).update(normalized).digest('hex');
}

/**
 * Formats a 13-digit Pakistani CNIC to masked display string (e.g. 42101-*******-1).
 */
export function maskCnic(cnicRaw: string): string {
  const clean = cnicRaw.replace(/\D/g, '');
  if (clean.length !== 13) {
    return '*****-*******-*';
  }
  const prefix = clean.substring(0, 5);
  const suffix = clean.substring(12, 13);
  return `${prefix}-*******-${suffix}`;
}

/**
 * Verifies Unity Ads Server-to-Server HMAC signature.
 */
export function verifyUnitySignature(secret: string, queryParams: Record<string, string>, signatureHeader: string): boolean {
  // Sort keys alphabetically to create deterministic signing string
  const sortedKeys = Object.keys(queryParams).sort();
  const rawString = sortedKeys.map((k) => `${k}=${queryParams[k]}`).join('&');
  const computed = crypto.createHmac('sha256', secret).update(rawString).digest('hex');
  return crypto.timingSafeEqual(Buffer.from(computed), Buffer.from(signatureHeader));
}

/**
 * Verifies AppLovin MAX Postback HMAC signature.
 */
export function verifyAppLovinSignature(secret: string, rawPayload: string, signatureHeader: string): boolean {
  const computed = crypto.createHmac('sha256', secret).update(rawPayload).digest('hex');
  if (computed.length !== signatureHeader.length) return false;
  return crypto.timingSafeEqual(Buffer.from(computed), Buffer.from(signatureHeader));
}
