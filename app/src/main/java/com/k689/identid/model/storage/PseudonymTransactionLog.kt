package com.k689.identid.model.storage

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pseudonym_transaction_logs")
data class PseudonymTransactionLog(
    @PrimaryKey
    val id: String,
    val timestamp: Long,
    val rpId: String,
    val rpName: String,
    val transactionType: String, // REGISTRATION or AUTHENTICATION
    val status: String, // COMPLETED or FAILED
    val failureReason: String? = null,
    val pseudonymId: String? = null,
    val credentialId: String? = null,
    val userName: String? = null,
) {
    companion object {
        const val TYPE_REGISTRATION = "REGISTRATION"
        const val TYPE_AUTHENTICATION = "AUTHENTICATION"
        const val STATUS_COMPLETED = "COMPLETED"
        const val STATUS_FAILED = "FAILED"
    }
}
