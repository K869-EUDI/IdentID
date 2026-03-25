package com.k689.identid.ui.pseudonym.transactions

import androidx.lifecycle.viewModelScope
import com.k689.identid.interactor.pseudonym.PseudonymInteractor
import com.k689.identid.interactor.pseudonym.PseudonymTransactionLogUi
import com.k689.identid.ui.mvi.MviViewModel
import com.k689.identid.ui.mvi.ViewEvent
import com.k689.identid.ui.mvi.ViewSideEffect
import com.k689.identid.ui.mvi.ViewState
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

data class State(
    val isLoading: Boolean = true,
    val logs: List<PseudonymTransactionLogUi> = emptyList(),
    val showDeleteAllConfirmation: Boolean = false,
) : ViewState

sealed class Event : ViewEvent {
    data object Init : Event()
    data object Pop : Event()
    data class NavigateToDetail(val logId: String) : Event()
    data object RequestDeleteAll : Event()
    data object ConfirmDeleteAll : Event()
    data object DismissDeleteAll : Event()
}

sealed class Effect : ViewSideEffect {
    sealed class Navigation : Effect() {
        data object Pop : Navigation()
        data class ToDetail(val logId: String) : Navigation()
    }
}

@KoinViewModel
class PseudonymTransactionLogListViewModel(
    private val pseudonymInteractor: PseudonymInteractor,
) : MviViewModel<Event, State, Effect>() {

    override fun setInitialState(): State = State()

    init {
        loadLogs()
    }

    override fun handleEvents(event: Event) {
        when (event) {
            is Event.Init -> loadLogs()
            is Event.Pop -> setEffect { Effect.Navigation.Pop }
            is Event.NavigateToDetail -> setEffect { Effect.Navigation.ToDetail(event.logId) }
            is Event.RequestDeleteAll -> setState { copy(showDeleteAllConfirmation = true) }
            is Event.ConfirmDeleteAll -> {
                setState { copy(showDeleteAllConfirmation = false) }
                deleteAll()
            }
            is Event.DismissDeleteAll -> setState { copy(showDeleteAllConfirmation = false) }
        }
    }

    private fun loadLogs() {
        viewModelScope.launch {
            setState { copy(isLoading = true) }
            val logs = pseudonymInteractor.getAllTransactionLogs()
            setState { copy(isLoading = false, logs = logs) }
        }
    }

    private fun deleteAll() {
        viewModelScope.launch {
            pseudonymInteractor.deleteAllTransactionLogs()
            loadLogs()
        }
    }
}
