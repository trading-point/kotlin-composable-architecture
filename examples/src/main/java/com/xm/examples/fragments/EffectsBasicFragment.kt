package com.xm.examples.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.xm.examples.MainActivity
import com.xm.examples.databinding.FragmentEffectsBasicBinding
import com.xm.examples.fragments.EffectsBasicsAction.DecrementButtonTapped
import com.xm.examples.fragments.EffectsBasicsAction.IncrementButtonTapped
import com.xm.examples.fragments.EffectsBasicsAction.NumberFactButtonTapped
import com.xm.tka.ui.ViewStore
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo

class EffectsBasicFragment : Fragment() {

    private lateinit var binding: FragmentEffectsBasicBinding

    private val compositeDisposable = CompositeDisposable()

    private val viewModel: EffectsBasicViewModel by viewModels()
    private lateinit var viewStore: ViewStore<EffectsBasicsState, EffectsBasicsAction>

    override fun onAttach(context: Context) {
        super.onAttach(context)
        viewStore = viewModel.viewStore
    }

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
            btnDecrement.setOnClickListener { viewStore.send(DecrementButtonTapped) }
            btnIncrement.setOnClickListener { viewStore.send(IncrementButtonTapped) }
            btnNumberFact.setOnClickListener { viewStore.send(NumberFactButtonTapped) }
        }
    }

    override fun onDestroyView() {
        compositeDisposable.clear()
        super.onDestroyView()
    }
}