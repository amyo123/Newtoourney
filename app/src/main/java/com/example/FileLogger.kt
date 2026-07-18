package com.example

import android.content.Context
import android.os.Build
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.system.exitProcess

object FileLogger {
    private const val TAG = "FileLogger"
    private var isInitialized = false
    private const val LOG_FILE_NAME = "tournaments_app_logs.txt"

    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            logError("FATAL_CRASH", "Uncaught exception on thread ${thread.name}", throwable, context)
            defaultHandler?.uncaughtException(thread, throwable) ?: exitProcess(1)
        }
        
        logInfo("APP_LIFECYCLE", "Logger initialized", context)
    }

    fun logInfo(tag: String, message: String, context: Context? = null) {
        Log.i(tag, message)
        writeToFile("INFO", tag, message, null, context)
    }

    fun logError(tag: String, message: String, throwable: Throwable? = null, context: Context? = null) {
        Log.e(tag, message, throwable)
        writeToFile("ERROR", tag, message, throwable, context)
    }

    private fun writeToFile(level: String, tag: String, message: String, throwable: Throwable?, context: Context?) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        
        val sb = StringBuilder()
        sb.append("[$timestamp] [$level] [$tag]: $message\n")
        
        if (throwable != null) {
            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            sb.append(sw.toString()).append("\n")
        }

        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }
            val file = File(downloadsDir, LOG_FILE_NAME)
            FileOutputStream(file, true).use { fos ->
                fos.write(sb.toString().toByteArray())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write to Downloads folder", e)
            if (context != null) {
                try {
                    val fallbackDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                    val file = File(fallbackDir, LOG_FILE_NAME)
                    FileOutputStream(file, true).use { fos ->
                        fos.write(sb.toString().toByteArray())
                    }
                } catch (ex: Exception) {
                    Log.e(TAG, "Failed fallback write", ex)
                }
            }
        }
    }
}

