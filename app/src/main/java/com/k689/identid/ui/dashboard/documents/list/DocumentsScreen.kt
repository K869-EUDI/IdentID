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

package com.k689.identid.ui.dashboard.documents.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.k689.identid.R
import com.k689.identid.extension.ui.paddingFrom
import com.k689.identid.model.core.DocumentCategory
import com.k689.identid.model.core.DocumentIdentifier
import com.k689.identid.ui.component.AppIcons
import com.k689.identid.ui.component.DualSelectorButton
import com.k689.identid.ui.component.DualSelectorButtonDataUi
import com.k689.identid.ui.component.DualSelectorButtons
import com.k689.identid.ui.component.FiltersSearchBar
import com.k689.identid.ui.component.InlineSnackbar
import com.k689.identid.ui.component.ListItemDataUi
import com.k689.identid.ui.component.ListItemMainContentDataUi
import com.k689.identid.ui.component.ModalOptionUi
import com.k689.identid.ui.component.SectionTitle
import com.k689.identid.ui.component.SystemBroadcastReceiver
import com.k689.identid.ui.component.content.BroadcastAction
import com.k689.identid.ui.component.content.ContentScreen
import com.k689.identid.ui.component.content.ScreenNavigateAction
import com.k689.identid.ui.component.content.ToolbarActionUi
import com.k689.identid.ui.component.content.ToolbarConfig
import com.k689.identid.ui.component.preview.PreviewTheme
import com.k689.identid.ui.component.preview.ThemeModePreviews
import com.k689.identid.ui.component.utils.HSpacer
import com.k689.identid.ui.component.utils.LifecycleEffect
import com.k689.identid.ui.component.utils.OneTimeLaunchedEffect
import com.k689.identid.ui.component.utils.SPACING_EXTRA_SMALL
import com.k689.identid.ui.component.utils.SPACING_LARGE
import com.k689.identid.ui.component.utils.SPACING_MEDIUM
import com.k689.identid.ui.component.utils.SPACING_SMALL
import com.k689.identid.ui.component.utils.VSpacer
import com.k689.identid.ui.component.wrap.BottomSheetTextDataUi
import com.k689.identid.ui.component.wrap.BottomSheetWithOptionsList
import com.k689.identid.ui.component.wrap.ButtonConfig
import com.k689.identid.ui.component.wrap.ButtonType
import com.k689.identid.ui.component.wrap.WrapConfirmationDialog
import com.k689.identid.ui.component.wrap.GenericBottomSheet
import com.k689.identid.ui.component.wrap.WrapButton
import com.k689.identid.ui.component.wrap.WrapExpandableListItem
import com.k689.identid.ui.component.wrap.WrapIconButton
import com.k689.identid.ui.component.wrap.WrapListItem
import com.k689.identid.ui.component.wrap.WrapModalBottomSheet
import com.k689.identid.ui.dashboard.documents.component.StackedDocumentIdentityCard
import com.k689.identid.ui.dashboard.documents.component.toCardIdentificationTag
import com.k689.identid.ui.dashboard.documents.detail.model.DocumentIssuanceStateUi
import com.k689.identid.ui.dashboard.documents.list.model.DocumentUi
import com.k689.identid.util.core.CoreActions
import com.k689.identid.util.dashboard.TestTag
import eu.europa.ec.eudi.wallet.document.DocumentId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

typealias DashboardEvent = com.k689.identid.ui.dashboard.dashboard.Event
typealias OpenSideMenuEvent = com.k689.identid.ui.dashboard.dashboard.Event.SideMenu.Open

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentsScreen(
    navHostController: NavController,
    viewModel: DocumentsViewModel,
    onDashboardEventSent: (DashboardEvent) -> Unit,
) {
    val state: State by viewModel.viewState.collectAsStateWithLifecycle()
    val onEventSend = viewModel::setEvent

    val isBottomSheetOpen = state.isBottomSheetOpen
    val scope = rememberCoroutineScope()
    val bottomSheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
        )

    val toolbarConfig =
        if (state.isSelectionModeActive) {
            ToolbarConfig(
                title = stringResource(R.string.documents_screen_selection_title, state.selectedDocumentIds.size),
                actions =
                    listOf(
                        ToolbarActionUi(
                            icon = AppIcons.Delete,
                            onClick = { onEventSend(Event.OnDeleteSelectedDocuments) },
                        ),
                    ),
            )
        } else {
            ToolbarConfig(
                title = stringResource(R.string.documents_screen_title),
            )
        }

    ContentScreen(
        isLoading = state.isLoading,
        navigatableAction = if (state.isSelectionModeActive) ScreenNavigateAction.CANCELABLE else ScreenNavigateAction.BACKABLE,
        onBack = {
            if (state.isSelectionModeActive) {
                onEventSend(Event.OnCancelSelectionMode)
            } else {
                onEventSend(Event.Pop)
            }
        },
        contentErrorConfig = null,
        toolBarConfig = toolbarConfig,
        broadcastAction =
            BroadcastAction(
                intentFilters =
                    listOf(
                        CoreActions.REVOCATION_WORK_REFRESH_ACTION,
                    ),
                callback = {
                    onEventSend(Event.GetDocuments())
                },
            ),
    ) { paddingValues ->
        Content(
            state = state,
            effectFlow = viewModel.effect,
            onEventSend = onEventSend,
            onNavigationRequested = { navigationEffect ->
                handleNavigationEffect(navigationEffect, navHostController)
            },
            paddingValues = paddingValues,
            coroutineScope = scope,
            modalBottomSheetState = bottomSheetState,
        )

        if (isBottomSheetOpen) {
            when (val content = state.sheetContent) {
                is DocumentsBottomSheetContent.ConfirmDelete -> {
                    WrapConfirmationDialog(
                        title = stringResource(R.string.documents_screen_delete_confirmation_title),
                        message = stringResource(R.string.documents_screen_delete_confirmation_message, content.documentIds.size),
                        primaryButtonText = stringResource(R.string.documents_screen_delete_confirmation_positive),
                        onPrimaryClick = { onEventSend(Event.OnConfirmDelete) },
                        secondaryButtonText = stringResource(R.string.documents_screen_delete_confirmation_negative),
                        onSecondaryClick = { onEventSend(Event.BottomSheet.UpdateBottomSheetState(isOpen = false)) },
                        isPrimaryWarning = true,
                        onDismissRequest = { onEventSend(Event.BottomSheet.UpdateBottomSheetState(isOpen = false)) }
                    )
                }

                is DocumentsBottomSheetContent.DeferredDocumentPressed -> {
                    WrapConfirmationDialog(
                        title = stringResource(id = R.string.dashboard_bottom_sheet_deferred_document_pressed_title),
                        message = stringResource(id = R.string.dashboard_bottom_sheet_deferred_document_pressed_subtitle),
                        primaryButtonText = stringResource(id = R.string.dashboard_bottom_sheet_deferred_document_pressed_primary_button_text),
                        onPrimaryClick = {
                            onEventSend(
                                Event.BottomSheet.DeferredDocument.DeferredNotReadyYet.PrimaryButtonPressed(
                                    documentId = content.documentId,
                                ),
                            )
                        },
                        secondaryButtonText = stringResource(id = R.string.dashboard_bottom_sheet_deferred_document_pressed_secondary_button_text),
                        onSecondaryClick = {
                            onEventSend(
                                Event.BottomSheet.DeferredDocument.DeferredNotReadyYet.SecondaryButtonPressed(
                                    documentId = content.documentId,
                                ),
                            )
                        },
                        onDismissRequest = { onEventSend(Event.BottomSheet.UpdateBottomSheetState(isOpen = false)) }
                    )
                }

                else -> {
                    WrapModalBottomSheet(
                        onDismissRequest = {
                            onEventSend(
                                Event.BottomSheet.UpdateBottomSheetState(
                                    isOpen = false,
                                ),
                            )
                        },
                        sheetState = bottomSheetState,
                    ) {
                        DocumentsSheetContent(
                            sheetContent = content,
                            state = state,
                            onEventSent = onEventSend,
                        )
                    }
                }
            }
        }
    }

    SystemBroadcastReceiver(
        intentFilters = listOf(CoreActions.DEFERRED_ISSUANCE_REFRESH_ACTION),
    ) { intent ->
        val failedIds =
            intent?.getStringArrayListExtra(CoreActions.DEFERRED_ISSUANCE_FAILED_IDS_EXTRA)
                ?: emptyList()
        onEventSend(Event.GetDocuments(failedIds))
    }
}

private fun handleNavigationEffect(
    navigationEffect: Effect.Navigation,
    navController: NavController,
) {
    when (navigationEffect) {
        is Effect.Navigation.Pop -> {
            navController.popBackStack()
        }

        is Effect.Navigation.SwitchScreen -> {
            navController.navigate(navigationEffect.screenRoute) {
                popUpTo(navigationEffect.popUpToScreenRoute) {
                    inclusive = navigationEffect.inclusive
                }
            }
        }
    }
}

@Composable
private fun TopBar(
    onEventSend: (Event) -> Unit,
    onDashboardEventSent: (DashboardEvent) -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    all = SPACING_SMALL.dp,
                ),
    ) {
        WrapIconButton(
            modifier = Modifier.align(Alignment.CenterStart),
            iconData = AppIcons.Menu,
            customTint = MaterialTheme.colorScheme.onSurface,
        ) {
            onDashboardEventSent(OpenSideMenuEvent)
        }

        Text(
            modifier = Modifier.align(Alignment.Center),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.headlineMedium,
            text = stringResource(R.string.documents_screen_title),
        )

        WrapIconButton(
            modifier =
                Modifier
                    .testTag(TestTag.DocumentsScreen.PLUS_BUTTON)
                    .align(Alignment.CenterEnd),
            iconData = AppIcons.Add,
            customTint = MaterialTheme.colorScheme.onSurfaceVariant,
        ) {
            onEventSend(Event.AddDocumentPressed)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Content(
    state: State,
    effectFlow: Flow<Effect>,
    onEventSend: (Event) -> Unit,
    onNavigationRequested: (navigationEffect: Effect.Navigation) -> Unit,
    paddingValues: PaddingValues,
    coroutineScope: CoroutineScope,
    modalBottomSheetState: SheetState,
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .paddingFrom(paddingValues, bottom = false),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = SPACING_MEDIUM.dp),
        ) {
            item {
                FiltersSearchBar(
                    placeholder = stringResource(R.string.documents_screen_search_label),
                    onValueChange = { onEventSend(Event.OnSearchQueryChanged(it)) },
                    onFilterClick = { onEventSend(Event.FiltersPressed) },
                    onClearClick = { onEventSend(Event.OnSearchQueryChanged("")) },
                    isFilteringActive = state.isFilteringActive,
                    text = state.searchText,
                )
                VSpacer.Large()
            }

            if (state.showNoResultsFound) {
                item {
                    NoResults(modifier = Modifier.fillMaxWidth())
                }
            } else {
                itemsIndexed(items = state.documentsUi) { index, (documentCategory, documents) ->
                    DocumentCategorySection(
                        modifier = Modifier.fillMaxWidth(),
                        category = documentCategory,
                        documents = documents,
                        selectedDocumentIds = state.selectedDocumentIds,
                        isSelectionModeActive = state.isSelectionModeActive,
                        onEventSend = onEventSend,
                    )

                    if (index != state.documentsUi.lastIndex) {
                        VSpacer.ExtraLarge()
                    }
                }
            }
        }

        if (state.error != null) {
            InlineSnackbar(
                error = state.error,
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = SPACING_EXTRA_SMALL.dp),
            )
        }
    }

    LifecycleEffect(
        lifecycleOwner = LocalLifecycleOwner.current,
        lifecycleEvent = Lifecycle.Event.ON_RESUME,
    ) {
        onEventSend(Event.GetDocuments())
    }

    LifecycleEffect(
        lifecycleOwner = LocalLifecycleOwner.current,
        lifecycleEvent = Lifecycle.Event.ON_PAUSE,
    ) {
        onEventSend(Event.OnPause)
    }

    OneTimeLaunchedEffect {
        onEventSend(Event.Init)
    }

    LaunchedEffect(Unit) {
        effectFlow.collect { effect ->
            when (effect) {
                is Effect.Navigation -> {
                    onNavigationRequested(effect)
                }

                is Effect.CloseBottomSheet -> {
                    coroutineScope
                        .launch {
                            modalBottomSheetState.hide()
                        }.invokeOnCompletion {
                            if (!modalBottomSheetState.isVisible) {
                                onEventSend(Event.BottomSheet.UpdateBottomSheetState(isOpen = false))
                            }
                        }
                }

                is Effect.ShowBottomSheet -> {
                    onEventSend(Event.BottomSheet.UpdateBottomSheetState(isOpen = true))
                }

                is Effect.ResumeOnApplyFilter -> {
                    onEventSend(Event.GetDocuments())
                }
            }
        }
    }
}

@Composable
private fun DocumentCategorySection(
    modifier: Modifier = Modifier,
    category: DocumentCategory,
    documents: List<DocumentUi>,
    selectedDocumentIds: Set<DocumentId>,
    isSelectionModeActive: Boolean,
    onEventSend: (Event) -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(SPACING_MEDIUM.dp),
    ) {
        SectionTitle(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(category.stringResId),
        )

        val categoryLabel = stringResource(category.stringResId)
        documents.forEach { documentItem: DocumentUi ->
            StackedDocumentIdentityCard(
                modifier = Modifier.fillMaxWidth(),
                title = documentItem.cardTitle(),
                identification = "$categoryLabel • ${documentItem.documentIdentifier.toCardIdentificationTag()}",
                supportingLines = documentItem.cardSupportingLines(),
                status = documentItem.documentIssuanceState.toCardStatusLabel(),
                isSelected = selectedDocumentIds.contains(documentItem.uiData.itemId),
                isSelectionModeActive = isSelectionModeActive,
                onClick = {
                    onEventSend(Event.OnDocumentClick(documentItem.uiData.itemId))
                },
                onLongClick = {
                    onEventSend(Event.OnDocumentLongClick(documentItem.uiData.itemId))
                },
            )
        }
    }
}

@Composable
private fun NoResults(
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        WrapListItem(
            item =
                ListItemDataUi(
                    itemId = stringResource(R.string.documents_screen_search_no_results_id),
                    mainContentData = ListItemMainContentDataUi.Text(text = stringResource(R.string.documents_screen_search_no_results)),
                ),
            onItemClick = null,
            modifier = Modifier.fillMaxWidth(),
            mainContentVerticalPadding = SPACING_MEDIUM.dp,
        )
    }
}

private fun DocumentUi.cardTitle(): String = (uiData.mainContentData as? ListItemMainContentDataUi.Text)?.text.orEmpty()

private fun DocumentUi.cardSupportingLines(): List<String> =
    listOfNotNull(
        uiData.overlineText,
        uiData.supportingText,
    )

@Composable
private fun DocumentIssuanceStateUi.toCardStatusLabel(): String? =
    when (this) {
        DocumentIssuanceStateUi.Issued -> null
        DocumentIssuanceStateUi.Pending -> stringResource(R.string.dashboard_document_deferred_pending)
        DocumentIssuanceStateUi.Failed -> stringResource(R.string.dashboard_document_deferred_failed)
        DocumentIssuanceStateUi.Expired -> stringResource(R.string.dashboard_document_has_expired)
        DocumentIssuanceStateUi.Revoked -> stringResource(R.string.dashboard_document_revoked)
    }

@Composable
private fun DocumentsSheetContent(
    sheetContent: DocumentsBottomSheetContent,
    state: State,
    onEventSent: (event: Event) -> Unit,
) {
    when (sheetContent) {
        is DocumentsBottomSheetContent.Filters -> {
            GenericBottomSheet(
                titleContent = {
                    Text(
                        text = stringResource(R.string.documents_screen_filters_title),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                },
                bodyContent = {
                    var buttonsRowHeight by remember { mutableIntStateOf(0) }

                    Box {
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState())
                                    .padding(bottom = with(LocalDensity.current) { buttonsRowHeight.toDp() }),
                            verticalArrangement = Arrangement.spacedBy(SPACING_LARGE.dp),
                        ) {
                            DualSelectorButtons(state.sortOrder) {
                                onEventSent(Event.OnSortingOrderChanged(it))
                            }
                            state.filtersUi.forEach { filter ->
                                if (filter.nestedItems.isNotEmpty()) {
                                    WrapExpandableListItem(
                                        header = filter.header,
                                        data = filter.nestedItems,
                                        isExpanded = filter.isExpanded,
                                        onExpandedChange = {
                                            onEventSent(Event.OnToggleFilterExpansion(it.itemId))
                                        },
                                        onItemClick = {
                                            val id = it.itemId
                                            val groupId = filter.header.itemId
                                            onEventSent(Event.OnFilterSelectionChanged(id, groupId))
                                        },
                                        addDivider = false,
                                        collapsedMainContentVerticalPadding = SPACING_MEDIUM.dp,
                                        expandedMainContentVerticalPadding = SPACING_MEDIUM.dp,
                                    )
                                }
                            }
                        }
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter)
                                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                                    .onGloballyPositioned { coordinates ->
                                        buttonsRowHeight = coordinates.size.height
                                    }.padding(top = SPACING_LARGE.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            WrapButton(
                                modifier = Modifier.weight(1f),
                                buttonConfig =
                                    ButtonConfig(
                                        type = ButtonType.SECONDARY,
                                        onClick = {
                                            onEventSent(Event.OnFiltersReset)
                                        },
                                    ),
                            ) {
                                Text(text = stringResource(R.string.documents_screen_filters_reset))
                            }
                            HSpacer.Small()
                            WrapButton(
                                modifier = Modifier.weight(1f),
                                buttonConfig =
                                    ButtonConfig(
                                        type = ButtonType.PRIMARY,
                                        onClick = {
                                            onEventSent(Event.OnFiltersApply)
                                        },
                                    ),
                            ) {
                                Text(text = stringResource(R.string.documents_screen_filters_apply))
                            }
                        }
                    }
                },
            )
        }

        is DocumentsBottomSheetContent.AddDocument -> {
            BottomSheetWithOptionsList(
                textData =
                    BottomSheetTextDataUi(
                        title = stringResource(R.string.documents_screen_add_document_title),
                        message = stringResource(R.string.documents_screen_add_document_description),
                    ),
                options =
                    listOf(
                        ModalOptionUi(
                            title = stringResource(R.string.documents_screen_add_document_option_list),
                            leadingIcon = AppIcons.AddDocumentFromList,
                            event = Event.BottomSheet.AddDocument.FromList,
                        ),
                        ModalOptionUi(
                            title = stringResource(R.string.documents_screen_add_document_option_qr),
                            leadingIcon = AppIcons.AddDocumentFromQr,
                            event = Event.BottomSheet.AddDocument.ScanQr,
                        ),
                    ),
                onEventSent = onEventSent,
                // hostTab = BottomNavigationItem.Documents.route.lowercase(),
            )
        }

        is DocumentsBottomSheetContent.DeferredDocumentPressed -> {
            // Handled by WrapConfirmationDialog
        }

        is DocumentsBottomSheetContent.DeferredDocumentsReady -> {
            BottomSheetWithOptionsList(
                textData =
                    BottomSheetTextDataUi(
                        title =
                            stringResource(
                                id = R.string.dashboard_bottom_sheet_deferred_documents_ready_title,
                            ),
                        message =
                            stringResource(
                                id = R.string.dashboard_bottom_sheet_deferred_documents_ready_subtitle,
                            ),
                    ),
                options = sheetContent.options,
                onEventSent = onEventSent,
            )
        }

        is DocumentsBottomSheetContent.ConfirmDelete -> {
            // Handled by WrapConfirmationDialog
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@ThemeModePreviews
@Composable
private fun DocumentsScreenPreview() {
    PreviewTheme {
        val scope = rememberCoroutineScope()
        val bottomSheetState =
            rememberModalBottomSheetState(
                skipPartiallyExpanded = true,
            )
        ContentScreen(
            isLoading = false,
            navigatableAction = ScreenNavigateAction.NONE,
            onBack = { },
            topBar = {
                TopBar(
                    onEventSend = { },
                    onDashboardEventSent = {},
                )
            },
        ) { paddingValues ->
            val issuerName = "Issuer name"
            val validUntil = "Valid Until"
            val documentsList =
                listOf(
                    DocumentUi(
                        documentIssuanceState = DocumentIssuanceStateUi.Issued,
                        uiData =
                            ListItemDataUi(
                                itemId = "id1",
                                mainContentData = ListItemMainContentDataUi.Text(text = "Document 1"),
                                overlineText = issuerName,
                                supportingText = validUntil,
                                leadingContentData = null,
                                trailingContentData = null,
                            ),
                        documentIdentifier = DocumentIdentifier.MdocPid,
                        documentCategory = DocumentCategory.Government,
                    ),
                    DocumentUi(
                        documentIssuanceState = DocumentIssuanceStateUi.Issued,
                        uiData =
                            ListItemDataUi(
                                itemId = "id2",
                                mainContentData = ListItemMainContentDataUi.Text(text = "Document 2"),
                                overlineText = issuerName,
                                supportingText = validUntil,
                                leadingContentData = null,
                                trailingContentData = null,
                            ),
                        documentIdentifier = DocumentIdentifier.MdocPid,
                        documentCategory = DocumentCategory.Government,
                    ),
                    DocumentUi(
                        documentIssuanceState = DocumentIssuanceStateUi.Issued,
                        uiData =
                            ListItemDataUi(
                                itemId = "id3",
                                mainContentData = ListItemMainContentDataUi.Text(text = "Document 3"),
                                overlineText = issuerName,
                                supportingText = validUntil,
                                leadingContentData = null,
                                trailingContentData = null,
                            ),
                        documentIdentifier = DocumentIdentifier.OTHER(formatType = ""),
                        documentCategory = DocumentCategory.Finance,
                    ),
                    DocumentUi(
                        documentIssuanceState = DocumentIssuanceStateUi.Issued,
                        uiData =
                            ListItemDataUi(
                                itemId = "id4",
                                mainContentData = ListItemMainContentDataUi.Text(text = "Document 4"),
                                overlineText = issuerName,
                                supportingText = validUntil,
                                leadingContentData = null,
                                trailingContentData = null,
                            ),
                        documentIdentifier = DocumentIdentifier.OTHER(formatType = ""),
                        documentCategory = DocumentCategory.Other,
                    ),
                )
            Content(
                state =
                    State(
                        isLoading = false,
                        isFilteringActive = false,
                        sortOrder =
                            DualSelectorButtonDataUi(
                                first = "first",
                                second = "second",
                                selectedButton = DualSelectorButton.FIRST,
                            ),
                        documentsUi = documentsList.groupBy { it.documentCategory }.toList(),
                    ),
                effectFlow = Channel<Effect>().receiveAsFlow(),
                onEventSend = {},
                onNavigationRequested = {},
                paddingValues = paddingValues,
                coroutineScope = scope,
                modalBottomSheetState = bottomSheetState,
            )
        }
    }
}
