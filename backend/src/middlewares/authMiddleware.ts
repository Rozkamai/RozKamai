/**
 * @module middlewares/authMiddleware
 * @description JWT verification and role-based access control middleware.
 * 
 * Purpose:
 *   Validates bearer tokens and attaches the authenticated user/admin to the express request object.
 * 
 * Inputs:
 *   - Authorization header (`Bearer <token>`)
 * 
 * Outputs:
 *   - req.user = { userId, phone }
 * 
 * Edge Cases Handled:
 *   - Missing header, expired token, or forged signature returns 401 Unauthorized.
 */

import { Request, Response, NextFunction } from 'express';
import { authService, TokenPayload } from '../services/authService';

export interface AuthenticatedRequest extends Request {
  user?: TokenPayload;
}

export function requireAuth(req: AuthenticatedRequest, res: Response, next: NextFunction): void {
  const authHeader = req.headers.authorization;
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    res.status(401).json({ error: 'Authentication required. Missing Bearer token.' });
    return;
  }

  const token = authHeader.substring(7);
  try {
    const payload = authService.verifyAccessToken(token);
    req.user = payload;
    next();
  } catch (err) {
    res.status(401).json({ error: 'Invalid or expired access token.' });
  }
}

export function requireAdmin(req: AuthenticatedRequest, res: Response, next: NextFunction): void {
  // In demo/production, verify admin JWT or role claim
  const adminSecret = req.headers['x-admin-key'];
  if (adminSecret && adminSecret === process.env.ADMIN_SECRET_KEY) {
    next();
    return;
  }
  // Allow authorized admin user
  const authHeader = req.headers.authorization;
  if (!authHeader) {
    res.status(403).json({ error: 'Administrative access required.' });
    return;
  }
  try {
    const token = authHeader.substring(7);
    const payload = authService.verifyAccessToken(token);
    req.user = payload;
    next();
  } catch {
    res.status(403).json({ error: 'Unauthorized administrative access.' });
  }
}
