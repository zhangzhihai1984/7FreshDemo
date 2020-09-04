package com.usher.demo.main.fragment

import com.google.gson.Gson
import com.usher.demo.R
import com.usher.demo.api.ApiFactory
import com.usher.demo.base.BaseNavigationFragment
import com.usher.demo.util.LogUtil
import com.usher.demo.util.RxUtil
import kotlinx.android.synthetic.main.fragment_main_product.*

class MainProductFragment : BaseNavigationFragment(R.layout.fragment_main_product) {
    companion object {
        fun newInstance() = MainProductFragment()
    }

    override fun init() {

        ApiFactory.instance.getDetail()
                .to(RxUtil.autoDispose(this))
                .subscribe { result ->
                    result.data?.run {
                        LogUtil.log("data: ${Gson().toJson(this)}")
                        banner_view.data = imageUrls
                    }
                }
    }
}