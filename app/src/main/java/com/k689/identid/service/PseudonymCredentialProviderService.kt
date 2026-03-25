package com.k689.identid.service

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.CancellationSignal
import android.os.OutcomeReceiver
import androidx.annotation.RequiresApi
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.ClearCredentialUnknownException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.CreateCredentialUnknownException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.provider.BeginCreateCredentialRequest
import androidx.credentials.provider.BeginCreateCredentialResponse
import androidx.credentials.provider.BeginCreatePublicKeyCredentialRequest
import androidx.credentials.provider.BeginGetCredentialRequest
import androidx.credentials.provider.BeginGetCredentialResponse
import androidx.credentials.provider.CreateEntry
import androidx.credentials.provider.CredentialProviderService
import androidx.credentials.provider.ProviderClearCredentialStateRequest
import androidx.credentials.provider.PublicKeyCredentialEntry
import com.k689.identid.R
import com.k689.identid.controller.pseudonym.PseudonymRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.koin.android.ext.android.inject
import timber.log.Timber

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class PseudonymCredentialProviderService : CredentialProviderService() {
    private val pseudonymRepository: PseudonymRepository by inject()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBeginCreateCredentialRequest(
        request: BeginCreateCredentialRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<BeginCreateCredentialResponse, CreateCredentialException>,
    ) {
        Timber.d("onBeginCreateCredentialRequest: type=${request.javaClass.simpleName}")

        if (request !is BeginCreatePublicKeyCredentialRequest) {
            Timber.w("onBeginCreateCredentialRequest: Unsupported type, returning error")
            callback.onError(CreateCredentialUnknownException("Unsupported request type"))
            return
        }

        Timber.d("onBeginCreateCredentialRequest: Building CreateEntry")

        val createEntry = CreateEntry.Builder(
            getString(R.string.pseudonym_credential_provider_name),
            createNewPendingIntent(PseudonymCredentialActivity.ACTION_CREATE),
        ).setDescription(getString(R.string.pseudonym_credential_provider_description))
            .build()

        callback.onResult(
            BeginCreateCredentialResponse.Builder()
                .addCreateEntry(createEntry)
                .build(),
        )

        Timber.d("onBeginCreateCredentialRequest: Response sent with 1 entry")
    }

    override fun onBeginGetCredentialRequest(
        request: BeginGetCredentialRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<BeginGetCredentialResponse, GetCredentialException>,
    ) {
        Timber.d("onBeginGetCredentialRequest: options=${request.beginGetCredentialOptions.size}")

        val job = serviceScope.launch {
            try {
                val responseBuilder = BeginGetCredentialResponse.Builder()
                var hasEntries = false

                for (option in request.beginGetCredentialOptions) {
                    if (option !is androidx.credentials.provider.BeginGetPublicKeyCredentialOption) {
                        Timber.d("onBeginGetCredentialRequest: Skipping non-PublicKey option")
                        continue
                    }

                    val json = JSONObject(option.requestJson)
                    val rpId = json.optString("rpId", "")
                    Timber.d("onBeginGetCredentialRequest: rpId=$rpId")
                    if (rpId.isEmpty()) continue

                    val pseudonyms = pseudonymRepository.getPseudonymsForRp(rpId)
                    Timber.d("onBeginGetCredentialRequest: Found ${pseudonyms.size} pseudonyms for rpId=$rpId")

                    for (pseudonym in pseudonyms) {
                        val displayName = pseudonym.userAlias ?: pseudonym.userName
                        val entry = PublicKeyCredentialEntry.Builder(
                            this@PseudonymCredentialProviderService,
                            displayName,
                            createNewPendingIntent(
                                PseudonymCredentialActivity.ACTION_GET,
                                pseudonym.id,
                            ),
                            option,
                        ).build()
                        responseBuilder.addCredentialEntry(entry)
                        hasEntries = true
                    }
                }

                Timber.d("onBeginGetCredentialRequest: hasEntries=$hasEntries")

                callback.onResult(responseBuilder.build())
            } catch (e: Exception) {
                Timber.e(e, "onBeginGetCredentialRequest: Failed")
                callback.onError(GetCredentialUnknownException(e.message))
            }
        }
        cancellationSignal.setOnCancelListener { job.cancel() }
    }

    override fun onClearCredentialStateRequest(
        request: ProviderClearCredentialStateRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<Void?, ClearCredentialException>,
    ) {
        Timber.d("onClearCredentialStateRequest")
        val job = serviceScope.launch {
            try {
                pseudonymRepository.deleteAllPseudonyms()
                callback.onResult(null)
            } catch (e: Exception) {
                Timber.e(e, "onClearCredentialStateRequest: Failed")
                callback.onError(ClearCredentialUnknownException(e.message))
            }
        }
        cancellationSignal.setOnCancelListener { job.cancel() }
    }

    private fun createNewPendingIntent(action: String, pseudonymId: String? = null): PendingIntent {
        val intent = Intent(this, PseudonymCredentialActivity::class.java).apply {
            this.action = action
            pseudonymId?.let { putExtra("pseudonymId", it) }
        }
        val flags = PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        return PendingIntent.getActivity(
            this,
            action.hashCode() + (pseudonymId?.hashCode() ?: 0),
            intent,
            flags,
        )
    }
}
