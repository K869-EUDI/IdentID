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

package com.k689.identid.controller.transfer

import android.util.Base64
import com.k689.identid.model.transfer.TransferSessionInfo
import kotlinx.serialization.json.Json
import java.security.SecureRandom
import java.util.UUID

interface TransferSessionManager {
    fun createSession(): TransferSessionInfo

    fun encodeSessionInfo(info: TransferSessionInfo): String

    fun decodeSessionInfo(encoded: String): TransferSessionInfo

    fun getSessionKey(info: TransferSessionInfo): ByteArray
}

class TransferSessionManagerImpl : TransferSessionManager {
    private val json = Json { ignoreUnknownKeys = true }

    override fun createSession(): TransferSessionInfo {
        val sessionId = UUID.randomUUID().toString()
        val keyBytes = ByteArray(32)
        SecureRandom().nextBytes(keyBytes)
        val sessionKey = Base64.encodeToString(keyBytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)

        return TransferSessionInfo(
            sessionId = sessionId,
            sessionKey = sessionKey,
        )
    }

    override fun encodeSessionInfo(info: TransferSessionInfo): String {
        val jsonStr = json.encodeToString(TransferSessionInfo.serializer(), info)
        return Base64.encodeToString(
            jsonStr.toByteArray(Charsets.UTF_8),
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
        )
    }

    override fun decodeSessionInfo(encoded: String): TransferSessionInfo {
        val jsonBytes = Base64.decode(encoded, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        return json.decodeFromString(TransferSessionInfo.serializer(), jsonBytes.toString(Charsets.UTF_8))
    }

    override fun getSessionKey(info: TransferSessionInfo): ByteArray = Base64.decode(info.sessionKey, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
}
