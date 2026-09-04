const express = require('express');
const { getHuaweiAccessToken } = require('../services/huaweiAuthService');
const { buildLoyaltyInstance, signPass } = require('../services/walletPassService');

const router = express.Router();

/**
 * POST /api/pass/generate
 * body: { userId: string, balance: number, holderName: string }
 *
 * Generates a signed loyaltyinstance pass for a wallet that has already
 * been funded via Open Banking, and returns the signed token so the
 * consumer app can push it into Huawei Wallet (Module 2).
 */
router.post('/api/pass/generate', async (req, res) => {
  const { userId, balance, holderName } = req.body || {};

  if (!userId || typeof balance !== 'number') {
    return res
      .status(400)
      .json({ error: 'userId (string) and balance (number) are required' });
  }

  try {
    // Validates Huawei credentials up front; also required if this route
    // is extended to call other AGC/Wallet Kit server APIs directly.
    await getHuaweiAccessToken();

    const serialNumber = `USR-${userId}`;
    const loyaltyInstance = buildLoyaltyInstance({
      serialNumber,
      balance,
      holderName,
    });
    const jwe = signPass(loyaltyInstance);

    return res.json({ serialNumber, jwe });
  } catch (err) {
    console.error('Pass generation failed:', err.response?.data || err.message);
    return res.status(502).json({ error: 'Failed to generate wallet pass' });
  }
});

module.exports = router;
