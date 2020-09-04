package com.usher.demo

import android.content.Intent
import android.view.KeyEvent
import android.view.View
import androidx.fragment.app.Fragment
import com.google.gson.Gson
import com.jakewharton.rxbinding4.view.clicks
import com.usher.demo.api.ApiFactory
import com.usher.demo.base.BaseActivity
import com.usher.demo.util.LogUtil
import com.usher.demo.util.RxUtil
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.PublishSubject
import kotlinx.android.synthetic.main.main_cart_layout.*
import java.util.concurrent.TimeUnit

class MainActivity : BaseActivity(R.layout.activity_main, Theme.LIGHT_AUTO) {
    companion object {
        private const val EXIT_DURATION = 1000
    }

    private val mFragmentMap = hashMapOf<String, Fragment>()
    private val mBackKey = PublishSubject.create<Unit>()

    private var mCartItemCount = 0

    override fun initData() {
        intent.getStringExtra(Constants.TAG_URL)?.let {
            startActivity(Intent(this, CommonWebActivity::class.java).apply {
                putExtra(Constants.TAG_URL, it)
            })
        }
    }

    override fun initView() {

        initExit()
        initCartLayout()


        ApiFactory.instance.getDetail()
                .to(RxUtil.autoDispose(this))
                .subscribe { result ->
                    result.data?.run {
                        LogUtil.log("data: ${Gson().toJson(this)}")
                    }
                }
    }

    private fun initCartLayout() {
        val addCart = Observable.timer(1000, TimeUnit.MILLISECONDS).compose(RxUtil.getSchedulerComposer())
        add_textview.clicks()
                .compose(RxUtil.singleClick())
                .switchMap {
                    showLoadingDialog()
                    addCart
                }
                .to(RxUtil.autoDispose(this))
                .subscribe {
                    dismissLoadingDialog()
                    mCartItemCount += 1

                    item_count_textview.run {
                        visibility = View.VISIBLE
                        text = "$mCartItemCount"
                    }

                    showToast("添加成功")
                }

        //TODO: New Page
        cart_imageview.clicks()
                .compose(RxUtil.singleClick())
                .to(RxUtil.autoDispose(this))
                .subscribe { }
    }

    private fun initExit() {
        val intervals = mBackKey.timeInterval().map { it.time() }
        val exits = intervals.skip(1).filter { it < EXIT_DURATION }

        exits.to(RxUtil.autoDispose(this)).subscribe { finish() }

        intervals.takeUntil(exits)
                .to(RxUtil.autoDispose(this))
                .subscribe { showToast("再次点击退出程序") }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            mBackKey.onNext(Unit)
            return false
        }

        return super.onKeyDown(keyCode, event)
    }
}