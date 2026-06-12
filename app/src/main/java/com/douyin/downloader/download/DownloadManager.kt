package com.douyin.downloader.download

import android.app.DownloadManager as SysDownloadManager
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.douyin.downloader.model.DownloadItem
import com.douyin.downloader.model.DownloadType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.io.FileInputStream

private data class DownloadProgress(
    val status: Int?,
    val bytes: Long,
    val total: Long,
    val localUri: String?
)

class DownloadManager(private val context: Context) {

    private val sysDownloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as SysDownloadManager

    /**
     * 通过系统 DownloadManager 下载并拷贝到相册。
     * 返回 Flow：每 500ms emit (进度 0..1, 速度字符串)；完成后 emit null 并结束。
     */
    fun downloadWithProgress(
        item: DownloadItem,
        savePath: String?
    ): Flow<Pair<Float, String>?> = flow {
        val extension = when (item.type) {
            DownloadType.VIDEO, DownloadType.LIVE_PHOTO -> ".mp4"
            DownloadType.IMAGE -> ".jpg"
        }
        val safeTitle = item.title.replace(Regex("[/\\\\:*?\"<>|]"), "_").take(80)
        val fileName = "${safeTitle}$extension"

        val request = SysDownloadManager.Request(Uri.parse(item.url))
            .setTitle("下载中…")
            .setNotificationVisibility(SysDownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "Douyin/$fileName")
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadId = sysDownloadManager.enqueue(request)
        var finished = false
        var lastBytes = 0L
        var lastTime = System.currentTimeMillis()

        try {
            while (!finished) {
                delay(500)
                val progress = withContext(Dispatchers.IO) {
                    queryProgress(downloadId)
                }

                when (progress.status) {
                    SysDownloadManager.STATUS_SUCCESSFUL -> {
                        finished = true
                        // 拷贝到相册
                        withContext(Dispatchers.IO) {
                            moveToGallery(downloadId, progress.localUri, fileName, item.type, savePath)
                        }
                        emit(null)  // 完成信号
                        return@flow
                    }
                    SysDownloadManager.STATUS_FAILED -> {
                        finished = true
                        sysDownloadManager.remove(downloadId)
                        throw RuntimeException("系统下载失败")
                    }
                }

                if (!finished && progress.total > 0) {
                    val fraction = (progress.bytes.toFloat() / progress.total).coerceIn(0f, 1f)
                    val now = System.currentTimeMillis()
                    val elapsed = now - lastTime
                    val speedStr = if (elapsed > 0) {
                        val bytesDelta = progress.bytes - lastBytes
                        val bytesPerSecond = (bytesDelta * 1000L / elapsed)
                        formatSpeed(bytesPerSecond)
                    } else "—"
                    lastBytes = progress.bytes
                    lastTime = now
                    emit(fraction to speedStr)
                }
            }
        } catch (e: Exception) {
            if (!finished) {
                sysDownloadManager.remove(downloadId)
            }
            throw e
        }
    }

    private fun queryProgress(downloadId: Long): DownloadProgress {
        val query = SysDownloadManager.Query().setFilterById(downloadId)
        var cursor: Cursor? = null
        return try {
            cursor = sysDownloadManager.query(query)
            if (cursor?.moveToFirst() == true) {
                val statusCol = cursor.getColumnIndexOrThrow(SysDownloadManager.COLUMN_STATUS)
                val bytesCol = cursor.getColumnIndexOrThrow(SysDownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                val totalCol = cursor.getColumnIndexOrThrow(SysDownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                val localUriCol = cursor.getColumnIndexOrThrow(SysDownloadManager.COLUMN_LOCAL_URI)
                DownloadProgress(
                    status = cursor.getInt(statusCol),
                    bytes = cursor.getLong(bytesCol),
                    total = cursor.getLong(totalCol),
                    localUri = cursor.getString(localUriCol)
                )
            } else {
                DownloadProgress(null, 0L, 0L, null)
            }
        } finally {
            cursor?.close()
        }
    }

    private fun moveToGallery(
        downloadId: Long,
        localUri: String?,
        fileName: String,
        type: DownloadType,
        savePath: String?
    ): String? {
        try {
            val filePath = localUri ?: return null
            val file = File(Uri.parse(filePath).path ?: return null)
            if (!file.exists()) return null

            val mime = when (type) {
                DownloadType.VIDEO, DownloadType.LIVE_PHOTO -> "video/mp4"
                DownloadType.IMAGE -> "image/jpeg"
            }
            val cv = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mime)
                if (type == DownloadType.IMAGE) put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/Douyin")
                else put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/Douyin")
            }
            val collection = if (type == DownloadType.IMAGE) MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            else MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            val uri = context.contentResolver.insert(collection, cv) ?: return null
            context.contentResolver.openOutputStream(uri)?.use { out ->
                val buf = ByteArray(8192)
                FileInputStream(file).use { fis ->
                    var read: Int
                    while (fis.read(buf).also { read = it } != -1) {
                        out.write(buf, 0, read)
                    }
                }
            }
            file.delete()
            sysDownloadManager.remove(downloadId)
            return uri.toString()
        } catch (_: Exception) {
            sysDownloadManager.remove(downloadId)
            return null
        }
    }

    private fun formatSpeed(bytesPerSecond: Long): String = when {
        bytesPerSecond < 1024 -> "$bytesPerSecond B/s"
        bytesPerSecond < 1024 * 1024 -> "${"%.1f".format(bytesPerSecond / 1024.0)} KB/s"
        bytesPerSecond < 1024 * 1024 * 1024 -> "${"%.1f".format(bytesPerSecond / (1024.0 * 1024))} MB/s"
        else -> "${"%.2f".format(bytesPerSecond / (1024.0 * 1024 * 1024))} GB/s"
    }
}