package com.xm.tka

import com.xm.tka.Effects.fireAndForget
import com.xm.tka.Effects.just
import com.xm.tka.Effects.merge
import com.xm.tka.Effects.none
import com.xm.tka.StoreTest.Action2.End
import com.xm.tka.StoreTest.Action2.Next1
import com.xm.tka.StoreTest.Action2.Next2
import com.xm.tka.StoreTest.Action2.Tap
import com.xm.tka.ui.ViewStore
import com.xm.tka.ui.ViewStore.Companion.view
import io.reactivex.schedulers.TestScheduler
import java.util.concurrent.TimeUnit.SECONDS
import org.junit.Assert.assertEquals
import org.junit.Test

class StoreTest {

    @Test
    fun testCancellableIsRemovedOnImmediatelyCompletingEffect() {
        val reducer = Reducer<Unit, Unit, Unit> { _, _, _ -> Unit + none() }
        val store = Store(Unit, reducer, Unit)

        assertEquals(0, store.effectDisposables.size)

        store.send(Unit)

        assertEquals(0, store.effectDisposables.size)
    }

    sealed class Action {
        object Start : Action()
        object End : Action()
    }

    @Test
    fun testCancellableIsRemovedWhenEffectCompletes() {
        val scheduler = TestScheduler()
        val effect = none<Unit>().delay(1, SECONDS, scheduler)
        val reducer = Reducer<Unit, Action, Unit> { _, action, _ ->
            when (action) {
                Action.Start -> Unit + effect.map { Action.End }
                Action.End -> Unit + none()
            }
        }

        val store = Store(Unit, reducer, Unit)

        assertEquals(0, store.effectDisposables.size)

        store.send(Action.Start)

        assertEquals(1, store.effectDisposables.size)

        scheduler.advanceTimeBy(2, SECONDS)

        assertEquals(0, store.effectDisposables.size)
    }

    @Test
    fun testScopedStoreReceivesUpdatesFromParent() {
        val counterReducer = Reducer<Int, Unit, Unit> { state, _, _ ->
            (state + 1) + none()
        }
        val parentStore = Store(0, counterReducer, Unit)
        val parentViewStore = ViewStore(parentStore)
        val childStore = parentStore.scope { "$it" }

        childStore.state
            .test()
            .assertNotComplete()
            .assertNoErrors()
            .assertValue("0")
            .also { parentViewStore.send(Unit) }
            .assertNotComplete()
            .assertNoErrors()
            .assertValues("0", "1")
    }

    @Test
    fun testParentStoreReceivesUpdatesFromChild() {
        val counterReducer = Reducer<Int, Unit, Unit> { state, _, _ ->
            (state + 1) + none()
        }
        val parentStore = Store(0, counterReducer, Unit)
        val childStore = parentStore.scope { "$it" }
        val childViewStore = childStore.view()

        parentStore.state.test()
            .assertNotComplete()
            .assertNoErrors()
            .assertValue(0)
            .also { childViewStore.send(Unit) }
            .assertNotComplete()
            .assertNoErrors()
            .assertValues(0, 1)
    }

    @Test
    fun testScopeWithPublisherTransform() {
        val counterReducer = Reducer<Int, Int, Unit> { _, action, _ ->
            action + none()
        }
        val parentStore = Store(0, counterReducer, Unit)

        val outputs = mutableListOf<String>()
        parentStore
            .scopes {
                it.map { state -> "$state" }.distinctUntilChanged()
            }
            .subscribe { childStore ->
                childStore.state.subscribe { outputs.add(it) }
            }

        parentStore.send(0)
        assertEquals(listOf("0"), outputs)
        parentStore.send(0)
        assertEquals(listOf("0"), outputs)
        parentStore.send(1)
        assertEquals(listOf("0", "1"), outputs)
        parentStore.send(1)
        assertEquals(listOf("0", "1"), outputs)
        parentStore.send(2)
        assertEquals(listOf("0", "1", "2"), outputs)
    }

    @Test
    fun testScopeCallCount() {
        val counterReducer = Reducer<Int, Unit, Unit> { state, _, _ ->
            (state + 1) + none()
        }

        var numCalls1 = 0

        Store(0, counterReducer, Unit)
            .scope {
                numCalls1++
                it
            }

        assertEquals(2, numCalls1)
    }

    @Test
    fun testScopeCallCount2() {
        val counterReducer = Reducer<Int, Unit, Unit> { state, _, _ ->
            (state + 1) + none()
        }

        var numCalls1 = 0
        var numCalls2 = 0
        var numCalls3 = 0

        val store = Store(0, counterReducer, Unit)
            .scope {
                numCalls1++
                it
            }
            .scope {
                numCalls2++
                it
            }
            .scope {
                numCalls3++
                it
            }

        assertEquals(2, numCalls1)
        assertEquals(2, numCalls2)
        assertEquals(2, numCalls3)

        store.send(Unit)

        assertEquals(4, numCalls1)
        assertEquals(5, numCalls2)
        assertEquals(6, numCalls3)

        store.send(Unit)

        assertEquals(6, numCalls1)
        assertEquals(8, numCalls2)
        assertEquals(10, numCalls3)

        store.send(Unit)

        assertEquals(8, numCalls1)
        assertEquals(11, numCalls2)
        assertEquals(14, numCalls3)
    }

    sealed class Action2 {
        object Tap : Action2()
        object Next1 : Action2()
        object Next2 : Action2()
        object End : Action2()
    }

    @Test
    fun testSynchronousEffectsSentAfterSinking() {
        val values = mutableListOf<Int>()
        val counterReducer = Reducer<Unit, Action2, Unit> { state, action, _ ->
            when (action) {
                Tap -> state + merge<Action2>(
                    just(Next1),
                    just(Next2),
                    fireAndForget { values.add(1) })
                Next1 -> state + merge<Action2>(
                    just(End),
                    fireAndForget { values.add(2) }
                )
                Next2 -> state + fireAndForget { values.add(3) }
                End -> state + fireAndForget { values.add(4) }
            }
        }

        val store = Store(Unit, counterReducer, Unit)

        store.send(Tap)

        assertEquals(listOf(1, 2, 3, 4), values)
    }
}
