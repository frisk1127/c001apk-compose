package com.example.c001apk.compose.util

import android.content.Context
import android.net.Uri
import net.mikaelzero.mojito.loader.ImageLoader
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

class MojitoOkHttpImageLoader(context: Context) : ImageLoader {

    private val client = OkHttpClient.Builder()
        .addInterceptor(AddCookiesInterceptor)
        .build()

    private val callMap = ConcurrentHashMap<Int, Call>()
    private val cacheDir = File(context.cacheDir, "mojito").apply { mkdirs() }

    override fun loadImage(
        requestId: Int,
        uri: Uri,
        onlyRetrieveFromCache: Boolean,
        callback: ImageLoader.Callback
    ) {
        val scheme = uri.scheme?.lowercase()
        val isNetwork = scheme == "http" || scheme == "https"
        val cacheFile = resolveCacheFile(uri)
        callback.onStart()

        if (!isNetwork) {
            val localFile = if (scheme == "file") File(uri.path ?: "") else cacheFile
            if (localFile.exists()) {
                callback.onSuccess(localFile)
            } else {
                callback.onFail(IOException("file not found"))
            }
            callback.onFinish()
            return
        }

        if (onlyRetrieveFromCache) {
            if (cacheFile.exists()) {
                callback.onSuccess(cacheFile)
            } else {
                callback.onFail(IOException("cache miss"))
            }
            callback.onFinish()
            return
        }

        if (cacheFile.exists()) {
            if (cacheFile.length() > 0) {
                callback.onSuccess(cacheFile)
                callback.onFinish()
                return
            } else {
                cacheFile.delete()
            }
        }

        val request = Request.Builder().url(uri.toString()).build()
        val call = client.newCall(request)
        callMap[requestId] = call
        thread(name = "mojito-loader-$requestId") {
            try {
                call.execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException("http ${response.code}")
                    }
                    val body = response.body ?: throw IOException("empty body")
                    val total = body.contentLength().coerceAtLeast(0L)
                    val tempFile = File(cacheDir, "${cacheFile.name}.tmp")
                    if (tempFile.exists()) {
                        tempFile.delete()
                    }
                    body.byteStream().use { input ->
                        FileOutputStream(tempFile).use { output ->
                            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                            var read: Int
                            var current = 0L
                            while (input.read(buffer).also { read = it } != -1) {
                                output.write(buffer, 0, read)
                                if (total > 0L) {
                                    current += read
                                    val progress = ((current * 100) / total).toInt()
                                    callback.onProgress(progress)
                                }
                            }
                            output.flush()
                        }
                    }
                    if (!tempFile.renameTo(cacheFile)) {
                        throw IOException("cache rename failed")
                    }
                    callback.onSuccess(cacheFile)
                }
            } catch (e: Exception) {
                if (e !is IOException && e.cause is IOException) {
                    callback.onFail(e.cause as IOException)
                } else {
                    callback.onFail(e)
                }
            } finally {
                callback.onFinish()
                callMap.remove(requestId)
            }
        }
    }

    override fun prefetch(uri: Uri) {
        loadImage(uri.hashCode(), uri, false, object : ImageLoader.Callback {
            override fun onStart() {}
            override fun onProgress(progress: Int) {}
            override fun onFinish() {}
            override fun onSuccess(image: File) {}
            override fun onFail(error: Exception) {}
        })
    }

    override fun cancel(requestId: Int) {
        callMap.remove(requestId)?.cancel()
    }

    override fun cancelAll() {
        callMap.values.forEach { it.cancel() }
        callMap.clear()
    }

    override fun cleanCache() {
        cacheDir.listFiles()?.forEach { it.delete() }
    }

    private fun resolveCacheFile(uri: Uri): File {
        val url = uri.toString()
        val extension = uri.lastPathSegment
            ?.substringBefore('?')
            ?.substringBefore('#')
            ?.substringAfterLast('.', "")
            ?.ifBlank { "img" }
            ?: "img"
        val name = md5(url)
        return File(cacheDir, "$name.$extension")
    }

    private fun md5(value: String): String {
        val digest = MessageDigest.getInstance("MD5")
        val bytes = digest.digest(value.toByteArray())
        val builder = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            builder.append(String.format("%02x", b))
        }
        return builder.toString()
    }
}
