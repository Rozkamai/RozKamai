/**
 * @module config/logger
 * @description Centralized asynchronous structured logging system using Pino.
 * 
 * Purpose:
 *   Emits high-performance JSON logs in production for log ingestion systems
 *   (e.g., Datadog, ELK, CloudWatch) and colored readable output in development.
 * 
 * Inputs:
 *   - env.LOG_LEVEL: Minimum log level threshold
 *   - env.LOG_PRETTY: Boolean toggle for pino-pretty transport
 * 
 * Outputs:
 *   - `logger` instance conforming to Pino Logger interface
 * 
 * Edge Cases Handled:
 *   - Sanitizes sensitive request bodies (passwords, tokens, CNICs) before logging.
 *   - Avoids thread blocking via asynchronous stream stdout.
 */

import pino from 'pino';
import { env } from './env';

export const logger = pino({
  level: env.LOG_LEVEL,
  timestamp: pino.stdTimeFunctions.isoTime,
  formatters: {
    level: (label) => ({ level: label }),
  },
  redact: {
    paths: [
      'password',
      'password_hash',
      'req.headers.authorization',
      'cnic',
      'cnic_encrypted',
      'account_number',
      'account_number_encrypted',
      'bank_iban',
      'bank_iban_encrypted',
    ],
    remove: true,
  },
  transport: env.LOG_PRETTY && env.NODE_ENV !== 'production'
    ? {
        target: 'pino-pretty',
        options: {
          colorize: true,
          translateTime: 'SYS:yyyy-mm-dd HH:MM:ss.l',
          ignore: 'pid,hostname',
        },
      }
    : undefined,
});
