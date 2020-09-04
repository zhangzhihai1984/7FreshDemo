package com.usher.demo

import android.content.Intent
import com.google.gson.Gson
import com.usher.demo.api.ApiFactory
import com.usher.demo.base.BaseActivity
import com.usher.demo.util.LogUtil
import com.usher.demo.util.RxUtil

class MainActivity : BaseActivity(R.layout.activity_main) {

    override fun initView() {
        intent.getStringExtra(Constants.TAG_URL)?.let {
            startActivity(Intent(this, CommonWebActivity::class.java).apply {
                putExtra(Constants.TAG_URL, it)
            })
        }

        ApiFactory.instance.getDetail()
                .to(RxUtil.autoDispose(this))
                .subscribe { result ->
                    result.data?.run {
                        LogUtil.log("data: ${Gson().toJson(this)}")
                    }
                }

//        showLoadingDialog()
    }
}