const { ValidationError } = require('../lib/errors');

/**
 * Middleware factory — rejects with 400 when any listed field is absent
 * from req.body. Chains cleanly with the central error handler.
 */
function requireFields(...fields) {
  return (req, res, next) => {
    const missing = fields.filter(
      (f) => req.body[f] === undefined || req.body[f] === null
    );
    if (missing.length) {
      return next(
        new ValidationError(`Missing required fields: ${missing.join(', ')}`)
      );
    }
    next();
  };
}

module.exports = { requireFields };
