/**
 * @module middlewares/rateLimiter
 * @description IP and route-based rate limiters to prevent brute-force attacks.
 */

import rateLimit from 'express-rate-limit';

export const authRateLimiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: 10, // 10 attempts per 15 minutes
  message: { error: 'Too many authentication attempts. Please try again in 15 minutes.' },
  standardHeaders: true,
  legacyHeaders: false,
});

export const apiRateLimiter = rateLimit({
  windowMs: 60 * 1000, // 1 minute
  max: 120, // 120 requests per minute
  message: { error: 'Request rate limit exceeded. Please throttle your client requests.' },
  standardHeaders: true,
  legacyHeaders: false,
});
