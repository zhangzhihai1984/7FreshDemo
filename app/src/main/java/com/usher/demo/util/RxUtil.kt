package com.usher.demo.util

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import autodispose2.AutoDispose
import autodispose2.AutoDisposeConverter
import autodispose2.androidx.lifecycle.AndroidLifecycleScopeProvider
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.ObservableTransformer
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.concurrent.TimeUnit

object RxUtil {
    fun <T> autoDispose(owner: LifecycleOwner): AutoDisposeConverter<T> =
        AutoDispose.autoDisposable(
            AndroidLifecycleScopeProvider.from(
                owner,
                Lifecycle.Event.ON_DESTROY
            )
        )

    fun <T> getSchedulerComposer(): ObservableTransformer<T, T> =
        ObservableTransformer { upstream ->
            upstream
                .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
        }

    fun <T> singleClick(): ObservableTransformer<T, T> =
        ObservableTransformer { upstream -> upstream.throttleFirst(500, TimeUnit.MILLISECONDS) }
}