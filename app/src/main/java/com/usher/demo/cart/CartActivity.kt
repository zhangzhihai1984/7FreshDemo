package com.usher.demo.cart

import android.view.View
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
import java.util.concurrent.TimeUnit

class CartActivity : BaseActivity(R.layout.activity_cart, Theme.LIGHT_AUTO) {
    private val mCartItems = ArrayList<CartItemEntity>()
    private val mAdapter by lazy { CartAdapter(mCartItems) }

    override fun initView() {
        initTitleView()

        cart_recyclerview.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        cart_recyclerview.adapter = mAdapter

        /**
         * check item
         * delete item
         * add item buyNum
         * minus item buyNum
         *
         * check all
         * delete all (status > 0)
         *
         * notifyDataSetChanged
         * updateBatchLayout (TBD)
         * updatePayLayout (total price & pay count)
         */

        //Check All
        all_layout.clicks()
                .compose(RxUtil.singleClick())
                .to(RxUtil.autoDispose(this))
                .subscribe {
                    mCartItems.forEach { it.status = if (all_checkbox.isChecked) 0 else 1 }
                    mAdapter.notifyDataSetChanged()
                    updateBatchLayout()
                    updatePayLayout()
                }

        //Check Item
        mAdapter.itemChildClicks()
                .compose(RxUtil.getSchedulerComposer())
                .filter { clickItem -> clickItem.view.id == R.id.confirm_layout }
                .map { it.position }
                .to(RxUtil.autoDispose(this))
                .subscribe { position ->
                    mCartItems[position].status = if (mCartItems[position].status <= 0) 1 else 0
                    mAdapter.notifyDataSetChanged()
                    updateBatchLayout()
                    updatePayLayout()
                }

        //Delete Item
        /**
         * delete item
         * show empty layout if needed
         */
        val deleteWarns: (position: Int) -> Observable<Unit> = { position ->
            CommonDialog(this)
                    .withContent(R.string.cart_delete_single_warn)
                    .withDialogType(CommonDialog.ButtonType.DOUBLE_WARN)
                    .confirms()
                    .doOnNext {
                        mCartItems.removeAt(position)
                        mAdapter.notifyDataSetChanged()

                        showEmptyLayoutIfNeeded()

                        updateBatchLayout()
                        updatePayLayout()
                    }
                    .map { Unit }
        }

        mAdapter.itemChildClicks()
                .compose(RxUtil.getSchedulerComposer())
                .filter { clickItem -> clickItem.view.id == R.id.delete_textview }
                .map { it.position }
                .switchMap { position -> deleteWarns(position) }
                .to(RxUtil.autoDispose(this))
                .subscribe {}

        //Add Item buyNum
        mAdapter.itemChildClicks()
                .compose(RxUtil.getSchedulerComposer())
                .filter { clickItem -> clickItem.view.id == R.id.add_imageview }
                .map { it.position }
                .to(RxUtil.autoDispose(this))
                .subscribe { position ->
                    mCartItems[position].buyNum = mCartItems[position].buyNum.inc()
                    mCartItems[position].status = 1
                    mAdapter.notifyDataSetChanged()
                    updateBatchLayout()
                    updatePayLayout()
                }

        //Minus Item buyNum
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
                        mCartItems[position].status = 1
                        mAdapter.notifyDataSetChanged()
                        updateBatchLayout()
                        updatePayLayout()
                        Observable.empty()
                    }
                }
                .to(RxUtil.autoDispose(this))
                .subscribe {}

        /**
         * 由于提供的数据只有一个item, 不利于删除和全选等功能的展现, 因此这里通过重发请求增加若干item
         */
        Observable.interval(0, 100, TimeUnit.MILLISECONDS)
                .take(3)
                .flatMap { ApiFactory.instance.getCart() }
                .to(RxUtil.autoDispose(this))
                .subscribe { result ->
                    result.data?.let { cartEntity ->
                        LogUtil.log("data: ${Gson().toJson(cartEntity)}")

//                        mCartItems.clear()
                        mCartItems.addAll(cartEntity.cartItems)

                        mAdapter.notifyDataSetChanged()

                        showEmptyLayoutIfNeeded()
                        updateBatchLayout()
                        updatePayLayout()
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
        end_textview.text = getString(R.string.cart_delete)
        end_textview.clicks()
                .compose(RxUtil.singleClick())
                .switchMap {
                    val checkedItems = mCartItems.filter { it.status > 0 }

                    CommonDialog(this)
                            .withContent(getString(R.string.cart_delete_batch_warn, checkedItems.size))
                            .withDialogType(CommonDialog.ButtonType.DOUBLE_WARN)
                            .confirms()
                            .doOnNext {
                                mCartItems.removeAll(checkedItems)
                                mAdapter.notifyDataSetChanged()

                                showEmptyLayoutIfNeeded()

                                updateBatchLayout()
                                updatePayLayout()
                            }
                }
                .to(RxUtil.autoDispose(this))
                .subscribe {}
    }

    private fun showEmptyLayoutIfNeeded() {
        cart_content_layout.visibility = if (mCartItems.isEmpty()) View.GONE else View.VISIBLE
        cart_empty_layout.visibility = if (mCartItems.isEmpty()) View.VISIBLE else View.GONE
    }

    /**
     * 1. 顶部"删除"显示与否
     * 2. 底部"全选"选中状态
     */
    private fun updateBatchLayout() {
        end_textview.visibility = mCartItems.find { it.status > 0 }?.run { View.VISIBLE }
                ?: View.GONE
        all_checkbox.isChecked = mCartItems.find { it.status <= 0 }?.run { false } ?: true
    }

    /**
     * 1. 合计价钱(保留2位小数)
     * 2. 去结算(X)数量(如果为0不可点击)
     */
    private fun updatePayLayout() {
        val checkedItems = mCartItems.filter { it.status > 0 }
        val totalPrice = checkedItems.map { it.unitPrice * it.buyNum }.reduceOrNull { acc, price -> acc + price }?.run { String.format("%.2f", this) }
                ?: "0.00"

        total_price_textview.text = getString(R.string.cart_price, totalPrice)

        pay_textview.text = getString(R.string.cart_pay, checkedItems.size)
        pay_textview.isEnabled = checkedItems.isNotEmpty()
    }

    private class CartAdapter(data: List<CartItemEntity>) : RxBaseQuickAdapter<CartItemEntity, BaseViewHolder>(R.layout.item_cart, data) {
        override fun convert(holder: BaseViewHolder, cartItem: CartItemEntity) {
            holder.addOnClickListener(R.id.confirm_layout)
            holder.addOnClickListener(R.id.delete_textview)
            holder.addOnClickListener(R.id.add_imageview)
            holder.addOnClickListener(R.id.minus_imageview)

            holder.itemView.run {
                confirm_checkbox.isChecked = cartItem.status > 0
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