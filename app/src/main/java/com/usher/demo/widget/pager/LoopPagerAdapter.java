package com.usher.demo.widget.pager;

import android.os.Parcelable;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

/**
 * 与{@link LoopViewPager}配合使用的Adapter，你通过调用{@link LoopViewPager#setAdapter(PagerAdapter)},
 * 最终生成的其实是{@link LoopPagerAdapter}，这个类不暴露给用户使用。
 */
class LoopPagerAdapter extends PagerAdapter {

    private final PagerAdapter mRawAdapter;

    LoopPagerAdapter(PagerAdapter adapter) {
        mRawAdapter = adapter;
    }

    /**
     * 为什么都是{@link PagerAdapter}，却要对{@link FragmentPagerAdapter}和{@link FragmentStatePagerAdapter}要区别对待呢？
     * <p>
     * 从名字上可以看出，这两个support包中仅有的两个与{@link ViewPager}
     * 配合的Adapter都是与{@link Fragment}相关。
     * <p>
     * 以{@link FragmentPagerAdapter}为例，{@link FragmentPagerAdapter#instantiateItem(ViewGroup, int)}中是以position作为itemId，
     * 进而生成一个name作为Fragment的Tag，{@link FragmentManager}通过这个Tag来维护所有的Fragment，
     * 如果有该Tag对应的Fragment，则复用这个Fragment，否则调用{@link FragmentPagerAdapter#getItem(int)}生成一个新的Fragment。
     * <p>
     * 还是以4个元素为例，最终形成的6个元素效果为[3,0,1,2,3,0]，对于两个"0"元素，如果position都传所谓"real"0的话，
     * 那么index为1和5的两个"0"由于position相同，因此会产生相同的Tag，那么，二者其实使用的是同一个Fragment，可以说是同呼吸共命运。
     * <p>
     * <strong>注：</strong>这里相关方法调用的次数是基于{@link ViewPager#getOffscreenPageLimit()}]为默认的1，即只保留当前及左右共计3个item。
     * 这样，当我们在index为4的"3"向左滑动屏幕滑至最右侧index为5的"0"时，根据我们无限循环的设计，会瞬间转至index为1的"0",
     * 这时，会调用三次{@link FragmentPagerAdapter#instantiateItem(ViewGroup, int)}，
     * position分别为当前的1以及左右的0和2，此时index为1的"0"已存在 (与index为5的"0"为同一个Fragment)，因此复用，
     * 随后，调用三次{@link FragmentPagerAdapter#destroyItem(ViewGroup, int, Object)}，
     * position分别为跳转之前的4，5，6分别对应"2"，"3"，"0"，两个Fragment都会detach掉。
     * <p>
     * 对于index为5的"0"，由于Fragment被detach掉，那么身为"同呼吸共命运"的另一个"0"的Fragment自然也会从界面中消失，
     * 那么我们看到的效果就是当前的"0"变为了空白。
     * <p>
     * 因此，我们必须要传递"real"的position给{@link FragmentPagerAdapter}，
     * 这样，两个不同位置的"0"就是两个Fragment了，不会出问题了(对于"3"而言，道理也是一样)。
     * <p>
     * 对于我们自己实现的ViewPager，在重写{@link PagerAdapter#instantiateItem(ViewGroup, int)}时，
     * 返回的是一个View，而非Fragment，不涉及Fragment的管理，自然就没有上面的问题了。
     * <p>
     * 需要注意的是，当我们实现{@link FragmentPagerAdapter#getItem(int)}时，
     * 传过来的position是{@link FragmentPagerAdapter#instantiateItem(ViewGroup, int)}中的position，
     * 它是"real"的，因此我们需要调用一下{@link LoopViewPager#getMatchedPosition(int, int)},
     * 将这个"real" position转换成[1,2,3]对应的index，即[0->3, 1->0, 2->1, 3->2, 4->3, 5->0]。
     */
    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        int realPosition = (mRawAdapter instanceof FragmentPagerAdapter || mRawAdapter instanceof FragmentStatePagerAdapter)
                ? position
                : getMatchedPosition(position);

        return mRawAdapter.instantiateItem(container, realPosition);
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        int realPosition = (mRawAdapter instanceof FragmentPagerAdapter || mRawAdapter instanceof FragmentStatePagerAdapter)
                ? position
                : getMatchedPosition(position);

        mRawAdapter.destroyItem(container, realPosition, object);
    }

    @Override
    public int getCount() {
        if (mRawAdapter.getCount() <= 0)
            return 0;

        if (mRawAdapter.getCount() == 1)
            return 1;

        return mRawAdapter.getCount() + 2;
    }

    /**
     * 出于用户体验的考虑，为了能在进行数据更新后当前页面也立即进行更新，这里直接返回{@link PagerAdapter#POSITION_NONE}。
     */
    @Override
    public int getItemPosition(@NonNull Object object) {
//        return mRawAdapter.getItemPosition(object);
        return POSITION_NONE;
    }

    private int getMatchedPosition(int realPosition) {
        int count = mRawAdapter.getCount();
        if (count <= 0)
            return 0;

        return (realPosition - 1 + count) % count;
    }

    /*
     * Delegate rest of methods directly to the inner adapter.
     */

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return mRawAdapter.isViewFromObject(view, object);
    }

    @Override
    public void startUpdate(@NonNull ViewGroup container) {
        mRawAdapter.startUpdate(container);
    }

    @Override
    public void finishUpdate(@NonNull ViewGroup container) {
        mRawAdapter.finishUpdate(container);
    }

    @Override
    public void setPrimaryItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        mRawAdapter.setPrimaryItem(container, position, object);
    }

    @Override
    public void restoreState(Parcelable bundle, ClassLoader classLoader) {
        mRawAdapter.restoreState(bundle, classLoader);
    }

    @Override
    public Parcelable saveState() {
        return mRawAdapter.saveState();
    }

    @Override
    public float getPageWidth(int position) {
        return mRawAdapter.getPageWidth(position);
    }
}