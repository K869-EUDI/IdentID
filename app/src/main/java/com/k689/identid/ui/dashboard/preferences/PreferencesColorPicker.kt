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

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel

@Composable
fun PreferencesColorPicker(viewModel: PreferencesViewModel = koinViewModel()) {
    val state by viewModel.viewState.collectAsStateWithLifecycle()
    val availableColors =
        listOf(
            Color(0xFF4CAF50), // Green
            Color(0xFF2196F3), // Blue
            Color(0xFF9C27B0), // Purple
            Color(0xFFE91E63), // Pink
            Color(0xFFFF9800), // Orange
            Color(0xFF795548), // Brown
        )

    val selectedColor = state.selectedColor ?: availableColors[0]

    LazyVerticalGrid(
        columns = GridCells.Adaptive(32.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(availableColors) { color ->
            ColorSwatch(
                color = color,
                isSelected = color == selectedColor,
                onClick = {
                    viewModel.setEvent(Event.OnColorSelected(color))
                },
            )
        }
    }
}

@Composable
fun ColorSwatch(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val scale by animateFloatAsState(targetValue = if (isSelected) 1.1f else 1f)

    Box(
        modifier =
            Modifier
                .scale(scale)
                .size(60.dp)
                // M3 often uses ExtraLarge shape (rounded corners) instead of perfect circles
                .clip(MaterialTheme.shapes.extraLarge)
                .background(color)
                .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        if (isSelected) {
            // M3 checkmark, contrasting against the chosen color
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                // Ensure the icon is visible by calculating contrast (or just use white/black)
                tint = Color.White,
                modifier = Modifier.size(32.dp),
            )
        }
    }
}
