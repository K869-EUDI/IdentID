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

package com.k689.identid.ui.transfer.send

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.k689.identid.controller.core.WalletCoreDocumentsController
import com.k689.identid.controller.storage.PinStorageController
import com.k689.identid.interactor.transfer.MoveWalletInteractor
import com.k689.identid.model.transfer.TransferableDocument
import com.k689.identid.navigation.DashboardScreens
import com.k689.identid.ui.component.content.ContentErrorConfig
import com.k689.identid.ui.mvi.MviViewModel
import com.k689.identid.ui.mvi.ViewEvent
import com.k689.identid.ui.mvi.ViewSideEffect
import com.k689.identid.ui.mvi.ViewState
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

data class MoveWalletApprovalState(
    val isLoading: Boolean = true,
    val error: ContentErrorConfig? = null,
    val documents: List<TransferableDocument> = emptyList(),
    val pinInput: String = "",
    val pinError: Boolean = false,
    val isSending: Boolean = false,
    val isSent: Boolean = false,
) : ViewState

sealed class MoveWalletApprovalEvent : ViewEvent {
    data class Init(
        val context: Context,
    ) : MoveWalletApprovalEvent()

    data class GoBack(
        val context: Context,
    ) : MoveWalletApprovalEvent()

    data class PinChanged(
        val pin: String,
    ) : MoveWalletApprovalEvent()

    data class ConfirmTransfer(
        val context: Context,
    ) : MoveWalletApprovalEvent()
}

sealed class MoveWalletApprovalEffect : ViewSideEffect {
    sealed class Navigation : MoveWalletApprovalEffect() {
        data object Pop : Navigation()

        data class NavigateToDashboard(
            val screenRoute: String,
        ) : Navigation()
    }
}

@KoinViewModel
class MoveWalletApprovalViewModel(
    private val interactor: MoveWalletInteractor,
    private val walletCoreDocumentsController: WalletCoreDocumentsController,
    private val pinStorageController: PinStorageController,
) : MviViewModel<MoveWalletApprovalEvent, MoveWalletApprovalState, MoveWalletApprovalEffect>() {
    override fun setInitialState(): MoveWalletApprovalState = MoveWalletApprovalState()

    override fun handleEvents(event: MoveWalletApprovalEvent) {
        when (event) {
            is MoveWalletApprovalEvent.Init -> {
                loadDocuments()
            }

            is MoveWalletApprovalEvent.GoBack -> {
                interactor.stopTransfer(event.context)
                setEffect { MoveWalletApprovalEffect.Navigation.Pop }
            }

            is MoveWalletApprovalEvent.PinChanged -> {
                setState { copy(pinInput = event.pin, pinError = false) }
            }

            is MoveWalletApprovalEvent.ConfirmTransfer -> {
                confirmAndSend(event.context)
            }
        }
    }

    private fun loadDocuments() {
        viewModelScope.launch {
            val locale = java.util.Locale.getDefault()

            // Only show documents that the user actually has issued, are not revoked, and are not expired
            val revokedIds = walletCoreDocumentsController.getRevokedDocumentIds().toSet()
            val now = java.time.Instant.now()
            val issuedFormats =
                walletCoreDocumentsController
                    .getAllIssuedDocuments()
                    .filter { doc ->
                        doc.id !in revokedIds &&
                            (doc.getValidUntil().getOrNull()?.isAfter(now) != false)
                    }.mapNotNull { doc ->
                        when (doc.format) {
                            is eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat -> {
                                (doc.format as eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat).docType
                            }

                            is eu.europa.ec.eudi.wallet.document.format.SdJwtVcFormat -> {
                                (doc.format as eu.europa.ec.eudi.wallet.document.format.SdJwtVcFormat).vct
                            }
                        }
                    }.toSet()

            val docs =
                when (
                    val result = walletCoreDocumentsController.getScopedDocuments(locale)
                ) {
                    is com.k689.identid.controller.core.FetchScopedDocumentsPartialState.Success -> {
                        result.documents
                            .filter { doc -> doc.formatType in issuedFormats }
                            .sortedBy { doc -> if (doc.configurationId.contains("deferred")) 1 else 0 }
                            .distinctBy { doc -> doc.formatType }
                            .map { doc ->
                                TransferableDocument(
                                    name = doc.name,
                                    configurationId = doc.configurationId,
                                    credentialIssuerId = doc.credentialIssuerId,
                                    formatType = doc.formatType,
                                    isPid = doc.isPid,
                                )
                            }
                    }

                    else -> {
                        emptyList()
                    }
                }
            setState { copy(isLoading = false, documents = docs) }
        }
    }

    private fun confirmAndSend(context: Context) {
        val currentPin = viewState.value.pinInput
        if (!pinStorageController.isPinValid(currentPin)) {
            setState { copy(pinError = true) }
            return
        }

        setState { copy(isSending = true, pinError = false) }

        viewModelScope.launch {
            try {
                val endpointId = interactor.getConnectedEndpointId()
                if (endpointId != null) {
                    interactor.encryptAndSendData(context, endpointId)
                    setState { copy(isSending = false, isSent = true) }
                    setEffect {
                        MoveWalletApprovalEffect.Navigation.NavigateToDashboard(
                            DashboardScreens.Dashboard.screenRoute,
                        )
                    }
                } else {
                    setState {
                        copy(
                            isSending = false,
                            error =
                                ContentErrorConfig(
                                    errorSubTitle = "No active connection. Please try again.",
                                    onRetry = { setEvent(MoveWalletApprovalEvent.Init(context)) },
                                    onCancel = { setEffect { MoveWalletApprovalEffect.Navigation.Pop } },
                                ),
                        )
                    }
                }
            } catch (e: Exception) {
                setState {
                    copy(
                        isSending = false,
                        error =
                            ContentErrorConfig(
                                errorSubTitle = e.localizedMessage ?: "Transfer failed",
                                onRetry = { setEvent(MoveWalletApprovalEvent.ConfirmTransfer(context)) },
                                onCancel = { setEffect { MoveWalletApprovalEffect.Navigation.Pop } },
                            ),
                    )
                }
            }
        }
    }
}
