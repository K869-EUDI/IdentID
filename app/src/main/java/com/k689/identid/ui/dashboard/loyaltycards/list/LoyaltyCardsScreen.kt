package com.k689.identid.ui.dashboard.loyaltycards.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.k689.identid.R
import com.k689.identid.navigation.DashboardScreens
import com.k689.identid.navigation.helper.generateComposableArguments
import com.k689.identid.navigation.helper.generateComposableNavigationLink
import com.k689.identid.ui.component.AppIcons
import com.k689.identid.ui.component.ListItemDataUi
import com.k689.identid.ui.component.ListItemMainContentDataUi
import com.k689.identid.ui.component.ListItemTrailingContentDataUi
import com.k689.identid.ui.component.content.ContentScreen
import com.k689.identid.ui.component.content.ScreenNavigateAction
import com.k689.identid.ui.component.content.ToolbarConfig
import com.k689.identid.ui.component.utils.SPACING_MEDIUM
import com.k689.identid.ui.component.wrap.WrapIconButton
import com.k689.identid.ui.component.wrap.WrapListItem
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
fun LoyaltyCardsScreen(
    navController: NavController,
    viewModel: LoyaltyCardsViewModel,
) {
    val state: State by viewModel.viewState.collectAsStateWithLifecycle()

    ContentScreen(
        navigatableAction = ScreenNavigateAction.BACKABLE,
        isLoading = state.isLoading,
        onBack = { viewModel.setEvent(Event.Pop) },
        toolBarConfig = ToolbarConfig(title = stringResource(R.string.loyalty_cards_title)),
    ) { paddingValues ->
        Content(
            state = state,
            paddingValues = paddingValues,
            onEventSend = viewModel::setEvent,
        )
    }

    LaunchedEffect(Unit) {
        viewModel.effect.onEach { effect ->
            when (effect) {
                is Effect.Navigation.Pop -> navController.popBackStack()
                is Effect.Navigation.ToScan -> navController.navigate(DashboardScreens.LoyaltyCardScan.screenRoute)
                is Effect.Navigation.ToDetail -> {
                    navController.navigate(
                        generateComposableNavigationLink(
                            screen = DashboardScreens.LoyaltyCardDetails,
                            arguments = generateComposableArguments(mapOf("loyaltyCardId" to effect.id)),
                        ),
                    )
                }
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
    Column(
        modifier = Modifier.fillMaxSize().padding(paddingValues),
    ) {
        WrapIconButton(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            iconData = AppIcons.Add,
            customTint = MaterialTheme.colorScheme.onSurface,
        ) {
            onEventSend(Event.AddCardPressed)
        }

        if (state.cards.isEmpty()) {
            Text(
                text = stringResource(R.string.loyalty_cards_empty),
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(SPACING_MEDIUM.dp),
            ) {
                items(state.cards, key = { it.id }) { card ->
                    WrapListItem(
                        item =
                            ListItemDataUi(
                                itemId = card.id,
                                mainContentData = ListItemMainContentDataUi.Text(card.displayName),
                                supportingText = "${card.barcodeFormat} • ${card.createdAtLabel}",
                                trailingContentData = ListItemTrailingContentDataUi.Icon(AppIcons.KeyboardArrowRight),
                            ),
                        onItemClick = { onEventSend(Event.CardClicked(card.id)) },
                    )
                }
            }
        }
    }
}