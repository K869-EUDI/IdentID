package com.k689.identid.storage.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.k689.identid.model.storage.Pseudonym

@Dao
interface PseudonymDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun store(pseudonym: Pseudonym)

    @Query("SELECT * FROM pseudonyms WHERE id = :id")
    suspend fun getById(id: String): Pseudonym?

    @Query("SELECT * FROM pseudonyms WHERE rpId = :rpId")
    suspend fun getByRpId(rpId: String): List<Pseudonym>

    @Query("SELECT * FROM pseudonyms WHERE credentialId = :credentialId")
    suspend fun getByCredentialId(credentialId: String): Pseudonym?

    @Query("SELECT * FROM pseudonyms")
    suspend fun getAll(): List<Pseudonym>

    @Update
    suspend fun update(pseudonym: Pseudonym)

    @Query("DELETE FROM pseudonyms WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM pseudonyms WHERE rpId = :rpId")
    suspend fun deleteByRpId(rpId: String)

    @Query("DELETE FROM pseudonyms")
    suspend fun deleteAll()
}
