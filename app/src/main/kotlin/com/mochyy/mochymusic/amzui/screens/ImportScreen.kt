package com.mochyy.mochymusic.amzui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mochyy.mochymusic.amzui.components.PrimaryButton
import com.mochyy.mochymusic.amzui.components.cardSurface
import com.mochyy.mochymusic.amzui.theme.AmzColors
import com.mochyy.mochymusic.amzui.theme.AmzDimens
import com.mochyy.mochymusic.amzui.theme.AmzDuration
import com.mochyy.mochymusic.amzui.theme.AmzShapes
import com.mochyy.mochymusic.amzui.theme.AmzType
import com.mochyy.mochymusic.amzui.theme.amzEnter
import com.mochyy.mochymusic.amzui.theme.amzShimmer

@Composable
fun ImportScreen(onContinue: () -> Unit) {
    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO,
        )
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { onContinue() }

    AmzScreen {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = AmzDimens.screenX),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Import",
                style = AmzType.screenTitle,
                modifier = Modifier
                    .fillMaxWidth()
                    .amzEnter(kind = com.mochyy.mochymusic.amzui.theme.AmzEnterKind.Drop),
            )
            Spacer(Modifier.height(AmzDimens.childGap))
            Text(
                text = "Bring your downloads into AMz",
                style = AmzType.secondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(AmzDimens.importIcon * 2)
                        .clip(AmzShapes.card)
                        .background(cardSurface())
                        .border(AmzDimens.borderThin, AmzColors.White10, AmzShapes.card)
                        .amzShimmer(AmzDuration.Sheen, AmzColors.White8),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Import files",
                        tint = AmzColors.TextPrimary,
                        modifier = Modifier.size(AmzDimens.importIcon),
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = AmzDimens.microGap, bottom = AmzDimens.microGap)
                            .size(AmzDimens.importIcon)
                            .clip(AmzShapes.row)
                            .background(AmzColors.Surface3)
                            .border(AmzDimens.borderThin, AmzColors.White16, AmzShapes.row),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = "Downloads folder",
                            tint = AmzColors.TextPrimary,
                            modifier = Modifier.size(AmzDimens.importIconInner),
                        )
                    }
                }
                Spacer(Modifier.height(AmzDimens.sectionGap))
                Text(
                    text = "Scan your Downloads folder and add files without moving them.",
                    style = AmzType.secondary,
                    textAlign = TextAlign.Center,
                )
            }
            PrimaryButton(
                label = "Import from device",
                onClick = { permissionLauncher.launch(permissions) },
            )
            Spacer(Modifier.height(AmzDimens.screenX))
        }
    }
}
