const db = require('./database');

const stmts = {
  upsertWallet: db.prepare(`
    INSERT INTO wallets (serial_number, holder_name, balance)
    VALUES (@serialNumber, @holderName, @balance)
    ON CONFLICT(serial_number) DO UPDATE SET
      balance    = excluded.balance,
      updated_at = unixepoch()
  `),

  getWallet: db.prepare(
    `SELECT * FROM wallets WHERE serial_number = ?`
  ),

  credit: db.prepare(`
    UPDATE wallets
    SET balance = balance + @amount, updated_at = unixepoch()
    WHERE serial_number = @serialNumber
  `),

  debit: db.prepare(`
    UPDATE wallets
    SET balance = balance - @amount, updated_at = unixepoch()
    WHERE serial_number = @serialNumber AND balance >= @amount
  `),

  logTx: db.prepare(`
    INSERT INTO transactions (serial_number, type, amount, reference)
    VALUES (@serialNumber, @type, @amount, @reference)
  `),
};

/**
 * Creates or resets a wallet row when a new pass is issued.
 */
function upsertWallet(serialNumber, holderName, balance) {
  stmts.upsertWallet.run({ serialNumber, holderName, balance });
}

/**
 * Returns the wallet row, or null if not found.
 */
function getWallet(serialNumber) {
  return stmts.getWallet.get(serialNumber) ?? null;
}

/**
 * Atomically debits `amount` from the wallet.
 * Returns { success, newBalance } — success is false when balance is insufficient.
 */
const debit = db.transaction((serialNumber, amount, reference) => {
  const result = stmts.debit.run({ serialNumber, amount });
  if (result.changes === 0) {
    return { success: false };
  }
  stmts.logTx.run({ serialNumber, type: 'debit', amount, reference });
  const wallet = stmts.getWallet.get(serialNumber);
  return { success: true, newBalance: wallet.balance };
});

/**
 * Credits `amount` to the wallet (used when funding via Stitch).
 */
const credit = db.transaction((serialNumber, amount, reference) => {
  stmts.credit.run({ serialNumber, amount });
  stmts.logTx.run({ serialNumber, type: 'credit', amount, reference });
  const wallet = stmts.getWallet.get(serialNumber);
  return { newBalance: wallet.balance };
});

module.exports = { upsertWallet, getWallet, debit, credit };
