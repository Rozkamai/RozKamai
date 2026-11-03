/**
 * @module server
 * @description Central Express application bootstrap and HTTP server listener.
 * 
 * Purpose:
 *   Initializes Helmet, CORS, rate limiters, API routing, health checks,
 *   and graceful shutdown handlers.
 * 
 * Inputs:
 *   - Configuration from `env`
 * 
 * Outputs:
 *   - Active HTTP listener on specified PORT
 */

import express from 'express';
import helmet from 'helmet';
import cors from 'cors';
import { env } from './config/env';
import { logger } from './config/logger';
import { pool } from './config/database';
import { apiRateLimiter } from './middlewares/rateLimiter';
import { errorHandler } from './middlewares/errorHandler';

import authRoutes from './routes/authRoutes';
import userRoutes from './routes/userRoutes';
import walletRoutes from './routes/walletRoutes';
import adRoutes from './routes/adRoutes';
import withdrawalRoutes from './routes/withdrawalRoutes';
import adminRoutes from './routes/adminRoutes';

const app = express();

// 1. Security & Core Middleware
app.use(helmet());
app.use(cors({ origin: env.CORS_ORIGIN, credentials: true }));
app.use(express.json({ limit: '1mb' }));
app.use(express.urlencoded({ extended: true, limit: '1mb' }));

// 2. Request Logging Middleware
app.use((req, res, next) => {
  const start = Date.now();
  res.on('finish', () => {
    const duration = Date.now() - start;
    logger.info(
      { method: req.method, path: req.originalUrl, status: res.statusCode, duration, ip: req.ip },
      'HTTP Request completed'
    );
  });
  next();
});

// 3. Online-Only Heartbeat & Health Check
app.get('/health', async (_req, res) => {
  try {
    const dbRes = await pool.query('SELECT 1 as healthy');
    const isDbConnected = dbRes.rows.length > 0;
    res.status(200).json({
      status: 'UP',
      app: env.APP_NAME,
      timestamp: new Date().toISOString(),
      database: isDbConnected ? 'CONNECTED' : 'DEGRADED',
      environment: env.NODE_ENV,
    });
  } catch (error) {
    res.status(503).json({
      status: 'DOWN',
      error: 'Database connection failed',
    });
  }
});

// 4. Mount API Routes with Rate Limiter
const prefix = env.API_PREFIX;
app.use(apiRateLimiter);

app.use(`${prefix}/auth`, authRoutes);
app.use(`${prefix}`, userRoutes);
app.use(`${prefix}/wallet`, walletRoutes);
app.use(`${prefix}/ads`, adRoutes);
app.use(`${prefix}/withdrawals`, withdrawalRoutes);
app.use(`${prefix}/admin`, adminRoutes);

// 5. 404 & Centralized Error Handler
app.use((_req, res) => {
  res.status(404).json({ error: 'Endpoint not found' });
});

app.use(errorHandler);

// 6. Server Initialization & Graceful Shutdown
const server = app.listen(env.PORT, () => {
  logger.info(`🚀 RozKamai API server running on port ${env.PORT} (${env.NODE_ENV})`);
  logger.info(`🔗 API Base: http://localhost:${env.PORT}${env.API_PREFIX}`);
});

async function gracefulShutdown(signal: string) {
  logger.info(`Received ${signal}. Starting graceful shutdown...`);
  server.close(async () => {
    logger.info('HTTP server closed.');
    await pool.end();
    logger.info('Database connection pool drained.');
    process.exit(0);
  });

  setTimeout(() => {
    logger.error('Forcefully terminating process due to shutdown timeout.');
    process.exit(1);
  }, 10000);
}

process.on('SIGTERM', () => gracefulShutdown('SIGTERM'));
process.on('SIGINT', () => gracefulShutdown('SIGINT'));

export default app;
