package com.xm.examples.cases

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.xm.examples.MainActivity
import com.xm.examples.cases.CounterAction.DecrementButtonTapped
import com.xm.examples.cases.CounterAction.IncrementButtonTapped
import com.xm.examples.databinding.FragmentCounterBinding
import com.xm.tka.Effects
import com.xm.tka.Reducer
import com.xm.tka.Store
import com.xm.tka.ui.ViewStore
import com.xm.tka.ui.ViewStore.Companion.view
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import com.xm.examples.R

private val readMe = """
  This screen demonstrates the basics of the Composable Architecture in an archetypal counter application.
  
  The domain of the application is modeled using simple data types that correspond to the mutable
  state of the application and any actions that can affect that state or the outside world.
""".trimIndent()

class GettingStartedCounter : Fragment() {

    private lateinit var binding: FragmentCounterBinding

    private var compositeDisposable = CompositeDisposable()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCounterBinding.inflate(layoutInflater)
        binding.readme.text = readMe
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as MainActivity).supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = resources.getString(R.string.getting_started_counter_toolbar_title)
        }

        viewStore.states
            .subscribe {
                binding.counter.number.text = it.count.toString()
            }
            .addTo(compositeDisposable)

        binding.counter.decrement.setOnClickListener { viewStore.send(DecrementButtonTapped) }
        binding.counter.increment.setOnClickListener { viewStore.send(IncrementButtonTapped) }
    }

    override fun onDestroyView() {
        compositeDisposable.clear()
        super.onDestroyView()
    }

    companion object {
        private val store = Store(
            initialState = CounterState(),
            reducer = counterReducer,
            environment = CounterEnvironment
        )

        // ViewStore could be provided by a ViewModel or directly injected via a DI framework
        val viewStore: ViewStore<CounterState, CounterAction> = store.view()
    }
}

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