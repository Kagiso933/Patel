package com.rynotec.pass.ui

import android.app.Activity
import android.util.Log
import com.rynotec.pass.data.model.WalletPass
import com.rynotec.pass.data.network.PassApiClient
import com.rynotec.pass.wallet.HuaweiWalletManager

/**
 * Coordinates the full Ryno-Tec pass issuance flow:
 *   Step 1 — call backend: POST /api/v1/passes  → receive signed JWT
 *   Step 2 — push JWT into Huawei Wallet via HMS SDK
 *
 * Intended to be used as a ViewModel or plain presenter; call issuePass()
 * from the Activity/Fragment button handler.
 */
class PassViewModel(
    private val activity: Activity,
    backendBaseUrl: String,
) {
    companion object {
        private const val TAG = "RynoTec.PassViewModel"
    }

    private val apiClient = PassApiClient(backendBaseUrl)
    private val walletManager = HuaweiWalletManager(activity)

    fun issuePass(
        userId: String,
        balance: Double,
        holderName: String,
        holderEmail: String? = null,
        onProgress: (message: String) -> Unit,
        onComplete: (success: Boolean, pass: WalletPass?, error: String?) -> Unit,
    ) {
        onProgress("Funding wallet via Ryno-Tec…")

        apiClient.issuePass(userId, balance, holderName, holderEmail) { result ->
            result.fold(
                onSuccess = { pass ->
                    Log.i(TAG, "Pass token received — ${pass.serialNumber}")
                    onProgress("Adding pass to Huawei Wallet…")

                    walletManager.addPass(pass.jwe) { success, message ->
                        if (success) {
                            onComplete(true, pass, null)
                        } else {
                            Log.e(TAG, "Wallet SDK error: $message")
                            onComplete(false, null, message)
                        }
                    }
                },
                onFailure = { e ->
                    Log.e(TAG, "Backend error", e)
                    onComplete(false, null, e.message ?: "Network error")
                }
            )
        }
    }
}
