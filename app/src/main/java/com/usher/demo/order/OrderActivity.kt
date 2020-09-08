package com.usher.demo.order

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jakewharton.rxbinding4.view.clicks
import com.usher.demo.Constants
import com.usher.demo.R
import com.usher.demo.api.entities.CartItemEntity
import com.usher.demo.base.BaseActivity
import com.usher.demo.util.RxUtil
import kotlinx.android.synthetic.main.order_pay_layout.*
import kotlinx.android.synthetic.main.title_layout.*

class OrderActivity : BaseActivity(R.layout.activity_order, Theme.LIGHT_AUTO) {
    private val mCartItems by lazy {
        val json = intent.getStringExtra(Constants.TAG_DATA)
        val type = object : TypeToken<List<CartItemEntity>>() {}.type
        Gson().fromJson<List<CartItemEntity>>(json, type)
    }

    override fun initView() {
        initTitleView()

        val totalPrice = mCartItems.map { it.unitPrice * it.buyNum }.reduceOrNull { acc, price -> acc + price }?.run { String.format("%.2f", this) }
                ?: "0.00"
        price_textview.text = getString(R.string.order_price, totalPrice)
    }

    private fun initTitleView() {
        start_imageview.clicks()
                .take(1)
                .to(RxUtil.autoDispose(this))
                .subscribe { finish() }

        center_textview.text = getString(R.string.order_title)
    }
}