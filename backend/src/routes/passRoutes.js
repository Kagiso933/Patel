const express = require('express');
const { getHuaweiAccessToken } = require('../services/huaweiAuthService');
const { buildLoyaltyInstance, signPass } = require('../services/walletPassService');
const { fundWallet } = require('../services/stitchFundingService');
const { upsertWallet } = require('../db/walletRepository');
const { requireFields } = require('../middleware/validateRequest');

const router = express.Router();

/**
 * POST /api/pass/generate
 * body: { userId, balance, holderName }
 *
 * Flow:
 *   1. Initiate Open Banking (Stitch) payment to fund the wallet.
 *   2. Persist the funded wallet in the DB.
 *   3. Obtain a Huawei access token (validates credentials early).
 *   4. Build and sign the loyaltyinstance pass.
 *   5. Return the signed token to the consumer app.
 */
router.post(
  '/api/pass/generate',
  requireFields('userId', 'balance', 'holderName'),
  async (req, res) => {
    const { userId, balance, holderName } = req.body;

    if (typeof balance !== 'number' || balance <= 0) {
      return res.status(400).json({ error: 'balance must be a positive number' });
    }

    const serialNumber = `USR-${userId}`;

    try {
      // Step 1 — fund via Stitch (Open Banking instant EFT).
      // In production this returns a redirectUri; the consumer app opens it
      // in a WebView so the user completes bank auth before the pass is issued.
      const funding = await fundWallet(
        userId,
        balance,
        `PASS-FUND-${serialNumber}`
      );

      // Step 2 — persist the wallet record.
      upsertWallet(serialNumber, holderName, balance);

      // Step 3 — validate Huawei credentials.
      await getHuaweiAccessToken();

      // Step 4 — build and sign the pass.
      const loyaltyInstance = buildLoyaltyInstance({ serialNumber, balance, holderName });
      const jwe = signPass(loyaltyInstance);

      return res.json({ serialNumber, jwe, funding });
    } catch (err) {
      console.error('Pass generation failed:', err.response?.data || err.message);
      return res.status(502).json({ error: 'Failed to generate wallet pass' });
    }
  }
);

module.exports = router;
