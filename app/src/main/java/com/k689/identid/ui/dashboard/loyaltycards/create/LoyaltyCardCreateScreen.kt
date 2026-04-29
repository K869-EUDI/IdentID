package com.k689.identid.ui.dashboard.loyaltycards.create

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.k689.identid.ui.component.content.ContentScreen
import com.k689.identid.ui.component.content.ContentTitle
import com.k689.identid.ui.component.content.ScreenNavigateAction
import com.k689.identid.ui.component.wrap.ButtonConfig
import com.k689.identid.ui.component.wrap.ButtonType
import com.k689.identid.ui.component.wrap.WrapButton
import com.k689.identid.ui.component.wrap.WrapTextField
import com.k689.identid.ui.dashboard.loyaltycards.component.BarcodeVisual
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
fun LoyaltyCardCreateScreen(
    navController: NavController,
    viewModel: LoyaltyCardCreateViewModel,
) {
    val state: State by viewModel.viewState.collectAsStateWithLifecycle()

    ContentScreen(
        navigatableAction = ScreenNavigateAction.BACKABLE,
        isLoading = state.isSaving,
        onBack = { viewModel.setEvent(Event.Pop) },
    ) { paddingValues ->
        Content(state = state, paddingValues = paddingValues, onEventSend = viewModel::setEvent)
    }

    LaunchedEffect(Unit) {
        viewModel.effect.onEach { effect ->
            when (effect) {
                is Effect.Navigation.Pop -> navController.popBackStack()
                is Effect.Navigation.ToDetail -> {
                    navController.navigate(
                        generateComposableNavigationLink(
                            screen = DashboardScreens.LoyaltyCardDetails,
                            arguments = generateComposableArguments(mapOf("loyaltyCardId" to effect.id)),
                        ),
                    ) {
                        popUpTo(DashboardScreens.LoyaltyCardCreate.screenRoute) { inclusive = true }
                    }
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
        modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
    ) {
        ContentTitle(
            modifier = Modifier.fillMaxWidth(),
            title = stringResource(R.string.loyalty_cards_name_title),
            subtitle = stringResource(R.string.loyalty_cards_name_subtitle),
        )

        BarcodeVisual(
            barcodeValue = state.barcodeValue,
            barcodeFormat = state.barcodeFormat,
            modifier = Modifier.padding(top = 16.dp),
        )

        Text(
            text = state.barcodeFormat,
            modifier = Modifier.padding(top = 12.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        WrapTextField(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            value = state.name,
            onValueChange = { onEventSend(Event.NameChanged(it)) },
            label = { Text(stringResource(R.string.loyalty_cards_name_label)) },
            singleLine = true,
        )

        WrapButton(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            buttonConfig = ButtonConfig(type = ButtonType.PRIMARY, onClick = { onEventSend(Event.Save) }, enabled = state.name.isNotBlank()),
        ) {
            Text(stringResource(R.string.loyalty_cards_name_save))
        }
    }
}