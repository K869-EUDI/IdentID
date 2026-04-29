/*
 * Copyright (c) 2023 European Commission
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European
 * Commission - subsequent versions of the EUPL (the "Licence"); You may not use this work
 * except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the Licence is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific language
 * governing permissions and limitations under the Licence.
 */

package com.k689.identid.storage.service

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.k689.identid.model.storage.Bookmark
import com.k689.identid.model.storage.LoyaltyCard
import com.k689.identid.model.storage.Pseudonym
import com.k689.identid.model.storage.PseudonymTransactionLog
import com.k689.identid.model.storage.RevokedDocument
import com.k689.identid.model.storage.TransactionLog
import com.k689.identid.storage.dao.BookmarkDao
import com.k689.identid.storage.dao.LoyaltyCardDao
import com.k689.identid.storage.dao.PseudonymDao
import com.k689.identid.storage.dao.PseudonymTransactionLogDao
import com.k689.identid.storage.dao.RevokedDocumentDao
import com.k689.identid.storage.dao.TransactionLogDao

@Database(
    entities = [
        Bookmark::class,
        RevokedDocument::class,
        TransactionLog::class,
        Pseudonym::class,
        PseudonymTransactionLog::class,
        LoyaltyCard::class,
    ],
    version = 5,
)
abstract class DatabaseService : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao

    abstract fun loyaltyCardDao(): LoyaltyCardDao

    abstract fun revokedDocumentDao(): RevokedDocumentDao

    abstract fun transactionLogDao(): TransactionLogDao

    abstract fun pseudonymDao(): PseudonymDao

    abstract fun pseudonymTransactionLogDao(): PseudonymTransactionLogDao

    companion object {
        val MIGRATION_3_4 =
            object : Migration(3, 4) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "ALTER TABLE pseudonyms ADD COLUMN signCount INTEGER NOT NULL DEFAULT 0"
                    )
                }
            }

        val MIGRATION_4_5 =
            object : Migration(4, 5) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `loyalty_cards` (
                            `id` TEXT NOT NULL,
                            `displayName` TEXT NOT NULL,
                            `barcodeValue` TEXT NOT NULL,
                            `barcodeFormat` TEXT NOT NULL,
                            `createdAt` INTEGER NOT NULL,
                            `updatedAt` INTEGER NOT NULL,
                            PRIMARY KEY(`id`)
                        )
                        """.trimIndent()
                    )
                }
            }
    }
}
