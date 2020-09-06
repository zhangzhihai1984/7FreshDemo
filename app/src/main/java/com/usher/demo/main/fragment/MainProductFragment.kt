package com.usher.demo.main.fragment

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebView
import com.google.gson.Gson
import com.jakewharton.rxbinding4.view.clicks
import com.jakewharton.rxbinding4.view.scrollChangeEvents
import com.usher.demo.R
import com.usher.demo.api.ApiFactory
import com.usher.demo.base.BaseNavigationFragment
import com.usher.demo.util.LogUtil
import com.usher.demo.util.RxUtil
import kotlinx.android.synthetic.main.fragment_main_product.*
import kotlinx.android.synthetic.main.main_delivery_layout.*
import kotlinx.android.synthetic.main.main_description_layout.*

class MainProductFragment : BaseNavigationFragment(R.layout.fragment_main_product) {
    companion object {
        fun newInstance() = MainProductFragment()
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun init() {
        //TODO: when to show "return to top"
        scrollview.scrollChangeEvents()
                .map { it.scrollY }
                .to(RxUtil.autoDispose(this))
                .subscribe {  }

        top_textview.clicks()
                .compose(RxUtil.singleClick())
                .to(RxUtil.autoDispose(this))
                .subscribe { scrollview.scrollTo(0, 0) }

        ApiFactory.instance.getDetail()
                .to(RxUtil.autoDispose(this))
                .subscribe { result ->
                    result.data?.run {
                        LogUtil.log("data: ${Gson().toJson(this)}")
                        banner_view.data = imageUrls

                        name_textview.text = skuName
                        description_textview.text = adText
                        price_textview.text = getString(R.string.main_price, jdPrice)
                        unit_textview.text = buyUnit

                        full_address_textview.text = getString(R.string.main_full_address, jdShipment.address)
                        address_textview.text = jdShipment.address
                        date_textview.text = jdShipment.deliveryDateAndTime

                        val webView = WebView(requireContext()).apply {
                            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                            settings.run {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                            }

                            loadUrl(detailUrl)
                        }
                        content_layout.addView(webView)
                    }
                }
    }
}