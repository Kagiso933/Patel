package com.example.merchant.ui

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.merchant.nfc.NfcAvailabilityChecker
import com.example.merchant.pos.PosTransactionController

/**
 * Full-screen POS tap screen. Place the tablet/phone on the counter;
 * the customer taps their Huawei device (phone or watch) to pay.
 */
class PosActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var posController: PosTransactionController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Replace with your actual layout resource.
        statusText = TextView(this).also { setContentView(it) }

        if (!NfcAvailabilityChecker.isReady(this)) {
            statusText.text = "NFC unavailable — check device settings"
            return
        }

        posController = PosTransactionController(
            activity = this,
            backendBaseUrl = "http://10.0.2.2:3000",   // emulator loopback; swap for prod URL
            transactionAmount = 45.00
        )
    }

    override fun onResume() {
        super.onResume()
        if (::posController.isInitialized) {
            posController.onResume()
            statusText.text = "Ready — tap to pay"
        }
    }

    override fun onPause() {
        super.onPause()
        if (::posController.isInitialized) posController.onPause()
    }
}
