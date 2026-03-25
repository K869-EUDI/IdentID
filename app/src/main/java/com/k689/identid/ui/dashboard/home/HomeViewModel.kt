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

package com.k689.identid.ui.dashboard.home

import androidx.lifecycle.viewModelScope
import com.k689.identid.R
import com.k689.identid.config.IssuanceFlowType
import com.k689.identid.config.IssuanceUiConfig
import com.k689.identid.config.PresentationMode
import com.k689.identid.config.QrScanFlow
import com.k689.identid.config.QrScanUiConfig
import com.k689.identid.config.RequestUriConfig
import com.k689.identid.di.core.getOrCreatePresentationScope
import com.k689.identid.interactor.dashboard.DocumentInteractorGetDocumentsPartialState
import com.k689.identid.interactor.dashboard.DocumentsInteractor
import com.k689.identid.interactor.dashboard.HomeInteractor
import com.k689.identid.interactor.dashboard.HomeInteractorGetUserNameViaMainPidDocumentPartialState
import com.k689.identid.interactor.dashboard.TransactionInteractorGetTransactionsPartialState
import com.k689.identid.interactor.dashboard.TransactionsInteractor
import com.k689.identid.navigation.CommonScreens
import com.k689.identid.navigation.DashboardScreens
import com.k689.identid.navigation.IssuanceScreens
import com.k689.identid.navigation.ProximityScreens
import com.k689.identid.navigation.helper.generateComposableArguments
import com.k689.identid.navigation.helper.generateComposableNavigationLink
import com.k689.identid.provider.resources.ResourceProvider
import com.k689.identid.ui.component.AppIcons
import com.k689.identid.ui.component.ListItemTrailingContentDataUi
import com.k689.identid.ui.component.wrap.ActionCardConfig
import com.k689.identid.ui.dashboard.component.BottomNavigationItem
import com.k689.identid.ui.dashboard.documents.detail.model.DocumentIssuanceStateUi
import com.k689.identid.ui.dashboard.documents.list.model.DocumentUi
import com.k689.identid.ui.dashboard.documents.list.model.DocumentsFilterableAttributes
import com.k689.identid.ui.dashboard.home.HomeScreenBottomSheetContent.Bluetooth
import com.k689.identid.ui.dashboard.transactions.list.model.TransactionUi
import com.k689.identid.ui.mvi.MviViewModel
import com.k689.identid.ui.mvi.ViewEvent
import com.k689.identid.ui.mvi.ViewSideEffect
import com.k689.identid.ui.mvi.ViewState
import com.k689.identid.ui.serializer.UiSerializer
import com.k689.identid.util.business.formatInstant
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class BleAvailability {
    AVAILABLE,
    NO_PERMISSION,
    DISABLED,
    UNKNOWN,
}

data class DashboardDocument(
    val documentUi: DocumentUi,
    val usagesLeft: String = "-",
    val expiresAt: String = "-",
    val isPending: Boolean = false,
)

data class DashboardTransaction(
    val transactionUi: TransactionUi,
    val isPending: Boolean = false,
)

data class State(
    val isLoading: Boolean = false,
    val isBottomSheetOpen: Boolean = false,
    val sheetContent: HomeScreenBottomSheetContent = HomeScreenBottomSheetContent.None,
    val welcomeUserMessage: String,
    val authenticateCardConfig: ActionCardConfig,
    val signCardConfig: ActionCardConfig,
    val bleAvailability: BleAvailability = BleAvailability.UNKNOWN,
    val isBleCentralClientModeEnabled: Boolean = false,
    val allDocuments: List<DashboardDocument> = emptyList(),
    val allTransactions: List<DashboardTransaction> = emptyList(),
) : ViewState

sealed class Event : ViewEvent {
    data object Init : Event()

    data object StartProximityFlow : Event()

    data object AddDocumentsClicked : Event()

    sealed class AuthenticateCard : Event() {
        data object AuthenticatePressed : Event()

        data object LearnMorePressed : Event()
    }

    sealed class SignDocumentCard : Event() {
        data object SignDocumentPressed : Event()

        data object LearnMorePressed : Event()
    }

    sealed class BottomSheet : Event() {
        data class UpdateBottomSheetState(
            val isOpen: Boolean,
        ) : BottomSheet()

        data object Close : BottomSheet()

        sealed class Authenticate : BottomSheet() {
            data object OpenAuthenticateInPerson : Authenticate()

            data object OpenAuthenticateOnLine : Authenticate()
        }

        sealed class SignDocument : BottomSheet() {
            data object OpenFromDevice : Authenticate()

            data object OpenScanQR : Authenticate()
        }

        sealed class Bluetooth : BottomSheet() {
            data class PrimaryButtonPressed(
                val availability: BleAvailability,
            ) : Bluetooth()

            data object SecondaryButtonPressed : Bluetooth()
        }
    }

    data object OnShowPermissionsRational : Event()

    data class OnPermissionStateChanged(
        val availability: BleAvailability,
    ) : Event()

    data class DocumentClicked(
        val documentId: String,
    ) : Event()

    data class TransactionClicked(
        val transactionId: String,
    ) : Event()

    data object SeeAllDocumentsClicked : Event()
}

sealed class Effect : ViewSideEffect {
    sealed class Navigation : Effect() {
        data class SwitchScreen(
            val screenRoute: String,
            val popUpToScreenRoute: String = DashboardScreens.Dashboard.screenRoute,
            val inclusive: Boolean = false,
        ) : Navigation()

        data object OnAppSettings : Navigation()

        data object OnSystemSettings : Navigation()
    }

    data object ShowBottomSheet : Effect()

    data class CloseBottomSheet(
        val hasNextBottomSheet: Boolean,
    ) : Effect()

    data class SwitchTab(
        val tab: BottomNavigationItem,
    ) : Effect()
}

sealed class HomeScreenBottomSheetContent {
    data object None : HomeScreenBottomSheetContent()

    data object Authenticate : HomeScreenBottomSheetContent()

    data object LearnMoreAboutAuthenticate : HomeScreenBottomSheetContent()

    data object LearnMoreAboutSignDocument : HomeScreenBottomSheetContent()

    data object Sign : HomeScreenBottomSheetContent()

    data class Bluetooth(
        val availability: BleAvailability,
    ) : HomeScreenBottomSheetContent()
}

@KoinViewModel
class HomeViewModel(
    private val homeInteractor: HomeInteractor,
    private val documentsInteractor: DocumentsInteractor,
    private val transactionsInteractor: TransactionsInteractor,
    private val uiSerializer: UiSerializer,
    private val resourceProvider: ResourceProvider,
) : MviViewModel<Event, State, Effect>() {
    private var userNameJob: Job? = null
    private var recentDataJob: Job? = null

    override fun setInitialState(): State =
        State(
            welcomeUserMessage = resourceProvider.getString(R.string.home_screen_welcome),
            authenticateCardConfig =
                ActionCardConfig(
                    title = resourceProvider.getString(R.string.home_screen_authentication_card_title),
                    icon = AppIcons.IdCards,
                    primaryButtonText = resourceProvider.getString(R.string.home_screen_authenticate),
                    secondaryButtonText = resourceProvider.getString(R.string.home_screen_learn_more),
                ),
            signCardConfig =
                ActionCardConfig(
                    title = resourceProvider.getString(R.string.home_screen_sign_card_title),
                    icon = AppIcons.Contract,
                    primaryButtonText = resourceProvider.getString(R.string.home_screen_sign),
                    secondaryButtonText = resourceProvider.getString(R.string.home_screen_learn_more),
                ),
            isBleCentralClientModeEnabled = homeInteractor.isBleCentralClientModeEnabled(),
        )

    override fun handleEvents(event: Event) {
        when (event) {
            is Event.Init -> {
                userNameJob?.cancel()
                recentDataJob?.cancel()
                userNameJob = getUserNameViaMainPidDocument()
                recentDataJob = getRecentData()
            }

            is Event.AuthenticateCard.AuthenticatePressed -> {
                navigateToAuthenticate()
            }

            is Event.AddDocumentsClicked -> {
                setEffect {
                    Effect.Navigation.SwitchScreen(
                        screenRoute =
                            generateComposableNavigationLink(
                                screen = IssuanceScreens.AddDocument,
                                arguments =
                                    generateComposableArguments(
                                        mapOf(
                                            IssuanceUiConfig.serializedKeyName to
                                                uiSerializer.toBase64(
                                                    IssuanceUiConfig(
                                                        flowType =
                                                            IssuanceFlowType.ExtraDocument(
                                                                formatType = null,
                                                            ),
                                                    ),
                                                    IssuanceUiConfig.Parser,
                                                ),
                                        ),
                                    ),
                            ),
                    )
                }
            }

            is Event.AuthenticateCard.LearnMorePressed -> {
                showBottomSheet(
                    sheetContent = HomeScreenBottomSheetContent.LearnMoreAboutAuthenticate,
                )
            }

            is Event.SignDocumentCard.SignDocumentPressed -> {
                navigateToDocumentSign()
            }

            is Event.SignDocumentCard.LearnMorePressed -> {
                showBottomSheet(
                    sheetContent = HomeScreenBottomSheetContent.LearnMoreAboutSignDocument,
                )
            }

            is Event.BottomSheet.UpdateBottomSheetState -> {
                setState {
                    copy(isBottomSheetOpen = event.isOpen)
                }
            }

            is Event.BottomSheet.Close -> {
                hideBottomSheet()
            }

            is Event.BottomSheet.Authenticate.OpenAuthenticateInPerson -> {
                checkIfBluetoothIsEnabled()
            }

            is Event.BottomSheet.Authenticate.OpenAuthenticateOnLine -> {
                hideBottomSheet()
                navigateToQrScan()
            }

            is Event.BottomSheet.SignDocument.OpenFromDevice -> {
                hideBottomSheet()
                navigateToDocumentSign()
            }

            is Event.BottomSheet.SignDocument.OpenScanQR -> {
                hideBottomSheet()
                navigateToQrSignatureScan()
            }

            is Event.OnPermissionStateChanged -> {
                setState { copy(bleAvailability = event.availability) }
            }

            is Event.OnShowPermissionsRational -> {
                setState { copy(bleAvailability = BleAvailability.UNKNOWN) }
                showBottomSheet(
                    sheetContent =
                        Bluetooth(
                            BleAvailability.NO_PERMISSION,
                        ),
                )
            }

            is Event.StartProximityFlow -> {
                hideBottomSheet()
                startProximityFlow()
            }

            is Event.BottomSheet.Bluetooth.PrimaryButtonPressed -> {
                hideBottomSheet()
                onBleUserAction(event.availability)
            }

            is Event.BottomSheet.Bluetooth.SecondaryButtonPressed -> {
                hideBottomSheet()
            }

            is Event.DocumentClicked -> {
                navigateToDocumentDetails(event.documentId)
            }

            is Event.TransactionClicked -> {
                navigateToTransactionDetails(event.transactionId)
            }

            is Event.SeeAllDocumentsClicked -> {
                setEffect { Effect.SwitchTab(BottomNavigationItem.Documents) }
            }
        }
    }

    private fun checkIfBluetoothIsEnabled() {
        if (homeInteractor.isBleAvailable()) {
            setState { copy(bleAvailability = BleAvailability.NO_PERMISSION) }
        } else {
            setState { copy(bleAvailability = BleAvailability.DISABLED) }
            hideAndShowNextBottomSheet()
            showBottomSheet(
                sheetContent = Bluetooth(BleAvailability.DISABLED),
            )
        }
    }

    private fun onBleUserAction(availability: BleAvailability) {
        when (availability) {
            BleAvailability.NO_PERMISSION -> {
                setEffect { Effect.Navigation.OnAppSettings }
            }

            BleAvailability.DISABLED -> {
                setEffect { Effect.Navigation.OnSystemSettings }
            }

            else -> {
                // no implementation
            }
        }
    }

    private fun showBottomSheet(sheetContent: HomeScreenBottomSheetContent) {
        setState {
            copy(sheetContent = sheetContent)
        }
        setEffect {
            Effect.ShowBottomSheet
        }
    }

    private fun hideBottomSheet() {
        setEffect {
            Effect.CloseBottomSheet(false)
        }
    }

    private fun hideAndShowNextBottomSheet() {
        setEffect {
            Effect.CloseBottomSheet(true)
        }
    }

    private fun navigateToAuthenticate() {
        setEffect {
            Effect.Navigation.SwitchScreen(
                screenRoute = DashboardScreens.Authenticate.screenRoute,
            )
        }
    }

    private fun navigateToDocumentSign() {
        setEffect {
            Effect.Navigation.SwitchScreen(
                screenRoute = DashboardScreens.DocumentSign.screenRoute,
            )
        }
    }

    private fun navigateToDocumentDetails(documentId: String) {
        setEffect {
            Effect.Navigation.SwitchScreen(
                screenRoute =
                    generateComposableNavigationLink(
                        screen = DashboardScreens.DocumentDetails,
                        arguments = generateComposableArguments(mapOf("documentId" to documentId)),
                    ),
            )
        }
    }

    private fun navigateToTransactionDetails(transactionId: String) {
        setEffect {
            Effect.Navigation.SwitchScreen(
                screenRoute =
                    generateComposableNavigationLink(
                        screen = DashboardScreens.TransactionDetails,
                        arguments = generateComposableArguments(mapOf("transactionId" to transactionId)),
                    ),
            )
        }
    }

    private fun startProximityFlow() {
        setState { copy(bleAvailability = BleAvailability.AVAILABLE) }
        // Create Koin scope for presentation
        getOrCreatePresentationScope()
        setEffect {
            Effect.Navigation.SwitchScreen(
                screenRoute =
                    generateComposableNavigationLink(
                        screen = ProximityScreens.QR,
                        arguments =
                            generateComposableArguments(
                                mapOf(
                                    RequestUriConfig.serializedKeyName to
                                        uiSerializer.toBase64(
                                            RequestUriConfig(PresentationMode.Ble(DashboardScreens.Dashboard.screenRoute)),
                                            RequestUriConfig.Parser,
                                        ),
                                ),
                            ),
                    ),
            )
        }
    }

    private fun navigateToQrSignatureScan() {
        val navigationEffect =
            Effect.Navigation.SwitchScreen(
                screenRoute =
                    generateComposableNavigationLink(
                        screen = CommonScreens.QrScan,
                        arguments =
                            generateComposableArguments(
                                mapOf(
                                    QrScanUiConfig.serializedKeyName to
                                        uiSerializer.toBase64(
                                            QrScanUiConfig(
                                                title = resourceProvider.getString(R.string.signature_qr_scan_title),
                                                subTitle = resourceProvider.getString(R.string.signature_qr_scan_subtitle),
                                                qrScanFlow = QrScanFlow.Signature,
                                            ),
                                            QrScanUiConfig.Parser,
                                        ),
                                ),
                            ),
                    ),
            )
        setEffect {
            navigationEffect
        }
    }

    private fun navigateToQrScan() {
        val navigationEffect =
            Effect.Navigation.SwitchScreen(
                screenRoute =
                    generateComposableNavigationLink(
                        screen = CommonScreens.QrScan,
                        arguments =
                            generateComposableArguments(
                                mapOf(
                                    QrScanUiConfig.serializedKeyName to
                                        uiSerializer.toBase64(
                                            QrScanUiConfig(
                                                title = resourceProvider.getString(R.string.presentation_qr_scan_title),
                                                subTitle = resourceProvider.getString(R.string.presentation_qr_scan_subtitle),
                                                qrScanFlow = QrScanFlow.Presentation,
                                            ),
                                            QrScanUiConfig.Parser,
                                        ),
                                ),
                            ),
                    ),
            )
        setEffect {
            navigationEffect
        }
    }

    private fun getUserNameViaMainPidDocument(): Job =
        viewModelScope.launch {
            homeInteractor.getUserNameViaMainPidDocument().collect { response ->
                when (response) {
                    is HomeInteractorGetUserNameViaMainPidDocumentPartialState.Failure -> {
                        // ignore
                    }

                    is HomeInteractorGetUserNameViaMainPidDocumentPartialState.Success -> {
                        setState {
                            copy(
                                welcomeUserMessage =
                                    if (response.userFirstName.isNotBlank()) {
                                        resourceProvider.getString(
                                            R.string.home_screen_welcome_user_message,
                                            response.userFirstName,
                                        )
                                    } else {
                                        resourceProvider.getString(R.string.home_screen_welcome)
                                    },
                            )
                        }
                    }
                }
            }
        }

    private fun Instant.formatInstant(): String {
        val formatter =
            DateTimeFormatter
                .ofPattern("dd/MM/yyyy", Locale.getDefault())
                .withZone(ZoneId.systemDefault())

        return formatter.format(this)
    }

    private fun getRecentData(): Job =
        viewModelScope.launch {
            combine(
                documentsInteractor.getDocuments(),
                transactionsInteractor.getTransactions(),
            ) { docsResponse, transResponse ->
                val allDocs =
                    if (docsResponse is DocumentInteractorGetDocumentsPartialState.Success) {
                        docsResponse.allDocuments.items
                            .sortedByDescending { (it.attributes as? DocumentsFilterableAttributes)?.issuedDate ?: Instant.MIN }
                            .mapNotNull { item ->
                                (item.payload as? DocumentUi)?.let { docUi ->
                                    if (docUi.documentIssuanceState == DocumentIssuanceStateUi.Pending) return@let null
                                    val attributes = item.attributes as? DocumentsFilterableAttributes
                                    val usagesLeft =
                                        when (val trailing = docUi.uiData.trailingContentData) {
                                            is ListItemTrailingContentDataUi.TextWithIcon -> trailing.text
                                            else -> "-"
                                        }
                                    DashboardDocument(
                                        documentUi = docUi,
                                        usagesLeft = usagesLeft,
                                        expiresAt = attributes?.expiryDate?.formatInstant() ?: "-",
                                        isPending = false,
                                    )
                                }
                            }
                    } else {
                        emptyList()
                    }

                val allTrans =
                    if (transResponse is TransactionInteractorGetTransactionsPartialState.Success) {
                        transResponse.allTransactions.items
                            .mapNotNull { item ->
                                (item.payload as? TransactionUi)?.let { transUi ->
                                    DashboardTransaction(
                                        transactionUi = transUi,
                                        isPending = false,
                                    )
                                }
                            }
                    } else {
                        emptyList()
                    }

                allDocs to allTrans
            }.collect { (docs, trans) ->
                setState {
                    copy(
                        allDocuments = docs,
                        allTransactions = trans,
                    )
                }
            }
        }
}
