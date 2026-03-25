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

data class DetailState(
    val isLoading: Boolean = true,
    val log: PseudonymTransactionLogUi? = null,
    val showDeleteConfirmation: Boolean = false,
) : ViewState

sealed class DetailEvent : ViewEvent {
    data object Pop : DetailEvent()
    data object RequestDelete : DetailEvent()
    data object ConfirmDelete : DetailEvent()
    data object DismissDelete : DetailEvent()
}

sealed class DetailEffect : ViewSideEffect {
    sealed class Navigation : DetailEffect() {
        data object Pop : Navigation()
    }
}

@KoinViewModel
class PseudonymTransactionLogDetailViewModel(
    private val logId: String,
    private val pseudonymInteractor: PseudonymInteractor,
) : MviViewModel<DetailEvent, DetailState, DetailEffect>() {

    override fun setInitialState(): DetailState = DetailState()

    init {
        loadDetail()
    }

    override fun handleEvents(event: DetailEvent) {
        when (event) {
            is DetailEvent.Pop -> setEffect { DetailEffect.Navigation.Pop }
            is DetailEvent.RequestDelete -> setState { copy(showDeleteConfirmation = true) }
            is DetailEvent.ConfirmDelete -> deleteAndPop()
            is DetailEvent.DismissDelete -> setState { copy(showDeleteConfirmation = false) }
        }
    }

    private fun loadDetail() {
        viewModelScope.launch {
            setState { copy(isLoading = true) }
            val log = pseudonymInteractor.getTransactionLogById(logId)
            setState { copy(isLoading = false, log = log) }
        }
    }

    private fun deleteAndPop() {
        viewModelScope.launch {
            setState { copy(showDeleteConfirmation = false) }
            pseudonymInteractor.deleteTransactionLog(logId)
            setEffect { DetailEffect.Navigation.Pop }
        }
    }
}
