package com.xm.examples.utils

import android.util.Log
import io.reactivex.rxjava3.core.Single
import java.net.URL

interface FactClient

object FactClientLive : FactClient {

    fun execute(number: String): Single<Result<String>> =
        Single.fromCallable {
            try {
                val response = URL("http://numbersapi.com/$number/trivia").readText()
                Result.success(response)
            } catch (e: Exception) {
                Log.i("Exception", "execute: $e ")
                // Sometimes numbersapi.com can be flakey, so if it ever fails we will just
                // default to a mock response.
                Result.success("$number is a good number Brent")
            }
        }
}