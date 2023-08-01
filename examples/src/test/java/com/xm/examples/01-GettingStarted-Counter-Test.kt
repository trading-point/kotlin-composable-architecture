package com.xm.examples

import com.xm.examples.cases.CounterAction
import com.xm.examples.cases.CounterEnvironment
import com.xm.examples.cases.CounterState
import com.xm.examples.cases.counterReducer
import com.xm.tka.test.TestStore
import org.junit.Test

class GettingStartedCounterTest {

    @Test
    fun testCounter() {
        val state = CounterState(count = 0)

        TestStore(state, counterReducer, CounterEnvironment).assert {
            send(CounterAction.IncrementButtonTapped) {
                it.copy(count = 1)
            }
            send(CounterAction.IncrementButtonTapped) {
                it.copy(count = 2)
            }
            send(CounterAction.DecrementButtonTapped) {
                it.copy(count = 1)
            }
            send(CounterAction.DecrementButtonTapped) {
                it.copy(count = 0)
            }
        }
    }
}