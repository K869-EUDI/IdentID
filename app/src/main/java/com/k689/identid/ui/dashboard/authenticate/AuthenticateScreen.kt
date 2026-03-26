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

package com.k689.identid.ui.dashboard.authenticate

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.k689.identid.R
import com.k689.identid.ui.component.AppIcons
import com.k689.identid.ui.component.LargeActionFooter
import com.k689.identid.ui.component.content.ContentScreen
import com.k689.identid.ui.component.content.ContentTitle
import com.k689.identid.ui.component.content.ScreenNavigateAction
import com.k689.identid.ui.component.content.ToolbarConfig
import com.k689.identid.ui.component.preview.PreviewTheme
import com.k689.identid.ui.component.preview.ThemeModePreviews
import com.k689.identid.ui.component.utils.SPACING_EXTRA_SMALL
import com.k689.identid.ui.component.utils.SPACING_MEDIUM
import com.k689.identid.ui.component.utils.SPACING_SMALL
import com.k689.identid.ui.component.utils.screenWidthInDp
import com.k689.identid.ui.component.wrap.WrapImage
import com.k689.identid.ui.proximity.qr.component.rememberQrBitmapPainter
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach

@Composable
internal fun AuthenticateScreen(
    navController: NavController,
    viewModel: AuthenticateViewModel,
) {
    val state by viewModel.viewState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.setEvent(Event.Init)
    }

    DisposableEffect(context) {
        val activity = context as? ComponentActivity
        activity?.let {
            viewModel.setEvent(Event.NfcEngagement(it, true))
        }
        onDispose {
            activity?.let {
                viewModel.setEvent(Event.NfcEngagement(it, false))
            }
        }
    }

    ContentScreen(
        isLoading = state.isLoading,
        contentErrorConfig = state.error,
        navigatableAction = ScreenNavigateAction.CANCELABLE,
        onBack = { viewModel.setEvent(Event.GoBack) },
        toolBarConfig =
            ToolbarConfig(
                title = stringResource(R.string.home_screen_authenticate),
            ),
    ) { paddingValues ->
        Content(
            state = state,
            paddingValues = paddingValues,
            onEventSend = { viewModel.setEvent(it) },
        )
    }

    LaunchedEffect(Unit) {
        viewModel.effect
            .onEach { effect ->
                when (effect) {
                    is Effect.Navigation.SwitchScreen -> {
                        navController.navigate(effect.screenRoute)
                    }

                    is Effect.Navigation.Pop -> {
                        navController.popBackStack()
                    }
                }
            }.collect()
    }
}

@Composable
private fun Content(
    state: State,
    onEventSend: (Event) -> Unit,
    paddingValues: PaddingValues,
) {
    val qrSize = screenWidthInDp(true) / 1.4f

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(paddingValues),
    ) {
        Text(
            text = stringResource(id = R.string.proximity_qr_title),
            color = MaterialTheme.colorScheme.onSurface,
            style =
                MaterialTheme.typography.headlineSmall.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    // fontSize = 22.sp,
                ),
        )

        Box(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            QRCode(
                qrCode = state.qrCode,
                qrSize = qrSize,
            )
        }

        SimplifiedNFCFooter(paddingValues)

        Text(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = SPACING_SMALL.dp),
            text = stringResource(id = R.string.presentation_qr_scan_option),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )

        LargeActionFooter(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = SPACING_MEDIUM.dp),
            text = stringResource(R.string.issuance_add_document_scan_qr_footer_button_text),
            onClick = { onEventSend(Event.OpenScanQr) },
        )
    }
}

@Composable
private fun SimplifiedNFCFooter(paddingValues: PaddingValues) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(bottom = SPACING_EXTRA_SMALL.dp),
        verticalArrangement = Arrangement.spacedBy(SPACING_SMALL.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        WrapImage(
            iconData = AppIcons.NFC,
            modifier = Modifier.size(48.dp),
        )
        Text(
            text = stringResource(id = R.string.proximity_qr_hold_near_reader),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun QRCode(
    modifier: Modifier = Modifier,
    qrCode: String,
    qrSize: Dp,
) {
    if (qrCode.isNotEmpty()) {
        WrapImage(
            modifier = modifier,
            painter =
                rememberQrBitmapPainter(
                    content = qrCode,
                    size = qrSize,
                ),
            contentDescription = stringResource(id = R.string.content_description_qr_code_icon),
        )
    }
}

@ThemeModePreviews
@Composable
private fun AuthenticateScreenPreview() {
    PreviewTheme {
        ContentScreen(
            isLoading = false,
            navigatableAction = ScreenNavigateAction.CANCELABLE,
            onBack = {},
            toolBarConfig =
                ToolbarConfig(
                    title = stringResource(R.string.home_screen_authenticate),
                ),
        ) { paddingValues ->
            Content(
                state = State(isLoading = false, qrCode = "dummy_qr_code"),
                paddingValues = paddingValues,
                onEventSend = {},
            )
        }
    }
}
