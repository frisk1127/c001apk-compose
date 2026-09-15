package com.example.c001apk.compose.dev

import android.content.Context
import android.util.Log
import com.example.c001apk.compose.BuildConfig
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

/**
 * 开发分支专用的日志工具：Logcat + 文件双写，带体积上限与轮转。
 *
 * 只在非 main / master 分支的构建里生效（见 [BuildConfig.DEV_LOG]）。main 分支上
 * [enabled] 是编译期常量 false，R8 会把整段调用连同 message lambda 一起剪掉，
 * 字符串拼接都不会执行，等于零开销。
 *
 * 落盘位置：/sdcard/Android/data/<包名>/files/logs/dev.log
 * 取日志（本机 adb + root）：
 *   adb shell su -c "cat /sdcard/Android/data/<包名>/files/logs/dev.log"
 */
object DevLog {

    /** 编译期总开关，由构建分支决定，运行期改不了 */
    val enabled: Boolean = BuildConfig.DEV_LOG

    /** 运行期开关，方便临时静音 */
    @Volatile
    var runtimeEnabled: Boolean = true

    /** 单个日志文件的体积上限，超过就轮转 */
    @Volatile
    var maxFileBytes: Long = 512L * 1024

    /** 轮转保留的历史文件数量（dev.log.1、dev.log.2 …） */
    @Volatile
    var maxBackupCount: Int = 3

    /** 是否同时输出到 Logcat */
    @Volatile
    var logcatEnabled: Boolean = true

    private const val DIR_NAME = "logs"
    private const val FILE_NAME = "dev.log"
    private const val FALLBACK_TAG = "DevLog"

    private val timeFormat = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US)
    private val lock = Any()
    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "dev-log").apply { isDaemon = true }
    }

    private var appContext: Context? = null
    private var writer: BufferedWriter? = null

    /** 在 Application.onCreate 里调用一次 */
    fun init(context: Context) {
        if (!enabled) return
        appContext = context.applicationContext
    }

    /** 日志所在目录 */
    val directory: File?
        get() {
            val ctx = appContext ?: return null
            return File(ctx.getExternalFilesDir(null) ?: ctx.filesDir, DIR_NAME)
        }

    /** 当前正在写入的文件 */
    val currentFile: File?
        get() = directory?.let { File(it, FILE_NAME) }

    inline fun v(tag: String, message: () -> String) {
        if (!enabled) return
        submit(Log.VERBOSE, tag, message(), null)
    }

    inline fun d(tag: String, message: () -> String) {
        if (!enabled) return
        submit(Log.DEBUG, tag, message(), null)
    }

    inline fun i(tag: String, message: () -> String) {
        if (!enabled) return
        submit(Log.INFO, tag, message(), null)
    }

    inline fun w(tag: String, message: () -> String) {
        if (!enabled) return
        submit(Log.WARN, tag, message(), null)
    }

    inline fun e(tag: String, throwable: Throwable? = null, message: () -> String) {
        if (!enabled) return
        submit(Log.ERROR, tag, message(), throwable)
    }

    /** 清空所有日志文件并重新开始 */
    fun clear() {
        if (!enabled) return
        executor.execute {
            synchronized(lock) {
                closeWriter()
                directory?.listFiles()?.forEach { it.delete() }
            }
        }
    }

    @PublishedApi
    internal fun submit(priority: Int, tag: String, message: String, throwable: Throwable?) {
        if (!enabled || !runtimeEnabled) return
        if (logcatEnabled) {
            when (priority) {
                Log.VERBOSE -> Log.v(tag, message, throwable)
                Log.DEBUG -> Log.d(tag, message, throwable)
                Log.INFO -> Log.i(tag, message, throwable)
                Log.WARN -> Log.w(tag, message, throwable)
                else -> Log.e(tag, message, throwable)
            }
        }
        if (appContext == null) return
        executor.execute {
            synchronized(lock) {
                try {
                    val w = writer ?: openWriter()
                    w.append(timeFormat.format(Date()))
                    w.append(' ')
                    w.append(if (tag.isEmpty()) message else "[$tag] $message")
                    if (throwable != null) {
                        w.append('\n')
                        w.append(Log.getStackTraceString(throwable))
                    }
                    w.append('\n')
                    w.flush()
                    val file = currentFile
                    if (file != null && file.length() >= maxFileBytes) rotate()
                } catch (t: Throwable) {
                    Log.w(FALLBACK_TAG, "write to file failed", t)
                }
            }
        }
    }

    private fun openWriter(): BufferedWriter {
        val dir = directory ?: error("DevLog.init() was not called")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, FILE_NAME)
        val w = BufferedWriter(OutputStreamWriter(FileOutputStream(file, true), Charsets.UTF_8))
        writer = w
        w.append("==== session start ====\n")
        w.flush()
        return w
    }

    private fun closeWriter() {
        try {
            writer?.flush()
            writer?.close()
        } catch (_: Throwable) {
            // 关不上就算了，下次写会重新打开
        }
        writer = null
    }

    private fun rotate() {
        closeWriter()
        val base = currentFile ?: return
        val parent = base.parentFile ?: return
        for (i in maxBackupCount - 1 downTo 1) {
            val from = File(parent, "$FILE_NAME.$i")
            if (from.exists()) from.renameTo(File(parent, "$FILE_NAME.${i + 1}"))
        }
        if (base.exists()) base.renameTo(File(parent, "$FILE_NAME.1"))
        var i = maxBackupCount + 1
        while (true) {
            val extra = File(parent, "$FILE_NAME.$i")
            if (!extra.exists()) break
            extra.delete()
            i++
        }
    }
}
