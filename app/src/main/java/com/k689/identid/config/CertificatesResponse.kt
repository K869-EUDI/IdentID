package com.k689.identid.config

import kotlinx.serialization.Serializable

@Serializable
data class CertificatesResponse(
    val version: Int,
    val certificates: List<CertificateEntry>,
)

@Serializable
data class CertificateEntry(
    val name: String,
    val usage: List<String>,
    val pem: String,
) {
    companion object {
        const val USAGE_READER_TRUST_STORE = "reader_trust_store"
        const val USAGE_RQES_DOCUMENT_RETRIEVAL = "rqes_document_retrieval"
    }
}
