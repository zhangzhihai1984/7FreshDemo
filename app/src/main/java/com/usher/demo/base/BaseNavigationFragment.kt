package com.usher.demo.base

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.PublishSubject

abstract class BaseNavigationFragment(private var layoutResId: Int) : Fragment() {
    private val mHiddenSubject = PublishSubject.create<Boolean>()
    var statusBarTheme = Theme.DARK
        set(theme) {
            field = theme

            val decorView = requireActivity().window.decorView
            val visibility = decorView.systemUiVisibility

            decorView.systemUiVisibility = when (theme.isLight) {
                true -> visibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                else -> visibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
            }
        }

    var statusBarColor = Color.TRANSPARENT
        set(color) {
            field = color
            requireActivity().window.statusBarColor = color
        }

    enum class Theme(val isLight: Boolean) {
        LIGHT(true),
        DARK(false)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? = inflater.inflate(layoutResId, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) = init()

    abstract fun init()

    override fun onHiddenChanged(hidden: Boolean) = mHiddenSubject.onNext(hidden)

    fun onHiddenChanged(): Observable<Boolean> = mHiddenSubject

    fun showToast(text: String?) = Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
}