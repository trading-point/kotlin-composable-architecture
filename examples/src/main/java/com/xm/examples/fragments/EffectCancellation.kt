package com.xm.examples.fragments

import com.xm.examples.utils.FactClientLive
import com.xm.examples.utils.Result
import com.xm.examples.utils.SchedulerProvider

data class EffectCancellationState(
    val count: Int = 0,
    val currentTrivia: String? = null,
    val isTriviaRequestInFlight: Boolean = false
)

sealed class EffectsCancellationAction {
    object cancelButtonTapped : EffectsCancellationAction()
    data class stepperChanged(val num: Int) : EffectsCancellationAction()
    object triviaButtonTapped : EffectsCancellationAction()
    data class triviaResponse(val response: Result<String>) : EffectsCancellationAction()
}

interface EffectsCancellationEnvironment {
    val fact: FactClientLive
    val mainQueue: SchedulerProvider

    companion object {

        operator fun invoke(
            fact: FactClientLive,
            mainQueue: SchedulerProvider
        ): EffectsCancellationEnvironment = object : EffectsCancellationEnvironment {
            override val fact: FactClientLive = fact
            override val mainQueue: SchedulerProvider = mainQueue
        }
    }
}

object TriviaRequestId
