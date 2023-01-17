package com.xm.tka

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.schedulers.TestScheduler
import io.reactivex.rxjava3.subjects.PublishSubject
import java.lang.Thread.sleep
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.SECONDS
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EffectCancellationTests {

    private val id = "ID"

    private val compositeDisposable = CompositeDisposable()

    @After
    fun tearDown() {
        compositeDisposable.dispose()
    }

    @Test
    fun testCancellation() {
        val subject = PublishSubject.create<Int>()
        val effect: Effect<Int> = subject.cancellable(id)

        effect
            .test()
            .also { compositeDisposable.add(it) }
            .assertNoValues()
            .also { subject.onNext(1) }
            .assertValue(1)
            .also { subject.onNext(2) }
            .assertValues(1, 2)
            .also { Effects.cancel<Int>(id).subscribe().also { compositeDisposable.add(it) } }
            .also { subject.onNext(3) }
            .assertValues(1, 2)
            .assertComplete()
            .assertNoErrors()
    }

    @Test
    fun testCancelInFlight() {
        val subject = PublishSubject.create<Int>()

        val effect = subject.cancellable(id, true)
            .test()
            .also { compositeDisposable.add(it) }
            .assertNoValues()
            .also { subject.onNext(1) }
            .assertValue(1)
            .also { subject.onNext(2) }
            .assertValues(1, 2)

        val effect2 = subject.cancellable(id, true)
            .test()
            .also { compositeDisposable.add(it) }
            .assertNoValues()

        effect.assertValues(1, 2)
            .assertComplete()
            .assertNoErrors()

        effect2.also { subject.onNext(1) }
            .assertValue(1)
            .also { subject.onNext(2) }
            .assertValues(1, 2)
            .assertNotComplete()
            .assertNoErrors()
    }

    @Test
    fun testCancellationAfterDelay() {
        var value: Int? = null

        val test = Observable.just(1)
            .delay(100, MILLISECONDS)
            .cancellable(id)
            .doOnNext { value = it }
            .test()
            .also { compositeDisposable.add(it) }

        assertNull(value)

        Schedulers.computation().scheduleDirect(
            { Effects.cancel<Int>(id).subscribe().also { compositeDisposable.add(it) } },
            10,
            MILLISECONDS
        )

        sleep(200)

        assertNull(value)

        test.assertComplete()
            .assertNoErrors()
            .assertNoValues()
    }

    @Test
    fun testCancellationAfterDelay_WithTestScheduler() {
        val scheduler = TestScheduler()

        var value: Int? = null

        val test = Observable.just(1)
            .delay(2, SECONDS, scheduler)
            .cancellable(id)
            .doOnNext { value = it }
            .test()
            .also { compositeDisposable.add(it) }

        assertNull(value)

        scheduler.advanceTimeBy(1, SECONDS)

        Effects.cancel<Int>(id).subscribe().also { compositeDisposable.add(it) }

        scheduler.advanceTimeBy(1, SECONDS)

        assertNull(value)

        test.assertComplete()
            .assertNoErrors()
            .assertNoValues()
    }

    @Test
    fun testCancellablesCleanUp_OnComplete() {
        Observable.just(1)
            .cancellable(id)
            .subscribe()
            .also { compositeDisposable.add(it) }

        assertEquals(0, cancellationDisposables.size)
    }

    @Test
    fun testCancellablesCleanUp_OnCancel() {
        val scheduler = TestScheduler()

        Observable.just(1)
            .delay(1, SECONDS, scheduler)
            .cancellable(id)
            .subscribe()
            .also { compositeDisposable.add(it) }

        Effects.cancel<Int>(id)
            .subscribe()
            .also { compositeDisposable.add(it) }

        assertEquals(0, cancellationDisposables.size)
    }

    @Test
    fun testDoubleCancellation() {
        val subject = PublishSubject.create<Int>()
        subject
            .cancellable(id)
            .cancellable(id)
            .test()
            .also { compositeDisposable.add(it) }
            .assertNoValues()
            .also { subject.onNext(1) }
            .assertValue(1)
            .also { Effects.cancel<Int>(id).subscribe() }
            .also { subject.onNext(2) }
            .assertValue(1)
            .assertComplete()
            .assertNoErrors()
    }

    @Test
    fun testCompleteBeforeCancellation() {
        val subject = PublishSubject.create<Int>()
        subject
            .cancellable(id)
            .test()
            .also { compositeDisposable.add(it) }
            .assertNoValues()
            .also { subject.onNext(1) }
            .assertValue(1)
            .also { subject.onComplete() }
            .assertValue(1)
            .assertComplete()
            .assertNoErrors()
            .also { Effects.cancel<Int>(id).subscribe() }
            .assertValue(1)
            .assertComplete()
            .assertNoErrors()
    }

    @Test
    fun testConcurrentCancels() {
        val schedulers = listOf(
            Schedulers.computation(),
            Schedulers.newThread(),
            Schedulers.trampoline(),
            Schedulers.io(),
            Schedulers.single()
        )

        Effects.merge(
            *(1..1_000).map { idx ->
                val id = idx % 10

                Effects.merge(
                    Observable.just(idx)
                        .delay((1..100L).random(), MILLISECONDS, schedulers.random())
                        .cancellable(id),
                    Observable.empty<Int>()
                        .delay((1..100L).random(), MILLISECONDS, schedulers.random())
                        .flatMap { Effects.cancel(id) }
                )
            }.toTypedArray()
        ).test()
            .also { compositeDisposable.add(it) }
            .await()
            .assertComplete()
            .assertNoErrors()

        assertEquals(0, cancellationDisposables.size)
    }

    @Test
    fun testNestedCancels() {
        var effect = Observable.never<Nothing>()
            .cancellable(id)

        repeat((1..(1..1_000).random()).count()) {
            effect = effect.cancellable(id)
        }

        effect
            .subscribe()
            .also { compositeDisposable.add(it) }
            .dispose()

        assertEquals(0, cancellationDisposables.size)
    }

    @Test
    fun testSharedId() {
        val scheduler = TestScheduler()

        val effect1 = Observable.just(1)
            .delay(1, SECONDS, scheduler)
            .cancellable(id)

        val effect2 = Observable.just(2)
            .delay(2, SECONDS, scheduler)
            .cancellable(id)

        Observable.merge(
            effect1,
            effect2
        ).test()
            .also { compositeDisposable.add(it) }
            .assertNoValues()
            .also { scheduler.advanceTimeBy(1, SECONDS) }
            .assertValue(1)
            .also { scheduler.advanceTimeBy(1, SECONDS) }
            .assertValues(1, 2)
            .assertComplete()
            .assertNoErrors()
    }

    @Test
    fun testImmediateCancellation() {
        val scheduler = TestScheduler()

        val expectedOutput = mutableListOf<Int>()

        // Don't hold onto cancellable so that it is deallocated immediately.
        Observable.defer { Observable.just(1) }
            .delay(1, SECONDS, scheduler)
            .cancellable(id)
            .subscribe { expectedOutput.add(it) }
            .dispose()

        assertEquals(emptyList<Int>(), expectedOutput)
        scheduler.advanceTimeTo(1, SECONDS)
        assertEquals(emptyList<Int>(), expectedOutput)
    }
}
