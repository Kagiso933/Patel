package com.rynotec.pos.ui

import android.util.Log
import com.rynotec.pos.data.TransactionApiClient
import com.rynotec.pos.data.TransactionResult
import com.rynotec.pos.nfc.NfcReadResult

/**
 * Ryno-Tec POS ViewModel — orchestrates the tap-to-pay loop:
 *
 *   NFC tap → SELECT AID → extract serial → deduct via backend → receipt
 *
 * Designed to be lifecycle-safe: all callbacks are delivered on whatever
 * thread OkHttp or the NFC reader fires on; the Activity must post UI
 * updates to the main thread (runOnUiThread / LiveData / Flow).
 */
class PosViewModel(
    backendBaseUrl: String,
    merchantApiKey: String,
    private val merchantId: String,
    private val transactionAmount: Double,
) {
    companion object {
        private const val TAG = "RynoTec.PosViewModel"
    }

    private val api = TransactionApiClient(backendBaseUrl, merchantApiKey, merchantId)

    enum class PosState { IDLE, READING, PROCESSING, APPROVED, DECLINED, ERROR }

    fun onNfcResult(
        result: NfcReadResult,
        onStateChange: (PosState, String) -> Unit,
    ) {
        when (result) {
            is NfcReadResult.Failure -> {
                Log.w(TAG, "NFC read failed: ${result.reason}")
                onStateChange(PosState.ERROR, result.reason)
            }

            is NfcReadResult.Success -> {
                Log.i(TAG, "Pass read: ${result.serialNumber}")
                onStateChange(PosState.PROCESSING, "Processing payment…")

                api.deduct(result.serialNumber, transactionAmount) { txResult ->
                    when (txResult) {
                        is TransactionResult.Approved -> {
                            Log.i(TAG, "Approved — ref: ${txResult.reference}")
                            onStateChange(
                                PosState.APPROVED,
                                "Approved ✓\nR${"%.2f".format(txResult.amount)}\nRef: ${txResult.reference}"
                            )
                        }
                        is TransactionResult.Declined -> {
                            Log.w(TAG, "Declined: ${txResult.reason}")
                            onStateChange(PosState.DECLINED, "Declined — ${txResult.reason}")
                        }
                        is TransactionResult.NetworkError -> {
                            Log.e(TAG, "Network error: ${txResult.message}")
                            onStateChange(PosState.ERROR, "Network error — please retry")
                        }
                    }
                }
            }
        }
    }
}
