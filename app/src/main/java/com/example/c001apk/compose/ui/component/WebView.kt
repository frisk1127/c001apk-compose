package com.example.c001apk.compose.ui.component

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.example.c001apk.compose.constant.Constants.APP_ID
import com.example.c001apk.compose.constant.Constants.UTF8
import com.example.c001apk.compose.logic.providable.LocalUserPreferences
import com.example.c001apk.compose.ui.webview.ActionType
import com.example.c001apk.compose.util.copyText
import com.example.c001apk.compose.util.decode
import com.example.c001apk.compose.util.makeToast
import com.example.c001apk.compose.util.openInBrowser
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.net.URISyntaxException

/**
 * Created by bggRGjQaUbCoE on 2024/6/9
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebView(
    modifier: Modifier = Modifier,
    url: String,
    isLogin: Boolean,
    onBack: Boolean,
    onBackReset: () -> Unit,
    actionType: ActionType = ActionType.NONE,
    resetActionType: () -> Unit,
    onFinishLogin: (String) -> Unit,
    onFinish: () -> Unit,
    onUpdateProgress: (Float) -> Unit,
    onUpdateTitle: (String) -> Unit,
    onShowSnackbar: (String) -> Unit,
) {

    val prefs = LocalUserPreferences.current
    val isDarkMode = prefs.isDarkMode()

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                settings.apply {
                    javaScriptEnabled = true
                    blockNetworkImage = false
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    domStorageEnabled = true
                    setSupportZoom(true)
                    builtInZoomControls = true
                    displayZoomControls = false
                    cacheMode = WebSettings.LOAD_NO_CACHE
                    defaultTextEncodingName = UTF8
                    allowContentAccess = true
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    javaScriptCanOpenWindowsAutomatically = true
                    loadsImagesAutomatically = true
                    allowFileAccess = false
                    userAgentString = prefs.userAgent
                    if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                        WebSettingsCompat.setAlgorithmicDarkeningAllowed(this, isDarkMode)
                    }
                }

                CookieManager.getInstance().let {
                    it.setAcceptCookie(true)
                    it.setAcceptThirdPartyCookies(this@apply, true)
                    if (prefs.isLogin) {
                        it.removeAllCookies { }
                        it.setCookie(".coolapk.com", "DID=${prefs.szlmId}")
                        it.setCookie(".coolapk.com", "forward=https://www.coolapk.com")
                        it.setCookie(".coolapk.com", "displayVersion=v14")
                        it.setCookie(".coolapk.com", "uid=${prefs.uid}")
                        it.setCookie(".coolapk.com", "username=${prefs.username}")
                        it.setCookie(".coolapk.com", "token=${prefs.token}")
                    }
                }

                setDownloadListener { url, userAgent, contentDisposition, mimetype, _ ->
                    val fileName = URLUtil.guessFileName(url, contentDisposition, mimetype).decode
                    MaterialAlertDialogBuilder(context).apply {
                        setTitle("确定下载文件吗？")
                        setMessage(fileName)
                        setNeutralButton("外部打开") { _, _ ->
                            context.openInBrowser(url)
                        }
                        setNegativeButton(android.R.string.cancel, null)
                        setPositiveButton(android.R.string.ok) { _, _ ->
                            try {
                                val request = DownloadManager.Request(Uri.parse(url))
                                    .setMimeType(mimetype)
                                    .addRequestHeader(
                                        "cookie",
                                        CookieManager.getInstance().getCookie(url)
                                    )
                                    .addRequestHeader("User-Agent", userAgent)
                                    .setTitle(fileName)
                                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                    .setDestinationInExternalPublicDir(
                                        Environment.DIRECTORY_DOWNLOADS,
                                        fileName
                                    )
                                val downloadManager =
                                    context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                                downloadManager.enqueue(request)
                            } catch (e: Exception) {
                                context.makeToast("下载失败")
                                context.copyText(url)
                                e.printStackTrace()
                            }
                        }
                        show()
                    }
                }

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        webView: WebView?, request: WebResourceRequest?
                    ): Boolean {

                        request?.let {
                            val requestUrl = request.url.toString()

                            if (isLogin && requestUrl == "https://www.coolapk.com/") {
                                val cookie = CookieManager.getInstance().getCookie(requestUrl)
                                onFinishLogin(cookie)
                            }

                            try {
                                //处理intent协议
                                if (requestUrl.startsWith("intent://")) {
                                    val intent: Intent
                                    try {
                                        intent =
                                            Intent.parseUri(requestUrl, Intent.URI_INTENT_SCHEME)
                                        intent.addCategory("android.intent.category.BROWSABLE")
                                        intent.component = null
                                        intent.selector = null
                                        val resolves =
                                            context.packageManager.queryIntentActivities(
                                                intent,
                                                0
                                            )
                                        if (resolves.size > 0) {
                                            ContextCompat.startActivity(context, intent, null)
                                        }
                                        return true
                                    } catch (e: URISyntaxException) {
                                        e.printStackTrace()
                                    }
                                }
                                // 处理自定义scheme协议
                                if (!requestUrl.startsWith("http")) {
                                    onShowSnackbar(requestUrl)
                                    return true
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        return super.shouldOverrideUrlLoading(webView, request)
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onCloseWindow(window: WebView?) {
                        super.onCloseWindow(window)
                        onFinish()
                    }

                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        onUpdateProgress(newProgress / 100f)
                    }

                    override fun onReceivedTitle(view: WebView, title: String) {
                        super.onReceivedTitle(view, title)
                        onUpdateTitle(title)
                    }
                }

                loadUrl(url, mapOf("X-Requested-With" to APP_ID))
            }
        },
        update = { webView ->

            when (actionType) {
                ActionType.REFRESH -> {
                    resetActionType()
                    webView.reload()
                }

                ActionType.COPY -> {
                    resetActionType()
                    webView.context.copyText(webView.url)
                }

                ActionType.OPEN -> {
                    resetActionType()
                    webView.url?.let { webView.context.openInBrowser(it) }
                }

                ActionType.CLEAN -> {
                    resetActionType()
                    webView.clearHistory()
                    webView.clearCache(true)
                    webView.clearFormData()
                    webView.context.makeToast("清除缓存成功")
                }

                ActionType.NONE -> {}
            }


            if (onBack) {
                onBackReset()
                if (webView.canGoBack())
                    webView.goBack()
                else
                    onFinish()
            }
        }
    )

}
