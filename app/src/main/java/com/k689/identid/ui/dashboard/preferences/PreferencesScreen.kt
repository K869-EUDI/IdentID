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

package com.k689.identid.ui.dashboard.preferences

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.k689.identid.R
import com.k689.identid.model.common.PinFlow
import com.k689.identid.navigation.CommonScreens
import com.k689.identid.navigation.DashboardScreens
import com.k689.identid.navigation.TransferScreens
import com.k689.identid.navigation.helper.generateComposableArguments
import com.k689.identid.navigation.helper.generateComposableNavigationLink
import com.k689.identid.theme.AppLanguage
import com.k689.identid.theme.AppTheme
import com.k689.identid.ui.component.AppIcons
import com.k689.identid.ui.component.SectionTitle
import com.k689.identid.ui.component.content.ContentScreen
import com.k689.identid.ui.component.content.ScreenNavigateAction
import com.k689.identid.ui.component.content.ToolbarConfig
import com.k689.identid.ui.component.preview.PreviewTheme
import com.k689.identid.ui.component.preview.ThemeModePreviews
import com.k689.identid.ui.component.utils.SPACING_LARGE
import com.k689.identid.ui.component.utils.SPACING_MEDIUM
import com.k689.identid.ui.component.utils.SPACING_SMALL
import com.k689.identid.ui.component.wrap.WrapIcon
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach

@Composable
fun PreferencesScreen(
    navController: NavController,
    viewModel: PreferencesViewModel,
) {
    val state by viewModel.viewState.collectAsStateWithLifecycle()

    ContentScreen(
        navigatableAction = ScreenNavigateAction.CANCELABLE,
        onBack = { viewModel.setEvent(Event.Pop) },
        toolBarConfig = ToolbarConfig(title = state.screenTitle),
    ) { paddingValues ->
        PreferencesContent(
            state = state,
            onEvent = { viewModel.setEvent(it) },
            paddingValues = paddingValues,
        )
    }

    LaunchedEffect(Unit) {
        viewModel.effect
            .onEach { effect ->
                when (effect) {
                    is Effect.Navigation.Pop -> navController.popBackStack()
                    is Effect.Navigation.NavigateToThemeCustomization -> {
                        navController.navigate(DashboardScreens.ThemeCustomization.screenRoute)
                    }

                    is Effect.Navigation.NavigateToChangePin -> {
                        val nextScreenRoute =
                            generateComposableNavigationLink(
                                screen = CommonScreens.QuickPin,
                                arguments =
                                    generateComposableArguments(
                                        mapOf("pinFlow" to PinFlow.UPDATE),
                                    ),
                            )
                        navController.navigate(nextScreenRoute)
                    }

                    is Effect.Navigation.NavigateToMoveWallet -> {
                        navController.navigate(TransferScreens.MoveWallet.screenRoute)
                    }

                    is Effect.Navigation.NavigateToReceiveWallet -> {
                        navController.navigate(TransferScreens.ReceiveWallet.screenRoute)
                    }
                }
            }.collect()
    }
}

@Composable
private fun PreferencesContent(
    state: State,
    onEvent: (Event) -> Unit,
    paddingValues: PaddingValues,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding =
            PaddingValues(
                top = paddingValues.calculateTopPadding() + SPACING_MEDIUM.dp,
                bottom = paddingValues.calculateBottomPadding() + SPACING_MEDIUM.dp,
            ),
    ) {
        item {
            ThemeSection(state, onEvent)
        }

        item {
            HorizontalDivider(
                modifier = Modifier.padding(vertical = SPACING_LARGE.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            )
        }

        item {
            LanguageSection(state, onEvent)
        }

        item {
            HorizontalDivider(
                modifier = Modifier.padding(vertical = SPACING_LARGE.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            )
        }

        item {
            WalletSection(onEvent)
        }
    }
}

@Composable
private fun ThemeSection(
    state: State,
    onEvent: (Event) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        SectionTitle(
            modifier =
                Modifier.padding(
                    start = SPACING_MEDIUM.dp,
                    end = SPACING_MEDIUM.dp,
                    bottom = SPACING_SMALL.dp,
                ),
            text = stringResource(R.string.preferences_theme_label),
        )

        ListItem(
            modifier = Modifier.clickable { expanded = true },
            headlineContent = { Text(stringResource(state.selectedTheme.labelRes)) },
            supportingContent = { Text(stringResource(R.string.preferences_theme_label)) },
            trailingContent = {
                Box {
                    WrapIcon(
                        iconData = if (expanded) AppIcons.KeyboardArrowUp else AppIcons.KeyboardArrowDown,
                        customTint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                    ) {
                        AppTheme.entries.forEach { theme ->
                            DropdownMenuItem(
                                text = { Text(stringResource(theme.labelRes)) },
                                onClick = {
                                    onEvent(Event.OnThemeSelected(theme))
                                    expanded = false
                                },
                                trailingIcon = {
                                    if (state.selectedTheme == theme) {
                                        WrapIcon(
                                            iconData = AppIcons.Check,
                                            customTint = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                },
                            )
                        }
                    }
                }
            },
        )

        ListItem(
            modifier = Modifier.clickable { onEvent(Event.OnThemeCustomizationClicked) },
            headlineContent = { Text(stringResource(R.string.preferences_theme_customization_label)) },
            supportingContent = { Text(stringResource(R.string.preferences_theme_customization_supporting_text)) },
            trailingContent = {
                WrapIcon(
                    iconData = AppIcons.KeyboardArrowRight,
                    customTint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
        )
    }
}

@Composable
private fun LanguageSection(
    state: State,
    onEvent: (Event) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        SectionTitle(
            modifier =
                Modifier.padding(
                    start = SPACING_MEDIUM.dp,
                    end = SPACING_MEDIUM.dp,
                    bottom = SPACING_SMALL.dp,
                ),
            text = stringResource(R.string.preferences_language_label),
        )

        ListItem(
            modifier = Modifier.clickable { expanded = true },
            headlineContent = { Text(stringResource(state.selectedLanguage.labelRes)) },
            supportingContent = { Text(stringResource(R.string.preferences_language_label)) },
            trailingContent = {
                Box {
                    WrapIcon(
                        iconData = if (expanded) AppIcons.KeyboardArrowUp else AppIcons.KeyboardArrowDown,
                        customTint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                    ) {
                        AppLanguage.entries.forEach { language ->
                            DropdownMenuItem(
                                text = { Text(stringResource(language.labelRes)) },
                                onClick = {
                                    onEvent(Event.OnLanguageSelected(language))
                                    expanded = false
                                },
                                trailingIcon = {
                                    if (state.selectedLanguage == language) {
                                        WrapIcon(
                                            iconData = AppIcons.Check,
                                            customTint = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                },
                            )
                        }
                    }
                }
            },
        )
    }
}

@Composable
private fun WalletSection(onEvent: (Event) -> Unit) {
    Column {
        SectionTitle(
            modifier =
                Modifier.padding(
                    start = SPACING_MEDIUM.dp,
                    end = SPACING_MEDIUM.dp,
                    bottom = SPACING_SMALL.dp,
                ),
            text = stringResource(R.string.preferences_wallet_section_label),
        )

        ListItem(
            modifier = Modifier.clickable { onEvent(Event.OnChangePinClicked) },
            headlineContent = { Text(stringResource(R.string.preferences_change_pin_label)) },
            trailingContent = {
                WrapIcon(
                    iconData = AppIcons.KeyboardArrowRight,
                    customTint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
        )

        ListItem(
            modifier = Modifier.clickable { onEvent(Event.OnMoveWalletClicked) },
            headlineContent = { Text(stringResource(R.string.preferences_move_wallet_label)) },
            trailingContent = {
                WrapIcon(
                    iconData = AppIcons.KeyboardArrowRight,
                    customTint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
        )

        ListItem(
            modifier = Modifier.clickable { onEvent(Event.OnReceiveWalletClicked) },
            headlineContent = { Text(stringResource(R.string.preferences_receive_wallet_label)) },
            trailingContent = {
                WrapIcon(
                    iconData = AppIcons.KeyboardArrowRight,
                    customTint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
        )
    }
}

@ThemeModePreviews
@Composable
private fun PreferencesScreenPreview() {
    PreviewTheme {
        PreferencesContent(
            state =
                State(
                    screenTitle = "Preferences",
                    useDynamicColor = false,
                ),
            onEvent = {},
            paddingValues = PaddingValues(0.dp),
        )
    }
}
