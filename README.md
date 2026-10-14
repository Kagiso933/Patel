# Ryno-Tec NFC Pass Platform

**Closed-Loop Pre-Funded NFC Wallet — South African Market**

A three-module ecosystem that lets South African consumers load value via
Open Banking (Stitch instant EFT) onto a signed Huawei Wallet pass, then
tap to pay at a Ryno-Tec merchant terminal — all without EMV certification.

---

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│  Consumer                                                   │
│  ┌──────────────────┐   POST /api/v1/passes                 │
│  │  Ryno-Tec Pass   │──────────────────────► Backend        │
│  │  (Android/Huawei)│◄──────── signed JWT ─── Node/Express  │
│  └────────┬─────────┘                        SQLite         │
│           │ HMS Wallet SDK                   Stitch EFT     │
│           ▼                                                  │
│  ┌──────────────────┐                                       │
│  │  Huawei Wallet   │──── auto-mirrors ────► HarmonyOS watch│
│  └──────────────────┘                                       │
└─────────────────────────────────────────────────────────────┘
             │ NFC tap (ISO-DEP / ISO 7816-4)
             ▼
┌─────────────────────────────────────────────────────────────┐
│  Merchant POS                                               │
│  ┌──────────────────┐   SELECT AID APDU                    │
│  │  Ryno-Tec POS    │◄─────────────────── Pass applet      │
│  │  (Android tablet)│   90 00 + serial                     │
│  └────────┬─────────┘                                       │
│           │ POST /api/v1/transactions/deduct + X-Api-Key    │
│           ▼                                                  │
│         Backend — atomic SQLite debit → receipt             │
└─────────────────────────────────────────────────────────────┘
```

---

## Modules

### `backend/` — Node.js / Express (Enterprise)

| Feature | Detail |
|---|---|
| Routes | `POST /api/v1/passes` · `POST /api/v1/transactions/deduct` · `GET /api/v1/wallets/:sn` · `GET /api/v1/wallets/:sn/transactions` · `GET /health` |
| Auth | Merchant API key via `X-Api-Key` header |
| Security | Helmet headers · Rate limiting (10 rpm passes / 120 rpm deducts) |
| Database | SQLite (better-sqlite3, WAL mode, atomic debit transaction, full TX log) |
| Logging | Winston (JSON in prod, coloured in dev) |
| Open Banking | Stitch instant EFT (stitchFundingService) |
| Pass signing | RS256 JWT — `walletPassService` (see note below) |
| Docker | `Dockerfile` + `docker-compose.yml` with health check |

> **JWT / JWE note:** Huawei's docs call the artifact a "JWE" but the wire
> format is an RS256-signed JWT (a JWS). True JWE encrypts to a public key;
> signing with a private key produces a JWS. The implementation follows
> the actual Huawei Wallet Kit wire format.

**Quick start:**
```bash
cd backend
cp .env.example .env   # fill in credentials
npm install
npm run dev
```

**Docker:**
```bash
docker compose up --build
```

---

### `consumer-app/` — Kotlin, Android (HMS)

| Class | Responsibility |
|---|---|
| `PassApiClient` | Calls `POST /api/v1/passes`, returns typed `WalletPass` |
| `HuaweiWalletManager` | HMS Core availability check + `WalletPassClient.createWalletPass()` |
| `PassViewModel` | Composes fetch → add-to-wallet with progress callbacks |

Pass auto-mirrors to any paired HarmonyOS smartwatch after a successful push.

---

### `merchant-pos-app/` — Kotlin, Android (standard NFC APIs)

| Class | Responsibility |
|---|---|
| `ApduCommands` | Builds `SELECT AID` APDU (`00 A4 04 00 05 F2 22 22 22 22 00`) |
| `NfcReaderManager` | `enableReaderMode` + ISO-DEP handshake + payload extraction |
| `TransactionApiClient` | `POST /api/v1/transactions/deduct` with `X-Api-Key` |
| `PosViewModel` | Tap → deduct → typed `TransactionResult` |
| `PosActivity` | Full-screen POS UI with colour-coded state (green/red/blue) |

---

## APDU Reference

```
00 A4 04 00 05 F2 22 22 22 22 00
│  │  │  │  │  └─── AID (5 bytes) ───┘ │
│  │  │  │  Lc=5                        Le=0 (any length)
│  │  │  P2=0x00 (first occurrence)
│  │  P1=0x04 (select by name)
│  INS=0xA4 (SELECT)
CLA=0x00 (standard, no secure messaging)
```

Success response ends in `90 00`. Payload bytes before `90 00` are the
UTF-8 encoded pass serial number.

---

## Branch Strategy

```
main          ← release
  └── develop ← integration
        ├── feature/backend-pass-generation
        ├── feature/consumer-app-wallet
        ├── feature/merchant-pos-nfc-reader
        └── feature/ryno-tec-rebrand   ← this branch
```

---

## Checklist Before First Device Test

- [ ] Fill `backend/.env` with real Huawei AGC + Stitch credentials
- [ ] Set `MERCHANT_API_KEY` to a strong random secret
- [ ] Verify HMS Wallet Kit SDK version in `consumer-app/build.gradle`
- [ ] Replace `BuildConfig.BACKEND_URL` debug value with LAN IP for physical device testing
- [ ] Point `docker-compose.yml` data volume to a persistent host path
