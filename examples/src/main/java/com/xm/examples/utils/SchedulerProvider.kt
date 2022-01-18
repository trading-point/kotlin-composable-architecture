package com.xm.examples.utils

import androidx.annotation.NonNull
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.internal.schedulers.ImmediateThinScheduler
import io.reactivex.rxjava3.schedulers.Schedulers

/**
 * Provides different types of schedulers.
 */
class BaseSchedulerProvider : SchedulerProvider {

    @NonNull
    override fun io(): Scheduler = Schedulers.io()

    @NonNull
    override fun mainThread(): Scheduler = AndroidSchedulers.mainThread()
}

interface SchedulerProvider {
    fun io(): Scheduler

    fun mainThread(): Scheduler
}