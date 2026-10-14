const logger = require('../lib/logger');
const { AppError } = require('../lib/errors');

// eslint-disable-next-line no-unused-vars
module.exports = function errorHandler(err, req, res, next) {
  if (err instanceof AppError) {
    logger.warn('Operational error', {
      code: err.code,
      message: err.message,
      path: req.path,
    });
    return res.status(err.statusCode).json({
      error: { code: err.code, message: err.message, ...(err.current !== undefined && { currentBalance: err.current }) },
    });
  }

  // Unexpected / programmer errors — log full stack, never expose internals.
  logger.error('Unhandled error', { message: err.message, stack: err.stack, path: req.path });
  return res.status(500).json({
    error: { code: 'INTERNAL_ERROR', message: 'An unexpected error occurred' },
  });
};
