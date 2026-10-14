package com.rynotec.pass.wallet

import android.app.Activity
import android.content.Context
import android.util.Log
import com.huawei.hms.api.ConnectionResult
import com.huawei.hms.api.HuaweiApiAvailability
import com.huawei.hms.wallet.CreateWalletPassRequest
import com.huawei.hms.wallet.WalletPassClient

/**
 * Ryno-Tec wrapper around the HMS Core Wallet Kit SDK.
 *
 * Handles:
 *   - HMS availability check with user-resolvable error dialog.
 *   - Pushing the signed pass JWT into Huawei Wallet (auto-mirrors to
 *     any paired HarmonyOS watch).
 *
 * Note: verify CreateWalletPassRequest / WalletPassClient API against the
 * HMS Wallet Kit version pinned in build.gradle before releasing — class
 * names shift across SDK releases.
 */
class HuaweiWalletManager(private val activity: Activity) {

    companion object {
        private const val TAG = "RynoTec.WalletManager"
        private const val HMS_RESOLVE_REQUEST = 1001
    }

    enum class HmsState { READY, UNAVAILABLE, USER_RESOLVABLE }

    fun checkHmsState(context: Context): HmsState {
        val result = HuaweiApiAvailability.getInstance()
            .isHuaweiMobileServicesAvailable(context)
        return when {
            result == ConnectionResult.SUCCESS -> HmsState.READY
            HuaweiApiAvailability.getInstance().isUserResolvableError(result) ->
                HmsState.USER_RESOLVABLE
            else -> HmsState.UNAVAILABLE
        }
    }

    fun showHmsResolutionDialog() {
        HuaweiApiAvailability.getInstance()
            .getErrorDialog(activity, ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED,
                HMS_RESOLVE_REQUEST)
            ?.show()
    }

    /**
     * Adds the Ryno-Tec pass to Huawei Wallet.
     *
     * @param jwe     The signed JWT returned by POST /api/v1/passes.
     * @param onResult Callback on the calling thread — success flag + message.
     */
    fun addPass(jwe: String, onResult: (success: Boolean, message: String) -> Unit) {
        when (checkHmsState(activity)) {
            HmsState.USER_RESOLVABLE -> {
                showHmsResolutionDialog()
                onResult(false, "HMS Core update required")
                return
            }
            HmsState.UNAVAILABLE -> {
                onResult(false, "Huawei Mobile Services not available on this device")
                return
            }
            HmsState.READY -> { /* continue */ }
        }

        val request = CreateWalletPassRequest.Builder()
            .setPassJwe(jwe)
            .build()

        WalletPassClient.getInstance(activity)
            .createWalletPass(request)
            .addOnSuccessListener { result ->
                Log.i(TAG, "Pass added — serial: ${result.serialNumber}")
                onResult(true, "Ryno-Tec pass added to Huawei Wallet")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to add pass", e)
                onResult(false, e.message ?: "Unknown wallet error")
            }
    }
}
