package com.k689.identid.ui.dashboard.preferences

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
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
import com.k689.identid.theme.AppLanguage
import com.k689.identid.theme.AppTheme
import com.k689.identid.ui.component.ListItemDataUi
import com.k689.identid.ui.component.ListItemMainContentDataUi
import com.k689.identid.ui.component.ListItemTrailingContentDataUi
import com.k689.identid.ui.component.content.ContentScreen
import com.k689.identid.ui.component.content.ContentTitle
import com.k689.identid.ui.component.content.ScreenNavigateAction
import com.k689.identid.ui.component.content.ToolbarConfig
import com.k689.identid.ui.component.wrap.RadioButtonDataUi
import com.k689.identid.ui.component.wrap.WrapListItem
import com.k689.identid.ui.component.wrap.WrapTextField
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
                    .padding(paddingValues),
        ) {
            // Theme section
            ContentTitle(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                title = state.themeLabel,
            )

            ExposedDropdownMenuBox(
                expanded = themeExpanded,
                onExpandedChange = { themeExpanded = !themeExpanded },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                OutlinedTextField(
                    modifier =
                        Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                    value = state.themeOptions[state.selectedTheme.ordinal].second ?: "",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = themeExpanded)
                    },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                )

                ExposedDropdownMenu(
                    expanded = themeExpanded,
                    onDismissRequest = { themeExpanded = false },
                ) {
                    state.themeOptions.forEach { (theme, displayName) ->
                        DropdownMenuItem(
                            text = {
                                Text(text = displayName)
                            },
                            onClick = {
                                viewModel.setEvent(Event.OnThemeSelected(theme))
                                themeExpanded = false
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                        )
                    }
                }
            }

/*            state.themeOptions.onEach { (theme, displayName) ->
                val isSelected = theme == state.selectedTheme
                WrapListItem(
                    item =
                        ListItemDataUi(
                            itemId = theme.name,
                            mainContentData =
                                ListItemMainContentDataUi.Text(displayName),
                            trailingContentData =
                                ListItemTrailingContentDataUi.RadioButton(
                                    RadioButtonDataUi(
                                        isSelected = isSelected,
                                        onCheckedChange = {
                                            viewModel.setEvent(
                                                Event.OnThemeSelected(
                                                    theme,
                                                ),
                                            )
                                        },
                                    ),
                                ),
                        ),
                    modifier = Modifier.padding(8.dp),
                    onItemClick = { viewModel.setEvent(Event.OnThemeSelected(theme)) },
                )
            }*/

            Spacer(modifier = Modifier.height(16.dp))

            // Language section
            ContentTitle(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                title = state.languageLabel,
            )

            ExposedDropdownMenuBox(
                expanded = languageExpanded,
                onExpandedChange = { languageExpanded = !languageExpanded },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                OutlinedTextField(
                    modifier =
                        Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                    value = AppLanguage.entries[state.selectedLanguage.ordinal].displayName ?: " ",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = themeExpanded)
                    },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                )

                ExposedDropdownMenu(
                    expanded = languageExpanded,
                    onDismissRequest = { languageExpanded = false },
                ) {
                    AppLanguage.entries.onEach { language ->
                        DropdownMenuItem(
                            text = {
                                Text(text = language.displayName)
                            },
                            onClick = {
                                viewModel.setEvent(Event.OnLanguageSelected(language))
                                languageExpanded = false
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                        )
                    }
                }
            }

            /*AppLanguage.entries.onEach { language ->
                val isSelected = language == state.selectedLanguage
                WrapListItem(
                    item =
                        ListItemDataUi(
                            itemId = language.name,
                            mainContentData =
                                ListItemMainContentDataUi.Text(language.displayName),
                            trailingContentData =
                                ListItemTrailingContentDataUi.RadioButton(
                                    RadioButtonDataUi(
                                        isSelected = isSelected,
                                        onCheckedChange = {
                                            viewModel.setEvent(
                                                Event.OnLanguageSelected(
                                                    language,
                                                ),
                                            )
                                        },
                                    ),
                                ),
                        ),
                    modifier = Modifier.padding(8.dp),
                    onItemClick = { viewModel.setEvent(Event.OnLanguageSelected(language)) },
                )
            }*/
        }
    }
}
