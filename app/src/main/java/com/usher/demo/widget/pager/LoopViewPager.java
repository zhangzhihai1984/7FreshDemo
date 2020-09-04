
package com.usher.demo.widget.pager;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.usher.demo.R;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * 这是一个实现了"无限循环"效果的{@link ViewPager}。
 * <p>
 * 以4个item的[0,1,2,3]为例,我们期望的视觉效果是在当前在"0"时，继续向右滑动，会转至"3"，当前在"3"时，继续向左滑动，会转至"0"。
 * 但是{@link ViewPager}本身是不支持循环滑动的，既然这样，那么我们只能通过某种视觉上的错觉来实现所谓的无限循环效果。
 * <p>
 * 我们的设计思路是在首尾添加两个item，形成4+2个item: [3,0,1,2,3,0]，这样就实现了上述的效果，但是如何实现"无限循环"呢？
 * <p>
 * 当我们在index为1的"0"继续向右滑动，转至index为0的"3"后，立即通过{@link ViewPager#setCurrentItem(int, boolean)}
 * 转至index为4的"3"，那么继续向右滑动的话，就会转至"2"，同理，当我们在index为4的"3"，继续向左滑动，转至index为5的"0"后，
 * 立即通过{@link ViewPager#setCurrentItem(int, boolean)}转至index为1的"0"，那么继续向左滑动的话，就会转至"1"，
 * 这样，就通过这种视觉上的错觉实现了"无限循环"效果。
 * <p>
 * 这里有一点需要注意，如果你的Adapter是继承自{@link FragmentPagerAdapter}或{@link FragmentStatePagerAdapter}，
 * 在实现{@link FragmentPagerAdapter#getItem(int)}或{@link FragmentStatePagerAdapter#getItem(int)}时，
 * 不要直接使用position的值，因为那个值是real的，是针对count+2而言的，需要你将该position和Adapter的count传给
 * {@link LoopViewPager#getMatchedPosition(int, int)}，使用该方法返回的index来获取你列表中的数据，以生成相应的
 * {@link Fragment}。具体原因可查看{@link LoopPagerAdapter#instantiateItem(ViewGroup, int)}
 * <p>
 * <p>
 * <strong>如果你有更新数据的需求的话，不要开启缓存模式(默认不开启)。</strong>同时，建议你的Adapter不要继承自{@link FragmentPagerAdapter}，
 * 具体原因可参见{@link #setCacheEnabled(boolean)}，如果重写{@link FragmentPagerAdapter#instantiateItem(ViewGroup, int)}和
 * {@link FragmentPagerAdapter#destroyItem(ViewGroup, int, Object)}的话，为什么不直接使用{@link FragmentStatePagerAdapter}。
 */
public class LoopViewPager extends ViewPager {
    private static final int DEFAULT_AUTO_PAGER_INTERVAL = 1000;

    private final List<OnPageChangeListener> mOnPageChangeListeners = new ArrayList<>();

    private LoopPagerAdapter mLoopAdapter;
    private PagerAdapter mRawAdapter;

    private AutoPageHandler mAutoPageHandler;

    private boolean mIsAutoPageEnabled = false;
    private int mAutoPageInterval;

    public LoopViewPager(Context context) {
        this(context, null);
    }

    public LoopViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.LoopViewPager);

        mIsAutoPageEnabled = a.getBoolean(R.styleable.LoopViewPager_pager_autopage_enabled, false);
        mAutoPageInterval = a.getInt(R.styleable.LoopViewPager_pager_autopage_interval, DEFAULT_AUTO_PAGER_INTERVAL);

        a.recycle();

        super.addOnPageChangeListener(mOnPageChangeListener);
    }

    /**
     * 这里需要主动调用一下setCurrentItem(0)，否则，以[3,0,1,2,3,0]为例，会默认显示"3"而不是"0"。
     */
    @Override
    public void setAdapter(PagerAdapter adapter) {
        mRawAdapter = adapter;

        if (mRawAdapter != null) {
            mRawAdapter.registerDataSetObserver(mDataSetObserver);

            mLoopAdapter = new LoopPagerAdapter(adapter);

            if (mIsAutoPageEnabled) {
                disableAutoPage();
                enableAutoPage(mAutoPageInterval);
            }

            super.setAdapter(mLoopAdapter);

            setCurrentItem(0);
        } else {
            super.setAdapter(null);
        }
    }

    @Override
    public PagerAdapter getAdapter() {
        return mRawAdapter;
    }

    /**
     * {@link ViewPager#getCurrentItem()}获得的是real的position，
     * 我们要将其转换为去掉首尾两个item的position。
     * <p>
     * 以[3,0,1,2,3,0]为例，当我们在index为4的"3"向左滑动屏幕滑至最右侧index为5的"0"时，根据我们无限循环的设计，会瞬间转至index为1的"0",
     * 因此当外界调用该方法时，一旦出现索引值大于等于count-1时，说明此时进行了数据更新(发生了数据更新不一定会发生这种情况，但发生这种情况肯定是
     * 由数据更新引起的)，考虑到用户会使用Indicator，参见{@link LoopViewPager#mOnPageChangeListener}我们对这种情况的处理方案为立即展现
     * position为0的item，相应的，这里返回0，让Indicator获取到正确的position。
     */
    @Override
    public int getCurrentItem() {
        if (mRawAdapter != null && mRawAdapter.getCount() > 0) {
            if (super.getCurrentItem() >= mLoopAdapter.getCount() - 1) {
                return 0;
            } else {
                return getMatchedPosition(super.getCurrentItem());
            }
        }

        return 0;
    }

    /**
     * 我们实际上在首尾添加了两个item，此处的index应做加1处理，传递real的position。
     * 比如说，用户想要展示首页面，穿过来的item为0，但是实际上，首页面的index为1.
     */
    @Override
    public void setCurrentItem(int item, boolean smoothScroll) {
        if (mRawAdapter == null || mRawAdapter.getCount() <= 0)
            return;

        if (item >= mRawAdapter.getCount()) {
            item = mRawAdapter.getCount() - 1;
        }

        super.setCurrentItem(item + 1, smoothScroll);
    }

    @Override
    public void setCurrentItem(int item) {
        setCurrentItem(item, false);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void setOnPageChangeListener(@NonNull OnPageChangeListener listener) {
        addOnPageChangeListener(listener);
    }

    @Override
    public void addOnPageChangeListener(@NonNull OnPageChangeListener listener) {
        mOnPageChangeListeners.add(listener);
    }

    @Override
    public void removeOnPageChangeListener(@NonNull OnPageChangeListener listener) {
        mOnPageChangeListeners.remove(listener);
    }

    @Override
    public void clearOnPageChangeListeners() {
        mOnPageChangeListeners.clear();
    }

    /**
     * 主要是防止内存泄漏
     */
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (mIsAutoPageEnabled) {
            mAutoPageHandler.removeMessages(0);
            mAutoPageHandler = null;
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (mIsAutoPageEnabled) {
            mAutoPageHandler.removeMessages(0);

            if (ev.getAction() == MotionEvent.ACTION_UP) {
                mAutoPageHandler.sendEmptyMessageDelayed(0, mAutoPageInterval);
            }
        }

        return super.onTouchEvent(ev);
    }

    public static int getMatchedPosition(int realPosition, int count) {
        if (count <= 0)
            throw new IllegalStateException("count must be greater than 0");

        return (realPosition - 1 + count) % count;
    }

    private int getMatchedPosition(int realPosition) {
        return getMatchedPosition(realPosition, mRawAdapter.getCount());
    }

    /**
     * 这个方法用于缓存首尾两个item，以防止在首尾两个item继续滑动进入循环时，页面出现"闪动了一下"的效果。
     * 如果你有更新数据的需求的话，不要开启缓存模式(默认不开启)。
     * <p>
     * 以[3,0,1,2,3,0]为例，当我们在index为4的"3"向左滑动屏幕滑至最右侧index为5的"0"时，根据我们无限循环的设计，会瞬间转至index为1的"0",
     * 但是有个问题，当前{@link ViewPager}维护的是index为4，5，6的"2"，"3"，"0"，转后的"0"的index为0，是不在其中的，也就是说需要调用
     * {@link PagerAdapter#instantiateItem(ViewGroup, int)}，一旦该Fragement或View不能复用，也就是说重新new的话，对于UI的初始化的
     * 过程是需要时间的，一旦时间稍长，用户就能明显的看到：明明加载好的页面，某个部分或是整个页面变为了空白，然后又加载出来，给人一种"闪动了一下"的效果。
     * <p>
     * 用户看到的其实是两个item瞬间的转换，但是由于这种"视觉上的错觉"没有处理好，导致漏了馅儿。
     * <p>
     * 我们处理的方法就是将首尾两个item缓存起来，当调用{@link PagerAdapter#instantiateItem(ViewGroup, int)}时，如果有缓存，直接将
     * 对应的Fragment或View返回。
     * <p>
     * 一般来说，尽管看上去{@link ViewPager}维护的是当前及左右共计3个item，但对于{@link FragmentPagerAdapter}不会有上述的情况，
     * 在{@link FragmentPagerAdapter#destroyItem(ViewGroup, int, Object)}中，创建的Fragment只是被detach掉，当重回视线
     * 调用{@link FragmentPagerAdapter#instantiateItem(ViewGroup, int)}时，会复用这个Fragment，重新将其attach到页面中。
     * 而在{@link FragmentStatePagerAdapter#destroyItem(ViewGroup, int, Object)}中，Fragment是直接被remove掉，
     * 当重回视线调用{@link FragmentStatePagerAdapter#instantiateItem(ViewGroup, int)}时，会重新new一个Fragment。
     */
    public void setCacheEnabled(boolean enabled) {
//        mIsCacheEnabled = enabled;
//        if (mLoopAdapter != null) {
//            mLoopAdapter.setCacheEnabled(enabled);
//        }
    }

    public void enableAutoPage() {
        enableAutoPage(mAutoPageInterval);
    }

    public void enableAutoPage(int millisecond) {
        mIsAutoPageEnabled = true;
        mAutoPageInterval = millisecond;

        if (mAutoPageHandler == null) {
            mAutoPageHandler = new AutoPageHandler(this);
        }

        mAutoPageHandler.removeMessages(0);
        mAutoPageHandler.sendEmptyMessageDelayed(0, mAutoPageInterval);
    }

    public void disableAutoPage() {
        mIsAutoPageEnabled = false;

        if (mAutoPageHandler != null) {
            mAutoPageHandler.removeMessages(0);
        }
    }

    public boolean isAutoPageEnabled() {
        return mIsAutoPageEnabled;
    }

    private void autoPage() {
        if (mRawAdapter.getCount() <= 1)
            return;

        int position = getCurrentItem();

        if (position == mRawAdapter.getCount() - 1) {
            setCurrentItem(0);
        } else {
            setCurrentItem(position + 1, true);
        }
    }

    /**
     * 以[0,1,2,3]从"1"向左滑动至"2"为例，讲一下接口中三个方法的调用顺序。
     * <p>
     * 对于用户手动滑动，三个方法均会调用，调用顺序为：
     * onPageScrollStateChanged(开始拖动即drag) -> onPageScrolled(持续多次，position为0，offset从0开始向1增加) ->
     * onPageScrollStateChanged(松手即settle)  -> onPageSelected(position为1) ->
     * onPageScrolled(持续多次，position为0，offset逐渐接近1) -> onPageScrolled(position为1，offset为0) ->
     * onPageScrollStateChanged(静止即idle)
     * <p>
     * 对于调用setCurrentItem实现页面的切换，
     * 如果smoothScroll为true，调用顺序为：
     * onPageScrollStateChanged(state直接为settle) -> onPageSelected(position为1) ->
     * onPageScrolled(持续多次，position为0，offset从0开始逐渐接近1) -> onPageScrolled(position为1，offset为0)
     * onPageScrollStateChanged(静止即idle)
     * <p>
     * 如果smoothScroll为false，调用顺序为：
     * onPageSelected(position为1) -> onPageScrolled(position为1，offset为0)
     * <p>
     * <p>
     * 以[0,1,2,3]为例，讲一下发生数据更新时接口方法的调用顺序。
     * <p>
     * 如果当前position为2，数据remove掉一个，调用顺序为：
     * onPageScrolled(position为2，offset为0)
     * <p>
     * 如果当前position为3，数据remove掉一个，调用顺序为：
     * onPageSelected(position为2) -> onPageScrolled(position为2，offset为0)
     * <p>
     * 我们可以看到，前一种情况，虽然remove掉了一个数据，但是由于当前item的position仍然小于count，并未越界，因此，position没变，只是调用了一下
     * onPageScrolled，好像在说，你很安全，继续在这呆着吧。同理，对于数据count增加或不变的情况，调用顺序也是这样的。但是对于后一种，remove掉了一个数据后，
     * 当前item的position已经等于count了，当然，如果remove掉的数据多的话，也会大于count，性质是一样的，都是导致了越界。这个时候，好像有人在说，
     * 兄弟，往里挪挪吧，你出界了。既然position发生了变化，自然就会调用onPageSelected，position为count-1。
     * <p>
     * <p>
     * 说了这么多，其实就是要说一下，对于支持"无限循环"的ViewPager在数据更新的情况下如何处理。
     * 以[3,0,1,2,3,0]为例，如果当前position是为3的"2"，数据remove掉一个后变为[2,0,1,2,0]，position为3的"2"很安全，没问题。
     * 如果当前的position为4的"3"，数据remove掉一个后变为[2,0,1,2,0]，position为4的"3"变为了"0"，依然安全，但是根据"无限循环"的设计，只有用户通过
     * 滑动屏幕才能到这个位置，然后通过跳转实现"无限循环"效果，可以看一下{@link #autoPage()}的实现，autoPage是不会展示首尾两个item的。所以此时，
     * 我们应该将其往前挪动一位，也就是应该显示position为3的"2"。
     * <p>
     * 还是上面的例子，如果当前的position是为4的"3"，数据remove掉两个后会变为[1,0,1,0]，此时由于越界，ViewPager会自动帮我们"往里挪挪"，结果是position变为3，
     * 还是"0"，跟上面一样，此时将其往前挪动一位，显示position为2的"1"。
     * <p>
     * 对于这种"往前挪动一位"的设计看上去已经没有什么问题了，但是我们需要考虑一下用户可能还会使用Indicator。我们可以看到，上面两种情况，position都停留在了最后一位，
     * 当Indicator通过{@link #getCurrentItem()}获取当前的位置时，我们会告知Indicator此时的position为0。尽管我们通过"往前挪动一位"的方式对position进行了
     * "纠正"，但是用户看到的效果就是，数据更新后，Indicator的小圆点或其他从首尾转至了最后一位，虽然这不会影响什么，但是还是有那么一点奇怪。
     * <p>
     * 那么，我们的解决方案就是，如果数据更新后，导致当前的position停留在了最后一位，我们就索性将其挪至position为0的位置。这样，从视觉上，用户就看不出什么异常了。
     * <p>
     * 接下来就是实现方案了。根据上面讲的调用顺序，对于数据更新，只有onPageScrolled是肯定会调用的，甚至只调用它，那么我们就只能在这个方法中进行处理。
     * 我们首先来看一下position这个参数。按照上面的解决方案，position的值需要为索引的最后一位，目前只有两种情况下会出现这种情况，第一个是用户手动滑动到
     * position为最后的"0"，这时需要跳转实现"无限循环"效果，另一个是数据更新导致的。那么我就需要通过方法中的另一个参数positionOffset来进行区分。
     * 如果是处于滑动状态，它会在(0,1)之间不断变化，在state为idle前变为0，如果是数据更新，会调用一次onPageScrolled，值为0。那么我们给出的区分的条件
     * 就是相邻两次的offset均为0。
     */
    private final OnPageChangeListener mOnPageChangeListener = new OnPageChangeListener() {
        private int mPreviousPosition = -1;
        private float mPreviousOffset = -1;

        /**
         * 防止在实现循环效果时，页面的Indicator调用两次，导致用户看到首尾item的动画play两次。
         * 具体原理参见{@link LoopViewPager#mOnPageChangeListener#onPageScrollStateChanged(int)}
         */
        @Override
        public void onPageSelected(int position) {
            int matchedPosition = getMatchedPosition(position);

            if (mPreviousPosition != matchedPosition) {
                mPreviousPosition = matchedPosition;

                for (OnPageChangeListener listener : mOnPageChangeListeners) {
                    listener.onPageSelected(getMatchedPosition(position));
                }
            }
        }

        /**
         * position的值指的是屏幕中可见的最左侧item的index，positionOffset为position对应item的偏移比例，取值[0, 1)，
         * positionOffsetPixels为偏移的像素。
         *
         * 以[0,1,2,3]为例，如果当前显示的是"1"，那么position为1，positionOffset为0。
         * 如果向左滑动屏幕，那么屏幕上可以看到"1"向左移动，"2"从右侧进入，此时的position仍为1，positionOffset从0逐渐增加增大。
         * 如果向右滑动屏幕，那么屏幕上可以看到"1"向右移动，"0"从左侧进入，此时的position为最左侧的0，position从1逐渐减小。
         *
         * 一般来讲，伴随着用户滑动，进而进行缩放，移动等效果的indicator会很关心这个方法的实现。
         * 对于循环效果，以[3,0,1,2,3,0]为例，最关键的如何处理首尾两个item之间滑动。
         * 鉴于我们的设计，用户可进行的操作只有position为1的"0"向右滑动转至position为0的"3"和position为4的"3"向左滑动转至position为5的"0"两种。
         * 前一种向右滑动，item为"3"从最左侧出现，后一种向左滑动，最左侧的item仍然为"3"，这样的话，只要不是item为"3"的情况，我们不需要做任何额外的处理。
         *
         * 站在用户视觉的角度，当处于第一个页面继续向右滑动屏幕时，indicator此时已经处于首部，不可能继续向前移动，只能转至尾部，
         * 同理，当处于最后一个页面继续向左滑动屏幕时，indicator此时已经处于尾部，不可能继续向后移动，只能转至首部，
         * 因此，此时对于positionOffset和positionOffsetPixels两个参数我们只能传0。
         *
         * 对于position参数的传递，我们继续讨论上面的两个操作。我们期望的效果是：
         * 前一种，向右滑动，index为0的item"3"的positionOffset从1逐渐减小，当屏幕滑过一半时(positionOffset<0.5)，indicator从首部转至尾部。
         * 后一种，向左滑动，index为4的item"3"的positionOffset从0逐渐增大，当屏幕滑过一半时(positionOffset>0.5)，indicator从尾部转至首部。
         */
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            if (mPreviousOffset == 0 && positionOffset == 0) {
                if (mLoopAdapter.getCount() > 1 && position == mLoopAdapter.getCount() - 1) {
                    setCurrentItem(0, false);
                }
            }

            mPreviousOffset = positionOffset;

            int matchedPosition = getMatchedPosition(position);

            for (OnPageChangeListener listener : mOnPageChangeListeners) {
                if (matchedPosition != mRawAdapter.getCount() - 1) {
                    listener.onPageScrolled(matchedPosition, positionOffset, positionOffsetPixels);
                } else {
                    if (positionOffset > .5) {
                        listener.onPageScrolled(0, 0, 0);
                    } else {
                        listener.onPageScrolled(mRawAdapter.getCount() - 1, 0, 0);
                    }
                }
            }
        }

        /**
         * 当滑动停止，也就是state为{@link ViewPager#SCROLL_STATE_IDLE}时，我们需要判断一下当前的position，
         * 以[3,0,1,2,3,0]为例，如果为0或5，也就是我们在首尾添加的两个item，那么应做如下处理：
         * 如果position为0，那么当前已滑动至最左侧，显示的是"3"，如果用户继续向右滑动的话，应该看到"2"，
         * 那么，为了能够营造出循环的效果，这时需要跳转到position为4的"3"。
         * 如果position为5，那么当前已滑动至最右侧，显示的是"0"，如果用户继续向左滑动的话，应该看到"1"，
         * 那么，为了能够营造出循环的效果，这时需要跳转到position为1的"0"。
         *
         * 之所以在这个方法中处理跳转，是因为，我们需要等到滑动停止之后再做处理，否则会出现"跳动"的效果。
         */
        @Override
        public void onPageScrollStateChanged(int state) {
            if (state == SCROLL_STATE_IDLE) {
                int position = LoopViewPager.super.getCurrentItem();

                if (position == 0) {
                    setCurrentItem(mRawAdapter.getCount() - 1, false);
                }

                if (position == mLoopAdapter.getCount() - 1) {
                    setCurrentItem(0, false);
                }
            }

            for (OnPageChangeListener listener : mOnPageChangeListeners) {
                listener.onPageScrollStateChanged(state);
            }
        }
    };

    private final DataSetObserver mDataSetObserver = new DataSetObserver() {
        @Override
        public void onChanged() {
            mLoopAdapter.notifyDataSetChanged();
        }
    };

    private static class AutoPageHandler extends Handler {
        private final WeakReference<LoopViewPager> mTarget;

        AutoPageHandler(LoopViewPager loopViewPager) {
            mTarget = new WeakReference<>(loopViewPager);
        }

        @Override
        public void handleMessage(Message msg) {
            LoopViewPager loopViewPager = mTarget.get();

            if (loopViewPager != null) {
                loopViewPager.autoPage();
                loopViewPager.mAutoPageHandler.sendEmptyMessageDelayed(0, loopViewPager.mAutoPageInterval);
            }
        }
    }
}