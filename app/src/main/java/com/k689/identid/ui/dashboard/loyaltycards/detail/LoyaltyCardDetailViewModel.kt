package com.k689.identid.ui.dashboard.loyaltycards.detail

import androidx.lifecycle.viewModelScope
import com.k689.identid.interactor.dashboard.LoyaltyCardDetailUi
import com.k689.identid.interactor.dashboard.LoyaltyCardsInteractor
import com.k689.identid.ui.component.content.ContentErrorConfig
import com.k689.identid.ui.mvi.MviViewModel
import com.k689.identid.ui.mvi.ViewEvent
import com.k689.identid.ui.mvi.ViewSideEffect
import com.k689.identid.ui.mvi.ViewState
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

data class State(
    val isLoading: Boolean = true,
    val detail: LoyaltyCardDetailUi? = null,
    val error: ContentErrorConfig? = null,
    val showDeleteConfirmation: Boolean = false,
) : ViewState

sealed class Event : ViewEvent {
    data object Init : Event()
    data object Pop : Event()
    data object ToggleBookmark : Event()
    data object RequestDelete : Event()
    data object DismissDelete : Event()
    data object ConfirmDelete : Event()
}

sealed class Effect : ViewSideEffect {
    sealed class Navigation : Effect() {
        data object Pop : Navigation()
    }
}

@KoinViewModel
class LoyaltyCardDetailViewModel(
    private val loyaltyCardId: String,
    private val loyaltyCardsInteractor: LoyaltyCardsInteractor,
) : MviViewModel<Event, State, Effect>() {
    override fun setInitialState(): State = State()

    init {
        load()
    }

    override fun handleEvents(event: Event) {
        when (event) {
            is Event.Init -> load()
            is Event.Pop -> setEffect { Effect.Navigation.Pop }
            is Event.ToggleBookmark -> toggleBookmark()
            is Event.RequestDelete -> setState { copy(showDeleteConfirmation = true) }
            is Event.DismissDelete -> setState { copy(showDeleteConfirmation = false) }
            is Event.ConfirmDelete -> delete()
        }
    }

    private fun load() {
        viewModelScope.launch {
            setState { copy(isLoading = true, error = null) }
            val detail = loyaltyCardsInteractor.getDetail(loyaltyCardId)
            setState {
                copy(
                    isLoading = false,
                    detail = detail,
                    error =
                        if (detail == null) {
                            ContentErrorConfig(onCancel = { setEvent(Event.Pop) })
                        } else {
                            null
                        },
                )
            }
        }
    }

    private fun toggleBookmark() {
        viewModelScope.launch {
            loyaltyCardsInteractor.toggleBookmark(loyaltyCardId)
            load()
        }
    }

    private fun delete() {
        viewModelScope.launch {
            loyaltyCardsInteractor.delete(loyaltyCardId)
            setEffect { Effect.Navigation.Pop }
        }
    }
}