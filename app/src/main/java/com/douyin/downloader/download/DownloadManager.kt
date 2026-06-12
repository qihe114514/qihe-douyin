package com.douyin.downloader.download

import android.app.DownloadManager as SysDownloadManager
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import com.douyin.downloader.model.DownloadItem
import com.douyin.downloader.model.DownloadType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.io.FileInputStream

class DownloadManager(private val context: Context) {

    private val sysDownloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as SysDownloadManager

    /**
     * 通过系统 DownloadManager 下载并拷贝到相册。
     * 返回 Flow 发出：(进度 0~1, 速度字符串), 完成后 emit null 并结束。
     */
    fun downloadWithProgress(
        item: DownloadItem,
        savePath: String?
    ): Flow<Pair<Float, String>?> = callbackFlow {
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
        val handler = Handler(Looper.getMainLooper())

        // 注册下载完成广播
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val id = intent?.getLongExtra(SysDownloadManager.EXTRA_DOWNLOAD_ID, -1) ?: -1
                if (id == downloadId && !finished) {
                    finished = true
                    trySend(null)

                    // 后台拷贝到相册
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val query = SysDownloadManager.Query().setFilterById(downloadId)
                            val cursor = sysDownloadManager.query(query)
                            var localUri: String? = null

                            if (cursor?.moveToFirst() == true) {
                                val status = cursor.getInt(cursor.getColumnIndexOrThrow(SysDownloadManager.COLUMN_STATUS))
                                if (status == SysDownloadManager.STATUS_SUCCESSFUL) {
                                    val filePath = cursor.getString(cursor.getColumnIndexOrThrow(SysDownloadManager.COLUMN_LOCAL_URI))
                                    if (filePath != null) {
                                        val file = File(Uri.parse(filePath).path ?: return@launch)
                                        if (!file.exists()) return@launch

                                        localUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                            saveToMediaStore(file, fileName, item.type)
                                        } else {
                                            copyLegacy(file, fileName, savePath)
                                        }
                                    }
                                }
                            }
                            cursor?.close()

                            // 移除系统下载条目
                            sysDownloadManager.remove(downloadId)
                        } catch (_: Exception) {}
                    }
                }
            }
        }

        context.registerReceiver(
            receiver,
            IntentFilter(SysDownloadManager.ACTION_DOWNLOAD_COMPLETE),
            Context.RECEIVER_NOT_EXPORTED
        )

        // 轮询进度
        val observer = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean) {
                if (finished) return
                try {
                    val query = SysDownloadManager.Query().setFilterById(downloadId)
                    val cursor = sysDownloadManager.query(query)
                    if (cursor?.moveToFirst() == true) {
                        val bytes = cursor.getLong(cursor.getColumnIndexOrThrow(SysDownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                        val total = cursor.getLong(cursor.getColumnIndexOrThrow(SysDownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                        val progress = if (total > 0) (bytes.toFloat() / total).coerceIn(0f, 1f) else -1f
                        val speed = formatSpeed(bytes, null)
                        trySend(progress to speed)
                    }
                    cursor?.close()
                } catch (_: Exception) {}
            }
        }
        context.contentResolver.registerContentObserver(
            Uri.parse("content://downloads/my_downloads"),
            true,
            observer
        )

        awaitClose {
            context.unregisterReceiver(receiver)
            context.contentResolver.unregisterContentObserver(observer)
            if (!finished) sysDownloadManager.remove(downloadId)
        }
    }

    private fun saveToMediaStore(file: File, fileName: String, type: DownloadType): String? {
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
        return uri.toString()
    }

    private fun copyLegacy(file: File, fileName: String, savePath: String?): String {
        val dir = if (!savePath.isNullOrEmpty() && File(savePath).exists()) File(savePath)
                  else File(context.getExternalFilesDir(null), "DouyinDownloads").also { it.mkdirs() }
        val dest = File(dir, fileName)
        file.copyTo(dest, overwrite = true)
        file.delete()
        return dest.absolutePath
    }

    private fun formatSpeed(bytesDownloaded: Long, elapsedMs: Long?): String = when {
        bytesDownloaded < 1024 -> "$bytesDownloaded B/s"
        bytesDownloaded < 1024 * 1024 -> "${"%.1f".format(bytesDownloaded / 1024.0)} KB/s"
        bytesDownloaded < 1024 * 1024 * 1024 -> "${"%.1f".format(bytesDownloaded / (1024.0 * 1024))} MB/s"
        else -> "${"%.2f".format(bytesDownloaded / (1024.0 * 1024 * 1024))} GB/s"
    }
}