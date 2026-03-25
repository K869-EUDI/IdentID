package com.k689.identid.interactor.pseudonym

import com.k689.identid.controller.pseudonym.PseudonymRepository
import com.k689.identid.controller.pseudonym.PseudonymTransactionLogger
import com.k689.identid.model.storage.Pseudonym
import com.k689.identid.model.storage.PseudonymTransactionLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PseudonymGroupUi(
    val rpName: String,
    val rpId: String,
    val pseudonyms: List<PseudonymItemUi>,
)

data class PseudonymItemUi(
    val id: String,
    val displayName: String,
    val credentialIdTruncated: String,
    val createdAt: String,
    val lastUsedAt: String?,
)

data class PseudonymDetailUi(
    val id: String,
    val rpName: String,
    val rpId: String,
    val userAlias: String?,
    val userName: String,
    val credentialId: String,
    val createdAt: String,
    val lastUsedAt: String?,
)

data class PseudonymTransactionLogUi(
    val id: String,
    val timestamp: String,
    val rpName: String,
    val rpId: String,
    val transactionType: String,
    val isRegistration: Boolean,
    val isCompleted: Boolean,
    val failureReason: String?,
    val pseudonymId: String?,
    val credentialId: String?,
    val userName: String?,
)

interface PseudonymInteractor {
    suspend fun getAllGroupedByRp(): List<PseudonymGroupUi>

    suspend fun getDetail(id: String): PseudonymDetailUi?

    suspend fun updateAlias(id: String, alias: String?)

    suspend fun deletePseudonym(id: String)

    suspend fun getAllTransactionLogs(): List<PseudonymTransactionLogUi>

    suspend fun getTransactionLogsForPseudonym(pseudonymId: String): List<PseudonymTransactionLogUi>

    suspend fun getTransactionLogById(id: String): PseudonymTransactionLogUi?

    suspend fun deleteTransactionLog(id: String)

    suspend fun deleteAllTransactionLogs()
}

class PseudonymInteractorImpl(
    private val pseudonymRepository: PseudonymRepository,
    private val transactionLogger: PseudonymTransactionLogger,
) : PseudonymInteractor {
    private val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

    override suspend fun getAllGroupedByRp(): List<PseudonymGroupUi> {
        val all = pseudonymRepository.getAllPseudonyms()
        return all
            .groupBy { it.rpId }
            .map { (rpId, pseudonyms) ->
                PseudonymGroupUi(
                    rpName = pseudonyms.first().rpName,
                    rpId = rpId,
                    pseudonyms = pseudonyms.map { it.toItemUi() },
                )
            }
    }

    override suspend fun getDetail(id: String): PseudonymDetailUi? {
        val pseudonym = pseudonymRepository.getPseudonymById(id) ?: return null
        return PseudonymDetailUi(
            id = pseudonym.id,
            rpName = pseudonym.rpName,
            rpId = pseudonym.rpId,
            userAlias = pseudonym.userAlias,
            userName = pseudonym.userName,
            credentialId = pseudonym.credentialId,
            createdAt = formatDate(pseudonym.createdAt),
            lastUsedAt = pseudonym.lastUsedAt?.let { formatDate(it) },
        )
    }

    override suspend fun updateAlias(id: String, alias: String?) {
        pseudonymRepository.updateAlias(id, alias)
    }

    override suspend fun deletePseudonym(id: String) {
        pseudonymRepository.deletePseudonym(id)
    }

    override suspend fun getAllTransactionLogs(): List<PseudonymTransactionLogUi> =
        transactionLogger.getAllLogs().map { it.toUi() }

    override suspend fun getTransactionLogsForPseudonym(pseudonymId: String): List<PseudonymTransactionLogUi> =
        transactionLogger.getLogsForPseudonym(pseudonymId).map { it.toUi() }

    override suspend fun getTransactionLogById(id: String): PseudonymTransactionLogUi? =
        transactionLogger.getLogById(id)?.toUi()

    override suspend fun deleteTransactionLog(id: String) =
        transactionLogger.deleteLog(id)

    override suspend fun deleteAllTransactionLogs() =
        transactionLogger.deleteAllLogs()

    private fun Pseudonym.toItemUi(): PseudonymItemUi =
        PseudonymItemUi(
            id = id,
            displayName = userAlias ?: userName,
            credentialIdTruncated = if (credentialId.length > 16) "${credentialId.take(16)}..." else credentialId,
            createdAt = formatDate(createdAt),
            lastUsedAt = lastUsedAt?.let { formatDate(it) },
        )

    private fun PseudonymTransactionLog.toUi(): PseudonymTransactionLogUi =
        PseudonymTransactionLogUi(
            id = id,
            timestamp = formatDate(timestamp),
            rpName = rpName,
            rpId = rpId,
            transactionType = transactionType,
            isRegistration = transactionType == PseudonymTransactionLog.TYPE_REGISTRATION,
            isCompleted = status == PseudonymTransactionLog.STATUS_COMPLETED,
            failureReason = failureReason,
            pseudonymId = pseudonymId,
            credentialId = credentialId,
            userName = userName,
        )

    private fun formatDate(timestamp: Long): String = dateFormat.format(Date(timestamp))
}
