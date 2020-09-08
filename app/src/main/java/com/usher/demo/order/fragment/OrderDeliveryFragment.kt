package com.usher.demo.order.fragment

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chad.library.adapter.base.BaseViewHolder
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jakewharton.rxbinding4.view.clicks
import com.squareup.picasso.Picasso
import com.usher.demo.Constants
import com.usher.demo.R
import com.usher.demo.api.entities.CartItemEntity
import com.usher.demo.base.BaseNavigationFragment
import com.usher.demo.base.RxBaseQuickAdapter
import com.usher.demo.util.RxUtil
import kotlinx.android.synthetic.main.fragment_order_delivery.*
import kotlinx.android.synthetic.main.item_order_product.view.*
import kotlinx.android.synthetic.main.order_price_layout.*
import kotlinx.android.synthetic.main.order_product_layout.*

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

            product_recyclerview.layoutManager = LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
            product_recyclerview.adapter = ProductAdapter(cartItems.map { it.imageUrl })

            count_textview.text = getString(R.string.order_product_count, cartItems.size)

            val totalPrice = cartItems.map { it.unitPrice * it.buyNum }.reduceOrNull { acc, price -> acc + price }?.run { String.format("%.2f", this) }
                    ?: "0.00"
            total_price_textview.text = getString(R.string.order_price, totalPrice)
            total_shipping_textview.text = getString(R.string.order_price, "0.00")
        }

        address_layout.clicks()
                .compose(RxUtil.singleClick())
                .to(RxUtil.autoDispose(this))
                .subscribe { showToast("查看地址") }

        delivery_layout.clicks()
                .compose(RxUtil.singleClick())
                .to(RxUtil.autoDispose(this))
                .subscribe { showToast("选择送达时间") }

        product_layout.clicks()
                .compose(RxUtil.singleClick())
                .to(RxUtil.autoDispose(this))
                .subscribe { showToast("展示商品列表") }

        coupon_layout.clicks()
                .compose(RxUtil.singleClick())
                .to(RxUtil.autoDispose(this))
                .subscribe { showToast("查看优惠券") }

        stockout_layout.clicks()
                .compose(RxUtil.singleClick())
                .to(RxUtil.autoDispose(this))
                .subscribe { showToast("选择缺货情况下的方案") }
    }

    private class ProductAdapter(data: List<String>) : RxBaseQuickAdapter<String, BaseViewHolder>(R.layout.item_order_product, data) {
        override fun convert(holder: BaseViewHolder, url: String) {
            holder.itemView.run {
                Picasso.get().load(url).into(product_imageview)
            }
        }
    }
}