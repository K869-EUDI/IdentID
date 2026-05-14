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
package com.k689.identid.theme

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import com.k689.identid.theme.sets.ThemeSet
import com.k689.identid.theme.templates.ThemeColorsTemplate
import com.k689.identid.theme.templates.ThemeColorsTemplate.Companion.toColorScheme
import com.k689.identid.theme.templates.ThemeDimensTemplate
import com.k689.identid.theme.templates.ThemeShapesTemplate
import com.k689.identid.theme.templates.ThemeShapesTemplate.Companion.toShapes
import com.k689.identid.theme.templates.ThemeTypographyTemplate
import com.k689.identid.theme.templates.ThemeTypographyTemplate.Companion.toTypography
import com.k689.identid.theme.values.ThemeColors
import com.k689.identid.theme.values.ThemeShapes
import com.k689.identid.theme.values.ThemeTypography

class ThemeManager {
    /**
     * Contains the data of the theme like colors, shapes, typography etc.
     */
    lateinit var set: ThemeSet
        private set

    /**
     * Defines if dynamic theming is supported. Notice that Dynamic color is available on Android 12+.
     */
    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
    val dynamicThemeSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    @Composable
    fun Theme(
        darkTheme: Boolean = isSystemInDarkTheme(),
        disableDynamicTheming: Boolean = false,
        seedColor: Color? = null,
        themeStyle: ThemeStyle = ThemeStyle.TONAL,
        isOledMode: Boolean = false,
        useDynamicColor: Boolean = true,
        content: @Composable () -> Unit,
    ) {
        val lightColorScheme = set.lightColors
        val darkColorScheme = set.darkColors

        val baseColorScheme =
            when {
                !disableDynamicTheming && dynamicThemeSupported && useDynamicColor -> {
                    val context = LocalContext.current
                    if (darkTheme) {
                        dynamicDarkColorScheme(context)
                    } else {
                        dynamicLightColorScheme(context)
                    }
                }

                seedColor != null -> {
                    generateColorSchemeFromSeed(
                        seed = seedColor,
                        isDark = darkTheme,
                        base = if (darkTheme) darkColorScheme else lightColorScheme,
                        style = themeStyle,
                    )
                }

                darkTheme -> {
                    set.darkColors
                }

                else -> {
                    set.lightColors
                }
            }

        val colorScheme =
            if (darkTheme && isOledMode) {
                baseColorScheme.applyOledMode()
            } else {
                baseColorScheme
            }

        set = set.copy(isInDarkMode = darkTheme)

        MaterialTheme(
            colorScheme = colorScheme,
            shapes = set.shapes,
            typography = set.typo,
            content = content,
        )
    }

    private fun ColorScheme.applyOledMode(): ColorScheme {
        return copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceContainerLowest = Color.Black,
            surfaceContainerLow = Color(0xFF0D0D0D),
            surfaceContainer = Color(0xFF141414),
            surfaceContainerHigh = Color(0xFF1D1D1D),
            surfaceContainerHighest = Color(0xFF282828),
        )
    }

    companion object {
        /**
         * Private instance of manager.
         */
        private lateinit var _instance: ThemeManager

        /**
         * Instance of theme manager. Built from [Builder].
         */
        val instance: ThemeManager
            get() {
                if (this::_instance.isInitialized.not()) {
                    _instance =
                        Builder()
                            .withLightColors(ThemeColors.lightColors)
                            .withDarkColors(ThemeColors.darkColors)
                            .withTypography(ThemeTypography.typo)
                            .withShapes(ThemeShapes.shapes)
                            .withDimensions(
                                ThemeDimensTemplate(
                                    screenPadding = 10.0,
                                ),
                            ).build()
                }

                return _instance
            }

        /**
         * Initializes the theme manager using the builder provided. This initializes the
         * static instance of the manager.
         */
        fun ThemeManager.build(builder: Builder): ThemeManager {
            set =
                ThemeSet(
                    isInDarkMode = builder.isInDarkMode == true,
                    lightColors = builder.lightColors.toColorScheme(),
                    darkColors = builder.darkColors.toColorScheme(),
                    typo = builder.typography.toTypography(),
                    shapes = builder.shapes.toShapes(),
                    dimens = builder.dimensions,
                )
            return this
        }
    }

    class Builder(
        val isInDarkMode: Boolean? = null,
    ) {
        lateinit var lightColors: ThemeColorsTemplate
        lateinit var darkColors: ThemeColorsTemplate
        lateinit var typography: ThemeTypographyTemplate
        lateinit var shapes: ThemeShapesTemplate
        lateinit var dimensions: ThemeDimensTemplate

        /**
         * Set the colors set for the theme configuration. These colors refer to the light theme
         * colors. You can set the dark mode colors by calling [withDarkColors]. If no dark colors are
         * set, light will be used for both modes.
         *
         * @param colors Set of colors to be used to construct the theme.
         *
         * @return This instance for chaining.
         */
        fun withLightColors(colors: ThemeColorsTemplate): Builder {
            this.lightColors = colors
            return this
        }

        /**
         * Set the colors set for the theme configuration. These colors refer to the light theme
         * colors. You can set the dark mode colors by calling [withDarkColors]. If no dark colors are
         * set, light will be used for both modes.
         *
         * @param colors Set of colors to be used to construct the theme.
         *
         * @return This instance for chaining.
         */
        fun withDarkColors(colors: ThemeColorsTemplate): Builder {
            this.darkColors = colors
            return this
        }

        /**
         * Set the typography for theme.
         *
         * @param typography Set of typography to be used to construct the theme.
         *
         * @return This instance for chaining.
         */
        fun withTypography(typography: ThemeTypographyTemplate): Builder {
            this.typography = typography
            return this
        }

        /**
         * Set the shapes for theme.
         *
         * @param shapes Set of shapes to be used to construct the theme.
         *
         * @return This instance for chaining.
         */
        fun withShapes(shapes: ThemeShapesTemplate): Builder {
            this.shapes = shapes
            return this
        }

        /**
         * Set the dimensions for theme.
         *
         * @param dimens Set of dimensions to be used to construct the theme.
         *
         * @return This instance for chaining.
         */
        fun withDimensions(dimens: ThemeDimensTemplate): Builder {
            this.dimensions = dimens
            return this
        }

        fun build(
            buildStatic: Boolean = true,
        ): ThemeManager {
            // Check light colors.
            if (this::lightColors.isInitialized.not()) {
                throw RuntimeException("lightColors is not initialized. Can not build theme manager.")
            }

            // If dark colors not initialized, set as light.
            if (this::darkColors.isInitialized.not()) {
                darkColors = lightColors
            }

            // Check typography.
            if (this::typography.isInitialized.not()) {
                throw RuntimeException("typography is not initialized. Can not build theme manager.")
            }

            // Check shapes.
            if (this::shapes.isInitialized.not()) {
                throw RuntimeException("shapes is not initialized. Can not build theme manager.")
            }

            // Check dimensions.
            if (this::dimensions.isInitialized.not()) {
                throw RuntimeException("dimensions is not initialized. Can not build theme manager.")
            }

            // Initialize instance.
            if (buildStatic) _instance = ThemeManager()

            // Initialize manager.
            return when (buildStatic) {
                true -> _instance.build(this)
                false -> ThemeManager().build(this)
            }
        }
    }

    private fun generateColorSchemeFromSeed(
        seed: Color,
        isDark: Boolean,
        base: ColorScheme,
        style: ThemeStyle = ThemeStyle.TONAL,
    ): ColorScheme {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(seed.toArgb(), hsv)
        val hue = hsv[0]
        val saturation = hsv[1]
        val value = hsv[2]

        fun fromHsv(h: Float, s: Float, v: Float): Color {
            return Color(android.graphics.Color.HSVToColor(floatArrayOf(h % 360f, s.coerceIn(0f, 1f), v.coerceIn(0f, 1f))))
        }

        // Adjust hue/saturation based on Material 3 Styles
        val primaryHue = hue
        val secondaryHue = when (style) {
            ThemeStyle.ANALOGOUS -> hue + 30f
            ThemeStyle.VIBRANT -> hue + 45f
            ThemeStyle.EXPRESSIVE -> hue + 90f
            else -> hue // TONAL / MONOCHROMATIC
        }
        val tertiaryHue = when (style) {
            ThemeStyle.ANALOGOUS -> hue - 30f
            ThemeStyle.VIBRANT -> hue + 120f
            ThemeStyle.EXPRESSIVE -> hue + 180f
            else -> hue + 60f
        }

        val chromaMult = if (style == ThemeStyle.MONOCHROMATIC) 0.1f else 1.0f
        val vibranceMult = if (style == ThemeStyle.VIBRANT || style == ThemeStyle.EXPRESSIVE) 1.2f else 1.0f
        val surfaceTintIntensity = if (style == ThemeStyle.EXPRESSIVE) 1.5f else 1.0f

        // Material 3 Tonal roles (Light / Dark)
        return if (isDark) {
            // Dark Mode Tones (M3 Standard: 80 for Primary, 30 for Container, 90 for OnContainer)
            val primary = fromHsv(primaryHue, (saturation * 0.4f * chromaMult * vibranceMult).coerceIn(0f, 1f), 0.9f)
            val onPrimary = fromHsv(primaryHue, 0.1f, 0.2f)
            val primaryContainer = fromHsv(primaryHue, (saturation * 0.5f * chromaMult).coerceIn(0f, 1f), 0.35f)
            val onPrimaryContainer = fromHsv(primaryHue, 0.15f, 0.95f)

            val secondary = fromHsv(secondaryHue, (saturation * 0.3f * chromaMult * vibranceMult).coerceIn(0f, 1f), 0.85f)
            val onSecondary = fromHsv(secondaryHue, 0.1f, 0.2f)
            val secondaryContainer = fromHsv(secondaryHue, (saturation * 0.4f * chromaMult).coerceIn(0f, 1f), 0.25f)
            val onSecondaryContainer = fromHsv(secondaryHue, 0.15f, 0.9f)

            val tertiary = fromHsv(tertiaryHue, (saturation * 0.4f * chromaMult * vibranceMult).coerceIn(0f, 1f), 0.8f)
            val onTertiary = fromHsv(tertiaryHue, 0.1f, 0.2f)
            val tertiaryContainer = fromHsv(tertiaryHue, (saturation * 0.45f * chromaMult).coerceIn(0f, 1f), 0.25f)
            val onTertiaryContainer = fromHsv(tertiaryHue, 0.15f, 0.85f)

            val background = fromHsv(primaryHue, (saturation * 0.12f * chromaMult * surfaceTintIntensity).coerceIn(0f, 1f), 0.06f)
            val surface = fromHsv(primaryHue, (saturation * 0.18f * chromaMult * surfaceTintIntensity).coerceIn(0f, 1f), 0.12f)

            base.copy(
                primary = primary,
                onPrimary = onPrimary,
                primaryContainer = primaryContainer,
                onPrimaryContainer = onPrimaryContainer,
                secondary = secondary,
                onSecondary = onSecondary,
                secondaryContainer = secondaryContainer,
                onSecondaryContainer = onSecondaryContainer,
                tertiary = tertiary,
                onTertiary = onTertiary,
                tertiaryContainer = tertiaryContainer,
                onTertiaryContainer = onTertiaryContainer,
                background = background,
                onBackground = Color(0xFFE2E2E6),
                surface = surface,
                onSurface = Color(0xFFE2E2E6),
                surfaceVariant = fromHsv(primaryHue, (saturation * 0.25f * chromaMult * surfaceTintIntensity).coerceIn(0f, 1f), 0.25f),
                onSurfaceVariant = Color(0xFFC4C6D0),
                outline = Color(0xFF8E9099),
                surfaceContainerLowest = fromHsv(primaryHue, (saturation * 0.10f * chromaMult * surfaceTintIntensity).coerceIn(0f, 1f), 0.04f),
                surfaceContainerLow = fromHsv(primaryHue, (saturation * 0.15f * chromaMult * surfaceTintIntensity).coerceIn(0f, 1f), 0.10f),
                surfaceContainer = fromHsv(primaryHue, (saturation * 0.20f * chromaMult * surfaceTintIntensity).coerceIn(0f, 1f), 0.12f),
                surfaceContainerHigh = fromHsv(primaryHue, (saturation * 0.25f * chromaMult * surfaceTintIntensity).coerceIn(0f, 1f), 0.17f),
                surfaceContainerHighest = fromHsv(primaryHue, (saturation * 0.30f * chromaMult * surfaceTintIntensity).coerceIn(0f, 1f), 0.22f),
            )
        } else {
            // Light Mode Tones (M3 Standard: 40 for Primary, 90 for Container, 10 for OnContainer)
            val primary = fromHsv(primaryHue, (saturation * 1.1f * chromaMult).coerceIn(0f, 1f), 0.45f)
            val onPrimary = Color.White
            val primaryContainer = fromHsv(primaryHue, (saturation * 0.25f * chromaMult).coerceIn(0f, 1f), 0.92f)
            val onPrimaryContainer = fromHsv(primaryHue, 0.9f, 0.15f)

            val secondary = fromHsv(secondaryHue, (saturation * 0.6f * chromaMult).coerceIn(0f, 1f), 0.4f)
            val onSecondary = Color.White
            val secondaryContainer = fromHsv(secondaryHue, (saturation * 0.2f * chromaMult).coerceIn(0f, 1f), 0.94f)
            val onSecondaryContainer = fromHsv(secondaryHue, 0.9f, 0.15f)

            val tertiary = fromHsv(tertiaryHue, (saturation * 0.5f * chromaMult * vibranceMult).coerceIn(0f, 1f), 0.35f)
            val onTertiary = Color.White
            val tertiaryContainer = fromHsv(tertiaryHue, (saturation * 0.25f * chromaMult).coerceIn(0f, 1f), 0.92f)
            val onTertiaryContainer = fromHsv(tertiaryHue, 0.9f, 0.15f)

            val background = fromHsv(primaryHue, (saturation * 0.08f * chromaMult * surfaceTintIntensity).coerceIn(0f, 1f), 0.99f)
            val surface = fromHsv(primaryHue, (saturation * 0.12f * chromaMult * surfaceTintIntensity).coerceIn(0f, 1f), 0.98f)

            base.copy(
                primary = primary,
                onPrimary = onPrimary,
                primaryContainer = primaryContainer,
                onPrimaryContainer = onPrimaryContainer,
                secondary = secondary,
                onSecondary = onSecondary,
                secondaryContainer = secondaryContainer,
                onSecondaryContainer = onSecondaryContainer,
                tertiary = tertiary,
                onTertiary = onTertiary,
                tertiaryContainer = tertiaryContainer,
                onTertiaryContainer = onTertiaryContainer,
                background = background,
                onBackground = Color(0xFF1A1C1E),
                surface = surface,
                onSurface = Color(0xFF1A1C1E),
                surfaceVariant = fromHsv(primaryHue, (saturation * 0.15f * chromaMult * surfaceTintIntensity).coerceIn(0f, 1f), 0.92f),
                onSurfaceVariant = Color(0xFF44474E),
                outline = Color(0xFF74777F),
                surfaceContainerLowest = Color.White,
                surfaceContainerLow = fromHsv(primaryHue, (saturation * 0.08f * chromaMult * surfaceTintIntensity).coerceIn(0f, 1f), 0.96f),
                surfaceContainer = fromHsv(primaryHue, (saturation * 0.10f * chromaMult * surfaceTintIntensity).coerceIn(0f, 1f), 0.94f),
                surfaceContainerHigh = fromHsv(primaryHue, (saturation * 0.12f * chromaMult * surfaceTintIntensity).coerceIn(0f, 1f), 0.92f),
                surfaceContainerHighest = fromHsv(primaryHue, (saturation * 0.15f * chromaMult * surfaceTintIntensity).coerceIn(0f, 1f), 0.90f),
            )
        }
    }
}
