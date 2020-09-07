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
import com.usher.demo.widget.dialog.CommonDialog
import io.reactivex.rxjava3.core.Observable
import kotlinx.android.synthetic.main.activity_cart.*
import kotlinx.android.synthetic.main.cart_pay_layout.*
import kotlinx.android.synthetic.main.item_cart.view.*
import kotlinx.android.synthetic.main.title_layout.*

class CartActivity : BaseActivity(R.layout.activity_cart, Theme.LIGHT_AUTO) {
    private val mCartItems = ArrayList<CartItemEntity>()
    private val mAdapter by lazy { CartAdapter(mCartItems) }

    override fun initView() {
        initTitleView()

        cart_recyclerview.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        cart_recyclerview.adapter = mAdapter

        //Check Item
        mAdapter.itemChildClicks()
                .compose(RxUtil.getSchedulerComposer())
                .filter { clickItem -> clickItem.view.id == R.id.confirm_layout }
                .map { it.position }
                .to(RxUtil.autoDispose(this))
                .subscribe { position ->
                    mCartItems[position].status = mCartItems[position].status.inv()
                    mAdapter.notifyDataSetChanged()
                }

        //Delete Item
        val deleteWarns: (position: Int) -> Observable<Unit> = { position ->
            CommonDialog(this)
                    .withContent(R.string.cart_delete_warn)
                    .withDialogType(CommonDialog.ButtonType.DOUBLE_WARN)
                    .confirms()
                    .doOnNext { mCartItems.removeAt(position) }
                    .map { Unit }
        }

        mAdapter.itemChildClicks()
                .compose(RxUtil.getSchedulerComposer())
                .filter { clickItem -> clickItem.view.id == R.id.delete_textview }
                .map { it.position }
                .switchMap { position -> deleteWarns(position) }
                .to(RxUtil.autoDispose(this))
                .subscribe {
                    mAdapter.notifyDataSetChanged()
                }

        //Add Item
        mAdapter.itemChildClicks()
                .compose(RxUtil.getSchedulerComposer())
                .filter { clickItem -> clickItem.view.id == R.id.add_imageview }
                .map { it.position }
                .to(RxUtil.autoDispose(this))
                .subscribe { position ->
                    mCartItems[position].buyNum = mCartItems[position].buyNum.inc()
                    mAdapter.notifyDataSetChanged()
                }

        //Minus Item
        mAdapter.itemChildClicks()
                .compose(RxUtil.getSchedulerComposer())
                .filter { clickItem -> clickItem.view.id == R.id.minus_imageview }
                .map { it.position }
                .switchMap { position ->
                    val num = mCartItems[position].buyNum.dec()
                    if (num <= 0) {
                        deleteWarns(position)
                    } else {
                        mCartItems[position].buyNum = num
                        mAdapter.notifyDataSetChanged()
                        Observable.empty()
                    }
                }
                .to(RxUtil.autoDispose(this))
                .subscribe {
                    mAdapter.notifyDataSetChanged()
                }

        ApiFactory.instance.getCart()
                .to(RxUtil.autoDispose(this))
                .subscribe { result ->
                    result.data?.let { cartEntity ->
                        LogUtil.log("data: ${Gson().toJson(cartEntity)}")
                        mCartItems.clear()
                        mCartItems.addAll(cartEntity.cartItems)
                        mAdapter.notifyDataSetChanged()

                        //TODO:
                        total_price_textview.text = getString(R.string.cart_price, "0.00")
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
            holder.addOnClickListener(R.id.confirm_layout)
            holder.addOnClickListener(R.id.delete_textview)
            holder.addOnClickListener(R.id.add_imageview)
            holder.addOnClickListener(R.id.minus_imageview)

            holder.itemView.run {
                confirm_checkbox.isChecked = cartItem.status == 1
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