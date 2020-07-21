package com.xm.tka

/**
 * A [StateLens] is an optic that can focus into a specific part of the state allowing to get/set/modify it
 *
 * @param GLOBAL_STATE the source state
 * @param LOCAL_STATE the focus state
 */
interface StateLens<GLOBAL_STATE, LOCAL_STATE> {

    fun get(state: GLOBAL_STATE): LOCAL_STATE
    fun set(state: GLOBAL_STATE, update: LOCAL_STATE): GLOBAL_STATE

    companion object {

        operator fun <GLOBAL_STATE, LOCAL_STATE> invoke(
            get: (GLOBAL_STATE) -> LOCAL_STATE,
            set: (GLOBAL_STATE, LOCAL_STATE) -> GLOBAL_STATE
        ): StateLens<GLOBAL_STATE, LOCAL_STATE> = object : StateLens<GLOBAL_STATE, LOCAL_STATE> {
            override fun get(state: GLOBAL_STATE): LOCAL_STATE = get(state)
            override fun set(state: GLOBAL_STATE, update: LOCAL_STATE): GLOBAL_STATE =
                set(state, update)
        }
    }
}

/**
 * A [ActionPrism] is a loss less invertible optic that can look into an action and optionally find its focus.
 *
 * @param GLOBAL_ACTION the source action
 * @param LOCAL_ACTION the focus action
 */
interface ActionPrism<GLOBAL_ACTION, LOCAL_ACTION> {

    fun get(action: GLOBAL_ACTION): LOCAL_ACTION?
    fun reverseGet(action: LOCAL_ACTION): GLOBAL_ACTION

    companion object {

        operator fun <GLOBAL_ACTION, LOCAL_ACTION> invoke(
            get: (GLOBAL_ACTION) -> LOCAL_ACTION?,
            reverseGet: (LOCAL_ACTION) -> GLOBAL_ACTION
        ): ActionPrism<GLOBAL_ACTION, LOCAL_ACTION> =
            object : ActionPrism<GLOBAL_ACTION, LOCAL_ACTION> {
                override fun get(action: GLOBAL_ACTION): LOCAL_ACTION? = get(action)
                override fun reverseGet(action: LOCAL_ACTION): GLOBAL_ACTION = reverseGet(action)
            }
    }
}

/**
 * A [Getter] is an optic that allows to see into a structure and getting a focus.
 *
 * @param A the source of a [Getter]
 * @param B the focus of a [Getter]
 */
typealias Getter<A, B> = (A) -> B
