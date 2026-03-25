package com.k689.identid.ui.pseudonym.detail

import androidx.lifecycle.viewModelScope
import com.k689.identid.interactor.pseudonym.PseudonymDetailUi
import com.k689.identid.interactor.pseudonym.PseudonymInteractor
import com.k689.identid.ui.mvi.MviViewModel
import com.k689.identid.ui.mvi.ViewEvent
import com.k689.identid.ui.mvi.ViewSideEffect
import com.k689.identid.ui.mvi.ViewState
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

data class State(
    val isLoading: Boolean = true,
    val detail: PseudonymDetailUi? = null,
    val editAlias: String = "",
    val showDeleteConfirmation: Boolean = false,
) : ViewState

sealed class Event : ViewEvent {
    data class Init(val pseudonymId: String) : Event()
    data object Pop : Event()
    data class UpdateAlias(val alias: String) : Event()
    data object SaveAlias : Event()
    data object RequestDelete : Event()
    data object ConfirmDelete : Event()
    data object DismissDelete : Event()
}

sealed class Effect : ViewSideEffect {
    data object AliasSaved : Effect()
    sealed class Navigation : Effect() {
        data object Pop : Navigation()
    }
}

@KoinViewModel
class PseudonymDetailViewModel(
    private val pseudonymId: String,
    private val pseudonymInteractor: PseudonymInteractor,
) : MviViewModel<Event, State, Effect>() {

    override fun setInitialState(): State = State()

    init {
        loadDetail()
    }

    override fun handleEvents(event: Event) {
        when (event) {
            is Event.Init -> loadDetail()
            is Event.Pop -> setEffect { Effect.Navigation.Pop }
            is Event.UpdateAlias -> setState { copy(editAlias = event.alias) }
            is Event.SaveAlias -> saveAlias()
            is Event.RequestDelete -> setState { copy(showDeleteConfirmation = true) }
            is Event.ConfirmDelete -> deleteAndNavigateBack()
            is Event.DismissDelete -> setState { copy(showDeleteConfirmation = false) }
        }
    }

    private fun loadDetail() {
        viewModelScope.launch {
            setState { copy(isLoading = true) }
            val detail = pseudonymInteractor.getDetail(pseudonymId)
            setState {
                copy(
                    isLoading = false,
                    detail = detail,
                    editAlias = detail?.userAlias ?: "",
                )
            }
        }
    }

    private fun saveAlias() {
        viewModelScope.launch {
            val alias = viewState.value.editAlias.ifBlank { null }
            pseudonymInteractor.updateAlias(pseudonymId, alias)
            loadDetail()
            setEffect { Effect.AliasSaved }
        }
    }

    private fun deleteAndNavigateBack() {
        viewModelScope.launch {
            setState { copy(showDeleteConfirmation = false) }
            pseudonymInteractor.deletePseudonym(pseudonymId)
            setEffect { Effect.Navigation.Pop }
        }
    }
}
