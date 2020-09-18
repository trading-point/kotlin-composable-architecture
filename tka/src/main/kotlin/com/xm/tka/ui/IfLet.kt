package com.xm.tka.ui

import com.xm.tka.Optional
import com.xm.tka.Store
import io.reactivex.annotations.CheckReturnValue
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable

/**
 * Subscribes to updates when a store containing optional state goes from `null` to non-`null` or
 * non-`null` to `null`.
 *
 * This is useful for handling navigation. The state for a screen that you want to
 * navigate to can be held as an optional value in the parent, and when that value switches
 * from `null` to non-`null` you want to trigger a navigation and hand the detail view a `Store`
 * whose domain has been scoped to just that feature
 *
 * @param unwrap: A function that is called with a store of non-optional state whenever the store's
 * optional state goes from `null` to non-`null`.
 * @param orElse: A function that is called whenever the store's optional state goes from non-`null` to
 * `null`.
 * @return A disposable associated with the underlying subscription.
 */
@CheckReturnValue
fun <STATE : Any, ACTION : Any> Store<Optional<STATE>, ACTION>.ifLet(
    unwrap: (Store<STATE, ACTION>) -> Unit,
    orElse: () -> Unit
): Disposable {
    val elseDisposable = scopes { states ->
        states.distinctUntilChanged { s1, s2 -> (s1.isPresent) == (s2.isPresent) }
    }.subscribe {
        if (it.currentState.isPresent.not()) orElse()
    }

    val unwrapDisposable = scopes { states ->
        states.distinctUntilChanged { s1, s2 -> (s1.isPresent) == (s2.isPresent) }
            .filter { it.isPresent }
            .map { it.orNull()!! }
    }.subscribe {
        unwrap(it)
    }

    return CompositeDisposable().apply {
        add(elseDisposable)
        add(unwrapDisposable)
    }
}

/**
 * An overload of `ifLet(then:else:)` for the times that you do not want to handle the `else`
 * case.
 *
 * @param unwrap: A function that is called with a store of non-optional state whenever the
 * store's optional state goes from `null` to non-`null`.
 * @return A disposable associated with the underlying subscription.
 */
@CheckReturnValue
fun <STATE : Any, ACTION : Any> Store<Optional<STATE>, ACTION>.ifLet(
    unwrap: (Store<STATE, ACTION>) -> Unit
): Disposable = ifLet(unwrap, {})
