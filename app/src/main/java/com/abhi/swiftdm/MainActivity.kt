package com.abhi.swiftdm

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.abhi.swiftdm.ui.SwiftDmApp
import com.abhi.swiftdm.ui.theme.SwiftDmTheme
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
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
        Timber.i("SwiftDM activity created")
        setContent {
            SwiftDmTheme {
                SwiftDmApp()
            }
        }
    }
}
