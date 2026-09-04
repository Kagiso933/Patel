package com.example.consumer.wallet

import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

data class PassToken(val serialNumber: String, val jwe: String)

/** Fetches a signed pass token from the backend (POST /api/pass/generate). */
class PassApiClient(private val baseUrl: String) {

    private val client = OkHttpClient()
    private val json = "application/json; charset=utf-8".toMediaType()

    fun generatePass(
        userId: String,
        balance: Double,
        holderName: String,
        onResult: (Result<PassToken>) -> Unit
    ) {
        val body = JSONObject()
            .put("userId", userId)
            .put("balance", balance)
            .put("holderName", holderName)
            .toString()
            .toRequestBody(json)

        val request = Request.Builder()
            .url("$baseUrl/api/pass/generate")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onResult(Result.failure(e))
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) {
                        onResult(Result.failure(IOException("HTTP ${it.code}")))
                        return
                    }
                    val obj = JSONObject(it.body!!.string())
                    onResult(
                        Result.success(
                            PassToken(
                                serialNumber = obj.getString("serialNumber"),
                                jwe = obj.getString("jwe")
                            )
                        )
                    )
                }
            }
        })
    }
}
