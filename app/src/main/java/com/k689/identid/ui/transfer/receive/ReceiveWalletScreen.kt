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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.k689.identid.R
import com.k689.identid.extension.ui.paddingFrom
import com.k689.identid.ui.component.content.ContentScreen
import com.k689.identid.ui.component.content.ContentTitle
import com.k689.identid.ui.component.content.ScreenNavigateAction
import com.k689.identid.ui.component.utils.SPACING_MEDIUM
import com.k689.identid.ui.component.wrap.ButtonConfig
import com.k689.identid.ui.component.wrap.ButtonType
import com.k689.identid.ui.component.wrap.WrapButton
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach

@Composable
fun ReceiveWalletScreen(
    navController: NavController,
    viewModel: ReceiveWalletViewModel,
) {
    val state: ReceiveWalletState by viewModel.viewState.collectAsStateWithLifecycle()

    ContentScreen(
        isLoading = state.isLoading,
        navigatableAction = ScreenNavigateAction.BACKABLE,
        onBack = { viewModel.setEvent(ReceiveWalletEvent.GoBack) },
        contentErrorConfig = state.error,
    ) { paddingValues ->
        Content(
            onChooseQr = { viewModel.setEvent(ReceiveWalletEvent.ChooseQr) },
            onChooseNfc = { viewModel.setEvent(ReceiveWalletEvent.ChooseNfc) },
            paddingValues = paddingValues,
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
}

@Composable
private fun Content(
    onChooseQr: () -> Unit,
    onChooseNfc: () -> Unit,
    paddingValues: PaddingValues,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .paddingFrom(paddingValues),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SPACING_MEDIUM.dp),
    ) {
        ContentTitle(
            modifier = Modifier.fillMaxWidth(),
            title = stringResource(id = R.string.transfer_receive_title),
            subtitle = stringResource(id = R.string.transfer_receive_subtitle),
        )

        Spacer(modifier = Modifier.weight(1f))

        WrapButton(
            modifier = Modifier.fillMaxWidth(),
            buttonConfig = ButtonConfig(
                type = ButtonType.PRIMARY,
                onClick = onChooseQr,
            ),
        ) {
            Text(stringResource(id = R.string.transfer_receive_scan_qr))
        }

        Spacer(modifier = Modifier.height(SPACING_MEDIUM.dp))

        WrapButton(
            modifier = Modifier.fillMaxWidth(),
            buttonConfig = ButtonConfig(
                type = ButtonType.SECONDARY,
                onClick = onChooseNfc,
            ),
        ) {
            Text(stringResource(id = R.string.transfer_receive_use_nfc))
        }

        Spacer(modifier = Modifier.height(SPACING_MEDIUM.dp))
    }
}
