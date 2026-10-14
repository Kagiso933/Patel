// Central configuration for the Ryno-Tec NFC Pass Platform.
// All process.env reads are isolated here so the rest of the code
// never reaches into process.env directly.
module.exports = {
  app: {
    name: 'Ryno-Tec NFC Pass Platform',
    version: '2.0.0',
    port: parseInt(process.env.PORT || '3000', 10),
    env: process.env.NODE_ENV || 'development',
  },

  huawei: {
    clientId: process.env.HUAWEI_CLIENT_ID,
    clientSecret: process.env.HUAWEI_CLIENT_SECRET,
    tokenUrl:
      process.env.HUAWEI_TOKEN_URL ||
      'https://oauth-login.cloud.huawei.com/oauth2/v3/token',
    issuerId: process.env.HUAWEI_ISSUER_ID,
    passTypeId: process.env.HUAWEI_PASS_TYPE_ID,
    signingPrivateKey: process.env.WALLET_SIGNING_PRIVATE_KEY,
  },

  stitch: {
    clientId: process.env.STITCH_CLIENT_ID,
    clientSecret: process.env.STITCH_CLIENT_SECRET,
    baseUrl: process.env.STITCH_BASE_URL || 'https://api.stitch.money',
  },

  db: {
    path: process.env.DB_PATH || './data/rynotec.db',
  },

  // API key that merchant POS devices send in X-Api-Key header.
  // In production, rotate and store in a secrets manager.
  merchantApiKey: process.env.MERCHANT_API_KEY,
};
