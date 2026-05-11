/*
 * Copyright (c) 2026 European Commission
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

package com.k689.identid.ui.pseudonym.transactions

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.k689.identid.R
import com.k689.identid.ui.component.content.ContentScreen
import com.k689.identid.ui.component.content.ContentTitle
import com.k689.identid.ui.component.content.ScreenNavigateAction
import com.k689.identid.ui.component.utils.SPACING_MEDIUM
import com.k689.identid.ui.component.utils.SPACING_SMALL
import com.k689.identid.ui.component.wrap.WrapConfirmationDialog
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach

@Composable
fun PseudonymTransactionLogDetailScreen(
    navController: NavController,
    viewModel: PseudonymTransactionLogDetailViewModel,
) {
    val state: DetailState by viewModel.viewState.collectAsStateWithLifecycle()

    ContentScreen(
        navigatableAction = ScreenNavigateAction.BACKABLE,
        isLoading = state.isLoading,
        onBack = { viewModel.setEvent(DetailEvent.Pop) },
    ) { paddingValues ->
        Content(
            state = state,
            onEventSend = { viewModel.setEvent(it) },
            paddingValues = paddingValues,
        )
    }

    if (state.showDeleteConfirmation) {
        WrapConfirmationDialog(
            title = stringResource(R.string.pseudonym_txlog_detail_delete_title),
            message = stringResource(R.string.pseudonym_txlog_detail_delete_message),
            primaryButtonText = stringResource(R.string.pseudonym_detail_delete_confirm),
            onPrimaryClick = { viewModel.setEvent(DetailEvent.ConfirmDelete) },
            secondaryButtonText = stringResource(R.string.pseudonym_detail_delete_cancel),
            onSecondaryClick = { viewModel.setEvent(DetailEvent.DismissDelete) },
            isPrimaryWarning = true,
            onDismissRequest = { viewModel.setEvent(DetailEvent.DismissDelete) },
        )
    }

    LaunchedEffect(Unit) {
        viewModel.effect
            .onEach { effect ->
                when (effect) {
                    is DetailEffect.Navigation.Pop -> navController.popBackStack()
                }
            }.collect()
    }
}

@Composable
private fun Content(
    state: DetailState,
    onEventSend: (DetailEvent) -> Unit,
    paddingValues: PaddingValues,
) {
    val log = state.log ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState()),
    ) {
        ContentTitle(
            modifier = Modifier.fillMaxWidth(),
            title = stringResource(R.string.pseudonym_txlog_detail_title),
        )

        Spacer(modifier = Modifier.height(SPACING_MEDIUM.dp))

        InfoRow(
            label = stringResource(R.string.pseudonym_txlog_detail_type),
            value = if (log.isRegistration) {
                stringResource(R.string.pseudonym_txlog_type_registration)
            } else {
                stringResource(R.string.pseudonym_txlog_type_authentication)
            },
        )
        InfoRow(
            label = stringResource(R.string.pseudonym_txlog_detail_status),
            value = if (log.isCompleted) {
                stringResource(R.string.pseudonym_txlog_status_completed)
            } else {
                stringResource(R.string.pseudonym_txlog_status_failed)
            },
        )
        InfoRow(
            label = stringResource(R.string.pseudonym_txlog_detail_timestamp),
            value = log.timestamp,
        )
        InfoRow(
            label = stringResource(R.string.pseudonym_txlog_detail_rp_name),
            value = log.rpName,
        )
        InfoRow(
            label = stringResource(R.string.pseudonym_txlog_detail_rp_id),
            value = log.rpId,
        )
        log.userName?.let {
            InfoRow(
                label = stringResource(R.string.pseudonym_txlog_detail_username),
                value = it,
            )
        }
        log.credentialId?.let {
            InfoRow(
                label = stringResource(R.string.pseudonym_txlog_detail_credential_id),
                value = it,
            )
        }
        if (!log.isCompleted && log.failureReason != null) {
            InfoRow(
                label = stringResource(R.string.pseudonym_txlog_detail_failure_reason),
                value = log.failureReason,
            )
        }

        Spacer(modifier = Modifier.height(SPACING_MEDIUM.dp))

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SPACING_MEDIUM.dp),
            onClick = { onEventSend(DetailEvent.RequestDelete) },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
            ),
        ) {
            Text(stringResource(R.string.pseudonym_txlog_detail_delete))
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SPACING_MEDIUM.dp, vertical = SPACING_SMALL.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
