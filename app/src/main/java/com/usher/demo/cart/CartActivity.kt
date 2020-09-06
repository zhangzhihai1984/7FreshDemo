package com.usher.demo.cart

import com.google.gson.Gson
import com.jakewharton.rxbinding4.view.clicks
import com.usher.demo.R
import com.usher.demo.api.ApiFactory
import com.usher.demo.base.BaseActivity
import com.usher.demo.util.LogUtil
import com.usher.demo.util.RxUtil
import kotlinx.android.synthetic.main.title_layout.*

class CartActivity : BaseActivity(R.layout.activity_cart, Theme.LIGHT_AUTO) {

    override fun initView() {
        initTitleView()

        ApiFactory.instance.getCart()
                .to(RxUtil.autoDispose(this))
                .subscribe { result ->
                    result.data?.run {
                        LogUtil.log("data: ${Gson().toJson(this)}")
                    } ?: run {
                        finish()
                    }
                }
    }

    private fun initTitleView() {
        start_imageview.clicks()
                .take(1)
                .to(RxUtil.autoDispose(this))
                .subscribe { finish() }

        center_textview.text = getString(R.string.cart_title)
    }
}