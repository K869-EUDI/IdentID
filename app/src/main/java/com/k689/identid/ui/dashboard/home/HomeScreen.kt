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

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MobileFriendly
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.k689.identid.R
import com.k689.identid.extension.ui.finish
import com.k689.identid.extension.ui.openAppSettings
import com.k689.identid.extension.ui.openBleSettings
import com.k689.identid.ui.component.AppIcons
import com.k689.identid.ui.component.ListItemMainContentDataUi
import com.k689.identid.ui.component.content.ContentScreen
import com.k689.identid.ui.component.content.ScreenNavigateAction
import com.k689.identid.ui.component.preview.PreviewTheme
import com.k689.identid.ui.component.preview.ThemeModePreviews
import com.k689.identid.ui.component.utils.SPACING_EXTRA_LARGE
import com.k689.identid.ui.component.utils.SPACING_LARGE
import com.k689.identid.ui.component.utils.SPACING_MEDIUM
import com.k689.identid.ui.component.utils.SPACING_SMALL
import com.k689.identid.ui.component.wrap.ActionCardConfig
import com.k689.identid.ui.component.wrap.BottomSheetTextDataUi
import com.k689.identid.ui.component.wrap.DialogBottomSheet
import com.k689.identid.ui.component.wrap.WrapIconButton
import com.k689.identid.ui.component.wrap.WrapModalBottomSheet
import com.k689.identid.ui.dashboard.component.BottomNavigationItem
import com.k689.identid.ui.dashboard.transactions.model.TransactionStatusUi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

typealias DashboardEvent = com.k689.identid.ui.dashboard.dashboard.Event
typealias OpenSideMenuEvent = com.k689.identid.ui.dashboard.dashboard.Event.SideMenu.Open

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navHostController: NavController,
    viewModel: HomeViewModel,
    onDashboardEventSent: (DashboardEvent) -> Unit,
) {
    val context = LocalContext.current
    val state: State by viewModel.viewState.collectAsStateWithLifecycle()
    val isBottomSheetOpen = state.isBottomSheetOpen
    val scope = rememberCoroutineScope()

    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val scaffoldState =
        rememberBottomSheetScaffoldState(
            bottomSheetState =
                rememberStandardBottomSheetState(
                    initialValue = SheetValue.PartiallyExpanded,
                ),
        )

    ContentScreen(
        isLoading = state.isLoading,
        navigatableAction = ScreenNavigateAction.NONE,
        onBack = { context.finish() },
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            BottomSheetScaffold(
                modifier = Modifier.fillMaxSize().statusBarsPadding(),
                scaffoldState = scaffoldState,
                sheetShadowElevation = 16.dp,
                sheetPeekHeight = 410.dp,
                sheetContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                sheetDragHandle = { BottomSheetDefaults.DragHandle() },
                sheetContent = {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxHeight()
                                .padding(bottom = paddingValues.calculateBottomPadding()),
                    ) {
                        Text(
                            text = stringResource(R.string.recent_transactions),
                            style =
                                MaterialTheme.typography.headlineSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                ),
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        horizontal = SPACING_LARGE.dp,
                                        vertical = SPACING_SMALL.dp,
                                    ),
                        )

                        val recentTransactions = state.allTransactions.filter { !it.isPending }

                        if (recentTransactions.isEmpty()) {
                            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                Text(
                                    text = "No recent transactions",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier =
                                        Modifier.align(Alignment.Center),
                                )
                            }
                        } else {
                            RecentTransactionsList(
                                transactions = recentTransactions,
                                onTransactionClicked = { viewModel.setEvent(Event.TransactionClicked(it)) },
                                modifier = Modifier.fillMaxWidth().weight(1f),
                            )
                        }

                        HomeScreenSheetContent(
                            sheetContent = state.sheetContent,
                            onEventSent = { event -> viewModel.setEvent(event) },
                        )
                    }
                },
            ) { scaffoldPadding ->
                Column(modifier = Modifier.fillMaxSize()) {
                    TopBar(onDashboardEventSent = onDashboardEventSent)
                    Content(
                        state = state,
                        effectFlow = viewModel.effect,
                        onEventSent = { viewModel.setEvent(it) },
                        onNavigationRequested = { handleNavigationEffect(it, navHostController, context, onDashboardEventSent) },
                        coroutineScope = scope,
                        bottomSheetState = scaffoldState.bottomSheetState,
                        paddingValues =
                            PaddingValues(
                                top = scaffoldPadding.calculateTopPadding(),
                                bottom = scaffoldPadding.calculateBottomPadding(),
                            ),
                        onDashboardEventSent = onDashboardEventSent,
                    )
                }
            }

            Surface(
                modifier =
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(
                            start = SPACING_LARGE.dp,
                            bottom = SPACING_LARGE.dp + paddingValues.calculateBottomPadding(),
                        ),
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                shadowElevation = 4.dp,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                ) {
                    IconButton(onClick = { viewModel.setEvent(Event.AuthenticateCard.AuthenticatePressed) }) {
                        Icon(
                            imageVector = Icons.Default.MobileFriendly,
                            contentDescription = "Authenticate",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    IconButton(onClick = { viewModel.setEvent(Event.SignDocumentCard.SignDocumentPressed) }) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Sign document",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }

            FloatingActionButton(
                onClick = { viewModel.setEvent(Event.AddDocumentsClicked) },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(16.dp),
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(
                            end = SPACING_LARGE.dp,
                            bottom = SPACING_LARGE.dp + paddingValues.calculateBottomPadding(),
                        ),
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add document",
                )
            }
        }
    }

    if (isBottomSheetOpen && state.sheetContent != HomeScreenBottomSheetContent.None) {
        WrapModalBottomSheet(
            onDismissRequest = {
                viewModel.setEvent(
                    Event.BottomSheet.UpdateBottomSheetState(
                        isOpen = false,
                    ),
                )
            },
            sheetState = bottomSheetState,
        ) {
            HomeScreenSheetContent(
                sheetContent = state.sheetContent,
                onEventSent = { event -> viewModel.setEvent(event) },
            )
        }
    }

    // Refresh transactions and state when the screen comes back into focus
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    viewModel.setEvent(Event.Init)
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

@Composable
private fun TopBar(
    onDashboardEventSent: (DashboardEvent) -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = SPACING_LARGE.dp,
                    vertical = SPACING_SMALL.dp,
                ),
    ) {
        // home menu icon
        WrapIconButton(
            modifier = Modifier.align(Alignment.CenterStart),
            iconData = AppIcons.Menu,
            customTint = MaterialTheme.colorScheme.onSurface,
        ) {
            onDashboardEventSent(OpenSideMenuEvent)
        }

        Text(
            modifier = Modifier.align(Alignment.Center),
            text = stringResource(R.string.app_title),
            style = MaterialTheme.typography.titleLarge,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Content(
    state: State,
    effectFlow: Flow<Effect>,
    onEventSent: ((event: Event) -> Unit),
    onNavigationRequested: (navigationEffect: Effect.Navigation) -> Unit,
    coroutineScope: CoroutineScope,
    bottomSheetState: SheetState,
    paddingValues: PaddingValues,
    onDashboardEventSent: (DashboardEvent) -> Unit,
) {
    val scrollState = rememberScrollState()

    val recentDocs = state.allDocuments.filter { !it.isPending }.take(3)

    val pageCount = if (recentDocs.isEmpty()) 1 else recentDocs.size + 1
    val pagerState = rememberPagerState(pageCount = { pageCount })

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(
                    top = paddingValues.calculateTopPadding(),
                    bottom = paddingValues.calculateBottomPadding(),
                ).verticalScroll(scrollState),
    ) {
        Text(
            text = state.welcomeUserMessage,
            style =
                MaterialTheme.typography.headlineMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
            modifier =
                Modifier.padding(
                    start = SPACING_EXTRA_LARGE.dp,
                    end = SPACING_EXTRA_LARGE.dp,
                    top = SPACING_SMALL.dp,
                    bottom = SPACING_EXTRA_LARGE.dp,
                ),
        )

        HorizontalPager(
            state = pagerState,
            modifier =
                Modifier
                    .fillMaxWidth()
                    // Increased height to give room for shadows and elevation
                    .height(220.dp),
            contentPadding = PaddingValues(horizontal = 32.dp, vertical = 8.dp),
            pageSpacing = 16.dp,
            verticalAlignment = Alignment.CenterVertically,
            beyondViewportPageCount = 1,
        ) { page ->
            if (page < recentDocs.size) {
                DocumentCard(
                    document = recentDocs[page],
                    onClicked = { onEventSent(Event.DocumentClicked(recentDocs[page].documentUi.uiData.itemId)) },
                    modifier =
                        Modifier
                            .graphicsLayer {
                                val pageOffset =
                                    (
                                        (pagerState.currentPage - page) +
                                            pagerState.currentPageOffsetFraction
                                    ).absoluteValue
                                alpha =
                                    lerp(
                                        start = 0.8f,
                                        stop = 1f,
                                        fraction = 1f - pageOffset.coerceIn(0f, 1f),
                                    )
                                scaleY =
                                    lerp(
                                        start = 0.9f,
                                        stop = 1f,
                                        fraction = 1f - pageOffset.coerceIn(0f, 1f),
                                    )
                                clip = false
                            },
                )
            } else {
                SeeAllDocumentsCard(
                    recentDocsCount = recentDocs.size,
                    onClicked = {
                        if (recentDocs.isEmpty()) {
                            onEventSent(Event.AddDocumentsClicked)
                        } else {
                            onDashboardEventSent(
                                com.k689.identid.ui.dashboard.dashboard.Event
                                    .SwitchTab(BottomNavigationItem.Documents),
                            )
                        }
                    },
                    modifier =
                        Modifier
                            .graphicsLayer {
                                val pageOffset =
                                    (
                                        (pagerState.currentPage - page) +
                                            pagerState.currentPageOffsetFraction
                                    ).absoluteValue
                                alpha =
                                    lerp(
                                        start = 0.8f,
                                        stop = 1f,
                                        fraction = 1f - pageOffset.coerceIn(0f, 1f),
                                    )
                                scaleY =
                                    lerp(
                                        start = 0.9f,
                                        stop = 1f,
                                        fraction = 1f - pageOffset.coerceIn(0f, 1f),
                                    )
                                clip = false
                            },
                )
            }
        }
    }

    if (state.bleAvailability == BleAvailability.NO_PERMISSION) {
        RequiredPermissionsAsk(state, onEventSent)
    }

    LaunchedEffect(Unit) {
        effectFlow
            .onEach { effect ->
                when (effect) {
                    is Effect.Navigation -> {
                        onNavigationRequested(effect)
                    }

                    is Effect.CloseBottomSheet -> {
                        coroutineScope.launch {
                            bottomSheetState.partialExpand()
                        }
                        onEventSent(Event.BottomSheet.UpdateBottomSheetState(isOpen = false))
                    }

                    is Effect.ShowBottomSheet -> {
                        onEventSent(Event.BottomSheet.UpdateBottomSheetState(isOpen = true))
                    }

                    is Effect.SwitchTab -> {
                        onDashboardEventSent(
                            com.k689.identid.ui.dashboard.dashboard.Event
                                .SwitchTab(effect.tab),
                        )
                    }
                }
            }.collect()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DocumentCard(
    document: DashboardDocument,
    onClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClicked,
        modifier =
            modifier
                .fillMaxSize(),
        shape = RoundedCornerShape(16.dp), // Snappier corners closer to real card shapes
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = SPACING_LARGE.dp, vertical = SPACING_MEDIUM.dp),
            verticalAlignment = Alignment.CenterVertically, // Keeping everything neatly centered vertically
        ) {
            // Placeholder area for the future user avatar/icon
            Box(
                modifier =
                    Modifier
                        .size(72.dp) // Adjusted sizing due to taller card space
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Avatar Placeholder",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(36.dp),
                )
            }

            Spacer(modifier = Modifier.width(SPACING_LARGE.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
            ) {
                val docName = (document.documentUi.uiData.mainContentData as? ListItemMainContentDataUi.Text)?.text ?: ""
                Text(
                    text = docName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Usages left: ${document.usagesLeft}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Expires: ${document.expiresAt}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SeeAllDocumentsCard(
    recentDocsCount: Int,
    onClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (recentDocsCount == 0) {
        Box(
            modifier = modifier.fillMaxSize().clickable { onClicked() },
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "No documents",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Add one now",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    } else {
        Card(
            onClick = onClicked,
            modifier =
                modifier
                    .fillMaxSize(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = stringResource(R.string.generic_view_details),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentTransactionsList(
    transactions: List<DashboardTransaction>,
    onTransactionClicked: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
    ) {
        items(
            items = transactions,
            key = { it.transactionUi.uiData.header.itemId },
        ) { transaction ->
            RecentTransactionItem(
                transaction = transaction,
                onTransactionClicked = onTransactionClicked,
            )
        }
    }
}

@Composable
private fun RecentTransactionItem(
    transaction: DashboardTransaction,
    onTransactionClicked: (String) -> Unit,
) {
    val itemId = transaction.transactionUi.uiData.header.itemId
    val header = transaction.transactionUi.uiData.header
    val status = transaction.transactionUi.uiStatus
    val isFailed = status == TransactionStatusUi.Failed

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onTransactionClicked(itemId) }
                .padding(horizontal = SPACING_LARGE.dp, vertical = SPACING_MEDIUM.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SPACING_MEDIUM.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = (header.mainContentData as? ListItemMainContentDataUi.Text)?.text ?: "",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = header.supportingText ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Icon(
            imageVector = if (isFailed) Icons.Default.Cancel else Icons.Default.CheckCircle,
            contentDescription = status.name,
            tint = if (isFailed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp),
        )
    }
}

private fun handleNavigationEffect(
    navigationEffect: Effect.Navigation,
    navController: NavController,
    context: Context,
    onDashboardEventSent: (DashboardEvent) -> Unit,
) {
    when (navigationEffect) {
        is Effect.Navigation.SwitchScreen -> {
            navController.navigate(navigationEffect.screenRoute) {
                popUpTo(navigationEffect.popUpToScreenRoute) {
                    inclusive = navigationEffect.inclusive
                }
            }
        }

        is Effect.Navigation.OnAppSettings -> {
            context.openAppSettings()
        }

        is Effect.Navigation.OnSystemSettings -> {
            context.openBleSettings()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreenSheetContent(
    sheetContent: HomeScreenBottomSheetContent,
    onEventSent: (event: Event) -> Unit,
) {
    when (sheetContent) {
        is HomeScreenBottomSheetContent.Bluetooth -> {
            DialogBottomSheet(
                textData =
                    BottomSheetTextDataUi(
                        title = stringResource(id = R.string.dashboard_bottom_sheet_bluetooth_title),
                        message = stringResource(id = R.string.dashboard_bottom_sheet_bluetooth_subtitle),
                        positiveButtonText = stringResource(id = R.string.dashboard_bottom_sheet_bluetooth_primary_button_text),
                        negativeButtonText = stringResource(id = R.string.dashboard_bottom_sheet_bluetooth_secondary_button_text),
                    ),
                onPositiveClick = {
                    onEventSent(
                        Event.BottomSheet.Bluetooth.PrimaryButtonPressed(
                            sheetContent.availability,
                        ),
                    )
                },
                onNegativeClick = { onEventSent(Event.BottomSheet.Bluetooth.SecondaryButtonPressed) },
            )
        }

        else -> {
            // Placeholder: Learn more modals can be added here
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun RequiredPermissionsAsk(
    state: State,
    onEventSend: (Event) -> Unit,
) {
    val permissions: MutableList<String> = mutableListOf()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
        permissions.add(Manifest.permission.BLUETOOTH_SCAN)
        permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
    }

    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2 && state.isBleCentralClientModeEnabled) {
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    val permissionsState = rememberMultiplePermissionsState(permissions = permissions)

    LaunchedEffect(permissionsState.allPermissionsGranted, permissionsState.shouldShowRationale) {
        when {
            permissionsState.allPermissionsGranted -> {
                onEventSend(Event.StartProximityFlow)
            }

            !permissionsState.allPermissionsGranted && permissionsState.shouldShowRationale -> {
                onEventSend(Event.OnShowPermissionsRational)
            }

            else -> {
                onEventSend(Event.OnPermissionStateChanged(BleAvailability.UNKNOWN))
                permissionsState.launchMultiplePermissionRequest()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@ThemeModePreviews
@Composable
private fun HomeScreenContentPreview() {
    PreviewTheme {
        val scaffoldState =
            rememberBottomSheetScaffoldState(
                bottomSheetState =
                    rememberStandardBottomSheetState(
                        initialValue = SheetValue.PartiallyExpanded,
                    ),
            )

        ContentScreen(
            isLoading = false,
            navigatableAction = ScreenNavigateAction.NONE,
            onBack = { },
        ) { paddingValues ->
            BottomSheetScaffold(
                modifier = Modifier.fillMaxSize(),
                scaffoldState = scaffoldState,
                sheetPeekHeight = 400.dp + paddingValues.calculateBottomPadding(),
                sheetContent = {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxHeight()
                                .padding(bottom = paddingValues.calculateBottomPadding()),
                    ) {}
                },
            ) { scaffoldPadding ->
                Column(modifier = Modifier.fillMaxSize()) {
                    TopBar(onDashboardEventSent = {})
                    Content(
                        state =
                            State(
                                isBottomSheetOpen = false,
                                welcomeUserMessage = "Welcome back, Alex",
                                authenticateCardConfig =
                                    ActionCardConfig(
                                        title = stringResource(R.string.home_screen_authentication_card_title),
                                        icon = AppIcons.WalletActivated,
                                        primaryButtonText = stringResource(R.string.home_screen_authenticate),
                                        secondaryButtonText = stringResource(R.string.home_screen_learn_more),
                                    ),
                                signCardConfig =
                                    ActionCardConfig(
                                        title = stringResource(R.string.home_screen_sign_card_title),
                                        icon = AppIcons.Contract,
                                        primaryButtonText = stringResource(R.string.home_screen_sign),
                                        secondaryButtonText = stringResource(R.string.home_screen_learn_more),
                                    ),
                            ),
                        effectFlow = Channel<Effect>().receiveAsFlow(),
                        onNavigationRequested = {},
                        coroutineScope = rememberCoroutineScope(),
                        bottomSheetState = scaffoldState.bottomSheetState,
                        onEventSent = {},
                        paddingValues =
                            PaddingValues(
                                top = scaffoldPadding.calculateTopPadding(),
                                bottom = scaffoldPadding.calculateBottomPadding(),
                            ),
                        onDashboardEventSent = {},
                    )
                }
            }
        }
    }
}
