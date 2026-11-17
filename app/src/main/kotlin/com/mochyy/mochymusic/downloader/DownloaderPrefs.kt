package com.mochyy.mochymusic.downloader

import androidx.datastore.preferences.core.booleanPreferencesKey

/** Downloader-only settings. Shared Mochy look settings live in the existing preference keys. */
object DownloaderPrefs {
    val IntroDoneKey = booleanPreferencesKey("amz_downloader_intro_done")
    val CardTiltKey = booleanPreferencesKey("amz_downloader_card_tilt")
    val ImmersiveKey = booleanPreferencesKey("amz_downloader_immersive")
    val ImmersiveAutoHideKey = booleanPreferencesKey("amz_downloader_immersive_autohide")

    /** Mochy's neutral primary; the accent chips overwrite this in the shared look store. */
    val DefaultAccentArgb = 0xFFE0E0E0.toInt()
}
