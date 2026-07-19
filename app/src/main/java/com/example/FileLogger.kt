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
    private const val MAX_FILE_SIZE = 5 * 1024 * 1024L // 5 MB

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

    fun logWarning(tag: String, message: String, throwable: Throwable? = null, context: Context? = null) {
        Log.w(tag, message, throwable)
        writeToFile("WARNING", tag, message, throwable, context)
    }

    fun logError(tag: String, message: String, throwable: Throwable? = null, context: Context? = null) {
        Log.e(tag, message, throwable)
        writeToFile("ERROR", tag, message, throwable, context)
    }

    private fun trimFileIfNeeded(file: File) {
        if (file.exists() && file.length() > MAX_FILE_SIZE) {
            try {
                // Keep the last 2.5MB
                val keepSize = (2.5 * 1024 * 1024).toLong()
                val raf = java.io.RandomAccessFile(file, "rw")
                val length = raf.length()
                raf.seek(length - keepSize)
                val bytes = ByteArray(keepSize.toInt())
                raf.readFully(bytes)
                raf.close()
                
                val fos = FileOutputStream(file, false)
                fos.write(bytes)
                fos.close()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to trim log file", e)
            }
        }
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
            trimFileIfNeeded(file)
            FileOutputStream(file, true).use { fos ->
                fos.write(sb.toString().toByteArray())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write to Downloads folder", e)
            if (context != null) {
                try {
                    val fallbackDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                    val file = File(fallbackDir, LOG_FILE_NAME)
                    if (file != null) {
                        trimFileIfNeeded(file)
                        FileOutputStream(file, true).use { fos ->
                            fos.write(sb.toString().toByteArray())
                        }
                    }
                } catch (ex: Exception) {
                    Log.e(TAG, "Failed fallback write", ex)
                }
            }
        }
    }

    fun getLogFile(context: Context): File? {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        var file = File(downloadsDir, LOG_FILE_NAME)
        if (file.exists()) return file
        val fallbackDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        file = File(fallbackDir, LOG_FILE_NAME)
        return if (file.exists()) file else null
    }

    fun dumpLogcat(context: Context? = null) {
        try {
            val process = Runtime.getRuntime().exec("logcat -d")
            val logcatOutput = process.inputStream.bufferedReader().use { it.readText() }
            writeToFile("LOGCAT", "Dump", "\n=== LOGCAT START ===\n$logcatOutput\n=== LOGCAT END ===", null, context)
            Runtime.getRuntime().exec("logcat -c")
        } catch (e: Exception) {
            logError("LOGCAT", "Failed to dump logcat", e, context)
        }
    }
}

