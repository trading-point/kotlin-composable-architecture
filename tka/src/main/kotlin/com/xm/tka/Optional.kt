@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package com.xm.tka

import java.util.Optional

fun <STATE : Any> STATE?.toOptional(): Optional<STATE> = Optional.ofNullable(this)

fun <T : Any?> Optional<T>.orNull(): T? = this.orElse(null)
