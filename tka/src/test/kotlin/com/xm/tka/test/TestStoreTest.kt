package com.xm.tka.test

import com.xm.tka.Effects.just
import com.xm.tka.Effects.none
import com.xm.tka.Reducer
import com.xm.tka.test.TestStore.Step.Companion.receive
import com.xm.tka.test.TestStore.Step.Companion.send
import com.xm.tka.test.TestStoreTest.Action.Change
import com.xm.tka.test.TestStoreTest.Action.Result
import org.junit.Test
import java.lang.AssertionError

private const val INITIAL_STATE = 0
private const val CHANGED_STATE = 1

class TestStoreTest {

    sealed class Action {
        object Change : Action()
        object Result : Action()
    }

    private data class State(val value: Int = INITIAL_STATE)

    @Test
    fun testSendSuccessful() {

        val store = TestStore(INITIAL_STATE, Reducer<Int, Action, Unit> { state, _, _ ->
            CHANGED_STATE + none()
        }, Unit)

        store.assert(
            send(Change) {
                CHANGED_STATE
            }
        )
    }

    @Test(expected = AssertionError::class)
    fun testSendFailAsserted() {

        val store = TestStore(INITIAL_STATE, Reducer<Int, Action, Unit> { state, _, _ ->
            CHANGED_STATE + none()
        }, Unit)

        store.assert(
            send(Change) {
                INITIAL_STATE
            }
        )
    }

    @Test
    fun testSendReceiveWithoutUpdateSuccessful() {

        val store = TestStore(Unit, Reducer<Unit, Action, Unit> { state, action, _ ->
            when (action) {
                Change -> state + just(Result)
                else -> state + none()
            }
        }, Unit)

        store.assert(
            send(Change),
            receive(Result)
        )
    }

    @Test
    fun testReceiveSuccessful() {

        val store = TestStore(State(), Reducer<State, Action, Unit> { state, action, _ ->
            when (action) {
                Change -> state + just(Result)
                Result -> state.copy(value = CHANGED_STATE) + none()
            }
        }, Unit)

        store.assert(
            send(Change),
            receive(Result) {
                it.copy(value = CHANGED_STATE)
            }
        )
    }

    @Test(expected = AssertionError::class)
    fun testReceiveStateFailAsserted() {

        val store = TestStore(State(), Reducer<State, Action, Unit> { state, action, _ ->
            when (action) {
                Change -> state + just(Result)
                Result -> state.copy(value = CHANGED_STATE) + none()
            }
        }, Unit)

        store.assert(
            send(Change),
            receive(Result) {
                it.copy(value = INITIAL_STATE)
            }
        )
    }
}