package com.xm.examples

import android.os.Bundle
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.xm.examples.cases.EffectsBasic
import com.xm.examples.cases.EffectsCancellation
import com.xm.examples.cases.GettingStartedCompositionTwoCounters
import com.xm.examples.cases.GettingStartedCompositionTwoCountersCompose
import com.xm.examples.cases.GettingStartedCounter
import com.xm.examples.databinding.ActivityMainBinding
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val viewModel: SharedViewModel by viewModels()
    private val compositeDisposable = CompositeDisposable()

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

        viewModel.selectedScreen.subscribe {
            when (it) {
                Screen.Counter -> goToScreen(GettingStartedCounter())
                Screen.TwoCounters -> goToScreen(GettingStartedCompositionTwoCounters())
                Screen.TwoCountersCompose -> goToScreen(GettingStartedCompositionTwoCountersCompose())
                Screen.EffectsBasic -> goToScreen(EffectsBasic())
                Screen.EffectsCancellation -> goToScreen(EffectsCancellation())
            }
        }.addTo(compositeDisposable)
    }

    private fun goToScreen(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.main_container, fragment, null)
            .addToBackStack(null)
            .commit()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        if (supportFragmentManager.backStackEntryCount == 0) {
            finish()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        compositeDisposable.clear()
        super.onDestroy()
    }
}