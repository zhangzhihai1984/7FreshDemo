package com.usher.demo.util

import android.content.Context

object SystemUtil {

    fun getStatusBarHeight(context: Context): Int =
            context.resources.getIdentifier("status_bar_height", "dimen", "android").let { resourceId ->
                if (resourceId > 0) context.resources.getDimensionPixelSize(resourceId) else 72
            }
}