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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
    ) { paddingValues ->
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background,
        ) { scaffoldPadding ->
            Content(
                state = state,
                onEventSend = { viewModel.setEvent(it) },
                paddingValues = paddingValues,
            )
        }
    }

    if (state.showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { viewModel.setEvent(Event.DismissDelete) },
            title = { Text(stringResource(R.string.pseudonym_detail_delete_title)) },
            text = { Text(stringResource(R.string.pseudonym_detail_delete_message)) },
            confirmButton = {
                TextButton(onClick = { viewModel.setEvent(Event.ConfirmDelete) }) {
                    Text(stringResource(R.string.pseudonym_detail_delete_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setEvent(Event.DismissDelete) }) {
                    Text(stringResource(R.string.pseudonym_detail_delete_cancel))
                }
            },
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
