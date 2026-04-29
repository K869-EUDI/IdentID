package com.k689.identid.ui.dashboard.loyaltycards.scan

import android.net.Uri
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.k689.identid.R
import com.k689.identid.extension.ui.throttledClickable
import com.k689.identid.extension.ui.openAppSettings
import com.k689.identid.navigation.DashboardScreens
import com.k689.identid.navigation.helper.generateComposableArguments
import com.k689.identid.navigation.helper.generateComposableNavigationLink
import com.k689.identid.ui.common.scan.component.qrBorderCanvas
import com.k689.identid.ui.component.content.ContentScreen
import com.k689.identid.ui.component.content.ContentTitle
import com.k689.identid.ui.component.ErrorInfo
import com.k689.identid.ui.component.content.ScreenNavigateAction
import com.k689.identid.ui.component.utils.SIZE_100
import com.k689.identid.ui.component.utils.SIZE_EXTRA_SMALL
import com.k689.identid.ui.component.utils.SIZE_LARGE
import com.k689.identid.ui.component.utils.SPACING_SMALL
import com.k689.identid.ui.component.utils.screenWidthInDp
import com.k689.identid.ui.component.wrap.WrapCard
import com.k689.identid.ui.component.wrap.WrapIcon
import com.k689.identid.ui.component.AppIcons
import com.k689.identid.ui.dashboard.loyaltycards.scan.component.BarcodeAnalyzer

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LoyaltyCardScanScreen(
    navController: NavController,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val permissionState = rememberPermissionState(permission = android.Manifest.permission.CAMERA)
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var finishedScanning by remember { mutableStateOf(false) }
    var cameraBindingFailed by remember { mutableStateOf(false) }
    val hasPermission = permissionState.status.isGranted
    val shouldShowPermissionRationale = permissionState.status.shouldShowRationale
    val barcodeAnalyzer =
        remember {
            BarcodeAnalyzer { result ->
                if (finishedScanning) {
                    return@BarcodeAnalyzer
                }
                finishedScanning = true
                navController.navigate(
                    generateComposableNavigationLink(
                        screen = DashboardScreens.LoyaltyCardCreate,
                        arguments = generateComposableArguments(
                            mapOf(
                                "barcodeValue" to Uri.encode(result.value),
                                "barcodeFormat" to Uri.encode(result.format),
                            ),
                        ),
                    ),
                ) {
                    popUpTo(DashboardScreens.LoyaltyCardScan.screenRoute) { inclusive = true }
                }
            }
        }
    val scannerAreaSize = screenWidthInDp(true) - SIZE_100.dp

    DisposableEffect(cameraProviderFuture, barcodeAnalyzer) {
        onDispose {
            barcodeAnalyzer.close()
            if (cameraProviderFuture.isDone) {
                runCatching { cameraProviderFuture.get().unbindAll() }
            }
        }
    }

    if (!hasPermission && !shouldShowPermissionRationale) {
        LaunchedEffect(Unit) {
            permissionState.launchPermissionRequest()
        }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            finishedScanning = false
        }
        cameraBindingFailed = false
    }

    ContentScreen(
        navigatableAction = ScreenNavigateAction.BACKABLE,
        onBack = { navController.popBackStack() },
        isLoading = false,
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ContentTitle(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.loyalty_cards_scan_title),
                subtitle = stringResource(R.string.loyalty_cards_scan_subtitle),
            )

            Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                if (hasPermission) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { previewContext ->
                            val previewView = PreviewView(previewContext)
                            cameraProviderFuture.addListener(
                                {
                                    runCatching {
                                        val cameraProvider = cameraProviderFuture.get()
                                        val preview =
                                            Preview.Builder().build().also {
                                                it.surfaceProvider = previewView.surfaceProvider
                                            }
                                        val imageAnalysis =
                                            ImageAnalysis.Builder()
                                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                                .build().also { analysis ->
                                                    analysis.setAnalyzer(
                                                        ContextCompat.getMainExecutor(previewContext),
                                                        barcodeAnalyzer,
                                                    )
                                                }

                                        cameraProvider.unbindAll()
                                        cameraProvider.bindToLifecycle(
                                            lifecycleOwner,
                                            CameraSelector.DEFAULT_BACK_CAMERA,
                                            preview,
                                            imageAnalysis,
                                        )
                                    }.onFailure {
                                        cameraBindingFailed = true
                                    }
                                },
                                ContextCompat.getMainExecutor(previewContext),
                            )

                            previewView
                        },
                    )

                    Canvas(
                        modifier = Modifier.size(scannerAreaSize),
                    ) {
                        qrBorderCanvas(
                            borderColor = Color.White,
                            curve = 0.dp,
                            strokeWidth = SIZE_EXTRA_SMALL.dp,
                            capSize = SIZE_LARGE.dp,
                            gapAngle = SIZE_EXTRA_SMALL,
                            cap = StrokeCap.Square,
                        )
                    }

                    Box(
                        modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                    ) {
                        InformativeText(text = stringResource(R.string.loyalty_cards_scan_hint))
                    }
                    if (cameraBindingFailed) {
                        ErrorInfo(
                            informativeText = stringResource(id = R.string.generic_error_message),
                            contentColor = Color.White,
                            modifier = Modifier.padding(24.dp),
                        )
                    }
                } else if (shouldShowPermissionRationale) {
                    ErrorInfo(
                        informativeText = stringResource(id = R.string.qr_scan_permission_not_granted),
                        contentColor = Color.White,
                        isIconEnabled = true,
                        modifier = Modifier.padding(24.dp).throttledClickable { context.openAppSettings() },
                    )
                } else {
                    Text(
                        text = stringResource(R.string.loyalty_cards_scan_permission_wait),
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(24.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun InformativeText(text: String) {
    WrapCard {
        Box(
            modifier = Modifier.padding(all = SPACING_SMALL.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                WrapIcon(AppIcons.Error)
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}