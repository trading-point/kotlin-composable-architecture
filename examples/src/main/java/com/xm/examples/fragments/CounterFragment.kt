package com.xm.examples.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import com.xm.examples.MainActivity
import com.xm.examples.databinding.FragmentCounterBinding
import com.xm.examples.fragments.CounterAction.DecrementButtonTapped
import com.xm.examples.fragments.CounterAction.IncrementButtonTapped
import com.xm.tka.Effects
import com.xm.tka.Reducer
import com.xm.tka.Store
import com.xm.tka.ui.ViewStore
import com.xm.tka.ui.ViewStore.Companion.view
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo

class CounterFragment : Fragment() {

    private lateinit var binding: FragmentCounterBinding

    private var compositeDisposable = CompositeDisposable()

    private val viewModel: CounterViewModel by viewModels()
    private lateinit var viewStore: ViewStore<CounterState, CounterAction>

    override fun onAttach(context: Context) {
        super.onAttach(context)
        viewStore = viewModel.viewStore
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCounterBinding.inflate(layoutInflater)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as MainActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)

        viewStore.states
            .subscribe {
                binding.tvNumber.text = it.count.toString()
            }
            .addTo(compositeDisposable)

        binding.btnDecrement.setOnClickListener { viewStore.send(DecrementButtonTapped) }
        binding.btnIncrement.setOnClickListener { viewStore.send(IncrementButtonTapped) }
    }

    override fun onDestroyView() {
        compositeDisposable.clear()
        super.onDestroyView()
    }
}

class CounterViewModel : ViewModel() {

    private val store = Store(
        initialState = CounterState(),
        reducer = counterReducer,
        environment = CounterEnvironment
    )

    val viewStore: ViewStore<CounterState, CounterAction> = store.view()
}

// Domain

data class CounterState(val count: Int = 0)

sealed class CounterAction {
    object DecrementButtonTapped : CounterAction()
    object IncrementButtonTapped : CounterAction()
}

object CounterEnvironment

val counterReducer = Reducer<CounterState, CounterAction, CounterEnvironment> { state, action, _ ->
    when (action) {
        DecrementButtonTapped -> state.copy(count = state.count - 1) + Effects.none()
        IncrementButtonTapped -> state.copy(count = state.count + 1) + Effects.none()
    }
}