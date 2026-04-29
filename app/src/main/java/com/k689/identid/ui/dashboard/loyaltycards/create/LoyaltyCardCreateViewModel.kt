package com.k689.identid.ui.dashboard.loyaltycards.create

import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.k689.identid.interactor.dashboard.LoyaltyCardsInteractor
import com.k689.identid.ui.mvi.MviViewModel
import com.k689.identid.ui.mvi.ViewEvent
import com.k689.identid.ui.mvi.ViewSideEffect
import com.k689.identid.ui.mvi.ViewState
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

data class State(
    val barcodeValue: String,
    val barcodeFormat: String,
    val name: String = "",
    val isSaving: Boolean = false,
) : ViewState

sealed class Event : ViewEvent {
    data object Pop : Event()
    data class NameChanged(val value: String) : Event()
    data object Save : Event()
}

sealed class Effect : ViewSideEffect {
    sealed class Navigation : Effect() {
        data object Pop : Navigation()
        data class ToDetail(val id: String) : Navigation()
    }
}

@KoinViewModel
class LoyaltyCardCreateViewModel(
    barcodeValue: String,
    barcodeFormat: String,
    private val loyaltyCardsInteractor: LoyaltyCardsInteractor,
) : MviViewModel<Event, State, Effect>() {
    private val decodedBarcodeValue = Uri.decode(barcodeValue)
    private val decodedBarcodeFormat = Uri.decode(barcodeFormat)

    override fun setInitialState(): State =
        State(
            barcodeValue = decodedBarcodeValue,
            barcodeFormat = decodedBarcodeFormat,
        )

    override fun handleEvents(event: Event) {
        when (event) {
            is Event.Pop -> setEffect { Effect.Navigation.Pop }
            is Event.NameChanged -> setState { copy(name = event.value) }
            is Event.Save -> save()
        }
    }

    private fun save() {
        val name = viewState.value.name.trim()
        if (name.isBlank()) {
            return
        }

        viewModelScope.launch {
            setState { copy(isSaving = true) }
            val id = loyaltyCardsInteractor.save(name, viewState.value.barcodeValue, viewState.value.barcodeFormat)
            setState { copy(isSaving = false) }
            setEffect { Effect.Navigation.ToDetail(id) }
        }
    }
}