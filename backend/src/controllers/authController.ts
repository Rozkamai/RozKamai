/**
 * @module controllers/authController
 * @description Controller for Pakistani phone authentication, OTP validation, and JWT refresh.
 */

import { Request, Response, NextFunction } from 'express';
import { z } from 'zod';
import { authService } from '../services/authService';

const registerSchema = z.object({
  phone: z.string().min(10).max(15),
  password: z.string().min(6, 'Password must be at least 6 characters'),
  fullName: z.string().min(2, 'Full name is required'),
  isAgeVerified: z.boolean().refine((val) => val === true, 'You must be 18+ to register'),
  deviceFingerprint: z.string().min(8),
  platform: z.string().default('android'),
  osVersion: z.string().optional().nullable(),
  appVersion: z.string().optional().nullable(),
  isRooted: z.boolean().default(false),
  playIntegrityStatus: z.string().default('UNEVALUATED'),
});

const loginSchema = z.object({
  phone: z.string().min(10).max(15),
  password: z.string().min(1),
  deviceFingerprint: z.string().min(8),
});

export const authController = {
  async register(req: Request, res: Response, next: NextFunction) {
    try {
      const body = registerSchema.parse(req.body);
      const ip = req.ip || req.socket.remoteAddress || '127.0.0.1';

      const result = await authService.register(
        body.phone,
        body.password,
        body.fullName,
        body.isAgeVerified,
        {
          fingerprint: body.deviceFingerprint,
          platform: body.platform,
          osVersion: body.osVersion || null,
          appVersion: body.appVersion || null,
          isRooted: body.isRooted,
          playIntegrityStatus: body.playIntegrityStatus,
          ip,
        }
      );

      res.status(201).json(result);
    } catch (err) {
      next(err);
    }
  },

  async login(req: Request, res: Response, next: NextFunction) {
    try {
      const body = loginSchema.parse(req.body);
      const ip = req.ip || req.socket.remoteAddress || '127.0.0.1';

      const result = await authService.login(body.phone, body.password, {
        fingerprint: body.deviceFingerprint,
        ip,
      });

      res.status(200).json(result);
    } catch (err) {
      next(err);
    }
  },

  async refresh(req: Request, res: Response, next: NextFunction) {
    try {
      const { refreshToken } = req.body;
      if (!refreshToken) {
        res.status(400).json({ error: 'refreshToken is required' });
        return;
      }
      const tokens = authService.refreshToken(refreshToken);
      res.status(200).json(tokens);
    } catch (err) {
      next(err);
    }
  },

  async logout(_req: Request, res: Response) {
    // In a stateless JWT system, client drops tokens; can optionally blacklist refresh tokens
    res.status(200).json({ message: 'Logged out successfully' });
  },
};
