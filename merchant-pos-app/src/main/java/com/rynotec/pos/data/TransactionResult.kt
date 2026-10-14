package com.rynotec.pos.data

sealed class TransactionResult {
    data class Approved(
        val serialNumber: String,
        val amount: Double,
        val newBalance: Double,
        val reference: String,
    ) : TransactionResult()

    data class Declined(val reason: String) : TransactionResult()
    data class NetworkError(val message: String) : TransactionResult()
}
