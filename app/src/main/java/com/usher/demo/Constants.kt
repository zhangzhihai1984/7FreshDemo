package com.usher.demo

import android.Manifest
import android.os.Environment
import java.io.File

object Constants {
    val PERMISSION_INIT_APP = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

    private val APP_FILE_PATH = Environment.getExternalStorageDirectory().toString() + File.separator + "7fresh" + File.separator
    val APP_LOG_PATH = APP_FILE_PATH + "logs" + File.separator
}