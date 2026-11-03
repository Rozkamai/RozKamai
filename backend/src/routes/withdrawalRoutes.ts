/**
 * @module routes/withdrawalRoutes
 * @description Routing definition for user withdrawal requests.
 */

import { Router } from 'express';
import { withdrawalController } from '../controllers/withdrawalController';
import { requireAuth } from '../middlewares/authMiddleware';

const router = Router();

router.post('/', requireAuth, withdrawalController.createWithdrawal);
router.get('/', requireAuth, withdrawalController.listWithdrawals);

export default router;
