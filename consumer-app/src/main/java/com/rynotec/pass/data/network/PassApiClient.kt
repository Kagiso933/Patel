package com.rynotec.pass.data.network

import com.rynotec.pass.data.model.WalletPass
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Ryno-Tec Pass API client — calls the Node backend to issue a signed
 * loyaltyinstance JWT and returns a typed WalletPass model.
 */
class PassApiClient(private val baseUrl: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val json = "application/json; charset=utf-8".toMediaType()

    fun issuePass(
        userId: String,
        balance: Double,
        holderName: String,
        holderEmail: String? = null,
        onResult: (Result<WalletPass>) -> Unit
    ) {
        val body = JSONObject().apply {
            put("userId", userId)
            put("balance", balance)
            put("holderName", holderName)
            holderEmail?.let { put("holderEmail", it) }
        }.toString().toRequestBody(json)

        val request = Request.Builder()
            .url("$baseUrl/api/v1/passes")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                onResult(Result.failure(e))
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use {
                    if (!it.isSuccessful) {
                        onResult(Result.failure(IOException("HTTP ${it.code}")))
                        return
                    }
                    val obj = JSONObject(it.body!!.string())
                    onResult(
                        Result.success(
                            WalletPass(
                                serialNumber = obj.getString("serialNumber"),
                                holderName   = obj.getString("holderName"),
                                balance      = obj.getDouble("balance"),
                                currency     = obj.optString("currency", "ZAR"),
                                jwe          = obj.getString("jwe"),
                            )
                        )
                    )
                }
            }
        })
    }
}
