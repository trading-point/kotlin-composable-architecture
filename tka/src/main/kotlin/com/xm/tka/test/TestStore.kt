@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package com.xm.tka.test

import com.xm.tka.Getter
import com.xm.tka.Reducer
import com.xm.tka.test.TestStore.Step.Type.Environment
import com.xm.tka.test.TestStore.Step.Type.Receive
import com.xm.tka.test.TestStore.Step.Type.Send
import io.reactivex.disposables.Disposable
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
class TestStore<STATE, LOCAL_STATE, ACTION, LOCAL_ACTION, ENVIRONMENT> private constructor(
    initialState: STATE,
    private val reducer: Reducer<STATE, ACTION, ENVIRONMENT>,
    private val environment: ENVIRONMENT,
    private val toLocalState: Getter<STATE, LOCAL_STATE>,
    private val fromLocalAction: Getter<LOCAL_ACTION, ACTION>
) {

    private var state: STATE = initialState

    /**
     * Asserts against a script of actions.
     */
    @Suppress("LongMethod")
    fun assert(vararg steps: Step<STATE, LOCAL_STATE, ACTION, LOCAL_ACTION, ENVIRONMENT>) {
        val receivedActions = LinkedList<ACTION>()

        val disposables = mutableListOf<Disposable>()

        val runReducer: (ACTION) -> Unit = { action ->
            val (newState, effect) = reducer.reduce(state, action, environment)
            state = newState
            var isComplete = false
            var disposable: Disposable? = null
            disposable = effect.subscribe(
                {
                    receivedActions.add(it)
                },
                {
                    // TODO("LOG)
                },
                {
                    isComplete = true
                    disposables.removeAll { it == disposable }
                }
            )
            if (isComplete.not()) disposables.add(disposable)
        }

        steps.forEach { step ->
            when (val type = step.type) {
                is Send -> {
                    assert(receivedActions.none()) {
                        """
Must handle ${receivedActions.size} received action${if (receivedActions.none()) "" else "S"}
before sending an action.
Unhandled actions: $receivedActions
                        """
                    }
                    runReducer(fromLocalAction(type.localAction))
                    val expectedState = toLocalState(state)
                    type.update(expectedState)
                }
                is Receive -> {
                    assert(receivedActions.any()) {
                        """
Expected to receive ${type.expectedAction}, but received none.
                        """
                    }
                    val receivedAction = receivedActions.remove()
                    assert(type.expectedAction == receivedAction) {
                        """
Received unexpected action
    Expected: ${type.expectedAction}
    Actual:   $receivedAction.
                        """
                    }
                    runReducer(receivedAction)
                    val expectedState = toLocalState(state)
                    type.update(expectedState)
                }
                is Environment -> {
                    assert(receivedActions.none()) {
                        """
Must handle ${receivedActions.size} received action${if (receivedActions.none()) "" else "s"}
before sending an action.
    Unhandled actions: $receivedActions
                        """
                    }
                    type.work(environment)
                }
            }

            val actualState = toLocalState(state)
            val expectedState = toLocalState(state)
            assert(expectedState == actualState) {
                """
State change does not match expectation
    Expected: $expectedState
    Actual:   $actualState
                """
            }
        }

        assert(receivedActions.none()) {
            """
Received ${receivedActions.size} unexpected action${if (receivedActions.none()) "" else "s"}.
    Unhandled actions: $receivedActions
            """
        }

        assert(disposables.none()) {
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

    /**
     * A single step of a [TestStore] assertion.
     */
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
            fun <STATE, LOCAL_STATE, ACTION, LOCAL_ACTION, ENVIRONMENT> send(
                action: LOCAL_ACTION,
                update: (LOCAL_STATE) -> LOCAL_STATE = { it }
            ): Step<STATE, LOCAL_STATE, ACTION, LOCAL_ACTION, ENVIRONMENT> =
                Step(Send(action, update))

            /**
             * A step that describes an action received by an effect and asserts against how the store's
             * state is expected to change.
             *
             * @param action: An action the test store should receive by evaluating an effect.
             * @param update: A function that describes how the test store's state is expected to change.
             * @return A step that describes an action received by an effect and asserts against how
             * the store's state is expected to change.
             */
            fun <STATE, LOCAL_STATE, ACTION, LOCAL_ACTION, ENVIRONMENT> receive(
                action: ACTION,
                update: (LOCAL_STATE) -> LOCAL_STATE = { it }
            ): Step<STATE, LOCAL_STATE, ACTION, LOCAL_ACTION, ENVIRONMENT> =
                Step(Receive(action, update))

            /**
             * A step that updates a test store's environment.
             *
             * @param update: A function that updates the test store's environment for subsequent
             * steps.
             * @return: A step that updates a test store's environment.
             */
            fun <STATE, LOCAL_STATE, ACTION, LOCAL_ACTION, ENVIRONMENT> environment(
                update: (ENVIRONMENT) -> ENVIRONMENT
            ): Step<STATE, LOCAL_STATE, ACTION, LOCAL_ACTION, ENVIRONMENT> =
                Step(Environment(update))

            /**
             * A step that captures some work to be done between assertions
             *
             * @param work: A function that is called between steps.
             * @return A step that captures some work to be done between assertions.
             */
            fun <STATE, LOCAL_STATE, ACTION, LOCAL_ACTION, ENVIRONMENT> `do`(
                work: () -> Unit
            ): Step<STATE, LOCAL_STATE, ACTION, LOCAL_ACTION, ENVIRONMENT> =
                Step(Environment { work(); it })
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
        operator fun <STATE, ACTION, ENVIRONMENT> invoke(
            initialState: STATE,
            reducer: Reducer<STATE, ACTION, ENVIRONMENT>,
            environment: ENVIRONMENT
        ): TestStore<STATE, STATE, ACTION, ACTION, ENVIRONMENT> = TestStore(
            initialState,
            reducer,
            environment,
            { it },
            { it }
        )
    }
}
