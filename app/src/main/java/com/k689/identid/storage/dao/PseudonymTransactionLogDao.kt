package com.k689.identid.storage.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.k689.identid.model.storage.PseudonymTransactionLog

@Dao
interface PseudonymTransactionLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun store(log: PseudonymTransactionLog)

    @Query("SELECT * FROM pseudonym_transaction_logs ORDER BY timestamp DESC")
    suspend fun getAll(): List<PseudonymTransactionLog>

    @Query("SELECT * FROM pseudonym_transaction_logs WHERE pseudonymId = :pseudonymId ORDER BY timestamp DESC")
    suspend fun getByPseudonymId(pseudonymId: String): List<PseudonymTransactionLog>

    @Query("SELECT * FROM pseudonym_transaction_logs WHERE rpId = :rpId ORDER BY timestamp DESC")
    suspend fun getByRpId(rpId: String): List<PseudonymTransactionLog>

    @Query("SELECT * FROM pseudonym_transaction_logs WHERE id = :id")
    suspend fun getById(id: String): PseudonymTransactionLog?

    @Query("DELETE FROM pseudonym_transaction_logs WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM pseudonym_transaction_logs")
    suspend fun deleteAll()
}
