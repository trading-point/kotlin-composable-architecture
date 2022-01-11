package com.xm.examples.fragments

/**
 * TwoCounters
 */
data class TwoCounterState(
    val firstCounter: CounterState = CounterState(),
    val secondCounter: CounterState = CounterState()
)

sealed class TwoCounterAction {
    data class PullbackFirstCounter(val action: CounterAction) : TwoCounterAction()
    data class PullbackSecondCounter(val action: CounterAction) : TwoCounterAction()
}

object TwoCounterEnvironment
