package com.xm.examples

import com.xm.examples.cases.EffectCancellationState
import com.xm.examples.cases.EffectsCancellationAction
import com.xm.examples.cases.EffectsCancellationAction.CancelButtonTapped
import com.xm.examples.cases.EffectsCancellationAction.StepperDecrement
import com.xm.examples.cases.EffectsCancellationAction.StepperIncrement
import com.xm.examples.cases.EffectsCancellationAction.TriviaButtonTapped
import com.xm.examples.cases.EffectsCancellationAction.TriviaResponse
import com.xm.examples.cases.EffectsCancellationEnvironment
import com.xm.examples.cases.TriviaRequestId
import com.xm.examples.cases.effectsCancellationReducer
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
import org.mockito.Mockito.anyString
import org.mockito.Mockito.mock

class EffectsCancellationTest {

    private val factClient = mock(FactClientLive::class.java)
    private val schedulerProvider = mock(BaseSchedulerProvider::class.java)

    @Test
    fun testTrivia_SuccessfulRequest() {
        val env = mock(EffectsCancellationEnvironment::class.java)

        val testScheduler = Schedulers.trampoline()
        `when`(env.schedulerProvider)
            .thenReturn(schedulerProvider)
        `when`(env.schedulerProvider.io())
            .thenReturn(testScheduler)
        `when`(env.schedulerProvider.mainThread())
            .thenReturn(testScheduler)

        `when`(env.fact)
            .thenReturn(factClient)

        val response = "0 is a good number Brent"
        `when`(factClient.execute("1"))
            .thenReturn(Single.just(Result.success(response)))

        TestStore(EffectCancellationState(), effectsCancellationReducer, env).assert(
            send(StepperIncrement(1)) {
                it.copy(2)
            },
            send(StepperDecrement(2)) {
                it.copy(1)
            },
            send(TriviaButtonTapped) {
                it.copy(isTriviaRequestInFlight = true)
            },
            receive(TriviaResponse(Result.success(response))) {
                it.copy(
                    currentTrivia = response,
                    isTriviaRequestInFlight = false
                )
            }
        )
    }

    @Test
    fun testTrivia_FailedRequest() {
        val env = mock(EffectsCancellationEnvironment::class.java)

        val testScheduler = Schedulers.trampoline()
        `when`(env.schedulerProvider)
            .thenReturn(schedulerProvider)
        `when`(env.schedulerProvider.io())
            .thenReturn(testScheduler)
        `when`(env.schedulerProvider.mainThread())
            .thenReturn(testScheduler)

        `when`(env.fact)
            .thenReturn(factClient)

        val exception = java.lang.Exception()
        `when`(factClient.execute(anyString()))
            .thenReturn(Single.just(Result.failure(exception)))

        TestStore(EffectCancellationState(), effectsCancellationReducer, env).assert(
            send(TriviaButtonTapped) {
                it.copy(isTriviaRequestInFlight = true)
            },
            receive(TriviaResponse(Result.failure(exception))) {
                it.copy(isTriviaRequestInFlight = false)
            },
            `do` { Effects.cancel<EffectsCancellationAction>(TriviaRequestId) }
        )
    }

    /**
     * NB: This tests that the cancel button really does cancel the in-flight API request.
     *
     * To see the real power of this test, try replacing the `.cancel` effect with a `.none` effect
     * in the `.cancelButtonTapped` action of the `effectsCancellationReducer`. This will cause the
     * test to fail, showing that we are exhaustively asserting that the effect truly is canceled and
     * will never emit.
     */
    @Test
    fun testTrivia_CancelButtonCancelsRequest() {
        val env = mock(EffectsCancellationEnvironment::class.java)

        val testScheduler = TestScheduler()
        `when`(env.schedulerProvider)
            .thenReturn(schedulerProvider)

        `when`(env.schedulerProvider.io())
            .thenReturn(testScheduler)
        `when`(env.schedulerProvider.mainThread())
            .thenReturn(testScheduler)

        `when`(env.fact)
            .thenReturn(factClient)

        val response = "0 is a good number Brent"
        `when`(factClient.execute(anyString()))
            .thenReturn(Single.just(Result.success(response)))

        TestStore(EffectCancellationState(), effectsCancellationReducer, env).assert(
            send(TriviaButtonTapped) {
                it.copy(isTriviaRequestInFlight = true)
            },
            send(CancelButtonTapped) {
                it.copy(isTriviaRequestInFlight = false)
            },
            `do` { Effects.cancel<EffectsCancellationAction>(TriviaRequestId) }
        )
    }

    @Test
    fun testTrivia_PlusMinusButtonsCancelsRequest() {
        val env = mock(EffectsCancellationEnvironment::class.java)

        val testScheduler = TestScheduler()
        `when`(env.schedulerProvider)
            .thenReturn(schedulerProvider)
        `when`(env.schedulerProvider.io())
            .thenReturn(testScheduler)
        `when`(env.schedulerProvider.mainThread())
            .thenReturn(testScheduler)

        `when`(env.fact)
            .thenReturn(factClient)
        val response = "1 is a good number Brent"
        `when`(factClient.execute(anyString()))
            .thenReturn(Single.just(Result.success(response)))

        TestStore(EffectCancellationState(), effectsCancellationReducer, env).assert(
            send(TriviaButtonTapped) {
                it.copy(isTriviaRequestInFlight = true)
            },
            send(StepperIncrement(1)) {
                it.copy(count = 2, isTriviaRequestInFlight = false)
            },
            `do` { Effects.cancel<EffectsCancellationAction>(TriviaRequestId) },
        )
    }
}