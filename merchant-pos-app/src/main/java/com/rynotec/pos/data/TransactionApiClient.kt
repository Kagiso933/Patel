package com.rynotec.pos.data

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Ryno-Tec POS API client.
 *
 * Calls POST /api/v1/transactions/deduct on the backend after a successful
 * NFC tap, and returns a typed TransactionResult.
 *
 * @param baseUrl      Backend base URL (e.g. https://api.ryno-tec.co.za)
 * @param merchantApiKey  X-Api-Key sent on every deduct request
 * @param merchantId   This terminal's merchant identifier
 */
class TransactionApiClient(
    private val baseUrl: String,
    private val merchantApiKey: String,
    private val merchantId: String,
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val json = "application/json; charset=utf-8".toMediaType()

    fun deduct(
        serialNumber: String,
        amount: Double,
        onResult: (TransactionResult) -> Unit,
    ) {
        val body = JSONObject()
            .put("serialNumber", serialNumber)
            .put("amount", amount)
            .put("merchantId", merchantId)
            .toString()
            .toRequestBody(json)

        val request = Request.Builder()
            .url("$baseUrl/api/v1/transactions/deduct")
            .addHeader("X-Api-Key", merchantApiKey)
            .post(body)
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                onResult(TransactionResult.NetworkError(e.message ?: "Network error"))
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use {
                    val bodyStr = it.body?.string() ?: "{}"
                    when {
                        it.isSuccessful -> {
                            val obj = JSONObject(bodyStr)
                            onResult(
                                TransactionResult.Approved(
                                    serialNumber = obj.getString("serialNumber"),
                                    amount       = obj.getDouble("deducted"),
                                    newBalance   = obj.getDouble("newBalance"),
                                    reference    = obj.optString("reference", ""),
                                )
                            )
                        }
                        it.code == 402 -> {
                            val obj = JSONObject(bodyStr)
                            onResult(
                                TransactionResult.Declined(
                                    obj.optJSONObject("error")?.optString("message")
                                        ?: "Insufficient balance"
                                )
                            )
                        }
                        else -> onResult(TransactionResult.NetworkError("HTTP ${it.code}"))
                    }
                }
            }
        })
    }
}
