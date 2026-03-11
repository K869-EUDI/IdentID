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

import android.Manifest
import android.app.Activity
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.k689.identid.R
import com.k689.identid.extension.ui.paddingFrom
import com.k689.identid.navigation.TransferScreens
import com.k689.identid.service.NfcServiceModeController
import com.k689.identid.ui.component.AppIcons
import com.k689.identid.ui.component.content.ContentScreen
import com.k689.identid.ui.component.content.ContentTitle
import com.k689.identid.ui.component.content.ScreenNavigateAction
import com.k689.identid.ui.component.utils.LifecycleEffect
import com.k689.identid.ui.component.utils.SPACING_MEDIUM
import com.k689.identid.ui.component.utils.SPACING_SMALL
import com.k689.identid.ui.component.utils.screenWidthInDp
import com.k689.identid.ui.component.wrap.WrapImage
import com.k689.identid.ui.proximity.qr.component.rememberQrBitmapPainter
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MoveWalletScreen(
    navController: NavController,
    viewModel: MoveWalletViewModel,
) {
    val state: MoveWalletState by viewModel.viewState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? Activity

    // Split into two groups: Android won't show location + nearby in same dialog
    val nearbyPermissions =
        remember {
            buildList {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    add(Manifest.permission.BLUETOOTH_ADVERTISE)
                    add(Manifest.permission.BLUETOOTH_SCAN)
                    add(Manifest.permission.BLUETOOTH_CONNECT)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2) {
                    add(Manifest.permission.NEARBY_WIFI_DEVICES)
                }
            }
        }

    val locationPermissions =
        remember {
            listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            )
        }

    val nearbyPermissionsState = rememberMultiplePermissionsState(nearbyPermissions)
    val locationPermissionsState = rememberMultiplePermissionsState(locationPermissions)

    val allGranted = nearbyPermissionsState.allPermissionsGranted && locationPermissionsState.allPermissionsGranted

    // Phase 1: Request nearby permissions
    LaunchedEffect(Unit) {
        if (!nearbyPermissionsState.allPermissionsGranted) {
            nearbyPermissionsState.launchMultiplePermissionRequest()
        }
    }

    // Phase 2: After nearby permissions granted, request location
    LaunchedEffect(nearbyPermissionsState.allPermissionsGranted) {
        if (nearbyPermissionsState.allPermissionsGranted && !locationPermissionsState.allPermissionsGranted) {
            locationPermissionsState.launchMultiplePermissionRequest()
        }
    }

    // Init when all permissions are granted
    LaunchedEffect(allGranted) {
        if (allGranted) {
            viewModel.setEvent(MoveWalletEvent.Init(context))
        }
    }

    ContentScreen(
        isLoading = state.isLoading,
        navigatableAction = ScreenNavigateAction.BACKABLE,
        onBack = { viewModel.setEvent(MoveWalletEvent.GoBack(context)) },
        contentErrorConfig = state.error,
    ) { paddingValues ->
        Content(
            state = state,
            paddingValues = paddingValues,
        )
    }

    LaunchedEffect(Unit) {
        viewModel.effect
            .onEach { effect ->
                when (effect) {
                    is MoveWalletEffect.Navigation.Pop -> {
                        navController.popBackStack()
                    }

                    is MoveWalletEffect.Navigation.SwitchScreen -> {
                        navController.navigate(effect.screenRoute) {
                            popUpTo(TransferScreens.MoveWallet.screenRoute) {
                                inclusive = true
                            }
                        }
                    }
                }
            }.collect()
    }

    LifecycleEffect(
        lifecycleOwner = LocalLifecycleOwner.current,
        lifecycleEvent = Lifecycle.Event.ON_RESUME,
    ) {
        activity?.let { NfcServiceModeController.activateTransferMode(it) }
    }

    LifecycleEffect(
        lifecycleOwner = LocalLifecycleOwner.current,
        lifecycleEvent = Lifecycle.Event.ON_PAUSE,
    ) {
        activity?.let { NfcServiceModeController.activateCredentialSharingMode(it) }
    }
}

@Composable
private fun Content(
    state: MoveWalletState,
    paddingValues: PaddingValues,
) {
    val qrSize = screenWidthInDp(true) / 1.4f

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .paddingFrom(paddingValues, bottom = false),
        ) {
            ContentTitle(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(id = R.string.transfer_move_wallet_title),
                subtitle = stringResource(id = R.string.transfer_move_wallet_subtitle),
            )

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                if (state.qrCode.isNotEmpty()) {
                    WrapImage(
                        painter =
                            rememberQrBitmapPainter(
                                content = state.qrCode,
                                size = qrSize,
                            ),
                        contentDescription = stringResource(id = R.string.content_description_qr_code_icon),
                    )
                }
            }
        }

        Column {
            HorizontalDivider()
            NFCSection(paddingValues)
        }
    }
}

@Composable
private fun NFCSection(paddingValues: PaddingValues) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(
                    start = SPACING_MEDIUM.dp,
                    end = SPACING_MEDIUM.dp,
                    top = SPACING_MEDIUM.dp,
                    bottom = paddingValues.calculateBottomPadding(),
                ),
        verticalArrangement = Arrangement.spacedBy(SPACING_SMALL.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(id = R.string.transfer_move_wallet_use_nfc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        WrapImage(iconData = AppIcons.NFC)
        Text(
            text = stringResource(id = R.string.transfer_move_wallet_hold_near_device),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
