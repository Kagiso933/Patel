const express = require('express');

const router = express.Router();

// In-memory balance store for local/demo use. A real deployment would read
// and update this in the same database that back the funded wallets.
const balances = new Map();

/**
 * POST /api/transaction/deduct
 * body: { serialNumber: string, amount: number }
 *
 * Called by the merchant POS app (Module 3) after a successful NFC tap and
 * SELECT AID handshake, to debit the pre-funded balance for that pass.
 */
router.post('/api/transaction/deduct', (req, res) => {
  const { serialNumber, amount } = req.body || {};

  if (!serialNumber || typeof amount !== 'number' || amount <= 0) {
    return res
      .status(400)
      .json({ error: 'serialNumber (string) and amount (positive number) are required' });
  }

  const currentBalance = balances.get(serialNumber) ?? 0;
  if (currentBalance < amount) {
    return res.status(402).json({ error: 'Insufficient balance', currentBalance });
  }

  const newBalance = currentBalance - amount;
  balances.set(serialNumber, newBalance);

  return res.json({ serialNumber, deducted: amount, newBalance });
});

module.exports = router;
