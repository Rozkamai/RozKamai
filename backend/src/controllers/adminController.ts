/**
 * @module controllers/adminController
 * @description Admin endpoints for managing payouts, CSV export, fraud events, and dynamic settings.
 */

import { Response, NextFunction } from 'express';
import { AuthenticatedRequest } from '../middlewares/authMiddleware';
import { adminService } from '../services/adminService';
import { settingsRepository } from '../repositories/settingsRepository';

export const adminController = {
  async getPendingWithdrawals(req: AuthenticatedRequest, res: Response, next: NextFunction) {
    try {
      const limit = parseInt((req.query.limit as string) || '50', 10);
      const offset = parseInt((req.query.offset as string) || '0', 10);
      const list = await adminService.getPendingWithdrawals(limit, offset);
      res.status(200).json(list);
    } catch (err) {
      next(err);
    }
  },

  async exportWithdrawalsCsv(_req: AuthenticatedRequest, res: Response, next: NextFunction) {
    try {
      const csv = await adminService.exportWithdrawalsCsv();
      res.setHeader('Content-Type', 'text/csv');
      res.setHeader('Content-Disposition', `attachment; filename=withdrawals_${Date.now()}.csv`);
      res.status(200).send(csv);
    } catch (err) {
      next(err);
    }
  },

  async updateWithdrawalStatus(req: AuthenticatedRequest, res: Response, next: NextFunction) {
    try {
      const id = req.params.id;
      const { status, paidReference, rejectedReason, adminNotes } = req.body;

      if (status === 'PAID') {
        if (!paidReference) {
          res.status(400).json({ error: 'paidReference is required to mark withdrawal as PAID' });
          return;
        }
        const updated = await adminService.markWithdrawalPaid(id, paidReference, adminNotes);
        res.status(200).json(updated);
      } else if (status === 'REJECTED') {
        if (!rejectedReason) {
          res.status(400).json({ error: 'rejectedReason is required to reject a withdrawal' });
          return;
        }
        const updated = await adminService.rejectWithdrawal(id, rejectedReason, adminNotes);
        res.status(200).json(updated);
      } else {
        res.status(400).json({ error: "status must be 'PAID' or 'REJECTED'" });
      }
    } catch (err) {
      next(err);
    }
  },

  async getRevenueStats(_req: AuthenticatedRequest, res: Response, next: NextFunction) {
    try {
      const stats = await adminService.getPlatformRevenueStats();
      res.status(200).json(stats);
    } catch (err) {
      next(err);
    }
  },

  async getFraudEvents(req: AuthenticatedRequest, res: Response, next: NextFunction) {
    try {
      const limit = parseInt((req.query.limit as string) || '50', 10);
      const offset = parseInt((req.query.offset as string) || '0', 10);
      const events = await adminService.getFraudEvents(limit, offset);
      res.status(200).json(events);
    } catch (err) {
      next(err);
    }
  },

  async blockUser(req: AuthenticatedRequest, res: Response, next: NextFunction) {
    try {
      const userId = req.params.id;
      const { isBlocked, reason } = req.body;
      await adminService.setBlockUser(userId, isBlocked === true, reason);
      res.status(200).json({ message: `User status updated to ${isBlocked ? 'BLOCKED' : 'ACTIVE'}` });
    } catch (err) {
      next(err);
    }
  },

  async getSettings(_req: AuthenticatedRequest, res: Response, next: NextFunction) {
    try {
      const settings = await settingsRepository.getAll();
      res.status(200).json(settings);
    } catch (err) {
      next(err);
    }
  },

  async updateSettings(req: AuthenticatedRequest, res: Response, next: NextFunction) {
    try {
      const { key, value } = req.body;
      await adminService.updateSettings(key, value, req.user?.userId);
      res.status(200).json({ message: `Setting ${key} updated successfully` });
    } catch (err) {
      next(err);
    }
  },
};
