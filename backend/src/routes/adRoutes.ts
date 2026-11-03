/**
 * @module routes/adRoutes
 * @description Routing definition for ad sessions, S2S network callbacks, and impression logging.
 */

import { Router } from 'express';
import { adController } from '../controllers/adController';
import { requireAuth } from '../middlewares/authMiddleware';

const router = Router();

router.post('/session', requireAuth, adController.createSession);
router.get('/session/:id/status', requireAuth, adController.getSessionStatus);
router.post('/impression', adController.recordImpression);

// Server-to-server callbacks (Supports both GET and POST per network requirements)
router.get('/callback/:network', adController.handleCallback);
router.post('/callback/:network', adController.handleCallback);

export default router;
