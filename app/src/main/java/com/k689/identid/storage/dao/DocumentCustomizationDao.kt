/*
 * Copyright (c) 2026 European Commission
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

package com.k689.identid.storage.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.k689.identid.model.storage.DocumentCustomization
import com.k689.identid.storage.dao.type.StorageDao

@Dao
interface DocumentCustomizationDao : StorageDao<DocumentCustomization> {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    override suspend fun store(value: DocumentCustomization)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    override suspend fun storeAll(values: List<DocumentCustomization>)

    @Query("SELECT * FROM document_customizations WHERE identifier = :identifier")
    override suspend fun retrieve(identifier: String): DocumentCustomization?

    @Query("SELECT * FROM document_customizations")
    override suspend fun retrieveAll(): List<DocumentCustomization>

    @Update
    override suspend fun update(value: DocumentCustomization)

    @Query("DELETE FROM document_customizations WHERE identifier = :identifier")
    override suspend fun delete(identifier: String)

    @Query("DELETE FROM document_customizations")
    override suspend fun deleteAll()
}
