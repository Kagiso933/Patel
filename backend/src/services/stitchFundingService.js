const axios = require('axios');

const STITCH_BASE_URL = 'https://api.stitch.money';

/**
 * Obtains an OAuth2 access token from Stitch (client-credentials flow).
 * Required before calling any Stitch payment initiation endpoints.
 */
async function getStitchToken() {
  const { data } = await axios.post(
    `${STITCH_BASE_URL}/connect/token`,
    new URLSearchParams({
      grant_type: 'client_credentials',
      client_id: process.env.STITCH_CLIENT_ID,
      client_secret: process.env.STITCH_CLIENT_SECRET,
      scope: 'payments',
    }),
    { headers: { 'Content-Type': 'application/x-www-form-urlencoded' } }
  );
  return data.access_token;
}

/**
 * Initiates an instant EFT payment via Stitch to fund a user wallet
 * before a pass is issued.
 *
 * @param {string} userId       Internal user/wallet identifier.
 * @param {number} amountZAR    Amount in South African rand.
 * @param {string} reference    Merchant reference shown on the payer's statement.
 * @returns {object}            Stitch payment intent (id, redirectUri, etc.)
 */
async function fundWallet(userId, amountZAR, reference) {
  const token = await getStitchToken();

  const { data } = await axios.post(
    `${STITCH_BASE_URL}/v1/payment_initiations`,
    {
      amount: { quantity: amountZAR, currency: 'ZAR' },
      externalReference: reference,
      beneficiaryReference: `wallet-${userId}`,
    },
    { headers: { Authorization: `Bearer ${token}` } }
  );

  return data;
}

module.exports = { fundWallet };
