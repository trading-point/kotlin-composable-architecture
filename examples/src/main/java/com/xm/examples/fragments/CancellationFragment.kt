package com.xm.examples.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.xm.examples.MainActivity
import com.xm.examples.databinding.FragmentCancellationBinding
import com.xm.examples.fragments.EffectsCancellationAction.stepperChanged
import com.xm.examples.fragments.EffectsCancellationAction.triviaButtonTapped
import com.xm.examples.fragments.EffectsCancellationAction.triviaResponse
import com.xm.examples.utils.FactClientLive
import com.xm.examples.utils.Result
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
import io.reactivex.rxjava3.kotlin.cast

class CancellationFragment : Fragment() {

    private lateinit var binding: FragmentCancellationBinding

    private val compositeDisposable = CompositeDisposable()

    private lateinit var viewStore: ViewStore<EffectCancellationState, EffectsCancellationAction>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCancellationBinding.inflate(layoutInflater)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as MainActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val store = Store(
            initialState = EffectCancellationState(),
            reducer = effectsCancellationReducer,
            environment = EffectsCancellationEnvironment(FactClientLive, SchedulerProvider.instance)
        )

        viewStore = store.view()

        viewStore.states
            .subscribe {
                with(binding) {
                    tvNumber.text = it.count.toString()
                    tvText.text = it.currentTrivia ?: ""

                    btnCancel.visibility =
                        if (it.isTriviaRequestInFlight) View.VISIBLE else View.GONE
                }
            }
            .addTo(compositeDisposable)

        with(binding) {
            btnDecrement.setOnClickListener {
                viewStore.send(stepperChanged(tvNumber.text.toString().toInt()))
            }
            btnIncrement.setOnClickListener {
                viewStore.send(stepperChanged(tvNumber.text.toString().toInt()))
            }
            btnNumberFact.setOnClickListener { viewStore.send(triviaButtonTapped) }
            btnCancel.setOnClickListener { viewStore.send(EffectsCancellationAction.cancelButtonTapped) }
        }
    }

    private val effectsCancellationReducer =
        Reducer<EffectCancellationState, EffectsCancellationAction, EffectsCancellationEnvironment> { state, action, env ->
            when (action) {
                is stepperChanged -> state.copy(
                    count = action.num + 1,
                    currentTrivia = null,
                    isTriviaRequestInFlight = false
                ) + Effects.none()

                EffectsCancellationAction.cancelButtonTapped -> state.copy(
                    isTriviaRequestInFlight = false
                ) + Effects.cancel(TriviaRequestId)

                triviaButtonTapped -> state.copy(
                    currentTrivia = null,
                    isTriviaRequestInFlight = true
                ) + env.fact.execute(state.count.toString())
                    .subscribeOn(env.mainQueue.io())
                    .observeOn(env.mainQueue.mainThread())
                    .map { triviaResponse(it) }
                    .toEffect()
                    .cancellable(TriviaRequestId)
                    .cast()

                is triviaResponse ->
                    when (action.response) {
                        is Result.Success -> state.copy(
                            currentTrivia = action.response.data,
                            isTriviaRequestInFlight = false
                        ) + Effects.none()
                        is Result.Error -> state.copy(
                            isTriviaRequestInFlight = false
                        ) + Effects.none()
                    }
            }
        }

    override fun onDestroyView() {
        compositeDisposable.clear()
        super.onDestroyView()
    }
}