// Lightweight body-validation middleware so routes don't each repeat the
// same guard clauses.

/**
 * Returns a middleware that checks required fields are present in req.body
 * and rejects with 400 if any are missing.
 */
function requireFields(...fields) {
  return (req, res, next) => {
    const missing = fields.filter(
      (f) => req.body[f] === undefined || req.body[f] === null
    );
    if (missing.length) {
      return res
        .status(400)
        .json({ error: `Missing required fields: ${missing.join(', ')}` });
    }
    next();
  };
}

module.exports = { requireFields };
