/**
 * @module services/fraudEngine
 * @description Real-time fraud detection, abuse prevention, and risk scoring engine.
 * 
 * Purpose:
 *   Evaluates ad request velocity, device tampering (root/emulator), VPN/proxy IPs,
 *   and suspicious cadence patterns to safeguard platform monetization.
 * 
 * Inputs:
 *   - User profile, device telemetry, request IP, ad session timing
 * 
 * Outputs:
 *   - Decision: { allowed: boolean; action: 'ALLOW' | 'THROTTLE' | 'HOLD_REWARDS' | 'BLOCK'; reason?: string }
 * 
 * Edge Cases Handled:
 *   - Enforces 30s minimum interval between ad completions.
 *   - Enforces new-user restriction (first 7 days reduced cap).
 *   - Blocks rooted devices from earning rewards.
 */

import { userRepository } from '../repositories/userRepository';
import { adRepository } from '../repositories/adRepository';
import { fraudRepository } from '../repositories/fraudRepository';
import { settingsRepository } from '../repositories/settingsRepository';

export interface IpIntelligenceProvider {
  isVpnOrProxy(ip: string): Promise<boolean>;
}

// Default IP Intelligence Provider (Mock for dev, easily swappable with MaxMind/IPQualityScore)
export const mockIpIntelligence: IpIntelligenceProvider = {
  async isVpnOrProxy(ip: string): Promise<boolean> {
    // Treat typical datacenter / known testing VPN subnets as flagged
    if (ip.startsWith('10.0.0.') || ip === '1.1.1.1' || ip === '8.8.8.8') {
      return true;
    }
    return false;
  },
};

export const fraudEngine = {
  async evaluateAdEligibility(
    userId: string,
    deviceId: string,
    ip: string
  ): Promise<{ allowed: boolean; action: 'ALLOW' | 'THROTTLE' | 'HOLD_REWARDS' | 'BLOCK'; reason?: string; cooldownSecondsRemaining?: number }> {
    const [user, settings] = await Promise.all([
      userRepository.findById(userId),
      settingsRepository.getAll(),
    ]);

    if (!user) {
      return { allowed: false, action: 'BLOCK', reason: 'User not found' };
    }

    if (user.is_blocked) {
      return { allowed: false, action: 'BLOCK', reason: user.block_reason || 'Account suspended' };
    }

    // 1. Device Root / Tamper Check
    const device = await userRepository.findDeviceByFingerprint(deviceId);
    if (device && device.is_rooted) {
      await fraudRepository.logEvent(
        userId,
        device.id,
        'INTEGRITY_FAIL',
        'HIGH',
        'BLOCK',
        ip,
        { reason: 'Rooted device or unlocked bootloader detected' }
      );
      return { allowed: false, action: 'BLOCK', reason: 'Rewards cannot be earned on rooted or modified devices.' };
    }

    // 2. VPN / Datacenter IP Check
    const isVpn = await mockIpIntelligence.isVpnOrProxy(ip);
    if (isVpn) {
      await fraudRepository.logEvent(
        userId,
        device?.id || null,
        'VPN_DETECTED',
        'MEDIUM',
        'THROTTLE',
        ip,
        { ip }
      );
      return { allowed: false, action: 'THROTTLE', reason: 'VPN or proxy detected. Please disconnect to continue.' };
    }

    // 3. Minimum 30s Cooldown Between Ads
    const lastAdTime = await adRepository.getLastCompletedAdTime(userId);
    if (lastAdTime) {
      const elapsedSeconds = Math.floor((Date.now() - new Date(lastAdTime).getTime()) / 1000);
      if (elapsedSeconds < settings.min_ad_interval_seconds) {
        const remaining = settings.min_ad_interval_seconds - elapsedSeconds;
        return {
          allowed: false,
          action: 'THROTTLE',
          reason: `Please wait ${remaining} seconds before watching another ad.`,
          cooldownSecondsRemaining: remaining,
        };
      }
    }

    // 4. Hourly Frequency Cap (10 ads / hour)
    const adsThisHour = await adRepository.countRecentRewardedAds(userId, 1);
    if (adsThisHour >= settings.max_hourly_rewarded_ads) {
      return {
        allowed: false,
        action: 'THROTTLE',
        reason: `Hourly limit reached (${settings.max_hourly_rewarded_ads} ads/hour). Please try again next hour.`,
      };
    }

    // 5. Daily Frequency Cap (60 ads / day, or 30 for new accounts < 7 days)
    const userAgeDays = (Date.now() - new Date(user.created_at).getTime()) / (1000 * 60 * 60 * 24);
    const dailyCap = userAgeDays < 7 ? settings.max_daily_ads_new_user : settings.max_daily_rewarded_ads;

    const adsToday = await adRepository.countRecentRewardedAds(userId, 24);
    if (adsToday >= dailyCap) {
      return {
        allowed: false,
        action: 'THROTTLE',
        reason: `Daily limit reached (${dailyCap} ads/day). Come back tomorrow for more rewards!`,
      };
    }

    return { allowed: true, action: 'ALLOW' };
  },
};
