package com.usher.demo

import android.content.Intent
import android.view.KeyEvent
import androidx.fragment.app.Fragment
import com.google.gson.Gson
import com.jakewharton.rxbinding4.view.clicks
import com.usher.demo.api.ApiFactory
import com.usher.demo.base.BaseActivity
import com.usher.demo.util.LogUtil
import com.usher.demo.util.RxUtil
import io.reactivex.rxjava3.subjects.PublishSubject
import kotlinx.android.synthetic.main.main_cart_layout.*

class MainActivity : BaseActivity(R.layout.activity_main, Theme.LIGHT_AUTO) {
    companion object {
        private const val EXIT_DURATION = 1000
    }

    private val mFragmentMap = hashMapOf<String, Fragment>()
    private val mBackKey = PublishSubject.create<Unit>()

    override fun initData() {
        intent.getStringExtra(Constants.TAG_URL)?.let {
            startActivity(Intent(this, CommonWebActivity::class.java).apply {
                putExtra(Constants.TAG_URL, it)
            })
        }
    }

    override fun initView() {

        val intervals = mBackKey.timeInterval().map { it.time() }
        val exits = intervals.skip(1).filter { it < EXIT_DURATION }

        exits.to(RxUtil.autoDispose(this)).subscribe { finish() }

        intervals.takeUntil(exits)
                .to(RxUtil.autoDispose(this))
                .subscribe { showToast("再次点击退出程序") }

        add_textview.clicks()
                .compose(RxUtil.singleClick())
                .to(RxUtil.autoDispose(this))
                .subscribe { }


        ApiFactory.instance.getDetail()
                .to(RxUtil.autoDispose(this))
                .subscribe { result ->
                    result.data?.run {
                        LogUtil.log("data: ${Gson().toJson(this)}")
                    }
                }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            mBackKey.onNext(Unit)
            return false
        }

        return super.onKeyDown(keyCode, event)
    }
}