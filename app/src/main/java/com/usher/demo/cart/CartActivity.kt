package com.usher.demo.cart

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chad.library.adapter.base.BaseViewHolder
import com.google.gson.Gson
import com.jakewharton.rxbinding4.view.clicks
import com.squareup.picasso.Picasso
import com.usher.demo.R
import com.usher.demo.api.ApiFactory
import com.usher.demo.api.entities.CartItemEntity
import com.usher.demo.base.BaseActivity
import com.usher.demo.base.RxBaseQuickAdapter
import com.usher.demo.util.LogUtil
import com.usher.demo.util.RxUtil
import kotlinx.android.synthetic.main.activity_cart.*
import kotlinx.android.synthetic.main.item_cart.view.*
import kotlinx.android.synthetic.main.title_layout.*

class CartActivity : BaseActivity(R.layout.activity_cart, Theme.LIGHT_AUTO) {
    private val mCartItems = ArrayList<CartItemEntity>()
    private val mAdapter by lazy { CartAdapter(mCartItems) }

    override fun initView() {
        initTitleView()

        cart_recyclerview.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        cart_recyclerview.adapter = mAdapter

        ApiFactory.instance.getCart()
                .to(RxUtil.autoDispose(this))
                .subscribe { result ->
                    result.data?.let { cartEntity ->
                        LogUtil.log("data: ${Gson().toJson(cartEntity)}")
                        mCartItems.clear()
                        mCartItems.addAll(cartEntity.cartItems)
                        mAdapter.notifyDataSetChanged()
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

    private class CartAdapter(data: List<CartItemEntity>) : RxBaseQuickAdapter<CartItemEntity, BaseViewHolder>(R.layout.item_cart, data) {
        override fun convert(holder: BaseViewHolder, cartItem: CartItemEntity) {
            holder.itemView.run {
                Picasso.get().load(cartItem.imageUrl).into(product_imageview)
                name_textview.text = cartItem.skuName
                spec_textview.text = mContext.getString(R.string.cart_spec, "${cartItem.weight}${cartItem.weightUnit}")
                price_textview.text = mContext.getString(R.string.cart_price, cartItem.unitPrice.toString())
                unit_textview.text = mContext.getString(R.string.cart_unit, cartItem.buyUnit)
                count_textview.text = "${cartItem.buyNum}"
            }
        }
    }
}