package com.usher.demo.widget.pager

import android.content.Context
import android.database.DataSetObserver
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.lifecycle.LifecycleOwner
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.jakewharton.rxbinding4.view.touches
import com.usher.demo.R
import com.usher.demo.util.RxUtil
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.PublishSubject
import java.util.concurrent.TimeUnit

/**
 * 这是一个实现了"无限循环"的ViewPager.
 * 以4个item的[0,1,2,3]为例, 我们期望的效果是:
 * 1. 当前为"0", 向右滑动, "3"从左侧进入
 * 2. 当前为"3", 向左滑动, "0"从右侧进入
 *
 * 正常来讲, [ViewPager]是不支持循环滑动的, 我们只能通过视觉上的错觉来实现"无限循环".
 * 我们的思路是在首尾添加两个item, 形成4+2个item, 即[3,0,1,2,3,0].
 * 1. 当前为index为1的"0", 向右滑动, index为0的"3"从左侧进入, 随后通过[setCurrentItem], 跳转至index为4的"3"
 * 2. 当前为index为4的"3", 向左滑动, index为5的"0"从右侧进入, 随后通过[setCurrentItem], 跳转至index为1的"0"
 *
 * 需要注意的是, 通过getItem创建Fragment时不要直接使用position, 它是real的, 是针对count+2的position,
 * 需要通过[getRevisedPosition]获得"修正"position.
 */
class LoopViewPager @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : ViewPager(context, attrs) {
    companion object {
        private const val DEFAULT_AUTO_PAGER_PERIOD = 1000

        fun getRevisedPosition(position: Int, count: Int): Int {
            if (count <= 0)
                throw Exception("Adapter is empty")
            return (position - 1 + count) % count
        }
    }

    private val mAutoPageStarts = PublishSubject.create<Unit>()
    private val mAutoPageStops = PublishSubject.create<Unit>()
    private val mAutoPageEnabled: Boolean
    private val mAutoPagePeriod: Long
    private val mOnPageChangeListeners = arrayListOf<OnPageChangeListener>()

    /**
     * 以[0,1,2,3]从"1"向左滑动至"2"为例, 方法调用顺序如下:
     * 1. 手动:
     * onPageScrollStateChanged:    state为drag, 开始拖动
     * onPageScrolled:              position为1, offset从0开始增加
     * onPageScrollStateChanged:    state为settle, 松手
     * onPageSelected:              position为2
     * onPageScrolled:              position为1, offset逐渐接近1
     * onPageScrolled:              position为2, offset为0
     * onPageScrollStateChanged:    state为idle, 静止
     *
     * 2. 调用setCurrentItem, smoothScroll为true:
     * onPageScrollStateChanged:    state为settle
     * onPageSelected:              position为2
     * onPageScrolled:              position为1, offset从0开始增加, 逐渐接近1
     * onPageScrolled:              position为2, offset为0
     * onPageScrollStateChanged:    state为idle, 静止
     *
     * 3. 调用setCurrentItem, smoothScroll为false:
     * onPageSelected:              position为2
     * onPageScrolled:              position为2, offset为0
     *
     *
     * 以[0,1,2,3]remove掉一个数据为例, 方法调用顺序如下:
     * 1. 当前在"2":
     * onPageScrolled:              position为2, offset为0
     *
     * 2. 当前在"3":
     * onPageSelected:              position为2
     * onPageScrolled:              position为2, offset为0
     *
     * 可以看到, 同样是remove数据, 如果当前item的index小于count, position不变, 只是调用一下onPageScrolled,
     * 好像在说, 你很安全, 继续在这呆着吧, 当然, 对于数据增加或是不变的情况, 调用的效果也是这样. 如果当前的item的index
     * 大于等于count了, 说明"越界"了, 这时候好像有人在说, 兄弟, 往里挪挪吧, 你出界了, 这时会调用onPageSelected和
     * onPageScrolled, position为count-1.
     *
     * 鉴于循环效果的设计, 以[3,0,1,2,3,0]为例:
     * 如果当前处于index为4的"3"的情况下remove掉一个数据后变为[2,0,1,2,0], index为4的"3"变成了"0", 仍然是"安全"的,
     * 但是根据"无限循环"的设计, 只有通过滑动才能展示首尾两个item, 也就是说, 此时尽管是"安全"的, 但是这并不是一个可以
     * "呆着"的地方, 我们可以把它理解为"越界"了, 如何处理呢？
     * 我们需要考虑一下用户的体验, 通过onPageSelected, indicator此刻已经知道了当前展示的是"0", 也就是已经完成了动画
     * 的展示, 自然用户也看到了. 这个时候我们只能"将错就错", 也就是显示index为1的"0".
     *
     * 实现方案:
     * 可以看到只有onPageScrolled是一定会调用的, 因此我们只能在这里处理.
     * position为count-1的情况有两种: item滑动或是删除数据.
     * 如果是item滑动, offset会在[0, 1)变化, 在state为idle前变为0, 如果是删除数据, 会调用一次onPageScrolled,
     * offset为0, 那么如果两次offset均为0的话, 我们便可以认为是进行了数据更新.
     */
    private val mOnPageChangeListener = object : OnPageChangeListener {
        private var mPreviousPosition = -1
        private var mPreviousOffset = -1f

        /**
         * position指的是屏幕中间可见的最左侧item的index.
         * positionOffset为position对应item的偏移比例, 取值[0, 1).
         * positionOffsetPixels为position对应item的偏移的像素值.
         *
         * 以[0,1,2,3]为例, 如果当前为"1", 那么position为1, positionOffset为0.
         * 如果向右滑动, "1"向右移动, "0"从左侧进入, 此时position变为0, positionOffset由1逐渐减小.
         * 如果向左滑动, "1"向左移动, "2"从右侧进入, 此时position仍为1, positionOffset由0逐渐增大.
         *
         * 鉴于循环效果的设计, 以[3,0,1,2,3,0]为例:
         * 由于在首尾添加了两个item, 我们需要传递"修正"position, 这样外部的indictor就可以显示在正确的位置.
         * 但是我们需要考虑两种情况:
         * 1. 当在首部的"0"时, indicator已经处于最左侧, 如果此时继续向右滑动.
         * 2. 当在尾部的"3"时, indicator已经处于最右侧, 如果此时继续向左滑动.
         * 我们需要看到的效果是:
         * 1. "3"从左侧进入, indicator保持不动, 当"3"进入50%后, indicator移动至最右侧.
         * 2. "0"从右侧进入, indicator保持不动, 当"0"进入50%后, indicator移动至最左侧.
         * 实现方案:
         * 1. "3"从左侧进入意味着"修正"position变为"3", "0"从右侧进入意味着"修正"position仍为"3", 也就是说当
         * "修正"position为3的时候就需要引起我们的注意了.
         * 2. indicator保持不动意味着positionOffset和positionOffsetPixels两个参数传0.
         * 3. positionOffset小于0.5意味着"3"从左侧进入并且超过50%或是"0"从右侧进入没超过50%, 此时position传3, 否则传0.
         */
        override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
            if (mPreviousOffset == 0f && positionOffset == 0f) {
                mLoopAdapter?.run {
                    if (count > 1 && position == count - 1) {
                        setCurrentItem(0, false)
                    }
                }
            }

            mPreviousOffset = positionOffset

            val revisedPosition = getRevisedPosition(position)

            mOnPageChangeListeners.forEach { listener ->
                mRawAdapter?.run {
                    if (revisedPosition == count - 1) {
                        if (positionOffset < .5f)
                            listener.onPageScrolled(revisedPosition, 0f, 0)
                        else
                            listener.onPageScrolled(0, 0f, 0)

                    } else
                        listener.onPageScrolled(revisedPosition, positionOffset, positionOffsetPixels)
                }
            }
        }

        /**
         * 鉴于循环效果的设计, 为了防止在首尾滑动item后indicator的动画出现两次, 这里需要判断一下相邻两次的"修正"position是否相同.
         */
        override fun onPageSelected(position: Int) {
            val revisedPosition = getRevisedPosition(position)
            if (mPreviousPosition != revisedPosition) {
                mPreviousPosition = revisedPosition

                mOnPageChangeListeners.forEach { listener -> listener.onPageSelected(revisedPosition) }
            }
        }

        /**
         * 当滑动停止, 我们需要根据当前的index进行对应的处理, 以[3,0,1,2,3,0]为例:
         * 如果当前的index为0, 说明已经滑动至最左侧的"3", 为了营造出循环的效果, 这时需要跳转到index为4的"3".
         * 如果当前的index为5, 说明已经滑动至最右侧的"0", 为了营造出循环的效果, 这时需要跳转到index为1的"0".
         */
        override fun onPageScrollStateChanged(state: Int) {
            if (state == SCROLL_STATE_IDLE) {
                mRawAdapter?.let { rawAdapter ->
                    when (super@LoopViewPager.getCurrentItem()) {
                        0 -> setCurrentItem(rawAdapter.count - 1, false)
                        rawAdapter.count + 1 -> setCurrentItem(0, false)
                    }
                }

                mOnPageChangeListeners.forEach { listener -> listener.onPageScrollStateChanged(state) }
            }
        }
    }

    private var mRawAdapter: PagerAdapter? = null
    private var mLoopAdapter: LoopPagerAdapter? = null

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.LoopViewPager, 0, 0)
        mAutoPageEnabled = a.getBoolean(R.styleable.LoopViewPager_pager_autopage_enabled, false)
        mAutoPagePeriod = a.getInt(R.styleable.LoopViewPager_pager_autopage_period, DEFAULT_AUTO_PAGER_PERIOD).toLong()
        a.recycle()

        super.addOnPageChangeListener(mOnPageChangeListener)

        touches { event ->
            if (mAutoPageEnabled)
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> mAutoPageStops.onNext(Unit)
                    MotionEvent.ACTION_UP -> mAutoPageStarts.onNext(Unit)
                }
            false
        }
                .to(RxUtil.autoDispose(context as LifecycleOwner))
                .subscribe { }

        mAutoPageStarts.switchMap {
            Observable.interval(mAutoPagePeriod, TimeUnit.MILLISECONDS)
                    .takeUntil(mAutoPageStops)
                    .filter { mRawAdapter?.run { count > 1 } ?: false }
        }
                .compose(RxUtil.getSchedulerComposer())
                .to(RxUtil.autoDispose(context as LifecycleOwner))
                .subscribe {
                    mRawAdapter?.run {
                        val position = currentItem
                        if (position == count - 1)
                            currentItem = 0
                        else
                            setCurrentItem(position + 1, true)
                    }
                }
    }

    /**
     * 这里需要将currentItem置为0, 否则, 以[3,0,1,2,3,0]为例, 会默认显示"3"而不是"0".
     */
    override fun setAdapter(adapter: PagerAdapter?) {
        mRawAdapter = adapter

        mRawAdapter?.let { rawAdapter ->
            rawAdapter.registerDataSetObserver(object : DataSetObserver() {
                override fun onChanged() {
                    mLoopAdapter?.notifyDataSetChanged()
                }
            })

            mLoopAdapter = LoopPagerAdapter(rawAdapter)
            super.setAdapter(mLoopAdapter)
            currentItem = 0

            if (mAutoPageEnabled) {
                mAutoPageStops.onNext(Unit)
                mAutoPageStarts.onNext(Unit)
            }

        } ?: super.setAdapter(null)
    }

    override fun getAdapter(): PagerAdapter? = mRawAdapter

    /**
     * [ViewPager.getCurrentItem]获得的是"real"的index, 由于在首尾添加了两个item, 因此此处需要"修正"一下.
     */
    override fun getCurrentItem(): Int {
        val currentItem = super.getCurrentItem()

        mRawAdapter?.let { rawAdapter ->
            if (rawAdapter.count > 0) {
                mLoopAdapter?.let { loopAdapter ->
                    if (currentItem < loopAdapter.count - 1)
                        return getRevisedPosition(currentItem)
                }
            }
        }

        return 0
    }

    override fun setCurrentItem(item: Int) = setCurrentItem(item, false)

    /**
     * 由于在首尾添加了两个item, 因此此处的index应做+1处理, 也就是传递"real"的index.
     */
    override fun setCurrentItem(item: Int, smoothScroll: Boolean) = super.setCurrentItem(item + 1, smoothScroll)

    override fun setOnPageChangeListener(listener: OnPageChangeListener) = addOnPageChangeListener(listener)

    override fun addOnPageChangeListener(listener: OnPageChangeListener) = mOnPageChangeListeners.add(listener).run { Unit }

    override fun removeOnPageChangeListener(listener: OnPageChangeListener) = mOnPageChangeListeners.remove(listener).run { Unit }

    override fun clearOnPageChangeListeners() = mOnPageChangeListeners.clear()

    fun getRevisedPosition(position: Int): Int =
            mRawAdapter?.run {
                getRevisedPosition(position, count)
            } ?: throw Exception("Adapter is NULL")
}