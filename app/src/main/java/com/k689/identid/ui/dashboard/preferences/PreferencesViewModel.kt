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

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.k689.identid.R
import com.k689.identid.controller.storage.PrefKeys
import com.k689.identid.provider.resources.ResourceProvider
import com.k689.identid.theme.AppLanguage
import com.k689.identid.theme.AppTheme
import com.k689.identid.theme.ThemeStyle
import com.k689.identid.ui.mvi.MviViewModel
import com.k689.identid.ui.mvi.ViewEvent
import com.k689.identid.ui.mvi.ViewSideEffect
import com.k689.identid.ui.mvi.ViewState
import org.koin.android.annotation.KoinViewModel

data class State(
    val screenTitle: String,
    val selectedTheme: AppTheme = AppTheme.SYSTEM,
    val selectedLanguage: AppLanguage = AppLanguage.SYSTEM,
    val selectedThemeStyle: ThemeStyle = ThemeStyle.TONAL,
    val seedHue: Float = 225f,
    val seedSaturation: Float = 0.8f,
    val seedValue: Float = 0.85f,
    val isOledMode: Boolean = false,
    val useDynamicColor: Boolean = true,
) : ViewState {
    val seedColor: Color
        get() = Color(android.graphics.Color.HSVToColor(floatArrayOf(seedHue, seedSaturation, seedValue)))
}

sealed class Event : ViewEvent {
    data object Pop : Event()

    data object OnThemeCustomizationClicked : Event()

    data object OnChangePinClicked : Event()

    data object OnMoveWalletClicked : Event()

    data object OnReceiveWalletClicked : Event()

    data class OnThemeSelected(val theme: AppTheme) : Event()

    data class OnLanguageSelected(val language: AppLanguage) : Event()

    data class OnThemeStyleSelected(val style: ThemeStyle) : Event()

    data class OnHueChanged(val hue: Float) : Event()

    data class OnSaturationChanged(val saturation: Float) : Event()

    data class OnValueChanged(val value: Float) : Event()

    data class OnOledModeChanged(val enabled: Boolean) : Event()

    data class OnUseDynamicColorChanged(val enabled: Boolean) : Event()

    data class OnPresetColorSelected(val hue: Float, val saturation: Float, val value: Float) : Event()
}

sealed class Effect : ViewSideEffect {
    sealed class Navigation : Effect() {
        data object Pop : Navigation()

        data object NavigateToThemeCustomization : Navigation()

        data object NavigateToChangePin : Navigation()

        data object NavigateToMoveWallet : Navigation()

        data object NavigateToReceiveWallet : Navigation()
    }
}

@KoinViewModel
class PreferencesViewModel(
    private val resourceProvider: ResourceProvider,
    private val prefKeys: PrefKeys,
) : MviViewModel<Event, State, Effect>() {
    override fun setInitialState(): State =
        State(
            screenTitle = resourceProvider.getString(R.string.preferences_screen_title),
            selectedTheme = prefKeys.theme.value,
            selectedLanguage = AppLanguage.fromCurrentLocale(resourceProvider.provideContext()),
            selectedThemeStyle = prefKeys.themeStyle.value,
            seedHue = prefKeys.seedHue.value,
            seedSaturation = prefKeys.seedSaturation.value,
            seedValue = prefKeys.seedValue.value,
            isOledMode = prefKeys.oledMode.value,
            useDynamicColor = prefKeys.useDynamicColor.value,
        )

    override fun handleEvents(event: Event) {
        when (event) {
            is Event.Pop -> {
                setEffect { Effect.Navigation.Pop }
            }

            is Event.OnThemeCustomizationClicked -> {
                setEffect { Effect.Navigation.NavigateToThemeCustomization }
            }

            is Event.OnChangePinClicked -> {
                setEffect { Effect.Navigation.NavigateToChangePin }
            }

            is Event.OnMoveWalletClicked -> {
                setEffect { Effect.Navigation.NavigateToMoveWallet }
            }

            is Event.OnReceiveWalletClicked -> {
                setEffect { Effect.Navigation.NavigateToReceiveWallet }
            }

            is Event.OnThemeSelected -> {
                prefKeys.setTheme(event.theme)
                setState { copy(selectedTheme = event.theme) }
            }

            is Event.OnLanguageSelected -> {
                AppLanguage.applyAndRestart(
                    context = resourceProvider.provideContext(),
                    language = event.language,
                )
            }

            is Event.OnThemeStyleSelected -> {
                prefKeys.setThemeStyle(event.style)
                setState { copy(selectedThemeStyle = event.style) }
            }

            is Event.OnHueChanged -> {
                prefKeys.setSeedHue(event.hue)
                setState { copy(seedHue = event.hue) }
                updateSeedColor()
            }

            is Event.OnSaturationChanged -> {
                prefKeys.setSeedSaturation(event.saturation)
                setState { copy(seedSaturation = event.saturation) }
                updateSeedColor()
            }

            is Event.OnValueChanged -> {
                prefKeys.setSeedValue(event.value)
                setState { copy(seedValue = event.value) }
                updateSeedColor()
            }

            is Event.OnOledModeChanged -> {
                prefKeys.setOledMode(event.enabled)
                setState { copy(isOledMode = event.enabled) }
            }

            is Event.OnUseDynamicColorChanged -> {
                prefKeys.setUseDynamicColor(event.enabled)
                setState { copy(useDynamicColor = event.enabled) }
            }

            is Event.OnPresetColorSelected -> {
                prefKeys.setSeedHue(event.hue)
                prefKeys.setSeedSaturation(event.saturation)
                prefKeys.setSeedValue(event.value)
                setState {
                    copy(
                        seedHue = event.hue,
                        seedSaturation = event.saturation,
                        seedValue = event.value,
                    )
                }
                updateSeedColor()
            }
        }
    }

    private fun updateSeedColor() {
        val currentState = viewState.value
        prefKeys.setSeedColor(currentState.seedColor.toArgb())
    }
}
