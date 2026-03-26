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

package com.k689.identid.ui.dashboard.authenticate

import androidx.activity.ComponentActivity
import androidx.lifecycle.viewModelScope
import com.k689.identid.R
import com.k689.identid.config.PresentationMode
import com.k689.identid.config.QrScanFlow
import com.k689.identid.config.QrScanUiConfig
import com.k689.identid.config.RequestUriConfig
import com.k689.identid.di.core.getOrCreatePresentationScope
import com.k689.identid.interactor.proximity.ProximityQRInteractor
import com.k689.identid.interactor.proximity.ProximityQRPartialState
import com.k689.identid.navigation.CommonScreens
import com.k689.identid.navigation.DashboardScreens
import com.k689.identid.navigation.ProximityScreens
import com.k689.identid.navigation.helper.generateComposableArguments
import com.k689.identid.navigation.helper.generateComposableNavigationLink
import com.k689.identid.provider.resources.ResourceProvider
import com.k689.identid.ui.component.content.ContentErrorConfig
import com.k689.identid.ui.mvi.MviViewModel
import com.k689.identid.ui.mvi.ViewEvent
import com.k689.identid.ui.mvi.ViewSideEffect
import com.k689.identid.ui.mvi.ViewState
import com.k689.identid.ui.serializer.UiSerializer
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

data class State(
    val isLoading: Boolean = true,
    val error: ContentErrorConfig? = null,
    val qrCode: String = "",
) : ViewState

sealed class Event : ViewEvent {
    data object Init : Event()

    data object GoBack : Event()

    data object OpenScanQr : Event()

    data class NfcEngagement(
        val componentActivity: ComponentActivity,
        val enable: Boolean,
    ) : Event()
}

sealed class Effect : ViewSideEffect {
    sealed class Navigation : Effect() {
        data class SwitchScreen(
            val screenRoute: String,
        ) : Navigation()

        data object Pop : Navigation()
    }
}

@KoinViewModel
class AuthenticateViewModel(
    private val uiSerializer: UiSerializer,
    private val resourceProvider: ResourceProvider,
) : MviViewModel<Event, State, Effect>() {
    private var interactorJob: Job? = null
    private var interactor: ProximityQRInteractor? = null

    override fun setInitialState(): State = State()

    override fun handleEvents(event: Event) {
        when (event) {
            is Event.Init -> {
                if (interactor == null) {
                    initializeConfig()
                    generateQrCode()
                }
            }

            is Event.GoBack -> {
                cleanUp()
                setState { copy(error = null) }
                setEffect { Effect.Navigation.Pop }
            }

            is Event.OpenScanQr -> {
                setEffect {
                    Effect.Navigation.SwitchScreen(
                        screenRoute =
                            generateComposableNavigationLink(
                                screen = CommonScreens.QrScan,
                                arguments =
                                    generateComposableArguments(
                                        mapOf(
                                            QrScanUiConfig.serializedKeyName to
                                                uiSerializer.toBase64(
                                                    QrScanUiConfig(
                                                        title = resourceProvider.getString(R.string.presentation_qr_scan_title),
                                                        subTitle = resourceProvider.getString(R.string.presentation_qr_scan_subtitle),
                                                        qrScanFlow = QrScanFlow.Presentation,
                                                    ),
                                                    QrScanUiConfig.Parser,
                                                ),
                                        ),
                                    ),
                            ),
                    )
                }
            }

            is Event.NfcEngagement -> {
                interactor?.toggleNfcEngagement(
                    event.componentActivity,
                    event.enable,
                )
            }
        }
    }

    private fun initializeConfig() {
        val scope = getOrCreatePresentationScope()
        interactor = scope.get()
        interactor?.setConfig(RequestUriConfig(PresentationMode.Ble(DashboardScreens.Dashboard.screenRoute)))
    }

    private fun generateQrCode() {
        setState {
            copy(
                isLoading = true,
                error = null,
            )
        }

        interactorJob =
            viewModelScope.launch {
                interactor?.startQrEngagement()?.collect { response ->
                    when (response) {
                        is ProximityQRPartialState.Error -> {
                            setState {
                                copy(
                                    isLoading = false,
                                    error =
                                        ContentErrorConfig(
                                            onRetry = { setEvent(Event.Init) },
                                            errorSubTitle = response.error,
                                            onCancel = { setEvent(Event.GoBack) },
                                        ),
                                )
                            }
                        }

                        is ProximityQRPartialState.QrReady -> {
                            setState {
                                copy(
                                    isLoading = false,
                                    error = null,
                                    qrCode = response.qrCode,
                                )
                            }
                        }

                        is ProximityQRPartialState.Connected -> {
                            unsubscribe()
                            setEffect {
                                Effect.Navigation.SwitchScreen(
                                    screenRoute =
                                        generateComposableNavigationLink(
                                            screen = ProximityScreens.Request,
                                            arguments =
                                                generateComposableArguments(
                                                    mapOf(
                                                        RequestUriConfig.serializedKeyName to
                                                            uiSerializer.toBase64(
                                                                RequestUriConfig(
                                                                    PresentationMode.Ble(
                                                                        DashboardScreens.Dashboard.screenRoute,
                                                                    ),
                                                                ),
                                                                RequestUriConfig.Parser,
                                                            ),
                                                    ),
                                                ),
                                        ),
                                )
                            }
                        }

                        is ProximityQRPartialState.Disconnected -> {
                            unsubscribe()
                            setEvent(Event.GoBack)
                        }
                    }
                }
            }
    }

    private fun unsubscribe() {
        interactorJob?.cancel()
    }

    private fun cleanUp() {
        unsubscribe()
        getOrCreatePresentationScope().close()
        interactor?.cancelTransfer()
    }

    override fun onCleared() {
        cleanUp()
        super.onCleared()
    }
}
