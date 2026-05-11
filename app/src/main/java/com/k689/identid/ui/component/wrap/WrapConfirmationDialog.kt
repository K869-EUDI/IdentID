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

package com.k689.identid.ui.component.wrap

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.k689.identid.ui.component.preview.PreviewTheme
import com.k689.identid.ui.component.preview.ThemeModePreviews
import com.k689.identid.ui.component.utils.SPACING_SMALL

/**
 * A reusable confirmation dialog that follows the app's Material You theme.
 * It uses [WrapButton] for primary and secondary actions to ensure consistency.
 */
@Composable
fun WrapConfirmationDialog(
    title: String,
    message: String,
    primaryButtonText: String,
    onPrimaryClick: () -> Unit,
    secondaryButtonText: String? = null,
    onSecondaryClick: () -> Unit = {},
    isPrimaryWarning: Boolean = false,
    onDismissRequest: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SPACING_SMALL.dp),
            ) {
                secondaryButtonText?.let {
                    WrapButton(
                        modifier = Modifier.weight(1f),
                        buttonConfig = ButtonConfig(
                            type = ButtonType.SECONDARY,
                            onClick = onSecondaryClick,
                        )
                    ) {
                        Text(
                            text = it,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
                WrapButton(
                    modifier = Modifier.weight(1f),
                    buttonConfig = ButtonConfig(
                        type = ButtonType.PRIMARY,
                        onClick = onPrimaryClick,
                        isWarning = isPrimaryWarning,
                    )
                ) {
                    Text(
                        text = primaryButtonText,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
    )
}

@ThemeModePreviews
@Composable
private fun WrapConfirmationDialogPreview() {
    PreviewTheme {
        WrapConfirmationDialog(
            title = "Delete item?",
            message = "Are you sure you want to delete this item? This action cannot be undone.",
            primaryButtonText = "Delete",
            onPrimaryClick = {},
            secondaryButtonText = "Cancel",
            onSecondaryClick = {},
            isPrimaryWarning = true,
            onDismissRequest = {}
        )
    }
}

@ThemeModePreviews
@Composable
private fun WrapConfirmationDialogSingleButtonPreview() {
    PreviewTheme {
        WrapConfirmationDialog(
            title = "Success",
            message = "Operation completed successfully.",
            primaryButtonText = "OK",
            onPrimaryClick = {},
            onDismissRequest = {}
        )
    }
}
