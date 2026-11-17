package com.mochyy.mochymusic.downloader

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.abhi.armz.data.DownloadRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.max

/** Keeps segmented transfers alive when the app is backgrounded and summarises their progress. */
@AndroidEntryPoint
class DownloaderService : Service() {

    @Inject lateinit var repository: DownloadRepository
    @Inject lateinit var controller: DownloadController

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var started = false

    override fun onCreate() {
        super.onCreate()
        createChannel()
        scope.launch {
            repository.observeDownloads().collectLatest { rows ->
                val active = rows.filter { it.status in ACTIVE_STATUSES }
                if (active.isEmpty()) {
                    if (started) stopSelf()
                    return@collectLatest
                }
                val speed = active.sumOf { max(0L, it.bytesPerSecond) }
                startForegroundNotification(active.size, speed)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_ALL -> scope.launch { controller.pauseAll() }
        }
        startForegroundNotification(0, 0L)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun startForegroundNotification(activeCount: Int, speedBps: Long) {
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, DownloaderActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stop = PendingIntent.getService(
            this,
            1,
            Intent(this, DownloaderService::class.java).setAction(ACTION_STOP_ALL),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(if (activeCount == 0) "AMz Downloader" else "$activeCount active download${if (activeCount == 1) "" else "s"}")
            .setContentText(if (speedBps > 0L) "${formatRate(speedBps)} · tap to manage" else "Tap to manage downloads")
            .setContentIntent(open)
            .setOngoing(activeCount > 0)
            .addAction(android.R.drawable.ic_media_pause, "Pause all", stop)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            @Suppress("DEPRECATION")
            startForeground(NOTIFICATION_ID, notification)
        }
        started = true
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        if (manager?.getNotificationChannel(CHANNEL_ID) != null) return
        manager?.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Downloads",
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = "Active AMz download progress" },
        )
    }

    private fun formatRate(bytesPerSecond: Long): String = when {
        bytesPerSecond >= 1_000_000_000L -> String.format("%.1f GB/s", bytesPerSecond / 1_000_000_000.0)
        bytesPerSecond >= 1_000_000L -> String.format("%.1f MB/s", bytesPerSecond / 1_000_000.0)
        bytesPerSecond >= 1_000L -> String.format("%.1f KB/s", bytesPerSecond / 1_000.0)
        else -> "$bytesPerSecond B/s"
    }

    companion object {
        private const val CHANNEL_ID = "amz_downloads"
        private const val NOTIFICATION_ID = 8402
        private const val ACTION_STOP_ALL = "com.abhi.armz.STOP_ALL"
        private val ACTIVE_STATUSES = setOf("Probing", "Downloading", "Finalizing")

        fun start(context: Context) {
            val intent = Intent(context, DownloaderService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
