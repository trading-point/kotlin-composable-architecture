package com.xm.examples.fragments

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.xm.examples.R
import com.xm.tka.ui.ViewStore
import com.xm.tka.ui.ViewStore.Companion.view

class TwoCountersComposeFragment : Fragment() {

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
}

@Composable
fun TwoCountersScreen(viewModel: TwoCountersViewModel) {

    Column(Modifier.padding(horizontal = 22.dp)) {
        Text(stringResource(R.string.two_counters_text).uppercase(), fontSize = 18.sp)

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Counter 1")
            CounterScreenView(viewModel.scopeCounter1.view())
        }
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Counter 2")
            CounterScreenView(viewModel.scopeCounter2.view())
        }
    }
}

@Composable
fun CounterScreenView(viewStore: ViewStore<CounterState, CounterAction>) {
    val counter: Int = viewStore.states.subscribeAsState(CounterState(0)).value.count

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
