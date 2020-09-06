package com.usher.demo.base

import android.view.View
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.PublishSubject

abstract class RxBaseQuickAdapter<T, K : BaseViewHolder>(layoutResId: Int, data: List<T>) : BaseQuickAdapter<T, K>(layoutResId, data) {
    private val mItemClickSubject = PublishSubject.create<Int>()
    private val mItemLongClickSubject = PublishSubject.create<Int>()
    private val mItemChildClickSubject = PublishSubject.create<ClickItem>()

    data class ClickItem(val view: View, val position: Int)

    init {
        setOnItemClickListener { _, _, position -> mItemClickSubject.onNext(position) }

        setOnItemLongClickListener { _, _, position ->
            mItemLongClickSubject.onNext(position)
            false
        }

        setOnItemChildClickListener { _, view, position -> mItemChildClickSubject.onNext(ClickItem(view, position)) }
    }

    fun itemClicks(): Observable<Int> = mItemClickSubject

    fun itemLongClicks(): Observable<Int> = mItemLongClickSubject

    fun itemChildClicks(): Observable<ClickItem> = mItemChildClickSubject

//    fun loadMore(recyclerView: RecyclerView, loadingView: LoadMoreView = EmptyLoadMoreView()): Observable<Unit> =
//            Observable.create { emitter ->
//                setLoadMoreView(loadingView)
//                setOnLoadMoreListener({ emitter.onNext(Unit) }, recyclerView)
//            }
}