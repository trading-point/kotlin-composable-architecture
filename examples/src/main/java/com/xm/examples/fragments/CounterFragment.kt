package com.xm.examples.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.xm.examples.MainActivity
import com.xm.examples.databinding.FragmentCounterBinding
import com.xm.examples.fragments.CounterAction.decrementButtonTapped
import com.xm.examples.fragments.CounterAction.incrementButtonTapped
import com.xm.tka.Store
import com.xm.tka.ui.ViewStore.Companion.view
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo

class CounterFragment : Fragment() {

    private lateinit var binding: FragmentCounterBinding

    private var compositeDisposable = CompositeDisposable()

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

        val store = Store(
            initialState = CounterState(),
            reducer = counterReducer,
            environment = CounterEnvironment
        )

        observe(store)
    }


    private fun observe(store: Store<CounterState, CounterAction>) {
        val viewStore = store.view()

        viewStore.states
            .subscribe {
                binding.tvNumber.text = it.count.toString()
            }
            .addTo(compositeDisposable)

        binding.btnDecrement.setOnClickListener { viewStore.send(decrementButtonTapped) }
        binding.btnIncrement.setOnClickListener { viewStore.send(incrementButtonTapped) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        compositeDisposable.clear()
    }
}