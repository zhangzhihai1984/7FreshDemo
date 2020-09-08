package com.usher.demo.cart

import android.content.Intent
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chad.library.adapter.base.BaseViewHolder
import com.google.gson.Gson
import com.jakewharton.rxbinding4.view.clicks
import com.squareup.picasso.Picasso
import com.usher.demo.Constants
import com.usher.demo.R
import com.usher.demo.api.ApiFactory
import com.usher.demo.api.entities.CartItemEntity
import com.usher.demo.base.BaseActivity
import com.usher.demo.base.RxBaseQuickAdapter
import com.usher.demo.order.OrderActivity
import com.usher.demo.util.LogUtil
import com.usher.demo.util.RxUtil
import com.usher.demo.widget.dialog.CommonDialog
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.PublishSubject
import kotlinx.android.synthetic.main.activity_cart.*
import kotlinx.android.synthetic.main.cart_pay_layout.*
import kotlinx.android.synthetic.main.item_cart.view.*
import kotlinx.android.synthetic.main.title_layout.*
import java.util.concurrent.TimeUnit

class CartActivity : BaseActivity(R.layout.activity_cart, Theme.LIGHT_AUTO) {
    private val mItemBatchDeletes = PublishSubject.create<Unit>()
    private val mCartItems = ArrayList<CartItemEntity>()
    private val mAdapter by lazy { CartAdapter(mCartItems) }

    /**
     * 删除item确认对话框:
     * 1. 如果确认删除, 移除当前item
     * 2. 如果确认删除, 如果购物车为空则显示empty_layout
     */
    private val itemDeleteWarns: (position: Int) -> Observable<Unit> = { position ->
        CommonDialog(this)
                .withContent(R.string.cart_delete_single_warn)
                .withDialogType(CommonDialog.ButtonType.DOUBLE_WARN)
                .confirms()
                .doOnNext {
                    mCartItems.removeAt(position)
                    showEmptyLayoutIfNeeded()
                }
                .map { Unit }
    }

    /**
     * 批量删除item确认对话框:
     * 1. 如果确认删除, 移除status为选中的item
     * 2. 如果确认删除, 如果购物车为空则显示empty_layout
     */
    val batchDeleteWarns: () -> Observable<Unit> = {
        val checkedItems = mCartItems.filter { it.status > 0 }
        CommonDialog(this)
                .withContent(getString(R.string.cart_delete_batch_warn, checkedItems.size))
                .withDialogType(CommonDialog.ButtonType.DOUBLE_WARN)
                .confirms()
                .doOnNext {
                    mCartItems.removeAll(checkedItems)
                    showEmptyLayoutIfNeeded()
                }
                .map { Unit }
    }

    override fun initView() {
        initTitleView()

        cart_recyclerview.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        cart_recyclerview.adapter = mAdapter

        /**
         * 选中item:
         * 1. 更新item的status
         */
        val itemChecks = mAdapter.itemChildClicks()
                .filter { clickItem -> clickItem.view.id == R.id.confirm_layout }
                .compose(RxUtil.singleClick())
                .compose(RxUtil.getSchedulerComposer())
                .map { it.position }
                .doOnNext { position -> mCartItems[position].status = if (mCartItems[position].status <= 0) 1 else 0 }
                .map { Unit }


        /**
         * 增加item购买数量:
         * 1. item的购买数量+1
         * 2. 更新item的status为选中
         */
        val itemAdds = mAdapter.itemChildClicks()
                .filter { clickItem -> clickItem.view.id == R.id.add_layout }
                .compose(RxUtil.singleClick())
                .compose(RxUtil.getSchedulerComposer())
                .map { it.position }
                .doOnNext { position ->
                    mCartItems[position].buyNum = mCartItems[position].buyNum.inc()
                    mCartItems[position].status = 1
                }
                .map { Unit }

        /**
         * 减少item购买数量:
         * 1. 如果item购买数量>1, item的购买数量-1, 更新item的status为选中
         * 2. 如果item购买数量=1, 弹出删除确认对话框
         */
        val itemLess = mAdapter.itemChildClicks()
                .filter { clickItem -> clickItem.view.id == R.id.minus_layout }
                .compose(RxUtil.singleClick())
                .compose(RxUtil.getSchedulerComposer())
                .map { it.position }
                .switchMap { position ->
                    val num = mCartItems[position].buyNum.dec()
                    if (num <= 0) {
                        itemDeleteWarns(position)
                    } else {
                        mCartItems[position].buyNum = num
                        mCartItems[position].status = 1
                        Observable.just(Unit)
                    }
                }

        /**
         * 删除item:
         * 1. 弹出删除确认对话框
         */
        val itemDeletes = mAdapter.itemChildClicks()
                .filter { clickItem -> clickItem.view.id == R.id.delete_textview }
                .compose(RxUtil.singleClick())
                .compose(RxUtil.getSchedulerComposer())
                .map { it.position }
                .switchMap { position -> itemDeleteWarns(position) }
                .map { Unit }

        /**
         * 全选/全不选item:
         * 1. 更新所有item的status
         */
        val allChecks = all_layout.clicks()
                .compose(RxUtil.singleClick())
                .doOnNext { mCartItems.forEach { it.status = if (all_checkbox.isChecked) 0 else 1 } }

        /**
         * 获取购物车数据:
         * 1. 更新购物车数据
         * 2. 如果购物车为空则显示empty_layout
         *
         * 注:由于提供的数据只有一个item, 不利于删除和全选等功能的展现, 因此这里通过重发请求增加若干item
         */
        val getCartData = Observable.interval(0, 100, TimeUnit.MILLISECONDS)
                .take(1)
                .flatMap { ApiFactory.instance.getCart() }
                .doOnNext { result ->
                    result.data?.run {
                        LogUtil.log("data: ${Gson().toJson(this)}")

//                        mCartItems.clear()
                        mCartItems.addAll(cartItems)
                        showEmptyLayoutIfNeeded()
                    } ?: run {
                        showToast("数据获取失败")
                        finish()
                    }
                }
                .map { Unit }

        /**
         * 1. 获取购物车数据
         * 2. 选中item
         * 3. 增加item购买数量
         * 4. 减少item购买数量
         * 5. 删除item
         * 6. 全选/全不选item
         * 7. 批量删除item
         *
         * 以上操作均会触发如下处理:
         * 1. 更新adapter
         * 2. 更新"全选"及批量"删除"状态
         * 3. 更新合计价钱及结算数量
         */
        Observable.mergeArray(getCartData, itemChecks, itemAdds, itemLess, itemDeletes, allChecks, mItemBatchDeletes)
                .to(RxUtil.autoDispose(this))
                .subscribe {
                    mAdapter.notifyDataSetChanged()
                    updateBatchLayout()
                    updatePayLayout()
                }

        /**
         * 去结算
         */
        pay_textview.clicks()
                .compose(RxUtil.singleClick())
                .to(RxUtil.autoDispose(this))
                .subscribe {
                    startActivity(Intent(this, OrderActivity::class.java).apply {
                        putExtra(Constants.TAG_DATA, Gson().toJson(mCartItems.filter { it.status > 0 }))
                    })
                }
    }

    private fun initTitleView() {
        start_imageview.clicks()
                .take(1)
                .to(RxUtil.autoDispose(this))
                .subscribe { finish() }

        center_textview.text = getString(R.string.cart_title)
        end_textview.text = getString(R.string.cart_delete)

        /**
         * 批量删除item:
         * 1. 弹出批量删除确认对话框
         */
        end_textview.clicks()
                .compose(RxUtil.singleClick())
                .switchMap { batchDeleteWarns() }
                .to(RxUtil.autoDispose(this))
                .subscribe { mItemBatchDeletes.onNext(Unit) }
    }

    private fun showEmptyLayoutIfNeeded() {
        cart_content_layout.visibility = if (mCartItems.isEmpty()) View.GONE else View.VISIBLE
        cart_empty_layout.visibility = if (mCartItems.isEmpty()) View.VISIBLE else View.GONE
    }

    /**
     * 1. 更新顶部"删除"显示与否
     * 2. 更新底部"全选"选中状态
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
            holder.addOnClickListener(R.id.add_layout)
            holder.addOnClickListener(R.id.minus_layout)

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