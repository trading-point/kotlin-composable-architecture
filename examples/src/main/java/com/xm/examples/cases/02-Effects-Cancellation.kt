package com.xm.examples.cases

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import com.xm.examples.MainActivity
import com.xm.examples.R
import com.xm.examples.cases.EffectsCancellationAction.CancelButtonTapped
import com.xm.examples.cases.EffectsCancellationAction.StepperDecrement
import com.xm.examples.cases.EffectsCancellationAction.StepperIncrement
import com.xm.examples.cases.EffectsCancellationAction.TriviaButtonTapped
import com.xm.examples.cases.EffectsCancellationAction.TriviaResponse
import com.xm.examples.databinding.FragmentCancellationBinding
import com.xm.examples.utils.BaseSchedulerProvider
import com.xm.examples.utils.FactClientLive
import com.xm.examples.utils.SchedulerProvider
import com.xm.tka.Effects
import com.xm.tka.Reducer
import com.xm.tka.Store
import com.xm.tka.cancellable
import com.xm.tka.toEffect
import com.xm.tka.ui.ViewStore
import com.xm.tka.ui.ViewStore.Companion.view
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import java.util.concurrent.TimeUnit

private val readMe = """
  This screen demonstrates how one can cancel in-flight effects in the Composable Architecture.
  
  Use the stepper to count to a number, and then tap the "Number fact" button to fetch
  a random fact about that number using an API.
  
  While the API request is in-flight, you can tap "Cancel" to cancel the effect and prevent
  it from feeding data back into the application. Interacting with the stepper while a
  request is in-flight will also cancel it.
    
""".trimIndent()

class EffectsCancellation : Fragment() {

    private lateinit var binding: FragmentCancellationBinding

    private val compositeDisposable = CompositeDisposable()

    private val viewModel: EffectsCancellationViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCancellationBinding.inflate(layoutInflater)
        binding.readme.text = readMe
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as MainActivity).supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = resources.getString(R.string.effects_cancellation_toolbar_title)
        }

        val viewStore = viewModel.viewStore

        viewStore.states
            .subscribe {
                with(binding) {
                    counter.number.text = it.count.toString()
                    response.text = it.currentTrivia ?: ""

                    progressBar.visibility =
                        if (it.isTriviaRequestInFlight) View.VISIBLE else View.GONE

                    cancel.visibility =
                        if (it.isTriviaRequestInFlight) View.VISIBLE else View.GONE
                }
            }
            .addTo(compositeDisposable)

        with(binding) {
            counter.decrement.setOnClickListener {
                viewStore.send(StepperDecrement(counter.number.text.toString().toInt()))
            }
            counter.increment.setOnClickListener {
                viewStore.send(StepperIncrement(counter.number.text.toString().toInt()))
            }
            numberFactButton.setOnClickListener { viewStore.send(TriviaButtonTapped) }
            cancel.setOnClickListener { viewStore.send(CancelButtonTapped) }
        }
    }

    override fun onDestroyView() {
        compositeDisposable.clear()
        super.onDestroyView()
    }
}

class EffectsCancellationViewModel : ViewModel() {

    private val store = Store(
        initialState = EffectCancellationState(),
        reducer = effectsCancellationReducer,
        environment = EffectsCancellationEnvironment(FactClientLive(), BaseSchedulerProvider())
    )

    val viewStore: ViewStore<EffectCancellationState, EffectsCancellationAction> = store.view()
}

data class EffectCancellationState(
    val count: Int = 0,
    val currentTrivia: String? = null,
    val isTriviaRequestInFlight: Boolean = false
)

sealed class EffectsCancellationAction {
    object CancelButtonTapped : EffectsCancellationAction()
    data class StepperDecrement(val num: Int) : EffectsCancellationAction()
    data class StepperIncrement(val num: Int) : EffectsCancellationAction()
    object TriviaButtonTapped : EffectsCancellationAction()
    data class TriviaResponse(val response: Result<String>) : EffectsCancellationAction()
}

class EffectsCancellationEnvironment(
    val fact: FactClientLive,
    val schedulerProvider: SchedulerProvider
)

object TriviaRequestId

val effectsCancellationReducer =
    Reducer<EffectCancellationState, EffectsCancellationAction, EffectsCancellationEnvironment> { state, action, env ->
        when (action) {
            is StepperDecrement -> state.copy(
                count = action.num - 1,
                currentTrivia = null,
                isTriviaRequestInFlight = false
            ) + Effects.cancel(TriviaRequestId)

            is StepperIncrement -> state.copy(
                count = action.num + 1,
                currentTrivia = null,
                isTriviaRequestInFlight = false
            ) + Effects.cancel(TriviaRequestId)

            CancelButtonTapped -> state.copy(
                isTriviaRequestInFlight = false
            ) + Effects.cancel(TriviaRequestId)

            TriviaButtonTapped -> state.copy(
                currentTrivia = null,
                isTriviaRequestInFlight = true
            ) + env.fact.execute(state.count.toString())
                // dummy delay to allow time to cancel
                .delay(1, TimeUnit.SECONDS)
                .subscribeOn(env.schedulerProvider.io())
                .observeOn(env.schedulerProvider.mainThread())
                .map<EffectsCancellationAction> { TriviaResponse(it) }
                .toEffect()
                .cancellable(TriviaRequestId)

            is TriviaResponse ->
                action.response.fold(
                    onSuccess = { data ->
                        state.copy(
                            currentTrivia = data,
                            isTriviaRequestInFlight = false
                        ) + Effects.none()
                    },
                    onFailure = {
                        state.copy(
                            isTriviaRequestInFlight = false
                        ) + Effects.none()
                    }
                )
        }
    }

