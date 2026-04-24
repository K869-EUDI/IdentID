package com.k689.identid.ui.dashboard.preferences

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.k689.identid.theme.AppLanguage
import com.k689.identid.ui.component.AppIcons
import com.k689.identid.ui.component.ListItemDataUi
import com.k689.identid.ui.component.ListItemMainContentDataUi
import com.k689.identid.ui.component.ListItemTrailingContentDataUi
import com.k689.identid.ui.component.content.ContentScreen
import com.k689.identid.ui.component.content.ScreenNavigateAction
import com.k689.identid.ui.component.content.ToolbarConfig
import com.k689.identid.ui.component.utils.VSpacer
import com.k689.identid.ui.component.wrap.SwitchDataUi
import com.k689.identid.ui.component.wrap.WrapCard
import com.k689.identid.ui.component.wrap.WrapListItem
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferencesScreen(
    navController: NavController,
    viewModel: PreferencesViewModel,
) {
    val state by viewModel.viewState.collectAsStateWithLifecycle()
    var themeExpanded by remember { mutableStateOf(false) }
    var languageExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.effect
            .onEach { effect ->
                when (effect) {
                    is Effect.Navigation.Pop -> navController.popBackStack()
                }
            }.collect()
    }

    ContentScreen(
        navigatableAction = ScreenNavigateAction.BACKABLE,
        isLoading = false,
        onBack = { viewModel.setEvent(Event.Pop) },
        toolBarConfig =
            ToolbarConfig(
                title = state.screenTitle,
            ),
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
        ) {
            VSpacer.Medium()

            // Theme section
            SectionHeader(title = state.themeLabel)

            WrapCard {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    PreferencesColorPicker()

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    )

                    // Dynamic Color toggle
                    WrapListItem(
                        item =
                            ListItemDataUi(
                                itemId = "dynamic_color",
                                mainContentData =
                                    ListItemMainContentDataUi.Text(
                                        text = "Use System Colors",
                                    ),
                                supportingText = "Follow Android 12+ wallpaper colors",
                                trailingContentData =
                                    ListItemTrailingContentDataUi.Switch(
                                        switchData =
                                            SwitchDataUi(
                                                isChecked = state.useDynamicColor,
                                            ),
                                    ),
                            ),
                        onItemClick = { viewModel.setEvent(Event.OnUseDynamicColorChanged(!state.useDynamicColor)) },
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    )

                    // Theme Mode Dropdown
                    ExposedDropdownMenuBox(
                        expanded = themeExpanded,
                        onExpandedChange = { themeExpanded = !themeExpanded },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                    .fillMaxWidth(),
                        ) {
                            WrapListItem(
                                item =
                                    ListItemDataUi(
                                        itemId = "theme_mode",
                                        mainContentData =
                                            ListItemMainContentDataUi.Text(
                                                text = "Theme Mode",
                                            ),
                                        supportingText = state.themeOptions[state.selectedTheme.ordinal].second,
                                        trailingContentData =
                                            ListItemTrailingContentDataUi.Icon(
                                                iconData = AppIcons.KeyboardArrowDown,
                                            ),
                                    ),
                                onItemClick = { themeExpanded = true },
                                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                            )
                        }

                        ExposedDropdownMenu(
                            expanded = themeExpanded,
                            onDismissRequest = { themeExpanded = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerLow),
                        ) {
                            state.themeOptions.forEach { (theme, displayName) ->
                                DropdownMenuItem(
                                    text = { Text(text = displayName) },
                                    onClick = {
                                        viewModel.setEvent(Event.OnThemeSelected(theme))
                                        themeExpanded = false
                                    },
                                )
                            }
                        }
                    }

                    // OLED Mode toggle
                    WrapListItem(
                        item =
                            ListItemDataUi(
                                itemId = "oled_mode",
                                mainContentData =
                                    ListItemMainContentDataUi.Text(
                                        text = "OLED Dark Mode",
                                    ),
                                supportingText = "Pure black backgrounds in dark mode",
                                trailingContentData =
                                    ListItemTrailingContentDataUi.Switch(
                                        switchData =
                                            SwitchDataUi(
                                                isChecked = state.isOledMode,
                                            ),
                                    ),
                            ),
                        onItemClick = { viewModel.setEvent(Event.OnOledModeChanged(!state.isOledMode)) },
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    )
                }
            }

            VSpacer.Large()

            // Language section
            SectionHeader(title = state.languageLabel)

            WrapCard {
                ExposedDropdownMenuBox(
                    expanded = languageExpanded,
                    onExpandedChange = { languageExpanded = !languageExpanded },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth(),
                    ) {
                        WrapListItem(
                            item =
                                ListItemDataUi(
                                    itemId = "language_selection",
                                    mainContentData =
                                        ListItemMainContentDataUi.Text(
                                            text = "App Language",
                                        ),
                                    supportingText = AppLanguage.entries[state.selectedLanguage.ordinal].displayName,
                                    trailingContentData =
                                        ListItemTrailingContentDataUi.Icon(
                                            iconData = AppIcons.KeyboardArrowDown,
                                        ),
                                ),
                            onItemClick = { languageExpanded = true },
                            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        )
                    }

                    ExposedDropdownMenu(
                        expanded = languageExpanded,
                        onDismissRequest = { languageExpanded = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerLow),
                    ) {
                        AppLanguage.entries.forEach { language ->
                            DropdownMenuItem(
                                text = { Text(text = language.displayName) },
                                onClick = {
                                    viewModel.setEvent(Event.OnLanguageSelected(language))
                                    languageExpanded = false
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 8.dp, bottom = 8.dp),
    )
}
