package com.usher.demo.widget.dialog

import android.app.Dialog
import android.content.Context
import android.text.Spannable
import android.view.View
import androidx.lifecycle.LifecycleOwner
import com.jakewharton.rxbinding4.view.clicks
import com.usher.demo.R
import com.usher.demo.util.RxUtil
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.PublishSubject
import kotlinx.android.synthetic.main.dialog_common_layout.*

class CommonDialog(context: Context) : Dialog(context, R.style.FramelessDialog) {
    private val mClickSubject = PublishSubject.create<Click>()
    private var mData: Any? = null

    enum class ClickType {
        CONFIRM,
        CANCEL
    }

    enum class ButtonType {
        SINGLE,
        DOUBLE_PRIMARY,
        DOUBLE_WARN
    }

    class Click(var type: ClickType, var data: Any?)

    init {
        setContentView(R.layout.dialog_common_layout)

        setCanceledOnTouchOutside(false)
        setCancelable(false)

        cancel_textview.clicks()
                .take(1)
                .to(RxUtil.autoDispose(context as LifecycleOwner))
                .subscribe {
                    mClickSubject.onNext(Click(ClickType.CANCEL, mData))
                    dismiss()
                }

        Observable.merge(confirm_textview.clicks(), done_textview.clicks())
                .take(1)
                .to(RxUtil.autoDispose(context as LifecycleOwner))
                .subscribe {
                    mClickSubject.onNext(Click(ClickType.CONFIRM, mData))
                    dismiss()
                }
    }

    fun withDialogType(type: ButtonType): CommonDialog = this.apply {
        when (type) {
            ButtonType.DOUBLE_PRIMARY, ButtonType.DOUBLE_WARN -> {
                double_layout.visibility = View.VISIBLE
                single_layout.visibility = View.GONE
            }
            ButtonType.SINGLE -> {
                double_layout.visibility = View.GONE
                single_layout.visibility = View.VISIBLE
            }
        }
    }

    fun withCancelable(cancelable: Boolean): CommonDialog = this.apply { setCancelable(cancelable) }

    fun withContent(contentRes: Int): CommonDialog = this.apply { content_textview.text = context.getString(contentRes) }

    fun withContent(content: Spannable): CommonDialog = this.apply { content_textview.text = content }

    fun withContent(content: String?): CommonDialog = this.apply { content_textview.text = content }

    fun withContentGravity(gravity: Int): CommonDialog = this.apply { content_textview.gravity = gravity }

    fun withCancelText(contentRes: Int): CommonDialog = this.apply { cancel_textview.text = context.getString(contentRes) }

    fun withCancelText(content: String?): CommonDialog = this.apply { cancel_textview.text = content }

    fun withConfirmText(contentRes: Int): CommonDialog = this.apply { confirm_textview.text = context.getString(contentRes) }

    fun withConfirmText(content: String?): CommonDialog = this.apply { confirm_textview.text = content }

    fun withDoneText(content: String?): CommonDialog = this.apply { done_textview.text = content }

    fun withDoneText(contentRes: Int): CommonDialog = this.apply { done_textview.text = context.getString(contentRes) }

    fun withData(data: Any?): CommonDialog = this.apply { mData = data }

    fun clicks(): Observable<Click> {
        show()
        return mClickSubject
    }

    fun confirms(): Observable<Click> = clicks().filter { it.type == ClickType.CONFIRM }
}