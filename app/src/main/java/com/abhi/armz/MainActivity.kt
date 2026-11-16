package com.abhi.armz

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.webkit.WebViewAssetLoader
import com.abhi.armz.bridge.BridgeInsets
import com.abhi.armz.bridge.BridgeMapper
import com.abhi.armz.bridge.WebViewBridge
import com.abhi.armz.data.DownloadRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var bridge: WebViewBridge

    @Inject
    lateinit var repository: DownloadRepository

    private lateinit var webView: WebView
    private lateinit var root: FrameLayout
    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val assetLoader by lazy {
        WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()
    }

    @Volatile
    private var webReady = false

    private var lastTopInset = 0
    private var lastBottomInset = 0
    private var lastLeftInset = 0
    private var lastRightInset = 0
    private var lastImeInset = 0

    private var pendingEvent: String? = null
    private var lastEmitAt = 0L
    private var eventJob: Job? = null

    private val emitRunnable = Runnable {
        val json = pendingEvent
        if (json != null) {
            pendingEvent = null
            lastEmitAt = SystemClock.uptimeMillis()
            webView.evaluateJavascript(
                "window.__armz_emit&&window.__armz_emit(${BridgeMapper.quote(json)})",
                null,
            )
        }
    }

    private val backCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (::webView.isInitialized && webView.canGoBack()) {
                webView.goBack()
            } else {
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        splash.setKeepOnScreenCondition { !webReady }
        splash.setOnExitAnimationListener { provider -> provider.remove() }

        bridge.onReady = { markWebReady() }
        bridge.onSettingsChanged = { json -> queueEvent(json) }
        bridge.notificationPermissionRequester = { requestNotificationPermissionIfNeeded() }
        onBackPressedDispatcher.addCallback(this, backCallback)

        root = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
        }
        webView = createWebView()
        root.addView(
            webView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
        setContentView(root)

        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            lastTopInset = bars.top
            lastBottomInset = bars.bottom
            lastLeftInset = bars.left
            lastRightInset = bars.right
            lastImeInset = ime.bottom.coerceAtLeast(0)
            bridge.insets = BridgeInsets(
                top = lastTopInset,
                bottom = lastBottomInset,
                left = lastLeftInset,
                right = lastRightInset,
            )
            applyCssInsets()
            insets
        }
        ViewCompat.requestApplyInsets(root)

        webView.loadUrl("https://appassets.androidplatform.net/assets/web/index.html")
        mainHandler.postDelayed({ markWebReady() }, SPLASH_FALLBACK_MS)

        eventJob = scope.launch {
            repository.observeDownloads().collect { downloads ->
                queueEvent(BridgeMapper.downloadsJson(downloads))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (webReady) pushPerfTier()
    }

    override fun onDestroy() {
        eventJob?.cancel()
        scope.cancel()
        mainHandler.removeCallbacksAndMessages(null)
        if (::webView.isInitialized) {
            webView.removeJavascriptInterface("ARMz")
            root.removeView(webView)
            webView.destroy()
        }
        super.onDestroy()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebView(): WebView = WebView(this).apply {
        setBackgroundColor(Color.TRANSPARENT)
        setLayerType(View.LAYER_TYPE_HARDWARE, null)
        overScrollMode = View.OVER_SCROLL_NEVER
        isHorizontalScrollBarEnabled = false
        isVerticalScrollBarEnabled = false

        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        settings.textZoom = 100
        settings.setSupportZoom(false)
        settings.builtInZoomControls = false
        settings.displayZoomControls = false
        settings.mediaPlaybackRequiresUserGesture = true
        settings.javaScriptCanOpenWindowsAutomatically = false
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.loadsImagesAutomatically = true
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = false

        webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest,
            ): WebResourceResponse? = assetLoader.shouldInterceptRequest(request.url)

            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest,
            ): Boolean {
                val scheme = request.url.scheme
                return request.url.host != ASSET_HOST || (scheme != "https" && scheme != "http")
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                applyCssInsets()
            }
        }

        addJavascriptInterface(bridge, "ARMz")
    }

    private fun markWebReady() {
        mainHandler.post {
            if (webReady) return@post
            webReady = true
            applyCssInsets()
            pushPerfTier()
        }
    }

    private fun pushPerfTier() {
        val tier = computePerfTier()
        if (::webView.isInitialized) {
            webView.evaluateJavascript(
                "window.__armz_perf&&window.__armz_perf(${BridgeMapper.quote(tier)})",
                null,
            )
        }
    }

    private fun computePerfTier(): String {
        val activityManager = getSystemService(ActivityManager::class.java)
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memoryInfo)
        val lowRam = activityManager?.isLowRamDevice == true ||
            (memoryInfo.totalMem in 1L..LOW_RAM_BYTES)
        val powerSave = getSystemService(PowerManager::class.java)?.isPowerSaveMode == true
        return if (lowRam || powerSave || bridge.isEffectsOff()) "low" else "high"
    }

    private fun applyCssInsets() {
        if (!::webView.isInitialized) return
        webView.evaluateJavascript(
            """
            (() => {
              const s = document.documentElement.style;
              s.setProperty('--sat', '${lastTopInset}px');
              s.setProperty('--sab', '${lastBottomInset}px');
              s.setProperty('--sal', '${lastLeftInset}px');
              s.setProperty('--sar', '${lastRightInset}px');
              s.setProperty('--kb', '${lastImeInset}px');
            })();
            """.trimIndent(),
            null,
        )
    }

    private fun queueEvent(json: String) {
        mainHandler.post {
            pendingEvent = json
            val now = SystemClock.uptimeMillis()
            val delay = (lastEmitAt + MIN_EMIT_INTERVAL_MS - now).coerceAtLeast(0L)
            mainHandler.removeCallbacks(emitRunnable)
            mainHandler.postDelayed(emitRunnable, delay)
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION_REQUEST_CODE)
        }
    }

    private companion object {
        const val ASSET_HOST = "appassets.androidplatform.net"
        const val SPLASH_FALLBACK_MS = 2_000L
        const val MIN_EMIT_INTERVAL_MS = 200L
        const val LOW_RAM_BYTES = 3L * 1024L * 1024L * 1024L
        const val NOTIFICATION_REQUEST_CODE = 731
    }
}
