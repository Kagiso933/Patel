package com.example.merchant.api

import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

/** Talks to the backend's transaction endpoints. */
class TransactionApiClient(private val baseUrl: String) {

    private val client = OkHttpClient()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * POST /api/transaction/deduct - called after a successful NFC tap and
     * SELECT AID handshake to debit the pass's pre-funded balance.
     */
    fun deduct(
        serialNumber: String,
        amount: Double,
        onResult: (success: Boolean, message: String) -> Unit
    ) {
        val body = JSONObject()
            .put("serialNumber", serialNumber)
            .put("amount", amount)
            .toString()
            .toRequestBody(jsonMediaType)

        val request = Request.Builder()
            .url("$baseUrl/api/transaction/deduct")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onResult(false, e.message ?: "Network error")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (it.isSuccessful) {
                        onResult(true, "Deducted R$amount from $serialNumber")
                    } else {
                        onResult(false, "Server error: ${it.code}")
                    }
                }
            }
        })
    }
}
