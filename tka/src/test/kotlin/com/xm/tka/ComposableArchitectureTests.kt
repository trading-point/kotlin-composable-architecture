package com.xm.tka

import com.xm.tka.ComposableArchitectureTests.Action.End
import com.xm.tka.ComposableArchitectureTests.Action.Incr
import com.xm.tka.ComposableArchitectureTests.Action.Start
import com.xm.tka.ComposableArchitectureTests.CounterAction.IncrAndSquareLater
import com.xm.tka.ComposableArchitectureTests.CounterAction.IncrNow
import com.xm.tka.ComposableArchitectureTests.CounterAction.SquareNow
import com.xm.tka.Effects.fireAndForget
import com.xm.tka.Effects.just
import com.xm.tka.Effects.merge
import com.xm.tka.Effects.none
import com.xm.tka.test.TestStore
import com.xm.tka.test.TestStore.Step.Companion.`do`
import com.xm.tka.test.TestStore.Step.Companion.receive
import com.xm.tka.test.TestStore.Step.Companion.send
import io.reactivex.Scheduler
import io.reactivex.schedulers.TestScheduler
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.TimeUnit.NANOSECONDS
import java.util.concurrent.TimeUnit.SECONDS
import org.junit.Assert.assertEquals
import org.junit.Test

class ComposableArchitectureTests {

    sealed class CounterAction {
        object IncrAndSquareLater : CounterAction()
        object IncrNow : CounterAction()
        object SquareNow : CounterAction()
    }

    @Test
    fun testScheduling() {
        val counterReducer = Reducer<Int, CounterAction, Scheduler> { state, action, scheduler ->
            when (action) {
                IncrAndSquareLater -> state + merge(
                    just<CounterAction>(IncrNow).delay(2, SECONDS, scheduler),
                    just<CounterAction>(SquareNow).delay(1, SECONDS, scheduler),
                    just<CounterAction>(SquareNow).delay(2, SECONDS, scheduler)
                )
                IncrNow -> (state + 1) + none()
                SquareNow -> (state * state) + none()
            }
        }

        val scheduler = TestScheduler()
        val store = TestStore(0, counterReducer, scheduler)

        store.assert(
            send(IncrAndSquareLater),
            `do` { scheduler.advanceTimeBy(1, SECONDS) },
            receive(SquareNow) { 4 },
            `do` { scheduler.advanceTimeBy(1, SECONDS) },
            receive(IncrNow) { 5 },
            receive(SquareNow) { 25 }
        )

        store.assert(
            send(IncrAndSquareLater),
            `do` { scheduler.advanceTimeBy(2, SECONDS) },
            receive(SquareNow) { 625 },
            receive(IncrNow) { 626 },
            receive(SquareNow) { 391876 }
        )
    }

    @Test
    fun testSimultaneousWorkOrdering() {
        val testScheduler = TestScheduler(1, NANOSECONDS)

        val values = mutableListOf<Int>()
        testScheduler.schedulePeriodicallyDirect(
            { values.add(1) },
            testScheduler.now(SECONDS),
            1,
            SECONDS
        )
        testScheduler.schedulePeriodicallyDirect(
            { values.add(42) },
            testScheduler.now(SECONDS),
            2,
            SECONDS
        )

        assertEquals(emptyList<Int>(), values)
        testScheduler.triggerActions()
        assertEquals(listOf(1, 42), values)
        testScheduler.advanceTimeBy(2, SECONDS)
        assertEquals(listOf(1, 42, 1, 42, 1), values)
    }

    sealed class Action {
        object End : Action()
        object Incr : Action()
        object Start : Action()
    }

    interface Environment {
        val startEffect: Effect<Unit>
        val stopEffect: Effect<Action>
    }

    @Test
    fun testLongLivingEffects() {
        val reducer = Reducer<Int, Action, Environment> { state, action, environment ->
            when (action) {
                End -> state + environment.stopEffect
                Incr -> (state + 1) + none()
                Start -> state + environment.startEffect.map<Action> { Incr }
            }
        }

        val subject = PublishSubject.create<Unit>()

        val store = TestStore(
            0,
            reducer,
            object : Environment {
                override val startEffect: Effect<Unit>
                    get() = subject
                override val stopEffect: Effect<Action>
                    get() = fireAndForget { subject.onComplete() }
            }
        )

        store.assert(
            send(Start),
            send(Incr) { 1 },
            `do` { subject.onNext(Unit) },
            receive(Incr) { 2 },
            send(End)
        )
    }
}
