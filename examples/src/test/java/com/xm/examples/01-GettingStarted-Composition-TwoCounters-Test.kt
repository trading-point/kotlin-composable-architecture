package com.xm.examples

import com.xm.examples.cases.CounterAction
import com.xm.examples.cases.CounterState
import com.xm.examples.cases.TwoCounterAction
import com.xm.examples.cases.TwoCounterEnvironment
import com.xm.examples.cases.TwoCounterState
import com.xm.examples.cases.twoCountersReducer
import com.xm.tka.test.TestStore
import org.junit.Test

class GettingStartedCompositionTwoCountersTest {

    @Test
    fun testCounter() {
        TestStore(TwoCounterState(), twoCountersReducer, TwoCounterEnvironment).assert {
            // increment actions
            send(TwoCounterAction.Counter1(CounterAction.IncrementButtonTapped)) {
                it.copy(firstCounter = CounterState(1))
            }
            send(TwoCounterAction.Counter2(CounterAction.IncrementButtonTapped)) {
                it.copy(secondCounter = CounterState(1))
            }
            send(TwoCounterAction.Counter1(CounterAction.IncrementButtonTapped)) {
                it.copy(firstCounter = CounterState(2))
            }
            send(TwoCounterAction.Counter2(CounterAction.IncrementButtonTapped)) {
                it.copy(secondCounter = CounterState(2))
            }

            // decrement actions
            send(TwoCounterAction.Counter1(CounterAction.DecrementButtonTapped)) {
                it.copy(firstCounter = CounterState(1))
            }
            send(TwoCounterAction.Counter2(CounterAction.DecrementButtonTapped)) {
                it.copy(secondCounter = CounterState(1))
            }
            send(TwoCounterAction.Counter1(CounterAction.DecrementButtonTapped)) {
                it.copy(firstCounter = CounterState(0))
            }
            send(TwoCounterAction.Counter2(CounterAction.DecrementButtonTapped)) {
                it.copy(secondCounter = CounterState(0))
            }
        }
    }
}