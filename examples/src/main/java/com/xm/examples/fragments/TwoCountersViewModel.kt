package com.xm.examples.fragments

import androidx.lifecycle.ViewModel
import com.xm.tka.ActionPrism
import com.xm.tka.Reducer
import com.xm.tka.StateLens
import com.xm.tka.Store
import com.xm.tka.ui.ViewStore
import com.xm.tka.ui.ViewStore.Companion.view

class TwoCountersViewModel : ViewModel() {

   private val store = Store(
        initialState = TwoCounterState(),
        reducer = twoCountersReducer,
        environment = TwoCounterEnvironment
    )

    val viewStore: ViewStore<TwoCounterState, TwoCounterAction> = store.view()

}

// Domain

data class TwoCounterState(
    val firstCounter: CounterState = CounterState(),
    val secondCounter: CounterState = CounterState()
)

sealed class TwoCounterAction {
    data class Counter1(val action: CounterAction) : TwoCounterAction()
    data class Counter2(val action: CounterAction) : TwoCounterAction()
}

object TwoCounterEnvironment

private val twoCountersReducer: Reducer<TwoCounterState, TwoCounterAction, TwoCounterEnvironment> =
    Reducer.combine(
        counterReducer.pullback(
            toLocalState = StateLens(
                get = { it.firstCounter },
                set = { state, update -> state.copy(firstCounter = update) }
            ),
            toLocalAction = ActionPrism(
                get = { (it as? TwoCounterAction.Counter1)?.action },
                reverseGet = { TwoCounterAction.Counter1(it) }
            ),
            toLocalEnvironment = { CounterEnvironment }
        ),
        counterReducer.pullback(
            toLocalState = StateLens(
                get = { it.secondCounter },
                set = { state, update -> state.copy(secondCounter = update) }
            ),
            toLocalAction = ActionPrism(
                get = { (it as? TwoCounterAction.Counter2)?.action },
                reverseGet = { TwoCounterAction.Counter2(it) }
            ),
            toLocalEnvironment = { CounterEnvironment }
        )
    )