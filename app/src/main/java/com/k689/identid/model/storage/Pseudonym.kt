package com.k689.identid.model.storage

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pseudonyms",
    indices = [Index(value = ["rpId", "credentialId"], unique = true)],
)
data class Pseudonym(
    @PrimaryKey
    val id: String,
    val rpId: String,
    val rpName: String,
    val credentialId: String,
    val publicKey: String,
    val keystoreAlias: String,
    val userAlias: String? = null,
    val userId: String,
    val userName: String,
    val createdAt: Long,
    val lastUsedAt: Long? = null,
)
