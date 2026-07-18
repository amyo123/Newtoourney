package com.example

import android.content.Context
import android.os.Build
import android.os.Environment
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.system.exitProcess

object CrashLogger {
    private const val TAG = "CrashLogger"
    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            handleCrash(context, thread, throwable)

            // Let the system/default handler finish the process
            defaultHandler?.uncaughtException(thread, throwable) ?: exitProcess(1)
        }
    }

    private fun handleCrash(context: Context, thread: Thread, throwable: Throwable) {
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        val stackTraceString = sw.toString()

        val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
        val systemInfo = """
            OS Version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})
            Device: ${Build.MANUFACTURER} ${Build.MODEL}
            Brand: ${Build.BRAND}
            Thread: ${thread.name} (ID: ${thread.id})
        """.trimIndent()

        val crashDetails = mapOf(
            "timestamp" to timestamp,
            "systemInfo" to systemInfo,
            "message" to (throwable.message ?: "No message"),
            "stackTrace" to stackTraceString
        )

        // 1. Store to Firebase Firestore
        try {
            val db = FirebaseFirestore.getInstance()
            db.collection("crash_logs")
                .document("crash_$timestamp")
                .set(crashDetails)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send crash to Firebase", e)
        }

        // 2. Store to Downloads folder
        val fileName = "tournaments_crash_$timestamp.txt"
        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }
            val file = File(downloadsDir, fileName)
            FileOutputStream(file).use { fos ->
                val fullLog = """
                    === TOURNAMENTS APP CRASH LOG ===
                    Timestamp: $timestamp
                    
                    $systemInfo
                    
                    Exception: ${throwable.javaClass.name}
                    Message: ${throwable.message}
                    
                    Stack Trace:
                    $stackTraceString
                """.trimIndent()
                fos.write(fullLog.toByteArray())
            }
            Log.d(TAG, "Crash log successfully saved to downloads folder: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save crash log to Downloads folder, trying fallback to App files directory", e)
            
            // Fallback: Store inside App specific external files directory
            try {
                val fallbackDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                val file = File(fallbackDir, fileName)
                FileOutputStream(file).use { fos ->
                    val fullLog = """
                        === TOURNAMENTS APP CRASH LOG (FALLBACK) ===
                        Timestamp: $timestamp
                        
                        $systemInfo
                        
                        Exception: ${throwable.javaClass.name}
                        Message: ${throwable.message}
                        
                        Stack Trace:
                        $stackTraceString
                    """.trimIndent()
                    fos.write(fullLog.toByteArray())
                }
                Log.d(TAG, "Crash log successfully saved to app downloads folder fallback: ${file.absolutePath}")
            } catch (ex: Exception) {
                Log.e(TAG, "Failed fallback write", ex)
            }
        }
    }
}
