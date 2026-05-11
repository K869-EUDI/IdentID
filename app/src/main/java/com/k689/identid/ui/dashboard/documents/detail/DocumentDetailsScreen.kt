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

package com.k689.identid.ui.dashboard.documents.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.k689.identid.R
import com.k689.identid.model.core.DocumentIdentifier
import com.k689.identid.theme.values.success
import com.k689.identid.theme.values.warning
import com.k689.identid.ui.component.AppIcons
import com.k689.identid.ui.component.IssuerDetailsCard
import com.k689.identid.ui.component.IssuerDetailsCardDataUi
import com.k689.identid.ui.component.ListItemDataUi
import com.k689.identid.ui.component.ListItemLeadingContentDataUi
import com.k689.identid.ui.component.ListItemMainContentDataUi
import com.k689.identid.ui.component.SectionTitle
import com.k689.identid.ui.component.content.BroadcastAction
import com.k689.identid.ui.component.content.ContentScreen
import com.k689.identid.ui.component.content.ScreenNavigateAction
import com.k689.identid.ui.component.content.ToolbarActionUi
import com.k689.identid.ui.component.content.ToolbarConfig
import com.k689.identid.ui.component.preview.PreviewTheme
import com.k689.identid.ui.component.preview.ThemeModePreviews
import com.k689.identid.ui.component.utils.LifecycleEffect
import com.k689.identid.ui.component.utils.SPACING_LARGE
import com.k689.identid.ui.component.utils.SPACING_MEDIUM
import com.k689.identid.ui.component.utils.SPACING_SMALL
import com.k689.identid.ui.component.utils.VSpacer
import com.k689.identid.ui.component.wrap.WrapConfirmationDialog
import com.k689.identid.ui.component.wrap.ButtonConfig
import com.k689.identid.ui.component.wrap.ButtonType
import com.k689.identid.ui.component.wrap.ExpandableListItemUi
import com.k689.identid.ui.component.wrap.SimpleBottomSheet
import com.k689.identid.ui.component.wrap.TextConfig
import com.k689.identid.ui.component.wrap.WrapButton
import com.k689.identid.ui.component.wrap.WrapCard
import com.k689.identid.ui.component.wrap.WrapListItems
import com.k689.identid.ui.component.wrap.WrapModalBottomSheet
import com.k689.identid.ui.component.wrap.WrapText
import com.k689.identid.ui.dashboard.documents.component.DocumentIdentityCard
import com.k689.identid.ui.dashboard.documents.component.toCardIdentificationTag
import com.k689.identid.ui.dashboard.documents.detail.model.DocumentDetailsUi
import com.k689.identid.ui.dashboard.documents.detail.model.DocumentIssuanceStateUi
import com.k689.identid.ui.dashboard.documents.model.DocumentCredentialsInfoUi
import com.k689.identid.util.core.CoreActions
import com.k689.identid.util.dashboard.TestTag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.net.URI
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentDetailsScreen(
    navController: NavController,
    viewModel: DocumentDetailsViewModel,
) {
    val state: State by viewModel.viewState.collectAsStateWithLifecycle()
    val onEventSend = viewModel::setEvent

    val isBottomSheetOpen = state.isBottomSheetOpen
    val scope = rememberCoroutineScope()
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val toolbarConfig =
        ToolbarConfig(
            actions =
                if (state.error == null) {
                    listOf(
                        ToolbarActionUi(
                            icon = if (state.isDocumentBookmarked) AppIcons.BookmarkFilled else AppIcons.Bookmark,
                            onClick = { onEventSend(Event.BookmarkPressed) },
                            enabled = !state.isLoading,
                            throttleClicks = true,
                        ),
                        ToolbarActionUi(
                            icon = if (state.hideSensitiveContent) AppIcons.VisibilityOff else AppIcons.Visibility,
                            onClick = { onEventSend(Event.ChangeContentVisibility) },
                            enabled = !state.isLoading,
                            throttleClicks = false,
                        ),
                    )
                } else {
                    emptyList()
                },
        )

    ContentScreen(
        isLoading = state.isLoading,
        contentErrorConfig = state.error,
        navigatableAction = ScreenNavigateAction.BACKABLE,
        onBack = { onEventSend(Event.Pop) },
        toolBarConfig = toolbarConfig,
        broadcastAction =
            BroadcastAction(
                intentFilters = listOf(CoreActions.REVOCATION_WORK_REFRESH_DETAILS_ACTION),
                callback = {
                    val ids = it?.getStringArrayListExtra(CoreActions.REVOCATION_IDS_DETAILS_EXTRA)?.toList() ?: emptyList()
                    onEventSend(Event.OnRevocationStatusChanged(ids))
                },
            ),
    ) { paddingValues ->
        Content(
            state = state,
            effectFlow = viewModel.effect,
            onEventSend = onEventSend,
            onNavigationRequested = { navigationEffect -> handleNavigationEffect(navigationEffect, navController) },
            paddingValues = paddingValues,
            coroutineScope = scope,
            modalBottomSheetState = bottomSheetState,
        )

        if (isBottomSheetOpen) {
            when (val content = state.sheetContent) {
                is DocumentDetailsBottomSheetContent.DeleteDocumentConfirmation -> {
                    WrapConfirmationDialog(
                        title = stringResource(id = R.string.document_details_bottom_sheet_delete_title),
                        message = stringResource(id = R.string.document_details_bottom_sheet_delete_subtitle),
                        primaryButtonText = stringResource(id = R.string.document_details_bottom_sheet_delete_primary_button_text),
                        onPrimaryClick = { onEventSend(Event.BottomSheet.Delete.PrimaryButtonPressed) },
                        secondaryButtonText = stringResource(id = R.string.document_details_bottom_sheet_delete_secondary_button_text),
                        onSecondaryClick = { onEventSend(Event.BottomSheet.Delete.SecondaryButtonPressed) },
                        isPrimaryWarning = true,
                        onDismissRequest = { onEventSend(Event.BottomSheet.UpdateBottomSheetState(isOpen = false)) }
                    )
                }

                else -> {
                    WrapModalBottomSheet(
                        onDismissRequest = {
                            onEventSend(Event.BottomSheet.UpdateBottomSheetState(isOpen = false))
                        },
                        sheetState = bottomSheetState,
                    ) {
                        SheetContent(
                            sheetContent = content,
                            onEventSent = onEventSend,
                        )
                    }
                }
            }
        }
    }

    LifecycleEffect(
        lifecycleOwner = LocalLifecycleOwner.current,
        lifecycleEvent = Lifecycle.Event.ON_RESUME,
    ) {
        onEventSend(Event.Init)
    }
}

private fun handleNavigationEffect(
    navigationEffect: Effect.Navigation,
    navController: NavController,
) {
    when (navigationEffect) {
        is Effect.Navigation.SwitchScreen -> {
            navController.navigate(navigationEffect.screenRoute) {
                navigationEffect.popUpToScreenRoute?.let { safePopUpToScreenRoute ->
                    popUpTo(safePopUpToScreenRoute) {
                        inclusive = navigationEffect.inclusive == true
                    }
                }
            }
        }

        is Effect.Navigation.Pop -> {
            navController.popBackStack()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Content(
    state: State,
    effectFlow: Flow<Effect>,
    onEventSend: (Event) -> Unit,
    onNavigationRequested: (Effect.Navigation) -> Unit,
    paddingValues: PaddingValues,
    coroutineScope: CoroutineScope,
    modalBottomSheetState: SheetState,
) {
    val layoutDirection = LocalLayoutDirection.current
    var topContentHeight by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current

    state.documentDetailsUi?.let { safeDocumentDetailsUi ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(top = paddingValues.calculateTopPadding()),
        ) {
            // card
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { coordinates ->
                            topContentHeight = coordinates.size.height
                        }.padding(
                            start = paddingValues.calculateStartPadding(layoutDirection),
                            end = paddingValues.calculateEndPadding(layoutDirection),
                        ),
            ) {
                AnimatedVisibility(visible = state.isRevoked) {
                    WrapCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.small,
                        colors =
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            ),
                    ) {
                        Column(modifier = Modifier.padding(SPACING_MEDIUM.dp)) {
                            WrapText(
                                text = stringResource(R.string.document_details_revoked_document_message),
                                textConfig =
                                    TextConfig(
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = Int.MAX_VALUE,
                                    ),
                            )
                        }
                    }
                    VSpacer.Large()
                }

                state.documentCredentialsInfoUi?.let { safeDocumentCredentialsInfo ->
                    DocumentCredentialsSection(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = SPACING_SMALL.dp),
                        state = state,
                        info = safeDocumentCredentialsInfo,
                        onUpdateClicked = { onEventSend(Event.DocumentCredentialsSectionPrimaryButtonPressed) },
                    )
                    VSpacer.ExtraLarge()
                }
            }

            // sheet
            if (topContentHeight > 0) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                ) {
                    val topOffset = with(density) { topContentHeight.toDp() }
                    Spacer(modifier = Modifier.height(topOffset))

                    FauxModalDetailsPanel(
                        modifier = Modifier.fillMaxWidth(),
                        onEventSend = onEventSend,
                        sectionTitle = stringResource(R.string.document_details_main_section_text),
                        documentDetailsUi = safeDocumentDetailsUi,
                        hideSensitiveContent = state.hideSensitiveContent,
                        issuerSectionTitle = stringResource(R.string.document_details_issuer_section_text),
                        issuerName = state.issuerName,
                        issuerLogo = state.issuerLogo,
                    )
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        effectFlow.collect { effect ->
            when (effect) {
                is Effect.Navigation -> {
                    onNavigationRequested(effect)
                }

                is Effect.CloseBottomSheet -> {
                    coroutineScope
                        .launch { modalBottomSheetState.hide() }
                        .invokeOnCompletion {
                            if (!modalBottomSheetState.isVisible) {
                                onEventSend(Event.BottomSheet.UpdateBottomSheetState(isOpen = false))
                            }
                        }
                }

                is Effect.ShowBottomSheet -> {
                    onEventSend(Event.BottomSheet.UpdateBottomSheetState(isOpen = true))
                }

                is Effect.BookmarkStored -> {
                    onEventSend(Event.OnBookmarkStored)
                }

                is Effect.BookmarkRemoved -> {
                    onEventSend(Event.OnBookmarkRemoved)
                }
            }
        }
    }
}

@Composable
private fun FauxModalDetailsPanel(
    modifier: Modifier = Modifier,
    onEventSend: (Event) -> Unit,
    sectionTitle: String,
    documentDetailsUi: DocumentDetailsUi,
    hideSensitiveContent: Boolean,
    issuerSectionTitle: String,
    issuerName: String?,
    issuerLogo: URI?,
) {
    WrapCard(
        modifier = modifier,
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier =
                Modifier
                    .padding(
                        start = SPACING_LARGE.dp,
                        end = SPACING_LARGE.dp,
                        top = SPACING_MEDIUM.dp,
                        bottom = SPACING_SMALL.dp,
                    ).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(SPACING_SMALL.dp),
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier =
                        Modifier
                            .width(42.dp)
                            .height(4.dp)
                            .background(
                                color = MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(50),
                            ),
                )
            }

            DocumentDetails(
                modifier = Modifier.fillMaxWidth(),
                onEventSend = onEventSend,
                sectionTitle = sectionTitle,
                documentDetailsUi = documentDetailsUi,
                hideSensitiveContent = hideSensitiveContent,
            )

            if (issuerName != null || issuerLogo != null) {
                VSpacer.Medium()
                IssuerDetails(
                    modifier = Modifier.fillMaxWidth(),
                    sectionTitle = issuerSectionTitle,
                    issuerName = issuerName,
                    issuerLogo = issuerLogo,
                )
            }

            ButtonsSection(
                onEventSend = onEventSend,
            )
        }
    }
}

@Composable
private fun SheetContent(
    sheetContent: DocumentDetailsBottomSheetContent,
    onEventSent: (event: Event) -> Unit,
) {
    when (sheetContent) {
        is DocumentDetailsBottomSheetContent.DeleteDocumentConfirmation -> {
            // Handled by WrapConfirmationDialog
        }

        is DocumentDetailsBottomSheetContent.BookmarkStoredInfo -> {
            SimpleBottomSheet(
                textData = sheetContent.bottomSheetTextData,
                leadingIcon = AppIcons.BookmarkFilled,
                leadingIconTint = MaterialTheme.colorScheme.warning,
            )
        }

        is DocumentDetailsBottomSheetContent.BookmarkRemovedInfo -> {
            SimpleBottomSheet(
                textData = sheetContent.bottomSheetTextData,
                leadingIcon = AppIcons.BookmarkFilled,
                leadingIconTint = MaterialTheme.colorScheme.error,
            )
        }

        is DocumentDetailsBottomSheetContent.TrustedRelyingPartyInfo -> {
            SimpleBottomSheet(
                textData = sheetContent.bottomSheetTextData,
                leadingIcon = AppIcons.Verified,
                leadingIconTint = MaterialTheme.colorScheme.success,
            )
        }
    }
}

@Composable
private fun IssuerDetails(
    modifier: Modifier = Modifier,
    sectionTitle: String,
    issuerName: String?,
    issuerLogo: URI?,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(SPACING_MEDIUM.dp),
    ) {
        SectionTitle(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        start = SPACING_SMALL.dp,
                        end = SPACING_SMALL.dp,
                        top = SPACING_LARGE.dp,
                        bottom = SPACING_SMALL.dp,
                    ),
            text = sectionTitle.toSentenceCaseHeading(),
            textConfig =
                TextConfig(
                    style =
                        MaterialTheme.typography.headlineSmall.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                        ),
                ),
        )
        IssuerDetailsCard(
            modifier = Modifier.fillMaxWidth(),
            item =
                IssuerDetailsCardDataUi(
                    issuerName = issuerName,
                    issuerLogo = issuerLogo,
                    issuerIsVerified = false,
                ),
            onClick = null,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DocumentCredentialsSection(
    modifier: Modifier = Modifier,
    state: State,
    onUpdateClicked: () -> Unit,
    info: DocumentCredentialsInfoUi,
) {
    val expandedInfo = info.expandedInfo
    val credentialsInfo = "${info.availableCredentials} / ${info.totalCredentials}"
    val supportingLines =
        buildList {
            add(stringResource(R.string.home_screen_document_usages_left, credentialsInfo))
            if (state.expiresAt.isNotBlank() && state.expiresAt != "-") {
                add(stringResource(R.string.home_screen_document_expires, state.expiresAt))
            }
        }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SPACING_SMALL.dp),
    ) {
        DocumentIdentityCard(
            title = state.title.orEmpty(),
            identification = state.documentDetailsUi?.documentIdentifier?.toCardIdentificationTag() ?: stringResource(R.string.generic_dash),
            supportingLines = supportingLines,
            status = state.documentDetailsUi?.documentIssuanceStateUi?.toStatusLabel(),
            onClick = null,
        )

        expandedInfo?.updateNowButtonText?.let { safeUpdateNowButtonText ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
            ) {
                WrapButton(
                    modifier = Modifier.wrapContentWidth(),
                    buttonConfig =
                        ButtonConfig(
                            type = ButtonType.PRIMARY,
                            onClick = onUpdateClicked,
                        ),
                ) {
                    Text(
                        text = safeUpdateNowButtonText,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun DocumentIssuanceStateUi.toStatusLabel(): String? =
    when (this) {
        DocumentIssuanceStateUi.Issued -> null
        DocumentIssuanceStateUi.Pending -> stringResource(R.string.dashboard_document_deferred_pending)
        DocumentIssuanceStateUi.Failed -> stringResource(R.string.dashboard_document_deferred_failed)
        DocumentIssuanceStateUi.Expired -> stringResource(R.string.dashboard_document_has_expired)
        DocumentIssuanceStateUi.Revoked -> stringResource(R.string.dashboard_document_revoked)
    }

@Composable
private fun DocumentDetails(
    modifier: Modifier = Modifier,
    onEventSend: (Event) -> Unit,
    sectionTitle: String,
    documentDetailsUi: DocumentDetailsUi,
    hideSensitiveContent: Boolean,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(SPACING_MEDIUM.dp),
    ) {
        SectionTitle(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        top = SPACING_LARGE.dp,
                        bottom = SPACING_SMALL.dp,
                    ),
            text = sectionTitle.toSentenceCaseHeading(),
            textConfig =
                TextConfig(
                    style =
                        MaterialTheme.typography.headlineSmall.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                        ),
                ),
        )

        WrapListItems(
            modifier = Modifier.fillMaxWidth(),
            items = documentDetailsUi.documentClaims,
            hideSensitiveContent = hideSensitiveContent,
            onExpandedChange = { item ->
                onEventSend(Event.ClaimClicked(itemId = item.itemId))
            },
            onItemClick = null,
            throttleClicks = false,
        )
    }
}

private fun String.toSentenceCaseHeading(): String {
    val locale = Locale.getDefault()
    return if (this == this.uppercase(locale)) {
        this
            .lowercase(locale)
            .replaceFirstChar { char ->
                if (char.isLowerCase()) {
                    char.titlecase(locale)
                } else {
                    char.toString()
                }
            }
    } else {
        this
    }
}

@Composable
private fun ButtonsSection(onEventSend: (Event) -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    vertical = SPACING_MEDIUM.dp,
                ).navigationBarsPadding(),
    ) {
        WrapButton(
            modifier =
                Modifier
                    .testTag(TestTag.DocumentDetailsScreen.DELETE_BUTTON)
                    .fillMaxWidth(),
            buttonConfig =
                ButtonConfig(
                    type = ButtonType.SECONDARY,
                    onClick = { onEventSend(Event.SecondaryButtonPressed) },
                    isWarning = true,
                ),
        ) {
            Text(
                text = stringResource(id = R.string.document_details_secondary_button_text),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@ThemeModePreviews
@Composable
private fun DocumentDetailsScreenPreview() {
    PreviewTheme {
        val availableCredentials = 3
        val totalCredentials = 15
        val state =
            State(
                documentCredentialsInfoUi =
                    DocumentCredentialsInfoUi(
                        availableCredentials = availableCredentials,
                        totalCredentials = totalCredentials,
                        title =
                            stringResource(
                                R.string.document_details_document_credentials_info_text,
                                availableCredentials,
                                totalCredentials,
                            ),
                        collapsedInfo =
                            DocumentCredentialsInfoUi.CollapsedInfo(
                                moreInfoText = stringResource(R.string.document_details_document_credentials_info_more_info_text),
                            ),
                        expandedInfo =
                            DocumentCredentialsInfoUi.ExpandedInfo(
                                subtitle = stringResource(R.string.document_details_document_credentials_info_expanded_text_subtitle),
                                updateNowButtonText = stringResource(R.string.document_details_document_credentials_info_expanded_button_update_now_text),
                                hideButtonText = stringResource(R.string.document_details_document_credentials_info_expanded_button_hide_text),
                            ),
                        isExpanded = false,
                    ),
                documentDetailsSectionTitle = stringResource(R.string.document_details_main_section_text),
                documentIssuerSectionTitle = stringResource(R.string.document_details_issuer_section_text),
                documentDetailsUi =
                    DocumentDetailsUi(
                        documentId = "1",
                        documentName = "Mobile Driving License",
                        documentIdentifier = DocumentIdentifier.OTHER(formatType = "org.iso.18013.5.1.mDL"),
                        documentClaims =
                            listOf(
                                ExpandableListItemUi.SingleListItem(
                                    header =
                                        ListItemDataUi(
                                            itemId = "1",
                                            mainContentData = ListItemMainContentDataUi.Text(text = ""),
                                            overlineText = "A reproduction of the mDL holder’s portrait.",
                                            leadingContentData =
                                                ListItemLeadingContentDataUi.UserImage(
                                                    userBase64Image = "",
                                                ),
                                        ),
                                ),
                                ExpandableListItemUi.SingleListItem(
                                    header =
                                        ListItemDataUi(
                                            itemId = "2",
                                            mainContentData = ListItemMainContentDataUi.Text(text = "GR"),
                                            overlineText = "Alpha-2 country code, as defined in ISO 3166-1 of the issuing authority’s country or territory.",
                                        ),
                                ),
                                ExpandableListItemUi.SingleListItem(
                                    header =
                                        ListItemDataUi(
                                            itemId = "3",
                                            mainContentData = ListItemMainContentDataUi.Text(text = "12345678900"),
                                            overlineText = "An audit control number assigned by the issuing authority.",
                                        ),
                                ),
                                ExpandableListItemUi.SingleListItem(
                                    header =
                                        ListItemDataUi(
                                            itemId = "4",
                                            mainContentData = ListItemMainContentDataUi.Text(text = "31 Dec 2040"),
                                            overlineText = "Date when mDL expires.",
                                        ),
                                ),
                            ),
                        documentIssuanceStateUi = DocumentIssuanceStateUi.Issued,
                    ),
                issuerName = "Digital Credentials Issuer",
                hideSensitiveContent = false,
                sheetContent = DocumentDetailsBottomSheetContent.DeleteDocumentConfirmation,
            )

        Content(
            state = state,
            effectFlow = Channel<Effect>().receiveAsFlow(),
            onEventSend = {},
            onNavigationRequested = {},
            paddingValues = PaddingValues(SPACING_MEDIUM.dp),
            coroutineScope = rememberCoroutineScope(),
            modalBottomSheetState = rememberModalBottomSheetState(),
        )
    }
}

@ThemeModePreviews
@Composable
private fun ExpandableDocumentCredentialsSectionPreview() {
    PreviewTheme {
        val availableCredentials = 3
        val totalCredentials = 15
        val documentCredentialsInfoUi =
            DocumentCredentialsInfoUi(
                availableCredentials = availableCredentials,
                totalCredentials = totalCredentials,
                title =
                    stringResource(
                        R.string.document_details_document_credentials_info_text,
                        availableCredentials,
                        totalCredentials,
                    ),
                collapsedInfo =
                    DocumentCredentialsInfoUi.CollapsedInfo(
                        moreInfoText = stringResource(R.string.document_details_document_credentials_info_more_info_text),
                    ),
                expandedInfo =
                    DocumentCredentialsInfoUi.ExpandedInfo(
                        subtitle = stringResource(R.string.document_details_document_credentials_info_expanded_text_subtitle),
                        updateNowButtonText = stringResource(R.string.document_details_document_credentials_info_expanded_button_update_now_text),
                        hideButtonText = stringResource(R.string.document_details_document_credentials_info_expanded_button_hide_text),
                    ),
                isExpanded = false,
            )

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(all = SPACING_MEDIUM.dp),
            verticalArrangement = Arrangement.spacedBy(SPACING_MEDIUM.dp),
        ) {
        }
    }
}
