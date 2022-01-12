package com.xm.examples.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
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