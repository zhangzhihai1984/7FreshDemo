package com.usher.demo.zxing

import android.app.AlertDialog
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.view.SurfaceHolder
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import androidx.core.view.updateLayoutParams
import com.google.zxing.Result
import com.google.zxing.client.result.ResultParser
import com.jakewharton.rxbinding4.view.clicks
import com.usher.demo.R
import com.usher.demo.base.BaseActivity
import com.usher.demo.util.LogUtil
import com.usher.demo.util.RxUtil
import com.usher.demo.widget.dialog.CommonDialog
import com.usher.demo.zxing.camera.CameraManager
import com.usher.demo.zxing.decode.DecodeThread
import com.usher.demo.zxing.util.BeepManager
import com.usher.demo.zxing.util.CaptureActivityHandler
import com.usher.demo.zxing.util.InactivityTimer
import kotlinx.android.synthetic.main.activity_qr_capture.*

class QRCaptureActivity : BaseActivity(R.layout.activity_qr_capture), SurfaceHolder.Callback {

    private lateinit var mActivityTimer: InactivityTimer
    private lateinit var mBeepManager: BeepManager

    private var mIsSurfaceCreated = false
    private var handler: CaptureActivityHandler? = null

    lateinit var cameraManager: CameraManager
        private set

    var cropRect: Rect? = null
        private set

    override fun initData() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        mActivityTimer = InactivityTimer(this)
        mBeepManager = BeepManager(this)
    }

    override fun initView() {
        statusbar_view.updateLayoutParams { height = statusBarHeight }

        start_imageview.clicks()
                .take(1)
                .to(RxUtil.autoDispose(this))
                .subscribe { finish() }

        capture_scan_line.startAnimation(TranslateAnimation(
                Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, 0.9f).apply {
            duration = 4500
            repeatCount = Animation.INFINITE
            repeatMode = Animation.RESTART
        })
    }

    fun handleDecode(rawResult: Result?, bundle: Bundle) {
        mActivityTimer.onActivity()
        mBeepManager.playBeepSoundAndVibrate()

        rawResult?.let {
            val result = ResultParser.parseResult(it).toString()
            LogUtil.log("[QRCapture] QRCode: $result")
            CommonDialog(this)
                    .withContent(result)
                    .withDialogType(CommonDialog.ButtonType.SINGLE)
                    .confirms()
                    .to(RxUtil.autoDispose(this))
                    .subscribe { continuePreview() }

        } ?: run {
            showToast("解析图片失败")
            continuePreview()
        }
    }

    override fun onResume() {
        super.onResume()

        /* CameraManager must be initialized here, not in onCreate().
        This is necessary because we don't want to open the camera driver and measure the screen size
        if we're going to show the help on first launch.
        That led to bugs where the scanning rectangle was the wrong size and partially off screen. */
        cameraManager = CameraManager(application)

        handler = null

        if (mIsSurfaceCreated) {
            /* The activity was paused but not stopped, so the surface still exists.
            Therefore surfaceCreated() won't be called, so init the camera here. */
            initCamera(capture_surfaceview.holder)
        } else {
            /* Install the callback and wait for surfaceCreated() to init the camera. */
            capture_surfaceview.holder.addCallback(this)
        }

        mActivityTimer.onResume()
    }

    override fun onPause() {
        handler?.run {
            quitSynchronously()
            handler = null
        }

        mActivityTimer.onPause()
        mBeepManager.close()
        cameraManager.closeDriver()
        if (!mIsSurfaceCreated) {
            capture_surfaceview.holder.removeCallback(this)
        }
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mActivityTimer.shutdown()
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        if (!mIsSurfaceCreated) {
            mIsSurfaceCreated = true
            initCamera(holder)
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        mIsSurfaceCreated = false
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {

    }

    private fun initCamera(surfaceHolder: SurfaceHolder?) {
        checkNotNull(surfaceHolder) { "No SurfaceHolder provided" }
        if (cameraManager.isOpen) {
            return
        }
        try {
            cameraManager.openDriver(surfaceHolder)
            // Creating the handler starts the preview, which can also throw a
            // RuntimeException.
            if (handler == null) {
                handler = CaptureActivityHandler(this, cameraManager, DecodeThread.ALL_MODE)
            }

            initCrop()
        } catch (e: Exception) {
            displayFrameworkBugMessageAndExit()
        }
    }

    /**
     * 初始化截取的矩形区域
     */
    private fun initCrop() {
        val cameraWidth = cameraManager.cameraResolution.y
        val cameraHeight = cameraManager.cameraResolution.x

        //获取布局中扫描框的位置信息
        val location = IntArray(2)
        capture_crop_view.getLocationInWindow(location)

        val cropLeft = location[0]
        val cropTop = location[1]
        val cropWidth = capture_crop_view.width
        val cropHeight = capture_crop_view.height

        //获取布局容器的宽高
        val containerWidth = capture_container.width
        val containerHeight = capture_container.height

        //计算最终截取的矩形的左上角顶点x坐标
        val x = cropLeft * cameraWidth / containerWidth
        //计算最终截取的矩形的左上角顶点y坐标
        val y = cropTop * cameraHeight / containerHeight

        //计算最终截取的矩形的宽度
        val width = cropWidth * cameraWidth / containerWidth
        //计算最终截取的矩形的高度
        val height = cropHeight * cameraHeight / containerHeight

        //生成最终的截取的矩形
        cropRect = Rect(x, y, width + x, height + y)
    }

    private fun continuePreview() {
        val surfaceHolder = capture_surfaceview.holder
        initCamera(surfaceHolder)
        Handler().postDelayed({
            handler?.run {
                restartPreviewAndDecode()
            }
        }, 3000)
    }

    private fun displayFrameworkBugMessageAndExit() {
        AlertDialog.Builder(this).apply {
            setTitle(getString(R.string.app_name))
            setMessage("相机打开出错，请稍后重试")
            setPositiveButton("确定") { _, _ -> finish() }
            setOnCancelListener { finish() }
        }.show()
    }

    fun getHandler(): Handler? {
        return handler
    }
}