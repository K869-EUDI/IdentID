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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.k689.identid.R
import com.k689.identid.theme.ThemeStyle
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
import com.k689.identid.ui.component.utils.VSpacer
import com.k689.identid.ui.component.wrap.WrapIcon
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import java.util.Locale

@Composable
fun ThemeCustomizationScreen(
    navController: NavController,
    viewModel: PreferencesViewModel,
) {
    val state by viewModel.viewState.collectAsStateWithLifecycle()

    ContentScreen(
        navigatableAction = ScreenNavigateAction.CANCELABLE,
        onBack = { viewModel.setEvent(Event.Pop) },
        toolBarConfig = ToolbarConfig(title = stringResource(R.string.preferences_theme_customization_label)),
    ) { paddingValues ->
        ThemeCustomizationContent(
            state = state,
            onEvent = { viewModel.setEvent(it) },
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

                    else -> {}
                }
            }.collect()
    }
}

@Composable
private fun ThemeCustomizationContent(
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
            LargeThemePreview(state)
            VSpacer.Large()
        }

        item {
            ListItem(
                headlineContent = { Text(stringResource(R.string.preferences_system_colors)) },
                supportingContent = { Text(stringResource(R.string.preferences_system_colors_description)) },
                trailingContent = {
                    Switch(
                        checked = state.useDynamicColor,
                        onCheckedChange = { onEvent(Event.OnUseDynamicColorChanged(it)) },
                    )
                },
            )
        }

        if (!state.useDynamicColor) {
            item {
                VSpacer.Medium()
                ColorStyleSelection(state, onEvent)
            }

            item {
                VSpacer.Medium()
                PresetColorSelection(state, onEvent)
            }

            item {
                VSpacer.Large()
                ColorTuningSelection(state, onEvent)
            }
        }

        item {
            VSpacer.Medium()
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = SPACING_MEDIUM.dp, vertical = SPACING_MEDIUM.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.preferences_oled_dark_mode)) },
                supportingContent = { Text(stringResource(R.string.preferences_oled_dark_mode_description)) },
                trailingContent = {
                    Switch(
                        checked = state.isOledMode,
                        onCheckedChange = { onEvent(Event.OnOledModeChanged(it)) },
                    )
                },
            )
        }
    }
}

@Composable
private fun LargeThemePreview(state: State) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = SPACING_MEDIUM.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ElevatedCard(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.1f),
            shape = RoundedCornerShape(32.dp),
            colors =
                CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(24.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Mock Status Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(modifier = Modifier.size(20.dp, 10.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)))
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)))
                        }
                    }

                    // Mock App Bar
                    Text(
                        text = stringResource(R.string.preferences_design_preview_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                    )

                    // Mock Content
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = stringResource(R.string.preferences_design_preview_primary_accent), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Box(
                                modifier =
                                    Modifier
                                        .padding(top = 4.dp)
                                        .size(60.dp, 8.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f)),
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ElevatedCard(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.CenterStart) {
                                Column {
                                    Text(text = stringResource(R.string.preferences_design_preview_secondary), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                    Box(
                                        modifier =
                                            Modifier
                                                .padding(top = 4.dp)
                                                .size(30.dp, 6.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.3f)),
                                    )
                                }
                            }
                        }
                        ElevatedCard(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.CenterStart) {
                                Column {
                                    Text(text = stringResource(R.string.preferences_design_preview_tertiary), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
                                    Box(
                                        modifier =
                                            Modifier
                                                .padding(top = 4.dp)
                                                .size(30.dp, 6.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.3f)),
                                    )
                                }
                            }
                        }
                    }

                    // Mock Bottom Navigation
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        repeat(4) { i ->
                            Box(
                                modifier =
                                    Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (i == 0) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorStyleSelection(
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
            text = stringResource(R.string.preferences_palette_style_label),
        )

        ListItem(
            modifier = Modifier.clickable { expanded = true },
            headlineContent = { Text(stringResource(state.selectedThemeStyle.labelRes)) },
            supportingContent = { Text(stringResource(R.string.preferences_palette_style_label)) },
            trailingContent = {
                Row {
                    WrapIcon(
                        iconData = if (expanded) AppIcons.KeyboardArrowUp else AppIcons.KeyboardArrowDown,
                        customTint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                    ) {
                        ThemeStyle.entries.forEach { style ->
                            DropdownMenuItem(
                                text = { Text(stringResource(style.labelRes)) },
                                onClick = {
                                    onEvent(Event.OnThemeStyleSelected(style))
                                    expanded = false
                                },
                                trailingIcon = {
                                    if (state.selectedThemeStyle == style) {
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
private fun PresetColorSelection(
    state: State,
    onEvent: (Event) -> Unit,
) {
    val presets =
        listOf(
            PresetColor(225f, 0.8f, 0.85f), // Blue
            PresetColor(150f, 0.7f, 0.8f), // Green
            PresetColor(280f, 0.65f, 0.85f), // Purple
            PresetColor(30f, 0.85f, 0.95f), // Orange
            PresetColor(0f, 0.75f, 0.9f), // Red
            PresetColor(330f, 0.7f, 0.9f), // Pink
        )

    Column {
        SectionTitle(
            modifier = Modifier.padding(start = SPACING_MEDIUM.dp, end = SPACING_MEDIUM.dp, bottom = SPACING_SMALL.dp),
            text = stringResource(R.string.preferences_preset_colors_label),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = SPACING_MEDIUM.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(presets) { preset ->
                val presetColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(preset.hue, preset.saturation, preset.value)))
                val isSelected =
                    (state.seedHue - preset.hue).let { it * it } < 1f &&
                        (state.seedSaturation - preset.saturation).let { it * it } < 0.001f &&
                        (state.seedValue - preset.value).let { it * it } < 0.001f

                Box(
                    modifier =
                        Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(presetColor)
                            .clickable { onEvent(Event.OnPresetColorSelected(preset.hue, preset.saturation, preset.value)) }
                            .then(
                                if (isSelected) {
                                    Modifier.border(
                                        3.dp,
                                        MaterialTheme.colorScheme.onSurface,
                                        CircleShape,
                                    )
                                } else {
                                    Modifier
                                },
                            ),
                )
            }
        }
    }
}

private data class PresetColor(
    val hue: Float,
    val saturation: Float,
    val value: Float,
)

@Composable
private fun ColorTuningSelection(
    state: State,
    onEvent: (Event) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = SPACING_MEDIUM.dp)) {
        SectionTitle(
            modifier = Modifier.padding(bottom = SPACING_SMALL.dp),
            text = stringResource(R.string.preferences_fine_tune_colors_label),
        )
        ElevatedCard(
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            shape = RoundedCornerShape(28.dp),
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                TuningSlider(
                    label = stringResource(R.string.preferences_hue_label),
                    value = state.seedHue,
                    onValueChange = { onEvent(Event.OnHueChanged(it)) },
                    valueRange = 0f..360f,
                )
                TuningSlider(
                    label = stringResource(R.string.preferences_saturation_label),
                    value = state.seedSaturation,
                    onValueChange = { onEvent(Event.OnSaturationChanged(it)) },
                    valueRange = 0f..1f,
                )
                TuningSlider(
                    label = stringResource(R.string.preferences_brightness_label),
                    value = state.seedValue,
                    onValueChange = { onEvent(Event.OnValueChanged(it)) },
                    valueRange = 0f..1f,
                )
            }
        }
    }
}

@Composable
private fun TuningSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(text = label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(text = String.format(Locale.getDefault(), "%.0f", value), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
        )
    }
}

@ThemeModePreviews
@Composable
private fun ThemeCustomizationScreenPreview() {
    PreviewTheme {
        ThemeCustomizationContent(
            state =
                State(
                    screenTitle = "Customize",
                    useDynamicColor = false,
                    selectedThemeStyle = ThemeStyle.EXPRESSIVE,
                    seedHue = 0f, // Red
                    seedSaturation = 0.8f,
                    seedValue = 0.9f,
                ),
            onEvent = {},
            paddingValues = PaddingValues(0.dp),
        )
    }
}
