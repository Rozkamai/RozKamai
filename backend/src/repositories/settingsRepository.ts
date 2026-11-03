/**
 * @module repositories/settingsRepository
 * @description SQL data access layer for dynamic platform economics & rules.
 * 
 * Purpose:
 *   Retrieves and caches dynamic settings (split percentages, conversion rates, fraud caps).
 * 
 * Inputs:
 *   - Setting keys
 * 
 * Outputs:
 *   - Parsed JSON setting values with fallbacks
 * 
 * Edge Cases Handled:
 *   - In-memory caching with TTL to avoid DB round-trips on every ad callback.
 */

import { query } from '../config/database';

export interface PlatformSettings {
  reward_gross_coins_per_ad: number;
  user_share_percentage: number;
  platform_share_percentage: number;
  coins_per_pkr: number;
  min_withdrawal_pkr: number;
  max_daily_rewarded_ads: number;
  max_hourly_rewarded_ads: number;
  min_ad_interval_seconds: number;
  new_user_cooldown_hours: number;
  max_daily_ads_new_user: number;
}

const DEFAULT_SETTINGS: PlatformSettings = {
  reward_gross_coins_per_ad: 100,
  user_share_percentage: 70,
  platform_share_percentage: 30,
  coins_per_pkr: 100,
  min_withdrawal_pkr: 500,
  max_daily_rewarded_ads: 60,
  max_hourly_rewarded_ads: 10,
  min_ad_interval_seconds: 30,
  new_user_cooldown_hours: 48,
  max_daily_ads_new_user: 30,
};

let cache: PlatformSettings | null = null;
let cacheTime = 0;
const CACHE_TTL_MS = 60000; // 1 minute

export const settingsRepository = {
  async getAll(): Promise<PlatformSettings> {
    const now = Date.now();
    if (cache && now - cacheTime < CACHE_TTL_MS) {
      return cache;
    }

    const res = await query<{ key: string; value: any }>('SELECT key, value FROM settings');
    const settings: any = { ...DEFAULT_SETTINGS };

    for (const row of res.rows) {
      if (row.key in settings) {
        settings[row.key] = typeof row.value === 'string' ? JSON.parse(row.value) : row.value;
      }
    }

    cache = settings as PlatformSettings;
    cacheTime = now;
    return cache;
  },

  async update(key: keyof PlatformSettings, value: number, adminId?: string): Promise<void> {
    await query(
      `INSERT INTO settings (key, value, description, updated_by, updated_at)
       VALUES ($1, $2, 'Dynamic setting', $3, NOW())
       ON CONFLICT (key) DO UPDATE
       SET value = EXCLUDED.value, updated_by = EXCLUDED.updated_by, updated_at = NOW()`,
      [key, JSON.stringify(value), adminId || null]
    );
    cache = null; // Invalidate cache
  },
};
