package com.mochyy.mochymusic.amzui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mochyy.mochymusic.amzui.components.ProgressFill
import com.mochyy.mochymusic.amzui.theme.AmzColors
import com.mochyy.mochymusic.amzui.theme.AmzDimens
import com.mochyy.mochymusic.amzui.theme.AmzDuration
import com.mochyy.mochymusic.amzui.theme.AmzEase
import com.mochyy.mochymusic.amzui.theme.AmzType
import com.mochyy.mochymusic.amzui.theme.amzEnter
import com.mochyy.mochymusic.amzui.theme.cssLinearGradient
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        started = true
        delay(AmzDuration.Splash.toLong())
        onFinished()
    }

    val scale by animateFloatAsState(
        targetValue = if (started) 1f else 0.72f,
        animationSpec = tween(AmzDuration.Rise, easing = AmzEase),
        label = "splashLogo",
    )
    val progress by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(AmzDuration.Splash, easing = AmzEase),
        label = "splashProgress",
    )

    AmzScreen {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = AmzDimens.screenX),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(AmzDimens.splashLogo)
                    .clip(RoundedCornerShape(AmzDimens.cardRadius))
                    .background(
                        cssLinearGradient(
                            145f,
                            0f to AmzColors.AccentBlueStart,
                            1f to AmzColors.AccentBlueEnd,
                        ),
                    )
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        alpha = scale
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "AMZ",
                    style = AmzType.screenTitle.copy(color = AmzColors.Background),
                )
            }
            Spacer(Modifier.height(24.dp))
            Text(
                text = "AMZ Downloader",
                style = AmzType.sectionTitle,
                modifier = Modifier.amzEnter(delayMs = 180),
            )
            Spacer(Modifier.height(AmzDimens.childGap))
            ProgressFill(
                progress = progress,
                hero = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
