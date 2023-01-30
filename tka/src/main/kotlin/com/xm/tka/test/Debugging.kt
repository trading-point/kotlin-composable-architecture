package com.xm.tka.test

import com.xm.tka.Reducer

/**
 * Prints debug messages describing all received actions and state mutations.
 *
 * @param prefix: A string with which to prefix all debug messages.
 * @param printer: A print function. Defaults to a function that uses `println` function.
 * @param actionsOnly: A boolean switch between action logging only or full log
 * @return A reducer that prints debug messages for all received actions.
 */
fun <STATE, ACTION : Any, ENVIRONMENT> Reducer<STATE, ACTION, ENVIRONMENT>.debug(
    prefix: String = "",
    actionsOnly: Boolean = false,
    printer: Printer = Printer()
): Reducer<STATE, ACTION, ENVIRONMENT> = Reducer { state, action, environment ->
    val pref = if (prefix.isEmpty()) prefix else prefix.plus(": ")
    kotlin.runCatching {
        reduce(state, action, environment)
            .also { (newState, _) ->
                pref
                    .let {
                        if (actionsOnly) {
                            """
                            ${it}Received: $action
                            """.trimIndent()
                        } else {
                            """
                            ${it}Received:
                            $it  State:   $state
                            $it  Action:  $action
                            $it  Reduced: $newState
                            """.trimIndent()
                        }
                    }
                    .also(printer::print)
            }
    }.onFailure { printer.print("TKA: $prefix", it) }
        .getOrThrow()
}

/**
 * [Reducer] extension to verify [com.xm.tka.Effect]s are dispatched on a main thread
 */
fun <STATE : Any, ACTION : Any, ENVIRONMENT : Any> Reducer<STATE, ACTION, ENVIRONMENT>.verifyDispatchOnMainThread(
    mainThreadProvider: () -> Thread
): Reducer<STATE, ACTION, ENVIRONMENT> = Reducer { state, action, environment ->
    assert(Thread.currentThread() == mainThreadProvider()) {
        "$action delivered on ${Thread.currentThread()}!!"
    }
    reduce(state, action, environment)
}

interface Printer {

    fun print(message: String, error: Throwable? = null)

    companion object {
        operator fun invoke(): Printer = object : Printer {
            override fun print(message: String, error: Throwable?) {
                if (error == null) {
                    println(message)
                } else {
                    System.err.println("$message: $error")
                }
            }
        }
    }
}
