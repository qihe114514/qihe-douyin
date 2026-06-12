package com.douyin.downloader.download

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import androidx.documentfile.provider.DocumentFile
import com.douyin.downloader.model.DownloadItem
import com.douyin.downloader.model.DownloadType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class DownloadManager(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val CHANNEL_ID = "download_channel"
    }

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

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("下载失败: HTTP ${response.code}"))
            }

            val body = response.body ?: return@withContext Result.failure(Exception("响应体为空"))

            val extension = when (item.type) {
                DownloadType.VIDEO, DownloadType.LIVE_PHOTO -> ".mp4"
                DownloadType.IMAGE -> ".jpg"
            }

            val safeTitle = item.title.replace(Regex("[/\\\\:*?\"<>|]"), "_").take(80)
            val fileName = "${safeTitle}$extension"

            // 读取数据
            val bytes = body.bytes()
            onProgress(1f, formatFileSize(bytes.size.toLong()))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveToMediaStore(bytes, fileName, item.type)
            } else {
                val dir = if (!savePath.isNullOrEmpty() && File(savePath).exists()) {
                    File(savePath)
                } else {
                    val defaultDir = File(context.getExternalFilesDir(null), "DouyinDownloads")
                    defaultDir.mkdirs()
                    defaultDir
                }
                val file = File(dir, fileName)
                FileOutputStream(file).use { fos ->
                    fos.write(bytes)
                    fos.flush()
                }
                MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), null) { _, _ -> }
                Result.success(file.absolutePath)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun saveToMediaStore(
        data: ByteArray,
        fileName: String,
        type: DownloadType
    ): Result<String> {
        return try {
            val mimeType = when (type) {
                DownloadType.VIDEO, DownloadType.LIVE_PHOTO -> "video/mp4"
                DownloadType.IMAGE -> "image/jpeg"
            }

            val contentValues = android.content.ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(
                    when (type) {
                        DownloadType.IMAGE -> MediaStore.MediaColumns.RELATIVE_PATH
                        else -> MediaStore.Video.Media.RELATIVE_PATH
                    },
                    when (type) {
                        DownloadType.IMAGE -> "Pictures/Douyin"
                        else -> "Movies/Douyin"
                    }
                )
            }

            val uri = when (type) {
                DownloadType.IMAGE -> context.contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                )
                else -> context.contentResolver.insert(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                )
            } ?: throw Exception("无法创建MediaStore条目")

            context.contentResolver.openOutputStream(uri)?.use { os ->
                os.write(data)
                os.flush()
            } ?: throw Exception("无法写入文件")

            Result.success(uri.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${"%.1f".format(bytes / (1024.0 * 1024))} MB"
            else -> "${"%.2f".format(bytes / (1024.0 * 1024 * 1024))} GB"
        }
    }
}