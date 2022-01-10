package com.xm.examples.fragments

import com.xm.examples.utils.FactClientLive
import com.xm.examples.utils.Result
import com.xm.examples.utils.SchedulerProvider

// MARK: - Feature domain

data class EffectsBasicsState(
    val count: Int = 0,
    val isNumberFactRequestInFlight: Boolean = false,
    val numberFact: String? = null
)

sealed class EffectsBasicsAction {
    object decrementButtonTapped : EffectsBasicsAction()
    object incrementButtonTapped : EffectsBasicsAction()
    object numberFactButtonTapped : EffectsBasicsAction()
    data class numberFactResponse(val response: Result<String>) : EffectsBasicsAction()
}

interface EffectsBasicsEnvironment {
    val fact: FactClientLive
    val mainQueue: SchedulerProvider

    companion object {

        operator fun invoke(
            fact: FactClientLive,
            mainQueue: SchedulerProvider
        ): EffectsBasicsEnvironment = object : EffectsBasicsEnvironment {
            override val fact: FactClientLive = fact
            override val mainQueue: SchedulerProvider = mainQueue
        }
    }
}
