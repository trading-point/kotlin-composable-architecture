package com.xm.tka

import com.xm.tka.Effects.fireAndForget
import com.xm.tka.Effects.none
import com.xm.tka.Reducer.Companion.combine
import com.xm.tka.ReducerTest.Action.Increment
import com.xm.tka.test.TestStore
import com.xm.tka.test.TestStore.Step.Companion.`do`
import com.xm.tka.test.TestStore.Step.Companion.send
import io.reactivex.Scheduler
import io.reactivex.schedulers.TestScheduler
import java.util.concurrent.TimeUnit.SECONDS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReducerTest {

    sealed class Action {
        object Increment : Action()
    }

    @Test
    fun testCallableAsFunction() {
        val reducer = Reducer<Int, Unit, Unit> { state, _, _ ->
            (state + 1) + none()
        }

        val (state, _) = reducer.reduce(0, Unit, Unit)

        assertEquals(1, state)
    }

    @Test
    fun testCombine_EffectsAreMerged() {
        var fastValue: Int? = null
        val fastReducer = Reducer<Int, Action, Scheduler> { state, _, environment ->
            (state + 1) + fireAndForget<Action> { fastValue = 42 }
                .delay(1, SECONDS, environment)
        }

        var slowValue: Int? = null
        val slowReducer = Reducer<Int, Action, Scheduler> { state, _, environment ->
            (state + 1) + fireAndForget<Action> { slowValue = 1729 }
                .delay(2, SECONDS, environment)
        }

        val scheduler = TestScheduler()
        val store = TestStore(
            0,
            combine(fastReducer, slowReducer),
            scheduler
        )

        store.assert(
            send(Increment) { 2 },
            // Waiting a second causes the fast effect to fire.
            `do` { scheduler.advanceTimeBy(1, SECONDS) },
            `do` { assertEquals(42, fastValue) },
            // Waiting one more second causes the slow effect to fire. This proves that the effects
            // are merged together, as opposed to concatenated.
            `do` { scheduler.advanceTimeBy(1, SECONDS) },
            `do` { assertEquals(1729, slowValue) }
        )
    }

    @Test
    fun testCombine() {
        var childEffectExecuted = false
        val childReducer = Reducer<Int, Action, Unit> { state, _, _ ->
            (state + 1) + fireAndForget { childEffectExecuted = true }
        }

        var mainEffectExecuted = false
        val mainReducer = Reducer<Int, Action, Unit> { state, _, _ ->
            (state + 1) + fireAndForget { mainEffectExecuted = true }
        }.combinedWith(childReducer)

        val store = TestStore(
            0,
            mainReducer,
            Unit
        )

        store.assert(
            send(Increment) { 2 }
        )

        assertTrue(childEffectExecuted)
        assertTrue(mainEffectExecuted)
    }
}
