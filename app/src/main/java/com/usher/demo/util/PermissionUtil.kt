package com.usher.demo.util

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.PublishSubject

object PermissionUtil {
    private val mPermissionSubject = PublishSubject.create<Boolean>()

    fun requestPermission(context: Context, permission: String): Observable<Boolean> = requestPermissions(context, arrayOf(permission))

    fun requestPermissions(context: Context, permissions: Array<String>): Observable<Boolean> {
        val permissionList = permissions.filter { PackageManager.PERMISSION_DENIED == ContextCompat.checkSelfPermission(context, it) }

        return if (permissionList.isNotEmpty()) {
            ActivityCompat.requestPermissions(context as Activity, permissionList.toTypedArray(), 0)
            mPermissionSubject
        } else {
            Observable.just(true)
        }
    }

    fun grant() = mPermissionSubject.onNext(true)

    fun deny() = mPermissionSubject.onNext(false)
}