package com.xm.tka.test

import com.xm.tka.Reducer

/**
 * Prints debug messages describing all received actions and state mutations.
 *
 * @param prefix: A string with which to prefix all debug messages.
 * @param printer: A print function. Defaults to a function that uses `println` function.
 * @return A reducer that prints debug messages for all received actions.
 */
fun <STATE, ACTION : Any, ENVIRONMENT> Reducer<STATE, ACTION, ENVIRONMENT>.debug(
    prefix: String = "",
    printer: Printer = Printer()
): Reducer<STATE, ACTION, ENVIRONMENT> = Reducer { state, action, environment ->
    val pref = if (prefix.isEmpty()) prefix else prefix.plus(": ")
    kotlin.runCatching {
        reduce(state, action, environment)
            .also { (newState, _) ->
                pref
                    .let {
                        """
                        ${it}Received:
                        $it  State:   $state
                        $it  Action:  $action
                        $it  Reduced: $newState
                        """.trimIndent()
                    }
                    .also(printer::print)
            }
    }.onFailure { printer.print("TKA: $prefix", it) }
        .getOrThrow()
}

interface Printer {

    fun print(message: String, error: Throwable? = null)

    companion object {
        operator fun invoke(): Printer = object : Printer {
            override fun print(message: String, error: Throwable?) {
                if (error == null) println(message)
                else System.err.println("$message: $error")
            }
        }
    }
}
