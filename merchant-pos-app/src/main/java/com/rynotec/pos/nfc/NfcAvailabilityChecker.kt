package com.rynotec.pos.nfc

import android.content.Context
import android.nfc.NfcManager

object NfcAvailabilityChecker {

    enum class State { AVAILABLE, NO_HARDWARE, DISABLED }

    fun check(context: Context): State {
        val adapter = (context.getSystemService(Context.NFC_SERVICE) as? NfcManager)
            ?.defaultAdapter ?: return State.NO_HARDWARE
        return if (adapter.isEnabled) State.AVAILABLE else State.DISABLED
    }
}
