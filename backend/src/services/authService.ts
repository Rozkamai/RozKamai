/**
 * @module services/authService
 * @description Authentication service managing Pakistani phone registration, OTP, JWTs, and session tokens.
 * 
 * Purpose:
 *   Handles secure onboarding with 18+ declaration, bcrypt password hashing,
 *   JWT access/refresh token lifecycle, and device registration.
 * 
 * Inputs:
 *   - Phone (Pakistani format 03XXXXXXXXX), password, full name, 18+ declaration, device info
 * 
 * Outputs:
 *   - Access token (15m), refresh token (7d), user profile, wallet summary
 * 
 * Edge Cases Handled:
 *   - Enforces 1 account per device fingerprint.
 *   - Enforces Pakistani mobile number pattern (starts with 03 or +923).
 *   - Rejects users under 18 years of age.
 */

import bcrypt from 'bcrypt';
import jwt from 'jsonwebtoken';
import { withTransaction } from '../config/database';
import { env } from '../config/env';
import { userRepository } from '../repositories/userRepository';
import { walletRepository } from '../repositories/walletRepository';
import { fraudRepository } from '../repositories/fraudRepository';

export interface TokenPayload {
  userId: string;
  phone: string;
  type: 'access' | 'refresh';
}

export function normalizePakistaniPhone(phone: string): string {
  let cleaned = phone.replace(/[\s\-]/g, '');
  if (cleaned.startsWith('+92')) {
    cleaned = '0' + cleaned.substring(3);
  } else if (cleaned.startsWith('92')) {
    cleaned = '0' + cleaned.substring(2);
  }
  if (!/^03\d{9}$/.test(cleaned)) {
    throw new Error('Invalid Pakistani phone number. Format must be 03XXXXXXXXX (11 digits)');
  }
  return cleaned;
}

export const authService = {
  async register(
    phoneRaw: string,
    password: string,
    fullName: string,
    isAgeVerified: boolean,
    deviceInfo: {
      fingerprint: string;
      platform: string;
      osVersion: string | null;
      appVersion: string | null;
      isRooted: boolean;
      playIntegrityStatus: string;
      ip: string;
    }
  ) {
    if (!isAgeVerified) {
      throw new Error('You must confirm you are 18 years or older to register.');
    }

    const phone = normalizePakistaniPhone(phoneRaw);

    // Check duplicate phone
    const existing = await userRepository.findByPhone(phone);
    if (existing) {
      throw new Error('An account with this phone number already exists.');
    }

    // Check 1 account per device rule
    const existingDevice = await userRepository.findDeviceByFingerprint(deviceInfo.fingerprint);
    if (existingDevice) {
      await fraudRepository.logEvent(
        null,
        existingDevice.id,
        'MULTI_ACCOUNT_DEVICE',
        'HIGH',
        'BLOCK',
        deviceInfo.ip,
        { fingerprint: deviceInfo.fingerprint, attemptedPhone: phone }
      );
      throw new Error('Only one account is permitted per mobile device.');
    }

    const passwordHash = await bcrypt.hash(password, env.BCRYPT_ROUNDS);

    return withTransaction(async (client) => {
      const user = await userRepository.createUser(
        client,
        phone,
        passwordHash,
        fullName,
        isAgeVerified
      );

      const device = await userRepository.registerDevice(
        client,
        user.id,
        deviceInfo.fingerprint,
        deviceInfo.platform,
        deviceInfo.osVersion,
        deviceInfo.appVersion,
        deviceInfo.isRooted,
        deviceInfo.playIntegrityStatus,
        deviceInfo.ip
      );

      const wallet = await walletRepository.createWallet(client, user.id);

      const tokens = authService.generateTokens(user.id, user.phone);

      return {
        user: {
          id: user.id,
          phone: user.phone,
          fullName: user.full_name,
          isAgeVerified: user.is_age_verified,
          firstWithdrawalEligibleAt: user.first_withdrawal_eligible_at,
        },
        device: { id: device.id },
        wallet: {
          availableCoins: wallet.available_balance_coins,
          totalEarnedCoins: wallet.total_earned_coins,
        },
        tokens,
      };
    });
  },

  async login(
    phoneRaw: string,
    password: string,
    deviceInfo: {
      fingerprint: string;
      ip: string;
    }
  ) {
    const phone = normalizePakistaniPhone(phoneRaw);
    const user = await userRepository.findByPhone(phone);

    if (!user) {
      throw new Error('Invalid phone number or password.');
    }

    if (user.is_blocked) {
      throw new Error(`Your account has been suspended: ${user.block_reason || 'Policy violation'}`);
    }

    const isMatch = await bcrypt.compare(password, user.password_hash);
    if (!isMatch) {
      throw new Error('Invalid phone number or password.');
    }

    const wallet = await walletRepository.findByUserId(user.id);
    const tokens = authService.generateTokens(user.id, user.phone);

    return {
      user: {
        id: user.id,
        phone: user.phone,
        fullName: user.full_name,
        isAgeVerified: user.is_age_verified,
        firstWithdrawalEligibleAt: user.first_withdrawal_eligible_at,
      },
      wallet: wallet
        ? {
            availableCoins: wallet.available_balance_coins,
            pendingCoins: wallet.pending_withdrawal_coins,
            totalEarnedCoins: wallet.total_earned_coins,
          }
        : null,
      tokens,
    };
  },

  generateTokens(userId: string, phone: string) {
    const accessToken = jwt.sign(
      { userId, phone, type: 'access' },
      env.JWT_ACCESS_SECRET,
      { expiresIn: '15m' }
    );

    const refreshToken = jwt.sign(
      { userId, phone, type: 'refresh' },
      env.JWT_REFRESH_SECRET,
      { expiresIn: '7d' }
    );

    return { accessToken, refreshToken, expiresInSeconds: 900 };
  },

  verifyAccessToken(token: string): TokenPayload {
    const payload = jwt.verify(token, env.JWT_ACCESS_SECRET) as TokenPayload;
    if (payload.type !== 'access') {
      throw new Error('Invalid token type');
    }
    return payload;
  },

  refreshToken(refreshToken: string) {
    const payload = jwt.verify(refreshToken, env.JWT_REFRESH_SECRET) as TokenPayload;
    if (payload.type !== 'refresh') {
      throw new Error('Invalid token type');
    }
    return authService.generateTokens(payload.userId, payload.phone);
  },
};
