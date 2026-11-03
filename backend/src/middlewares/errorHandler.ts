/**
 * @module middlewares/errorHandler
 * @description Centralized Express error handler.
 */

import { Request, Response, NextFunction } from 'express';
import { logger } from '../config/logger';

export function errorHandler(err: any, req: Request, res: Response, _next: NextFunction): void {
  const status = err.status || 400;
  logger.error({ err, path: req.path, method: req.method }, 'Request error occurred');
  res.status(status).json({
    error: err.message || 'An unexpected server error occurred.',
  });
}
