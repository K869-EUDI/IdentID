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
import com.k689.identid.config.RequestUriConfig
import com.k689.identid.di.core.getOrCreatePresentationScope
import com.k689.identid.interactor.dashboard.DocumentInteractorGetDocumentsPartialState
import com.k689.identid.interactor.dashboard.DocumentsInteractor
import com.k689.identid.interactor.dashboard.HomeInteractor
import com.k689.identid.interactor.dashboard.HomeInteractorGetUserNameViaMainPidDocumentPartialState
import com.k689.identid.interactor.dashboard.TransactionInteractorGetTransactionsPartialState
import com.k689.identid.interactor.dashboard.TransactionsInteractor
import com.k689.identid.model.common.PinFlow
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
import com.k689.identid.ui.dashboard.home.components.DrawerMenuItem
import com.k689.identid.ui.dashboard.transactions.list.model.TransactionsFilterableAttributes
import com.k689.identid.ui.dashboard.transactions.list.model.TransactionUi
import com.k689.identid.ui.mvi.MviViewModel
import com.k689.identid.ui.mvi.ViewEvent
import com.k689.identid.ui.mvi.ViewSideEffect
import com.k689.identid.ui.mvi.ViewState
import com.k689.identid.ui.serializer.UiSerializer
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

private data class HomeDocumentCandidate(
    val dashboardDocument: DashboardDocument,
    val attributes: DocumentsFilterableAttributes?,
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
    val recentDocuments: List<DashboardDocument> = emptyList(),
    val allTransactions: List<DashboardTransaction> = emptyList(),
    val recentTransactions: List<DashboardTransaction> = emptyList(),
    val hasMoreTransactions: Boolean = false,
) : ViewState

sealed class Event : ViewEvent {
    data object Init : Event()

    data object StartProximityFlow : Event()

    data object AddDocumentsClicked : Event()

    sealed class AuthenticateCard : Event() {
        data object AuthenticatePressed : Event()
    }

    sealed class SignDocumentCard : Event() {
        data object SignDocumentPressed : Event()
    }

    sealed class BottomSheet : Event() {
        data class UpdateBottomSheetState(
            val isOpen: Boolean,
        ) : BottomSheet()

        data object Close : BottomSheet()

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
        val isPseudonym: Boolean = false,
    ) : Event()

    data object SeeAllDocumentsClicked : Event()

    data object SeeAllTransactionsClicked : Event()

    data class DrawerMenuItemClicked(
        val item: DrawerMenuItem,
    ) : Event()
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

            is Event.SignDocumentCard.SignDocumentPressed -> {
                navigateToDocumentSign()
            }

            is Event.BottomSheet.UpdateBottomSheetState -> {
                setState {
                    copy(isBottomSheetOpen = event.isOpen)
                }
            }

            is Event.BottomSheet.Close -> {
                hideBottomSheet()
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
                navigateToTransactionDetails(event.transactionId, event.isPseudonym)
            }

            is Event.SeeAllDocumentsClicked -> {
                navigateToAllDocuments()
            }

            is Event.SeeAllTransactionsClicked -> {
                navigateToAllTransactions()
            }

            is Event.DrawerMenuItemClicked -> {
                onDrawerMenuItemClicked(event.item)
            }
        }
    }

    private fun onDrawerMenuItemClicked(item: DrawerMenuItem) {
        when (item) {
            DrawerMenuItem.ChangePin -> {
                setEffect {
                    Effect.Navigation.SwitchScreen(
                        screenRoute =
                            generateComposableNavigationLink(
                                screen = CommonScreens.QuickPin,
                                arguments =
                                    generateComposableArguments(
                                        mapOf("pinFlow" to PinFlow.UPDATE),
                                    ),
                            ),
                    )
                }
            }

            else -> {
                item.route?.let { route ->
                    setEffect { Effect.Navigation.SwitchScreen(screenRoute = route) }
                }
            }
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

    private fun navigateToAllDocuments() {
        setEffect {
            Effect.Navigation.SwitchScreen(
                screenRoute = DashboardScreens.Documents.screenRoute,
            )
        }
    }

    private fun navigateToAllTransactions() {
        setEffect {
            Effect.Navigation.SwitchScreen(
                screenRoute = DashboardScreens.Transactions.screenRoute,
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

    private fun navigateToTransactionDetails(
        transactionId: String,
        isPseudonym: Boolean = false,
    ) {
        setEffect {
            Effect.Navigation.SwitchScreen(
                screenRoute =
                    if (!isPseudonym) {
                        generateComposableNavigationLink(
                            screen = DashboardScreens.TransactionDetails,
                            arguments =
                                generateComposableArguments(
                                    mapOf(
                                        "transactionId" to transactionId,
                                    ),
                                ),
                        )
                    } else {
                        generateComposableNavigationLink(
                            screen = DashboardScreens.PseudonymTransactionLogDetail,
                            arguments =
                                generateComposableArguments(
                                    mapOf(
                                        "logId" to transactionId,
                                    ),
                                ),
                        )
                    },
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

    private fun java.time.LocalDateTime.toUsageInstant(): Instant =
        atZone(ZoneId.systemDefault()).toInstant()

    private fun getRecentData(): Job =
        viewModelScope.launch {
            val documentsFlow = documentsInteractor.getDocuments()
            val transactionsFlow = transactionsInteractor.getTransactions()

            combine(
                documentsFlow,
                transactionsFlow,
            ) { docsResponse, transResponse ->
                docsResponse to transResponse
            }.collect { (docsResponse, transResponse) ->
                val bookmarkedDocumentIds = homeInteractor.getBookmarkedDocumentIds()

                val documentCandidates =
                    if (docsResponse is DocumentInteractorGetDocumentsPartialState.Success) {
                        docsResponse.allDocuments.items
                            .sortedByDescending { (it.attributes as? DocumentsFilterableAttributes)?.issuedDate ?: Instant.MIN }
                            .mapNotNull { item ->
                                (item.payload as? DocumentUi)?.let { docUi ->
                                    if (docUi.documentIssuanceState == DocumentIssuanceStateUi.Pending) {
                                        return@let null
                                    }

                                    val attributes = item.attributes as? DocumentsFilterableAttributes
                                    val usagesLeft =
                                        when (val trailing = docUi.uiData.trailingContentData) {
                                            is ListItemTrailingContentDataUi.TextWithIcon -> trailing.text
                                            else -> "-"
                                        }

                                    HomeDocumentCandidate(
                                        dashboardDocument =
                                            DashboardDocument(
                                                documentUi = docUi,
                                                usagesLeft = usagesLeft,
                                                expiresAt = attributes?.expiryDate?.formatInstant() ?: "-",
                                                isPending = false,
                                            ),
                                        attributes = attributes,
                                    )
                                }
                            }
                    } else {
                        emptyList()
                    }

                val allDocs = documentCandidates.map { it.dashboardDocument }

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

                val latestUsageByDocumentName =
                    if (transResponse is TransactionInteractorGetTransactionsPartialState.Success) {
                        transResponse.allTransactions.items
                            .mapNotNull { item ->
                                val attributes = item.attributes as? TransactionsFilterableAttributes ?: return@mapNotNull null
                                val usageInstant = attributes.creationLocalDateTime?.toUsageInstant() ?: return@mapNotNull null
                                attributes.documentNames.map { documentName -> documentName to usageInstant }
                            }.flatten()
                            .groupBy(keySelector = { it.first }, valueTransform = { it.second })
                            .mapValues { (_, usages) -> usages.maxOrNull() ?: Instant.MIN }
                    } else {
                        emptyMap()
                    }

                val bookmarkedDocs =
                    documentCandidates
                        .filter { it.dashboardDocument.documentUi.uiData.itemId in bookmarkedDocumentIds }
                        .sortedWith(
                            compareByDescending<HomeDocumentCandidate> {
                                latestUsageByDocumentName[it.attributes?.name] ?: Instant.MIN
                            }.thenByDescending {
                                it.attributes?.issuedDate ?: Instant.MIN
                            },
                        ).map { it.dashboardDocument }

                val filteredTrans = allTrans.filter { !it.isPending }.reversed()
                val recentTrans = filteredTrans.take(5)
                val hasMoreTrans = filteredTrans.size > 5

                setState {
                    copy(
                        allDocuments = allDocs,
                        recentDocuments = bookmarkedDocs,
                        allTransactions = allTrans,
                        recentTransactions = recentTrans,
                        hasMoreTransactions = hasMoreTrans,
                    )
                }
            }
        }
}
