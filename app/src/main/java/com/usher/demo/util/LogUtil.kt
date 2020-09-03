package com.usher.demo.util

import android.util.Log
import com.usher.demo.Constants
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*


object LogUtil {
    private const val TAG = "7fresh"

    private val logDirPath: String?
        get() = File(Constants.APP_LOG_PATH).let { file ->
            if (file.exists() || file.mkdirs()) Constants.APP_LOG_PATH else null
        }

    fun log(log: String) {
        Log.i(TAG, log)

        if (android.os.Environment.MEDIA_MOUNTED != android.os.Environment.getExternalStorageState())
            return

        val date = Date()
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val timeDateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

        val fileName = "7fresh_log_${dateFormat.format(date)}.txt"
        val data = "${timeDateFormat.format(date)}:  $log\r\n"

        try {
            val fout = FileOutputStream(logDirPath + fileName, true)
            fout.write(data.toByteArray())
            fout.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}