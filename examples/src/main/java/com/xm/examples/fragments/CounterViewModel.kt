package com.xm.examples.fragments

import androidx.lifecycle.ViewModel
import com.xm.examples.fragments.CounterAction.DecrementButtonTapped
import com.xm.examples.fragments.CounterAction.IncrementButtonTapped
import com.xm.tka.Effects
import com.xm.tka.Reducer
import com.xm.tka.Store
import com.xm.tka.ui.ViewStore
import com.xm.tka.ui.ViewStore.Companion.view

class CounterViewModel : ViewModel() {

    private val store = Store(
        initialState = CounterState(),
        reducer = counterReducer,
        environment = CounterEnvironment
    )

    val viewStore: ViewStore<CounterState, CounterAction> = store.view()
}

// Domain

data class CounterState(val count: Int = 0)

sealed class CounterAction {
    object DecrementButtonTapped : CounterAction()
    object IncrementButtonTapped : CounterAction()
}

object CounterEnvironment

val counterReducer = Reducer<CounterState, CounterAction, CounterEnvironment> { state, action, _ ->
    when (action) {
        DecrementButtonTapped -> state.copy(count = state.count - 1) + Effects.none()
        IncrementButtonTapped -> state.copy(count = state.count + 1) + Effects.none()
    }
}