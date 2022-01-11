package com.xm.examples.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.xm.examples.MainActivity
import com.xm.examples.databinding.FragmentCounterBinding
import com.xm.examples.fragments.CounterAction.DecrementButtonTapped
import com.xm.examples.fragments.CounterAction.IncrementButtonTapped
import com.xm.tka.ui.ViewStore
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