package com.abhi.armz.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Haptics are reserved for state changes that matter: pause, resume and
 * completion. Navigation uses [selection] only.
 */
@Stable
class SwiftHaptics(private val feedback: HapticFeedback) {
    fun selection() {
        feedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    fun impact() {
        feedback.performHapticFeedback(HapticFeedbackType.LongPress)
    }
}

@Composable
fun rememberSwiftHaptics(): SwiftHaptics {
    val feedback = LocalHapticFeedback.current
    return remember(feedback) { SwiftHaptics(feedback) }
}
