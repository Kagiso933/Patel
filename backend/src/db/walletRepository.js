const { v4: uuidv4 } = require('uuid');
const db = require('./database');
const { NotFoundError, InsufficientBalanceError } = require('../lib/errors');

const stmts = {
  upsert: db.prepare(`
    INSERT INTO wallets (serial_number, holder_name, holder_email, balance)
    VALUES (@serialNumber, @holderName, @holderEmail, @balance)
    ON CONFLICT(serial_number) DO UPDATE SET
      holder_name  = excluded.holder_name,
      holder_email = excluded.holder_email,
      updated_at   = unixepoch()
  `),

  get: db.prepare(`SELECT * FROM wallets WHERE serial_number = ?`),

  debit: db.prepare(`
    UPDATE wallets
    SET balance    = balance - @amount,
        updated_at = unixepoch()
    WHERE serial_number = @serialNumber AND balance >= @amount AND status = 'active'
  `),

  credit: db.prepare(`
    UPDATE wallets
    SET balance    = balance + @amount,
        updated_at = unixepoch()
    WHERE serial_number = @serialNumber AND status = 'active'
  `),

  logTx: db.prepare(`
    INSERT INTO transactions
      (id, serial_number, merchant_id, type, amount, balance_after, reference)
    VALUES
      (@id, @serialNumber, @merchantId, @type, @amount, @balanceAfter, @reference)
  `),

  history: db.prepare(`
    SELECT * FROM transactions
    WHERE serial_number = ?
    ORDER BY created_at DESC
    LIMIT ?
  `),
};

function upsertWallet({ serialNumber, holderName, holderEmail = null, balance }) {
  stmts.upsert.run({ serialNumber, holderName, holderEmail, balance });
  return stmts.get.get(serialNumber);
}

function getWallet(serialNumber) {
  const wallet = stmts.get.get(serialNumber);
  if (!wallet) throw new NotFoundError(`Wallet ${serialNumber}`);
  return wallet;
}

const debit = db.transaction((serialNumber, amount, merchantId, reference) => {
  const before = stmts.get.get(serialNumber);
  if (!before) throw new NotFoundError(`Wallet ${serialNumber}`);
  if (before.balance < amount) {
    throw new InsufficientBalanceError(before.balance, amount);
  }

  const result = stmts.debit.run({ serialNumber, amount });
  if (result.changes === 0) {
    // Race or suspended wallet
    throw new InsufficientBalanceError(before.balance, amount);
  }

  const after = stmts.get.get(serialNumber);
  stmts.logTx.run({
    id: uuidv4(),
    serialNumber,
    merchantId: merchantId || null,
    type: 'debit',
    amount,
    balanceAfter: after.balance,
    reference,
  });

  return { newBalance: after.balance };
});

const credit = db.transaction((serialNumber, amount, reference) => {
  stmts.credit.run({ serialNumber, amount });
  const after = stmts.get.get(serialNumber);
  stmts.logTx.run({
    id: uuidv4(),
    serialNumber,
    merchantId: null,
    type: 'credit',
    amount,
    balanceAfter: after.balance,
    reference,
  });
  return { newBalance: after.balance };
});

function getTransactionHistory(serialNumber, limit = 20) {
  return stmts.history.all(serialNumber, limit);
}

module.exports = { upsertWallet, getWallet, debit, credit, getTransactionHistory };
