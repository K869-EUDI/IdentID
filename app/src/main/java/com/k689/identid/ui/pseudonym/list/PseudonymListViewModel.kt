package com.k689.identid.ui.pseudonym.list

import androidx.lifecycle.viewModelScope
import com.k689.identid.interactor.pseudonym.PseudonymGroupUi
import com.k689.identid.interactor.pseudonym.PseudonymInteractor
import com.k689.identid.ui.mvi.MviViewModel
import com.k689.identid.ui.mvi.ViewEvent
import com.k689.identid.ui.mvi.ViewSideEffect
import com.k689.identid.ui.mvi.ViewState
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

data class State(
    val isLoading: Boolean = true,
    val groups: List<PseudonymGroupUi> = emptyList(),
) : ViewState

sealed class Event : ViewEvent {
    data object Init : Event()

    data object Pop : Event()

    data class NavigateToDetail(
        val pseudonymId: String,
    ) : Event()
}

sealed class Effect : ViewSideEffect {
    sealed class Navigation : Effect() {
        data object Pop : Navigation()

        data class ToDetail(
            val pseudonymId: String,
        ) : Navigation()
    }
}

@KoinViewModel
class PseudonymListViewModel(
    private val pseudonymInteractor: PseudonymInteractor,
) : MviViewModel<Event, State, Effect>() {
    override fun setInitialState(): State = State()

    init {
        loadPseudonyms()
    }

    override fun handleEvents(event: Event) {
        when (event) {
            is Event.Init -> loadPseudonyms()
            is Event.Pop -> setEffect { Effect.Navigation.Pop }
            is Event.NavigateToDetail -> setEffect { Effect.Navigation.ToDetail(event.pseudonymId) }
        }
    }

    private fun loadPseudonyms() {
        viewModelScope.launch {
            setState { copy(isLoading = true) }
            val groups = pseudonymInteractor.getAllGroupedByRp()
            setState { copy(isLoading = false, groups = groups) }
        }
    }
}
