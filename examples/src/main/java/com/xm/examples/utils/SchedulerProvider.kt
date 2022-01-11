package com.xm.examples.utils

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.schedulers.Schedulers

/**
 * Provides different types of schedulers.
 */
object SchedulerProvider {

    @NonNull
    fun io(): Scheduler = Schedulers.io()

    @NonNull
    fun mainThread(): Scheduler = AndroidSchedulers.mainThread()
}