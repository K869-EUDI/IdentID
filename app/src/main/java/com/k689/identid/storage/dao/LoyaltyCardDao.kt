package com.k689.identid.storage.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.k689.identid.model.storage.LoyaltyCard
import kotlinx.coroutines.flow.Flow

@Dao
interface LoyaltyCardDao {
    @Query("SELECT * FROM loyalty_cards ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<LoyaltyCard>>

    @Query("SELECT * FROM loyalty_cards ORDER BY updatedAt DESC")
    suspend fun retrieveAll(): List<LoyaltyCard>

    @Query("SELECT * FROM loyalty_cards WHERE id = :id")
    suspend fun retrieve(id: String): LoyaltyCard?

    @Query("SELECT * FROM loyalty_cards WHERE barcodeValue = :barcodeValue LIMIT 1")
    suspend fun retrieveByBarcodeValue(barcodeValue: String): LoyaltyCard?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun store(value: LoyaltyCard)

    @Query("DELETE FROM loyalty_cards WHERE id = :id")
    suspend fun delete(id: String)
}