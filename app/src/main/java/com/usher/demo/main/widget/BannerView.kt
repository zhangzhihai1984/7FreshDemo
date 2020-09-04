package com.usher.demo.main.widget

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.squareup.picasso.Picasso
import com.usher.demo.Constants
import com.usher.demo.R
import com.usher.demo.base.BasePagerFragment
import com.usher.demo.widget.SquareLayout
import com.usher.demo.widget.pager.LoopViewPager
import kotlinx.android.synthetic.main.fragment_main_banner.*
import kotlinx.android.synthetic.main.main_banner_layout.view.*

class BannerView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0, defStyleRes: Int = 0) : SquareLayout(context, attrs, defStyleAttr, defStyleRes) {
    private val mAdapter by lazy { BannerFragmentAdapter((context as FragmentActivity).supportFragmentManager) }

    var data = listOf<String>()
        set(value) {
            field = value
            mAdapter.data = value
            mAdapter.notifyDataSetChanged()
        }

    init {
        inflate(context, R.layout.main_banner_layout, this)

        banner_viewpager.adapter = mAdapter
    }

    private class BannerFragmentAdapter(fm: FragmentManager) : FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
        var data = listOf<String>()

        override fun getCount(): Int = data.size

        override fun getItem(position: Int): Fragment {
            val pos = LoopViewPager.getMatchedPosition(position, count)
            return BannerFragment.newInstance(data[pos])
        }
    }

    class BannerFragment : BasePagerFragment(R.layout.fragment_main_banner) {
        companion object {
            fun newInstance(url: String): BannerFragment =
                    BannerFragment().apply {
                        arguments = Bundle().apply {
                            putString(Constants.TAG_DATA, url)
                        }
                    }
        }

        override fun init() {
            arguments?.let {
                val url = it.getString(Constants.TAG_DATA)
                Picasso.get().load(url).into(banner_imageview)
            }
        }
    }
}