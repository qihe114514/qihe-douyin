package com.douyin.downloader.download

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.douyin.downloader.model.DownloadItem
import com.douyin.downloader.model.DownloadType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.buffer
import okio.sink
import java.io.File
import java.io.OutputStream
import java.util.concurrent.TimeUnit

class DownloadManager(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    suspend fun downloadToGalleryWithProgress(
        item: DownloadItem,
        savePath: String?,
        onProgress: (progress: Float, speed: String) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(item.url)
                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 16; Pixel) AppleWebKit/537.36")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext Result.failure(Exception("HTTP ${response.code}"))
            val body = response.body ?: return@withContext Result.failure(Exception("响应体为空"))
            val contentLength = body.contentLength().coerceAtLeast(0L)

            val mimeType = when (item.type) {
                DownloadType.VIDEO, DownloadType.LIVE_PHOTO -> "video/mp4"
                DownloadType.IMAGE -> "image/jpeg"
            }
            val extension = when (item.type) {
                DownloadType.VIDEO, DownloadType.LIVE_PHOTO -> ".mp4"
                DownloadType.IMAGE -> ".jpg"
            }
            val safeTitle = item.title.replace(Regex("[/\\\\:*?\"<>|]"), "_").take(80)
            val fileName = "${safeTitle}$extension"

            // 打开目标输出流
            val outStream: OutputStream
            val resultUri: Uri?

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val cv = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(
                        if (item.type == DownloadType.IMAGE) MediaStore.MediaColumns.RELATIVE_PATH
                        else MediaStore.Video.Media.RELATIVE_PATH,
                        if (item.type == DownloadType.IMAGE) "Pictures/Douyin" else "Movies/Douyin"
                    )
                }
                val collection = if (item.type == DownloadType.IMAGE) MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                                else MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                resultUri = context.contentResolver.insert(collection, cv)
                    ?: throw Exception("无法创建媒体条目")
                outStream = context.contentResolver.openOutputStream(resultUri)
                    ?: throw Exception("无法打开输出流")
            } else {
                resultUri = null
                val dir = if (!savePath.isNullOrEmpty() && File(savePath).exists()) File(savePath)
                          else File(context.getExternalFilesDir(null), "DouyinDownloads").also { it.mkdirs() }
                val file = File(dir, fileName)
                outStream = file.outputStream()
            }

            // 流式下载并直接写入
            var totalRead = 0L
            var lastReportTs = System.currentTimeMillis()
            var lastReportBytes = 0L
            val buffer = okio.Buffer()

            try {
                outStream.buffered().use { os ->
                    val source = body.source()
                    while (!source.exhausted()) {
                        val chunk = source.read(buffer, 8192)
                        if (chunk == -1L) break
                        buffer.readAll(os.sink().buffer())
                        totalRead += chunk

                        val now = System.currentTimeMillis()
                        if (now - lastReportTs >= 500 || totalRead == contentLength) {
                            val dt = (now - lastReportTs).coerceAtLeast(1L)
                            val bytesPerSec = ((totalRead - lastReportBytes).toDouble() / dt) * 1000.0
                            val speed = formatSpeed(bytesPerSec)
                            val progress = if (contentLength > 0) (totalRead.toFloat() / contentLength).coerceIn(0f, 1f) else -1f
                            onProgress(progress, speed)
                            lastReportTs = now
                            lastReportBytes = totalRead
                        }
                    }
                }

                // 最终通知
                onProgress(1f, formatSize(totalRead))
                Result.success(resultUri?.toString() ?: File(File(context.getExternalFilesDir(null), "DouyinDownloads"), fileName).absolutePath)
            } finally {
                outStream.close()
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun formatSize(bytes: Long) = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${"%.1f".format(bytes / (1024.0 * 1024))} MB"
        else -> "${"%.2f".format(bytes / (1024.0 * 1024 * 1024))} GB"
    }

    private fun formatSpeed(bytesPerSec: Double) = when {
        bytesPerSec < 1024 -> "${bytesPerSec.toLong()} B/s"
        bytesPerSec < 1024 * 1024 -> "${"%.1f".format(bytesPerSec / 1024)} KB/s"
        bytesPerSec < 1024 * 1024 * 1024 -> "${"%.1f".format(bytesPerSec / (1024 * 1024))} MB/s"
        else -> "${"%.2f".format(bytesPerSec / (1024 * 1024 * 1024))} GB/s"
    }
}