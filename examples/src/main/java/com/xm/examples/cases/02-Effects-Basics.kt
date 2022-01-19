package com.xm.examples.cases

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import com.xm.examples.MainActivity
import com.xm.examples.cases.EffectsBasicsAction.DecrementButtonTapped
import com.xm.examples.cases.EffectsBasicsAction.IncrementButtonTapped
import com.xm.examples.cases.EffectsBasicsAction.NumberFactButtonTapped
import com.xm.examples.cases.EffectsBasicsAction.NumberFactResponse
import com.xm.examples.databinding.FragmentEffectsBasicBinding
import com.xm.examples.utils.BaseSchedulerProvider
import com.xm.examples.utils.FactClientLive
import com.xm.tka.Effects
import com.xm.tka.Reducer
import com.xm.tka.Store
import com.xm.tka.cancellable
import com.xm.tka.toEffect
import com.xm.tka.ui.ViewStore
import com.xm.tka.ui.ViewStore.Companion.view
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.cast
import java.util.concurrent.TimeUnit
import com.xm.examples.R
import com.xm.examples.utils.SchedulerProvider

private val readMe = """
  This screen demonstrates how to introduce side effects into a feature built with the
  Composable Architecture.

  A side effect is a unit of work that needs to be performed in the outside world. For example, an
  API request needs to reach an external service over HTTP, which brings with it lots of
  uncertainty and complexity.

  Many things we do in our applications involve side effects, such as timers, database requests,
  file access, socket connections, and anytime a scheduler is involved (such as debouncing,
  throttling and delaying), and they are typically difficult to test.

  This application has two simple side effects:

  • Each time you count down the number will be incremented back up after a delay of 1 second.
  • Tapping "Number fact" will trigger an API request to load a piece of trivia about that number.

  Both effects are handled by the reducer, and a full test suite is written to confirm that the
  effects behave in the way we expect.
    
""".trimIndent()

class EffectsBasic : Fragment() {

    private lateinit var binding: FragmentEffectsBasicBinding

    private val compositeDisposable = CompositeDisposable()

    private val viewModel: EffectsBasicViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentEffectsBasicBinding.inflate(layoutInflater)
        binding.readme.text = readMe
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as MainActivity).supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(false)
            title = resources.getString(R.string.effects_basics_toolbar_title)
        }

        val viewStore = viewModel.viewStore

        viewStore.states
            .subscribe {
                with(binding) {
                    number.text = it.count.toString()
                    response.text = it.numberFact

                    progressBar.visibility =
                        if (it.isNumberFactRequestInFlight) View.VISIBLE else View.GONE
                }
            }
            .addTo(compositeDisposable)

        with(binding) {
            decrement.setOnClickListener { viewStore.send(DecrementButtonTapped) }
            increment.setOnClickListener { viewStore.send(IncrementButtonTapped) }
            numberFactButton.setOnClickListener { viewStore.send(NumberFactButtonTapped) }
        }
    }

    override fun onDestroyView() {
        compositeDisposable.clear()
        super.onDestroyView()
    }
}

class EffectsBasicViewModel : ViewModel() {

    private val store = Store(
        initialState = EffectsBasicsState(),
        reducer = effectsBasicReducer,
        environment = EffectsBasicsEnvironment(FactClientLive(), BaseSchedulerProvider())
    )

    val viewStore: ViewStore<EffectsBasicsState, EffectsBasicsAction> = store.view()
}

data class EffectsBasicsState(
    val count: Int = 0,
    val isNumberFactRequestInFlight: Boolean = false,
    val numberFact: String? = null
)

sealed class EffectsBasicsAction {
    object DecrementButtonTapped : EffectsBasicsAction()
    object IncrementButtonTapped : EffectsBasicsAction()
    object NumberFactButtonTapped : EffectsBasicsAction()
    data class NumberFactResponse(val response: Result<String>) : EffectsBasicsAction()
}

class EffectsBasicsEnvironment(
    val fact: FactClientLive,
    val schedulerProvider: SchedulerProvider
)

val effectsBasicReducer =
    Reducer<EffectsBasicsState, EffectsBasicsAction, EffectsBasicsEnvironment> { state, action, env ->
        when (action) {
            DecrementButtonTapped -> state.copy(
                count = state.count - 1,
                numberFact = null
            ) + Effects.just(IncrementButtonTapped)
                .delay(1, TimeUnit.SECONDS)
                .observeOn(env.schedulerProvider.mainThread())
                .cast()

            IncrementButtonTapped -> state.copy(
                count = state.count + 1,
                numberFact = null
            ) + Effects.none()

            NumberFactButtonTapped -> state.copy(
                isNumberFactRequestInFlight = true,
                numberFact = null
                // Return an effect that fetches a number fact from the API and returns the
                // value back to the reducer's `numberFactResponse` action.
            ) + env.fact.execute(state.count.toString())
                .subscribeOn(env.schedulerProvider.io())
                .observeOn(env.schedulerProvider.mainThread())
                .map<EffectsBasicsAction> { NumberFactResponse(it) }
                .toEffect()
                .cancellable(FactId)

            is NumberFactResponse ->
                action.response.fold(
                    onSuccess = { data ->
                        state.copy(
                            numberFact = data,
                            isNumberFactRequestInFlight = false
                        ) + Effects.none()
                    },
                    onFailure = { throwable ->
                        state.copy(
                            numberFact = throwable.toString(),
                            isNumberFactRequestInFlight = false
                        ) + Effects.none()
                    }
                )
        }
    }

object FactId