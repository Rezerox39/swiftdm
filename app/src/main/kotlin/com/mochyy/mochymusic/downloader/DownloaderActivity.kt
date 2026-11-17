package com.mochyy.mochymusic.downloader

import android.Manifest
import android.content.Intent
import android.database.ContentObserver
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.createLifecycleAwareWindowRecomposer
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import com.mochyy.mochymusic.constants.AnimationSpeedKey
import com.mochyy.mochymusic.constants.ElementBlurKey
import com.mochyy.mochymusic.constants.EnableElementBlurAnimatorKey
import com.mochyy.mochymusic.constants.PureBlackKey
import com.mochyy.mochymusic.ui.theme.MochyTheme
import com.mochyy.mochymusic.ui.utils.AppMotion
import com.mochyy.mochymusic.utils.dataStore
import com.mochyy.mochymusic.utils.rememberPreference
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** The only launcher surface now: a downloader app wearing Mochy's design language. */
@AndroidEntryPoint
class DownloaderActivity : ComponentActivity() {

    private val incomingLink = mutableStateOf<String?>(null)
    private val immersive = mutableStateOf(false)
    private var systemScaleObserver: ContentObserver? = null

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* progress stays visible whether or not the user grants notifications */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        systemScaleObserver = AppMotion.followSystemScale(this)
        followLookSettings()
        incomingLink.value = extractLink(intent)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val motionRecomposer = window.decorView.createLifecycleAwareWindowRecomposer(
            AppMotion.durationScale,
            lifecycle,
        )
        setContent(parent = motionRecomposer) {
            val immersiveNow by immersive
            val controller = WindowCompat.getInsetsController(window, window.decorView.rootView)

            LaunchedEffect(immersiveNow) {
                if (immersiveNow) {
                    controller.hide(WindowInsetsCompat.Type.systemBars())
                } else {
                    controller.show(WindowInsetsCompat.Type.systemBars())
                }
            }
            DisposableEffect(Unit) {
                onDispose { controller.show(WindowInsetsCompat.Type.systemBars()) }
            }

            val (pureBlack) = rememberPreference(PureBlackKey, false)
            MochyTheme(pureBlack = pureBlack) {
                DownloaderApp(
                    viewModel = hiltViewModel(),
                    incomingLink = incomingLink.value,
                    onIncomingLinkConsumed = { incomingLink.value = null },
                    onImmersiveChange = { immersive.value = it },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        incomingLink.value = extractLink(intent)
    }

    override fun onDestroy() {
        systemScaleObserver?.let { contentResolver.unregisterContentObserver(it) }
        super.onDestroy()
    }

    private fun extractLink(intent: Intent?): String? {
        if (intent == null) return null
        return when (intent.action) {
            Intent.ACTION_VIEW -> intent.dataString
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)
            else -> null
        }
    }

    /** Mirrors the music app's customization store into the values draw code reads each frame. */
    private fun followLookSettings() {
        lifecycleScope.launch {
            dataStore.data
                .map { prefs ->
                    DownloaderLookSettings(
                        animations = prefs[EnableElementBlurAnimatorKey] ?: true,
                        speed = prefs[AnimationSpeedKey] ?: 1f,
                        elementBlur = prefs[ElementBlurKey] ?: true,
                    )
                }
                .distinctUntilChanged()
                .collect { look ->
                    AppMotion.enabled = look.animations
                    AppMotion.speed = look.speed
                    AppMotion.elementBlur = look.elementBlur
                }
        }
    }

    private data class DownloaderLookSettings(
        val animations: Boolean,
        val speed: Float,
        val elementBlur: Boolean,
    )
}
