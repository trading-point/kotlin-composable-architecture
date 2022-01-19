package com.xm.examples.cases

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
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
import com.xm.examples.R

private val readMe = """
  This screen demonstrates how to take small features and compose them into bigger ones using the
  `pullback` and `combine` operators on reducers, and the `scope` operator on stores.
  
  It reuses the domain of the counter screen and embeds it, twice, in a larger domain.
""".trimIndent()

class GettingStartedCompositionTwoCounters : Fragment() {

    private lateinit var binding: FragmentTwoCountersBinding

    private var compositeDisposable = CompositeDisposable()

    private val viewModel: TwoCountersViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTwoCountersBinding.inflate(layoutInflater)
        binding.readme.text = readMe
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as MainActivity).supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = resources.getString(R.string.getting_started_two_counters_toolbar_title)
        }

        val viewStore = viewModel.store.view()
        val scopeCounter1 = viewModel.store.scopeCounter1()
        val scopeCounter2 = viewModel.store.scopeCounter2()

        // first counter
        viewStore.states
            .subscribe {
                binding.firstNumber.text = it.firstCounter.count.toString()
            }
            .addTo(compositeDisposable)

        with(binding) {
            firstDecrement.setOnClickListener {
                scopeCounter1.send(CounterAction.DecrementButtonTapped)
            }
            firstIncrement.setOnClickListener {
                scopeCounter1.send(CounterAction.IncrementButtonTapped)
            }
        }

        // second counter
        viewStore.states
            .subscribe {
                binding.secondNumber.text = it.secondCounter.count.toString()
            }
            .addTo(compositeDisposable)

        with(binding) {
            secondDecrement.setOnClickListener {
                scopeCounter2.send(CounterAction.DecrementButtonTapped)
            }
            secondIncrement.setOnClickListener {
                scopeCounter2.send(CounterAction.IncrementButtonTapped)
            }
        }
    }

    override fun onDestroyView() {
        compositeDisposable.clear()
        super.onDestroyView()
    }
}

class TwoCountersViewModel : ViewModel() {

    val store = Store(
        initialState = TwoCounterState(),
        reducer = twoCountersReducer,
        environment = TwoCounterEnvironment
    )
}

data class TwoCounterState(
    val firstCounter: CounterState = CounterState(),
    val secondCounter: CounterState = CounterState()
)

sealed class TwoCounterAction {
    data class Counter1(val action: CounterAction) : TwoCounterAction()
    data class Counter2(val action: CounterAction) : TwoCounterAction()
}

object TwoCounterEnvironment

val twoCountersReducer: Reducer<TwoCounterState, TwoCounterAction, TwoCounterEnvironment> =
    Reducer.combine(
        counterReducer.pullback(
            toLocalState = StateLens(
                get = { it.firstCounter },
                set = { state, update -> state.copy(firstCounter = update) }
            ),
            toLocalAction = ActionPrism(
                get = { (it as? TwoCounterAction.Counter1)?.action },
                reverseGet = { TwoCounterAction.Counter1(it) }
            ),
            toLocalEnvironment = { CounterEnvironment }
        ),
        counterReducer.pullback(
            toLocalState = StateLens(
                get = { it.secondCounter },
                set = { state, update -> state.copy(secondCounter = update) }
            ),
            toLocalAction = ActionPrism(
                get = { (it as? TwoCounterAction.Counter2)?.action },
                reverseGet = { TwoCounterAction.Counter2(it) }
            ),
            toLocalEnvironment = { CounterEnvironment }
        )
    )

internal fun Store<TwoCounterState, TwoCounterAction>.scopeCounter1(): Store<CounterState, CounterAction> =
    this.scope(
        toLocalState = { it.firstCounter },
        fromLocalAction = { TwoCounterAction.Counter1(it) }
    )

internal fun Store<TwoCounterState, TwoCounterAction>.scopeCounter2(): Store<CounterState, CounterAction> =
    this.scope(
        toLocalState = { it.secondCounter },
        fromLocalAction = { TwoCounterAction.Counter2(it) }
    )
