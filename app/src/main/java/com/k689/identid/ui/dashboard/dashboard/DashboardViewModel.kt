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

package com.k689.identid.ui.dashboard.dashboard

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.k689.identid.R
import com.k689.identid.config.ConfigNavigation
import com.k689.identid.config.NavigationType
import com.k689.identid.config.OfferUiConfig
import com.k689.identid.config.PresentationMode
import com.k689.identid.config.RequestUriConfig
import com.k689.identid.di.common.getOrCreateCredentialOfferScope
import com.k689.identid.di.core.getOrCreatePresentationScope
import com.k689.identid.interactor.dashboard.DashboardInteractor
import com.k689.identid.interactor.dashboard.DocumentInteractorGetDocumentsPartialState
import com.k689.identid.interactor.dashboard.DocumentInteractorRetryIssuingDeferredDocumentsPartialState
import com.k689.identid.interactor.dashboard.DocumentsInteractor
import com.k689.identid.model.common.PinFlow
import com.k689.identid.model.core.DeferredDocumentDataDomain
import com.k689.identid.model.core.FormatType
import com.k689.identid.model.core.RevokedDocumentDataDomain
import com.k689.identid.navigation.CommonScreens
import com.k689.identid.navigation.DashboardScreens
import com.k689.identid.navigation.TransferScreens
import com.k689.identid.navigation.helper.DeepLinkType
import com.k689.identid.navigation.helper.generateComposableArguments
import com.k689.identid.navigation.helper.generateComposableNavigationLink
import com.k689.identid.navigation.helper.hasDeepLink
import com.k689.identid.provider.resources.ResourceProvider
import com.k689.identid.ui.component.AppIcons
import com.k689.identid.ui.component.ModalOptionUi
import com.k689.identid.ui.dashboard.component.BottomNavigationItem
import com.k689.identid.ui.dashboard.dashboard.model.SideMenuItemUi
import com.k689.identid.ui.dashboard.dashboard.model.SideMenuTypeUi
import com.k689.identid.ui.dashboard.documents.detail.model.DocumentIssuanceStateUi
import com.k689.identid.ui.dashboard.documents.list.model.DocumentUi
import com.k689.identid.ui.mvi.MviViewModel
import com.k689.identid.ui.mvi.ViewEvent
import com.k689.identid.ui.mvi.ViewSideEffect
import com.k689.identid.ui.mvi.ViewState
import com.k689.identid.ui.serializer.UiSerializer
import eu.europa.ec.eudi.wallet.document.DocumentId
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

data class State(
    // side menu
    val isSideMenuVisible: Boolean = false,
    val sideMenuTitle: String,
    val sideMenuOptions: List<SideMenuItemUi>,
    val sideMenuAnimation: SideMenuAnimation = SideMenuAnimation.SLIDE,
    val menuAnimationDuration: Int = 1500,
    val isBottomSheetOpen: Boolean = false,
    val sheetContent: DashboardBottomSheetContent =
        DashboardBottomSheetContent.DocumentRevocation(
            options = emptyList(),
        ),
    val deferredFailedDocIds: List<DocumentId> = emptyList(),
) : ViewState

sealed class Event : ViewEvent {
    data class Init(
        val deepLinkUri: Uri?,
    ) : Event()

    data object OnPause : Event()

    data object Pop : Event()

    data class DocumentRevocationNotificationReceived(
        val payload: List<RevokedDocumentDataDomain>,
    ) : Event()

    // side menu events
    sealed class SideMenu : Event() {
        data object Open : SideMenu()

        data object Close : SideMenu()

        data class ItemClicked(
            val itemType: SideMenuTypeUi,
        ) : SideMenu()
    }

    sealed class BottomSheet : Event() {
        data class UpdateBottomSheetState(
            val isOpen: Boolean,
        ) : BottomSheet()

        sealed class DocumentRevocation : BottomSheet() {
            data class OptionListItemForRevokedDocumentSelected(
                val documentId: String,
            ) : DocumentRevocation()
        }

        sealed class DeferredDocument : BottomSheet() {
            data class OptionListItemForSuccessfullyIssuingDeferredDocumentSelected(
                val documentId: DocumentId,
            ) : DeferredDocument()
        }
    }

    data class SwitchTab(val tab: BottomNavigationItem) : Event()

    data object GetDocuments : Event()

    data class TryIssuingDeferredDocuments(
        val deferredDocs: Map<DocumentId, FormatType>,
    ) : Event()
}

sealed class Effect : ViewSideEffect {
    sealed class Navigation : Effect() {
        data object Pop : Navigation()

        data class SwitchScreen(
            val screenRoute: String,
            val popUpToScreenRoute: String = DashboardScreens.Dashboard.screenRoute,
            val inclusive: Boolean = false,
        ) : Navigation()

        data class OpenDeepLinkAction(
            val deepLinkUri: Uri,
            val arguments: String?,
        ) : Navigation()

        data object OnAppSettings : Navigation()

        data object OnSystemSettings : Navigation()

        data class OpenUrlExternally(
            val url: Uri,
        ) : Navigation()
    }

    data class ShareLogFile(
        val intent: Intent,
        val chooserTitle: String,
    ) : Effect()

    data object ShowBottomSheet : Effect()

    data object CloseBottomSheet : Effect()

    data class SwitchTab(val route: String) : Effect()

    data object NotifyDeferredIssuanceRefresh : Effect()
}

sealed class DashboardBottomSheetContent {
    data class DocumentRevocation(
        val options: List<ModalOptionUi<Event>>,
    ) : DashboardBottomSheetContent()

    data class DeferredDocumentsReady(
        val successfullyIssuedDeferredDocuments: List<DeferredDocumentDataDomain>,
        val options: List<ModalOptionUi<Event>>,
    ) : DashboardBottomSheetContent()
}

enum class SideMenuAnimation {
    SLIDE,
    FADE,
}

@KoinViewModel
class DashboardViewModel(
    private val dashboardInteractor: DashboardInteractor,
    private val documentsInteractor: DocumentsInteractor,
    private val uiSerializer: UiSerializer,
    private val resourceProvider: ResourceProvider,
) : MviViewModel<Event, State, Effect>() {
    private var retryDeferredDocsJob: Job? = null

    override fun setInitialState(): State =
        State(
            sideMenuTitle = resourceProvider.getString(R.string.dashboard_side_menu_title),
            sideMenuOptions = dashboardInteractor.getSideMenuOptions(),
        )

    override fun handleEvents(event: Event) {
        when (event) {
            is Event.Init -> {
                handleDeepLink(event.deepLinkUri)
                setEvent(Event.GetDocuments)
            }

            is Event.OnPause -> {
                retryDeferredDocsJob?.cancel()
            }

            is Event.Pop -> {
                setEffect { Effect.Navigation.Pop }
            }

            is Event.SideMenu.ItemClicked -> {
                handleSideMenuItemClicked(event.itemType)
            }

            is Event.SideMenu.Close -> {
                setState {
                    copy(
                        isSideMenuVisible = false,
                        sideMenuAnimation = SideMenuAnimation.SLIDE,
                    )
                }
            }

            is Event.SideMenu.Open -> {
                setState {
                    copy(
                        isSideMenuVisible = true,
                        sideMenuAnimation = SideMenuAnimation.SLIDE,
                    )
                }
            }

            is Event.DocumentRevocationNotificationReceived -> {
                showBottomSheet(
                    sheetContent =
                        DashboardBottomSheetContent.DocumentRevocation(
                            options = getDocumentRevocationBottomSheetOptions(event.payload),
                        ),
                )
            }

            is Event.BottomSheet.UpdateBottomSheetState -> {
                setState {
                    copy(isBottomSheetOpen = event.isOpen)
                }
            }

            is Event.BottomSheet.DocumentRevocation.OptionListItemForRevokedDocumentSelected -> {
                hideBottomSheet()
                goToDocumentDetails(docId = event.documentId)
            }

            is Event.BottomSheet.DeferredDocument.OptionListItemForSuccessfullyIssuingDeferredDocumentSelected -> {
                hideBottomSheet()
                goToDocumentDetails(docId = event.documentId)
            }

            is Event.SwitchTab -> {
                setEffect { Effect.SwitchTab(event.tab.route) }
            }

            is Event.GetDocuments -> {
                getDocuments()
            }

            is Event.TryIssuingDeferredDocuments -> {
                tryIssuingDeferredDocuments(event.deferredDocs)
            }
        }
    }

    private fun getDocuments() {
        viewModelScope.launch {
            documentsInteractor
                .getDocuments()
                .collect { response ->
                    if (response is DocumentInteractorGetDocumentsPartialState.Success) {
                        val deferredDocs: MutableMap<DocumentId, FormatType> = mutableMapOf()
                        response.allDocuments.items
                            .filter { document ->
                                with(document.payload as DocumentUi) {
                                    documentIssuanceState == DocumentIssuanceStateUi.Pending &&
                                        uiData.itemId !in viewState.value.deferredFailedDocIds
                                }
                            }.forEach { documentItem ->
                                with(documentItem.payload as DocumentUi) {
                                    deferredDocs[uiData.itemId] =
                                        documentIdentifier.formatType
                                }
                            }

                        if (deferredDocs.isNotEmpty()) {
                            setEvent(Event.TryIssuingDeferredDocuments(deferredDocs))
                        }
                    }
                }
        }
    }

    private fun tryIssuingDeferredDocuments(deferredDocs: Map<DocumentId, FormatType>) {
        retryDeferredDocsJob?.cancel()
        retryDeferredDocsJob =
            viewModelScope.launch {
                if (deferredDocs.isEmpty()) {
                    return@launch
                }

                delay(5000L)

                documentsInteractor.tryIssuingDeferredDocumentsFlow(deferredDocs).collect { response ->
                    when (response) {
                        is DocumentInteractorRetryIssuingDeferredDocumentsPartialState.Result -> {
                            val successDocs = response.successfullyIssuedDeferredDocuments
                            if (successDocs.isNotEmpty() &&
                                (
                                    !viewState.value.isBottomSheetOpen ||
                                        (
                                            viewState.value.isBottomSheetOpen &&
                                                viewState.value.sheetContent !is DashboardBottomSheetContent.DeferredDocumentsReady
                                        )
                                )
                            ) {
                                showBottomSheet(
                                    sheetContent =
                                        DashboardBottomSheetContent.DeferredDocumentsReady(
                                            successfullyIssuedDeferredDocuments = successDocs,
                                            options =
                                                getDeferredBottomSheetOptions(
                                                    deferredDocumentsData = successDocs,
                                                ),
                                        ),
                                )
                            }

                            if (successDocs.isNotEmpty() || response.failedIssuedDeferredDocuments.isNotEmpty()) {
                                setState {
                                    copy(deferredFailedDocIds = response.failedIssuedDeferredDocuments)
                                }
                                setEffect { Effect.NotifyDeferredIssuanceRefresh }
                            }

                            // Re-check after 10 seconds to continue polling for remaining NotReady docs
                            delay(10000L)
                            setEvent(Event.GetDocuments)
                        }

                        is DocumentInteractorRetryIssuingDeferredDocumentsPartialState.Failure -> {
                            // On generic failure (e.g. network), retry again after 30 seconds
                            delay(30000L)
                            setEvent(Event.GetDocuments)
                        }
                    }
                }
            }
    }

    private fun getDeferredBottomSheetOptions(deferredDocumentsData: List<DeferredDocumentDataDomain>): List<ModalOptionUi<Event>> =
        deferredDocumentsData.map {
            ModalOptionUi(
                title = it.docName,
                trailingIcon = AppIcons.KeyboardArrowRight,
                event =
                    Event.BottomSheet.DeferredDocument.OptionListItemForSuccessfullyIssuingDeferredDocumentSelected(
                        documentId = it.documentId,
                    ),
            )
        }

    private fun goToDocumentDetails(docId: DocumentId) {
        setEffect {
            Effect.Navigation.SwitchScreen(
                screenRoute =
                    generateComposableNavigationLink(
                        screen = DashboardScreens.DocumentDetails,
                        arguments =
                            generateComposableArguments(
                                mapOf(
                                    "documentId" to docId,
                                ),
                            ),
                    ),
            )
        }
    }

    private fun showBottomSheet(sheetContent: DashboardBottomSheetContent) {
        setState {
            copy(
                sheetContent = sheetContent,
            )
        }
        setEffect {
            Effect.ShowBottomSheet
        }
    }

    private fun hideBottomSheet() {
        setEffect {
            Effect.CloseBottomSheet
        }
    }

    private fun hideSideMenu() {
        setState {
            copy(
                isSideMenuVisible = false,
                sideMenuAnimation = SideMenuAnimation.SLIDE,
            )
        }
    }

    private fun getDocumentRevocationBottomSheetOptions(revokedDocumentData: List<RevokedDocumentDataDomain>): List<ModalOptionUi<Event>> =
        revokedDocumentData.map {
            ModalOptionUi(
                title = it.name,
                trailingIcon = AppIcons.KeyboardArrowRight,
                event =
                    Event.BottomSheet.DocumentRevocation.OptionListItemForRevokedDocumentSelected(
                        documentId = it.id,
                    ),
            )
        }

    private fun handleSideMenuItemClicked(itemType: SideMenuTypeUi) {
        when (itemType) {
            SideMenuTypeUi.SETTINGS -> {
                hideSideMenu()
                setEffect { Effect.Navigation.SwitchScreen(screenRoute = DashboardScreens.Settings.screenRoute) }
            }

            SideMenuTypeUi.CHANGE_PIN -> {
                hideSideMenu()
                val nextScreenRoute =
                    generateComposableNavigationLink(
                        screen = CommonScreens.QuickPin,
                        arguments =
                            generateComposableArguments(
                                mapOf("pinFlow" to PinFlow.UPDATE),
                            ),
                    )
                setEffect { Effect.Navigation.SwitchScreen(screenRoute = nextScreenRoute) }
            }

            SideMenuTypeUi.MOVE_WALLET -> {
                hideSideMenu()
                setEffect { Effect.Navigation.SwitchScreen(screenRoute = TransferScreens.MoveWallet.screenRoute) }
            }

            SideMenuTypeUi.RECEIVE_WALLET -> {
                hideSideMenu()
                setEffect { Effect.Navigation.SwitchScreen(screenRoute = TransferScreens.ReceiveWallet.screenRoute) }
            }
        }
    }

    private fun handleDeepLink(deepLinkUri: Uri?) {
        deepLinkUri?.let { uri ->
            hasDeepLink(uri)?.let {
                val arguments: String? =
                    when (it.type) {
                        DeepLinkType.OPENID4VP -> {
                            getOrCreatePresentationScope()
                            generateComposableArguments(
                                mapOf(
                                    RequestUriConfig.serializedKeyName to
                                        uiSerializer.toBase64(
                                            RequestUriConfig(
                                                PresentationMode.OpenId4Vp(
                                                    uri.toString(),
                                                    DashboardScreens.Dashboard.screenRoute,
                                                ),
                                            ),
                                            RequestUriConfig.Parser,
                                        ),
                                ),
                            )
                        }

                        DeepLinkType.CREDENTIAL_OFFER -> {
                            getOrCreateCredentialOfferScope()
                            generateComposableArguments(
                                mapOf(
                                    OfferUiConfig.serializedKeyName to
                                        uiSerializer.toBase64(
                                            OfferUiConfig(
                                                offerUri = it.link.toString(),
                                                onSuccessNavigation =
                                                    ConfigNavigation(
                                                        navigationType =
                                                            NavigationType.PopTo(
                                                                screen = DashboardScreens.Dashboard,
                                                            ),
                                                    ),
                                                onCancelNavigation =
                                                    ConfigNavigation(
                                                        navigationType = NavigationType.Pop,
                                                    ),
                                            ),
                                            OfferUiConfig.Parser,
                                        ),
                                ),
                            )
                        }

                        else -> {
                            null
                        }
                    }
                setEffect {
                    Effect.Navigation.OpenDeepLinkAction(
                        deepLinkUri = uri,
                        arguments = arguments,
                    )
                }
            }
        }
    }
}
