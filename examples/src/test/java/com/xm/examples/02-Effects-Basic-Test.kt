package com.xm.examples

import com.xm.examples.cases.EffectsBasicsAction
import com.xm.examples.cases.EffectsBasicsAction.IncrementButtonTapped
import com.xm.examples.cases.EffectsBasicsEnvironment
import com.xm.examples.cases.EffectsBasicsState
import com.xm.examples.cases.FactId
import com.xm.examples.cases.effectsBasicReducer
import com.xm.examples.utils.BaseSchedulerProvider
import com.xm.examples.utils.FactClientLive
import com.xm.tka.Effects
import com.xm.tka.test.TestStore
import com.xm.tka.test.TestStore.Step.Companion.`do`
import com.xm.tka.test.TestStore.Step.Companion.receive
import com.xm.tka.test.TestStore.Step.Companion.send
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.schedulers.TestScheduler
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import java.util.concurrent.TimeUnit

class EffectsBasicTest {

    private val factClient = mock(FactClientLive::class.java)

    private val schedulerProvider = mock(BaseSchedulerProvider::class.java)

    @Test
    fun testCountDown() {
        val env: EffectsBasicsEnvironment = mock(EffectsBasicsEnvironment::class.java)

        val testScheduler = TestScheduler()
        `when`(env.schedulerProvider)
            .thenReturn(schedulerProvider)

        `when`(env.schedulerProvider.mainThread())
            .thenReturn(testScheduler)

        TestStore(EffectsBasicsState(count = 0), effectsBasicReducer, env).assert(
            send(IncrementButtonTapped) {
                it.copy(1)
            },
            send(EffectsBasicsAction.DecrementButtonTapped) {
                it.copy(count = 0, numberFact = null)
            },
            `do` { testScheduler.advanceTimeBy(1, TimeUnit.SECONDS) },
            receive(IncrementButtonTapped) {
                it.copy(1)
            }
        )
    }

    @Test
    fun testNumberFact() {
        val env: EffectsBasicsEnvironment = mock(EffectsBasicsEnvironment::class.java)

        val testScheduler = Schedulers.trampoline()
        `when`(env.schedulerProvider)
            .thenReturn(schedulerProvider)

        `when`(env.schedulerProvider.io())
            .thenReturn(testScheduler)

        `when`(env.schedulerProvider.mainThread())
            .thenReturn(testScheduler)

        `when`(env.fact)
            .thenReturn(factClient)

        val response = Result.success("1 is a good number Brent")
        `when`(factClient.execute("1"))
            .thenReturn(Single.just(response))

        TestStore(EffectsBasicsState(count = 0), effectsBasicReducer, env).assert(
            send(IncrementButtonTapped) {
                it.copy(1)
            },
            send(EffectsBasicsAction.NumberFactButtonTapped) {
                it.copy(isNumberFactRequestInFlight = true)
            },
            receive(EffectsBasicsAction.NumberFactResponse(response)) {
                it.copy(
                    isNumberFactRequestInFlight = false,
                    numberFact = "1 is a good number Brent"
                )
            },
            `do` { Effects.cancel<EffectsBasicsAction>(FactId) }
        )
    }

}