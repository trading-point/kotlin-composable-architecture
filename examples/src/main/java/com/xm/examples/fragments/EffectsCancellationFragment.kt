package com.xm.examples.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.xm.examples.MainActivity
import com.xm.examples.databinding.FragmentCancellationBinding
import com.xm.examples.fragments.EffectsCancellationAction.CancelButtonTapped
import com.xm.examples.fragments.EffectsCancellationAction.StepperDecrement
import com.xm.examples.fragments.EffectsCancellationAction.StepperIncrement
import com.xm.examples.fragments.EffectsCancellationAction.TriviaButtonTapped
import com.xm.tka.ui.ViewStore
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo

class EffectsCancellationFragment : Fragment() {

    private lateinit var binding: FragmentCancellationBinding

    private val compositeDisposable = CompositeDisposable()

    private val viewModel: EffectsCancellationViewModel by viewModels()
    private lateinit var viewStore: ViewStore<EffectCancellationState, EffectsCancellationAction>

    override fun onAttach(context: Context) {
        super.onAttach(context)
        viewStore = viewModel.viewStore
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCancellationBinding.inflate(layoutInflater)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as MainActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)

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
                viewStore.send(StepperDecrement(tvNumber.text.toString().toInt()))
            }
            btnIncrement.setOnClickListener {
                viewStore.send(StepperIncrement(tvNumber.text.toString().toInt()))
            }
            btnNumberFact.setOnClickListener { viewStore.send(TriviaButtonTapped) }
            btnCancel.setOnClickListener { viewStore.send(CancelButtonTapped) }
        }
    }

    override fun onDestroyView() {
        compositeDisposable.clear()
        super.onDestroyView()
    }
}