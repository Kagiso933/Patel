const Database = require('better-sqlite3');
const path = require('path');
const fs = require('fs');
const config = require('../config');

const dbPath = path.resolve(config.db.path);
fs.mkdirSync(path.dirname(dbPath), { recursive: true });

const db = new Database(dbPath);
db.pragma('journal_mode = WAL');
db.pragma('foreign_keys = ON');

db.exec(`
  CREATE TABLE IF NOT EXISTS merchants (
    id          TEXT PRIMARY KEY,
    name        TEXT NOT NULL,
    api_key     TEXT NOT NULL UNIQUE,
    active      INTEGER NOT NULL DEFAULT 1,
    created_at  INTEGER NOT NULL DEFAULT (unixepoch())
  );

  CREATE TABLE IF NOT EXISTS wallets (
    serial_number TEXT PRIMARY KEY,
    holder_name   TEXT NOT NULL,
    holder_email  TEXT,
    balance       REAL NOT NULL DEFAULT 0,
    currency      TEXT NOT NULL DEFAULT 'ZAR',
    status        TEXT NOT NULL DEFAULT 'active'
                  CHECK(status IN ('active','suspended','expired')),
    created_at    INTEGER NOT NULL DEFAULT (unixepoch()),
    updated_at    INTEGER NOT NULL DEFAULT (unixepoch())
  );

  CREATE TABLE IF NOT EXISTS transactions (
    id            TEXT PRIMARY KEY,
    serial_number TEXT NOT NULL,
    merchant_id   TEXT,
    type          TEXT NOT NULL CHECK(type IN ('credit','debit')),
    amount        REAL NOT NULL,
    balance_after REAL NOT NULL,
    reference     TEXT,
    status        TEXT NOT NULL DEFAULT 'completed'
                  CHECK(status IN ('completed','failed','reversed')),
    created_at    INTEGER NOT NULL DEFAULT (unixepoch()),
    FOREIGN KEY (serial_number) REFERENCES wallets(serial_number),
    FOREIGN KEY (merchant_id)   REFERENCES merchants(id)
  );

  CREATE INDEX IF NOT EXISTS idx_transactions_serial
    ON transactions(serial_number, created_at DESC);
`);

module.exports = db;
