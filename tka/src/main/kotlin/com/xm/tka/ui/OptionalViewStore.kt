@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package com.xm.tka.ui

import com.xm.tka.Optional
import com.xm.tka.Optional.Companion.toOptional
import com.xm.tka.Store
import io.reactivex.Observable

/**
 * A `ViewStore` is an object that can observe state changes and send actions. They are most
 * commonly used in views but they can be used anywhere it makes sense to observe state
 * and send actions.
 *
 * [OptionalViewStore] is a special kind of [ViewStore] that operates on a `Store<Optional<STATE>` and emits only
 * non-optional states
 */
class OptionalViewStore<STATE : Any, ACTION : Any>(
    store: Store<Optional<STATE>, ACTION>,
    removeDuplicates: (STATE, STATE) -> Boolean
) {

    constructor(store: Store<Optional<STATE>, ACTION>) : this(store, { s1, s2 -> s1 == s2 })

    var currentState: Optional<STATE> = store.currentState

    val states: Observable<STATE> = store.state
        .filter { it.isPresent }
        .map { it.orNull()!! }
        .distinctUntilChanged(removeDuplicates)

    /**
     * Sends an action to the store.
     *
     * `ViewStore` is not thread safe and you should only send actions to it from the main thread.
     * If you are wanting to send actions on background threads due to the fact that the reducer
     * is performing computationally expensive work, then a better way to handle this is to wrap
     * that work in an `Effect` that is performed on a background thread so that the result can
     * be fed back into the store.
     */
    val send: (ACTION) -> Unit = { store.send(it) }

    private val viewDisposable = states.subscribe { currentState = it.toOptional() }

    fun dispose() {
        viewDisposable.dispose()
    }

    companion object {
        fun <STATE : Any, ACTION : Any> Store<Optional<STATE>, ACTION>.optionalView():
            OptionalViewStore<STATE, ACTION> = OptionalViewStore(this)

        fun <STATE : Any, ACTION : Any> Store<Optional<STATE>, ACTION>.optionalView(
            removeDuplicates: (STATE, STATE) -> Boolean
        ): OptionalViewStore<STATE, ACTION> = OptionalViewStore(this, removeDuplicates)
    }
}
