package com.douyin.downloader.ui.screens

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.size.Size
import com.douyin.downloader.model.*
import com.douyin.downloader.ui.DownloadStatus
import com.douyin.downloader.ui.MainUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    uiState: MainUiState,
    onUrlChange: (String) -> Unit,
    onParseClick: () -> Unit,
    onDownloadClick: (Int, DownloadItem) -> Unit,
    onSettingsClick: () -> Unit,
    onHistoryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        WallpaperBackground(uiState)

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background.copy(alpha = uiState.bgOpacity * 0.55f)
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("抖音视频下载", fontWeight = FontWeight.Bold) },
                        actions = {
                            IconButton(onClick = onHistoryClick) {
                                Icon(Icons.Default.AutoAwesome, "历史记录")
                            }
                            IconButton(onClick = onSettingsClick) {
                                Icon(Icons.Default.Tune, "设置")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                        )
                    )
                },
                containerColor = Color.Transparent
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 24.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(32.dp))

                    Icon(
                        Icons.Default.WaterDrop,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        modifier = Modifier.size(48.dp)
                    )

                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "抖音无水印解析",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "粘贴抖音分享链接，一键下载无水印视频",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )

                    Spacer(Modifier.height(28.dp))

                    GlassUrlInputField(
                        url = uiState.shareUrl,
                        onUrlChange = onUrlChange,
                        onParseClick = onParseClick
                    )

                    Spacer(Modifier.height(14.dp))

                    Button(
                        onClick = onParseClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(18.dp),
                        enabled = !uiState.isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                        )
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("解析中...")
                        } else {
                            Icon(Icons.Default.Radar, "解析")
                            Spacer(Modifier.width(8.dp))
                            Text("解析视频", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    if (uiState.error != null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Report, "错误", tint = MaterialTheme.colorScheme.onErrorContainer)
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = uiState.error ?: "",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    if (uiState.downloadItems.isNotEmpty()) {
                        Spacer(Modifier.height(24.dp))

                        uiState.parsedData?.let { data -> GlassVideoInfoCard(data) }

                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "可下载内容 (${uiState.downloadItems.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )

                        uiState.downloadItems.forEachIndexed { index, item ->
                            GlassDownloadItemCard(
                                index = index,
                                item = item,
                                status = uiState.downloadStatus[index],
                                progress = uiState.downloadProgress[index] ?: 0f,
                                speed = uiState.downloadSpeed[index] ?: "",
                                onDownload = { onDownloadClick(index, item) }
                            )
                            Spacer(Modifier.height(10.dp))
                        }
                    }

                    Spacer(Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
private fun GlassUrlInputField(
    url: String,
    onUrlChange: (String) -> Unit,
    onParseClick: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val focusManager = LocalFocusManager.current

    OutlinedTextField(
        value = url,
        onValueChange = onUrlChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("请输入抖音视频分享链接") },
        leadingIcon = { Icon(Icons.Default.Link, "链接") },
        trailingIcon = {
            if (url.isNotEmpty()) {
                IconButton(onClick = { onUrlChange("") }) {
                    Icon(Icons.Default.Backspace, "清除")
                }
            } else {
                IconButton(onClick = {
                    clipboardManager.getText()?.let { clipText ->
                        onUrlChange(clipText.text)
                    }
                }) {
                    Icon(Icons.Default.ContentPaste, "粘贴")
                }
            }
        },
        shape = RoundedCornerShape(18.dp),
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Uri,
            imeAction = ImeAction.Go
        ),
        keyboardActions = KeyboardActions(
            onGo = {
                focusManager.clearFocus()
                onParseClick()
            }
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
    )
}

@Composable
private fun WallpaperBackground(uiState: MainUiState) {
    val context = LocalContext.current
    val type = uiState.bgWallpaperType
    val blurRadius = if (uiState.bgBlurRadius > 0) (2.dp * uiState.bgBlurRadius) else 0.dp

    when (type) {
        "image" -> {
            if (uiState.bgWallpaperUri.isNotBlank()) {
                val uri = remember(uiState.bgWallpaperUri) { Uri.parse(uiState.bgWallpaperUri) }
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(uri)
                        .size(Size.ORIGINAL)
                        .build(),
                    contentDescription = "背景壁纸",
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (blurRadius > 0.dp) Modifier.blur(blurRadius) else Modifier),
                    contentScale = ContentScale.Crop
                )
            }
        }
        "video" -> {
            if (uiState.bgWallpaperUri.isNotBlank()) {
                val uri = remember(uiState.bgWallpaperUri) { Uri.parse(uiState.bgWallpaperUri) }
                VideoBackground(
                    uri = uri,
                    blurRadiusDp = blurRadius,
                    soundEnabled = uiState.videoSoundEnabled
                )
            }
        }
    }
}

@Composable
private fun VideoBackground(
    uri: Uri,
    blurRadiusDp: androidx.compose.ui.unit.Dp,
    soundEnabled: Boolean
) {
    val context = LocalContext.current
    val player = remember(uri, soundEnabled) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            repeatMode = Player.REPEAT_MODE_ALL
            volume = if (soundEnabled) 0.4f else 0f
            playWhenReady = true
            prepare()
        }
    }

    LaunchedEffect(soundEnabled) {
        player.volume = if (soundEnabled) 0.4f else 0f
    }

    DisposableEffect(uri) {
        onDispose { player.release() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(if (blurRadiusDp > 0.dp) Modifier.blur(blurRadiusDp) else Modifier)
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = false
                    resizeMode = 1
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun GlassVideoInfoCard(data: VideoData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (data.type) {
                        "video" -> "📹 视频"
                        "image" -> "🖼️ 图集"
                        "live" -> "📸 实况"
                        else -> "🎬 ${data.type}"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            data.author?.let { author ->
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = author.avatar,
                        contentDescription = null,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(14.dp))
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "@${author.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (data.title.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = data.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun GlassDownloadItemCard(
    index: Int,
    item: DownloadItem,
    status: DownloadStatus?,
    progress: Float,
    speed: String,
    onDownload: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = when (item.type) {
                        DownloadType.VIDEO, DownloadType.LIVE_PHOTO -> Icons.Default.Movie
                        DownloadType.IMAGE -> Icons.Default.Collections
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when (item.type) {
                            DownloadType.VIDEO -> "🎬 视频"
                            DownloadType.LIVE_PHOTO -> "📸 实况"
                            DownloadType.IMAGE -> "🖼️ 图片"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                    Text(
                        text = item.displayInfo.ifBlank {
                            when (item.type) {
                                DownloadType.VIDEO, DownloadType.LIVE_PHOTO -> "视频文件"
                                DownloadType.IMAGE -> "图片文件"
                            }
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (status is DownloadStatus.Downloading && speed.isNotBlank()) {
                        Text(
                            text = speed,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Spacer(Modifier.width(10.dp))

            when (val s = status) {
                is DownloadStatus.Downloading -> {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(44.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.size(38.dp),
                            strokeWidth = 3.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (progress >= 0f) {
                            Text(
                                text = "${(progress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                is DownloadStatus.Success -> {
                    Icon(
                        Icons.Default.CheckCircle,
                        "下载完成",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(28.dp)
                    )
                }
                is DownloadStatus.Error -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Warning,
                            "下载失败",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        FilledTonalButton(onClick = onDownload) {
                            Text("重试", fontSize = 12.sp)
                        }
                    }
                }
                else -> {
                    Button(
                        onClick = onDownload,
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Download, "下载", modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("下载", fontSize = 14.sp)
                    }
                }
            }
        }
    }
}