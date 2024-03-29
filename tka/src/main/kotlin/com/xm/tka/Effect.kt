@file:Suppress("unused")

package com.xm.tka

import com.xm.tka.Effects.just
import com.xm.tka.Effects.none
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.subjects.PublishSubject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * The `Effect` type encapsulates a unit of work that can be run in the outside world, and can feed
 * data back to the `Store`. It is the perfect place to do side effects, such as network requests,
 * saving/loading from disk, creating timers, interacting with dependencies, and more.
 *
 * Effects are returned from reducers so that the `Store` can perform the effects after the reducer
 * is done running. It is important to note that `Store` is not thread safe, and so all effects
 * must receive values on the same thread, **and** if the store is being used to drive UI then it
 * must receive values on the main thread.
 *
 * An effect simply wraps a `Publisher` value and provides some convenience initializers for
 * constructing some common types of effects.
 *
 * Source: https://github.com/pointfreeco/swift-composable-architecture/blob/main/Sources/ComposableArchitecture/Effect.swift
 */
typealias Effect<ACTION> = Observable<ACTION>

/**
 * [Effect] utility functions
 */
object Effects {

    /**
     * An effect that does nothing and completes immediately. Useful for situations where you must
     * return an effect, but you don't need to do anything.
     */
    fun <ACTION : Any> none(): Effect<ACTION> = Observable.empty()

    /**
     * Initializes an effect that immediately emits the value passed in.
     *
     * @param action: The action that is immediately emitted by the effect.
     */
    fun <ACTION : Any> just(action: ACTION): Effect<ACTION> = Observable.just(action)

    /**
     * Initializes an effect that immediately fails with the error passed in.
     *
     * @param throwable: The error that is immediately emitted by the effect.
     */
    fun <ACTION : Any> error(throwable: Throwable): Effect<ACTION> = Observable.error(throwable)

    /**
     * Creates an effect that can supply a single value asynchronously in the future.
     *
     * This can be helpful for converting APIs that are callback-based into ones that deal with
     * `Effect`s.
     */
    fun <ACTION : Any> future(work: () -> ACTION): Effect<ACTION> =
        Maybe.fromCallable(work).toEffect()

    /**
     * Creates an effect that executes some work in the real world that doesn't need to feed data
     * back into the store.
     *
     * @param work: closure encapsulating some work to execute in the real world.
     */
    fun <ACTION : Any> fireAndForget(work: () -> Unit): Effect<ACTION> =
        Maybe.fromAction<ACTION> { runCatching(work) }.toEffect()

    /**
     * Merges a variadic list of effects together into a single effect, which runs the effects at the
     * same time.
     *
     * @param effects: A list of effects.
     * @return A new effect
     */
    fun <ACTION : Any> merge(vararg effects: Effect<ACTION>): Effect<ACTION> =
        Observable.merge(effects.toList())

    /**
     * Concatenates a variadic list of effects together into a single effect, which runs the effects
     * one after the other
     *
     * @param effects: A list of effects.
     * @return A new effect
     */
    fun <ACTION : Any> concatenate(vararg effects: Effect<ACTION>): Effect<ACTION> =
        Observable.concat(effects.toList())

    /**
     * An effect that will cancel any currently in-flight effect with the given identifier.
     *
     * @param id: An effect identifier.
     * @return A new effect that will cancel any currently in-flight effect with the given
     * identifier.
     */
    fun <ACTION : Any> cancel(id: Any): Effect<ACTION> = fireAndForget {
        cancellationDisposables[id]?.forEach { it.dispose() }
    }

    /**
     * An effect that will cancel any currently in-flight effects with the given identifier.
     *
     * @param ids: The effect identifiers.
     * @return A new effect that will cancel any currently in-flight effects with the given
     * identifiers.
     */
    fun <ACTION : Any> cancel(vararg ids: Any): Effect<ACTION> = fireAndForget {
        ids.forEach { id ->
            cancellationDisposables[id]?.forEach { it.dispose() }
        }
    }
}

/**
 * Turns any [Flowable] into an `Effect`
 */
fun <ACTION : Any> Flowable<ACTION>.toEffect(): Effect<ACTION> = this.toObservable()

/**
 * Turns any [Single] into an `Effect`
 */
fun <ACTION : Any> Single<ACTION>.toEffect(): Effect<ACTION> = this.toObservable()

/**
 * Turns any [Maybe] into an `Effect`
 */
fun <ACTION : Any> Maybe<ACTION>.toEffect(): Effect<ACTION> = this.toObservable()

/**
 * Turns any [Completable] into a [none] `Effect`
 */
fun <ACTION : Any> Completable.toEffect(): Effect<ACTION> = this.andThen(none())

/**
 * Turns any [Completable] into a [just] `Effect`
 */
fun <ACTION : Any> Completable.toEffect(action: ACTION): Effect<ACTION> = this.andThen(just(action))

/**
 * Turns an effect into one that is capable of being canceled.
 *
 * To turn an effect into a cancellable one you must provide an identifier, which is used in
 * `Effect.cancel(id:)` to identify which in-flight effect should be canceled.
 *
 * @param id: The effect's identifier.
 * @param cancelInFlight: Determines if any in-flight effect with the same identifier should be
 * canceled before starting this new one.
 * @return A new effect that is capable of being canceled by an identifier.
 */
fun <ACTION : Any> Effect<ACTION>.cancellable(
    id: Any,
    cancelInFlight: Boolean = false
): Effect<ACTION> {
    val effect = Observable.defer {
        cancellablesLock.withLock {
            if (cancelInFlight) cancellationDisposables[id]?.forEach { it.dispose() }

            val subject = PublishSubject.create<ACTION>()
            val values = mutableListOf<ACTION>()
            var isCaching = true
            val disposable = this
                .doOnNext { if (isCaching) values.add(it) }
                .subscribe(subject::onNext, subject::onError, subject::onComplete)

            var cancellationDisposable: Disposable? = null
            cancellationDisposable = Disposable.fromAction {
                cancellablesLock.withLock {
                    subject.onComplete()
                    disposable.dispose()
                    cancellationDisposables[id]
                        ?.apply {
                            remove(cancellationDisposable)
                        }
                        ?.ifEmpty {
                            cancellationDisposables.remove(id)
                        }
                }
            }

            cancellationDisposables.getOrPut(id) { ConcurrentLinkedDeque() }.add(cancellationDisposable)

            Observable.fromIterable(values)
                .concatWith(subject)
                .doOnError { cancellationDisposable.dispose() }
                .doOnComplete { cancellationDisposable.dispose() }
                .doOnSubscribe { isCaching = false }
                .doOnDispose { cancellationDisposable.dispose() }
        }
    }
    return effect
}

internal val cancellationDisposables = ConcurrentHashMap<Any, ConcurrentLinkedDeque<Disposable>>()
private val cancellablesLock = ReentrantLock()
