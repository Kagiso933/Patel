package com.rynotec.pos.ui

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.rynotec.pos.nfc.NfcReaderManager
import com.rynotec.pos.nfc.NfcAvailabilityChecker

/**
 * Ryno-Tec POS — full-screen tap-to-pay Activity.
 *
 * Place the tablet/phone on the counter face-up; the customer taps their
 * Huawei device (phone or HarmonyOS watch). The NFC reader loop runs for
 * the Activity's entire foreground lifetime.
 *
 * Replace BuildConfig.BACKEND_URL / MERCHANT_API_KEY / MERCHANT_ID with
 * your actual gradle buildConfigField values.
 */
class PosActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var nfcReader: NfcReaderManager
    private lateinit var viewModel: PosViewModel
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        statusText = TextView(this).apply {
            textSize = 28f
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#121212"))
            setTextColor(Color.WHITE)
            setPadding(48, 48, 48, 48)
        }
        setContentView(statusText)

        when (NfcAvailabilityChecker.check(this)) {
            NfcAvailabilityChecker.State.NO_HARDWARE -> {
                showStatus("NFC not supported on this device", "#E53935")
                return
            }
            NfcAvailabilityChecker.State.DISABLED -> {
                showStatus("NFC is disabled — enable it in Settings", "#FFA000")
                return
            }
            NfcAvailabilityChecker.State.AVAILABLE -> { /* proceed */ }
        }

        viewModel = PosViewModel(
            backendBaseUrl    = BuildConfig.BACKEND_URL,
            merchantApiKey    = BuildConfig.MERCHANT_API_KEY,
            merchantId        = BuildConfig.MERCHANT_ID,
            transactionAmount = 45.00,
        )

        nfcReader = NfcReaderManager(this) { result ->
            viewModel.onNfcResult(result) { state, message ->
                mainHandler.post {
                    val color = when (state) {
                        PosViewModel.PosState.APPROVED    -> "#2E7D32"
                        PosViewModel.PosState.DECLINED    -> "#C62828"
                        PosViewModel.PosState.ERROR       -> "#E53935"
                        PosViewModel.PosState.PROCESSING  -> "#1565C0"
                        else                              -> "#121212"
                    }
                    showStatus(message, color)

                    // Auto-reset to ready state after 3 s
                    if (state in listOf(
                            PosViewModel.PosState.APPROVED,
                            PosViewModel.PosState.DECLINED,
                            PosViewModel.PosState.ERROR,
                        )
                    ) {
                        mainHandler.postDelayed({ showReady() }, 3_000)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::nfcReader.isInitialized) {
            nfcReader.startReading()
            showReady()
        }
    }

    override fun onPause() {
        super.onPause()
        if (::nfcReader.isInitialized) nfcReader.stopReading()
    }

    private fun showReady() =
        showStatus("Ryno-Tec POS\n\nTap to pay", "#121212")

    private fun showStatus(message: String, hexColor: String) {
        statusText.text = message
        statusText.setBackgroundColor(Color.parseColor(hexColor))
    }
}
