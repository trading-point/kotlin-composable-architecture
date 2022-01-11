package com.xm.examples.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.xm.examples.MainActivity
import com.xm.examples.databinding.FragmentEffectsBasicBinding
import com.xm.examples.fragments.EffectsBasicsAction.decrementButtonTapped
import com.xm.examples.fragments.EffectsBasicsAction.incrementButtonTapped
import com.xm.examples.fragments.EffectsBasicsAction.numberFactButtonTapped
import com.xm.examples.fragments.EffectsBasicsAction.numberFactResponse
import com.xm.examples.utils.FactClientLive
import com.xm.examples.utils.Result
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

    private lateinit var viewStore: ViewStore<EffectsBasicsState, EffectsBasicsAction>

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

        val store = Store(
            initialState = EffectsBasicsState(),
            reducer = effectsBasicReducer,
            environment = EffectsBasicsEnvironment(FactClientLive, SchedulerProvider)
        )

        viewStore = store.view()

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
            btnDecrement.setOnClickListener { viewStore.send(decrementButtonTapped) }
            btnIncrement.setOnClickListener { viewStore.send(incrementButtonTapped) }
            btnNumberFact.setOnClickListener { viewStore.send(numberFactButtonTapped) }
        }
    }

    private val effectsBasicReducer =
        Reducer<EffectsBasicsState, EffectsBasicsAction, EffectsBasicsEnvironment> { state, action, env ->
            when (action) {
                decrementButtonTapped -> state.copy(
                    count = state.count - 1,
                    numberFact = null
                ) + Effects.just(incrementButtonTapped)
                    .delay(1, TimeUnit.SECONDS)
                    .observeOn(env.mainQueue.mainThread())
                    .cast()

                incrementButtonTapped -> state.copy(
                    count = state.count + 1,
                    numberFact = null
                ) + Effects.none()

                numberFactButtonTapped -> state.copy(
                    isNumberFactRequestInFlight = true,
                    numberFact = null
                    // Return an effect that fetches a number fact from the API and returns the
                    // value back to the reducer's `numberFactResponse` action.
                ) + env.fact.execute(state.count.toString())
                    .subscribeOn(env.mainQueue.io())
                    .observeOn(env.mainQueue.mainThread())
                    .map { numberFactResponse(it) }
                    .toEffect()
                    .cast()

                is numberFactResponse ->
                    when (action.response) {
                        is Result.Success -> state.copy(
                            numberFact = action.response.data,
                            isNumberFactRequestInFlight = false
                        ) + Effects.none()
                        is Result.Error -> state.copy(
                            numberFact = action.response.exception.toString(),
                            isNumberFactRequestInFlight = false
                        ) + Effects.none()
                    }
            }
        }

    override fun onDestroyView() {
        compositeDisposable.clear()
        super.onDestroyView()
    }
}