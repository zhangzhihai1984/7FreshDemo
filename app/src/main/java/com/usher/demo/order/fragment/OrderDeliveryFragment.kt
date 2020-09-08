package com.usher.demo.order.fragment

import com.jakewharton.rxbinding4.view.clicks
import com.usher.demo.R
import com.usher.demo.base.BaseNavigationFragment
import com.usher.demo.util.RxUtil
import kotlinx.android.synthetic.main.fragment_order_delivery.*

class OrderDeliveryFragment : BaseNavigationFragment(R.layout.fragment_order_delivery) {
    companion object {
        fun newInstance() = OrderDeliveryFragment()
    }

    override fun init() {
        address_layout.clicks()
            .compose(RxUtil.singleClick())
            .to(RxUtil.autoDispose(this))
            .subscribe { showToast("更换地址") }
    }
}