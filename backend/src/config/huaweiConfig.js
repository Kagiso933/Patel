// Central place to read Huawei Wallet Kit / AGC configuration from the
// environment so routes and services don't each parse process.env directly.
module.exports = {
  clientId: process.env.HUAWEI_CLIENT_ID,
  clientSecret: process.env.HUAWEI_CLIENT_SECRET,
  tokenUrl:
    process.env.HUAWEI_TOKEN_URL ||
    'https://oauth-login.cloud.huawei.com/oauth2/v3/token',
  issuerId: process.env.HUAWEI_ISSUER_ID,
  passTypeId: process.env.HUAWEI_PASS_TYPE_ID,
  signingPrivateKey: process.env.WALLET_SIGNING_PRIVATE_KEY,
};
