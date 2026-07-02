package com.douyin.downloaderqh.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.douyin.downloaderqh.ui.MainUiState
import com.douyin.downloaderqh.ui.components.GlassCard
import androidx.compose.ui.platform.LocalContext

enum class SettingsPage { MAIN, DOWNLOAD, WALLPAPER, PAGE_SETTINGS, ABOUT, TAB_ORDER }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    savePath: String,
    bgWallpaperUri: String,
    bgWallpaperType: String,
    bgBlurRadius: Float,
    bgOpacity: Float,
    videoSoundEnabled: Boolean,
    defaultPage: Int,
    updateChannel: String,
    tabOrder: List<Int>,
    onSetSavePath: (Uri) -> Unit,
    onSetBgWallpaper: (Uri, String) -> Unit,
    onSetBgBlurRadius: (Float) -> Unit,
    onSetBgOpacity: (Float) -> Unit,
    onSetVideoSoundEnabled: (Boolean) -> Unit,
    onClearBgWallpaper: () -> Unit,
    onSetDefaultPage: (Int) -> Unit,
    onUpdateClick: () -> Unit,
    onUpdateChannelChange: (String) -> Unit,
    onSetTabOrder: (List<Int>) -> Unit,
    onBack: () -> Unit
) {
    var currentPage by remember { mutableStateOf(SettingsPage.MAIN) }

    BackHandler(enabled = currentPage != SettingsPage.MAIN) {
        currentPage = SettingsPage.MAIN
    }

    when (currentPage) {
        SettingsPage.MAIN -> MainSettingsPage(onBack = onBack, onNavigate = { currentPage = it })
        SettingsPage.DOWNLOAD -> DownloadSettingsSubPage(savePath = savePath, onSetSavePath = onSetSavePath, onBack = { currentPage = SettingsPage.MAIN })
        SettingsPage.WALLPAPER -> WallpaperSubPage(
            bgWallpaperUri = bgWallpaperUri, bgWallpaperType = bgWallpaperType,
            bgBlurRadius = bgBlurRadius, bgOpacity = bgOpacity, videoSoundEnabled = videoSoundEnabled,
            onSetBgWallpaper = onSetBgWallpaper, onSetBgBlurRadius = onSetBgBlurRadius,
            onSetBgOpacity = onSetBgOpacity, onSetVideoSoundEnabled = onSetVideoSoundEnabled,
            onClearBgWallpaper = onClearBgWallpaper,
            onBack = { currentPage = SettingsPage.MAIN }
        )
        SettingsPage.PAGE_SETTINGS -> PageSettingsSubPage(
            defaultPage = defaultPage, onSetDefaultPage = onSetDefaultPage,
            onBack = { currentPage = SettingsPage.MAIN },
            onTabOrder = { currentPage = SettingsPage.TAB_ORDER }
        )
        SettingsPage.ABOUT -> AboutSubPage(updateChannel = updateChannel, onUpdateClick = onUpdateClick, onUpdateChannelChange = onUpdateChannelChange, onBack = { currentPage = SettingsPage.MAIN })
        SettingsPage.TAB_ORDER -> TabOrderSubPage(tabOrder = tabOrder, onSaveTabOrder = onSetTabOrder, onBack = { currentPage = SettingsPage.PAGE_SETTINGS })
    }

}

// ==================== 主设置页 ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainSettingsPage(onBack: () -> Unit, onNavigate: (SettingsPage) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 20.dp).verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(16.dp))
            SettingsEntryCard(title = "下载设置", subtitle = "设置文件保存路径", icon = Icons.Default.Folder, onClick = { onNavigate(SettingsPage.DOWNLOAD) })
            Spacer(Modifier.height(8.dp))
            SettingsEntryCard(title = "背景壁纸", subtitle = "设置应用背景图片或视频", icon = Icons.Default.Image, onClick = { onNavigate(SettingsPage.WALLPAPER) })
            Spacer(Modifier.height(8.dp))
            SettingsEntryCard(title = "页面设置", subtitle = "默认打开页面与底栏排序", icon = Icons.Default.WebStories, onClick = { onNavigate(SettingsPage.PAGE_SETTINGS) })
            Spacer(Modifier.height(8.dp))
            SettingsEntryCard(title = "关于", subtitle = "版本信息与检查更新", icon = Icons.Default.Info, onClick = { onNavigate(SettingsPage.ABOUT) })
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
fun SettingsEntryCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    GlassCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
        }
    }
}

// ==================== 下载设置子页 ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadSettingsSubPage(savePath: String, onSetSavePath: (Uri) -> Unit, onBack: () -> Unit) {
    val savePathLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri -> uri?.let { onSetSavePath(it) } }
    SubPageScaffold(title = "下载设置", onBack = onBack) {
        GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text("保存路径", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        Text(if (savePath.isNotBlank()) "自定义路径" else "默认路径 (Movies/Douyin)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    FilledTonalButton(onClick = { savePathLauncher.launch(null) }) { Text("选择") }
                }
            }
        }
    }
}

// ==================== 背景壁纸子页 ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallpaperSubPage(
    bgWallpaperUri: String, bgWallpaperType: String,
    bgBlurRadius: Float, bgOpacity: Float, videoSoundEnabled: Boolean,
    onSetBgWallpaper: (Uri, String) -> Unit, onSetBgBlurRadius: (Float) -> Unit,
    onSetBgOpacity: (Float) -> Unit, onSetVideoSoundEnabled: (Boolean) -> Unit,
    onClearBgWallpaper: () -> Unit, onBack: () -> Unit
) {
    val bgImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let { onSetBgWallpaper(it, "image") } }
    val bgVideoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let { onSetBgWallpaper(it, "video") } }
    SubPageScaffold(title = "背景壁纸", onBack = onBack) {
        GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = { bgImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, modifier = Modifier.weight(1f)) { Text("选择图片") }
                    FilledTonalButton(onClick = { bgVideoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)) }, modifier = Modifier.weight(1f)) { Text("选择视频") }
                }
                if (bgWallpaperType != "none") {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = onClearBgWallpaper, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("清除壁纸") }
                }
                if (bgWallpaperType == "video") {
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("背景声音", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = videoSoundEnabled, onCheckedChange = onSetVideoSoundEnabled)
                    }
                }
                if (bgWallpaperType != "none") {
                    Spacer(Modifier.height(12.dp))
                    Divider()
                    Spacer(Modifier.height(12.dp))
                    Text("模糊度", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Slider(value = bgBlurRadius, onValueChange = onSetBgBlurRadius, valueRange = 0f..10f, modifier = Modifier.weight(1f))
                        Text("${bgBlurRadius.toInt()}", style = MaterialTheme.typography.labelSmall)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("背景透明度", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Slider(value = bgOpacity, onValueChange = onSetBgOpacity, valueRange = 0f..1f, modifier = Modifier.weight(1f))
                        Text("${(bgOpacity * 100).toInt()}%", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

// ==================== 页面设置子页 ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageSettingsSubPage(defaultPage: Int, onSetDefaultPage: (Int) -> Unit, onBack: () -> Unit, onTabOrder: () -> Unit) {
    SubPageScaffold(title = "页面设置", onBack = onBack) {
        GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("默认打开页面", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                listOf("首页", "抖音", "小红书").forEachIndexed { i, name ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = defaultPage == i, onClick = { onSetDefaultPage(i) })
                        Spacer(Modifier.width(8.dp))
                        Text(name)
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        GlassCard(onClick = onTabOrder, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("底栏排序", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    Text("拖拽调整底部标签顺序", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            }
        }
    }
}

// ==================== 关于子页 ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutSubPage(updateChannel: String, onUpdateClick: () -> Unit, onUpdateChannelChange: (String) -> Unit, onBack: () -> Unit) {
    SubPageScaffold(title = "关于", onBack = onBack) {
        GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("万能下载器 v2.4.0", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                // 更新通道选择 + 检查更新按钮
                UpdateChannelSection(updateChannel = updateChannel, onUpdateClick = onUpdateClick, onUpdateChannelChange = onUpdateChannelChange)
                Spacer(Modifier.height(12.dp))
                Divider()
                Spacer(Modifier.height(12.dp))
                Text("开发者", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("其核 (@qihe114514)", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Divider()
                Spacer(Modifier.height(12.dp))
                Text("社交主页", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                // B站/抖音 并排小按钮
                val context = LocalContext.current
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilledTonalButton(
                        onClick = {
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://m.bilibili.com/space/1049283248"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("B站", fontSize = 13.sp)
                    }
                    FilledTonalButton(
                        onClick = {
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://www.douyin.com/user/MS4wLjABAAAAuUtKOArTFKTBm4C6o5MwDQuGMNZ9-0CWZfUay6U9wUI"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.MusicNote, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("抖音", fontSize = 13.sp)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Divider()
                Spacer(Modifier.height(12.dp))
                Text("API", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("api.bugpk.com", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

// ==================== 底栏排序子页 ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabOrderSubPage(tabOrder: List<Int>, onSaveTabOrder: (List<Int>) -> Unit, onBack: () -> Unit) {
    var currentOrder by remember(tabOrder) { mutableStateOf(tabOrder.toMutableList()) }
    val tabNames = listOf("首页", "抖音", "小红书")

    SubPageScaffold(title = "底栏排序", onBack = onBack) {
        Column {
            Text("调整底部导航栏顺序", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("点击上下箭头移动标签位置", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    currentOrder.forEachIndexed { i, tabIdx ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DragHandle, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(tabNames[tabIdx], style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            // 上移按钮
                            IconButton(
                                onClick = {
                                    if (i > 0) {
                                        val newOrder = currentOrder.toMutableList()
                                        val tmp = newOrder[i]
                                        newOrder[i] = newOrder[i - 1]
                                        newOrder[i - 1] = tmp
                                        currentOrder = newOrder
                                    }
                                },
                                enabled = i > 0,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.KeyboardArrowUp, "上移", modifier = Modifier.size(20.dp), tint = if (i > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                            }
                            // 下移按钮
                            IconButton(
                                onClick = {
                                    if (i < currentOrder.size - 1) {
                                        val newOrder = currentOrder.toMutableList()
                                        val tmp = newOrder[i]
                                        newOrder[i] = newOrder[i + 1]
                                        newOrder[i + 1] = tmp
                                        currentOrder = newOrder
                                    }
                                },
                                enabled = i < currentOrder.size - 1,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.KeyboardArrowDown, "下移", modifier = Modifier.size(20.dp), tint = if (i < currentOrder.size - 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                            }
                        }
                        if (i < currentOrder.size - 1) Divider()
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { onSaveTabOrder(currentOrder.toList()); onBack() },
                modifier = Modifier.fillMaxWidth()
            ) { Text("保存排序") }
        }
    }
}


// ==================== 更新通道选择组件 ====================
@Composable
fun UpdateChannelSection(updateChannel: String, onUpdateClick: () -> Unit, onUpdateChannelChange: (String) -> Unit) {
    Column {
        GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("更新通道", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                // RadioButton 选择
                Row(Modifier.fillMaxWidth().clickable { onUpdateChannelChange("release") }, verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = updateChannel == "release", onClick = { onUpdateChannelChange("release") })
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("稳定版", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text("检测 GitHub Release 最新版本", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Row(Modifier.fillMaxWidth().clickable { onUpdateChannelChange("beta") }, verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = updateChannel == "beta", onClick = { onUpdateChannelChange("beta") })
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("Beta版", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text("检测 GitHub Actions 最新构建版本", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Button(onClick = onUpdateClick, modifier = Modifier.fillMaxWidth()) { Text("检查更新") }
            }
        }
    }
}

// ==================== 子页面通用 Scaffold ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubPageScaffold(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 20.dp).padding(top = 16.dp)
        ) {
            content()
        }
    }
}
