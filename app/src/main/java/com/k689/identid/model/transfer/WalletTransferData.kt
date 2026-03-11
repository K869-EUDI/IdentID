/*
 * Copyright (c) 2025 European Commission
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

package com.k689.identid.model.transfer

import kotlinx.serialization.Serializable

@Serializable
data class WalletTransferData(
    val version: Int = 1,
    val documents: List<TransferableDocument>,
    val transactionLogs: List<TransferableTransactionLog>,
    val bookmarks: List<String>,
    val revokedDocuments: List<String>,
)

@Serializable
data class TransferableDocument(
    val name: String,
    val configurationId: String,
    val credentialIssuerId: String,
    val formatType: String?,
    val isPid: Boolean,
)

@Serializable
data class TransferableTransactionLog(
    val identifier: String,
    val value: String,
)

@Serializable
data class TransferSessionInfo(
    val sessionId: String,
    val sessionKey: String,
)

@Serializable
data class WalletTransferEnvelope(
    val protocolVersion: Int = 1,
    val sessionId: String? = null,
    val encryptedPayloadBase64: String,
)
