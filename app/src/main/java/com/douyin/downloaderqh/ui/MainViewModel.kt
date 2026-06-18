package com.douyin.downloaderqh.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.douyin.downloaderqh.api.DouyinApiClient
import com.douyin.downloaderqh.data.SettingsDataStore
import com.douyin.downloaderqh.download.DownloadManager
import com.douyin.downloaderqh.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import okhttp3.*
import kotlinx.serialization.json.*

@Serializable
data class HistoryEntry(
    val url: String,
    val title: String,
    val type: String,
    val timestamp: Long,
    val avatar: String = "",
    val author: String = ""
)

data class MainUiState(
    val currentTab: Int = 0,
    val selectedPlatform: Platform = Platform.DOUYIN,
    val defaultPage: Int = 0,

    val shareUrl: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val parsedTitle: String = "",
    val parsedCover: String = "",
    val parsedDesc: String = "",
    val downloadItems: List<DownloadItem> = emptyList(),

    val downloadStatus: Map<Int, DownloadStatus> = emptyMap(),
    val downloadProgress: Map<Int, Float> = emptyMap(),
    val downloadSpeed: Map<Int, String> = emptyMap(),

    val savePath: String = "",
    val bgWallpaperUri: String = "",
    val bgWallpaperType: String = "none",
    val bgBlurRadius: Float = 0f,
    val bgOpacity: Float = 0.5f,
    val videoSoundEnabled: Boolean = false,

    val parseHistory: List<HistoryEntry> = emptyList(),
    val updateChannel: String = "beta",
    val latestVersion: String = ""
)

sealed class DownloadStatus {
    data object Idle : DownloadStatus()
    data object Downloading : DownloadStatus()
    data class Success(val path: String) : DownloadStatus()
    data class Error(val message: String) : DownloadStatus()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val apiClient = DouyinApiClient()
    val downloadManager = DownloadManager(application)
    private val settingsStore = SettingsDataStore(application)
    private val appContext = application
    private val updateClient = OkHttpClient()

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { settingsStore.savePath.collect { v -> _uiState.update { it.copy(savePath = v) } } }
        viewModelScope.launch { settingsStore.bgWallpaperUri.collect { v -> _uiState.update { it.copy(bgWallpaperUri = v) } } }
        viewModelScope.launch { settingsStore.bgWallpaperType.collect { v -> _uiState.update { it.copy(bgWallpaperType = v) } } }
        viewModelScope.launch { settingsStore.bgBlurRadius.collect { v -> _uiState.update { it.copy(bgBlurRadius = v) } } }
        viewModelScope.launch { settingsStore.bgOpacity.collect { v -> _uiState.update { it.copy(bgOpacity = v) } } }
        viewModelScope.launch { settingsStore.videoSoundEnabled.collect { v -> _uiState.update { it.copy(videoSoundEnabled = v) } } }
        viewModelScope.launch { settingsStore.parseHistory.collect { v -> _uiState.update { it.copy(parseHistory = v) } } }
        viewModelScope.launch { settingsStore.updateChannel.collect { v -> _uiState.update { it.copy(updateChannel = v) } } }
        viewModelScope.launch { settingsStore.defaultPage.collect { v -> _uiState.update { it.copy(defaultPage = v, currentTab = v) } } }
    }

    fun selectTab(index: Int, platform: Platform) {
        _uiState.update { it.copy(currentTab = index, selectedPlatform = platform, shareUrl = "", error = null, downloadItems = emptyList(), parsedTitle = "", parsedCover = "", parsedDesc = "", downloadStatus = emptyMap(), downloadProgress = emptyMap(), downloadSpeed = emptyMap()) }
    }

    fun updateShareUrl(url: String) { _uiState.update { it.copy(shareUrl = url, error = null) } }

    fun parseVideo() {
        val url = _uiState.value.shareUrl.trim()
        if (url.isEmpty()) { _uiState.update { it.copy(error = "请输入链接") }; return }
        val platform = _uiState.value.selectedPlatform
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, parsedTitle = "", parsedCover = "", parsedDesc = "", downloadItems = emptyList(), downloadStatus = emptyMap(), downloadProgress = emptyMap(), downloadSpeed = emptyMap()) }
            when (platform) {
                Platform.DOUYIN -> parseDouyin(url)
                Platform.XIAOHONGSHU -> parseXiaohongshu(url)
            }
        }
    }

    private suspend fun parseDouyin(url: String) {
        apiClient.parseVideo(url).fold(
            onSuccess = { response ->
                val data = response.data
                if (data != null) {
                    val items = data.getAllVideoUrls()
                    settingsStore.addParseHistory(HistoryEntry(url = url, title = data.title.ifEmpty { "未知视频" }, type = data.type, timestamp = System.currentTimeMillis(), avatar = data.author?.avatar ?: "", author = data.author?.name ?: ""))
                    _uiState.update { it.copy(isLoading = false, parsedTitle = data.title, parsedCover = data.cover, parsedDesc = data.desc, downloadItems = items, error = null) }
                } else { _uiState.update { it.copy(isLoading = false, error = "API返回了空数据") } }
            },
            onFailure = { e -> _uiState.update { it.copy(isLoading = false, error = e.message ?: "解析失败") } }
        )
    }

    /** 解小红书短链接 (xhslink.com → xiaohongshu.com/explore/...) */
    private suspend fun resolveXhsShortLink(shortUrl: String): String {
        try {
            val request = Request.Builder()
                .url(shortUrl)
                .head()
                .addHeader("User-Agent", "DouyinDownloader/2.0 (Android)")
                .build()
            val client = OkHttpClient.Builder()
                .followRedirects(false)  // 手动处理重定向
                .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .build()
            val response = client.newCall(request).execute()
            val location = response.header("Location") ?: ""
            response.close()
            if (location.isNotBlank()) {
                // 取重定向目标中 xiaohongshu.com/explore/ 的完整 URL
                val fullUrl = if (location.startsWith("http")) location
                else "https://www.xiaohongshu.com$location"
                return fullUrl
            }
        } catch (_: Exception) {}
        return shortUrl // 解链失败，用原链
    }

    private suspend fun parseXiaohongshu(url: String) {
        // 检测并解短链接
        val resolvedUrl = if (url.contains("xhslink.com")) {
            resolveXhsShortLink(url).also {
                if (it != url) _uiState.update { s -> s.copy(shareUrl = it) }
            }
        } else url

        apiClient.parseXiaohongshu(resolvedUrl).fold(
            onSuccess = { response ->
                val dataList = response.data
                if (dataList != null && dataList.isNotEmpty()) {
                    val combined = dataList.toCombined()
                    if (combined != null) {
                        val items = combined.getAllDownloadItems()
                        settingsStore.addParseHistory(HistoryEntry(url = url, title = combined.title.ifEmpty { "未知笔记" }, type = "xhs", timestamp = System.currentTimeMillis(), avatar = combined.avatar, author = combined.author))
                        _uiState.update { it.copy(isLoading = false, parsedTitle = combined.title, parsedCover = combined.cover, parsedDesc = combined.desc, downloadItems = items, error = null) }
                    } else { _uiState.update { it.copy(isLoading = false, error = "解析失败：无法合并数据") } }
                } else { _uiState.update { it.copy(isLoading = false, error = "API返回了空数据: ${response.msg}") } }
            },
            onFailure = { e -> _uiState.update { it.copy(isLoading = false, error = e.message ?: "解析失败") } }
        )
    }

    fun downloadItem(index: Int, item: DownloadItem) {
        val statusMap = _uiState.value.downloadStatus.toMutableMap()
        if (statusMap[index] is DownloadStatus.Downloading) return
        statusMap[index] = DownloadStatus.Downloading
        _uiState.update { it.copy(downloadStatus = statusMap) }
        triggerLightHaptic()
        viewModelScope.launch {
            try {
                downloadManager.downloadSync(item) { progress, speed ->
                    val pm = _uiState.value.downloadProgress.toMutableMap(); val sm = _uiState.value.downloadSpeed.toMutableMap()
                    pm[index] = progress; sm[index] = speed
                    _uiState.update { it.copy(downloadProgress = pm, downloadSpeed = sm) }
                }
                val map = _uiState.value.downloadStatus.toMutableMap()
                map[index] = DownloadStatus.Success("")
                _uiState.update { it.copy(downloadStatus = map) }
            } catch (e: Exception) {
                val map = _uiState.value.downloadStatus.toMutableMap()
                map[index] = DownloadStatus.Error(e.message ?: "下载失败")
                _uiState.update { it.copy(downloadStatus = map) }
            }
        }
    }

    private fun triggerLightHaptic() {
        try {
            val ctx = appContext
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                manager.defaultVibrator.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                (ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        } catch (_: Exception) {}
    }

    fun setSavePath(uri: Uri) { viewModelScope.launch { settingsStore.setSavePath(uri.toString()) } }
    fun setBgWallpaper(uri: Uri, type: String) { viewModelScope.launch { getApplication<Application>().contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION); settingsStore.setBgWallpaper(uri.toString(), type) } }
    fun setBgBlurRadius(radius: Float) { viewModelScope.launch { settingsStore.setBgBlurRadius(radius) } }
    fun setBgOpacity(opacity: Float) { viewModelScope.launch { settingsStore.setBgOpacity(opacity) } }
    fun setVideoSoundEnabled(enabled: Boolean) { viewModelScope.launch { settingsStore.setVideoSoundEnabled(enabled) } }
    fun clearBgWallpaper() { viewModelScope.launch { val curUri = _uiState.value.bgWallpaperUri; if (curUri.isNotBlank()) try { getApplication<Application>().contentResolver.releasePersistableUriPermission(Uri.parse(curUri), android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}; settingsStore.setBgWallpaper("", "none") } }
    fun clearHistory() { viewModelScope.launch { settingsStore.clearParseHistory() } }
    fun clearError() { _uiState.update { it.copy(error = null) } }

    fun setUpdateChannel(channel: String) { viewModelScope.launch { settingsStore.setUpdateChannel(channel); _uiState.update { it.copy(updateChannel = channel) } } }
    fun setDefaultPage(page: Int) { viewModelScope.launch { settingsStore.setDefaultPage(page); _uiState.update { it.copy(defaultPage = page) } } }

    fun checkUpdate() {
        viewModelScope.launch {
            try {
                val channel = _uiState.value.updateChannel
                val currentVerName = try {
                    appContext.packageManager.getPackageInfo(appContext.packageName, 0).versionName ?: "2.4.0"
                } catch (_: Exception) { "2.4.0" }

                if (channel == "release") {
                    // 稳定版：查询 GitHub Releases 最新版
                    val url = "https://api.github.com/repos/qihe114514/qihe-douyin/releases/latest"
                    val request = Request.Builder().url(url).get()
                        .addHeader("Accept", "application/vnd.github.v3+json")
                        .addHeader("User-Agent", "DouyinDownloader")
                        .build()
                    val response = updateClient.newCall(request).execute()
                    val body = response.body?.string() ?: ""
                    if (!response.isSuccessful) {
                        _uiState.update { it.copy(latestVersion = "更新检查失败: HTTP ${response.code}") }
                        return@launch
                    }
                    val json = Json.parseToJsonElement(body)
                    val tag = json.jsonObject["tag_name"]?.jsonPrimitive?.content ?: "未知"
                    val releaseUrl = json.jsonObject["html_url"]?.jsonPrimitive?.content ?: ""
                    val bodyText = json.jsonObject["body"]?.jsonPrimitive?.contentOrNull ?: ""

                    val remoteVer = tag.removePrefix("v")
                    val localVer = currentVerName.removePrefix("v")
                    if (compareVersions(remoteVer, localVer) > 0) {
                        _uiState.update { it.copy(latestVersion = "发现新版本: $tag (当前: v$currentVerName)\n$releaseUrl") }
                    } else {
                        _uiState.update { it.copy(latestVersion = "已是最新版本 (v$currentVerName)") }
                    }
                } else {
                    // Beta版：查询 GitHub Actions 最新成功构建
                    val url = "https://api.github.com/repos/qihe114514/qihe-douyin/actions/artifacts?per_page=5"
                    val request = Request.Builder().url(url).get()
                        .addHeader("Accept", "application/vnd.github.v3+json")
                        .addHeader("User-Agent", "DouyinDownloader")
                        .build()
                    val response = updateClient.newCall(request).execute()
                    val body = response.body?.string() ?: ""
                    if (!response.isSuccessful) {
                        _uiState.update { it.copy(latestVersion = "Beta检查失败: HTTP ${response.code}") }
                        return@launch
                    }
                    val json = Json.parseToJsonElement(body)
                    val artifacts = json.jsonObject["artifacts"]?.jsonArray
                    if (artifacts.isNullOrEmpty()) {
                        _uiState.update { it.copy(latestVersion = "暂无Beta构建产物") }
                        return@launch
                    }
                    // 找到最新的 APK 构建产物
                    val latestArtifact = artifacts.firstOrNull { art ->
                        art.jsonObject["name"]?.jsonPrimitive?.content?.contains("app-release", ignoreCase = true) == true
                    } ?: artifacts.first()
                    val artifactName = latestArtifact.jsonObject["name"]?.jsonPrimitive?.content ?: "未知"
                    val createdAt = latestArtifact.jsonObject["created_at"]?.jsonPrimitive?.content ?: ""
                    val archiveUrl = latestArtifact.jsonObject["archive_download_url"]?.jsonPrimitive?.content ?: ""

                    // 从 artifact name 提取版本信息
                    val buildVer = "build-$artifactName-$createdAt".take(40)
                    _uiState.update { it.copy(latestVersion = "最新Beta构建: $artifactName\n创建时间: ${createdAt.take(10)}\n$archiveUrl") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(latestVersion = "检查失败: ${e.message}") }
            }
        }
    }

    // 版本号比较工具 例如 "2.4.0" > "2.3.0"
    private fun compareVersions(v1: String, v2: String): Int {
        val parts1 = v1.split(".").map { it.toIntOrNull() ?: 0 }
        val parts2 = v2.split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(parts1.size, parts2.size)) {
            val p1 = parts1.getOrElse(i) { 0 }
            val p2 = parts2.getOrElse(i) { 0 }
            if (p1 != p2) return p1 - p2
        }
        return 0
    }
}
