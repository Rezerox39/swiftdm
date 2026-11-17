@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.mochyy.mochymusic.downloader

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import java.net.URLEncoder

private const val HomeUrl = "https://www.google.com"
private const val DesktopUserAgent =
    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

/** A small WebView whose downloads are captured by the AMz engine with the session's cookies/UA. */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserScreen(
    desktop: Boolean,
    onDesktopChange: (Boolean) -> Unit,
    onDownload: (
        url: String,
        userAgent: String?,
        cookie: String?,
        referer: String?,
        contentDisposition: String?,
        mimeType: String?,
    ) -> Unit,
) {
    val context = LocalContext.current
    val currentDownload by rememberUpdatedState(onDownload)
    val webView = remember { WebView(context.applicationContext) }
    var address by remember { mutableStateOf(HomeUrl) }
    var loadedUrl by remember { mutableStateOf(HomeUrl) }
    var progress by remember { mutableStateOf(0) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }

    DisposableEffect(webView) {
        onDispose { webView.destroy() }
    }

    LaunchedEffect(Unit) {
        webView.loadUrl(HomeUrl)
    }

    BackHandler(enabled = canGoBack) { webView.goBack() }

    Column(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Search or enter a file link") },
                trailingIcon = {
                    IconButton(
                        onClick = {
                            navigate(webView, address, onResolved = { address = it }) { url ->
                                currentDownload(
                                    url,
                                    webView.settings.userAgentString,
                                    CookieManager.getInstance().getCookie(url),
                                    webView.url,
                                    null,
                                    null,
                                )
                            }
                        },
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "Go")
                    }
                },
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { webView.goBack() }, enabled = canGoBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                IconButton(onClick = { webView.goForward() }, enabled = canGoForward) {
                    Icon(Icons.Default.ArrowForward, contentDescription = "Forward")
                }
                IconButton(onClick = { webView.reload() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reload")
                }
                Spacer(Modifier.weight(1f))
                FilterChip(
                    selected = desktop,
                    onClick = { onDesktopChange(!desktop) },
                    label = { Text("Desktop site") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.primary,
                    ),
                )
            }
            if (progress in 1..99) {
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp),
                )
            }
        }

        Text(
            text = loadedUrl,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
        )

        AndroidView(
            factory = { webView },
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)),
            update = { view ->
                view.settings.javaScriptEnabled = true
                view.settings.domStorageEnabled = true
                view.settings.cacheMode = WebSettings.LOAD_DEFAULT
                view.settings.userAgentString = if (desktop) DesktopUserAgent else null
                view.webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                        val url = request.url.toString()
                        if (DownloadLinkPolicy.shouldCapture(url)) {
                            currentDownload(
                                url,
                                view.settings.userAgentString,
                                CookieManager.getInstance().getCookie(url),
                                view.url,
                                null,
                                null,
                            )
                            return true
                        }
                        address = url
                        return false
                    }

                    override fun onPageFinished(view: WebView, url: String) {
                        loadedUrl = url
                        address = url
                        canGoBack = view.canGoBack()
                        canGoForward = view.canGoForward()
                    }
                }
                view.webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView, newProgress: Int) {
                        progress = newProgress
                    }
                }
                view.setDownloadListener(
                    DownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
                        currentDownload(
                            url,
                            userAgent,
                            CookieManager.getInstance().getCookie(url),
                            view.url,
                            contentDisposition,
                            mimeType,
                        )
                    },
                )
            },
        )
    }
}

private fun navigate(
    webView: WebView,
    raw: String,
    onResolved: (String) -> Unit,
    onDirectDownload: (String) -> Unit,
) {
    val value = raw.trim()
    if (value.isBlank()) return
    val url = DownloadLinkPolicy.normalize(value)
        ?: "https://duckduckgo.com/?q=${URLEncoder.encode(value, Charsets.UTF_8.name())}"
    onResolved(url)
    if (DownloadLinkPolicy.shouldCapture(url)) {
        onDirectDownload(url)
    } else {
        webView.loadUrl(url)
    }
}
