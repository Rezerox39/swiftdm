package com.abhi.armz.bridge

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.webkit.JavascriptInterface
import androidx.core.content.FileProvider
import com.abhi.armz.data.DownloadRepository
import com.abhi.armz.data.local.DownloadEntity
import com.abhi.armz.engine.DownloadHandle
import com.google.gson.Gson
import com.google.gson.JsonObject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The only object exposed to JavaScript. Methods accept and return JSON strings;
 * every input is validated before it reaches the engine or the file system.
 */
@Singleton
class WebViewBridge @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: DownloadRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val handles = ConcurrentHashMap<String, DownloadHandle>()
    private val preferences = context.getSharedPreferences("armz_web_bridge", Context.MODE_PRIVATE)
    private val gson = Gson()

    @Volatile
    var insets: BridgeInsets = BridgeInsets()

    @Volatile
    var onReady: (() -> Unit)? = null

    @Volatile
    var onSettingsChanged: ((String) -> Unit)? = null

    @Volatile
    var notificationPermissionRequester: (() -> Unit)? = null

    @JavascriptInterface
    fun startDownload(inputJson: String): String {
        val input = BridgeValidation.parseStart(inputJson) ?: return error("invalid_start_input")
        scope.launch {
            runCatching {
                val handle = repository.startNew(
                    url = input.url,
                    segments = input.segments ?: loadSettings().defaultSegments,
                    fileName = null,
                    mimeType = null,
                )
                handles[handle.id] = handle
            }
        }
        return ok()
    }

    @JavascriptInterface
    fun pause(inputJson: String): String {
        val input = BridgeValidation.parseId(inputJson) ?: return error("invalid_id")
        val handle = handles[input.id] ?: return error("unknown_id")
        scope.launch { runCatching { handle.pause() } }
        return ok()
    }

    @JavascriptInterface
    fun resume(inputJson: String): String {
        val input = BridgeValidation.parseId(inputJson) ?: return error("invalid_id")
        scope.launch {
            runCatching {
                repository.resume(input.id)?.let { handle -> handles[handle.id] = handle }
            }
        }
        return ok()
    }

    @JavascriptInterface
    fun cancel(inputJson: String): String {
        val input = BridgeValidation.parseId(inputJson) ?: return error("invalid_id")
        val handle = handles[input.id] ?: return error("unknown_id")
        scope.launch { runCatching { handle.cancel() } }
        return ok()
    }

    @JavascriptInterface
    fun remove(inputJson: String): String {
        val input = BridgeValidation.parseId(inputJson) ?: return error("invalid_id")
        val exists = runCatching { runBlocking(Dispatchers.IO) { repository.find(input.id) } }.getOrNull()
        if (exists == null) return error("unknown_id")
        scope.launch {
            runCatching {
                handles.remove(input.id)?.cancel()
                repository.delete(input.id)
            }
        }
        return ok()
    }

    @JavascriptInterface
    fun pauseAll(): String {
        handles.values.forEach { handle ->
            scope.launch { runCatching { handle.pause() } }
        }
        return ok()
    }

    @JavascriptInterface
    fun resumeAll(): String {
        scope.launch {
            runCatching {
                repository.observeDownloads().first()
                    .filter { it.status == "Paused" }
                    .forEach { entity ->
                        repository.resume(entity.id)?.let { handle -> handles[handle.id] = handle }
                    }
            }
        }
        return ok()
    }

    @JavascriptInterface
    fun listDownloads(): String = runCatching {
        val entities = runBlocking(Dispatchers.IO) { repository.observeDownloads().first() }
        BridgeMapper.downloadsJson(entities)
    }.getOrElse { "[]" }

    @JavascriptInterface
    fun probeUrl(urlJson: String): String {
        val objectValue = BridgeValidation.parseObject(urlJson) ?: return "null"
        val url = objectValue.get("url")?.takeIf { !it.isJsonNull }?.asString?.trim().orEmpty()
        if (!BridgeValidation.isHttpUrl(url)) return "null"
        return runCatching {
            val result = runBlocking(Dispatchers.IO) { repository.probe(url) }
            BridgeMapper.probeJson(result)
        }.getOrElse { "null" }
    }

    @JavascriptInterface
    fun getSettings(): String = BridgeMapper.settingsJson(loadSettings())

    @JavascriptInterface
    fun setSettings(settingsJson: String): String {
        val patch = BridgeValidation.parseObject(settingsJson) ?: return error("invalid_settings")
        val next = runCatching { mergeSettings(loadSettings(), patch) }.getOrElse {
            return error("invalid_settings")
        }
        saveSettings(next)
        onSettingsChanged?.invoke(BridgeMapper.settingsEventJson(next))
        return BridgeMapper.settingsJson(next)
    }

    @JavascriptInterface
    fun openFile(idJson: String): String {
        val input = BridgeValidation.parseId(idJson) ?: return error("invalid_id")
        val entity = runBlocking(Dispatchers.IO) { repository.find(input.id) } ?: return error("unknown_id")
        val uri = uriFor(entity) ?: return error("file_unavailable")
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, entity.mimeType ?: "application/octet-stream")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return launch(intent)
    }

    @JavascriptInterface
    fun shareFile(idJson: String): String {
        val input = BridgeValidation.parseId(idJson) ?: return error("invalid_id")
        val entity = runBlocking(Dispatchers.IO) { repository.find(input.id) } ?: return error("unknown_id")
        val uri = uriFor(entity) ?: return error("file_unavailable")
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = entity.mimeType ?: "application/octet-stream"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return launch(intent)
    }

    @JavascriptInterface
    fun copyText(textJson: String): String {
        val input = BridgeValidation.parseText(textJson) ?: return error("invalid_text")
        val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return error("no_clipboard")
        clipboard.setPrimaryClip(ClipData.newPlainText("ARMz", input.text))
        return ok()
    }

    @JavascriptInterface
    fun haptic(kindJson: String): String {
        val input = BridgeValidation.parseHaptic(kindJson) ?: return error("invalid_haptic")
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            ?: return error("no_vibrator")
        val duration = when (input.kind) {
            "light" -> 12L
            "medium" -> 20L
            "heavy" -> 32L
            "success" -> 18L
            "warning", "error" -> 28L
            else -> 14L
        }
        vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
        return ok()
    }

    @JavascriptInterface
    fun requestNotificationPermission(): String {
        notificationPermissionRequester?.invoke()
        return ok()
    }

    @JavascriptInterface
    fun openBatterySettings(): String {
        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return launch(intent)
    }

    @JavascriptInterface
    fun openAutostartSettings(): String {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return launch(intent)
    }

    @JavascriptInterface
    fun getClipboardUrl(): String {
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        val text = clipboard?.primaryClip
            ?.takeIf { it.itemCount > 0 }
            ?.getItemAt(0)
            ?.coerceToText(context)
            ?.toString()
            ?.trim()
            .orEmpty()
        val url = text.takeIf { BridgeValidation.isHttpUrl(it) }.orEmpty()
        return gson.toJson(mapOf("url" to url))
    }

    @JavascriptInterface
    fun getInsets(): String = BridgeMapper.insetsJson(insets)

    @JavascriptInterface
    fun ready(): String {
        onReady?.invoke()
        return ok()
    }

    fun isEffectsOff(): Boolean = loadSettings().visualEffects == "off"

    private fun loadSettings(): BridgeSettings = BridgeSettings(
        wifiOnly = preferences.getBoolean(KEY_WIFI_ONLY, false),
        maxConcurrentDownloads = preferences.getInt(KEY_MAX_CONCURRENT, 3),
        defaultSegments = preferences.getInt(KEY_DEFAULT_SEGMENTS, 16),
        speedLimitBytesPerSecond = preferences.getLong(KEY_SPEED_LIMIT, -1L).takeIf { it >= 0L },
        keepDownloadsAlive = preferences.getBoolean(KEY_KEEP_ALIVE, true),
        visualEffects = preferences.getString(KEY_VISUAL_EFFECTS, "auto") ?: "auto",
    )

    private fun saveSettings(settings: BridgeSettings) {
        preferences.edit()
            .putBoolean(KEY_WIFI_ONLY, settings.wifiOnly)
            .putInt(KEY_MAX_CONCURRENT, settings.maxConcurrentDownloads)
            .putInt(KEY_DEFAULT_SEGMENTS, settings.defaultSegments)
            .putLong(KEY_SPEED_LIMIT, settings.speedLimitBytesPerSecond ?: -1L)
            .putBoolean(KEY_KEEP_ALIVE, settings.keepDownloadsAlive)
            .putString(KEY_VISUAL_EFFECTS, settings.visualEffects)
            .apply()
    }

    private fun mergeSettings(current: BridgeSettings, patch: JsonObject): BridgeSettings {
        var next = current
        if (patch.has("wifiOnly")) next = next.copy(wifiOnly = patch.get("wifiOnly").asBoolean)
        if (patch.has("keepDownloadsAlive")) next = next.copy(keepDownloadsAlive = patch.get("keepDownloadsAlive").asBoolean)
        if (patch.has("maxConcurrentDownloads")) {
            val value = patch.get("maxConcurrentDownloads").asInt
            require(value in 1..5)
            next = next.copy(maxConcurrentDownloads = value)
        }
        if (patch.has("defaultSegments")) {
            val value = patch.get("defaultSegments").asInt
            require(value in 1..16)
            next = next.copy(defaultSegments = value)
        }
        if (patch.has("speedLimitBytesPerSecond")) {
            val element = patch.get("speedLimitBytesPerSecond")
            val value = if (element.isJsonNull) null else element.asLong
            if (value != null) require(value >= 0L)
            next = next.copy(speedLimitBytesPerSecond = value)
        }
        if (patch.has("visualEffects")) {
            val value = patch.get("visualEffects").asString
            require(value in setOf("auto", "reduced", "off"))
            next = next.copy(visualEffects = value)
        }
        return next
    }

    private fun uriFor(entity: DownloadEntity): Uri? {
        if (!entity.contentUri.isNullOrBlank()) return Uri.parse(entity.contentUri)
        if (entity.filePath.isBlank()) return null
        val file = File(entity.filePath)
        if (!file.exists()) return null
        return FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    }

    private fun launch(intent: Intent): String = runCatching {
        context.startActivity(intent)
        ok()
    }.getOrElse { error("no_activity") }

    private fun ok(): String = "{\"ok\":true}"

    private fun error(message: String): String = "{\"ok\":false,\"error\":${BridgeMapper.quote(message)}}"

    private companion object {
        const val KEY_WIFI_ONLY = "wifiOnly"
        const val KEY_MAX_CONCURRENT = "maxConcurrentDownloads"
        const val KEY_DEFAULT_SEGMENTS = "defaultSegments"
        const val KEY_SPEED_LIMIT = "speedLimitBytesPerSecond"
        const val KEY_KEEP_ALIVE = "keepDownloadsAlive"
        const val KEY_VISUAL_EFFECTS = "visualEffects"
    }
}
