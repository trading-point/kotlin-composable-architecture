package com.xm.examples

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.xm.examples.databinding.FragmentMainBinding
import com.xm.examples.fragments.CounterFragment

class MainFragment : Fragment() {

    private lateinit var binding: FragmentMainBinding

    lateinit var callback: Callback

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

        binding.btnBasics.setOnClickListener { callback.onBasicClicked() }
    }

    fun setupCallback(callback: MainActivity) {
        this.callback = callback
    }
}


interface Callback {

    fun onBasicClicked()

}