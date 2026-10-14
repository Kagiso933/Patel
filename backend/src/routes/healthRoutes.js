const express = require('express');
const db = require('../db/database');
const config = require('../config');

const router = express.Router();

/**
 * GET /health
 * Liveness + readiness probe — used by Docker, K8s, and uptime monitors.
 */
router.get('/health', (req, res) => {
  let dbStatus = 'ok';
  try {
    db.prepare('SELECT 1').get();
  } catch {
    dbStatus = 'error';
  }

  const status = dbStatus === 'ok' ? 'healthy' : 'degraded';
  return res.status(status === 'healthy' ? 200 : 503).json({
    status,
    version: config.app.version,
    timestamp: new Date().toISOString(),
    checks: { database: dbStatus },
  });
});

module.exports = router;
