package com.usher.demo.order.fragment

import android.os.Bundle
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jakewharton.rxbinding4.view.clicks
import com.usher.demo.Constants
import com.usher.demo.R
import com.usher.demo.api.entities.CartItemEntity
import com.usher.demo.base.BaseNavigationFragment
import com.usher.demo.util.RxUtil
import kotlinx.android.synthetic.main.fragment_order_delivery.*
import kotlinx.android.synthetic.main.order_price_layout.*

class OrderDeliveryFragment : BaseNavigationFragment(R.layout.fragment_order_delivery) {
    companion object {
        fun newInstance(data: List<CartItemEntity>) = OrderDeliveryFragment().apply {
            arguments = Bundle().apply {
                putString(Constants.TAG_DATA, Gson().toJson(data))
            }
        }
    }

    override fun init() {
        arguments?.run {
            val json = getString(Constants.TAG_DATA)
            val type = object : TypeToken<List<CartItemEntity>>() {}.type
            val cartItems = Gson().fromJson<List<CartItemEntity>>(json, type)

            val totalPrice = cartItems.map { it.unitPrice * it.buyNum }.reduceOrNull { acc, price -> acc + price }?.run { String.format("%.2f", this) }
                    ?: "0.00"
            total_price_textview.text = getString(R.string.order_price, totalPrice)
            total_shipping_textview.text = getString(R.string.order_price, "0.00")
        }

        address_layout.clicks()
                .compose(RxUtil.singleClick())
                .to(RxUtil.autoDispose(this))
                .subscribe { showToast("查看地址") }

        coupon_layout.clicks()
                .compose(RxUtil.singleClick())
                .to(RxUtil.autoDispose(this))
                .subscribe { showToast("查看优惠券") }

    }
}