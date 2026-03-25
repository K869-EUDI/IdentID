package com.k689.identid.config

import android.content.Context
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.ByteArrayInputStream
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

class CertificateRepository(
    private val context: Context,
) {
    private val json = Json { ignoreUnknownKeys = true }

    private val httpClient by lazy {
        HttpClient(Android) {
            install(ContentNegotiation) {
                json(json = json, contentType = ContentType.Application.Json)
            }
        }
    }
    private var cachedReaderTrustCerts: List<X509Certificate>? = null
    private var cachedRqesCerts: List<X509Certificate>? = null

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getReaderTrustStoreCerts(): List<X509Certificate> {
        cachedReaderTrustCerts?.let { return it }
        val response = loadCachedResponse()
        return if (response != null) {
            parseCertsByUsage(response, CertificateEntry.USAGE_READER_TRUST_STORE).also {
                cachedReaderTrustCerts = it
            }
        } else {
            loadBundledReaderTrustCerts()
        }
    }

    fun getRqesDocumentRetrievalCerts(): List<X509Certificate> {
        cachedRqesCerts?.let { return it }
        val response = loadCachedResponse()
        return if (response != null) {
            parseCertsByUsage(response, CertificateEntry.USAGE_RQES_DOCUMENT_RETRIEVAL).also {
                cachedRqesCerts = it
            }
        } else {
            loadBundledRqesCerts()
        }
    }

    suspend fun refreshFromServer() {
        try {
            val responseText = httpClient.get(CERTS_URL).bodyAsText()
            // Validate it parses before caching
            json.decodeFromString<CertificatesResponse>(responseText)
            prefs.edit().putString(KEY_CERTS_JSON, responseText).apply()
            // Clear in-memory cache to force reload from new data
            cachedReaderTrustCerts = null
            cachedRqesCerts = null
            Timber.d("Certificates refreshed from server")
        } catch (e: Exception) {
            Timber.w(e, "Failed to refresh certificates from server")
        }
    }

    private fun loadCachedResponse(): CertificatesResponse? {
        val cached = prefs.getString(KEY_CERTS_JSON, null) ?: return null
        return try {
            json.decodeFromString<CertificatesResponse>(cached)
        } catch (e: Exception) {
            Timber.w(e, "Failed to parse cached certificates")
            null
        }
    }

    private fun parseCertsByUsage(response: CertificatesResponse, usage: String): List<X509Certificate> {
        val factory = CertificateFactory.getInstance("X.509")
        return response.certificates
            .filter { usage in it.usage }
            .flatMap { entry -> parsePemCertificates(factory, entry.pem) }
    }

    private fun parsePemCertificates(factory: CertificateFactory, pem: String): List<X509Certificate> {
        return try {
            val inputStream = ByteArrayInputStream(pem.toByteArray())
            factory.generateCertificates(inputStream)
                .filterIsInstance<X509Certificate>()
        } catch (e: Exception) {
            Timber.w(e, "Failed to parse PEM certificate")
            emptyList()
        }
    }

    private fun loadBundledReaderTrustCerts(): List<X509Certificate> {
        val factory = CertificateFactory.getInstance("X.509")
        return BUNDLED_READER_TRUST_RESOURCES.mapNotNull { resId ->
            loadBundledCert(factory, resId)
        }
    }

    private fun loadBundledRqesCerts(): List<X509Certificate> {
        val factory = CertificateFactory.getInstance("X.509")
        return BUNDLED_RQES_RESOURCES.mapNotNull { resId ->
            loadBundledCert(factory, resId)
        }
    }

    private fun loadBundledCert(factory: CertificateFactory, resId: Int): X509Certificate? {
        return try {
            context.resources.openRawResource(resId).use { stream ->
                factory.generateCertificate(stream) as? X509Certificate
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to load bundled certificate")
            null
        }
    }

    companion object {
        private const val CERTS_URL = "https://pid.linux123123.com/certs.json"
        private const val PREFS_NAME = "certificate_cache"
        private const val KEY_CERTS_JSON = "certs_json"

        private val BUNDLED_READER_TRUST_RESOURCES = listOf(
            com.k689.identid.R.raw.pidissuerca02_cz,
            com.k689.identid.R.raw.pidissuerca02_ee,
            com.k689.identid.R.raw.pidissuerca02_eu,
            com.k689.identid.R.raw.pidissuerca02_lt,
            com.k689.identid.R.raw.pidissuerca02_lu,
            com.k689.identid.R.raw.pidissuerca02_nl,
            com.k689.identid.R.raw.pidissuerca02_pt,
            com.k689.identid.R.raw.pidissuerca02_ut,
            com.k689.identid.R.raw.dc4eu,
            com.k689.identid.R.raw.r45_staging,
            com.k689.identid.R.raw.verifier,
        )

        private val BUNDLED_RQES_RESOURCES = listOf(
            com.k689.identid.R.raw.pidissuerca02_cz,
            com.k689.identid.R.raw.pidissuerca02_ee,
            com.k689.identid.R.raw.pidissuerca02_eu,
            com.k689.identid.R.raw.pidissuerca02_lt,
            com.k689.identid.R.raw.pidissuerca02_lu,
            com.k689.identid.R.raw.pidissuerca02_nl,
            com.k689.identid.R.raw.pidissuerca02_pt,
            com.k689.identid.R.raw.pidissuerca02_ut,
        )
    }
}
