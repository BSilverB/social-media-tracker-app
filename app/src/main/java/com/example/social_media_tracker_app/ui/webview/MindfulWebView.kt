package com.example.social_media_tracker_app.ui.webview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.social_media_tracker_app.bridge.WebAppInterface
import com.example.social_media_tracker_app.data.repository.StatsRepository
import java.io.BufferedReader
import java.io.InputStreamReader

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MindfulWebView(
    repository: StatsRepository,
    targetUrl: String = "https://m.youtube.com",
    modifier: Modifier = Modifier,
    onStudyCheckInNeeded: ((String, String) -> Unit)? = null,
    onCustomViewShown: ((View, () -> Unit) -> Unit)? = null,
    onCustomViewHidden: (() -> Unit)? = null,
    onWebViewCreated: (WebView) -> Unit = {}
) {
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                // Configure WebSettings for optimal mobile browsing & video playback
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    mediaPlaybackRequiresUserGesture = false
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    cacheMode = WebSettings.LOAD_DEFAULT
                    allowContentAccess = true
                    allowFileAccess = false
                    javaScriptCanOpenWindowsAutomatically = true

                    // Mobile user agent
                    userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
                }

                // Register native JavaScript bridge
                val bridge = WebAppInterface(context, repository, onStudyCheckInNeeded)
                addJavascriptInterface(bridge, WebAppInterface.INTERFACE_NAME)

                // Inject Tracker Script helper
                val injectScript = { webView: WebView ->
                    val script = readAssetScript(context, "inject_tracker.js")
                    if (script.isNotBlank()) {
                        webView.evaluateJavascript(script) { result ->
                            Log.d("MindfulWebView", "Injected tracker result: $result")
                        }
                    }
                }

                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        view?.let { injectScript(it) }
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        view?.let { injectScript(it) }
                    }

                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        val uri = request?.url ?: return false
                        val scheme = uri.scheme?.lowercase() ?: ""
                        if (scheme != "http" && scheme != "https") {
                            return true // Don't crash WebView on app intents
                        }
                        return false // Load in WebView
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    private var currentCustomView: View? = null
                    private var currentCallback: CustomViewCallback? = null

                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        super.onProgressChanged(view, newProgress)
                        if (newProgress >= 70 && view != null) {
                            injectScript(view)
                        }
                    }

                    override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                        super.onShowCustomView(view, callback)
                        if (view != null) {
                            currentCustomView = view
                            currentCallback = callback
                            onCustomViewShown?.invoke(view) {
                                onHideCustomView()
                            }
                        }
                    }

                    override fun onHideCustomView() {
                        super.onHideCustomView()
                        currentCallback?.onCustomViewHidden()
                        currentCallback = null
                        currentCustomView = null
                        onCustomViewHidden?.invoke()
                    }
                }

                loadUrl(targetUrl)
                onWebViewCreated(this)
            }
        }
    )
}

private fun readAssetScript(context: Context, filename: String): String {
    return try {
        context.assets.open(filename).use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                reader.readText()
            }
        }
    } catch (e: Exception) {
        Log.e("MindfulWebView", "Could not load asset: $filename", e)
        ""
    }
}
