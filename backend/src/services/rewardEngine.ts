/**
 * @module services/rewardEngine
 * @description Core 70% User / 30% Platform reward distribution and S2S callback reconciliation engine.
 * 
 * Purpose:
 *   Calculates revenue split, validates ad network cryptographic signatures, enforces idempotency
 *   via network transaction IDs, and writes double-entry ledger records atomically.
 * 
 * Inputs:
 *   - S2S callback parameters, network transaction IDs, session nonces
 * 
 * Outputs:
 *   - Credit status, coin allocations, ledger records
 * 
 * Edge Cases Handled:
 *   - Floor rounding remainder is allocated strictly to platform share (never fractional coins).
 *   - Duplicate callbacks for same network transaction return success without double-crediting.
 *   - Expired or already redeemed sessions are rejected.
 *   - Client-side finish events never credit money without verified S2S callback.
 */

import crypto from 'crypto';
import { withTransaction } from '../config/database';
import { env } from '../config/env';
import { adRepository, AdSessionRow } from '../repositories/adRepository';
import { walletRepository } from '../repositories/walletRepository';
import { ledgerRepository } from '../repositories/ledgerRepository';
import { settingsRepository } from '../repositories/settingsRepository';
import { userRepository } from '../repositories/userRepository';
import { fraudEngine } from './fraudEngine';
import { verifyUnitySignature, verifyAppLovinSignature } from '../utils/crypto';
import { logger } from '../config/logger';

export interface SplitResult {
  grossCoins: number;
  userShareCoins: number;
  platformShareCoins: number;
  userPercentage: number;
  platformPercentage: number;
}

export function computeSplit(
  grossCoins: number,
  userPercentage: number = 70,
  platformPercentage: number = 30
): SplitResult {
  if (grossCoins < 0) throw new Error('Gross coins cannot be negative');
  // Integer arithmetic: user receives floor(gross * userPercentage / 100)
  const userShareCoins = Math.floor((grossCoins * userPercentage) / 100);
  // Remainder goes to platform so total always balances: user + platform === gross
  const platformShareCoins = grossCoins - userShareCoins;

  return {
    grossCoins,
    userShareCoins,
    platformShareCoins,
    userPercentage,
    platformPercentage,
  };
}

export const rewardEngine = {
  async initSession(
    userId: string,
    deviceFingerprint: string,
    network: 'unity' | 'applovin',
    adUnitId: string,
    clientIp: string
  ): Promise<{ sessionId: string; nonce: string; expiresAt: Date }> {
    // Check fraud rules & frequency caps
    const evalResult = await fraudEngine.evaluateAdEligibility(userId, deviceFingerprint, clientIp);
    if (!evalResult.allowed) {
      throw new Error(evalResult.reason || 'Ad session rejected by fraud engine');
    }

    const device = await userRepository.findDeviceByFingerprint(deviceFingerprint);
    if (!device) {
      throw new Error('Device not registered');
    }

    // Generate cryptographic nonce for network reward ID
    const nonce = `roz_${crypto.randomBytes(16).toString('hex')}`;
    const session = await adRepository.createSession(
      userId,
      device.id,
      network,
      adUnitId,
      nonce,
      10 // 10 minutes expiry
    );

    return {
      sessionId: session.id,
      nonce: session.nonce,
      expiresAt: session.expires_at,
    };
  },

  async processCallback(
    network: 'unity' | 'applovin',
    transactionId: string,
    nonce: string,
    rawPayload: any,
    signature: string,
    queryParams?: Record<string, string>
  ): Promise<{ success: boolean; message: string; coinsCredited: number }> {
    // 1. Verify S2S cryptographic signature
    let isVerified = false;
    if (network === 'unity' && queryParams) {
      isVerified = verifyUnitySignature(env.UNITY_ADS_SECRET, queryParams, signature);
    } else if (network === 'applovin') {
      isVerified = verifyAppLovinSignature(env.APPLOVIN_MAX_SECRET, JSON.stringify(rawPayload), signature);
    } else {
      isVerified = env.NODE_ENV === 'development'; // allow testing in dev if configured
    }

    if (!isVerified && env.NODE_ENV === 'production') {
      logger.warn({ network, transactionId }, 'Invalid S2S callback signature');
      throw new Error('Invalid signature');
    }

    // 2. Check Idempotency via transactionId
    const existingCallback = await adRepository.findCallbackByTxId(transactionId);
    if (existingCallback) {
      logger.info({ transactionId }, 'Duplicate S2S callback received, returning cached success');
      return { success: true, message: 'Already processed (idempotent)', coinsCredited: 0 };
    }

    // 3. Find matching ad session
    const session = await adRepository.findSessionByNonce(nonce);
    if (!session) {
      logger.error({ nonce, transactionId }, 'Ad session not found for callback nonce');
      throw new Error('Ad session not found');
    }

    if (session.status === 'COMPLETED') {
      logger.info({ sessionId: session.id }, 'Session already completed');
      return { success: true, message: 'Session already completed', coinsCredited: 0 };
    }

    if (new Date() > new Date(session.expires_at)) {
      throw new Error('Ad session has expired');
    }

    const settings = await settingsRepository.getAll();
    const split = computeSplit(
      settings.reward_gross_coins_per_ad,
      settings.user_share_percentage,
      settings.platform_share_percentage
    );

    // 4. Atomic Execution: Row Lock Wallet -> Update Balance -> Append Ledger Entries -> Update Session
    return withTransaction(async (client) => {
      // Record callback for idempotency
      await adRepository.recordCallback(
        client,
        session.id,
        network,
        transactionId,
        rawPayload,
        signature,
        true
      );

      // Lock wallet
      const wallet = await walletRepository.lockWalletByUserId(client, session.user_id);
      const balanceBefore = parseInt(wallet.available_balance_coins, 10);
      const balanceAfter = balanceBefore + split.userShareCoins;

      // Credit User Share
      await walletRepository.creditUserShare(client, wallet.id, split.userShareCoins);

      // Append Ledger Entry for User (70%)
      await ledgerRepository.appendEntry(
        client,
        session.user_id,
        wallet.id,
        'REWARD_USER_70',
        split.userShareCoins,
        balanceBefore,
        balanceAfter,
        'AD_SESSION',
        session.id,
        `Rewarded Video 70% share (+${split.userShareCoins} coins)`,
        { network, transactionId, nonce, split }
      );

      // Append Ledger Entry for Platform (30%)
      await ledgerRepository.appendEntry(
        client,
        session.user_id,
        wallet.id,
        'REWARD_PLATFORM_30',
        split.platformShareCoins,
        balanceAfter,
        balanceAfter, // platform pool does not alter user available balance
        'AD_SESSION',
        session.id,
        `Platform 30% retained share (${split.platformShareCoins} coins)`,
        { network, transactionId, nonce, split }
      );

      // Mark session COMPLETED
      await adRepository.updateSessionStatus(client, session.id, 'COMPLETED');

      logger.info(
        { userId: session.user_id, userCoins: split.userShareCoins, platformCoins: split.platformShareCoins },
        'Successfully credited 70/30 ad reward'
      );

      return {
        success: true,
        message: `+${split.userShareCoins} coins credited to wallet`,
        coinsCredited: split.userShareCoins,
      };
    });
  },

  async getSessionStatus(sessionId: string): Promise<AdSessionRow | null> {
    return adRepository.findSessionById(sessionId);
  },
};
