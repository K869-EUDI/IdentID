/*
 * Copyright (c) 2025 European Commission
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

package com.k689.identid.theme.templates.structures

import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight

sealed class ThemeFontWeight(
    val value: Float,
) {
    data object W100 : ThemeFontWeight(100f)

    data object W200 : ThemeFontWeight(200f)

    data object W300 : ThemeFontWeight(300f)

    data object W400 : ThemeFontWeight(400f)

    data object W500 : ThemeFontWeight(500f)

    data object W600 : ThemeFontWeight(600f)

    data object W700 : ThemeFontWeight(700f)

    data object W750 : ThemeFontWeight(750f)

    data object W800 : ThemeFontWeight(800f)

    data object W900 : ThemeFontWeight(900f)

    companion object {
        // Standard Compose FontWeight conversion (for non-variable fallback)
        fun ThemeFontWeight.toFontWeight(): FontWeight = FontWeight(this.value.toInt())

        // Variable Font Variation setting (for Google Sans Flex 'wght' axis)
        fun ThemeFontWeight.toFontVariation(): FontVariation.Setting = FontVariation.weight(this.value.toInt())
    }
}
