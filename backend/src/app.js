require('dotenv').config();
const express = require('express');
const helmet = require('helmet');
const morgan = require('morgan');
const rateLimit = require('express-rate-limit');

const config = require('./config');
const logger = require('./lib/logger');
const errorHandler = require('./middleware/errorHandler');
const healthRoutes = require('./routes/healthRoutes');
const passRoutes = require('./routes/passRoutes');
const transactionRoutes = require('./routes/transactionRoutes');

const app = express();

// ── Security headers ──────────────────────────────────────────────────────────
app.use(helmet());
app.disable('x-powered-by');

// ── Request logging ───────────────────────────────────────────────────────────
app.use(
  morgan('combined', {
    stream: { write: (msg) => logger.http(msg.trim()) },
    skip: (req) => req.path === '/health',
  })
);

// ── Body parsing ──────────────────────────────────────────────────────────────
app.use(express.json({ limit: '64kb' }));

// ── Rate limiting ─────────────────────────────────────────────────────────────
// Pass issuance: 10 per minute per IP (prevents wallet stuffing).
app.use(
  '/api/v1/passes',
  rateLimit({ windowMs: 60_000, max: 10, standardHeaders: true, legacyHeaders: false })
);

// Transaction deduct: 120 per minute per IP (covers high-throughput POS).
app.use(
  '/api/v1/transactions',
  rateLimit({ windowMs: 60_000, max: 120, standardHeaders: true, legacyHeaders: false })
);

// ── Routes ────────────────────────────────────────────────────────────────────
app.use(healthRoutes);
app.use(passRoutes);
app.use(transactionRoutes);

// 404 catch-all
app.use((req, res) => {
  res.status(404).json({ error: { code: 'NOT_FOUND', message: 'Route not found' } });
});

// Central error handler — must be last
app.use(errorHandler);

// ── Start ─────────────────────────────────────────────────────────────────────
if (require.main === module) {
  app.listen(config.app.port, () => {
    logger.info(`${config.app.name} v${config.app.version} listening`, {
      port: config.app.port,
      env: config.app.env,
    });
  });
}

module.exports = app;
