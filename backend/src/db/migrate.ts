/**
 * @module db/migrate
 * @description Database migration runner utility for PostgreSQL.
 * 
 * Purpose:
 *   Tracks applied database migrations in a dedicated `schema_migrations` table
 *   and applies pending migrations sequentially or rolls them back safely.
 * 
 * Inputs:
 *   - Command-line argument: "up" (apply pending) or "down" (rollback latest)
 * 
 * Outputs:
 *   - Applies SQL files in `migrations/` inside transactions
 * 
 * Edge Cases Handled:
 *   - Automatically creates `schema_migrations` table if not present.
 *   - Wraps each migration file in an isolated transaction to prevent partial state on error.
 *   - CLI logs status and exits with error code 1 on failure.
 */

import fs from 'fs';
import path from 'path';
import { pool, withTransaction } from '../config/database';
import { logger } from '../config/logger';

async function initMigrationTable(): Promise<void> {
  await pool.query(`
    CREATE TABLE IF NOT EXISTS schema_migrations (
      id SERIAL PRIMARY KEY,
      version VARCHAR(255) NOT NULL UNIQUE,
      applied_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );
  `);
}

async function runUp(): Promise<void> {
  await initMigrationTable();
  const migrationsDir = path.join(__dirname, 'migrations');
  const files = fs.readdirSync(migrationsDir)
    .filter((f) => f.endsWith('.sql') && !f.endsWith('.down.sql'))
    .sort();

  const { rows: appliedRows } = await pool.query<{ version: string }>(
    'SELECT version FROM schema_migrations'
  );
  const applied = new Set(appliedRows.map((r) => r.version));

  for (const file of files) {
    if (!applied.has(file)) {
      logger.info({ migration: file }, 'Applying migration...');
      const filePath = path.join(migrationsDir, file);
      const sql = fs.readFileSync(filePath, 'utf-8');

      await withTransaction(async (client) => {
        await client.query(sql);
        await client.query('INSERT INTO schema_migrations (version) VALUES ($1)', [file]);
      });

      logger.info({ migration: file }, 'Migration applied successfully.');
    } else {
      logger.debug({ migration: file }, 'Migration already applied, skipping.');
    }
  }

  logger.info('Database migrations are up to date.');
}

async function runDown(): Promise<void> {
  await initMigrationTable();
  const { rows } = await pool.query<{ version: string }>(
    'SELECT version FROM schema_migrations ORDER BY id DESC LIMIT 1'
  );

  if (rows.length === 0) {
    logger.info('No migrations found to rollback.');
    return;
  }

  const latest = rows[0].version;
  const downFile = latest.replace('.sql', '.down.sql');
  const downFilePath = path.join(__dirname, 'migrations', downFile);

  if (!fs.existsSync(downFilePath)) {
    throw new Error(`Rollback file ${downFile} does not exist for migration ${latest}`);
  }

  logger.warn({ migration: latest, downFile }, 'Rolling back migration...');
  const sql = fs.readFileSync(downFilePath, 'utf-8');

  await withTransaction(async (client) => {
    await client.query(sql);
    await client.query('DELETE FROM schema_migrations WHERE version = $1', [latest]);
  });

  logger.info({ migration: latest }, 'Migration rolled back successfully.');
}

async function main(): Promise<void> {
  const direction = process.argv[2] || 'up';
  try {
    if (direction === 'up') {
      await runUp();
    } else if (direction === 'down') {
      await runDown();
    } else {
      logger.error(`Unknown migration command "${direction}". Use "up" or "down".`);
      process.exit(1);
    }
  } catch (err) {
    logger.error({ err }, 'Migration execution failed.');
    process.exit(1);
  } finally {
    await pool.end();
  }
}

if (require.main === module) {
  main();
}
