/**
 * @module config/database
 * @description PostgreSQL connection pool and transactional execution utilities.
 * 
 * Purpose:
 *   Provides managed connection pooling, prepared statement execution,
 *   and transactional execution wrappers with automatic commit/rollback and row locking.
 * 
 * Inputs:
 *   - Database credentials from `env`
 * 
 * Outputs:
 *   - `pool`: pg.Pool instance
 *   - `query(sql, params)`: Helper for single queries
 *   - `withTransaction(callback)`: Higher-order function providing an isolated client with BEGIN/COMMIT/ROLLBACK
 * 
 * Edge Cases Handled:
 *   - Unexpected connection disconnects are logged and handled via pool error listeners.
 *   - Deadlock / transaction error triggers automatic ROLLBACK before re-throwing.
 *   - Client release is guaranteed via finally blocks.
 */

import { Pool, PoolClient, QueryResult, QueryResultRow } from 'pg';
import { env } from './env';
import { logger } from './logger';

export const pool = new Pool({
  host: env.DB_HOST,
  port: env.DB_PORT,
  database: env.DB_NAME,
  user: env.DB_USER,
  password: env.DB_PASSWORD,
  min: env.DB_POOL_MIN,
  max: env.DB_POOL_MAX,
  idleTimeoutMillis: 30000,
  connectionTimeoutMillis: 5000,
  ssl: env.DB_SSL ? { rejectUnauthorized: false } : false,
});

pool.on('error', (err: Error) => {
  logger.error({ err }, 'Unexpected error on idle PostgreSQL client');
});

/**
 * Execute a query with parameterized arguments.
 */
export async function query<T extends QueryResultRow = any>(
  text: string,
  params?: any[]
): Promise<QueryResult<T>> {
  const start = Date.now();
  try {
    const res = await pool.query<T>(text, params);
    const duration = Date.now() - start;
    logger.debug({ text, duration, rows: res.rowCount }, 'Executed PostgreSQL query');
    return res;
  } catch (error) {
    logger.error({ text, params, error }, 'PostgreSQL query failed');
    throw error;
  }
}

/**
 * Execute work inside an isolated database transaction.
 * Automatically issues BEGIN, commits on success, or issues ROLLBACK on error.
 */
export async function withTransaction<T>(
  callback: (client: PoolClient) => Promise<T>
): Promise<T> {
  const client = await pool.connect();
  try {
    await client.query('BEGIN');
    const result = await callback(client);
    await client.query('COMMIT');
    return result;
  } catch (error) {
    await client.query('ROLLBACK');
    logger.error({ error }, 'Transaction failed and was rolled back');
    throw error;
  } finally {
    client.release();
  }
}
