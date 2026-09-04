package com.example.consumer.wallet

import android.app.Activity
import android.util.Log

/**
 * Coordinates the two-step flow:
 *   1. Fetch a signed pass token from the backend.
 *   2. Push the token into Huawei Wallet.
 *
 * Call from an Activity that has already confirmed HMS availability.
 */
class WalletFlowOrchestrator(
    private val activity: Activity,
    backendBaseUrl: String
) {
    companion object {
        private const val TAG = "WalletFlowOrchestrator"
    }

    private val apiClient = PassApiClient(backendBaseUrl)
    private val walletManager = AddToWalletManager(activity)

    fun issueAndAddPass(
        userId: String,
        balance: Double,
        holderName: String,
        onComplete: (success: Boolean, message: String) -> Unit
    ) {
        apiClient.generatePass(userId, balance, holderName) { result ->
            result.fold(
                onSuccess = { token ->
                    Log.i(TAG, "Pass token received for ${token.serialNumber}")
                    walletManager.addPassToWallet(token.jwe, onComplete)
                },
                onFailure = { e ->
                    Log.e(TAG, "Failed to fetch pass from backend", e)
                    onComplete(false, e.message ?: "Backend error")
                }
            )
        }
    }
}
