/**
 * @module routes/authRoutes
 * @description Routing definition for authentication endpoints.
 */

import { Router } from 'express';
import { authController } from '../controllers/authController';
import { authRateLimiter } from '../middlewares/rateLimiter';

const router = Router();

router.post('/register', authRateLimiter, authController.register);
router.post('/login', authRateLimiter, authController.login);
router.post('/refresh', authController.refresh);
router.post('/logout', authController.logout);

export default router;
