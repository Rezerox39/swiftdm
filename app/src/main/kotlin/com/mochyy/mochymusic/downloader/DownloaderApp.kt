@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.mochyy.mochymusic.downloader

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mochyy.mochymusic.ui.component.GlassState
import com.mochyy.mochymusic.ui.component.TopToastHost
import com.mochyy.mochymusic.ui.component.glassRoot
import com.mochyy.mochymusic.ui.utils.AppMotion

enum class DownloaderTab(val label: String) {
    Downloads("Downloads"),
    Browser("Browser"),
    Profile("Profile"),
}

@Composable
fun DownloaderApp(
    viewModel: DownloaderViewModel,
    incomingLink: String?,
    onIncomingLinkConsumed: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf(DownloaderTab.Downloads) }
    val glass = remember { GlassState() }

    LaunchedEffect(incomingLink) {
        if (!incomingLink.isNullOrBlank()) {
            viewModel.receiveSharedText(incomingLink)
            tab = DownloaderTab.Downloads
            onIncomingLinkConsumed()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .glassRoot(glass),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Text(
                        text = "AMz Downloader",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                    if (state.stats.active > 0) {
                        Text(
                            text = "${state.stats.active} · ${DownloaderUiMapper.formatRate(state.stats.bytesPerSecond)}",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                        )
                    }
                }
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ) {
                    DownloaderTab.entries.forEach { item ->
                        NavigationBarItem(
                            selected = tab == item,
                            onClick = { tab = item },
                            icon = {
                                Icon(
                                    when (item) {
                                        DownloaderTab.Downloads -> Icons.Default.Download
                                        DownloaderTab.Browser -> Icons.Default.Language
                                        DownloaderTab.Profile -> Icons.Default.Person
                                    },
                                    contentDescription = item.label,
                                )
                            },
                            label = { Text(item.label, fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )
                    }
                }
            },
        ) { padding ->
            AnimatedContent(
                targetState = tab,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                transitionSpec = {
                    if (!AppMotion.enabled) {
                        EnterTransition.None togetherWith ExitTransition.None
                    } else {
                        (fadeIn(tween(220)) + slideInHorizontally(tween(220)) { it / 16 }) togetherWith
                            (fadeOut(tween(160)) + slideOutHorizontally(tween(160)) { -it / 16 })
                    }
                },
                label = "AMz tab",
            ) { current ->
                when (current) {
                    DownloaderTab.Downloads -> DownloadsScreen(viewModel, state, onOpenBrowser = { tab = DownloaderTab.Browser })
                    DownloaderTab.Browser -> BrowserScreen(
                        desktop = state.desktopBrowser,
                        onDesktopChange = viewModel::setDesktopBrowser,
                        onDownload = { url, userAgent, cookie, referer, disposition, mime ->
                            viewModel.addBrowserDownload(url, userAgent, cookie, referer, disposition, mime)
                            tab = DownloaderTab.Downloads
                        },
                    )
                    DownloaderTab.Profile -> ProfileScreen(viewModel, state)
                }
            }
        }
        TopToastHost()
    }
}
