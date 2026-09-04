const jwt = require('jsonwebtoken');
const huaweiConfig = require('../config/huaweiConfig');

/**
 * Builds the loyaltyinstance JSON payload Huawei Wallet renders on the
 * pass and mirrors to a paired HarmonyOS watch.
 *
 * nfcProps carries the data the merchant POS actually reads over
 * ISO-DEP/APDU at the point of sale - it does not need everything the
 * visual card shows, just enough to correlate the tap back to this
 * user's serial number server-side.
 */
function buildLoyaltyInstance({ serialNumber, balance, holderName }) {
  return {
    serialNumber,
    passStyleIdentifier: huaweiConfig.passTypeId,
    issuerId: huaweiConfig.issuerId,
    status: 'ACTIVATE',
    cardHolderName: holderName,
    balance: {
      value: balance,
      currencyCode: 'ZAR',
    },
    nfcProps: {
      // Raw bytes the NFC applet returns on SELECT AID; the merchant app
      // decodes this back to `serialNumber` (see Module 3).
      nfcPayload: Buffer.from(serialNumber, 'utf8').toString('base64'),
    },
  };
}

/**
 * Signs the loyaltyinstance payload with the developer's RSA private key.
 *
 * Note on naming: Huawei's Wallet Kit docs refer to this token as a "JWE",
 * but the artifact it produces is an RS256-signed JWT (a JWS) - integrity-
 * protected with the private key registered in AppGallery Connect, not
 * encrypted. Standard JWE key-management algorithms encrypt to a
 * *public* key; there is no such thing as "encrypting with a private key".
 * We follow Huawei's actual wire format here (RS256 JWT) rather than the
 * JWE name.
 */
function signPass(loyaltyInstance) {
  const payload = {
    iss: huaweiConfig.issuerId,
    typ: 'loyaltyinstance',
    iat: Math.floor(Date.now() / 1000),
    payload: { loyaltyInstance },
  };

  return jwt.sign(payload, huaweiConfig.signingPrivateKey, {
    algorithm: 'RS256',
  });
}

module.exports = { buildLoyaltyInstance, signPass };
