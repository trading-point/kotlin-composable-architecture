@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package com.xm.tka

import com.xm.tka.Effects.none
import com.xm.tka.Reducer.Companion.combine
import io.reactivex.Observable

/**
 * A reducer describes how to evolve the current state of an application to the next state, given
 * an action, and describes what `Effect`s should be executed later by the store, if any.
 *
 * Reducers have 3 generics:
 * * [STATE]: A type that holds the current state of the application
 * * [ACTION]: A type that holds all possible actions that cause the state of the application to
 *   change.
 * * [ENVIRONMENT]: A type that holds all dependencies needed in order to produce `Effect`s, such
 *   as API clients, analytics clients, random number generators, etc.
 *
 * - Note: The thread on which effects output is important. An effect's output is immediately sent
 * back into the store, and `Store` is not thread safe. This means all effects must receive
 * values on the same thread, **and** if the `Store` is being used to drive UI then all output
 * must be on the main thread. You can use the `Publisher` method `receive(on:)` for make the
 * effect output its values on the thread of your choice.
 *
 * Source: https://github.com/pointfreeco/swift-composable-architecture/blob/main/Sources/ComposableArchitecture/Reducer.swift
 */
interface Reducer<STATE, ACTION, ENVIRONMENT> {

    fun reduce(state: STATE, action: ACTION, environment: ENVIRONMENT): Reduced<STATE, ACTION>

    /**
     * Transforms a reducer that works on local state, action and environment into one that works on
     * global state, action and environment. It accomplishes this by providing 3 transformations to
     * the method:
     *
     * * A [StateLens] that can get/set a piece of local state from the global state.
     * * An [ActionPrism] that can extract/embed a local action into a global action.
     * * A [Getter] that can transform the global environment into a local environment.
     *
     * This operation is important for breaking down large reducers into small ones. When used with
     * the `combine` operator you can define many reducers that work on small pieces of domain, and
     * then _pull them back_ and _combine_ them into one big reducer that works on a large domain.
     *
     * @param toLocalState: A [StateLens] that can get/set [STATE] inside [GLOBAL_STATE].
     * @param toLocalAction: An [ActionPrism] that can extract/embed [ACTION] from GLOBAL_ACTION.
     * @param toLocalEnvironment: A [Getter] that transforms [GLOBAL_ENVIRONMENT] into [ENVIRONMENT].
     */
    fun <GLOBAL_STATE, GLOBAL_ACTION, GLOBAL_ENVIRONMENT> pullback(
        toLocalState: StateLens<GLOBAL_STATE, STATE>,
        toLocalAction: ActionPrism<GLOBAL_ACTION, ACTION>,
        toLocalEnvironment: Getter<GLOBAL_ENVIRONMENT, ENVIRONMENT>
    ): Reducer<GLOBAL_STATE, GLOBAL_ACTION, GLOBAL_ENVIRONMENT> =
        Reducer { state, action, environment ->
            // convert to global action to local action
            toLocalAction.get(action)
                // map global state to local state and reduce it
                ?.let { localAction ->
                    reduce(
                        toLocalState.get(state),
                        localAction,
                        toLocalEnvironment(environment)
                    )
                }
                // update global state with the new local state and return the updated state
                ?.let { (localState, localEffect) ->
                    // map local effect to global effect
                    localEffect
                        .map { localAction -> toLocalAction.reverseGet(localAction) }
                        // return the update global state along with the global actions
                        .let { effect -> toLocalState.set(state, localState) + effect }
                }
                ?: state + none()
        }

    companion object {

        /**
         * Initializes a reducer from a simple reducer function signature.
         *
         * @param reduce : A function signature that takes state, action and environment.
         */
        operator fun <STATE, ACTION, ENVIRONMENT> invoke(
            reduce: Reduce<STATE, ACTION, ENVIRONMENT>
        ): Reducer<STATE, ACTION, ENVIRONMENT> = object : Reducer<STATE, ACTION, ENVIRONMENT> {

            private val context: ReduceContext<STATE, ACTION, ENVIRONMENT> = ReduceContext()

            override fun reduce(
                state: STATE,
                action: ACTION,
                environment: ENVIRONMENT
            ): Reduced<STATE, ACTION> = reduce(context, state, action, environment)
        }

        /**
         * A reducer that performs no state mutations and returns no effects.
         */
        fun <STATE, ACTION, ENVIRONMENT> empty(): Reducer<STATE, ACTION, ENVIRONMENT> =
            Reducer { state, _, _ -> state + none() }

        /**
         * Combines many reducers into a single one by running each one on the state, and merging
         * all of the effects.
         *
         * It is important to note that the order of combining reducers matter. Combining `reducerA` with
         * `reducerB` is not necessarily the same as combining `reducerB` with `reducerA`.
         *
         * This can become an issue when working with reducers that have overlapping domains. For
         * example, if `reducerA` embeds the domain of `reducerB` and reacts to its actions or modifies
         * its state, it can make a difference if `reducerA` chooses to modify `reducerB`'s state
         * _before_ or _after_ `reducerB` runs.
         *
         *  This is perhaps most easily seen when working with `optional` reducers, where the parent
         *  domain may listen to the child domain and `null` out its state. If the parent reducer runs
         *  before the child reducer, then the child reducer will not be able to react to its own action.
         *
         *  Similar can be said for a `forEach` reducer. If the parent domain modifies the child
         *  collection by moving, removing, or modifying an element before the `forEach` reducer runs, the
         *  `forEach` reducer may perform its action against the wrong element, an element that no longer
         *  exists, or an element in an unexpected state.
         *
         *  Running a parent reducer before a child reducer can be considered an application logic
         *  error, and can produce assertion failures. So you should almost always combine reducers in
         *  order from child to parent domain.
         *
         * @param reducers: A list of reducers.
         * @return A single [Reducer].
         */
        fun <STATE, ACTION, ENVIRONMENT> combine(
            reducers: List<Reducer<STATE, ACTION, ENVIRONMENT>>
        ): Reducer<STATE, ACTION, ENVIRONMENT> =
            Reducer { state, action, environment ->
                // apply all reducers on the state
                reducers.fold(state + none(), { (state, effect), reducer ->
                    reducer.reduce(state, action, environment)
                        // send the final state with the accumulated side-effects
                        .let { (newState, newEffect) -> newState + (effect + newEffect) }
                })
            }

        /**
         * Combines many reducers into a single one by running each one on the state, and merging
         * all of the effects.
         *
         * @see combine
         *
         * @param reducers An array of reducers.
         * @return A single [Reducer].
         */
        fun <STATE, ACTION, ENVIRONMENT> combine(
            vararg reducers: Reducer<STATE, ACTION, ENVIRONMENT>
        ): Reducer<STATE, ACTION, ENVIRONMENT> = combine(reducers.toList())
    }
}

/**
 * Helper context to attach extensions to generic types to be used with the reduce function
 */
interface ReduceContext<STATE, ACTION, ENVIRONMENT> {

    /**
     * Combines a state and an effect into a [Reduced] result
     */
    operator fun STATE.plus(effect: Effect<ACTION>): Reduced<STATE, ACTION> =
        Reduced(this, effect)

    /**
     * Merges effects into a new one
     */
    operator fun <ACTION> Effect<ACTION>.plus(effect: Effect<ACTION>): Effect<ACTION> =
        Observable.merge(this, effect)

    companion object {

        /**
         * [ReduceContext] constructor
         */
        operator fun <STATE, ACTION, ENVIRONMENT> invoke(): ReduceContext<STATE, ACTION, ENVIRONMENT> =
            object : ReduceContext<STATE, ACTION, ENVIRONMENT> {}
    }
}

/**
 * A function that accepts a State and an Action and produces a new state
 */
typealias Reduce<STATE, ACTION, ENVIRONMENT> =
    ReduceContext<STATE, ACTION, ENVIRONMENT>.(STATE, ACTION, ENVIRONMENT) -> Reduced<STATE, ACTION>

/**
 * The Result of the state reduction along with its side-effects
 */
typealias Reduced<STATE, ACTION> = Pair<STATE, Effect<ACTION>>

/**
 * Combines a reducers into a single one by running each one on the state, and merging
 * all of the effects.
 *
 * @see combine
 *
 * @param reducer Another reducer
 * @return A single [Reducer].
 */
fun <STATE, ACTION, ENVIRONMENT> Reducer<STATE, ACTION, ENVIRONMENT>.combinedWith(
    reducer: Reducer<STATE, ACTION, ENVIRONMENT>
): Reducer<STATE, ACTION, ENVIRONMENT> = combine(this, reducer)

/**
 * Transforms a reducer that works on non-optional state into one that works on optional state
 * by only running the non-optional reducer when state is non-null.
 */
fun <STATE, ACTION, ENVIRONMENT> Reducer<STATE, ACTION, ENVIRONMENT>.optional(): Reducer<STATE?, ACTION, ENVIRONMENT> =
    Reducer { state, action, environment ->
        state
            .also {
                assert(it != null) {
                    """
                    $action was received by an optional reducer when its state was "null".
                    This can happen for a few reasons:
                    * The optional reducer was combined with or run from another reducer that set 
                      $state to "null" before the optional reducer ran. Combine or run optional reducers
                      before reducers that can set their state to "null". This ensures that optional 
                      reducers can handle their actions while their state is still non-"null".
                    * An active effect emitted this action while state was "null". Make sure that effects
                      for this optional reducer are canceled when optional state is set to "null".
                    * This action was sent to the store while state was "null". Make sure that actions
                      for this reducer can only be sent to a view store when state is non-"null".
                    """
                }
            }
            ?.let { reduce(it, action, environment) }
            ?: state + none()
    }
