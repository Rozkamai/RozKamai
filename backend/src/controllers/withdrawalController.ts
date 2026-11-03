/**
 * @module controllers/withdrawalController
 * @description User withdrawal request creation and status tracking.
 */

import { Response, NextFunction } from 'express';
import { z } from 'zod';
import { AuthenticatedRequest } from '../middlewares/authMiddleware';
import { withdrawalService } from '../services/withdrawalService';

const withdrawalRequestSchema = z.object({
  amountPkr: z.number().int().min(500, 'Minimum withdrawal is PKR 500'),
  paymentMethod: z.enum(['JAZZ_CASH', 'EASY_PAISA', 'BANK_TRANSFER']),
  accountNumber: z.string().min(1),
  accountTitle: z.string().min(3),
  bankName: z.string().optional(),
  bankIban: z.string().optional(),
});

export const withdrawalController = {
  async createWithdrawal(req: AuthenticatedRequest, res: Response, next: NextFunction) {
    try {
      const userId = req.user!.userId;
      const body = withdrawalRequestSchema.parse(req.body);

      const withdrawal = await withdrawalService.requestWithdrawal(
        userId,
        body.amountPkr,
        body.paymentMethod,
        body.accountNumber,
        body.accountTitle,
        body.bankName,
        body.bankIban
      );

      res.status(201).json({
        id: withdrawal.id,
        amountPkr: parseFloat(withdrawal.amount_pkr_display),
        amountCoins: parseInt(withdrawal.amount_coins, 10),
        paymentMethod: withdrawal.payment_method,
        status: withdrawal.status,
        accountTitle: withdrawal.account_title,
        createdAt: withdrawal.created_at,
      });
    } catch (err) {
      next(err);
    }
  },

  async listWithdrawals(req: AuthenticatedRequest, res: Response, next: NextFunction) {
    try {
      const userId = req.user!.userId;
      const rows = await withdrawalService.listUserWithdrawals(userId);

      const formatted = rows.map((r) => ({
        id: r.id,
        amountPkr: parseFloat(r.amount_pkr_display),
        amountCoins: parseInt(r.amount_coins, 10),
        paymentMethod: r.payment_method,
        status: r.status,
        accountTitle: r.account_title,
        bankName: r.bank_name,
        paidReference: r.paid_reference,
        paidAt: r.paid_at,
        rejectedReason: r.rejected_reason,
        createdAt: r.created_at,
      }));

      res.status(200).json(formatted);
    } catch (err) {
      next(err);
    }
  },
};
