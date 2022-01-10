package com.xm.examples.fragments

import com.xm.examples.fragments.CounterAction.decrementButtonTapped
import com.xm.examples.fragments.CounterAction.incrementButtonTapped
import com.xm.tka.Effects
import com.xm.tka.Reducer

/**
 * Counter
 */
data class CounterState(val count: Int = 0)

sealed class CounterAction {
    object decrementButtonTapped : CounterAction()
    object incrementButtonTapped : CounterAction()
}

object CounterEnvironment

val counterReducer = Reducer<CounterState, CounterAction, CounterEnvironment> { state, action, _ ->
    when (action) {
        decrementButtonTapped -> state.copy(count = state.count - 1) + Effects.none()
        incrementButtonTapped -> state.copy(count = state.count + 1) + Effects.none()
    }
}