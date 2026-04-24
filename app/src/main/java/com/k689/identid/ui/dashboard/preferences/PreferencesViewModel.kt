package com.k689.identid.ui.dashboard.preferences

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.k689.identid.R
import com.k689.identid.controller.storage.PrefKeys
import com.k689.identid.provider.resources.ResourceProvider
import com.k689.identid.theme.AppLanguage
import com.k689.identid.theme.AppTheme
import com.k689.identid.theme.AppTheme.Companion.toUiText
import com.k689.identid.ui.mvi.MviViewModel
import com.k689.identid.ui.mvi.ViewEvent
import com.k689.identid.ui.mvi.ViewSideEffect
import com.k689.identid.ui.mvi.ViewState
import org.koin.android.annotation.KoinViewModel

data class State(
    val screenTitle: String,
    val themeLabel: String = "",
    val languageLabel: String = "",
    val themeOptions: List<Pair<AppTheme, String>> = emptyList(),
    val selectedTheme: AppTheme = AppTheme.SYSTEM,
    val selectedLanguage: AppLanguage = AppLanguage.SYSTEM,
    val selectedColor: Color? = null,
    val isOledMode: Boolean = false,
    val useDynamicColor: Boolean = true,
) : ViewState

sealed class Event : ViewEvent {
    data object Pop : Event()

    data class OnThemeSelected(
        val theme: AppTheme,
    ) : Event()

    data class OnLanguageSelected(
        val language: AppLanguage,
    ) : Event()

    data class OnColorSelected(
        val color: Color,
    ) : Event()

    data class OnOledModeChanged(
        val enabled: Boolean,
    ) : Event()

    data class OnUseDynamicColorChanged(
        val enabled: Boolean,
    ) : Event()
}

sealed class Effect : ViewSideEffect {
    sealed class Navigation : Effect() {
        data object Pop : Navigation()
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
            themeLabel = resourceProvider.getString(R.string.preferences_theme_label),
            languageLabel = resourceProvider.getString(R.string.preferences_language_label),
            themeOptions = AppTheme.entries.map { it to it.toUiText(resourceProvider) },
            selectedTheme = prefKeys.theme.value,
            selectedLanguage = AppLanguage.fromCurrentLocale(resourceProvider.provideContext()),
            selectedColor = prefKeys.seedColor.value?.let { Color(it) },
            isOledMode = prefKeys.oledMode.value,
            useDynamicColor = prefKeys.useDynamicColor.value,
        )

    override fun handleEvents(event: Event) {
        when (event) {
            is Event.Pop -> {
                setEffect { Effect.Navigation.Pop }
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

            is Event.OnColorSelected -> {
                prefKeys.setSeedColor(event.color.toArgb())
                setState { copy(selectedColor = event.color) }
            }

            is Event.OnOledModeChanged -> {
                prefKeys.setOledMode(event.enabled)
                setState { copy(isOledMode = event.enabled) }
            }

            is Event.OnUseDynamicColorChanged -> {
                prefKeys.setUseDynamicColor(event.enabled)
                setState { copy(useDynamicColor = event.enabled) }
            }
        }
    }
}
