package com.douyin.downloaderqh.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    onSetSavePath: (Uri) -> Unit,
    onSetBgWallpaper: (Uri, String) -> Unit,
    onSetBgBlurRadius: (Float) -> Unit,
    onSetBgOpacity: (Float) -> Unit,
    onSetVideoSoundEnabled: (Boolean) -> Unit,
    onClearBgWallpaper: () -> Unit,
    onSetDefaultPage: (Int) -> Unit,
    onUpdateClick: () -> Unit,
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()
    val savePathLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri -> uri?.let { onSetSavePath(it) } }
    val bgImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let { onSetBgWallpaper(it, "image") } }
    val bgVideoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let { onSetBgWallpaper(it, "video") } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 20.dp).verticalScroll(scrollState)
        ) {
            Spacer(Modifier.height(16.dp))

            Text("下载设置", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 8.dp))
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
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

            Spacer(Modifier.height(24.dp))

            Text("背景壁纸", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 8.dp))
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
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

            Spacer(Modifier.height(24.dp))

            Text("页面设置", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 8.dp))
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
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
                    Spacer(Modifier.height(16.dp))
                    Text("底栏排序 (拖拽排序)", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(4.dp))
                    Text("拖拽调整底部标签顺序", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(24.dp))

            Text("关于", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 8.dp))
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("万能下载器 v2.2.3", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = onUpdateClick, modifier = Modifier.fillMaxWidth()) { Text("检查更新") }
                    Spacer(Modifier.height(8.dp))
                    Text("开发者: 其核 (@qihe114514)", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(4.dp))
                    Text("API: api.bugpk.com", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}
