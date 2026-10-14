package com.rynotec.pass.data.model

data class WalletPass(
    val serialNumber: String,
    val holderName: String,
    val balance: Double,
    val currency: String,
    val jwe: String,
)

data class Transaction(
    val id: String,
    val type: String,       // "credit" | "debit"
    val amount: Double,
    val balanceAfter: Double,
    val reference: String?,
    val createdAt: Long,
)
