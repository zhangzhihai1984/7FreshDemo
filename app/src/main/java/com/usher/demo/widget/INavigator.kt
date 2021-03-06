package com.usher.demo.widget

import io.reactivex.rxjava3.core.Observable

interface INavigator {
    fun clear()
    fun updateTabs(tag: String)
    fun navigates(): Observable<String>
}