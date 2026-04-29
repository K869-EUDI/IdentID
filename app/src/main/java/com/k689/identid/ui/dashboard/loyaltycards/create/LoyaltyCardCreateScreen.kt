package com.k689.identid.ui.dashboard.loyaltycards.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.k689.identid.R
import com.k689.identid.extension.ui.paddingFrom
import com.k689.identid.navigation.DashboardScreens
import com.k689.identid.navigation.helper.generateComposableArguments
import com.k689.identid.navigation.helper.generateComposableNavigationLink
import com.k689.identid.ui.component.content.ContentScreen
import com.k689.identid.ui.component.content.ContentTitle
import com.k689.identid.ui.component.content.ImePaddingConfig
import com.k689.identid.ui.component.content.ScreenNavigateAction
import com.k689.identid.ui.component.wrap.ButtonConfig
import com.k689.identid.ui.component.wrap.ButtonType
import com.k689.identid.ui.component.wrap.WrapButton
import com.k689.identid.ui.component.wrap.WrapCard
import com.k689.identid.ui.component.wrap.WrapTextField
import com.k689.identid.ui.dashboard.loyaltycards.component.BarcodeVisual
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.k689.identid.theme.values.warning
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach

@Composable
fun LoyaltyCardCreateScreen(
    navController: NavController,
    viewModel: LoyaltyCardCreateViewModel,
) {
    val state: State by viewModel.viewState.collectAsStateWithLifecycle()

    ContentScreen(
        navigatableAction = ScreenNavigateAction.BACKABLE,
        isLoading = state.isSaving,
        imePaddingConfig = ImePaddingConfig.ONLY_CONTENT,
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
        modifier =
            Modifier
                .fillMaxSize()
                .paddingFrom(paddingValues, bottom = false)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ContentTitle(
            modifier = Modifier.fillMaxWidth(),
            title = stringResource(R.string.loyalty_cards_name_title),
            subtitle = stringResource(R.string.loyalty_cards_name_subtitle),
        )

        BarcodeVisual(
            barcodeValue = state.barcodeValue,
            barcodeFormat = state.barcodeFormat,
            modifier = Modifier,
        )

        BarcodeMetadata(
            label = stringResource(R.string.loyalty_cards_name_barcode_label),
            value = state.barcodeValue,
        )

        BarcodeMetadata(
            label = stringResource(R.string.loyalty_cards_detail_format),
            value = state.barcodeFormat,
        )

        state.duplicateCardName?.let { duplicateCardName ->
            DuplicateWarningCard(
                warningText =
                    stringResource(
                        R.string.loyalty_cards_name_duplicate_warning,
                        duplicateCardName,
                    ),
            )
        }

        WrapTextField(
            modifier = Modifier.fillMaxWidth(),
            value = state.name,
            onValueChange = { onEventSend(Event.NameChanged(it)) },
            label = { Text(stringResource(R.string.loyalty_cards_name_label)) },
            singleLine = true,
            isError = state.duplicateCardName != null,
            errorMsg =
                state.duplicateCardName?.let { duplicateCardName ->
                    stringResource(
                        R.string.loyalty_cards_name_duplicate_warning,
                        duplicateCardName,
                    )
                }.orEmpty(),
        )

        WrapButton(
            modifier = Modifier.fillMaxWidth(),
            buttonConfig = ButtonConfig(type = ButtonType.PRIMARY, onClick = { onEventSend(Event.Save) }, enabled = state.name.isNotBlank() && state.duplicateCardName == null),
        ) {
            Text(stringResource(R.string.loyalty_cards_name_save))
        }
    }
}

@Composable
private fun BarcodeMetadata(
    label: String,
    value: String,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
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

@Composable
private fun DuplicateWarningCard(
    warningText: String,
) {
    WrapCard {
        Text(
            text = warningText,
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.warning,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}