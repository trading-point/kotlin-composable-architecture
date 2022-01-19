package com.xm.examples

import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.PublishSubject

class SharedViewModel : ViewModel() {
    private var selected: PublishSubject<Screen> = PublishSubject.create()
    val selectedScreen: Observable<Screen> = selected.hide()

    fun onItemClicked(screen: Screen) {
        selected.onNext(screen)
    }
}