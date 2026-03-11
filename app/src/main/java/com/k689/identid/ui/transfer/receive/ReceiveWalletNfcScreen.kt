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

import android.app.Activity
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.k689.identid.R
import com.k689.identid.extension.ui.paddingFrom
import com.k689.identid.service.NfcServiceModeController
import com.k689.identid.ui.component.AppIcons
import com.k689.identid.ui.component.content.ContentScreen
import com.k689.identid.ui.component.content.ContentTitle
import com.k689.identid.ui.component.content.ScreenNavigateAction
import com.k689.identid.ui.component.utils.LifecycleEffect
import com.k689.identid.ui.component.utils.SPACING_MEDIUM
import com.k689.identid.ui.component.wrap.WrapImage
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach

@Composable
fun ReceiveWalletNfcScreen(
    navController: NavController,
    viewModel: ReceiveWalletViewModel,
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    val state: ReceiveWalletState by viewModel.viewState.collectAsStateWithLifecycle()

    ContentScreen(
        isLoading = false,
        navigatableAction = ScreenNavigateAction.BACKABLE,
        onBack = { viewModel.setEvent(ReceiveWalletEvent.GoBack) },
        contentErrorConfig = state.error,
    ) { paddingValues ->
        Content(
            paddingValues = paddingValues,
            isConnected = state.isConnected,
        )
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

    LifecycleEffect(
        lifecycleOwner = LocalLifecycleOwner.current,
        lifecycleEvent = Lifecycle.Event.ON_RESUME,
    ) {
        activity?.let {
            NfcServiceModeController.activateTransferMode(it)

            val adapter = NfcAdapter.getDefaultAdapter(it)
            if (adapter == null) return@let

            adapter.enableReaderMode(
                it,
                { tag ->
                    val encodedSession = readSessionFromTag(tag)
                    if (encodedSession != null) {
                        mainHandler.post {
                            viewModel.setEvent(ReceiveWalletEvent.SessionScanned(context, encodedSession))
                        }
                    }
                },
                NfcAdapter.FLAG_READER_NFC_A or
                    NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
                null,
            )
        }
    }

    LifecycleEffect(
        lifecycleOwner = LocalLifecycleOwner.current,
        lifecycleEvent = Lifecycle.Event.ON_PAUSE,
    ) {
        activity?.let {
            NfcAdapter.getDefaultAdapter(it)?.disableReaderMode(it)
            NfcServiceModeController.activateCredentialSharingMode(it)
        }
    }
}

private fun readSessionFromTag(tag: Tag): String? {
    val isoDep = IsoDep.get(tag) ?: return null
    return runCatching {
        isoDep.connect()

        val selectAid = hexToBytes("00A4040007F0010203040506")
        val selectResponse = isoDep.transceive(selectAid)
        if (!isSwOk(selectResponse)) {
            return null
        }

        val getData = hexToBytes("00CA000000")
        val dataResponse = isoDep.transceive(getData)
        if (!isSwOk(dataResponse) || dataResponse.size < 2) {
            return null
        }

        val payload = dataResponse.copyOfRange(0, dataResponse.size - 2)
        payload.toString(Charsets.UTF_8)
    }.getOrNull().also {
        runCatching { isoDep.close() }
    }
}

private fun isSwOk(response: ByteArray): Boolean {
    if (response.size < 2) return false
    val sw1 = response[response.size - 2].toInt() and 0xFF
    val sw2 = response[response.size - 1].toInt() and 0xFF
    return sw1 == 0x90 && sw2 == 0x00
}

private fun hexToBytes(hex: String): ByteArray {
    val out = ByteArray(hex.length / 2)
    var i = 0
    while (i < hex.length) {
        out[i / 2] = ((hex[i].digitToInt(16) shl 4) + hex[i + 1].digitToInt(16)).toByte()
        i += 2
    }
    return out
}

@Composable
private fun Content(
    paddingValues: PaddingValues,
    isConnected: Boolean,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .paddingFrom(paddingValues),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SPACING_MEDIUM.dp),
    ) {
        ContentTitle(
            modifier = Modifier.fillMaxWidth(),
            title = stringResource(id = R.string.transfer_receive_nfc_title),
            subtitle = stringResource(id = R.string.transfer_receive_nfc_subtitle),
        )

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            WrapImage(iconData = AppIcons.NFC)

            Text(
                text = stringResource(id = R.string.transfer_receive_nfc_hold_devices),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text =
                        if (isConnected) {
                            stringResource(id = R.string.transfer_receive_nfc_hold_devices)
                        } else {
                            stringResource(id = R.string.transfer_receive_connecting_status)
                        },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
