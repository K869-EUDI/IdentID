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

package com.k689.identid.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.k689.identid.ui.component.utils.SPACING_MEDIUM
import com.k689.identid.ui.component.wrap.ButtonConfig
import com.k689.identid.ui.component.wrap.ButtonType
import com.k689.identid.ui.component.wrap.WrapButton
import com.k689.identid.ui.component.wrap.WrapIcon

/**
 * A dynamic footer component with a large action button.
 * Used for primary screen actions like scanning, submitting, or proceeding.
 */
@Composable
fun LargeActionFooter(
    modifier: Modifier = Modifier,
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    icon: IconDataUi? = null,
    buttonType: ButtonType = ButtonType.PRIMARY,
) {
    Column(
        modifier = modifier.padding(SPACING_MEDIUM.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        WrapButton(
            modifier = Modifier.fillMaxWidth(),
            buttonConfig =
                ButtonConfig(
                    type = buttonType,
                    shape = RoundedCornerShape(16.dp),
                    onClick = onClick,
                    enabled = enabled,
                ),
        ) {
            if (icon != null) {
                WrapIcon(
                    iconData = icon,
                    modifier = Modifier.padding(end = 8.dp).size(24.dp),
                )
            }
            Text(
                text = text,
                style =
                    MaterialTheme.typography.headlineSmall.copy(
                        color = if (buttonType == ButtonType.PRIMARY) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                    ),
            )
        }
    }
}
