const express = require('express');
const { getHuaweiAccessToken } = require('../services/huaweiAuthService');
const { buildLoyaltyInstance, signPass } = require('../services/walletPassService');
const { fundWallet } = require('../services/stitchFundingService');
const { upsertWallet } = require('../db/walletRepository');
const { requireFields } = require('../middleware/validateRequest');
const { ValidationError } = require('../lib/errors');
const logger = require('../lib/logger');

const router = express.Router();

/**
 * POST /api/v1/passes
 * body: { userId, balance, holderName, holderEmail? }
 *
 * Ryno-Tec Pass Issuance Flow:
 *   1. Validate inputs.
 *   2. Initiate Stitch instant EFT to fund the wallet.
 *   3. Persist the wallet record in SQLite.
 *   4. Obtain Huawei AGC access token.
 *   5. Build and RS256-sign the loyaltyinstance JWT.
 *   6. Return token + funding metadata to the consumer app.
 */
router.post(
  '/api/v1/passes',
  requireFields('userId', 'balance', 'holderName'),
  async (req, res, next) => {
    const { userId, balance, holderName, holderEmail } = req.body;

    if (typeof balance !== 'number' || balance <= 0) {
      return next(new ValidationError('balance must be a positive number'));
    }

    const serialNumber = `RT-${String(userId).toUpperCase()}`;

    try {
      logger.info('Pass issuance initiated', { serialNumber, balance });

      const funding = await fundWallet(userId, balance, `RT-FUND-${serialNumber}`);
      const wallet = upsertWallet({ serialNumber, holderName, holderEmail, balance });

      await getHuaweiAccessToken();

      const loyaltyInstance = buildLoyaltyInstance({ serialNumber, balance, holderName });
      const jwe = signPass(loyaltyInstance);

      logger.info('Pass issued successfully', { serialNumber });

      return res.status(201).json({
        serialNumber,
        holderName,
        balance: wallet.balance,
        currency: wallet.currency,
        jwe,
        funding,
      });
    } catch (err) {
      next(err);
    }
  }
);

module.exports = router;
