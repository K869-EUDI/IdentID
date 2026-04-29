package com.k689.identid.model.storage

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "loyalty_cards")
data class LoyaltyCard(
    @PrimaryKey
    val id: String,
    val displayName: String,
    val barcodeValue: String,
    val barcodeFormat: String,
    val createdAt: Long,
    val updatedAt: Long,
)