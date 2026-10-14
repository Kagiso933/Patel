const config = require('../config');
const { AppError } = require('../lib/errors');

/**
 * Validates the X-Api-Key header on merchant POS requests.
 * In a production multi-tenant deployment this would look up the key
 * in the database and attach the merchant record to req.merchant.
 */
function requireMerchantKey(req, res, next) {
  const provided = req.headers['x-api-key'];
  if (!provided || provided !== config.merchantApiKey) {
    return next(new AppError('Invalid or missing API key', 401, 'UNAUTHORIZED'));
  }
  next();
}

module.exports = { requireMerchantKey };
