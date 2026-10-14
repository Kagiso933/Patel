const express = require('express');
const { debit, getWallet, getTransactionHistory } = require('../db/walletRepository');
const { requireFields } = require('../middleware/validateRequest');
const { requireMerchantKey } = require('../middleware/auth');
const { ValidationError } = require('../lib/errors');
const logger = require('../lib/logger');
const { v4: uuidv4 } = require('uuid');

const router = express.Router();

/**
 * POST /api/v1/transactions/deduct
 * Headers: X-Api-Key (merchant key)
 * body: { serialNumber, amount, merchantId? }
 *
 * Atomically debits the pass balance. Protected by merchant API key auth.
 * The SQLite transaction prevents double-spend under concurrent taps.
 */
router.post(
  '/api/v1/transactions/deduct',
  requireMerchantKey,
  requireFields('serialNumber', 'amount'),
  (req, res, next) => {
    const { serialNumber, amount, merchantId } = req.body;

    if (typeof amount !== 'number' || amount <= 0) {
      return next(new ValidationError('amount must be a positive number'));
    }

    try {
      const reference = `TAP-${uuidv4()}`;
      const { newBalance } = debit(serialNumber, amount, merchantId || null, reference);

      logger.info('Debit successful', { serialNumber, amount, newBalance });

      return res.json({ serialNumber, deducted: amount, newBalance, reference });
    } catch (err) {
      next(err);
    }
  }
);

/**
 * GET /api/v1/wallets/:serialNumber
 * Returns the wallet's current state (balance, status).
 */
router.get('/api/v1/wallets/:serialNumber', (req, res, next) => {
  try {
    const wallet = getWallet(req.params.serialNumber);
    return res.json(wallet);
  } catch (err) {
    next(err);
  }
});

/**
 * GET /api/v1/wallets/:serialNumber/transactions?limit=20
 * Returns the transaction history for a wallet — used by the consumer app
 * receipt / history screen.
 */
router.get('/api/v1/wallets/:serialNumber/transactions', (req, res, next) => {
  try {
    const limit = Math.min(parseInt(req.query.limit || '20', 10), 100);
    const history = getTransactionHistory(req.params.serialNumber, limit);
    return res.json({ serialNumber: req.params.serialNumber, transactions: history });
  } catch (err) {
    next(err);
  }
});

module.exports = router;
