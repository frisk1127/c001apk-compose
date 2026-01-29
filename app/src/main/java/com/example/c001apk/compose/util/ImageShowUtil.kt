package com.example.c001apk.compose.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.os.Environment
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewTreeLifecycleOwner
import androidx.lifecycle.ViewTreeViewModelStoreOwner
import androidx.savedstate.ViewTreeSavedStateRegistryOwner
import com.example.c001apk.compose.R
import com.example.c001apk.compose.constant.Constants.EMPTY_STRING
import com.example.c001apk.compose.constant.Constants.SUFFIX_THUMBNAIL
import com.example.c001apk.compose.view.CircleIndexIndicator
import com.example.c001apk.compose.view.NineGridImageView
import com.example.c001apk.compose.ui.theme.C001apkComposeTheme
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.mikaelzero.mojito.Mojito
import net.mikaelzero.mojito.ext.mojito
import net.mikaelzero.mojito.impl.DefaultPercentProgress
import net.mikaelzero.mojito.impl.DefaultTargetFragmentCover
import net.mikaelzero.mojito.impl.SimpleMojitoViewCallback
import java.io.File

object ImageShowUtil {

    fun startBigImgView(
        nineGridView: NineGridImageView,
        imageView: ImageView,
        urlList: List<String>,
        position: Int,
        cookie: String? = null,
        userAgent: String? = null,
    ) {
        val thumbnailList = urlList.map { it.http2https }
        val originList = urlList.map {
            if (it.contains(SUFFIX_THUMBNAIL)) it.replace(SUFFIX_THUMBNAIL, EMPTY_STRING).http2https
            else it.http2https
        }
        Mojito.start(imageView.context) {
            urls(thumbnailList, originList)
            position(position)
            progressLoader {
                DefaultPercentProgress()
            }
            if (urlList.size != 1)
                setIndicator(CircleIndexIndicator())
            views(nineGridView.getImageViews().toTypedArray())
            when (CookieUtil.imageQuality) {
                0 -> if (NetWorkUtil.isWifiConnected())
                    autoLoadTarget(true)
                else
                    autoLoadTarget(false)

                1 -> autoLoadTarget(true)

                2 -> autoLoadTarget(false)
            }
            fragmentCoverLoader {
                DefaultTargetFragmentCover()
            }
            setOnMojitoListener(
                object : SimpleMojitoViewCallback() {
                    override fun onStartAnim(position: Int) {
                        nineGridView.getImageViewAt(position)?.apply {
                            postDelayed({
                                this.isVisible = false
                            }, 200)
                        }
                    }

                    override fun onMojitoViewFinish(pagePosition: Int) {
                        nineGridView.getImageViews().forEach {
                            it.isVisible = true
                        }
                    }

                    override fun onViewPageSelected(position: Int) {
                        nineGridView.getImageViews().forEachIndexed { index, imageView ->
                            imageView.isVisible = position != index
                        }
                    }

                    override fun onLongClick(
                        fragmentActivity: FragmentActivity?,
                        view: View,
                        x: Float,
                        y: Float,
                        position: Int
                    ) {
                        Log.d(
                            "MojitoLongPress",
                            "ImageShowUtil onLongClick pos=$position x=$x y=$y activity=${fragmentActivity != null}"
                        )
                        if (fragmentActivity != null) {
                            showSaveImgDialog(
                                fragmentActivity,
                                originList[position],
                                originList,
                                userAgent,
                            )
                        } else {
                            Log.i("Mojito", "fragmentActivity is null, skip save image")
                        }
                    }
                },
            )
        }

    }

    fun startBigImgViewSimple(
        context: Context,
        urlList: List<String>,
        cookie: String? = null,
        userAgent: String? = null,
    ) {
        val thumbnailList = urlList.map { "${it.http2https}$SUFFIX_THUMBNAIL" }
        val originList = urlList.map { it.http2https }
        Mojito.start(context) {
            urls(thumbnailList, originList)
            when (CookieUtil.imageQuality) {
                0 -> if (NetWorkUtil.isWifiConnected())
                    autoLoadTarget(true)
                else
                    autoLoadTarget(false)

                1 -> autoLoadTarget(true)

                2 -> autoLoadTarget(false)
            }
            fragmentCoverLoader {
                DefaultTargetFragmentCover()
            }
            progressLoader {
                DefaultPercentProgress()
            }
            if (urlList.size > 1)
                setIndicator(CircleIndexIndicator())
            setOnMojitoListener(object : SimpleMojitoViewCallback() {
                override fun onLongClick(
                    fragmentActivity: FragmentActivity?,
                    view: View,
                    x: Float,
                    y: Float,
                    position: Int
                ) {
                    Log.d(
                        "MojitoLongPress",
                        "ImageShowUtilSimple onLongClick pos=$position x=$x y=$y activity=${fragmentActivity != null}"
                    )
                    if (fragmentActivity != null) {
                        showSaveImgDialog(
                            fragmentActivity,
                            originList[position],
                            originList,
                            userAgent
                        )
                    } else {
                        Log.i("Mojito", "fragmentActivity is null, skip save image")
                    }
                }
            })
        }
    }

    fun startBigImgViewSimple(
        imageView: ImageView,
        url: String,
        cookie: String? = null,
        userAgent: String? = null,
    ) {
        imageView.mojito(
            url = url,
            builder = {
                progressLoader {
                    DefaultPercentProgress()
                }
                setOnMojitoListener(object : SimpleMojitoViewCallback() {
                    override fun onLongClick(
                        fragmentActivity: FragmentActivity?,
                        view: View,
                        x: Float,
                        y: Float,
                        position: Int
                    ) {
                        Log.d(
                            "MojitoLongPress",
                            "ImageShowUtilSingle onLongClick pos=$position x=$x y=$y activity=${fragmentActivity != null}"
                        )
                        if (fragmentActivity != null) {
                            showSaveImgDialog(fragmentActivity, url, null, userAgent)
                        } else {
                            Log.i("Mojito", "fragmentActivity is null, skip save image")
                        }
                    }
                })
            },
        )
    }

    private fun showSaveImgDialog(
        context: Context,
        url: String,
        urlList: List<String>?,
        userAgent: String?,
    ) {
        if (context is FragmentActivity) {
            showSaveImgDialogCompose(context, url, urlList, userAgent)
            return
        }
        val items = arrayOf("保存图片", "保存全部图片", "图片分享", "复制图片地址")
        MaterialAlertDialogBuilder(context).apply {
            setItems(items) { _, position: Int ->
                handleSaveDialogAction(context, url, urlList, userAgent, position)
            }
            show()
        }
    }

    private fun showSaveImgDialogCompose(
        activity: FragmentActivity,
        url: String,
        urlList: List<String>?,
        userAgent: String?,
    ) {
        val dialog = Dialog(activity)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        val items = listOf("保存图片", "保存全部图片", "图片分享", "复制图片地址")
        val composeView = ComposeView(activity).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            ViewTreeLifecycleOwner.set(this, activity)
            ViewTreeViewModelStoreOwner.set(this, activity)
            ViewTreeSavedStateRegistryOwner.set(this, activity)
            setContent {
                C001apkComposeTheme(
                    darkTheme = CookieUtil.isDarkMode,
                    themeType = CookieUtil.themeType,
                    seedColor = CookieUtil.seedColor,
                    materialYou = CookieUtil.materialYou,
                    pureBlack = CookieUtil.pureBlack,
                    paletteStyle = CookieUtil.paletteStyle,
                    fontScale = CookieUtil.fontScale,
                    contentScale = CookieUtil.contentScale,
                ) {
                    SaveImageDialogContent(
                        items = items,
                        onClick = { index ->
                            handleSaveDialogAction(activity, url, urlList, userAgent, index)
                            dialog.dismiss()
                        }
                    )
                }
            }
        }
        dialog.setContentView(composeView)
        dialog.show()
    }

    @Composable
    private fun SaveImageDialogContent(
        items: List<String>,
        onClick: (Int) -> Unit,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(PaddingValues(vertical = 8.dp)),
            ) {
                items.forEachIndexed { index, item ->
                    Text(
                        text = item,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onClick(index) }
                            .padding(horizontal = 24.dp, vertical = 14.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }

    private fun handleSaveDialogAction(
        context: Context,
        url: String,
        urlList: List<String>?,
        userAgent: String?,
        position: Int,
    ) {
        when (position) {
            0 -> CoroutineScope(Dispatchers.IO).launch {
                checkImageExist(context, url, true, userAgent)
            }

            1 -> CoroutineScope(Dispatchers.IO).launch {
                if (urlList.isNullOrEmpty()) {
                    checkImageExist(context, url, true, userAgent)
                } else {
                    urlList.forEachIndexed { index, url ->
                        checkImageExist(context, url, index == urlList.lastIndex, userAgent)
                    }
                }
            }

            2 -> CoroutineScope(Dispatchers.IO).launch {
                val index = url.lastIndexOf('/')
                val filename = url.substring(index + 1)
                if (checkShareImageExist(context, filename)) {
                    shareImage(
                        context,
                        File(context.externalCacheDir, "imageShare/$filename"),
                        null
                    )
                } else {
                    ImageDownloadUtil.downloadImage(
                        context, url, filename,
                        isEnd = true,
                        isShare = true,
                        userAgent = userAgent,
                    )
                }
            }

            3 -> context.copyText(url)
        }
    }

    private fun checkShareImageExist(context: Context, filename: String): Boolean {
        val imageCheckDir = File(context.externalCacheDir, "imageShare/$filename")
        return imageCheckDir.exists()
    }

    private suspend fun checkImageExist(
        context: Context,
        url: String,
        isEnd: Boolean,
        userAgent: String?,
    ) {
        val filename = url.substring(url.lastIndexOf('/') + 1)
        val path = "${context.getString(R.string.app_name)}/$filename"
        val imageFile = if (SDK_INT >= 29) File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), path
        )
        else File(Environment.getExternalStorageDirectory().toString(), path)
        if (imageFile.exists()) {
            if (isEnd)
                withContext(Dispatchers.Main) {
                    context.makeToast("文件已存在")
                }
        } else {
            ImageDownloadUtil.downloadImage(context, url, filename, isEnd, userAgent = userAgent)
        }
    }

    private fun getFileProvider(context: Context, file: File): Uri {
        val authority = context.packageName + ".fileprovider"
        return FileProvider.getUriForFile(context, authority, file)
    }

    fun shareImage(context: Context, file: File, title: String?) {
        try {
            val contentUri = getFileProvider(context, file)
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "image/*"
            intent.putExtra(Intent.EXTRA_STREAM, contentUri)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            ContextCompat.startActivity(context, Intent.createChooser(intent, title), null)
        } catch (e: ActivityNotFoundException) {
            context.makeToast("failed to share image")
            e.printStackTrace()
        }
    }

    fun getImageLp(url: String): Pair<Int, Int> {
        var imgWidth = 1
        var imgHeight = 1
        val at = url.lastIndexOf("@")
        val x = url.lastIndexOf("x")
        val dot = url.lastIndexOf(".")
        if (at != -1 && x != -1 && dot != -1) {
            imgWidth = url.substring(at + 1, x).toInt()
            imgHeight = url.substring(x + 1, dot).toInt()
        }
        return Pair(imgWidth, imgHeight)
    }

}
