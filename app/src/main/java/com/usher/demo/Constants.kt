package com.usher.demo

import android.Manifest
import android.os.Environment
import java.io.File

object Constants {
    /**
     * Tab Tag
     */
    const val TAB_TAG_PRODUCT = "TAB_TAG_PRODUCT"
    const val TAB_TAG_COMMENT = "TAB_TAG_COMMENT"
    const val TAB_TAG_DETAIL = "TAB_TAG_DETAIL"
    const val TAB_TAG_RECOMMEND = "TAB_TAG_RECOMMEND"

    /**
     * Broadcast Action
     */


    /**
     * Intent Extra Name
     */
    const val TAG_URL = "TAG_URL"
    const val TAG_DATA = "TAG_DATA"

    /**
     * Permission
     */
    val PERMISSION_INIT_APP = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    const val PERMISSION_USE_CAMERA = Manifest.permission.CAMERA

    private val APP_FILE_PATH = Environment.getExternalStorageDirectory().toString() + File.separator + "7fresh" + File.separator
    val APP_LOG_PATH = APP_FILE_PATH + "logs" + File.separator
}