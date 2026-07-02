package com.douyin.downloaderqh

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.douyin.downloaderqh.model.Platform
import com.douyin.downloaderqh.ui.MainViewModel
import com.douyin.downloaderqh.ui.MainUiState
import com.douyin.downloaderqh.ui.components.BottomTabDef
import com.douyin.downloaderqh.ui.components.GlassBottomBar
import com.douyin.downloaderqh.ui.components.GlassCard
import com.douyin.downloaderqh.ui.components.rememberGlassBackdrop
import com.douyin.downloaderqh.ui.screens.*
import com.douyin.downloaderqh.ui.theme.DouyinDownloaderTheme
import com.kyant.backdrop.backdrops.layerBackdrop

data class BottomTab(val label: String, val icon: ImageVector, val platform: Platform?)

private val BOTTOM_BAR_HEIGHT = 80.dp

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            DouyinDownloaderTheme {
                val uiState by viewModel.uiState.collectAsState()
                var showSettings by remember { mutableStateOf(false) }
                var showHistory by remember { mutableStateOf(false) }

                val allTabs = listOf(
                    BottomTab("首页", Icons.Default.Home, null),
                    BottomTab("抖音", Icons.Default.MusicNote, Platform.DOUYIN),
                    BottomTab("小红书", Icons.Default.Star, Platform.XIAOHONGSHU)
                )
                val tabs = uiState.tabOrder.map { allTabs[it] }

                // 预测性返回动画：系统返回手势与设置/历史页面联动
                BackHandler(enabled = showSettings || showHistory) {
                    showSettings = false
                    showHistory = false
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding()
                ) {
                    WallpaperBackground(uiState)

                    val backdrop = rememberGlassBackdrop(
                        backgroundColor = Color.Transparent
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .layerBackdrop(backdrop)
                    ) {
                        Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background.copy(
                            alpha = uiState.bgOpacity * 0.7f
                        )
                    ) {
                            if (showHistory) {
                                HistoryScreen(
                                    historyList = uiState.parseHistory,
                                    onItemClick = { entry ->
                                        viewModel.updateShareUrl(entry.url)
                                        viewModel.parseVideo()
                                        showHistory = false
                                    },
                                    onClearHistory = { viewModel.clearHistory() },
                                    onBack = { showHistory = false },
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(bottom = BOTTOM_BAR_HEIGHT)
                                )
                            } else if (showSettings) {
                                SettingsScreen(
                                    savePath = uiState.savePath,
                                    bgWallpaperUri = uiState.bgWallpaperUri,
                                    bgWallpaperType = uiState.bgWallpaperType,
                                    bgBlurRadius = uiState.bgBlurRadius,
                                    bgOpacity = uiState.bgOpacity,
                                    videoSoundEnabled = uiState.videoSoundEnabled,
                                    defaultPage = uiState.defaultPage,
                                    updateChannel = uiState.updateChannel,
                                    tabOrder = uiState.tabOrder,
                                    onSetSavePath = { viewModel.setSavePath(it) },
                                    onSetBgWallpaper = { uri, type -> viewModel.setBgWallpaper(uri, type) },
                                    onSetBgBlurRadius = { viewModel.setBgBlurRadius(it) },
                                    onSetBgOpacity = { viewModel.setBgOpacity(it) },
                                    onSetVideoSoundEnabled = { viewModel.setVideoSoundEnabled(it) },
                                    onClearBgWallpaper = { viewModel.clearBgWallpaper() },
                                    onSetDefaultPage = { viewModel.setDefaultPage(it) },
                                    onUpdateClick = { viewModel.checkUpdate() },
                                    onUpdateChannelChange = { viewModel.setUpdateChannel(it) },
                                    onSetTabOrder = { viewModel.setTabOrder(it) },
                                    onBack = { showSettings = false }
                                )
                            } else {
                                AnimatedContent(
                                    targetState = uiState.currentTab,
                                    modifier = Modifier.fillMaxSize(),
                                    transitionSpec = { fadeIn() togetherWith fadeOut() }
                                ) { tabIndex ->
                                    when (tabIndex) {
                                        0 -> HomePage(
                                            onNavigateDouyin = { viewModel.selectTab(1, Platform.DOUYIN) },
                                            onNavigateXiaohongshu = { viewModel.selectTab(2, Platform.XIAOHONGSHU) },
                                            uiState = uiState,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        1 -> MainScreen(
                                            backdrop = backdrop,
                                            platform = Platform.DOUYIN,
                                            uiState = uiState,
                                            onUrlChange = { viewModel.updateShareUrl(it) },
                                            onParseClick = { viewModel.parseVideo() },
                                            onDownloadClick = { index, item -> viewModel.downloadItem(index, item) },
                                            onSettingsClick = { showSettings = true },
                                            onHistoryClick = { showHistory = true },
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        2 -> MainScreen(
                                            backdrop = backdrop,
                                            platform = Platform.XIAOHONGSHU,
                                            uiState = uiState,
                                            onUrlChange = { viewModel.updateShareUrl(it) },
                                            onParseClick = { viewModel.parseVideo() },
                                            onDownloadClick = { index, item -> viewModel.downloadItem(index, item) },
                                            onSettingsClick = { showSettings = true },
                                            onHistoryClick = { showHistory = true },
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Glass bottom bar overlaying on top
                    GlassBottomBar(
                        backdrop = backdrop,
                        tabs = tabs.map { tab ->
                            BottomTabDef(
                                label = tab.label,
                                icon = tab.icon
                            )
                        },
                        selectedIndex = uiState.currentTab,
                        onTabClick = { index ->
                            showSettings = false
                            showHistory = false
                            val tab = tabs[index]
                            if (tab.platform != null) viewModel.selectTab(index, tab.platform)
                            else viewModel.selectTab(index, Platform.DOUYIN)
                        },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }
    }
}

@Composable
fun HomePage(
    onNavigateDouyin: () -> Unit,
    onNavigateXiaohongshu: () -> Unit,
    uiState: MainUiState = MainUiState(),
    modifier: Modifier = Modifier
) {
    // HomePage no longer has its own WallpaperBackground — it's at the root level.
    // Just render the content cards.
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(bottom = BOTTOM_BAR_HEIGHT)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(20.dp))
        Text(
            "万能下载器",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(2.dp))
        Text(
            "支持抖音 & 小红书无水印解析下载",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(2.dp))
        Text(
            "by 其核",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )

        Spacer(Modifier.height(16.dp))

        GlassCard(onClick = onNavigateDouyin, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "🎵 抖音解析",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "粘贴链接，解析无水印视频/图集/实况",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        GlassCard(onClick = onNavigateXiaohongshu, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "📕 小红书解析",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "粘贴链接，解析无水印视频和图片",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}
