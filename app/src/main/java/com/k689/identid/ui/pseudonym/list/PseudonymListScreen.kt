package com.k689.identid.ui.pseudonym.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.k689.identid.interactor.pseudonym.PseudonymItemUi
import com.k689.identid.navigation.DashboardScreens
import com.k689.identid.ui.component.content.ContentScreen
import com.k689.identid.ui.component.content.ContentTitle
import com.k689.identid.ui.component.content.ScreenNavigateAction
import com.k689.identid.ui.component.utils.LifecycleEffect
import com.k689.identid.ui.component.utils.SPACING_MEDIUM
import com.k689.identid.ui.component.utils.SPACING_SMALL
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach

@Composable
fun PseudonymListScreen(
    navController: NavController,
    viewModel: PseudonymListViewModel,
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

    LaunchedEffect(Unit) {
        viewModel.effect
            .onEach { effect ->
                when (effect) {
                    is Effect.Navigation.Pop -> {
                        navController.popBackStack()
                    }

                    is Effect.Navigation.ToDetail -> {
                        navController.navigate(
                            DashboardScreens.PseudonymDetail.screenRoute
                                .replace("{pseudonymId}", effect.pseudonymId),
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
        modifier =
            Modifier
                .fillMaxSize()
                .padding(paddingValues),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ContentTitle(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.pseudonym_list_title),
            )
        }

        if (state.groups.isEmpty() && !state.isLoading) {
            Spacer(modifier = Modifier.height(SPACING_MEDIUM.dp))
            Text(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = SPACING_MEDIUM.dp),
                text = stringResource(R.string.pseudonym_list_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
            ) {
                state.groups.forEach { group ->
                    item {
                        Text(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        horizontal = SPACING_MEDIUM.dp,
                                        vertical = SPACING_SMALL.dp,
                                    ),
                            text = group.rpName,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    items(group.pseudonyms) { pseudonym ->
                        PseudonymItem(
                            item = pseudonym,
                            onClick = { onEventSend(Event.NavigateToDetail(pseudonym.id)) },
                        )
                    }
                    item {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = SPACING_SMALL.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PseudonymItem(
    item: PseudonymItemUi,
    onClick: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = SPACING_MEDIUM.dp, vertical = 4.dp)
                .clickable(onClick = onClick),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
    ) {
        Column(
            modifier = Modifier.padding(SPACING_MEDIUM.dp),
        ) {
            Text(
                text = item.displayName,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = item.credentialIdTruncated,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.pseudonym_list_created, item.createdAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            item.lastUsedAt?.let {
                Text(
                    text = stringResource(R.string.pseudonym_list_last_used, it),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
