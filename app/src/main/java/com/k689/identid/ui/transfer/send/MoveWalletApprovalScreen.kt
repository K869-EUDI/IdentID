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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.k689.identid.R
import com.k689.identid.extension.ui.paddingFrom
import com.k689.identid.navigation.DashboardScreens
import com.k689.identid.ui.component.content.ContentScreen
import com.k689.identid.ui.component.content.ContentTitle
import com.k689.identid.ui.component.content.ScreenNavigateAction
import com.k689.identid.ui.component.utils.OneTimeLaunchedEffect
import com.k689.identid.ui.component.utils.SPACING_MEDIUM
import com.k689.identid.ui.component.wrap.ButtonConfig
import com.k689.identid.ui.component.wrap.ButtonType
import com.k689.identid.ui.component.wrap.WrapButton
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach

@Composable
fun MoveWalletApprovalScreen(
    navController: NavController,
    viewModel: MoveWalletApprovalViewModel,
) {
    val state: MoveWalletApprovalState by viewModel.viewState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    ContentScreen(
        isLoading = state.isLoading || state.isSending,
        navigatableAction = ScreenNavigateAction.BACKABLE,
        onBack = { viewModel.setEvent(MoveWalletApprovalEvent.GoBack(context)) },
        contentErrorConfig = state.error,
    ) { paddingValues ->
        Content(
            state = state,
            onPinChanged = { viewModel.setEvent(MoveWalletApprovalEvent.PinChanged(it)) },
            onConfirm = { viewModel.setEvent(MoveWalletApprovalEvent.ConfirmTransfer(context)) },
            paddingValues = paddingValues,
        )
    }

    OneTimeLaunchedEffect {
        viewModel.setEvent(MoveWalletApprovalEvent.Init(context))
    }

    LaunchedEffect(Unit) {
        viewModel.effect
            .onEach { effect ->
                when (effect) {
                    is MoveWalletApprovalEffect.Navigation.Pop -> {
                        navController.popBackStack()
                    }

                    is MoveWalletApprovalEffect.Navigation.NavigateToDashboard -> {
                        navController.navigate(DashboardScreens.Dashboard.screenRoute) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            }.collect()
    }
}

@Composable
private fun Content(
    state: MoveWalletApprovalState,
    onPinChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    paddingValues: PaddingValues,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .paddingFrom(paddingValues),
    ) {
        ContentTitle(
            modifier = Modifier.fillMaxWidth(),
            title = stringResource(id = R.string.transfer_approval_title),
            subtitle = stringResource(id = R.string.transfer_approval_subtitle),
        )

        Spacer(modifier = Modifier.height(SPACING_MEDIUM.dp))

        LazyColumn(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.documents) { doc ->
                DocumentItem(doc.name, doc.isPid)
            }
        }

        Spacer(modifier = Modifier.height(SPACING_MEDIUM.dp))

        OutlinedTextField(
            value = state.pinInput,
            onValueChange = onPinChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(id = R.string.transfer_approval_pin_label)) },
            isError = state.pinError,
            supportingText =
                if (state.pinError) {
                    { Text(stringResource(id = R.string.transfer_approval_pin_error)) }
                } else {
                    null
                },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
        )

        Spacer(modifier = Modifier.height(SPACING_MEDIUM.dp))

        WrapButton(
            modifier = Modifier.fillMaxWidth(),
            buttonConfig =
                ButtonConfig(
                    type = ButtonType.PRIMARY,
                    enabled = state.pinInput.isNotEmpty() && !state.isSending,
                    onClick = onConfirm,
                ),
        ) {
            Text(stringResource(id = R.string.transfer_approval_confirm_button))
        }
    }
}

@Composable
private fun DocumentItem(
    name: String,
    isPid: Boolean,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = SPACING_MEDIUM.dp, vertical = 4.dp),
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (isPid) {
            Text(
                text = stringResource(id = R.string.transfer_approval_pid_label),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
