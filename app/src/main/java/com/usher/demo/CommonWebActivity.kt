package com.usher.demo

import android.annotation.SuppressLint
import android.view.View
import android.webkit.*
import androidx.core.view.updateLayoutParams
import com.jakewharton.rxbinding4.view.clicks
import com.usher.demo.base.BaseActivity
import com.usher.demo.util.RxUtil
import kotlinx.android.synthetic.main.activity_common_web.*
import kotlinx.android.synthetic.main.title_layout.*

class CommonWebActivity : BaseActivity(R.layout.activity_common_web) {
    private val mUrl by lazy { intent.getStringExtra(Constants.TAG_URL) }
    private var mReloadUrl: String = ""

    @SuppressLint("SetJavaScriptEnabled")
    override fun initView() {
        initTitleView()

        webview.run {
            setOnLongClickListener { true }
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
            webViewClient = mWebViewClient
            webChromeClient = mWebChromeClient

            settings.run {
                allowFileAccess = false         // 设置允许访问文件数据
                javaScriptEnabled = true        // 设置支持javascript脚本
                builtInZoomControls = false     // 设置支持缩放
                setSupportZoom(false)
                useWideViewPort = true          // 设置加载进来的页面自适应手机屏幕
                loadWithOverviewMode = true
                cacheMode = WebSettings.LOAD_NO_CACHE
                domStorageEnabled = true
            }
        }

        error_layout.clicks()
                .compose(RxUtil.singleClick())
                .to(RxUtil.autoDispose(this))
                .subscribe {
                    webview.loadUrl(mReloadUrl)
                    error_layout.visibility = View.GONE
                }


        mUrl?.let { url -> webview.loadUrl(url) } ?: finish()
    }

    private fun initTitleView() {
        start_imageview.clicks()
                .compose(RxUtil.singleClick())
                .to(RxUtil.autoDispose(this))
                .subscribe {
                    if (error_layout.visibility == View.VISIBLE) {
                        finish()
                    } else {
                        if (webview.canGoBack())
                            webview.goBack()
                        else
                            finish()
                    }
                }

        statusbar_view.updateLayoutParams { height = statusBarHeight }
    }

    private val mWebViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            if (request.url.toString().startsWith("http"))
                view.loadUrl(request.url.toString())

            return true
        }

        override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
            mReloadUrl = request.url.toString()
//            error_layout.visibility = View.VISIBLE
        }
    }

    private val mWebChromeClient = object : WebChromeClient() {
        override fun onProgressChanged(view: WebView, newProgress: Int) {
            if (newProgress == 100) {
                progressbar.visibility = View.GONE
            } else {
                if (progressbar.visibility == View.GONE)
                    progressbar.visibility = View.VISIBLE
                progressbar.progress = newProgress
            }
        }

        override fun onReceivedTitle(view: WebView, title: String) {
            if (!title.contains("text/html"))
                center_textview.text = title
        }
    }
}