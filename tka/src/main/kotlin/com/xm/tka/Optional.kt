@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package com.xm.tka

/**
 * An Optional wrapper for a state
 */
class Optional<out STATE : Any> private constructor(
    private val state: STATE?
) {

    val isPresent = state != null

    fun get(): STATE = requireNotNull(state)

    companion object {

        fun <STATE : Any> STATE?.toOptional(): Optional<STATE> = Optional(this)
    }
}
