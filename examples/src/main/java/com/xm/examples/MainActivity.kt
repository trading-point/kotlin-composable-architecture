package com.xm.examples

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.xm.examples.databinding.ActivityMainBinding
import com.xm.examples.cases.EffectsCancellation
import com.xm.examples.cases.GettingStartedCounter
import com.xm.examples.cases.EffectsBasic
import com.xm.examples.cases.GettingStartedCompositionTwoCountersCompose
import com.xm.examples.cases.GettingStartedCompositionTwoCounters

class MainActivity : AppCompatActivity(), Callback {

    private lateinit var binding: ActivityMainBinding

    private lateinit var mainFragment: MainFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mainFragment = MainFragment()
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.main_container, mainFragment, null)
                .addToBackStack(null)
                .commit()
        }

        mainFragment.setupCallback(this)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        if (supportFragmentManager.backStackEntryCount == 0) {
            finish()
        }
    }

    override fun onBasicClicked() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.main_container, GettingStartedCounter(), null)
            .addToBackStack(null)
            .commit()
    }

    override fun onTwoCountersClicked() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.main_container, GettingStartedCompositionTwoCounters(), null)
            .addToBackStack(null)
            .commit()
    }

    override fun onTwoCountersComposeClicked() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.main_container, GettingStartedCompositionTwoCountersCompose(), null)
            .addToBackStack(null)
            .commit()
    }

    override fun onEffectsBasicClicked() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.main_container, EffectsBasic(), null)
            .addToBackStack(null)
            .commit()
    }

    override fun onCancellationClicked() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.main_container, EffectsCancellation(), null)
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