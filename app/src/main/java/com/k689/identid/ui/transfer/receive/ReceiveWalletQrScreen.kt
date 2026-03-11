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

package com.k689.identid.ui.transfer.receive

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.k689.identid.R
import com.k689.identid.extension.ui.paddingFrom
import com.k689.identid.ui.common.scan.component.QrCodeAnalyzer
import com.k689.identid.ui.component.content.ContentScreen
import com.k689.identid.ui.component.content.ContentTitle
import com.k689.identid.ui.component.content.ScreenNavigateAction
import com.k689.identid.ui.component.utils.SPACING_MEDIUM
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ReceiveWalletQrScreen(
    navController: NavController,
    viewModel: ReceiveWalletViewModel,
) {
    val state: ReceiveWalletState by viewModel.viewState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var hasScanned by remember { mutableStateOf(false) }

    // Split permissions into two groups because Android won't show
    // location and nearby-devices prompts in the same dialog
    val nearbyPermissions =
        remember {
            buildList {
                add(Manifest.permission.CAMERA)
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
    var permissionRequested by remember { mutableStateOf(false) }

    val cameraGranted =
        nearbyPermissionsState.permissions
            .firstOrNull { it.permission == Manifest.permission.CAMERA }
            ?.status
            ?.isGranted == true

    val allGranted = nearbyPermissionsState.allPermissionsGranted && locationPermissionsState.allPermissionsGranted

    // Phase 1: Request nearby + camera permissions
    LaunchedEffect(Unit) {
        if (!nearbyPermissionsState.allPermissionsGranted) {
            nearbyPermissionsState.launchMultiplePermissionRequest()
        }
    }

    // Phase 2: After nearby permissions are granted, request location
    LaunchedEffect(nearbyPermissionsState.allPermissionsGranted) {
        if (nearbyPermissionsState.allPermissionsGranted && !locationPermissionsState.allPermissionsGranted) {
            locationPermissionsState.launchMultiplePermissionRequest()
            permissionRequested = true
        } else if (nearbyPermissionsState.allPermissionsGranted && locationPermissionsState.allPermissionsGranted) {
            permissionRequested = true
        }
    }

    // Also mark as requested when location state changes
    LaunchedEffect(locationPermissionsState.allPermissionsGranted) {
        if (locationPermissionsState.allPermissionsGranted) {
            permissionRequested = true
        }
    }

    LaunchedEffect(state.error) {
        if (state.error != null) {
            hasScanned = false
        }
    }

    ContentScreen(
        isLoading = false,
        navigatableAction = ScreenNavigateAction.BACKABLE,
        onBack = { viewModel.setEvent(ReceiveWalletEvent.GoBack) },
        contentErrorConfig = state.error,
    ) { paddingValues ->
        when {
            allGranted && cameraGranted && (hasScanned || state.isLoading) -> {
                ConnectingContent(
                    paddingValues = paddingValues,
                    title = stringResource(id = R.string.transfer_receive_qr_title),
                )
            }

            // All permissions granted → show camera
            allGranted && cameraGranted -> {
                CameraContent(
                    onQrScanned = { code ->
                        hasScanned = true
                        viewModel.setEvent(ReceiveWalletEvent.SessionScanned(context, code))
                    },
                    paddingValues = paddingValues,
                )
            }

            // Location denied after request → show settings prompt
            permissionRequested && !locationPermissionsState.allPermissionsGranted -> {
                val deniedPermissions =
                    locationPermissionsState.permissions
                        .filter { !it.status.isGranted }
                        .map { it.permission }
                PermissionDeniedContent(
                    paddingValues = paddingValues,
                    onOpenSettings = {
                        val intent =
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                        context.startActivity(intent)
                    },
                )
            }

            // Waiting for permission response
            else -> {
                PermissionContent(paddingValues = paddingValues)
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effect
            .onEach { effect ->
                when (effect) {
                    is ReceiveWalletEffect.Navigation.Pop -> {
                        navController.popBackStack()
                    }

                    is ReceiveWalletEffect.Navigation.SwitchScreen -> {
                        navController.navigate(effect.screenRoute)
                    }

                    is ReceiveWalletEffect.Navigation.NavigateToDashboard -> {
                        navController.navigate(effect.screenRoute) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            }.collect()
    }
}

@Composable
private fun ConnectingContent(
    paddingValues: PaddingValues,
    title: String,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .paddingFrom(paddingValues),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ContentTitle(
            modifier = Modifier.fillMaxWidth(),
            title = title,
        )

        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(id = R.string.transfer_receive_connecting_status),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun CameraContent(
    onQrScanned: (String) -> Unit,
    paddingValues: PaddingValues,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasScanned by remember { mutableStateOf(false) }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .paddingFrom(paddingValues),
    ) {
        ContentTitle(
            modifier = Modifier.fillMaxWidth(),
            title = stringResource(id = R.string.transfer_receive_qr_title),
            subtitle = stringResource(id = R.string.transfer_receive_qr_subtitle),
        )

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview =
                            Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }

                        val imageAnalysis =
                            ImageAnalysis
                                .Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                                .also {
                                    it.setAnalyzer(
                                        ContextCompat.getMainExecutor(ctx),
                                        QrCodeAnalyzer { result ->
                                            if (!hasScanned) {
                                                hasScanned = true
                                                onQrScanned(result)
                                            }
                                        },
                                    )
                                }

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageAnalysis,
                            )
                        } catch (_: Exception) {
                            // Camera binding can fail if lifecycle is destroyed
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
            )
        }
    }
}

@Composable
private fun PermissionContent(
    paddingValues: PaddingValues,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .paddingFrom(paddingValues),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ContentTitle(
            modifier = Modifier.fillMaxWidth(),
            title = stringResource(id = R.string.transfer_receive_qr_title),
        )

        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(id = R.string.transfer_receive_qr_permission_needed),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun PermissionDeniedContent(
    paddingValues: PaddingValues,
    onOpenSettings: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .paddingFrom(paddingValues),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ContentTitle(
            modifier = Modifier.fillMaxWidth(),
            title = stringResource(id = R.string.transfer_receive_qr_title),
        )

        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Location permission is required for nearby device transfer. Please grant it in Settings.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(SPACING_MEDIUM.dp))
                Button(onClick = onOpenSettings) {
                    Text(text = "Open Settings")
                }
            }
        }
    }
}
