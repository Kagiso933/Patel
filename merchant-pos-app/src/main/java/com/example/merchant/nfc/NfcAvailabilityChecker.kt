package com.example.merchant.nfc

import android.content.Context
import android.nfc.NfcAdapter
import android.nfc.NfcManager

/** Guards the POS screen against devices without NFC or with NFC disabled. */
object NfcAvailabilityChecker {

    enum class State { AVAILABLE, NO_HARDWARE, DISABLED }

    fun check(context: Context): State {
        val manager = context.getSystemService(Context.NFC_SERVICE) as? NfcManager
        val adapter = manager?.defaultAdapter ?: return State.NO_HARDWARE
        return if (adapter.isEnabled) State.AVAILABLE else State.DISABLED
    }

    /** True only when the adapter exists and NFC is switched on. */
    fun isReady(context: Context) = check(context) == State.AVAILABLE
}
