package com.xm.examples

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.xm.examples.databinding.FragmentMainBinding

class MainFragment : Fragment() {

    private lateinit var binding: FragmentMainBinding

    private val viewModel: SharedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMainBinding.inflate(layoutInflater)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as MainActivity).supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(false)
            title = resources.getString(R.string.main_fragment_toolbar_title)
        }

        with(binding) {
            btnBasics.setOnClickListener { viewModel.onItemClicked(Screen.Counter) }
            btnPullback.setOnClickListener { viewModel.onItemClicked(Screen.TwoCounters) }
            btnPullbackCompose.setOnClickListener { viewModel.onItemClicked(Screen.TwoCountersCompose) }
            btnEffects.setOnClickListener { viewModel.onItemClicked(Screen.EffectsBasic) }
            btnCancellation.setOnClickListener { viewModel.onItemClicked(Screen.EffectsCancellation) }
        }
    }
}

sealed class Screen {
    object Counter : Screen()
    object TwoCounters : Screen()
    object TwoCountersCompose : Screen()
    object EffectsBasic : Screen()
    object EffectsCancellation : Screen()
}