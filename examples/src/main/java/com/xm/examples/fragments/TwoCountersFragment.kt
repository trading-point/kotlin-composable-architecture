package com.xm.examples.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.xm.examples.MainActivity
import com.xm.examples.databinding.FragmentTwoCountersBinding
import com.xm.tka.ActionPrism
import com.xm.tka.Reducer
import com.xm.tka.StateLens
import com.xm.tka.Store
import com.xm.tka.ui.ViewStore
import com.xm.tka.ui.ViewStore.Companion.view
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo

class TwoCountersFragment : Fragment() {

    private lateinit var binding: FragmentTwoCountersBinding

    private var compositeDisposable = CompositeDisposable()

    private lateinit var viewStore: ViewStore<TwoCounterState, TwoCounterAction>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTwoCountersBinding.inflate(layoutInflater)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as MainActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val store = Store(
            initialState = TwoCounterState(),
            reducer = twoCountersReducer,
            environment = Unit
        )

        viewStore = store.view()

        observeFirstCounter()
        observeSecondCounter()
    }

    private fun observeFirstCounter() {
        viewStore.states
            .subscribe {
                binding.tvFirstNumber.text = it.firstCounter.count.toString()
            }
            .addTo(compositeDisposable)

        with(binding) {
            btnFirstDecrement.setOnClickListener {
                viewStore.send(TwoCounterAction.PullbackFirstCounter(CounterAction.DecrementButtonTapped))
            }
            btnFirstIncrement.setOnClickListener {
                viewStore.send(TwoCounterAction.PullbackFirstCounter(CounterAction.IncrementButtonTapped))
            }
        }
    }

    private fun observeSecondCounter() {
        viewStore.states
            .subscribe {
                binding.tvSecondNumber.text = it.secondCounter.count.toString()
            }
            .addTo(compositeDisposable)

        with(binding) {
            btnSecondDecrement.setOnClickListener {
                viewStore.send(TwoCounterAction.PullbackSecondCounter(CounterAction.DecrementButtonTapped))
            }
            btnSecondIncrement.setOnClickListener {
                viewStore.send(TwoCounterAction.PullbackSecondCounter(CounterAction.IncrementButtonTapped))
            }
        }
    }

    override fun onDestroyView() {
        compositeDisposable.clear()
        super.onDestroyView()
    }

    private val twoCountersReducer: Reducer<TwoCounterState, TwoCounterAction, Unit> =
        Reducer.combine(
            counterReducer.pullback(
                toLocalState = StateLens(
                    get = { it.firstCounter },
                    set = { state, update -> state.copy(firstCounter = update) }
                ),
                toLocalAction = ActionPrism(
                    get = { (it as? TwoCounterAction.PullbackFirstCounter)?.action },
                    reverseGet = { TwoCounterAction.PullbackFirstCounter(it) }
                ),
                toLocalEnvironment = { CounterEnvironment }
            ),
            counterReducer.pullback(
                toLocalState = StateLens(
                    get = { it.secondCounter },
                    set = { state, update -> state.copy(secondCounter = update) }
                ),
                toLocalAction = ActionPrism(
                    get = { (it as? TwoCounterAction.PullbackSecondCounter)?.action },
                    reverseGet = { TwoCounterAction.PullbackSecondCounter(it) }
                ),
                toLocalEnvironment = { CounterEnvironment }
            )
        )
}