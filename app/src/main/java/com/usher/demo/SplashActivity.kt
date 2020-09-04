package com.usher.demo

import android.content.Intent
import android.view.View
import com.jakewharton.rxbinding4.view.clicks
import com.usher.demo.base.BaseActivity
import com.usher.demo.util.PermissionUtil
import com.usher.demo.util.RxUtil
import io.reactivex.rxjava3.core.Observable
import kotlinx.android.synthetic.main.activity_splash.*
import java.util.concurrent.TimeUnit

class SplashActivity : BaseActivity(R.layout.activity_splash) {
    companion object {
        private const val COUNTDOWN_SECONDS = 3L
        private const val URL = "https://www.7fresh.com/"
    }

    private var mSplashUrl: String? = null

    override fun initView() {
        val countdowns = Observable.interval(0, 1000, TimeUnit.MILLISECONDS)
                .compose(RxUtil.getSchedulerComposer())
                .take(COUNTDOWN_SECONDS + 1)
                .map { COUNTDOWN_SECONDS - it }

        val adClicks = ad_imageview.clicks().take(1).doOnNext { mSplashUrl = URL }
        val skipClicks = skip_textview.clicks().take(1)

        PermissionUtil.requestPermissions(this, Constants.PERMISSION_INIT_APP)
                .take(1)
                .switchMap {
                    ad_imageview.visibility = View.VISIBLE
                    skip_textview.visibility = View.VISIBLE
                    countdowns
                }
                .takeUntil { it <= 0 }
                .takeUntil(adClicks)
                .takeUntil(skipClicks)
                .doOnComplete {
                    startActivity(Intent(this, MainActivity::class.java).apply {
                        putExtra(Constants.TAG_URL, mSplashUrl)
                    })

                    finish()
                }
                .to(RxUtil.autoDispose(this))
                .subscribe {
                    skip_textview.text = getString(R.string.splash_skip, it)
                }
    }
}