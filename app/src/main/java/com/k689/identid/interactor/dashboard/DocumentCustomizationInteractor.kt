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

package com.k689.identid.interactor.dashboard

import com.k689.identid.model.storage.DocumentCustomization
import com.k689.identid.storage.dao.DocumentCustomizationDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.koin.core.annotation.Factory

sealed class DocumentCustomizationPartialState {
    data class Success(
        val customization: DocumentCustomization?,
    ) : DocumentCustomizationPartialState()

    data class Failure(
        val errorMessage: String,
    ) : DocumentCustomizationPartialState()
}

interface DocumentCustomizationInteractor {
    fun getCustomization(documentId: String): Flow<DocumentCustomizationPartialState>

    fun saveCustomization(customization: DocumentCustomization): Flow<DocumentCustomizationPartialState>

    fun deleteCustomization(documentId: String): Flow<DocumentCustomizationPartialState>
}

@Factory
class DocumentCustomizationInteractorImpl(
    private val documentCustomizationDao: DocumentCustomizationDao,
) : DocumentCustomizationInteractor {
    override fun getCustomization(documentId: String): Flow<DocumentCustomizationPartialState> =
        flow {
            try {
                val customization = documentCustomizationDao.retrieve(documentId)
                emit(DocumentCustomizationPartialState.Success(customization))
            } catch (e: Exception) {
                emit(DocumentCustomizationPartialState.Failure(e.message ?: "Unknown error"))
            }
        }

    override fun saveCustomization(customization: DocumentCustomization): Flow<DocumentCustomizationPartialState> =
        flow {
            try {
                documentCustomizationDao.store(customization)
                emit(DocumentCustomizationPartialState.Success(customization))
            } catch (e: Exception) {
                emit(DocumentCustomizationPartialState.Failure(e.message ?: "Unknown error"))
            }
        }

    override fun deleteCustomization(documentId: String): Flow<DocumentCustomizationPartialState> =
        flow {
            try {
                documentCustomizationDao.delete(documentId)
                emit(DocumentCustomizationPartialState.Success(null))
            } catch (e: Exception) {
                emit(DocumentCustomizationPartialState.Failure(e.message ?: "Unknown error"))
            }
        }
}
