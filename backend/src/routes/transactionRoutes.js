const express = require('express');
const { debit, getWallet } = require('../db/walletRepository');
const { requireFields } = require('../middleware/validateRequest');

const router = express.Router();

/**
 * POST /api/transaction/deduct
 * body: { serialNumber, amount }
 *
 * Called by the merchant POS app after a successful NFC tap + SELECT AID
 * handshake. Atomically debits the balance — the DB transaction ensures no
 * double-spend even under concurrent tap events.
 */
router.post(
  '/api/transaction/deduct',
  requireFields('serialNumber', 'amount'),
  (req, res) => {
    const { serialNumber, amount } = req.body;

    if (typeof amount !== 'number' || amount <= 0) {
      return res.status(400).json({ error: 'amount must be a positive number' });
    }

    const wallet = getWallet(serialNumber);
    if (!wallet) {
      return res.status(404).json({ error: 'Wallet not found' });
    }

    const result = debit(serialNumber, amount, `POS-TAP-${Date.now()}`);
    if (!result.success) {
      return res
        .status(402)
        .json({ error: 'Insufficient balance', currentBalance: wallet.balance });
    }

    return res.json({ serialNumber, deducted: amount, newBalance: result.newBalance });
  }
);

/**
 * GET /api/wallet/:serialNumber
 * Returns current wallet balance — useful for the merchant receipt screen.
 */
router.get('/api/wallet/:serialNumber', (req, res) => {
  const wallet = getWallet(req.params.serialNumber);
  if (!wallet) {
    return res.status(404).json({ error: 'Wallet not found' });
  }
  return res.json(wallet);
});

module.exports = router;
