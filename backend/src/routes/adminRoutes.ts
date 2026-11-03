/**
 * @module routes/adminRoutes
 * @description Routing definition for administrative dashboard and operations.
 */

import { Router } from 'express';
import { adminController } from '../controllers/adminController';
import { requireAdmin } from '../middlewares/authMiddleware';

const router = Router();

router.use(requireAdmin);

router.get('/withdrawals', adminController.getPendingWithdrawals);
router.get('/withdrawals/export.csv', adminController.exportWithdrawalsCsv);
router.patch('/withdrawals/:id', adminController.updateWithdrawalStatus);
router.get('/revenue', adminController.getRevenueStats);
router.get('/fraud-events', adminController.getFraudEvents);
router.patch('/users/:id/block', adminController.blockUser);
router.get('/settings', adminController.getSettings);
router.put('/settings', adminController.updateSettings);

export default router;
