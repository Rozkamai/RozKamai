/**
 * @module controllers/walletController
 * @description Wallet balances and ledger transaction history endpoints.
 */

import { Response, NextFunction } from 'express';
import { AuthenticatedRequest } from '../middlewares/authMiddleware';
import { walletRepository } from '../repositories/walletRepository';
import { ledgerRepository } from '../repositories/ledgerRepository';

export const walletController = {
  async getWallet(req: AuthenticatedRequest, res: Response, next: NextFunction) {
    try {
      const userId = req.user!.userId;
      const wallet = await walletRepository.findByUserId(userId);
      if (!wallet) {
        res.status(404).json({ error: 'Wallet not found' });
        return;
      }

      const availableCoins = parseInt(wallet.available_balance_coins, 10);
      const pendingCoins = parseInt(wallet.pending_withdrawal_coins, 10);
      const totalEarnedCoins = parseInt(wallet.total_earned_coins, 10);

      // Reconcile with ledger on request
      const recon = await walletRepository.reconcileWithLedger(userId);

      res.status(200).json({
        availableCoins,
        pendingCoins,
        totalEarnedCoins,
        availablePkr: (availableCoins / 100).toFixed(2),
        pendingPkr: (pendingCoins / 100).toFixed(2),
        totalEarnedPkr: (totalEarnedCoins / 100).toFixed(2),
        isReconciled: recon.isReconciled,
      });
    } catch (err) {
      next(err);
    }
  },

  async getTransactions(req: AuthenticatedRequest, res: Response, next: NextFunction) {
    try {
      const userId = req.user!.userId;
      const limit = parseInt((req.query.limit as string) || '50', 10);
      const offset = parseInt((req.query.offset as string) || '0', 10);

      const result = await ledgerRepository.listByUser(userId, limit, offset);
      res.status(200).json(result);
    } catch (err) {
      next(err);
    }
  },
};
