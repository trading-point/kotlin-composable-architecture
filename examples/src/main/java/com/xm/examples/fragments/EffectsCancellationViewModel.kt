package com.xm.examples.fragments

import androidx.lifecycle.ViewModel
import com.xm.examples.utils.FactClientLive
import com.xm.examples.utils.Result
import com.xm.examples.utils.SchedulerProvider
import com.xm.tka.Effects
import com.xm.tka.Reducer
import com.xm.tka.Store
import com.xm.tka.cancellable
import com.xm.tka.toEffect
import com.xm.tka.ui.ViewStore
import com.xm.tka.ui.ViewStore.Companion.view
import io.reactivex.rxjava3.kotlin.cast

class EffectsCancellationViewModel : ViewModel() {

    private val store = Store(
        initialState = EffectCancellationState(),
        reducer = effectsCancellationReducer,
        environment = EffectsCancellationEnvironment(FactClientLive, SchedulerProvider)
    )

    val viewStore: ViewStore<EffectCancellationState, EffectsCancellationAction> = store.view()
}

// Domain

data class EffectCancellationState(
    val count: Int = 0,
    val currentTrivia: String? = null,
    val isTriviaRequestInFlight: Boolean = false
)

sealed class EffectsCancellationAction {
    object CancelButtonTapped : EffectsCancellationAction()
    data class StepperDecrement(val num: Int) : EffectsCancellationAction()
    data class StepperIncrement(val num: Int) : EffectsCancellationAction()
    object TriviaButtonTapped : EffectsCancellationAction()
    data class TriviaResponse(val response: Result<String>) : EffectsCancellationAction()
}

interface EffectsCancellationEnvironment {
    val fact: FactClientLive
    val schedulerProvider: SchedulerProvider

    companion object {

        operator fun invoke(
            fact: FactClientLive,
            mainQueue: SchedulerProvider
        ): EffectsCancellationEnvironment = object : EffectsCancellationEnvironment {
            override val fact: FactClientLive = fact
            override val schedulerProvider: SchedulerProvider = mainQueue
        }
    }
}

object TriviaRequestId

private val effectsCancellationReducer =
    Reducer<EffectCancellationState, EffectsCancellationAction, EffectsCancellationEnvironment> { state, action, env ->
        when (action) {
            is EffectsCancellationAction.StepperDecrement -> state.copy(
                count = action.num - 1,
                currentTrivia = null,
                isTriviaRequestInFlight = false
            ) + Effects.none()

            is EffectsCancellationAction.StepperIncrement -> state.copy(
                count = action.num + 1,
                currentTrivia = null,
                isTriviaRequestInFlight = false
            ) + Effects.none()

            EffectsCancellationAction.CancelButtonTapped -> state.copy(
                isTriviaRequestInFlight = false
            ) + Effects.cancel(TriviaRequestId)

            EffectsCancellationAction.TriviaButtonTapped -> state.copy(
                currentTrivia = null,
                isTriviaRequestInFlight = true
            ) + env.fact.execute(state.count.toString())
                .subscribeOn(env.schedulerProvider.io())
                .observeOn(env.schedulerProvider.mainThread())
                .map { EffectsCancellationAction.TriviaResponse(it) }
                .toEffect()
                .cancellable(TriviaRequestId)
                .cast()

            is EffectsCancellationAction.TriviaResponse ->
                when (action.response) {
                    is Result.Success -> state.copy(
                        currentTrivia = action.response.data,
                        isTriviaRequestInFlight = false
                    ) + Effects.none()
                    is Result.Error -> state.copy(
                        isTriviaRequestInFlight = false
                    ) + Effects.none()
                }
        }
    }

