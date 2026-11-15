package com.abhi.armz

import android.animation.ValueAnimator
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import com.abhi.armz.ui.ArmzApp
import com.abhi.armz.ui.theme.ArmzTheme
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        splash.setOnExitAnimationListener { provider ->
            val icon = provider.iconView
            if (!ValueAnimator.areAnimatorsEnabled()) {
                provider.remove()
                return@setOnExitAnimationListener
            }
            val spring = SpringForce(0.84f)
                .setDampingRatio(0.8f)
                .setStiffness(300f)
            SpringAnimation(icon, DynamicAnimation.SCALE_X)
                .setSpring(spring)
                .addEndListener { _, _, _, _ ->
                    icon.animate()
                        .alpha(0f)
                        .setDuration(120L)
                        .withEndAction { provider.remove() }
                        .start()
                }
                .start()
            SpringAnimation(icon, DynamicAnimation.SCALE_Y)
                .setSpring(spring)
                .start()
        }
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                lightScrim = Color.TRANSPARENT,
                darkScrim = Color.TRANSPARENT,
            ),
            navigationBarStyle = SystemBarStyle.auto(
                lightScrim = Color.argb(0xE6, 0xF2, 0xF2, 0xF7),
                darkScrim = Color.argb(0xE6, 0x00, 0x00, 0x00),
            ),
        )
        super.onCreate(savedInstanceState)
        Timber.i("ARMz activity created (cold=%s)", savedInstanceState == null)
        setContent {
            ArmzTheme {
                ArmzApp(showIntro = savedInstanceState == null)
            }
        }
    }
}
