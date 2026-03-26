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

package com.k689.identid.config

import com.k689.identid.BuildConfig
import eu.europa.ec.eudi.rqes.HashAlgorithmOID
import eu.europa.ec.eudi.rqes.core.documentRetrieval.X509CertificateTrust
import eu.europa.ec.eudi.rqesui.domain.extension.toUriOrEmpty
import eu.europa.ec.eudi.rqesui.infrastructure.config.DocumentRetrievalConfig
import eu.europa.ec.eudi.rqesui.infrastructure.config.EudiRQESUiConfig
import eu.europa.ec.eudi.rqesui.infrastructure.config.data.QtspData
import java.net.URI

class RQESConfigImpl(
    private val certificateRepository: CertificateRepository,
) : EudiRQESUiConfig {
    override val qtsps: List<QtspData>
        get() =
            listOf(
                QtspData(
                    name = "Wallet-Centric",
                    endpoint = "https://qtsp.linux123123.com/csc/v2".toUriOrEmpty(),
                    tsaUrl = "https://timestamp.sectigo.com/qualified",
                    clientId = "wallet-client",
                    clientSecret = "wallet-client-secret",
                    authFlowRedirectionURI = URI.create(BuildConfig.RQES_DEEPLINK),
                    hashAlgorithm = HashAlgorithmOID.SHA_256,
                ),
            )

    override val printLogs: Boolean get() = BuildConfig.DEBUG

    override val documentRetrievalConfig: DocumentRetrievalConfig
        get() {
            val certs = certificateRepository.getRqesDocumentRetrievalCerts()
            return if (certs.isNotEmpty()) {
                DocumentRetrievalConfig.X509CertificateImpl(
                    X509CertificateTrust(
                        trustedCertificates = certs,
                        logException = if (BuildConfig.DEBUG) {
                            { th -> th.printStackTrace() }
                        } else {
                            null
                        },
                    ),
                )
            } else {
                DocumentRetrievalConfig.NoValidation
            }
        }
}
