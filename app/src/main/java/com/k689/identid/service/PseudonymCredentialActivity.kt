package com.k689.identid.service

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.util.Base64
import androidx.annotation.RequiresApi
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.GetCredentialResponse
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import androidx.credentials.provider.PendingIntentHandler
import com.k689.identid.controller.pseudonym.PseudonymRepository
import com.k689.identid.controller.pseudonym.PseudonymTransactionLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.koin.android.ext.android.inject
import timber.log.Timber

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class PseudonymCredentialActivity : Activity() {
    private val pseudonymRepository: PseudonymRepository by inject()
    private val transactionLogger: PseudonymTransactionLogger by inject()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val action = intent.action
        Timber.d("PseudonymCredentialActivity: action=$action")

        when (action) {
            ACTION_CREATE -> handleCreate()
            ACTION_GET -> handleGet()
            else -> {
                Timber.e("PseudonymCredentialActivity: unknown action: $action")
                setResult(RESULT_CANCELED)
                finish()
            }
        }
    }

    private fun handleCreate() {
        val providerRequest = PendingIntentHandler.retrieveProviderCreateCredentialRequest(intent)
        if (providerRequest == null) {
            Timber.e("PseudonymCredentialActivity: No create request in intent")
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        val callingRequest = providerRequest.callingRequest
        Timber.d("PseudonymCredentialActivity: Create request type=${callingRequest.type}")

        if (callingRequest !is CreatePublicKeyCredentialRequest) {
            Timber.e("PseudonymCredentialActivity: Not a PublicKey create request")
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        val requestJson = callingRequest.requestJson
        Timber.d("PseudonymCredentialActivity: requestJson=$requestJson")

        scope.launch {
            try {
                val json = JSONObject(requestJson)
                val rp = json.getJSONObject("rp")
                val rpId = rp.getString("id")
                val rpName = rp.optString("name", rpId)
                val user = json.getJSONObject("user")
                val userId = Base64.decode(
                    user.getString("id"),
                    Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP,
                )
                val userName = user.optString("name", "user")

                val challengeStr = json.getString("challenge")

                // Use clientDataHash from the caller (browser) if provided.
                // Chrome constructs its own clientDataJSON and passes the hash —
                // we MUST sign with that hash, not our own.
                val callerClientDataHash = callingRequest.clientDataHash
                val clientDataJsonBytes: ByteArray?
                val clientDataHash: ByteArray

                if (callerClientDataHash != null && callerClientDataHash.isNotEmpty()) {
                    Timber.d("PseudonymCredentialActivity: Using caller-provided clientDataHash")
                    clientDataHash = callerClientDataHash
                    clientDataJsonBytes = null // caller owns the clientDataJSON
                } else {
                    Timber.d("PseudonymCredentialActivity: Building own clientDataJSON")
                    val clientDataJson = buildClientDataJson(
                        type = "webauthn.create",
                        challenge = challengeStr,
                        origin = "https://$rpId",
                    )
                    clientDataJsonBytes = clientDataJson.toByteArray(Charsets.UTF_8)
                    clientDataHash = java.security.MessageDigest.getInstance("SHA-256")
                        .digest(clientDataJsonBytes)
                }

                val result = pseudonymRepository.createPseudonym(
                    rpId = rpId,
                    rpName = rpName,
                    userId = userId,
                    userName = userName,
                    clientDataHash = clientDataHash,
                )

                val credentialIdB64 = Base64.encodeToString(
                    result.credentialIdBytes,
                    Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP,
                )
                val attestationObjectB64 = Base64.encodeToString(
                    result.attestationObject,
                    Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP,
                )
                // If caller provided clientDataHash, use a placeholder —
                // Chrome will replace it with its own clientDataJSON.
                val clientDataJsonB64 = if (clientDataJsonBytes != null) {
                    Base64.encodeToString(
                        clientDataJsonBytes,
                        Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP,
                    )
                } else {
                    Base64.encodeToString(
                        "{}".toByteArray(Charsets.UTF_8),
                        Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP,
                    )
                }
                val authDataB64 = Base64.encodeToString(
                    result.authData,
                    Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP,
                )
                val publicKeyB64 = Base64.encodeToString(
                    result.publicKeyBytes,
                    Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP,
                )

                val responseJson = JSONObject().apply {
                    put("id", credentialIdB64)
                    put("rawId", credentialIdB64)
                    put("type", "public-key")
                    put("response", JSONObject().apply {
                        put("clientDataJSON", clientDataJsonB64)
                        put("attestationObject", attestationObjectB64)
                        put("transports", org.json.JSONArray().apply {
                            put("internal")
                        })
                        put("authenticatorData", authDataB64)
                        put("publicKey", publicKeyB64)
                        put("publicKeyAlgorithm", -7)
                    })
                    put("clientExtensionResults", JSONObject())
                    put("authenticatorAttachment", "platform")
                }

                Timber.d("PseudonymCredentialActivity: Creation successful, credId=$credentialIdB64")

                transactionLogger.logRegistration(
                    rpId = rpId,
                    rpName = rpName,
                    pseudonymId = result.pseudonym.id,
                    credentialId = result.pseudonym.credentialId,
                    userName = userName,
                )

                val credentialResponse = CreatePublicKeyCredentialResponse(responseJson.toString())

                val resultIntent = android.content.Intent()
                PendingIntentHandler.setCreateCredentialResponse(resultIntent, credentialResponse)
                setResult(RESULT_OK, resultIntent)
                finish()
            } catch (e: Exception) {
                Timber.e(e, "PseudonymCredentialActivity: Create failed")
                try {
                    transactionLogger.logRegistrationFailed(
                        rpId = intent.getStringExtra("rpId") ?: "unknown",
                        rpName = intent.getStringExtra("rpName") ?: "unknown",
                        reason = e.message ?: "Unknown error",
                    )
                } catch (_: Exception) { }
                val resultIntent = android.content.Intent()
                PendingIntentHandler.setCreateCredentialException(
                    resultIntent,
                    androidx.credentials.exceptions.CreateCredentialUnknownException(e.message),
                )
                setResult(RESULT_OK, resultIntent)
                finish()
            }
        }
    }

    private fun handleGet() {
        val request = PendingIntentHandler.retrieveProviderGetCredentialRequest(intent)
        if (request == null) {
            Timber.e("PseudonymCredentialActivity: No get request in intent")
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        val pseudonymId = intent.getStringExtra("pseudonymId")
        if (pseudonymId == null) {
            Timber.e("PseudonymCredentialActivity: No pseudonymId in intent")
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        Timber.d("PseudonymCredentialActivity: Get request, pseudonymId=$pseudonymId")

        scope.launch {
            try {
                // Find the PublicKeyCredentialRequestOptions from the request
                var requestJson: String? = null
                var callerClientDataHash: ByteArray? = null
                for (option in request.credentialOptions) {
                    if (option is GetPublicKeyCredentialOption) {
                        requestJson = option.requestJson
                        callerClientDataHash = option.clientDataHash
                        break
                    }
                }

                if (requestJson == null) {
                    Timber.e("PseudonymCredentialActivity: No request JSON in get request")
                    setResult(RESULT_CANCELED)
                    runOnUiThread { finish() }
                    return@launch
                }

                val json = JSONObject(requestJson)
                val challengeStr = json.getString("challenge")
                val rpId = json.getString("rpId")

                // Use caller-provided clientDataHash if available (browser flow)
                val clientDataJsonBytes: ByteArray?
                val clientDataHash: ByteArray

                if (callerClientDataHash != null && callerClientDataHash.isNotEmpty()) {
                    Timber.d("PseudonymCredentialActivity: Get - using caller-provided clientDataHash")
                    clientDataHash = callerClientDataHash
                    clientDataJsonBytes = null
                } else {
                    Timber.d("PseudonymCredentialActivity: Get - building own clientDataJSON")
                    val clientDataJson = buildClientDataJson(
                        type = "webauthn.get",
                        challenge = challengeStr,
                        origin = "https://$rpId",
                    )
                    clientDataJsonBytes = clientDataJson.toByteArray(Charsets.UTF_8)
                    clientDataHash = java.security.MessageDigest.getInstance("SHA-256")
                        .digest(clientDataJsonBytes)
                }

                val authResult = pseudonymRepository.authenticate(pseudonymId, clientDataHash)
                if (authResult == null) {
                    Timber.e("PseudonymCredentialActivity: Pseudonym not found: $pseudonymId")
                    setResult(RESULT_CANCELED)
                    runOnUiThread { finish() }
                    return@launch
                }

                val pseudonym = pseudonymRepository.getPseudonymById(pseudonymId)!!
                val credentialIdBytes = Base64.decode(
                    pseudonym.credentialId,
                    Base64.URL_SAFE or Base64.NO_WRAP,
                )
                val credentialIdB64 = Base64.encodeToString(
                    credentialIdBytes,
                    Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP,
                )
                val clientDataJsonB64 = if (clientDataJsonBytes != null) {
                    Base64.encodeToString(
                        clientDataJsonBytes,
                        Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP,
                    )
                } else {
                    Base64.encodeToString(
                        "{}".toByteArray(Charsets.UTF_8),
                        Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP,
                    )
                }
                val authDataB64 = Base64.encodeToString(
                    authResult.authenticatorData,
                    Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP,
                )
                val signatureB64 = Base64.encodeToString(
                    authResult.signature,
                    Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP,
                )
                val userHandleB64 = Base64.encodeToString(
                    authResult.userHandle,
                    Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP,
                )

                val responseJson = JSONObject().apply {
                    put("id", credentialIdB64)
                    put("rawId", credentialIdB64)
                    put("type", "public-key")
                    put("response", JSONObject().apply {
                        put("clientDataJSON", clientDataJsonB64)
                        put("authenticatorData", authDataB64)
                        put("signature", signatureB64)
                        put("userHandle", userHandleB64)
                    })
                    put("clientExtensionResults", JSONObject())
                    put("authenticatorAttachment", "platform")
                }

                Timber.d("PseudonymCredentialActivity: Auth successful")

                transactionLogger.logAuthentication(
                    rpId = rpId,
                    rpName = pseudonym.rpName,
                    pseudonymId = pseudonymId,
                    credentialId = pseudonym.credentialId,
                    userName = pseudonym.userName,
                )

                val credential = PublicKeyCredential(responseJson.toString())
                val credentialResponse = GetCredentialResponse(credential)

                val resultIntent = android.content.Intent()
                PendingIntentHandler.setGetCredentialResponse(resultIntent, credentialResponse)
                setResult(RESULT_OK, resultIntent)
                runOnUiThread { finish() }
            } catch (e: Exception) {
                Timber.e(e, "PseudonymCredentialActivity: Get failed")
                try {
                    transactionLogger.logAuthenticationFailed(
                        rpId = "unknown",
                        rpName = "unknown",
                        reason = e.message ?: "Unknown error",
                        pseudonymId = pseudonymId,
                    )
                } catch (_: Exception) { }
                val resultIntent = android.content.Intent()
                PendingIntentHandler.setGetCredentialException(
                    resultIntent,
                    androidx.credentials.exceptions.GetCredentialUnknownException(e.message),
                )
                setResult(RESULT_OK, resultIntent)
                runOnUiThread { finish() }
            }
        }
    }

    private fun buildClientDataJson(type: String, challenge: String, origin: String): String {
        return JSONObject().apply {
            put("type", type)
            put("challenge", challenge)
            put("origin", origin)
            put("androidPackageName", packageName)
        }.toString()
    }

    companion object {
        const val ACTION_CREATE = "com.k689.identid.CREDENTIAL_CREATE"
        const val ACTION_GET = "com.k689.identid.CREDENTIAL_GET"
    }
}
