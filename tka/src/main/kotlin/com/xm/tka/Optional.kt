@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package com.xm.tka

/**
 * An Optional wrapper for a state
 */
class Optional<out STATE : Any> private constructor(
    private val state: STATE?
) {

    val isPresent = state != null

    fun orNull(): STATE? = state

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Optional<*>

        if (state != other.state) return false

        return true
    }

    override fun hashCode(): Int {
        return state?.hashCode() ?: 0
    }

    companion object {

        fun <STATE : Any> STATE?.toOptional(): Optional<STATE> = Optional(this)
    }
}
