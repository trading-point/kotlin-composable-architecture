package com.xm.examples

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.xm.examples.databinding.ActivityMainBinding
import com.xm.examples.fragments.CounterFragment

class MainActivity : AppCompatActivity(), Callback {

    private lateinit var binding: ActivityMainBinding

    private lateinit var mainFragment: MainFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mainFragment = MainFragment()
        supportFragmentManager.beginTransaction()
            .add(R.id.container, mainFragment, null)
            .addToBackStack(null)
            .commit()

        mainFragment.setupCallback(this)

    }

    override fun onBasicClicked() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, CounterFragment(), null)
            .addToBackStack(null)
            .commit()
    }
}