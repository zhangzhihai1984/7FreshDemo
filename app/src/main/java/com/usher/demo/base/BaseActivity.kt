package com.usher.demo.base

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.usher.demo.util.PermissionUtil
import com.usher.demo.widget.CommonDialog
import com.usher.demo.widget.CustomProgressDialog
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.PublishSubject

@SuppressLint("Registered")
open class BaseActivity(contentLayoutId: Int, private val statusBarThemeForDayMode: Theme = Theme.DARK_AUTO) : AppCompatActivity(contentLayoutId) {
    private val mContextMenuSubject = PublishSubject.create<MenuItem>()
    private val mActivityResultSubject = PublishSubject.create<ActivityResult>()
    private var mIsLocalNightMode = false
    private val mLoadingDialog by lazy { CustomProgressDialog(this) }

    private var lightStatusBarTheme = true
        set(isLight) {
            field = isLight

            val decorView = window.decorView
            val visibility = decorView.systemUiVisibility

            decorView.systemUiVisibility = when (isLight) {
                true -> visibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                false -> visibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
            }
        }

    private val isSystemNightMode: Boolean
        get() = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

    enum class Theme(val isLight: Boolean, val isAuto: Boolean) {
        LIGHT_AUTO(true, true),
        DARK_AUTO(false, true),
        LIGHT_ONLY(true, false),
        DARK_ONLY(false, false)
    }

    data class ActivityResult(val requestCode: Int, val resultCode: Int, val data: Intent)

    final override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mIsLocalNightMode = isSystemNightMode

        //TODO:
        lightStatusBarTheme = statusBarThemeForDayMode.isLight
//        updateStatusBarTheme(mIsLocalNightMode)

        initData()
        initView()
    }

    open fun initData() {}

    open fun initView() {}

    override fun onDestroy() {
        super.onDestroy()
        dismissLoadingDialog()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) =
            grantResults.find { it == PackageManager.PERMISSION_DENIED }?.let {
                PermissionUtil.deny()
                CommonDialog(this)
                        .withContent("未取得应用权限，您将不能使用该功能")
                        .withDialogType(CommonDialog.ButtonType.SINGLE)
                        .show()
            } ?: run {
                PermissionUtil.grant()
            }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        mContextMenuSubject.onNext(item)
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        mActivityResultSubject.onNext(ActivityResult(requestCode, resultCode, data ?: Intent()))
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        val isCurrentNightMode = isSystemNightMode

        if (mIsLocalNightMode != isCurrentNightMode)
            onUIModeChanged(isCurrentNightMode)

        mIsLocalNightMode = isCurrentNightMode
    }

    /**
     * statusbar的theme有light和dark之分, 同时也有auto和only之分.
     *
     * 1. 如果是only的话, 表明不关心当前系统ui mode为day还是night, 保持设置的light或night不变.
     *
     * 2. 如果是auto的话, 表明需要根据当前系统ui mode为day还是night进行相应的切换.
     * (1) 如果当前为day mode, 保持设置的light或night不变.
     * (2) 如果当前为night mode, 如果设置的是light, 变为dark, 如果设置的dark, 变为light
     */
    fun updateStatusBarTheme(isNightMode: Boolean) {
        lightStatusBarTheme = when {
            !statusBarThemeForDayMode.isAuto || !isNightMode -> statusBarThemeForDayMode.isLight
            statusBarThemeForDayMode.isLight -> false
            else -> true
        }
    }

    open fun onUIModeChanged(isNightMode: Boolean) {
        //TODO:
//        startActivity(Intent(this, SplashActivity::class.java).apply {
//            flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
//        })
    }

    fun showLoadingDialog(isTouchAble: Boolean = false) {
        mLoadingDialog.setTouchAble(isTouchAble)
        mLoadingDialog.show()
    }

    fun dismissLoadingDialog() {
        if (mLoadingDialog.isShowing)
            mLoadingDialog.dismiss()
    }

    fun isLoadingDialogShowing() = mLoadingDialog.isShowing

    fun showToast(text: String?) = Toast.makeText(this, text ?: "", Toast.LENGTH_SHORT).show()

    fun setBackgroundAlpha(alpha: Float) {
        val lp = window.attributes
        lp.alpha = alpha

        if (alpha == 1f) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        }

        window.attributes = lp
    }

    fun contextMenuClicks(): Observable<MenuItem> = mContextMenuSubject

    fun activityResult(): Observable<ActivityResult> = mActivityResultSubject
}