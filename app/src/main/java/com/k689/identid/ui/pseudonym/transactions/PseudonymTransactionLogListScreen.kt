package com.k689.identid.ui.pseudonym.transactions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.k689.identid.R
import com.k689.identid.interactor.pseudonym.PseudonymTransactionLogUi
import com.k689.identid.navigation.DashboardScreens
import com.k689.identid.ui.component.content.ContentScreen
import com.k689.identid.ui.component.content.ContentTitle
import com.k689.identid.ui.component.content.ScreenNavigateAction
import com.k689.identid.ui.component.utils.SPACING_MEDIUM
import com.k689.identid.ui.component.utils.SPACING_SMALL
import com.k689.identid.ui.component.utils.LifecycleEffect
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach

@Composable
fun PseudonymTransactionLogListScreen(
    navController: NavController,
    viewModel: PseudonymTransactionLogListViewModel,
) {
    val state: State by viewModel.viewState.collectAsStateWithLifecycle()

    LifecycleEffect(
        lifecycleOwner = LocalLifecycleOwner.current,
        lifecycleEvent = Lifecycle.Event.ON_RESUME,
    ) {
        viewModel.setEvent(Event.Init)
    }

    ContentScreen(
        navigatableAction = ScreenNavigateAction.BACKABLE,
        isLoading = state.isLoading,
        onBack = { viewModel.setEvent(Event.Pop) },
    ) { paddingValues ->
        Content(
            state = state,
            onEventSend = { viewModel.setEvent(it) },
            paddingValues = paddingValues,
        )
    }

    if (state.showDeleteAllConfirmation) {
        AlertDialog(
            onDismissRequest = { viewModel.setEvent(Event.DismissDeleteAll) },
            title = { Text(stringResource(R.string.pseudonym_txlog_delete_all_title)) },
            text = { Text(stringResource(R.string.pseudonym_txlog_delete_all_message)) },
            confirmButton = {
                TextButton(onClick = { viewModel.setEvent(Event.ConfirmDeleteAll) }) {
                    Text(
                        stringResource(R.string.pseudonym_detail_delete_confirm),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setEvent(Event.DismissDeleteAll) }) {
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
                    is Effect.Navigation.ToDetail -> {
                        navController.navigate(
                            DashboardScreens.PseudonymTransactionLogDetail.screenRoute
                                .replace("{logId}", effect.logId),
                        )
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ContentTitle(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.pseudonym_txlog_title),
            )
            if (state.logs.isNotEmpty()) {
                IconButton(
                    onClick = { onEventSend(Event.RequestDeleteAll) },
                    modifier = Modifier.padding(end = SPACING_MEDIUM.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.pseudonym_txlog_delete_all_title),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        if (state.logs.isEmpty() && !state.isLoading) {
            Spacer(modifier = Modifier.height(SPACING_MEDIUM.dp))
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SPACING_MEDIUM.dp),
                text = stringResource(R.string.pseudonym_txlog_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
            ) {
                items(state.logs) { log ->
                    TransactionLogItem(
                        item = log,
                        onClick = { onEventSend(Event.NavigateToDetail(log.id)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionLogItem(
    item: PseudonymTransactionLogUi,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SPACING_MEDIUM.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isCompleted) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.errorContainer
            },
        ),
    ) {
        Column(
            modifier = Modifier.padding(SPACING_MEDIUM.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (item.isRegistration) {
                        stringResource(R.string.pseudonym_txlog_type_registration)
                    } else {
                        stringResource(R.string.pseudonym_txlog_type_authentication)
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = if (item.isCompleted) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                )
                Text(
                    text = if (item.isCompleted) {
                        stringResource(R.string.pseudonym_txlog_status_completed)
                    } else {
                        stringResource(R.string.pseudonym_txlog_status_failed)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (item.isCompleted) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                )
            }
            Text(
                text = item.rpName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = item.timestamp,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!item.isCompleted && item.failureReason != null) {
                Text(
                    text = item.failureReason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 1,
                )
            }
        }
    }
}
