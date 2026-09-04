package com.example.consumer.wallet

import android.app.Activity
import android.content.Context
import android.util.Log
import com.huawei.hms.api.ConnectionResult
import com.huawei.hms.api.HuaweiApiAvailability
import com.huawei.hms.wallet.CreateWalletPassRequest
import com.huawei.hms.wallet.CreateWalletPassResult
import com.huawei.hms.wallet.WalletPassClient

/**
 * Pushes a backend-issued pass into Huawei Wallet.
 *
 * NOTE: exact class/method names on com.huawei.hms.wallet.* have shifted
 * between HMS Core Wallet Kit SDK releases - verify these against the
 * Wallet Kit SDK reference for the version pinned in build.gradle before
 * shipping.
 */
class AddToWalletManager(private val activity: Activity) {

    companion object {
        private const val TAG = "AddToWalletManager"
    }

    /** True when HMS Core is installed and up to date on this device. */
    fun isHmsAvailable(context: Context): Boolean {
        val availability = HuaweiApiAvailability.getInstance()
        val result = availability.isHuaweiMobileServicesAvailable(context)

        if (result != ConnectionResult.SUCCESS) {
            if (availability.isUserResolvableError(result)) {
                availability.getErrorDialog(activity, result, 0)?.show()
            } else {
                Log.e(TAG, "HMS Core unavailable, error code=$result")
            }
            return false
        }
        return true
    }

    /**
     * Pushes the signed pass token returned by POST /api/pass/generate
     * (Module 1) into Huawei Wallet. A successful push automatically
     * mirrors the pass to any paired HarmonyOS watch.
     */
    fun addPassToWallet(jwe: String, onResult: (success: Boolean, message: String) -> Unit) {
        if (!isHmsAvailable(activity)) {
            onResult(false, "HMS Core not available on this device")
            return
        }

        val request = CreateWalletPassRequest.Builder()
            .setPassJwe(jwe)
            .build()

        WalletPassClient.getInstance(activity)
            .createWalletPass(request)
            .addOnSuccessListener { result: CreateWalletPassResult ->
                Log.i(TAG, "Pass created, serial=${result.serialNumber}")
                onResult(true, "Pass added to Huawei Wallet")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to add pass to wallet", e)
                onResult(false, e.message ?: "Unknown wallet error")
            }
    }
}
