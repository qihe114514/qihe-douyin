package com.douyin.downloaderqh.download

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.douyin.downloaderqh.model.DownloadItem
import com.douyin.downloaderqh.model.DownloadType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

class DownloadManager(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(300, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    /**
     * 完全在 IO 线程流式下载，每 500ms emit (进度 0..1, 速度字符串)。
     * 完成后拷贝到系统相册，最后 emit null 表示结束。
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

        val tempDir = File(context.cacheDir, "downloads")
        if (!tempDir.exists()) tempDir.mkdirs()
        val tempFile = File(tempDir, fileName)

        // ========== 所有网络和文件操作在 Dispatchers.IO 执行 ==========
        withContext(Dispatchers.IO) {
            val request = Request.Builder().url(item.url).build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                throw RuntimeException("下载失败 HTTP ${response.code}")
            }

            val body = response.body ?: throw RuntimeException("空响应体")
            val contentLength = body.contentLength()
            val inputStream: InputStream = body.byteStream()

            FileOutputStream(tempFile).use { fos ->
                val buffer = ByteArray(8192)
                var totalBytesRead = 0L
                var lastBytes = 0L
                var lastTime = System.currentTimeMillis()

                while (true) {
                    if (Thread.currentThread().isInterrupted) throw CancellationException()
                    val read = inputStream.read(buffer)
                    if (read == -1) break
                    fos.write(buffer, 0, read)
                    totalBytesRead += read

                    val now = System.currentTimeMillis()
                    val elapsed = now - lastTime
                    if (elapsed >= 500) {
                        // ---------- 进度 emit（可在主线程安全更新 UI）----------
                        val fraction = if (contentLength > 0) {
                            (totalBytesRead.toFloat() / contentLength).coerceIn(0f, 1f)
                        } else -1f
                        val speedStr = if (elapsed > 0) {
                            val bytesDelta = totalBytesRead - lastBytes
                            val bytesPerSecond = (bytesDelta * 1000L / elapsed)
                            formatSpeed(bytesPerSecond)
                        } else "—"
                        lastBytes = totalBytesRead
                        lastTime = now
                        emit(fraction to speedStr)
                    }
                }
            }

            response.close()

            try {
                moveToGallery(tempFile, fileName, item.type, savePath)
            } catch (_: Exception) {
                // 拷贝失败不抛异常，文件至少已在缓存目录
            }

            emit(null)   // 完成信号
        }
    }

    private fun moveToGallery(
        file: File,
        fileName: String,
        type: DownloadType,
        savePath: String?
    ): String? {
        if (!file.exists() || file.length() == 0L) return null

        val mime = when (type) {
            DownloadType.VIDEO, DownloadType.LIVE_PHOTO -> "video/mp4"
            DownloadType.IMAGE -> "image/jpeg"
        }
        val cv = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH,
                    if (type == DownloadType.IMAGE) "Pictures/Douyin" else "Movies/Douyin")
            }
        }
        val collection = if (type == DownloadType.IMAGE) MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        else MediaStore.Video.Media.EXTERNAL_CONTENT_URI

        val uri = context.contentResolver.insert(collection, cv) ?: return null
        context.contentResolver.openOutputStream(uri)?.use { out ->
            FileInputStream(file).use { fis ->
                fis.copyTo(out, 8192)
            }
        }
        file.delete()
        return uri.toString()
    }

    private fun formatSpeed(bytesPerSecond: Long): String = when {
        bytesPerSecond < 1024 -> "$bytesPerSecond B/s"
        bytesPerSecond < 1024 * 1024 -> "${"%.1f".format(bytesPerSecond / 1024.0)} KB/s"
        bytesPerSecond < 1024 * 1024 * 1024 -> "${"%.1f".format(bytesPerSecond / (1024.0 * 1024))} MB/s"
        else -> "${"%.2f".format(bytesPerSecond / (1024.0 * 1024 * 1024))} GB/s"
    }
}