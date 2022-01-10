package com.xm.examples.utils

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.schedulers.Schedulers

/**
 * Provides different types of schedulers.
 */
class SchedulerProvider private constructor(/* Prevent direct instantiation. */) {

    @NonNull
    fun io(): Scheduler = Schedulers.io()

    @NonNull
    fun mainThread(): Scheduler = AndroidSchedulers.mainThread()

    companion object {
        @Nullable
        private var INSTANCE: SchedulerProvider? = null

        @get:Synchronized
        val instance: SchedulerProvider
            get() {
                if (INSTANCE == null) {
                    INSTANCE = SchedulerProvider()
                }
                return INSTANCE!!
            }
    }
}