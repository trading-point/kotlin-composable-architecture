package com.xm.examples.cases

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import com.xm.examples.MainActivity
import com.xm.examples.databinding.FragmentEffectsBasicBinding
import com.xm.examples.cases.EffectsBasicsAction.DecrementButtonTapped
import com.xm.examples.cases.EffectsBasicsAction.IncrementButtonTapped
import com.xm.examples.cases.EffectsBasicsAction.NumberFactButtonTapped
import com.xm.examples.cases.EffectsBasicsAction.NumberFactResponse
import com.xm.examples.utils.FactClientLive
import com.xm.examples.utils.SchedulerProvider
import com.xm.tka.Effects
import com.xm.tka.Reducer
import com.xm.tka.Store
import com.xm.tka.toEffect
import com.xm.tka.ui.ViewStore
import com.xm.tka.ui.ViewStore.Companion.view
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.cast
import java.util.concurrent.TimeUnit

class EffectsBasicFragment : Fragment() {

    private lateinit var binding: FragmentEffectsBasicBinding

    private val compositeDisposable = CompositeDisposable()

    private val viewModel: EffectsBasicViewModel by viewModels()
    private lateinit var viewStore: ViewStore<EffectsBasicsState, EffectsBasicsAction>

    override fun onAttach(context: Context) {
        super.onAttach(context)
        viewStore = viewModel.viewStore
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentEffectsBasicBinding.inflate(layoutInflater)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as MainActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)

        viewStore.states
            .subscribe {
                with(binding) {
                    tvNumber.text = it.count.toString()
                    tvText.text = it.numberFact

                    progressBar.visibility =
                        if (it.isNumberFactRequestInFlight) View.VISIBLE else View.GONE
                }
            }
            .addTo(compositeDisposable)

        with(binding) {
            btnDecrement.setOnClickListener { viewStore.send(DecrementButtonTapped) }
            btnIncrement.setOnClickListener { viewStore.send(IncrementButtonTapped) }
            btnNumberFact.setOnClickListener { viewStore.send(NumberFactButtonTapped) }
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
        environment = EffectsBasicsEnvironment(FactClientLive, SchedulerProvider)
    )

    val viewStore: ViewStore<EffectsBasicsState, EffectsBasicsAction> = store.view()
}

// Domain

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

interface EffectsBasicsEnvironment {
    val fact: FactClientLive
    val schedulerProvider: SchedulerProvider

    companion object {

        operator fun invoke(
            fact: FactClientLive,
            schedulerProvider: SchedulerProvider
        ): EffectsBasicsEnvironment = object : EffectsBasicsEnvironment {
            override val fact: FactClientLive = fact
            override val schedulerProvider: SchedulerProvider = schedulerProvider
        }
    }
}

private val effectsBasicReducer =
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
