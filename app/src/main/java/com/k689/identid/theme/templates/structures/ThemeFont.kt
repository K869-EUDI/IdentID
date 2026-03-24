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

import androidx.annotation.FontRes
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontVariation
import com.k689.identid.theme.templates.structures.ThemeFontRoundness.Companion.toFontVariation
import com.k689.identid.theme.templates.structures.ThemeFontStyle.Companion.toFontStyle
import com.k689.identid.theme.templates.structures.ThemeFontWeight.Companion.toFontWeight
import com.k689.identid.theme.templates.structures.ThemeFontWidth.Companion.toFontVariation

data class ThemeFont(
    @param:FontRes val res: Int,
    val weight: ThemeFontWeight,
    val width: ThemeFontWidth,
    val roundness: ThemeFontRoundness,
    val style: ThemeFontStyle,
) {
    companion object {
        @OptIn(ExperimentalTextApi::class)
        fun ThemeFont.toFont(): Font =
            Font(
                resId = res,
                style = style.toFontStyle(),
                variationSettings =
                    FontVariation.Settings(
                        FontVariation.weight(weight.value.toInt()), // Uses 'wght' axis
                        FontVariation.width(width.value), // Uses 'wdth' axis
                        FontVariation.Setting("ROND", roundness.value), // Uses 'ROND' axis
                    ),
            )
    }
}
