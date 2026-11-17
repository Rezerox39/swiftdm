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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mochyy.mochymusic.constants.PureBlackKey
import com.mochyy.mochymusic.constants.SelectedThemeColorKey
import com.mochyy.mochymusic.constants.SlimNavBarKey
import com.mochyy.mochymusic.ui.component.GlassState
import com.mochyy.mochymusic.ui.component.TopToastHost
import com.mochyy.mochymusic.ui.component.glassRoot
import com.mochyy.mochymusic.ui.utils.AppMotion
import com.mochyy.mochymusic.utils.rememberPreference

enum class DownloaderTab(val label: String) {
    Downloads("Downloads"),
    Browser("Browser"),
    Profile("Profile"),
    Settings("Settings"),
}

@Composable
fun DownloaderApp(
    viewModel: DownloaderViewModel,
    incomingLink: String?,
    onIncomingLinkConsumed: () -> Unit,
    onImmersiveChange: (Boolean) -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf(DownloaderTab.Downloads) }
    val glass = remember { GlassState() }
    val context = LocalContext.current

    val (introDone, setIntroDone) = rememberPreference(DownloaderPrefs.IntroDoneKey, false)
    val (immersiveEnabled) = rememberPreference(DownloaderPrefs.ImmersiveKey, true)
    val (immersiveAutoHide) = rememberPreference(DownloaderPrefs.ImmersiveAutoHideKey, true)
    val (pureBlack) = rememberPreference(PureBlackKey, false)
    val (slimNav) = rememberPreference(SlimNavBarKey, false)
    val (accentArgb) = rememberPreference(SelectedThemeColorKey, DownloaderPrefs.DefaultAccentArgb)

    var immersiveRowId by remember { mutableStateOf<String?>(null) }
    val immersiveRow = remember(immersiveRowId, state.rows) {
        immersiveRowId?.let { id -> state.rows.firstOrNull { it.id == id } }
    }

    LaunchedEffect(incomingLink) {
        if (!incomingLink.isNullOrBlank()) {
            if (!introDone) setIntroDone(true)
            viewModel.receiveSharedText(incomingLink)
            tab = DownloaderTab.Downloads
            onIncomingLinkConsumed()
        }
    }

    LaunchedEffect(immersiveRowId, immersiveRow) {
        if (immersiveRowId != null && immersiveRow == null) immersiveRowId = null
    }
    LaunchedEffect(immersiveRow) {
        onImmersiveChange(immersiveRow != null)
    }
    DisposableEffect(Unit) {
        onDispose { onImmersiveChange(false) }
    }

    val accent = Color(accentArgb)
    val accentScheme = MaterialTheme.colorScheme.copy(
        primary = accent,
        onPrimary = Color.Black,
        primaryContainer = accent.copy(alpha = 0.18f),
        onPrimaryContainer = accent,
    )

    val navItems = remember {
        listOf(
            DownloaderNavItem(DownloaderTab.Downloads.label, Icons.Default.Download),
            DownloaderNavItem(DownloaderTab.Browser.label, Icons.Default.Language),
            DownloaderNavItem(DownloaderTab.Profile.label, Icons.Default.Person),
            DownloaderNavItem(DownloaderTab.Settings.label, Icons.Default.Settings),
        )
    }

    MaterialTheme(colorScheme = accentScheme) {
        if (!introDone) {
            DownloaderIntroScreen(onDone = { setIntroDone(true) })
        } else {
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
                        DownloaderBottomBar(
                            items = navItems,
                            selectedIndex = tab.ordinal,
                            onSelect = { index -> tab = DownloaderTab.entries[index] },
                            glass = glass,
                            pureBlack = pureBlack,
                            slim = slimNav,
                        )
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
                            DownloaderTab.Downloads -> DownloadsScreen(
                                viewModel = viewModel,
                                state = state,
                                onOpenBrowser = { tab = DownloaderTab.Browser },
                                onOpenImmersive = { row ->
                                    if (immersiveEnabled) immersiveRowId = row.id
                                },
                            )

                            DownloaderTab.Browser -> BrowserScreen(
                                desktop = state.desktopBrowser,
                                onDesktopChange = viewModel::setDesktopBrowser,
                                onDownload = { url, userAgent, cookie, referer, disposition, mime ->
                                    viewModel.addBrowserDownload(url, userAgent, cookie, referer, disposition, mime)
                                    tab = DownloaderTab.Downloads
                                },
                            )

                            DownloaderTab.Profile -> ProfileScreen(viewModel, state)

                            DownloaderTab.Settings -> DownloaderSettingsScreen(
                                viewModel = viewModel,
                                state = state,
                                onReplayIntro = { setIntroDone(false) },
                            )
                        }
                    }
                }

                immersiveRow?.let { row ->
                    DownloaderImmersiveScreen(
                        row = row,
                        onClose = { immersiveRowId = null },
                        onPause = { viewModel.pause(row.id) },
                        onResume = { viewModel.resume(row.id) },
                        onCancel = {
                            viewModel.cancel(row.id)
                            immersiveRowId = null
                        },
                        onOpen = { openDownload(context, row) },
                        autoHideControls = immersiveAutoHide,
                    )
                }
            }
        }
    }

    TopToastHost()
}
