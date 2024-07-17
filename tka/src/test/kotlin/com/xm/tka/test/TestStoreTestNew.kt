package com.xm.tka.test

import com.xm.tka.Effects
import com.xm.tka.Effects.just
import com.xm.tka.Effects.none
import com.xm.tka.Reducer
import com.xm.tka.cancellable
import com.xm.tka.test.TestStoreTestNew.Action.A
import com.xm.tka.test.TestStoreTestNew.Action.B1
import com.xm.tka.test.TestStoreTestNew.Action.B2
import com.xm.tka.test.TestStoreTestNew.Action.B3
import com.xm.tka.test.TestStoreTestNew.Action.C1
import com.xm.tka.test.TestStoreTestNew.Action.C2
import com.xm.tka.test.TestStoreTestNew.Action.C3
import com.xm.tka.test.TestStoreTestNew.Action.D
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.schedulers.TestScheduler
import java.util.concurrent.TimeUnit
import org.junit.Test

class TestStoreTestNew {

    private sealed interface Action {
        object A : Action
        object B1 : Action
        object B2 : Action
        object B3 : Action
        object C1 : Action
        object C2 : Action
        object C3 : Action
        object D : Action
    }

    private object State

    private val testScheduler = TestScheduler()

    @Test
    fun testEffectConcatenation() {
        val reducer = Reducer<State, Action, Scheduler> { state, action, scheduler ->
            state + when (action) {
                is A -> Effects.merge(
                    Effects.concatenate(just(B1), just(C1))
                        .delay(1, TimeUnit.SECONDS, scheduler),
                    none<Action>()
                        .cancellable(1)
                )
                is B1 -> Effects.concatenate(just(B2), just(B3))
                is C1 -> Effects.concatenate(just(C2), just(C3))
                is B2, B3, C2, C3 -> none()
                is D -> Effects.cancel(1)
            }
        }

        TestStore(initialState = State, reducer = reducer, environment = testScheduler).assert {
            send(A)
            testScheduler.advanceTimeBy(1, TimeUnit.SECONDS)
            receive(B1)
            receive(B2)
            receive(B3)
            receive(C1)
            receive(C2)
            receive(C3)
            send(D)
        }
    }
}
