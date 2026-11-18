package com.mochyy.mochymusic.amzui.screens

import android.net.Uri
import android.webkit.DownloadListener
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mochyy.mochymusic.amzui.AmzUiViewModel
import com.mochyy.mochymusic.amzui.components.AmzChip
import com.mochyy.mochymusic.amzui.components.Avatar
import com.mochyy.mochymusic.amzui.components.FileTile
import com.mochyy.mochymusic.amzui.components.RoundIconButton
import com.mochyy.mochymusic.amzui.theme.amzPress
import com.mochyy.mochymusic.amzui.components.cardSurface
import com.mochyy.mochymusic.amzui.data.AmzDetectedFile
import com.mochyy.mochymusic.amzui.data.AmzFileKind
import com.mochyy.mochymusic.amzui.data.gradient
import com.mochyy.mochymusic.amzui.theme.AmzColors
import com.mochyy.mochymusic.amzui.theme.AmzDimens
import com.mochyy.mochymusic.amzui.theme.AmzShapes
import com.mochyy.mochymusic.amzui.theme.AmzType

@Composable
fun BrowserScreen(
    viewModel: AmzUiViewModel,
    profileName: String,
    profilePhoto: String?,
    initialLink: String?,
    onLinkConsumed: () -> Unit,
    onAvatarClick: () -> Unit,
    onBack: () -> Unit,
    onTab: (Int) -> Unit,
) {
    val detected by viewModel.detected.collectAsStateWithLifecycle()
    var paste by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("releases.ubuntu.com") }
    var filter by remember { mutableStateOf("All") }
    var webView by remember { mutableStateOf<WebView?>(null) }

    LaunchedEffect(initialLink) {
        if (!initialLink.isNullOrBlank()) {
            paste = initialLink
            onLinkConsumed()
        }
    }

    val visible = detected.filter { file ->
        when (filter) {
            "Links" -> file.kind == AmzFileKind.OTHER
            "Files" -> file.kind != AmzFileKind.OTHER
            else -> true
        }
    }

    AmzScreen {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = AmzDimens.screenX)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AmzDimens.smallGap),
            ) {
                RoundIconButton(
                    size = AmzDimens.browserBack,
                    background = AmzColors.Surface2,
                    contentDescription = "Back",
                    onClick = onBack,
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = null,
                        tint = AmzColors.TextPrimary,
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(AmzDimens.searchHeight)
                        .clip(AmzShapes.round)
                        .background(AmzColors.Surface2)
                        .padding(horizontal = AmzDimens.statPadX),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Secure connection",
                            tint = AmzColors.Success,
                            modifier = Modifier.size(AmzDimens.browserLockIcon),
                        )
                        Spacer(Modifier.width(AmzDimens.smallGap))
                        Text(
                            text = address,
                            style = AmzType.secondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Avatar(
                    name = profileName,
                    photoUri = profilePhoto,
                    size = AmzDimens.avatarTile,
                    onClick = onAvatarClick,
                )
            }
            Spacer(Modifier.height(AmzDimens.childGap))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AmzDimens.searchHeight)
                    .clip(AmzShapes.round)
                    .background(AmzColors.SurfacePill)
                    .border(AmzDimens.borderThin, AmzColors.White8, AmzShapes.round)
                    .padding(start = AmzDimens.statPadX, end = AmzDimens.microGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicTextField(
                    value = paste,
                    onValueChange = { paste = it },
                    singleLine = true,
                    textStyle = AmzType.secondary,
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        if (paste.isEmpty()) Text("Paste a download link", style = AmzType.secondary)
                        inner()
                    },
                )
                if (paste.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .height(AmzDimens.pillDownloadHeight)
                            .clip(AmzShapes.round)
                            .background(AmzColors.TextPrimary)
                            .amzPress(0.94f) {
                                val url = paste.trim()
                                address = Uri.parse(url).host ?: url
                                webView?.loadUrl(url)
                                viewModel.add(url)
                            }
                            .padding(horizontal = AmzDimens.chipPaddingX),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Go", style = AmzType.chipSelected)
                    }
                }
            }
            Spacer(Modifier.height(AmzDimens.childGap))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AmzDimens.chipGap),
            ) {
                listOf("All", "Links", "Files").forEach { label ->
                    AmzChip(
                        label = label,
                        selected = filter == label,
                        onClick = { filter = label },
                    )
                }
            }
            Spacer(Modifier.height(AmzDimens.smallGap))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(AmzDimens.cardRadius))
                    .background(cardSurface()),
            ) {
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            setBackgroundColor(AmzColors.Background.toArgb())
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                cacheMode = WebSettings.LOAD_DEFAULT
                                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            }
                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    if (!url.isNullOrBlank()) {
                                        address = Uri.parse(url).host ?: address
                                    }
                                }
                            }
                            setDownloadListener(
                                DownloadListener { url, _, _, _, _ ->
                                    if (!url.isNullOrBlank()) viewModel.add(url)
                                },
                            )
                            loadUrl("https://releases.ubuntu.com/")
                        }.also { webView = it }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Spacer(Modifier.height(AmzDimens.smallGap))
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AmzDimens.browserRowHeight * 2),
                verticalArrangement = Arrangement.spacedBy(AmzDimens.row8),
            ) {
                items(visible) { file -> DetectedFileRow(file, onAdd = { viewModel.add(file.url) }) }
            }
            BottomNav(selected = 1, onSelect = onTab)
        }
    }
}

@Composable
private fun DetectedFileRow(
    file: AmzDetectedFile,
    onAdd: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(AmzDimens.browserRowHeight)
            .clip(AmzShapes.row)
            .background(AmzColors.Surface2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AmzDimens.childGap),
    ) {
        FileTile(
            ext = file.kind.extension,
            tileSize = AmzDimens.browserTile,
            radius = AmzDimens.rowTileRadius,
            fontSize = AmzType.tertiary.fontSize,
            gradient = file.kind.gradient(),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.name,
                style = AmzType.rowTitleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(text = file.sizeLabel, style = AmzType.tertiary)
        }
        RoundIconButton(
            size = AmzDimens.browserButton,
            background = AmzColors.Surface3,
            contentDescription = "Add ${file.name}",
            onClick = onAdd,
        ) {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = null,
                tint = AmzColors.TextPrimary,
            )
        }
    }
}
