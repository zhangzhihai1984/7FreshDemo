package com.usher.demo.main.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.core.view.children
import androidx.lifecycle.LifecycleOwner
import com.jakewharton.rxbinding4.view.clicks
import com.usher.demo.Constants
import com.usher.demo.R
import com.usher.demo.util.RxUtil
import com.usher.demo.widget.INavigator
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.PublishSubject
import kotlinx.android.synthetic.main.top_tab_item.view.*

class TopTabView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0, defStyleRes: Int = 0) : LinearLayout(context, attrs, defStyleAttr, defStyleRes), INavigator {
    private val mNavigateSubject = PublishSubject.create<String>()

    private val mTextIds = arrayOf(R.string.top_tab_product, R.string.top_tab_comment, R.string.top_tab_detail, R.string.top_tab_recommend)
    private val mTags = arrayOf(Constants.TAB_TAG_PRODUCT, Constants.TAB_TAG_COMMENT, Constants.TAB_TAG_DETAIL, Constants.TAB_TAG_RECOMMEND)

    private var mCurrentTabTag = Constants.TAB_TAG_PRODUCT

    init {
        orientation = HORIZONTAL

        mTags.forEachIndexed { i, tabTag ->
            val view = LayoutInflater.from(context).inflate(R.layout.top_tab_item, this, false).apply {
                tag = tabTag
                tab_textview.text = resources.getString(mTextIds[i])

                clicks().compose(RxUtil.singleClick())
                        .map { tag as String }
                        .filter { tag -> mCurrentTabTag != tag }
                        .to(RxUtil.autoDispose(context as LifecycleOwner))
                        .subscribe { tag -> updateTabs(tag) }
            }

            addView(view, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
        }

        updateTabs(mCurrentTabTag)
    }

    override fun clear() {}

    override fun updateTabs(tag: String) {
        mCurrentTabTag = tag

        children.forEachIndexed { i, view ->
            if (tag == view.tag) {
                view.tab_textview.setTextColor(context.getColor(R.color.text_teal))
                view.indicator_view.visibility = View.VISIBLE
            } else {
                view.tab_textview.setTextColor(context.getColor(R.color.text_4A))
                view.indicator_view.visibility = View.INVISIBLE
            }
        }

        mNavigateSubject.onNext(tag)
    }

    override fun navigates(): Observable<String> = mNavigateSubject
}