package com.example.c001apk.compose.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Build.VERSION.SDK_INT
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.request.ImageRequest
import com.example.c001apk.compose.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream


/**
 * Created by bggRGjQaUbCoE on 2024/6/7
 */
object ImageDownloadUtil {

    suspend fun downloadImage(
        context: Context,
        imageUrl: String,
        fileName: String,
        isEnd: Boolean,
        isShare: Boolean = false,
        userAgent: String?,
    ) {
        if (isGifRequest(imageUrl, fileName)) {
            CoroutineScope(Dispatchers.IO).launch {
                val (bytes, mimeType) = downloadBytes(imageUrl, userAgent)
                val result = if (bytes != null) {
                    if (isShare || SDK_INT < 29) {
                        saveBytesBelowQ(context, bytes, fileName, isShare)
                    } else {
                        saveBytesAboveQ(context, bytes, fileName, mimeType)
                    }
                } else {
                    false
                }
                if (!isShare && isEnd) {
                    withContext(Dispatchers.Main) {
                        context.makeToast(
                            if (result) "Image saved successfully"
                            else "Failed to save image"
                        )
                    }
                }
                if (isShare && result) {
                    ImageShowUtil.shareImage(
                        context,
                        File(context.externalCacheDir, "imageShare/$fileName"),
                        null
                    )
                }
                if (!result) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Failed to download image", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }
            return
        }

        val imageLoader = ImageLoader.Builder(context).build()

        val request = ImageRequest.Builder(context)
            .data(imageUrl)
            .addHeader("User-Agent", userAgent ?: "")
            .target(
                onSuccess = { drawable ->
                    CoroutineScope(Dispatchers.IO).launch {
                        val result =
                            if (isShare || SDK_INT < 29) {
                                saveImageBelowQ(context, drawable, fileName, isShare)
                            } else {
                                saveImageAboveQ(context, drawable, fileName)
                            }
                        if (!isShare && isEnd) {
                            withContext(Dispatchers.Main) {
                                context.makeToast(
                                    if (result) "Image saved successfully"
                                    else "Failed to save image"
                                )
                            }
                        }
                        if (isShare && result) {
                            ImageShowUtil.shareImage(
                                context,
                                File(context.externalCacheDir, "imageShare/$fileName"),
                                null
                            )
                        }
                    }
                },
                onError = {
                    Toast.makeText(context, "Failed to download image", Toast.LENGTH_SHORT).show()
                }
            )
            .build()

        imageLoader.enqueue(request)
    }

    private fun saveImageAboveQ(
        context: Context,
        drawable: Drawable,
        fileName: String
    ): Boolean {
        return try {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.DESCRIPTION, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/${context.getString(R.string.app_name)}/"
                )
            }
            resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?.let { uri ->
                    resolver.openOutputStream(uri)?.use { outputStream ->
                        drawable.toBitmap().compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                    } ?: false
                } ?: false
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun saveBytesAboveQ(
        context: Context,
        bytes: ByteArray,
        fileName: String,
        mimeType: String?,
    ): Boolean {
        return try {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.DESCRIPTION, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, mimeType ?: "image/gif")
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/${context.getString(R.string.app_name)}/"
                )
            }
            resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?.let { uri ->
                    resolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(bytes)
                        outputStream.flush()
                    } ?: false
                } ?: false
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private suspend fun saveImageBelowQ(
        context: Context,
        drawable: Drawable,
        fileName: String,
        isShare: Boolean
    ): Boolean {
        return withContext(Dispatchers.IO) {
            val imagesDir =
                // Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                Environment.getExternalStorageDirectory().toString() +
                        "/${context.getString(R.string.app_name)}/"
            val imageFile =
                if (isShare) File(context.externalCacheDir, "/imageShare/$fileName") else File(
                    imagesDir,
                    fileName
                )
            if (imageFile.parentFile?.exists() == false)
                imageFile.parentFile?.mkdirs()

            try {
                FileOutputStream(imageFile).use { outputStream ->
                    drawable.toBitmap().compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    private suspend fun saveBytesBelowQ(
        context: Context,
        bytes: ByteArray,
        fileName: String,
        isShare: Boolean,
    ): Boolean {
        return withContext(Dispatchers.IO) {
            val imagesDir =
                Environment.getExternalStorageDirectory().toString() +
                        "/${context.getString(R.string.app_name)}/"
            val imageFile =
                if (isShare) File(context.externalCacheDir, "/imageShare/$fileName") else File(
                    imagesDir,
                    fileName
                )
            if (imageFile.parentFile?.exists() == false)
                imageFile.parentFile?.mkdirs()

            try {
                FileOutputStream(imageFile).use { outputStream ->
                    outputStream.write(bytes)
                    outputStream.flush()
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    private fun isGifRequest(imageUrl: String, fileName: String): Boolean {
        val cleanUrl = imageUrl.substringBefore('?').substringBefore('#')
        val cleanName = fileName.substringBefore('?').substringBefore('#')
        return cleanUrl.endsWith(".gif", ignoreCase = true) ||
                cleanName.endsWith(".gif", ignoreCase = true)
    }

    private suspend fun downloadBytes(
        imageUrl: String,
        userAgent: String?,
    ): Pair<ByteArray?, String?> {
        return withContext(Dispatchers.IO) {
            try {
                val client = OkHttpClient()
                val request = Request.Builder()
                    .url(imageUrl)
                    .header("User-Agent", userAgent ?: "")
                    .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext null to null
                    }
                    val body = response.body ?: return@withContext null to null
                    val bytes = body.bytes()
                    val contentType = response.header("Content-Type")
                    bytes to contentType
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null to null
            }
        }
    }

}
