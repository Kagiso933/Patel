package com.rynotec.pos.nfc

/** Typed result returned by the NFC reader so callers pattern-match instead of checking nulls. */
sealed class NfcReadResult {
    data class Success(val serialNumber: String) : NfcReadResult()
    data class Failure(val reason: String) : NfcReadResult()
}
