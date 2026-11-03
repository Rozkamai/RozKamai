/**
 * @module controllers/adController
 * @description Ad session orchestration, S2S callback verification, and impression telemetry.
 */

import { Request, Response, NextFunction } from 'express';
import { z } from 'zod';
import { AuthenticatedRequest } from '../middlewares/authMiddleware';
import { rewardEngine } from '../services/rewardEngine';
import { adRepository } from '../repositories/adRepository';

const sessionRequestSchema = z.object({
  deviceFingerprint: z.string().min(8),
  network: z.enum(['unity', 'applovin']).default('unity'),
  adUnitId: z.string().default('rewarded_video_pkr'),
});

const impressionSchema = z.object({
  deviceFingerprint: z.string().optional(),
  adType: z.enum(['BANNER', 'INTERSTITIAL', 'REWARDED']),
  network: z.string().default('unity'),
  placementName: z.string().default('default_placement'),
  clientTimestamp: z.string().optional(),
});

export const adController = {
  async createSession(req: AuthenticatedRequest, res: Response, next: NextFunction) {
    try {
      const userId = req.user!.userId;
      const body = sessionRequestSchema.parse(req.body);
      const ip = req.ip || req.socket.remoteAddress || '127.0.0.1';

      const session = await rewardEngine.initSession(
        userId,
        body.deviceFingerprint,
        body.network,
        body.adUnitId,
        ip
      );

      res.status(201).json(session);
    } catch (err) {
      next(err);
    }
  },

  async getSessionStatus(req: AuthenticatedRequest, res: Response, next: NextFunction) {
    try {
      const sessionId = req.params.id;
      const session = await rewardEngine.getSessionStatus(sessionId);

      if (!session) {
        res.status(404).json({ error: 'Session not found' });
        return;
      }

      res.status(200).json({
        sessionId: session.id,
        status: session.status,
        expiresAt: session.expires_at,
        isCompleted: session.status === 'COMPLETED',
      });
    } catch (err) {
      next(err);
    }
  },

  async handleCallback(req: Request, res: Response, next: NextFunction) {
    try {
      const network = req.params.network as 'unity' | 'applovin';
      let transactionId = '';
      let nonce = '';
      let signature = '';

      if (network === 'unity') {
        // Unity S2S GET or POST callback
        const q = req.query as Record<string, string>;
        transactionId = q.sid || q.transactionId || (req.body && req.body.sid);
        nonce = q.customData || q.user || (req.body && req.body.customData);
        signature = (req.headers['x-unity-signature'] as string) || q.hmac || '';
      } else if (network === 'applovin') {
        // AppLovin MAX Postback
        transactionId = req.body.event_id || req.body.transaction_id || req.query.event_id;
        nonce = req.body.user_id || req.body.custom_data || req.query.user_id;
        signature = (req.headers['x-max-signature'] as string) || '';
      }

      if (!transactionId || !nonce) {
        res.status(400).json({ error: 'Missing transactionId or session nonce' });
        return;
      }

      const result = await rewardEngine.processCallback(
        network,
        transactionId,
        nonce,
        req.body,
        signature,
        req.query as Record<string, string>
      );

      // S2S networks expect 200 OK with specific acknowledgment strings
      res.status(200).send(result.success ? '1' : '0');
    } catch (err) {
      next(err);
    }
  },

  async recordImpression(req: AuthenticatedRequest, res: Response, next: NextFunction) {
    try {
      const userId = req.user ? req.user.userId : null;
      const body = impressionSchema.parse(req.body);

      await adRepository.recordImpression(
        userId,
        null,
        body.adType,
        body.network,
        body.placementName,
        body.clientTimestamp ? new Date(body.clientTimestamp) : new Date()
      );

      res.status(201).json({ status: 'recorded' });
    } catch (err) {
      next(err);
    }
  },
};
