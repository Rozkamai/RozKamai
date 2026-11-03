/**
 * @module tests/rewardEngine.test
 * @description Unit tests verifying 70/30 split math, rounding rules, CNIC masking, and IBAN validation.
 */

import { computeSplit } from '../src/services/rewardEngine';
import { maskCnic, computeBlindIndex } from '../src/utils/crypto';
import { validatePakistaniIban, validateMobileWalletNumber } from '../src/services/withdrawalService';
import { normalizePakistaniPhone } from '../src/services/authService';

describe('70/30 Reward Split Calculations', () => {
  it('correctly splits 100 gross coins into 70 user and 30 platform coins', () => {
    const result = computeSplit(100, 70, 30);
    expect(result.userShareCoins).toBe(70);
    expect(result.platformShareCoins).toBe(30);
    expect(result.userShareCoins + result.platformShareCoins).toBe(100);
  });

  it('allocates rounding remainders to platform share so total always reconciles', () => {
    // 99 * 0.70 = 69.3 -> floor is 69. Platform receives 99 - 69 = 30.
    const result = computeSplit(99, 70, 30);
    expect(result.userShareCoins).toBe(69);
    expect(result.platformShareCoins).toBe(30);
    expect(result.userShareCoins + result.platformShareCoins).toBe(99);
  });

  it('handles 1 gross coin (user gets 0, platform gets 1)', () => {
    const result = computeSplit(1, 70, 30);
    expect(result.userShareCoins).toBe(0);
    expect(result.platformShareCoins).toBe(1);
    expect(result.userShareCoins + result.platformShareCoins).toBe(1);
  });

  it('handles 0 gross coins', () => {
    const result = computeSplit(0, 70, 30);
    expect(result.userShareCoins).toBe(0);
    expect(result.platformShareCoins).toBe(0);
  });

  it('throws on negative coins', () => {
    expect(() => computeSplit(-10)).toThrow();
  });
});

describe('Pakistani Identity & Validation Rules', () => {
  it('masks 13-digit Pakistani CNIC correctly with asterisks', () => {
    expect(maskCnic('4210112345671')).toBe('42101-*******-1');
    expect(maskCnic('3520298765432')).toBe('35202-*******-2');
  });

  it('generates consistent blind index hash for duplicate detection', () => {
    const hash1 = computeBlindIndex('42101-1234567-1');
    const hash2 = computeBlindIndex('4210112345671');
    expect(hash1).toBe(hash2);
  });

  it('normalizes Pakistani phone numbers to 03XXXXXXXXX format', () => {
    expect(normalizePakistaniPhone('03001234567')).toBe('03001234567');
    expect(normalizePakistaniPhone('+923001234567')).toBe('03001234567');
    expect(normalizePakistaniPhone('923001234567')).toBe('03001234567');
    expect(normalizePakistaniPhone('0300-1234567')).toBe('03001234567');
  });

  it('validates Pakistani IBAN format', () => {
    const validIban = 'PK36SCBL0000001123456701';
    expect(validatePakistaniIban(validIban)).toBe(validIban);
    expect(() => validatePakistaniIban('INVALID_IBAN')).toThrow('Invalid Pakistani IBAN format');
  });

  it('validates mobile wallet numbers for JazzCash and EasyPaisa', () => {
    expect(validateMobileWalletNumber('03121234567')).toBe('03121234567');
    expect(() => validateMobileWalletNumber('04231234567')).toThrow('Invalid mobile wallet number');
  });
});
