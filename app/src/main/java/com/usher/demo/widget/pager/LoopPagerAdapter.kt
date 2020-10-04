package com.usher.demo.widget.pager

import android.os.Parcelable
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter

internal class LoopPagerAdapter(private val rawAdapter: PagerAdapter) : PagerAdapter() {

    override fun instantiateItem(container: ViewGroup, position: Int): Any = rawAdapter.instantiateItem(container, position)

    override fun destroyItem(container: ViewGroup, position: Int, obj: Any) = rawAdapter.destroyItem(container, position, obj)

    override fun getCount(): Int =
            when {
                rawAdapter.count <= 1 -> rawAdapter.count
                else -> rawAdapter.count + 2
            }

    /**
     * 考虑到有刷新的需求, 返回[PagerAdapter.POSITION_NONE]
     */
    override fun getItemPosition(obj: Any): Int = POSITION_NONE

    override fun isViewFromObject(view: View, obj: Any): Boolean = rawAdapter.isViewFromObject(view, obj)

    override fun startUpdate(container: ViewGroup) = rawAdapter.startUpdate(container)

    override fun finishUpdate(container: ViewGroup) = rawAdapter.finishUpdate(container)

    override fun setPrimaryItem(container: ViewGroup, position: Int, obj: Any) = rawAdapter.setPrimaryItem(container, position, obj)

    override fun restoreState(state: Parcelable?, loader: ClassLoader?) = rawAdapter.restoreState(state, loader)

    override fun saveState(): Parcelable? = rawAdapter.saveState()

    override fun getPageWidth(position: Int): Float = rawAdapter.getPageWidth(position)
}