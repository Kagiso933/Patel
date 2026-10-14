const winston = require('winston');
const config = require('../config');

const fmt = winston.format;

const logger = winston.createLogger({
  level: config.app.env === 'production' ? 'info' : 'debug',
  format: fmt.combine(
    fmt.timestamp({ format: 'YYYY-MM-DDTHH:mm:ss.SSSZ' }),
    fmt.errors({ stack: true }),
    config.app.env === 'production'
      ? fmt.json()
      : fmt.combine(fmt.colorize(), fmt.printf(
          ({ timestamp, level, message, ...meta }) =>
            `${timestamp} [${level}] ${message}${
              Object.keys(meta).length ? ' ' + JSON.stringify(meta) : ''
            }`
        ))
  ),
  transports: [new winston.transports.Console()],
});

module.exports = logger;
