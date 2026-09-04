package com.example.merchant.pos

import android.app.Activity
import android.util.Log
import com.example.merchant.api.TransactionApiClient
import com.example.merchant.nfc.NfcReaderManager

/**
 * Wires the NFC reader (Module 3's core logic) to the backend transaction
 * API: tap -> SELECT AID -> extract serial number -> deduct balance.
 */
class PosTransactionController(
    activity: Activity,
    backendBaseUrl: String,
    private val transactionAmount: Double = 45.00
) {
    companion object {
        private const val TAG = "PosTransactionController"
    }

    private val transactionApi = TransactionApiClient(backendBaseUrl)

    private val nfcReader = NfcReaderManager(
        activity = activity,
        onPassRead = { serialNumber -> handlePassRead(serialNumber) },
        onError = { message -> Log.e(TAG, "NFC read failed: $message") }
    )

    fun onResume() = nfcReader.startReaderMode()

    fun onPause() = nfcReader.stopReaderMode()

    private fun handlePassRead(serialNumber: String) {
        Log.i(TAG, "Pass read: $serialNumber, deducting R$transactionAmount")
        transactionApi.deduct(serialNumber, transactionAmount) { success, message ->
            if (success) {
                Log.i(TAG, message)
            } else {
                Log.e(TAG, "Deduction failed: $message")
            }
        }
    }
}
