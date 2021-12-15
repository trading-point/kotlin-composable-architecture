package com.xm.tka

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.TestScheduler
import java.util.concurrent.TimeUnit.SECONDS
import org.junit.Test

class EffectsTest {

    @Test
    fun testEraseToEffectWithError() {
        Effects.future { 42 }
            .test()
            .assertComplete()
            .assertNoErrors()
            .assertValue(42)

        val error = Error()
        Effects.future { throw error }
            .test()
            .assertNotComplete()
            .assertError(error)
            .assertNoValues()
    }

    @Test
    fun testMerge() {
        val scheduler = TestScheduler()

        val effect = Effects.merge(
            Effects.just(1).delay(1, SECONDS, scheduler),
            Effects.just(2).delay(2, SECONDS, scheduler),
            Effects.just(3).delay(3, SECONDS, scheduler)
        )

        effect.test()
            .assertNotComplete()
            .assertNoErrors()
            .assertNoValues()
            .also { scheduler.advanceTimeBy(1, SECONDS) }
            .assertNotComplete()
            .assertNoErrors()
            .assertValues(1)
            .also { scheduler.advanceTimeBy(1, SECONDS) }
            .assertNotComplete()
            .assertNoErrors()
            .assertValues(1, 2)
            .also { scheduler.advanceTimeBy(1, SECONDS) }
            .assertComplete()
            .assertNoErrors()
            .assertValues(1, 2, 3)
    }

    @Test
    fun testEffectSubscriberInitializer() {
        val scheduler = TestScheduler()

        Observable.create<Int> { subscriber ->
            subscriber.onNext(1)
            subscriber.onNext(2)

            scheduler.scheduleDirect({ subscriber.onNext(3) }, 1, SECONDS)
            scheduler.scheduleDirect(
                {
                    subscriber.onNext(4)
                    subscriber.onComplete()
                },
                2,
                SECONDS
            )
        }
            .test()
            .assertNotComplete()
            .assertNoErrors()
            .assertValues(1, 2)
            .also { scheduler.advanceTimeBy(1, SECONDS) }
            .assertNotComplete()
            .assertNoErrors()
            .assertValues(1, 2, 3)
            .also { scheduler.advanceTimeBy(1, SECONDS) }
            .assertComplete()
            .assertNoErrors()
            .assertValues(1, 2, 3, 4)
    }

    @Test
    fun testEffectSubscriberInitializer_WithCancellation() {
        val cancelId = "id"

        val scheduler = TestScheduler()

        val effect = Observable.create<Int> { observer ->
            observer.onNext(1)

            scheduler.scheduleDirect({ observer.onNext(2) }, 1, SECONDS)
        }.cancellable(cancelId)

        effect
            .test()
            .assertValue(1)
            .assertNotComplete()
            .also { Effects.cancel<Int>(cancelId).subscribe() }
            .also { scheduler.advanceTimeBy(1, SECONDS) }
            .assertValue(1)
            .assertComplete()
    }
}
