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

    private val viewModel: TwoCountersViewModel by viewModels()
    private lateinit var viewStore: ViewStore<TwoCounterState, TwoCounterAction>

    override fun onAttach(context: Context) {
        super.onAttach(context)
        viewStore = viewModel.viewStore
    }

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

        // first counter
        viewStore.states
            .subscribe {
                binding.tvFirstNumber.text = it.firstCounter.count.toString()
            }
            .addTo(compositeDisposable)

        with(binding) {
            btnFirstDecrement.setOnClickListener {
                viewModel.scopeCounter1.send(CounterAction.DecrementButtonTapped)
            }
            btnFirstIncrement.setOnClickListener {
                viewModel.scopeCounter1.send(CounterAction.IncrementButtonTapped)
            }
        }

        // second counter
        viewStore.states
            .subscribe {
                binding.tvSecondNumber.text = it.secondCounter.count.toString()
            }
            .addTo(compositeDisposable)

        with(binding) {
            btnSecondDecrement.setOnClickListener {
                viewModel.scopeCounter2.send(CounterAction.DecrementButtonTapped)
            }
            btnSecondIncrement.setOnClickListener {
                viewModel.scopeCounter2.send(CounterAction.IncrementButtonTapped)
            }
        }
    }

    override fun onDestroyView() {
        compositeDisposable.clear()
        super.onDestroyView()
    }
}

class TwoCountersViewModel : ViewModel() {

    private val store = Store(
        initialState = TwoCounterState(),
        reducer = twoCountersReducer,
        environment = TwoCounterEnvironment
    )

    val viewStore: ViewStore<TwoCounterState, TwoCounterAction> = store.view()

    val scopeCounter1: Store<CounterState, CounterAction> = store.scopeCounter1()
    val scopeCounter2: Store<CounterState, CounterAction> = store.scopeCounter2()
}

// Domain

data class TwoCounterState(
    val firstCounter: CounterState = CounterState(),
    val secondCounter: CounterState = CounterState()
)

sealed class TwoCounterAction {
    data class Counter1(val action: CounterAction) : TwoCounterAction()
    data class Counter2(val action: CounterAction) : TwoCounterAction()
}

object TwoCounterEnvironment

private val twoCountersReducer: Reducer<TwoCounterState, TwoCounterAction, TwoCounterEnvironment> =
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
