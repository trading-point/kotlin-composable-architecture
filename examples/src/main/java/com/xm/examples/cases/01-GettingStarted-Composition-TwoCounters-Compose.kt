package com.xm.examples.cases

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rxjava3.subscribeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy.DisposeOnLifecycleDestroyed
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.xm.examples.MainActivity
import com.xm.examples.R
import com.xm.tka.ui.ViewStore
import com.xm.tka.ui.ViewStore.Companion.view

private val readMe = """
  This screen demonstrates how to take small features and compose them into bigger ones using the
  `pullback` and `combine` operators on reducers, and the `scope` operator on stores.
  
  It reuses the domain of the counter screen and embeds it, twice, in a larger domain.
""".trimIndent()

class GettingStartedCompositionTwoCountersCompose : Fragment() {

    private val viewModel: TwoCountersViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(DisposeOnLifecycleDestroyed(viewLifecycleOwner))

            setContent {
                MaterialTheme {
                    TwoCountersScreen(viewModel)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as MainActivity).supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = resources.getString(R.string.getting_started_two_counters_compose_toolbar_title)
        }
    }
}

@Composable
fun TwoCountersScreen(viewModel: TwoCountersViewModel) {

    Column(Modifier.padding(horizontal = 22.dp)) {
        Text(readMe.uppercase(), fontSize = 14.sp, color = colorResource(R.color.grey))

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Counter 1")
            CounterScreenView(viewModel.store.scopeCounter1().view())
        }
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Counter 2")
            CounterScreenView(viewModel.store.scopeCounter2().view())
        }
    }
}

@Composable
fun CounterScreenView(viewStore: ViewStore<CounterState, CounterAction>) {
    val counter: Int = viewStore.states.subscribeAsState(viewStore.currentState).value.count

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { viewStore.send(CounterAction.DecrementButtonTapped) }) {
                Text("-", fontSize = 22.sp)
            }

            Text(counter.toString(), fontSize = 22.sp)

            TextButton(onClick = { viewStore.send(CounterAction.IncrementButtonTapped) }) {
                Text("+", fontSize = 22.sp)
            }
        }
    }
}
