package com.nkds.hosikoouma.jasmine.core

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.nkds.hosikoouma.jasmine.CrashActivity
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.system.exitProcess

class CrashHandler(private val context: Context) : Thread.UncaughtExceptionHandler {

    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

    companion object {
        fun initialize(context: Context) {
            val handler = CrashHandler(context)
            Thread.setDefaultUncaughtExceptionHandler(handler)
        }
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            val report = generateErrorReport(throwable)
            val file = saveReportToFile(report)
            
            val intent = Intent(context, CrashActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                putExtra("crash_report_path", file.absolutePath)
                putExtra("crash_message", throwable.message ?: "Unknown error")
            }
            context.startActivity(intent)
            
            // Завершаем процесс
            exitProcess(10)
        } catch (e: Exception) {
            Log.e("CrashHandler", "Error while handling crash", e)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun generateErrorReport(throwable: Throwable): String {
        val writer = StringWriter()
        throwable.printStackTrace(PrintWriter(writer))
        val stackTrace = writer.toString()

        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val date = sdf.format(Date())

        return buildString {
            append("Jasmine Crash Report\n")
            append("Date: $date\n")
            append("Device: ${Build.MANUFACTURER} ${Build.MODEL}\n")
            append("Android Version: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})\n")
            append("App Version: 3.0-beta1\n")
            append("\n--- Stack Trace ---\n")
            append(stackTrace)
        }
    }

    private fun saveReportToFile(report: String): File {
        val dir = File(context.cacheDir, "crashes")
        if (!dir.exists()) dir.mkdirs()
        
        val file = File(dir, "crash_log.txt")
        file.writeText(report)
        return file
    }
}
