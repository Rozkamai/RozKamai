/**
 * @module routes/userRoutes
 * @description Routing definition for user profile, KYC, and deletion.
 */

import { Router } from 'express';
import { userController } from '../controllers/userController';
import { requireAuth } from '../middlewares/authMiddleware';

const router = Router();

router.get('/me', requireAuth, userController.getMe);
router.post('/kyc/submit', requireAuth, userController.submitKyc);
router.delete('/me', requireAuth, userController.deleteAccount);

export default router;
