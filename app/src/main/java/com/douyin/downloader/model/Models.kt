package com.douyin.downloader.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse(
    val code: Int,
    val msg: String,
    val data: VideoData? = null
)

@Serializable
data class VideoData(
    val type: String = "video",
    val title: String = "",
    val desc: String = "",
    val author: Author? = null,
    val cover: String = "",
    val url: String? = null,
    @SerialName("video_backup")
    val videoBackup: List<VideoBackupItem> = emptyList(),
    val images: List<String> = emptyList(),
    @SerialName("live_photo")
    val livePhoto: List<LivePhoto> = emptyList(),
    val music: Music? = null
)

@Serializable
data class VideoBackupItem(
    val label: String = "",
    val url: String = ""
)

@Serializable
data class Author(
    val name: String = "",
    val id: Long = 0,
    val avatar: String = ""
)

@Serializable
data class LivePhoto(
    val image: String = "",
    val video: String = ""
)

@Serializable
data class Music(
    val title: String = "",
    val author: String = "",
    val url: String = ""
)

/**
 * 获取所有可下载的URL（含分辨率/码率/帧率信息）
 */
fun VideoData.getAllVideoUrls(): List<DownloadItem> {
    val items = mutableListOf<DownloadItem>()

    val baseTitle = title.ifEmpty { "${author?.name ?: "视频"}_${System.currentTimeMillis()}" }

    when (type) {
        "video" -> {
            url?.let {
                items.add(DownloadItem(
                    url = it,
                    title = baseTitle,
                    type = DownloadType.VIDEO,
                    resolution = "原画",
                    bitrate = null,
                    fps = null,
                    fileSize = null
                ))
            }
            videoBackup.forEach { backup ->
                items.add(DownloadItem(
                    url = backup.url,
                    title = "$baseTitle _ ${backup.label}",
                    type = DownloadType.VIDEO,
                    resolution = backup.label,
                    bitrate = null,
                    fps = null,
                    fileSize = null
                ))
            }
        }
        "image" -> {
            images.forEachIndexed { index, imgUrl ->
                items.add(DownloadItem(
                    url = imgUrl,
                    title = "${baseTitle}_图${index + 1}",
                    type = DownloadType.IMAGE,
                    resolution = null,
                    bitrate = null,
                    fps = null,
                    fileSize = null
                ))
            }
        }
        "live" -> {
            livePhoto.forEachIndexed { index, lp ->
                items.add(DownloadItem(
                    url = lp.video,
                    title = "${baseTitle}_实况${index + 1}",
                    type = DownloadType.LIVE_PHOTO,
                    resolution = null,
                    bitrate = null,
                    fps = null,
                    fileSize = null
                ))
                items.add(DownloadItem(
                    url = lp.image,
                    title = "${baseTitle}_实况${index + 1}_封面",
                    type = DownloadType.IMAGE,
                    resolution = null,
                    bitrate = null,
                    fps = null,
                    fileSize = null
                ))
            }
        }
    }

    return items
}

data class DownloadItem(
    val url: String,
    val title: String,
    val type: DownloadType,
    val resolution: String? = null,
    val bitrate: Double? = null,
    val fps: Int? = null,
    val fileSize: String? = null
)

enum class DownloadType {
    VIDEO, IMAGE, LIVE_PHOTO
}
