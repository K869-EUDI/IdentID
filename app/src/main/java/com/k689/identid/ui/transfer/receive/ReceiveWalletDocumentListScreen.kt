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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.k689.identid.R
import com.k689.identid.extension.ui.paddingFrom
import com.k689.identid.interactor.transfer.DocumentImportResult
import com.k689.identid.model.transfer.TransferableDocument
import com.k689.identid.ui.component.content.BroadcastAction
import com.k689.identid.ui.component.content.ContentScreen
import com.k689.identid.ui.component.content.ContentTitle
import com.k689.identid.ui.component.content.ScreenNavigateAction
import com.k689.identid.ui.component.utils.SPACING_MEDIUM
import com.k689.identid.ui.component.wrap.ButtonConfig
import com.k689.identid.ui.component.wrap.ButtonType
import com.k689.identid.ui.component.wrap.WrapButton
import com.k689.identid.util.core.CoreActions
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach

@Composable
fun ReceiveWalletDocumentListScreen(
    navController: NavController,
    viewModel: ReceiveWalletViewModel,
) {
    val state: ReceiveWalletState by viewModel.viewState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    ContentScreen(
        isLoading = state.isImporting,
        navigatableAction = ScreenNavigateAction.BACKABLE,
        onBack = { viewModel.setEvent(ReceiveWalletEvent.GoBack) },
        contentErrorConfig = state.error,
        broadcastAction = BroadcastAction(
            intentFilters = listOf(CoreActions.VCI_RESUME_ACTION),
            callback = { intent ->
                intent?.extras?.getString("uri")?.let { uri ->
                    viewModel.setEvent(ReceiveWalletEvent.OnResumeIssuance(uri))
                }
            },
        ),
    ) { paddingValues ->
        if (state.isImportComplete) {
            ImportResultsContent(
                importResults = state.importResults,
                onDone = { viewModel.setEvent(ReceiveWalletEvent.NavigateToDashboard) },
                paddingValues = paddingValues,
            )
        } else {
            DocumentListContent(
                documents = state.receivedData?.documents.orEmpty(),
                onImport = { viewModel.setEvent(ReceiveWalletEvent.ImportDocuments(context)) },
                paddingValues = paddingValues,
            )
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
private fun DocumentListContent(
    documents: List<TransferableDocument>,
    onImport: () -> Unit,
    paddingValues: PaddingValues,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .paddingFrom(paddingValues),
    ) {
        ContentTitle(
            modifier = Modifier.fillMaxWidth(),
            title = stringResource(id = R.string.transfer_document_list_title),
            subtitle = stringResource(id = R.string.transfer_document_list_subtitle),
        )

        Spacer(modifier = Modifier.height(SPACING_MEDIUM.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(documents) { doc ->
                DocumentItem(doc)
            }
        }

        Spacer(modifier = Modifier.height(SPACING_MEDIUM.dp))

        WrapButton(
            modifier = Modifier.fillMaxWidth(),
            buttonConfig = ButtonConfig(
                type = ButtonType.PRIMARY,
                enabled = documents.isNotEmpty(),
                onClick = onImport,
            ),
        ) {
            Text(stringResource(id = R.string.transfer_document_list_import_button))
        }
    }
}

@Composable
private fun ImportResultsContent(
    importResults: List<DocumentImportResult>,
    onDone: () -> Unit,
    paddingValues: PaddingValues,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .paddingFrom(paddingValues),
    ) {
        ContentTitle(
            modifier = Modifier.fillMaxWidth(),
            title = stringResource(id = R.string.transfer_import_complete_title),
            subtitle = stringResource(id = R.string.transfer_import_complete_subtitle),
        )

        Spacer(modifier = Modifier.height(SPACING_MEDIUM.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(importResults) { result ->
                ImportResultItem(result)
            }
        }

        Spacer(modifier = Modifier.height(SPACING_MEDIUM.dp))

        WrapButton(
            modifier = Modifier.fillMaxWidth(),
            buttonConfig = ButtonConfig(
                type = ButtonType.PRIMARY,
                onClick = onDone,
            ),
        ) {
            Text(stringResource(id = R.string.transfer_import_done_button))
        }
    }
}

@Composable
private fun DocumentItem(doc: TransferableDocument) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SPACING_MEDIUM.dp, vertical = 4.dp),
    ) {
        Text(
            text = doc.name,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (doc.isPid) {
            Text(
                text = stringResource(id = R.string.transfer_approval_pid_label),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun ImportResultItem(result: DocumentImportResult) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SPACING_MEDIUM.dp, vertical = 4.dp),
    ) {
        when (result) {
            is DocumentImportResult.Success -> {
                Text(
                    text = result.documentName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(id = R.string.transfer_import_status_success),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            is DocumentImportResult.Failed -> {
                Text(
                    text = result.documentName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = result.error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
