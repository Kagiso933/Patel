// Typed error classes so route handlers can distinguish operational errors
// (return a structured 4xx/5xx) from programmer errors (let crash).

class AppError extends Error {
  constructor(message, statusCode, code) {
    super(message);
    this.name = this.constructor.name;
    this.statusCode = statusCode;
    this.code = code;
    Error.captureStackTrace(this, this.constructor);
  }
}

class ValidationError extends AppError {
  constructor(message) {
    super(message, 400, 'VALIDATION_ERROR');
  }
}

class NotFoundError extends AppError {
  constructor(resource) {
    super(`${resource} not found`, 404, 'NOT_FOUND');
  }
}

class InsufficientBalanceError extends AppError {
  constructor(current, requested) {
    super('Insufficient wallet balance', 402, 'INSUFFICIENT_BALANCE');
    this.current = current;
    this.requested = requested;
  }
}

class UpstreamError extends AppError {
  constructor(service, detail) {
    super(`Upstream service error: ${service}`, 502, 'UPSTREAM_ERROR');
    this.service = service;
    this.detail = detail;
  }
}

module.exports = {
  AppError,
  ValidationError,
  NotFoundError,
  InsufficientBalanceError,
  UpstreamError,
};
