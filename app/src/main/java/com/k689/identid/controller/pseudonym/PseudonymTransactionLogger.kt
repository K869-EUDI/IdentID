package com.k689.identid.controller.pseudonym

import com.k689.identid.model.storage.PseudonymTransactionLog
import com.k689.identid.storage.dao.PseudonymTransactionLogDao
import timber.log.Timber
import java.util.UUID

class PseudonymTransactionLogger(
    private val dao: PseudonymTransactionLogDao,
) {
    suspend fun logRegistration(
        rpId: String,
        rpName: String,
        pseudonymId: String,
        credentialId: String,
        userName: String,
    ) {
        val log = PseudonymTransactionLog(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            rpId = rpId,
            rpName = rpName,
            transactionType = PseudonymTransactionLog.TYPE_REGISTRATION,
            status = PseudonymTransactionLog.STATUS_COMPLETED,
            pseudonymId = pseudonymId,
            credentialId = credentialId,
            userName = userName,
        )
        dao.store(log)
        Timber.d("PseudonymTransactionLogger: Logged registration for rpId=$rpId")
    }

    suspend fun logRegistrationFailed(
        rpId: String,
        rpName: String,
        reason: String,
    ) {
        val log = PseudonymTransactionLog(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            rpId = rpId,
            rpName = rpName,
            transactionType = PseudonymTransactionLog.TYPE_REGISTRATION,
            status = PseudonymTransactionLog.STATUS_FAILED,
            failureReason = reason,
        )
        dao.store(log)
        Timber.d("PseudonymTransactionLogger: Logged failed registration for rpId=$rpId")
    }

    suspend fun logAuthentication(
        rpId: String,
        rpName: String,
        pseudonymId: String,
        credentialId: String,
        userName: String,
    ) {
        val log = PseudonymTransactionLog(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            rpId = rpId,
            rpName = rpName,
            transactionType = PseudonymTransactionLog.TYPE_AUTHENTICATION,
            status = PseudonymTransactionLog.STATUS_COMPLETED,
            pseudonymId = pseudonymId,
            credentialId = credentialId,
            userName = userName,
        )
        dao.store(log)
        Timber.d("PseudonymTransactionLogger: Logged authentication for rpId=$rpId")
    }

    suspend fun logAuthenticationFailed(
        rpId: String,
        rpName: String,
        reason: String,
        pseudonymId: String? = null,
    ) {
        val log = PseudonymTransactionLog(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            rpId = rpId,
            rpName = rpName,
            transactionType = PseudonymTransactionLog.TYPE_AUTHENTICATION,
            status = PseudonymTransactionLog.STATUS_FAILED,
            failureReason = reason,
            pseudonymId = pseudonymId,
        )
        dao.store(log)
        Timber.d("PseudonymTransactionLogger: Logged failed authentication for rpId=$rpId")
    }

    suspend fun getAllLogs(): List<PseudonymTransactionLog> = dao.getAll()

    suspend fun getLogsForPseudonym(pseudonymId: String): List<PseudonymTransactionLog> =
        dao.getByPseudonymId(pseudonymId)

    suspend fun getLogById(id: String): PseudonymTransactionLog? = dao.getById(id)

    suspend fun deleteLog(id: String) = dao.delete(id)

    suspend fun deleteAllLogs() = dao.deleteAll()
}
