package com.usher.demo.order

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jakewharton.rxbinding4.view.clicks
import com.usher.demo.Constants
import com.usher.demo.R
import com.usher.demo.api.entities.CartItemEntity
import com.usher.demo.base.BaseActivity
import com.usher.demo.order.fragment.OrderDeliveryFragment
import com.usher.demo.order.fragment.OrderPickupFragment
import com.usher.demo.util.RxUtil
import kotlinx.android.synthetic.main.activity_order.*
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

        order_viewpager.adapter = OrderFragmentAdapter(supportFragmentManager, mCartItems)
        tablayout.setViewPager(order_viewpager)

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

    private class OrderFragmentAdapter(fm: FragmentManager, private val data: List<CartItemEntity>) : FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

        override fun getItem(position: Int): Fragment = if (position == 0) OrderDeliveryFragment.newInstance(data) else OrderPickupFragment.newInstance()

        override fun getCount(): Int = 2

        override fun getPageTitle(position: Int): CharSequence? = if (position == 0) "配送到家" else "到店自提"
    }
}