package com.k689.identid.ui.dashboard.loyaltycards.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.k689.identid.R
import com.k689.identid.ui.component.content.ContentErrorConfig
import com.k689.identid.ui.component.content.ContentScreen
import com.k689.identid.ui.component.content.ContentTitle
import com.k689.identid.ui.component.content.ScreenNavigateAction
import com.k689.identid.ui.component.wrap.ButtonConfig
import com.k689.identid.ui.component.wrap.ButtonType
import com.k689.identid.ui.component.wrap.WrapButton
import com.k689.identid.ui.dashboard.loyaltycards.component.BarcodeVisual
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach

@Composable
fun LoyaltyCardDetailScreen(
    navController: NavController,
    viewModel: LoyaltyCardDetailViewModel,
) {
    val state: State by viewModel.viewState.collectAsStateWithLifecycle()

    ContentScreen(
        navigatableAction = ScreenNavigateAction.BACKABLE,
        isLoading = state.isLoading,
        contentErrorConfig =
            state.error?.let {
                ContentErrorConfig(
                    errorTitle = stringResource(R.string.generic_error_message),
                    errorSubTitle = stringResource(R.string.generic_error_description),
                    onCancel = it.onCancel,
                )
            },
        onBack = { viewModel.setEvent(Event.Pop) },
    ) { paddingValues ->
        Content(state = state, paddingValues = paddingValues, onEventSend = viewModel::setEvent)
    }

    if (state.showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { viewModel.setEvent(Event.DismissDelete) },
            title = { Text(stringResource(R.string.loyalty_cards_delete_title)) },
            text = { Text(stringResource(R.string.loyalty_cards_delete_message)) },
            confirmButton = {
                TextButton(onClick = { viewModel.setEvent(Event.ConfirmDelete) }) {
                    Text(stringResource(R.string.generic_bottom_sheet_delete_primary_button_text), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setEvent(Event.DismissDelete) }) {
                    Text(stringResource(R.string.generic_bottom_sheet_delete_secondary_button_text))
                }
            },
        )
    }

    LaunchedEffect(Unit) {
        viewModel.effect.onEach { effect ->
            when (effect) {
                is Effect.Navigation.Pop -> navController.popBackStack()
            }
        }.collect()
    }
}

@Composable
private fun Content(
    state: State,
    paddingValues: PaddingValues,
    onEventSend: (Event) -> Unit,
) {
    val detail = state.detail ?: return

    Column(
        modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
    ) {
        ContentTitle(
            modifier = Modifier.fillMaxWidth(),
            title = detail.displayName,
            subtitle = stringResource(R.string.loyalty_cards_detail_title),
        )

        BarcodeVisual(
            barcodeValue = detail.barcodeValue,
            barcodeFormat = detail.barcodeFormat,
            modifier = Modifier.padding(top = 16.dp),
        )

        DetailRow(stringResource(R.string.loyalty_cards_detail_code), detail.barcodeValue)
        DetailRow(stringResource(R.string.loyalty_cards_detail_format), detail.barcodeFormat)
        DetailRow(stringResource(R.string.loyalty_cards_detail_created), detail.createdAtLabel)

        WrapButton(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            buttonConfig = ButtonConfig(type = ButtonType.SECONDARY, onClick = { onEventSend(Event.ToggleBookmark) }),
        ) {
            Text(
                if (detail.isBookmarked) {
                    stringResource(R.string.loyalty_cards_detail_remove_bookmark)
                } else {
                    stringResource(R.string.loyalty_cards_detail_bookmark)
                },
            )
        }

        WrapButton(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            buttonConfig = ButtonConfig(type = ButtonType.SECONDARY, onClick = { onEventSend(Event.RequestDelete) }, isWarning = true),
        ) {
            Text(stringResource(R.string.loyalty_cards_delete))
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}