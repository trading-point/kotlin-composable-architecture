package com.xm.examples.utils

import android.util.Log
import androidx.annotation.VisibleForTesting
import io.reactivex.rxjava3.core.Single
import java.net.URL

interface FactClient

class FactClientLive : FactClient {

    fun execute(number: String): Single<Result<String>> =
        Single.fromCallable {
            kotlin.runCatching { URL("http://numbersapi.com/$number/trivia").readText() }
                // Sometimes numbersapi.com can be flaky, so if it ever fails we will just
                // default to a mock response.
                .recover {
                    Log.i("Exception", "execute: $it")
                    "$number is a good number Brent"
                }
        }
}