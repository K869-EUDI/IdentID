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

package com.k689.identid.ui.pseudonym.detail

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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
fun PseudonymDetailScreen(
    navController: NavController,
    viewModel: PseudonymDetailViewModel,
) {
    val state: State by viewModel.viewState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val aliasSavedMessage = stringResource(R.string.pseudonym_detail_alias_saved)

    ContentScreen(
        navigatableAction = ScreenNavigateAction.BACKABLE,
        isLoading = state.isLoading,
        onBack = { viewModel.setEvent(Event.Pop) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Content(
            state = state,
            onEventSend = { viewModel.setEvent(it) },
            paddingValues = paddingValues,
        )
    }

    if (state.showDeleteConfirmation) {
        WrapConfirmationDialog(
            title = stringResource(R.string.pseudonym_detail_delete_title),
            message = stringResource(R.string.pseudonym_detail_delete_message),
            primaryButtonText = stringResource(R.string.pseudonym_detail_delete_confirm),
            onPrimaryClick = { viewModel.setEvent(Event.ConfirmDelete) },
            secondaryButtonText = stringResource(R.string.pseudonym_detail_delete_cancel),
            onSecondaryClick = { viewModel.setEvent(Event.DismissDelete) },
            isPrimaryWarning = true,
            onDismissRequest = { viewModel.setEvent(Event.DismissDelete) },
        )
    }

    LaunchedEffect(Unit) {
        viewModel.effect
            .onEach { effect ->
                when (effect) {
                    is Effect.Navigation.Pop -> navController.popBackStack()
                    is Effect.AliasSaved -> snackbarHostState.showSnackbar(aliasSavedMessage)
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
    val detail = state.detail ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState()),
    ) {
        ContentTitle(
            modifier = Modifier.fillMaxWidth(),
            title = stringResource(R.string.pseudonym_detail_title),
        )

        Spacer(modifier = Modifier.height(SPACING_MEDIUM.dp))

        InfoRow(label = stringResource(R.string.pseudonym_detail_relying_party), value = detail.rpName)
        InfoRow(label = stringResource(R.string.pseudonym_detail_domain), value = detail.rpId)
        InfoRow(label = stringResource(R.string.pseudonym_detail_username), value = detail.userName)
        InfoRow(label = stringResource(R.string.pseudonym_detail_credential_id), value = detail.credentialId)
        InfoRow(label = stringResource(R.string.pseudonym_detail_created), value = detail.createdAt)
        detail.lastUsedAt?.let { InfoRow(label = stringResource(R.string.pseudonym_detail_last_used), value = it) }

        Spacer(modifier = Modifier.height(SPACING_MEDIUM.dp))

        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SPACING_MEDIUM.dp),
            value = state.editAlias,
            onValueChange = { onEventSend(Event.UpdateAlias(it)) },
            label = { Text(stringResource(R.string.pseudonym_detail_alias_label)) },
            singleLine = true,
        )

        Spacer(modifier = Modifier.height(SPACING_SMALL.dp))

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SPACING_MEDIUM.dp),
            onClick = { onEventSend(Event.SaveAlias) },
        ) {
            Text(stringResource(R.string.pseudonym_detail_save_alias))
        }

        Spacer(modifier = Modifier.height(SPACING_MEDIUM.dp))

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SPACING_MEDIUM.dp),
            onClick = { onEventSend(Event.RequestDelete) },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
            ),
        ) {
            Text(stringResource(R.string.pseudonym_detail_delete))
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
