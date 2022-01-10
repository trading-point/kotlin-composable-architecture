package com.xm.examples

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.xm.examples.databinding.ActivityMainBinding
import com.xm.examples.fragments.CounterFragment
import com.xm.examples.fragments.EffectsBasicFragment
import com.xm.examples.fragments.TwoCountersFragment

class MainActivity : AppCompatActivity(), Callback {

    private lateinit var binding: ActivityMainBinding

    private lateinit var mainFragment: MainFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mainFragment = MainFragment()
        supportFragmentManager.beginTransaction()
            .add(R.id.main_container, mainFragment, null)
            .addToBackStack(null)
            .commit()

        mainFragment.setupCallback(this)
    }

    override fun onBasicClicked() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.main_container, CounterFragment(), null)
            .addToBackStack(null)
            .commit()
    }

    override fun onTwoCountersClicked() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.main_container, TwoCountersFragment(), null)
            .addToBackStack(null)
            .commit()
    }

    override fun onEffectsBasicClicked() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.main_container, EffectsBasicFragment(), null)
            .addToBackStack(null)
            .commit()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }
}