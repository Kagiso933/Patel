require('dotenv').config();
const express = require('express');
const passRoutes = require('./routes/passRoutes');
const transactionRoutes = require('./routes/transactionRoutes');

const app = express();
app.use(express.json());

app.use(passRoutes);
app.use(transactionRoutes);

const port = process.env.PORT || 3000;
app.listen(port, () => {
  console.log(`NFC pass backend listening on port ${port}`);
});

module.exports = app;
