/**
 * @module routes/walletRoutes
 * @description Routing definition for wallet balance and ledger audit entries.
 */

import { Router } from 'express';
import { walletController } from '../controllers/walletController';
import { requireAuth } from '../middlewares/authMiddleware';

const router = Router();

router.get('/', requireAuth, walletController.getWallet);
router.get('/transactions', requireAuth, walletController.getTransactions);

export default router;
