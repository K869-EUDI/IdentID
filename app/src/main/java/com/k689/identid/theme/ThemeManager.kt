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
        isOledMode: Boolean = false,
        useDynamicColor: Boolean = true,
        content: @Composable () -> Unit,
    ) {
        val lightColorScheme = set.lightColors
        val darkColorScheme = set.darkColors

        val colorScheme =
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
                    generateColorSchemeFromSeed(seedColor, darkTheme, if (darkTheme) darkColorScheme else lightColorScheme, isOledMode)
                }

                darkTheme -> {
                    set.darkColors
                }

                else -> {
                    set.lightColors
                }
            }

        set = set.copy(isInDarkMode = darkTheme)

        MaterialTheme(
            colorScheme = colorScheme,
            shapes = set.shapes,
            typography = set.typo,
            content = content,
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
        isOledMode: Boolean = false,
    ): ColorScheme {
        val toneSurface =
            if (isDark) {
                if (isOledMode) Color.Black else Color(0xFF060606)
            } else {
                Color.White
            }

        return if (isDark) {
            // Material 3 Dark Tones - High vibrancy with deep, tinted backgrounds
            val primary = lerp(seed, Color.White, 0.4f)
            val primaryContainer = lerp(seed, Color.Black, 0.4f)
            val secondary = lerp(seed, Color.White, 0.2f)
            val secondaryContainer = lerp(seed, Color.Black, 0.6f)

            base.copy(
                primary = primary,
                onPrimary = Color.Black,
                primaryContainer = primaryContainer,
                onPrimaryContainer = Color.White,
                secondary = secondary,
                onSecondary = Color.Black,
                secondaryContainer = secondaryContainer,
                onSecondaryContainer = Color.White,
                tertiary = lerp(seed, Color.Cyan, 0.3f),
                surface = lerp(seed, toneSurface, if (isOledMode) 0.98f else 0.92f),
                onSurface = Color.White,
                background = lerp(seed, toneSurface, if (isOledMode) 1f else 0.95f),
                onBackground = Color.White,
                surfaceVariant = lerp(seed, Color(0xFF49454F), 0.6f),
                onSurfaceVariant = Color(0xFFCAC4D0),
                outline = lerp(seed, Color(0xFF938F99), 0.4f),
                surfaceContainerLowest = lerp(seed, if (isOledMode) Color.Black else Color(0xFF0E0E0E), 0.94f),
                surfaceContainerLow = lerp(seed, if (isOledMode) Color(0xFF040404) else Color(0xFF131313), 0.88f),
                surfaceContainer = lerp(seed, if (isOledMode) Color(0xFF080808) else Color(0xFF1A1A1A), 0.82f),
                surfaceContainerHigh = lerp(seed, if (isOledMode) Color(0xFF101010) else Color(0xFF242424), 0.78f),
                surfaceContainerHighest = lerp(seed, if (isOledMode) Color(0xFF181818) else Color(0xFF2C2C2C), 0.74f),
            )
        } else {
            // Material 3 Light Tones - Vibrant but balanced (matching Figma "warm" look)
            val primary = seed
            val primaryContainer = lerp(seed, Color.White, 0.82f)
            val secondary = lerp(seed, Color.Black, 0.2f)
            val secondaryContainer = lerp(seed, Color.White, 0.88f)

            base.copy(
                primary = primary,
                onPrimary = if (primary.luminance() > 0.5f) Color.Black else Color.White,
                primaryContainer = primaryContainer,
                onPrimaryContainer = lerp(seed, Color.Black, 0.7f),
                secondary = secondary,
                onSecondary = Color.White,
                secondaryContainer = secondaryContainer,
                onSecondaryContainer = lerp(seed, Color.Black, 0.7f),
                tertiary = lerp(seed, Color.Cyan, 0.2f),
                surface = lerp(seed, Color.White, 0.96f), // Warm background (Tone 96-98)
                onSurface = lerp(seed, Color.Black, 0.85f), // Tinted dark text
                background = lerp(seed, Color.White, 0.96f),
                onBackground = lerp(seed, Color.Black, 0.85f),
                surfaceVariant = lerp(seed, Color(0xFFE7E0EC), 0.82f),
                onSurfaceVariant = lerp(seed, Color.Black, 0.6f),
                outline = lerp(seed, Color(0xFF79747E), 0.5f),
                surfaceContainerLowest = Color.White,
                surfaceContainerLow = lerp(seed, Color.White, 0.92f), // Clear but subtle tint
                surfaceContainer = lerp(seed, Color.White, 0.88f),
                surfaceContainerHigh = lerp(seed, Color.White, 0.84f),
                surfaceContainerHighest = lerp(seed, Color.White, 0.80f),
            )
        }
    }
}
