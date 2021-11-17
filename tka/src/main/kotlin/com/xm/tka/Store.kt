@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package com.xm.tka

import com.xm.tka.Effects.none
import com.xm.tka.Optional.Companion.toOptional
import com.xm.tka.test.Printer
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject
import java.util.concurrent.LinkedBlockingDeque

/**
 * A store represents the runtime that powers the application. It is the object that you will pass
 * around to views that need to interact with the application.
 *
 * You will typically construct a single one of these at the root of your application, and then use
 * the `scope` method to derive more focused stores that can be passed to subviews.
 *
 * Source: https://github.com/pointfreeco/swift-composable-architecture/blob/main/Sources/ComposableArchitecture/Store.swift
 */
class Store<STATE : Any, ACTION : Any> private constructor(
    initialState: STATE,
    private val reducer: (STATE, ACTION) -> Reduced<STATE, ACTION>,
    private val printer: Printer,
    parentStream: Observable<STATE> = Observable.never()
) {

    private val _state: BehaviorSubject<STATE> = BehaviorSubject.createDefault(initialState)
        .apply {
            parentStream
                // don't delegate onError events
                .doOnError { printer.print("TKA: Store: parentStream", it) }
                .onErrorResumeNext(Observable.never())
                // don't delegate onComplete events
                .concatWith(Observable.never())
                // forward events to subject
                .subscribe(this)
        }
    internal val state: Observable<STATE> = _state.hide()
    internal val currentState: STATE
        get() = requireNotNull(_state.value)

    private val synchronousActionsToSend: LinkedBlockingDeque<ACTION> = LinkedBlockingDeque()
    private val bufferedActions: LinkedBlockingDeque<ACTION> = LinkedBlockingDeque()
    private var isSending: Boolean = false

    internal val effectDisposables: MutableMap<Long, Disposable> = mutableMapOf()
    private var id: Long = 0

    fun send(action: ACTION) {
        if (!isSending) {
            synchronousActionsToSend.add(action)
        } else {
            bufferedActions.add(action)
            return
        }

        while (synchronousActionsToSend.any() || bufferedActions.any()) {
            with(synchronousActionsToSend.poll() ?: bufferedActions.poll() ?: continue) {

                isSending = true
                val (newState, effect) = try {
                    reducer(currentState, this)
                } finally {
                    isSending = false
                }
                _state.onNext(newState)

                var didComplete = false
                val id = id.inc()

                var isProcessingEffects = true
                val effectDisposable = effect.subscribe(
                    {
                        if (isProcessingEffects) synchronousActionsToSend.add(it)
                        else send(it)
                    },
                    {
                        printer.print("TKA: Store: effectDisposable", it)
                        didComplete = true
                        effectDisposables.remove(id)
                    },
                    {
                        didComplete = true
                        effectDisposables.remove(id)
                    }
                )
                isProcessingEffects = false

                if (didComplete.not()) {
                    effectDisposables[id] = effectDisposable
                } else {
                    effectDisposable.dispose()
                }
            }
        }
    }

    fun dispose() {
        effectDisposables
            .onEach { (_, disposable) -> disposable.dispose() }
            .clear()
    }

    //region Scopes
    /**
     * Scopes the store to one that exposes local state and actions.
     *
     * This can be useful for deriving new stores to hand to child views in an application.
     *
     * @param toLocalState: An optic that transforms [STATE] into [LOCAL_STATE].
     * @param fromLocalAction: An optic that transforms [LOCAL_ACTION] into [ACTION].
     * @return A new store with its domain (state and action) transformed.
     */
    fun <LOCAL_STATE : Any, LOCAL_ACTION : Any> scope(
        toLocalState: Getter<STATE, LOCAL_STATE>,
        fromLocalAction: Getter<LOCAL_ACTION, ACTION>
    ): Store<LOCAL_STATE, LOCAL_ACTION> =
        Store(
            initialState = toLocalState(currentState),
            reducer = { _, action ->
                // force store to internally mutate it's value
                fromLocalAction(action).let(::send)
                // get a new local value
                Reduced(toLocalState(currentState), none())
            },
            parentStream = _state.map { toLocalState(it) },
            printer = printer
        )

    /**
     * Scopes the store to one that exposes local state.
     *
     * @param toLocalState: An optic that transforms [STATE] into [LOCAL_STATE].
     * @return A new store with its domain (state and action) transformed.
     */
    fun <LOCAL_STATE : Any> scope(
        toLocalState: Getter<STATE, LOCAL_STATE>
    ): Store<LOCAL_STATE, ACTION> = scope(
        toLocalState,
        { it }
    )

    /**
     * Scopes the store to an [Observable] of stores of more local state and local actions
     *
     * @param toLocalState: An option that transforms a [Observable] of [STATE]` into a [Observable] of [LOCAL_STATE]
     * @param fromLocalAction: An optic that transforms [LOCAL_ACTION] into [ACTION]
     * @return A [Observable] of stores with its domain (state and action) transformed
     */
    fun <LOCAL_STATE : Any, LOCAL_ACTION : Any> scopes(
        toLocalState: Getter<Observable<STATE>, Observable<LOCAL_STATE>>,
        fromLocalAction: Getter<LOCAL_ACTION, ACTION>
    ): Observable<Store<LOCAL_STATE, LOCAL_ACTION>> {

        val extractLocalState: (STATE) -> LOCAL_STATE? = {
            toLocalState(Observable.just(it)).blockingFirst(null)
        }

        return toLocalState(_state)
            .map { localState ->
                Store(
                    initialState = localState,
                    reducer = { state, action ->
                        // force store to internally mutate it's value
                        fromLocalAction(action).let(::send)
                        // get a new local value
                        Reduced(
                            extractLocalState(this@Store.currentState) ?: state,
                            none()
                        )
                    },
                    parentStream = _state.map { extractLocalState(it).toOptional() }
                        .filter { it.isPresent }
                        .map { it.orNull()!! },
                    printer = printer
                )
            }
    }

    /**
     * Scopes the store to an [Observable] of stores of more local state and local actions
     *
     * @param toLocalState: An optic that transforms a [Observable] of [STATE] into a publisher of [LOCAL_STATE]
     * @return A [Observable] of stores with its domain (state and action) transformed
     */
    fun <LOCAL_STATE : Any> scopes(
        toLocalState: Getter<Observable<STATE>, Observable<LOCAL_STATE>>
    ): Observable<Store<LOCAL_STATE, ACTION>> = scopes(toLocalState, { it })

    /**
     * Scopes the store to one that exposes local optional state and actions.
     *
     * This can be useful for deriving new stores to hand to child views in an application.
     *
     * @param toLocalState: An optic that transforms [STATE] into an optional [LOCAL_STATE].
     * @param fromLocalAction: An optic that transforms [LOCAL_ACTION] into [ACTION].
     * @return A new store with its domain (state and action) transformed.
     */
    fun <LOCAL_STATE : Any, LOCAL_ACTION : Any> optional(
        toLocalState: Getter<STATE, LOCAL_STATE?>,
        fromLocalAction: Getter<LOCAL_ACTION, ACTION>
    ): Store<Optional<LOCAL_STATE>, LOCAL_ACTION> = scope(
        { toLocalState(it).toOptional() },
        fromLocalAction
    )

    /**
     * Scopes the store to one that exposes optional local state.
     *
     * @param toLocalState: An optic that transforms [STATE] into an optional [LOCAL_STATE].
     * @return A new store with its domain (state and action) transformed.
     */
    fun <LOCAL_STATE : Any> optional(
        toLocalState: Getter<STATE, LOCAL_STATE?>
    ): Store<Optional<LOCAL_STATE>, ACTION> = scope(
        { toLocalState(it).toOptional() },
        { it }
    )

    /**
     * Returns a "stateless" store by erasing state to [Unit].
     */
    val stateless: Store<Unit, ACTION>
        get() = scope(
            toLocalState = { }
        )

    /**
     * Returns an "actionless" store by erasing action to [Nothing].
     */
    val actionless: Store<STATE, Nothing>
        get() = scope(
            toLocalState = { it },
            fromLocalAction = { it }
        )
    //endregion

    companion object {

        /**
         * Initializes a store from an initial state, a reducer, and an environment.
         *
         * @param initialState: The state to start the application in.
         * @param reducer: The reducer that powers the business logic of the application.
         * @param environment: The environment of dependencies for the application.
         */
        operator fun <STATE : Any, ACTION : Any, ENVIRONMENT : Any> invoke(
            initialState: STATE,
            reducer: Reducer<STATE, ACTION, ENVIRONMENT>,
            environment: ENVIRONMENT,
            printer: Printer = Printer()
        ): Store<STATE, ACTION> = Store(
            initialState,
            { state, action -> reducer.reduce(state, action, environment) },
            printer = printer
        )
    }
}
