# Closed-Loop Pre-Funded NFC Pass

Foundational code for a three-part NFC loyalty pass ecosystem built around
HMS Wallet Kit, bypassing EMV certification by treating the pass as a
closed-loop reader/applet system rather than a card-network transaction.

## Modules

### 1. `backend/` — Node.js / Express
Funds wallets via Open Banking and issues signed loyalty passes.

- `src/routes/passRoutes.js` — `POST /api/pass/generate`, orchestrates the
  flow below.
- `src/services/huaweiAuthService.js` — OAuth2 client-credentials token
  fetch/cache against Huawei's token endpoint.
- `src/services/walletPassService.js` — builds the `loyaltyinstance`
  payload (including `nfcProps`, the data the merchant POS reads over
  NFC) and signs it with the developer's RSA private key. See the comment
  in that file for a note on Huawei's "JWE" terminology.
- `src/routes/transactionRoutes.js` — `POST /api/transaction/deduct`,
  a minimal in-memory balance store the merchant app calls after a
  successful tap.

Setup:
```
cd backend
cp .env.example .env   # fill in Huawei/AGC credentials + signing key
npm install
npm start
```

### 2. `consumer-app/` — Kotlin (Android, HMS Core)
`AddToWalletManager` takes the JWE/JWT from `/api/pass/generate` and pushes
it into Huawei Wallet via `WalletPassClient`, including an HMS
availability check. A successful push auto-mirrors to any paired
HarmonyOS watch.

### 3. `merchant-pos-app/` — Kotlin (Android, standard NFC APIs)
The POS reader. Drop these files into an Android app module:

- `nfc/ApduCommands.kt` — builds the SELECT AID APDU
  (`00 A4 04 00 05 F2 22 22 22 22 00`) targeting AID `F222222222`.
- `nfc/NfcReaderManager.kt` — `NfcAdapter.enableReaderMode()` with
  `FLAG_READER_NFC_A | FLAG_READER_SKIP_NDEF_CHECK`, connects via
  `IsoDep`, sends the SELECT AID command, verifies the response ends in
  `90 00`, and extracts the serial number from the payload.
- `api/TransactionApiClient.kt` — posts `{ serialNumber, amount }` to
  `/api/transaction/deduct`.
- `pos/PosTransactionController.kt` — wires the above together
  (`onResume`/`onPause` lifecycle hooks), deducting a fixed R45.00 per
  tap as a simulation.

These are source files, not full Gradle modules — add them to an existing
Android project's `app/src/main/java/...` tree and declare the
`android.permission.NFC` permission plus the OkHttp/HMS Wallet Kit
dependencies each file imports.

## Flow

1. Backend funds a wallet (Open Banking) and signs a pass →
   `backend`.
2. Consumer app adds that pass to Huawei Wallet, which mirrors to the
   watch → `consumer-app`.
3. At checkout, the watch/phone taps the merchant tablet; the POS app
   reads the pass over NFC and deducts the balance server-side →
   `merchant-pos-app`.
