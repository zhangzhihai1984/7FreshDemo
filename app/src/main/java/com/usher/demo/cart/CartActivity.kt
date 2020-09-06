package com.usher.demo.cart

import com.jakewharton.rxbinding4.view.clicks
import com.usher.demo.R
import com.usher.demo.base.BaseActivity
import com.usher.demo.util.RxUtil
import kotlinx.android.synthetic.main.title_layout.*

class CartActivity : BaseActivity(R.layout.activity_cart, Theme.LIGHT_AUTO) {

    override fun initView() {
        initTitleView()
    }

    private fun initTitleView() {
        start_imageview.clicks()
                .take(1)
                .to(RxUtil.autoDispose(this))
                .subscribe { finish() }

        center_textview.text = getString(R.string.cart_title)
    }
}