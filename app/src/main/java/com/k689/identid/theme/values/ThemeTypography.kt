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

package com.k689.identid.theme.values

import com.k689.identid.R
import com.k689.identid.theme.templates.ThemeTextStyle
import com.k689.identid.theme.templates.ThemeTypographyTemplate
import com.k689.identid.theme.templates.structures.ThemeFont
import com.k689.identid.theme.templates.structures.ThemeFontRoundness
import com.k689.identid.theme.templates.structures.ThemeFontStyle
import com.k689.identid.theme.templates.structures.ThemeFontWeight
import com.k689.identid.theme.templates.structures.ThemeFontWidth
import com.k689.identid.theme.templates.structures.ThemeTextAlign

internal class ThemeTypography {
    companion object {
        val typo: ThemeTypographyTemplate
            get() {
                return ThemeTypographyTemplate(
                    displayLarge =
                        ThemeTextStyle(
                            fontFamily = listOf(GoogleSansFlexRegular),
                            fontSize = 57,
                            letterSpacing = -0.25f,
                            textAlign = ThemeTextAlign.Start,
                        ),
                    displayMedium =
                        ThemeTextStyle(
                            fontFamily = listOf(GoogleSansFlexRegular),
                            fontSize = 45,
                            letterSpacing = 0f,
                            textAlign = ThemeTextAlign.Start,
                        ),
                    displaySmall =
                        ThemeTextStyle(
                            fontFamily = listOf(GoogleSansFlexRegular),
                            fontSize = 36,
                            letterSpacing = 0f,
                            textAlign = ThemeTextAlign.Start,
                        ),
                    headlineLarge =
                        ThemeTextStyle(
                            fontFamily = listOf(GoogleSansFlexHeading1),
                            fontSize = 32,
                            letterSpacing = 0f,
                            textAlign = ThemeTextAlign.Start,
                        ),
                    headlineMedium =
                        ThemeTextStyle(
                            fontFamily = listOf(GoogleSansFlexHeading1),
                            fontSize = 28,
                            letterSpacing = 0f,
                            textAlign = ThemeTextAlign.Start,
                        ),
                    headlineSmall =
                        ThemeTextStyle(
                            fontFamily = listOf(GoogleSansFlexHeading2),
                            fontSize = 24,
                            letterSpacing = 0f,
                            textAlign = ThemeTextAlign.Start,
                        ),
                    titleLarge =
                        ThemeTextStyle(
                            fontFamily = listOf(GoogleSansFlexTitle),
                            fontSize = 22,
                            letterSpacing = 0f,
                            textAlign = ThemeTextAlign.Start,
                        ),
                    titleMedium =
                        ThemeTextStyle(
                            fontFamily = listOf(GoogleSansFlexTitle),
                            fontSize = 16,
                            letterSpacing = 0.15f,
                            textAlign = ThemeTextAlign.Start,
                        ),
                    titleSmall =
                        ThemeTextStyle(
                            fontFamily = listOf(GoogleSansFlexTitle),
                            fontSize = 14,
                            letterSpacing = 0.1f,
                            textAlign = ThemeTextAlign.Start,
                        ),
                    labelLarge =
                        ThemeTextStyle(
                            fontFamily = listOf(GoogleSansFlexMedium),
                            fontSize = 14,
                            letterSpacing = 0.1f,
                            textAlign = ThemeTextAlign.Start,
                        ),
                    labelMedium =
                        ThemeTextStyle(
                            fontFamily = listOf(GoogleSansFlexMedium),
                            fontSize = 12,
                            letterSpacing = 0.5f,
                            textAlign = ThemeTextAlign.Start,
                        ),
                    labelSmall =
                        ThemeTextStyle(
                            fontFamily = listOf(GoogleSansFlexMedium),
                            fontSize = 11,
                            letterSpacing = 0.5f,
                            textAlign = ThemeTextAlign.Start,
                        ),
                    bodyLarge =
                        ThemeTextStyle(
                            fontFamily = listOf(GoogleSansFlexRegular),
                            fontSize = 16,
                            letterSpacing = 0.5f,
                            textAlign = ThemeTextAlign.Start,
                        ),
                    bodyMedium =
                        ThemeTextStyle(
                            fontFamily = listOf(GoogleSansFlexRegular),
                            fontSize = 14,
                            letterSpacing = 0.25f,
                            textAlign = ThemeTextAlign.Start,
                        ),
                    bodySmall =
                        ThemeTextStyle(
                            fontFamily = listOf(GoogleSansFlexRegular),
                            fontSize = 12,
                            letterSpacing = 0.4f,
                            textAlign = ThemeTextAlign.Start,
                        ),
                )
            }
    }
}

internal val GoogleSansFlexTitle =
    ThemeFont(
        res = R.font.google_sans_flex,
        weight = ThemeFontWeight.W700,
        width = ThemeFontWidth.W105,
        roundness = ThemeFontRoundness.Sharp,
        style = ThemeFontStyle.Normal,
    )

internal val GoogleSansFlexHeading1 =
    ThemeFont(
        res = R.font.google_sans_flex,
        weight = ThemeFontWeight.W750,
        width = ThemeFontWidth.W130,
        roundness = ThemeFontRoundness.Full,
        style = ThemeFontStyle.Normal,
    )

internal val GoogleSansFlexHeading2 =
    ThemeFont(
        res = R.font.google_sans_flex,
        weight = ThemeFontWeight.W600,
        width = ThemeFontWidth.W130,
        roundness = ThemeFontRoundness.Full,
        style = ThemeFontStyle.Normal,
    )

internal val GoogleSansFlexRegular =
    ThemeFont(
        res = R.font.google_sans_flex,
        weight = ThemeFontWeight.W400,
        width = ThemeFontWidth.W100, // Standard width
        roundness = ThemeFontRoundness.Normal,
        style = ThemeFontStyle.Normal,
    )

internal val GoogleSansFlexMedium =
    ThemeFont(
        res = R.font.google_sans_flex,
        weight = ThemeFontWeight.W500,
        width = ThemeFontWidth.W100,
        roundness = ThemeFontRoundness.Normal,
        style = ThemeFontStyle.Normal,
    )
