const Database = require('better-sqlite3');
const path = require('path');

const DB_PATH = process.env.DB_PATH || path.join(__dirname, '../../data/wallets.db');

const db = new Database(DB_PATH);

// Enable WAL mode for better concurrent read performance.
db.pragma('journal_mode = WAL');

db.exec(`
  CREATE TABLE IF NOT EXISTS wallets (
    serial_number TEXT PRIMARY KEY,
    holder_name   TEXT NOT NULL,
    balance       REAL NOT NULL DEFAULT 0,
    created_at    INTEGER NOT NULL DEFAULT (unixepoch()),
    updated_at    INTEGER NOT NULL DEFAULT (unixepoch())
  );

  CREATE TABLE IF NOT EXISTS transactions (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    serial_number TEXT NOT NULL,
    type          TEXT NOT NULL CHECK(type IN ('credit','debit')),
    amount        REAL NOT NULL,
    reference     TEXT,
    created_at    INTEGER NOT NULL DEFAULT (unixepoch()),
    FOREIGN KEY (serial_number) REFERENCES wallets(serial_number)
  );
`);

module.exports = db;
