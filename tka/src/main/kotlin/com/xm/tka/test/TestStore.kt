@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package com.xm.tka.test

import com.xm.tka.Getter
import com.xm.tka.Reducer
import com.xm.tka.Store
import java.util.LinkedList

/**
 * A testable runtime for a reducer.
 *
 * This object aids in writing expressive and exhaustive tests for features built in the
 * Composable Architecture. It allows you to send a sequence of actions to the store, and each
 * step of the way you must assert exactly how state changed, and how effect emissions were fed
 * back into the system.
 *
 * There are multiple ways the test store forces you to exhaustively assert on how your feature
 * behaves:
 * * After each action is sent you must describe precisely how the state changed from before the
 *   action was sent to after it was sent.
 *   If even the smallest piece of data differs the test will fail. This guarantees that you are
 *   proving you know precisely how the state of the system changes.
 * * Sending an action can sometimes cause an effect to be executed, and if that effect emits an
 *   action that is fed back into the system, you **must** explicitly assert that you expect to
 *   receive that action from the effect, _and_ you must assert how state changed as a result.
 *   If you try to send another action before you have handled all effect emissions the assertion
 *   will fail. This guarantees that you do not accidentally forget about an effect emission, and
 *   that the sequence of steps you are describing will mimic how the application behaves in
 *   reality.
 * * All effects must complete by the time the assertion has finished running the steps you
 *   specify.
 *   If at the end of the assertion there is still an in-flight effect running, the assertion
 *   will fail. This helps exhaustively prove that you know what effects are in flight and forces
 *   you to prove that effects will not cause any future changes to your state.
 *
 *   Source: https://github.com/pointfreeco/swift-composable-architecture/blob/main/Sources/ComposableArchitecture/TestSupport/TestStore.swift
 */
class TestStore<STATE : Any, LOCAL_STATE : Any, ACTION : Any, LOCAL_ACTION : Any, ENVIRONMENT : Any> private constructor(
    initialState: STATE,
    private val reducer: Reducer<STATE, ACTION, ENVIRONMENT>,
    private val environment: ENVIRONMENT,
    private val toLocalState: Getter<STATE, LOCAL_STATE>,
    private val fromLocalAction: Getter<LOCAL_ACTION, ACTION>,
    private val printer: Printer
) {

    private sealed interface TestAction<out ACTION : Any, out LOCAL_ACTION : Any> {
        data class Send<LOCAL_ACTION : Any>(val localAction: LOCAL_ACTION) :
            TestAction<Nothing, LOCAL_ACTION>

        data class Receive<ACTION : Any>(val action: ACTION) : TestAction<ACTION, Nothing>
    }

    private var snapshotState: STATE = initialState
    private val receivedActions = LinkedList<Pair<ACTION, STATE>>()
    private var id: ULong = ULong.MIN_VALUE
    private val longLivingEffects = mutableListOf<ULong>()

    private val store = Store<STATE, TestAction<ACTION, LOCAL_ACTION>, Unit>(
        initialState,
        Reducer { state, testAction, _ ->
            val (newState, newEffect) = when (testAction) {
                is TestAction.Send -> reducer.reduce(
                    state,
                    fromLocalAction(testAction.localAction),
                    environment
                ).also { (newState, _) -> snapshotState = newState }
                is TestAction.Receive -> reducer.reduce(
                    state,
                    testAction.action,
                    environment
                ).also { (newState, _) -> receivedActions.add(testAction.action to newState) }
            }
            val effect = id.inc()
            newState + newEffect
                .doOnSubscribe { longLivingEffects.add(effect) }
                .doOnTerminate { longLivingEffects.remove(effect) }
                .doOnDispose { longLivingEffects.remove(effect) }
                .map { TestAction.Receive(it) }
        },
        Unit
    )

    /**
     * Asserts against a script of actions.
     */
    @Deprecated("`Step`s are deprecated, use distinct send/receive methods", ReplaceWith("assert { steps }"))
    fun assert(vararg steps: Step<STATE, LOCAL_STATE, ACTION, LOCAL_ACTION, ENVIRONMENT>) {
        steps.forEachIndexed { index, step ->
            when (val type = step.type) {
                is Step.Type.Send -> {
                    send(index + 1 to type, type.localAction, type.update)
                }
                is Step.Type.Receive -> {
                    receive(index + 1 to type, type.expectedAction, type.update)
                }
                is Step.Type.Environment -> {
                    type.work(environment)
                }
            }
        }
        completed()
    }

    /**
     * Asserts against a script of actions and verify
     */
    fun assert(steps: TestStore<STATE, LOCAL_STATE, ACTION, LOCAL_ACTION, ENVIRONMENT>.() -> Unit) {
        apply(steps).completed()
    }

    /**
     * Assert a sent action
     */
    fun send(
        action: LOCAL_ACTION,
        update: (LOCAL_STATE) -> LOCAL_STATE = { it }
    ) = send(null, action, update)

    private fun send(
        step: Pair<Int, Step.Type.Send<LOCAL_STATE, LOCAL_ACTION>>? = null,
        action: LOCAL_ACTION,
        update: (LOCAL_STATE) -> LOCAL_STATE
    ) {
        assertActions()
        val expectedState = toLocalState(snapshotState)
        store
            .scope<LOCAL_STATE, LOCAL_ACTION>(
                toLocalState = { toLocalState(it) },
                fromLocalAction = { TestAction.Send(it) }
            )
            .send(action)
        assertState(step, update(expectedState), toLocalState(snapshotState))
    }

    /**
     * Assert a received action
     */
    fun receive(
        expectedAction: ACTION,
        update: (LOCAL_STATE) -> LOCAL_STATE = { it }
    ) = receive(null, expectedAction, update)

    private fun receive(
        step: Pair<Int, Step.Type.Receive<LOCAL_STATE, ACTION>>? = null,
        expectedAction: ACTION,
        update: (LOCAL_STATE) -> LOCAL_STATE
    ) {
        assert(receivedActions.any()) {
            """
Expected to receive ${expectedAction}, but received none.
                        """
        }
        val (receivedAction, state) = receivedActions.remove()
        assert(expectedAction == receivedAction) {
            """
Received unexpected action
    Expected: $expectedAction
    Actual:   $receivedAction.
                        """
        }
        val expectedState = toLocalState(snapshotState)
        val actualState = toLocalState(state)
        assertState(step, update(expectedState), actualState)
        snapshotState = state
    }

    /**
     * Assert that all store operations are complete
     */
    fun completed() {
        assert(receivedActions.none()) {
            """
Received ${receivedActions.size} unexpected action${if (receivedActions.none()) "" else "s"}.
    Unhandled actions: $receivedActions
            """
        }

        assert(longLivingEffects.none()) {
            """
Some effects are still running. All effects must complete by the end of the assertion.
This can happen for a few reasons:
* If you are using a scheduler in your effect, then make sure that you wait enough time 
  for the effect to finish. If you are using a test scheduler, then make sure you advance
  the scheduler so that the effects complete.
* If you are using long-living effects (for example timers, notifications, etc.), then 
  ensure those effects are completed by returning an `Effect.cancel` effect from a 
  particular action in your reducer, and sending that action in the test. 
            """
        }
    }

    private fun assertState(
        step: Pair<Int, Step.Type<STATE, LOCAL_STATE, ACTION, LOCAL_ACTION, ENVIRONMENT>>? = null,
        expected: LOCAL_STATE,
        actual: LOCAL_STATE
    ) {
        assert(expected == actual) {
            """
State change ${step?.let { (index, type) -> "on step $index: $type" } ?: ""} does not match expectation
    Expected: $expected
    Actual:   $actual
            """.trim()
        }
    }

    private fun assertActions(
        index: Int? = null
    ) {
        assert(receivedActions.none()) {
            """
Must handle ${receivedActions.size} received action${if (receivedActions.none()) "" else "s"}
before sending an action ${index?.let { "on step $index" } ?: ""}
Unhandled actions: $receivedActions
                        """
        }
    }

    /**
     * A single step of a [TestStore] assertion.
     */
    @Deprecated("`Step`s are deprecated, use distinct send/receive methods")
    class Step<STATE, LOCAL_STATE, ACTION, LOCAL_ACTION, ENVIRONMENT> private constructor(
        internal val type: Type<STATE, LOCAL_STATE, ACTION, LOCAL_ACTION, ENVIRONMENT>
    ) {

        internal sealed class Type<out STATE, out LOCAL_STATE, out ACTION, out LOCAL_ACTION, out ENVIRONMENT> {
            data class Send<LOCAL_STATE, LOCAL_ACTION>(
                val localAction: LOCAL_ACTION,
                val update: (LOCAL_STATE) -> LOCAL_STATE
            ) : Type<Nothing, LOCAL_STATE, Nothing, LOCAL_ACTION, Nothing>()

            data class Receive<LOCAL_STATE, ACTION>(
                val expectedAction: ACTION,
                val update: (LOCAL_STATE) -> LOCAL_STATE
            ) : Type<Nothing, LOCAL_STATE, ACTION, Nothing, Nothing>()

            data class Environment<ENVIRONMENT>(
                val work: (ENVIRONMENT) -> ENVIRONMENT
            ) : Type<Nothing, Nothing, Nothing, Nothing, ENVIRONMENT>()
        }

        companion object {
            /**
             * A step that describes an action sent to a store and asserts against how the store's state
             * is expected to change.
             *
             * @param action: An action to send to the test store.
             * @paramupdate: A function that describes how the test store's state is expected to change.
             * @return A step that describes an action sent to a store and asserts against how the
             * store's state is expected to change.
             */
            @Deprecated("`Step`s are deprecated, use distinct send/receive methods")
            fun <STATE, LOCAL_STATE, ACTION, LOCAL_ACTION, ENVIRONMENT> send(
                action: LOCAL_ACTION,
                update: (LOCAL_STATE) -> LOCAL_STATE = { it }
            ): Step<STATE, LOCAL_STATE, ACTION, LOCAL_ACTION, ENVIRONMENT> =
                Step(Type.Send(action, update))

            /**
             * A step that describes an action received by an effect and asserts against how the store's
             * state is expected to change.
             *
             * @param action: An action the test store should receive by evaluating an effect.
             * @param update: A function that describes how the test store's state is expected to change.
             * @return A step that describes an action received by an effect and asserts against how
             * the store's state is expected to change.
             */
            @Deprecated("`Step`s are deprecated, use distinct send/receive methods")
            fun <STATE, LOCAL_STATE, ACTION, LOCAL_ACTION, ENVIRONMENT> receive(
                action: ACTION,
                update: (LOCAL_STATE) -> LOCAL_STATE = { it }
            ): Step<STATE, LOCAL_STATE, ACTION, LOCAL_ACTION, ENVIRONMENT> =
                Step(Type.Receive(action, update))

            /**
             * A step that captures some work to be done between assertions
             *
             * @param work: A function that is called between steps.
             * @return A step that captures some work to be done between assertions.
             */
            @Deprecated("`Step`s are deprecated, use distinct send/receive methods")
            fun <STATE, LOCAL_STATE, ACTION, LOCAL_ACTION, ENVIRONMENT> `do`(
                work: () -> Unit
            ): Step<STATE, LOCAL_STATE, ACTION, LOCAL_ACTION, ENVIRONMENT> =
                Step(Type.Environment { work(); it })
        }
    }

    companion object {
        /**
         * Initializes a test store from an initial state, a reducer, and an initial environment.
         *
         * @param initialState: The state to start the test from.
         * @param reducer: A reducer.
         * @param environment: The environment to start the test from.
         */
        operator fun <STATE : Any, ACTION : Any, ENVIRONMENT : Any> invoke(
            initialState: STATE,
            reducer: Reducer<STATE, ACTION, ENVIRONMENT>,
            environment: ENVIRONMENT,
            printer: Printer = Printer()
        ): TestStore<STATE, STATE, ACTION, ACTION, ENVIRONMENT> = TestStore(
            initialState,
            reducer,
            environment,
            { it },
            { it },
            printer
        )
    }
}
