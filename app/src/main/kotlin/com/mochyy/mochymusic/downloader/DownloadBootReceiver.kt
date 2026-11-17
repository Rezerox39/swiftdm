package com.mochyy.mochymusic.downloader

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Restarts transfers interrupted by a reboot or an update. */
@AndroidEntryPoint
class DownloadBootReceiver : BroadcastReceiver() {

    @Inject lateinit var controller: DownloadController

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED &&
            action != "android.intent.action.QUICKBOOT_POWERON"
        ) return

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                controller.resumeInterrupted()
            } finally {
                pending.finish()
            }
        }
    }
}
