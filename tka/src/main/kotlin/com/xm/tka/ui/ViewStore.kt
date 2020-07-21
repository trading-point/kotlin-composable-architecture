@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package com.xm.tka.ui

import com.xm.tka.Store
import io.reactivex.Observable

/**
 * A `ViewStore` is an object that can observe state changes and send actions. They are most
 * commonly used in views but they can be used anywhere it makes sense to observe state
 * and send actions.
 *
 * Source: https://github.com/pointfreeco/swift-composable-architecture/blob/main/Sources/ComposableArchitecture/ViewStore.swift
 */
class ViewStore<STATE : Any, ACTION : Any>(
    store: Store<STATE, ACTION>,
    removeDuplicates: (STATE, STATE) -> Boolean
) {

    constructor(store: Store<STATE, ACTION>) : this(store, { s1, s2 -> s1 == s2 })

    var currentState = store.currentState

    val states: Observable<STATE> = store.state.distinctUntilChanged(removeDuplicates)

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

    private val viewDisposable = states.subscribe { currentState = it }

    fun dispose() {
        viewDisposable.dispose()
    }

    companion object {
        fun <STATE : Any, ACTION : Any> Store<STATE, ACTION>.view(): ViewStore<STATE, ACTION> =
            ViewStore(this)

        fun <STATE : Any, ACTION : Any> Store<STATE, ACTION>.view(
            removeDuplicates: (STATE, STATE) -> Boolean
        ): ViewStore<STATE, ACTION> =
            ViewStore(this, removeDuplicates)
    }
}
