/**
 * @module controllers/userController
 * @description User profile, KYC status, and data management endpoints (GET /me, DELETE /me, POST /kyc/submit).
 */

import { Response, NextFunction } from 'express';
import { AuthenticatedRequest } from '../middlewares/authMiddleware';
import { userRepository } from '../repositories/userRepository';
import { walletRepository } from '../repositories/walletRepository';
import { kycService } from '../services/kycService';

export const userController = {
  async getMe(req: AuthenticatedRequest, res: Response, next: NextFunction) {
    try {
      const userId = req.user!.userId;
      const [user, wallet, kyc] = await Promise.all([
        userRepository.findById(userId),
        walletRepository.findByUserId(userId),
        kycService.getKycStatus(userId),
      ]);

      if (!user) {
        res.status(404).json({ error: 'User not found' });
        return;
      }

      res.status(200).json({
        id: user.id,
        phone: user.phone,
        fullName: user.full_name,
        isAgeVerified: user.is_age_verified,
        riskScore: user.risk_score,
        firstWithdrawalEligibleAt: user.first_withdrawal_eligible_at,
        wallet: wallet
          ? {
              availableCoins: parseInt(wallet.available_balance_coins, 10),
              pendingCoins: parseInt(wallet.pending_withdrawal_coins, 10),
              totalEarnedCoins: parseInt(wallet.total_earned_coins, 10),
              availablePkr: (parseInt(wallet.available_balance_coins, 10) / 100).toFixed(2),
            }
          : null,
        kyc,
      });
    } catch (err) {
      next(err);
    }
  },

  async submitKyc(req: AuthenticatedRequest, res: Response, next: NextFunction) {
    try {
      const userId = req.user!.userId;
      const { cnic, otp } = req.body;
      const ip = req.ip || req.socket.remoteAddress || '127.0.0.1';

      if (!cnic) {
        res.status(400).json({ error: 'CNIC number is required' });
        return;
      }

      const result = await kycService.submitKyc(userId, cnic, otp || '123456', ip);
      res.status(200).json(result);
    } catch (err) {
      next(err);
    }
  },

  async deleteAccount(req: AuthenticatedRequest, res: Response, next: NextFunction) {
    try {
      const userId = req.user!.userId;
      await userRepository.softDelete(userId);
      res.status(200).json({ message: 'Account deleted successfully per data minimization rules.' });
    } catch (err) {
      next(err);
    }
  },
};
