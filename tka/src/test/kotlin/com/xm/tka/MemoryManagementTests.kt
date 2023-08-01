package com.xm.tka

import com.xm.tka.Effects.none
import com.xm.tka.ui.ViewStore.Companion.view
import org.junit.Assert.assertEquals
import org.junit.Test

class MemoryManagementTests {

    @Test
    fun testOwnership_ScopeHoldsOntoParent() {
        val counterReducer = Reducer<Int, Unit, Unit> { state, _, _ ->
            (state + 1) + none()
        }

        val store = Store(0, counterReducer, Unit)
            .scope { "$it" }
            .scope { it.toInt() }
        val viewStore = store.view()

        var count = 0
        viewStore.states.subscribe { count = it }

        assertEquals(0, count)
        store.send(Unit)
        assertEquals(1, count)
    }

    @Test
    fun testOwnership_ViewStoreHoldsOntoStore() {
        val counterReducer = Reducer<Int, Unit, Unit> { state, _, _ ->
            (state + 1) + none()
        }

        val store = Store(0, counterReducer, Unit)
        val viewStore = store.view()

        var count = 0
        viewStore.states.subscribe { count = it }

        assertEquals(0, count)
        store.send(Unit)
        assertEquals(1, count)
    }
}
