package com.usher.demo.order.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import androidx.lifecycle.LifecycleOwner
import com.jakewharton.rxbinding4.view.globalLayouts
import com.usher.demo.util.RxUtil

class AlternateDashView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0, defStyleRes: Int = 0) : View(context, attrs, defStyleAttr, defStyleRes) {
    companion object {
        private const val DEFAULT_WIDTH = 30f
        private const val DEFAULT_GAP = 8f
        private const val DEFAULT_DELTA = 6f
    }

    private val mEventPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#f58282")
    }

    private val mOddPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#96beff")
    }

    private val mPathWithPaints = arrayListOf<Pair<Path, Paint>>()

    init {
        globalLayouts()
                .take(1)
                .to(RxUtil.autoDispose(context as LifecycleOwner))
                .subscribe {
                    val count = (width / (DEFAULT_WIDTH + DEFAULT_DELTA)).toInt()

                    (0..count).forEach { i ->
                        val x1 = (DEFAULT_WIDTH + DEFAULT_DELTA + DEFAULT_GAP) * i
                        val x2 = x1 + DEFAULT_DELTA
                        val x3 = x2 + DEFAULT_WIDTH
                        val x4 = x1 + DEFAULT_WIDTH

                        val path = Path()
                        path.moveTo(x1, height.toFloat())
                        path.lineTo(x2, 0f)
                        path.lineTo(x3, 0f)
                        path.lineTo(x4, height.toFloat())
                        path.close()

                        val paint = if (i % 2 == 0) mEventPaint else mOddPaint

                        mPathWithPaints.add(path to paint)
                    }
                }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        mPathWithPaints.forEach { pathWithPaint -> canvas.drawPath(pathWithPaint.first, pathWithPaint.second) }
    }
}