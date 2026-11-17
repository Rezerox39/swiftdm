package com.mochyy.mochymusic.downloader

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.createLifecycleAwareWindowRecomposer
import androidx.hilt.navigation.compose.hiltViewModel
import com.mochyy.mochymusic.ui.theme.MochyTheme
import com.mochyy.mochymusic.ui.utils.AppMotion
import dagger.hilt.android.AndroidEntryPoint

/** The only launcher surface now: a downloader app wearing Mochy's design language. */
@AndroidEntryPoint
class DownloaderActivity : ComponentActivity() {

    private val incomingLink = mutableStateOf<String?>(null)

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* progress stays visible whether or not the user grants notifications */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        incomingLink.value = extractLink(intent)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val motionRecomposer = window.decorView.createLifecycleAwareWindowRecomposer(
            AppMotion.durationScale,
            lifecycle,
        )
        setContent(parent = motionRecomposer) {
            MochyTheme {
                DownloaderApp(
                    viewModel = hiltViewModel(),
                    incomingLink = incomingLink.value,
                    onIncomingLinkConsumed = { incomingLink.value = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        incomingLink.value = extractLink(intent)
    }

    private fun extractLink(intent: Intent?): String? {
        if (intent == null) return null
        return when (intent.action) {
            Intent.ACTION_VIEW -> intent.dataString
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)
            else -> null
        }
    }
}
