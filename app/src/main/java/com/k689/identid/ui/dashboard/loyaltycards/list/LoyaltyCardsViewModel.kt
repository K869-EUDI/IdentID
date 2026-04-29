package com.k689.identid.ui.dashboard.loyaltycards.list

import androidx.lifecycle.viewModelScope
import com.k689.identid.interactor.dashboard.LoyaltyCardListItemUi
import com.k689.identid.interactor.dashboard.LoyaltyCardsInteractor
import com.k689.identid.ui.mvi.MviViewModel
import com.k689.identid.ui.mvi.ViewEvent
import com.k689.identid.ui.mvi.ViewSideEffect
import com.k689.identid.ui.mvi.ViewState
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

data class State(
    val isLoading: Boolean = true,
    val cards: List<LoyaltyCardListItemUi> = emptyList(),
) : ViewState

sealed class Event : ViewEvent {
    data object Init : Event()
    data object Pop : Event()
    data object AddCardPressed : Event()
    data class CardClicked(val id: String) : Event()
}

sealed class Effect : ViewSideEffect {
    sealed class Navigation : Effect() {
        data object Pop : Navigation()
        data object ToScan : Navigation()
        data class ToDetail(val id: String) : Navigation()
    }
}

@KoinViewModel
class LoyaltyCardsViewModel(
    private val loyaltyCardsInteractor: LoyaltyCardsInteractor,
) : MviViewModel<Event, State, Effect>() {
    override fun setInitialState(): State = State()

    init {
        loadCards()
    }

    override fun handleEvents(event: Event) {
        when (event) {
            is Event.Init -> loadCards()
            is Event.Pop -> setEffect { Effect.Navigation.Pop }
            is Event.AddCardPressed -> setEffect { Effect.Navigation.ToScan }
            is Event.CardClicked -> setEffect { Effect.Navigation.ToDetail(event.id) }
        }
    }

    private fun loadCards() {
        viewModelScope.launch {
            setState { copy(isLoading = true) }
            val cards = loyaltyCardsInteractor.getAll()
            setState { copy(isLoading = false, cards = cards) }
        }
    }
}