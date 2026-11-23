package com.naman.focusos.ui.components

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay

class SplashBridge(private val onComplete: () -> Unit) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isCalled = false

    @JavascriptInterface
    fun onAnimationFinished() {
        mainHandler.post {
            if (!isCalled) {
                isCalled = true
                onComplete()
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SplashScreen(
    onAnimationComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var completed by remember { mutableStateOf(false) }

    val completeAction = remember {
        {
            if (!completed) {
                completed = true
                onAnimationComplete()
            }
        }
    }

    // Safety timeout: animation duration is ~2.93s; transition after 3.2s if callback doesn't arrive
    LaunchedEffect(Unit) {
        delay(3200L)
        completeAction()
    }

    // Clean up WebView resources on exit
    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.let { wv ->
                wv.stopLoading()
                wv.removeJavascriptInterface("AndroidBridge")
                wv.destroy()
            }
            webViewRef = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color(0xFF0B1018))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                completeAction()
            }
    ) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setBackgroundColor(Color.parseColor("#0B1018"))
                    isVerticalScrollBarEnabled = false
                    isHorizontalScrollBarEnabled = false
                    overScrollMode = WebView.OVER_SCROLL_NEVER

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        cacheMode = WebSettings.LOAD_NO_CACHE
                        allowFileAccess = true
                        allowContentAccess = true
                    }

                    addJavascriptInterface(SplashBridge { completeAction() }, "AndroidBridge")

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            view?.setBackgroundColor(Color.parseColor("#0B1018"))
                        }
                    }

                    loadUrl("file:///android_asset/splash.html")
                    webViewRef = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
