import 'dart:convert';

// ==================== 平台枚举 ====================
enum Platform { douyin, xiaohongshu }

// ==================== 通用 API 响应 ====================
class ApiResponse {
  final int code;
  final String msg;
  final VideoData? data;

  ApiResponse({required this.code, required this.msg, this.data});

  factory ApiResponse.fromJson(Map<String, dynamic> json) => ApiResponse(
        code: json['code'] ?? 0,
        msg: json['msg'] ?? '',
        data: json['data'] != null ? VideoData.fromJson(json['data']) : null,
      );
}

// ==================== 抖音数据模型 ====================
class VideoData {
  final String type;
  final String title;
  final String desc;
  final Author? author;
  final String cover;
  final String? url;
  final List<VideoBackupItem> videoBackup;
  final List<String> images;
  final List<LivePhoto> livePhoto;
  final Music? music;

  VideoData({
    this.type = 'video',
    this.title = '',
    this.desc = '',
    this.author,
    this.cover = '',
    this.url,
    this.videoBackup = const [],
    this.images = const [],
    this.livePhoto = const [],
    this.music,
  });

  factory VideoData.fromJson(Map<String, dynamic> json) => VideoData(
        type: json['type'] ?? 'video',
        title: json['title'] ?? '',
        desc: json['desc'] ?? '',
        author: json['author'] != null ? Author.fromJson(json['author']) : null,
        cover: json['cover'] ?? '',
        url: json['url'],
        videoBackup: (json['video_backup'] as List<dynamic>?)
                ?.map((e) => VideoBackupItem.fromJson(e))
                .toList() ??
            [],
        images: (json['images'] as List<dynamic>?)
                ?.map((e) => e.toString())
                .toList() ??
            [],
        livePhoto: (json['live_photo'] as List<dynamic>?)
                ?.map((e) => LivePhoto.fromJson(e))
                .toList() ??
            [],
        music: json['music'] != null ? Music.fromJson(json['music']) : null,
      );

  /// 获取所有可下载的URL项
  List<DownloadItem> getAllVideoUrls() {
    final items = <DownloadItem>[];
    final baseTitle =
        title.isNotEmpty ? title : '${author?.name ?? "视频"}_${DateTime.now().millisecondsSinceEpoch}';

    switch (type) {
      case 'video':
        if (url != null && url!.isNotEmpty) {
          items.add(DownloadItem(
            url: url!,
            title: baseTitle,
            type: DownloadType.video,
            displayInfo: '源视频文件',
          ));
        }
        for (final backup in videoBackup) {
          items.add(DownloadItem(
            url: backup.url,
            title: baseTitle,
            type: DownloadType.video,
            displayInfo: buildDisplayInfo(backup.label),
          ));
        }
        break;
      case 'image':
        for (var i = 0; i < images.length; i++) {
          items.add(DownloadItem(
            url: images[i],
            title: '${baseTitle}_图${i + 1}',
            type: DownloadType.image,
            displayInfo: '图片',
          ));
        }
        break;
      case 'live':
        for (var i = 0; i < livePhoto.length; i++) {
          items.add(DownloadItem(
            url: livePhoto[i].video,
            title: '${baseTitle}_实况${i + 1}',
            type: DownloadType.livePhoto,
            displayInfo: '实况视频',
          ));
          items.add(DownloadItem(
            url: livePhoto[i].image,
            title: '${baseTitle}_实况${i + 1}_封面',
            type: DownloadType.image,
            displayInfo: '实况封面',
          ));
        }
        break;
    }
    return items;
  }
}

class VideoBackupItem {
  final String label;
  final String url;

  VideoBackupItem({this.label = '', this.url = ''});

  factory VideoBackupItem.fromJson(Map<String, dynamic> json) => VideoBackupItem(
        label: json['label'] ?? '',
        url: json['url'] ?? '',
      );
}

class Author {
  final String name;
  final int id;
  final String avatar;

  Author({this.name = '', this.id = 0, this.avatar = ''});

  factory Author.fromJson(Map<String, dynamic> json) => Author(
        name: json['name'] ?? '',
        id: json['id'] ?? 0,
        avatar: json['avatar'] ?? '',
      );
}

class LivePhoto {
  final String image;
  final String video;

  LivePhoto({this.image = '', this.video = ''});

  factory LivePhoto.fromJson(Map<String, dynamic> json) => LivePhoto(
        image: json['image'] ?? '',
        video: json['video'] ?? '',
      );
}

class Music {
  final String title;
  final String author;
  final String url;

  Music({this.title = '', this.author = '', this.url = ''});

  factory Music.fromJson(Map<String, dynamic> json) => Music(
        title: json['title'] ?? '',
        author: json['author'] ?? '',
        url: json['url'] ?? '',
      );
}

// ==================== 小红书数据模型 ====================
class XhsApiResponse {
  final int code;
  final String msg;
  final List<XhsDataItem>? data;

  XhsApiResponse({required this.code, required this.msg, this.data});

  factory XhsApiResponse.fromJson(Map<String, dynamic> json) => XhsApiResponse(
        code: json['code'] ?? 0,
        msg: json['msg'] ?? '',
        data: (json['data'] as List<dynamic>?)
            ?.map((e) => XhsDataItem.fromJson(e))
            .toList(),
      );
}

class XhsDataItem {
  final String author;
  final String authorID;
  final String title;
  final String desc;
  final String avatar;
  final String cover;
  final String url;
  final String type;

  XhsDataItem({
    this.author = '',
    this.authorID = '',
    this.title = '',
    this.desc = '',
    this.avatar = '',
    this.cover = '',
    this.url = '',
    this.type = '',
  });

  factory XhsDataItem.fromJson(Map<String, dynamic> json) => XhsDataItem(
        author: json['author'] ?? '',
        authorID: json['authorID'] ?? '',
        title: json['title'] ?? '',
        desc: json['desc'] ?? '',
        avatar: json['avatar'] ?? '',
        cover: json['cover'] ?? '',
        url: json['url'] ?? '',
        type: json['type'] ?? '',
      );

  List<DownloadItem> getAllDownloadItems() {
    final items = <DownloadItem>[];
    final baseTitle =
        title.isNotEmpty ? title : '小红书_${author}_${DateTime.now().millisecondsSinceEpoch}';
    if (url.isNotEmpty) {
      items.add(DownloadItem(
        url: url,
        title: baseTitle,
        type: DownloadType.video,
        displayInfo: '小红书视频',
      ));
    }
    if (cover.isNotEmpty) {
      items.add(DownloadItem(
        url: cover,
        title: '${baseTitle}_封面',
        type: DownloadType.image,
        displayInfo: '封面图片',
      ));
    }
    return items;
  }
}

/// 聚合多条笔记为一个展示
XhsDataItem? combineXhsItems(List<XhsDataItem> items) {
  if (items.isEmpty) return null;
  final first = items.first;
  final allUrls = items.map((e) => e.url).where((u) => u.isNotEmpty).toList();
  final allCovers = items.map((e) => e.cover).where((c) => c.isNotEmpty).toList();
  return XhsDataItem(
    url: allUrls.isNotEmpty ? allUrls.first : '',
    cover: allCovers.isNotEmpty ? allCovers.first : '',
    title: first.title.isNotEmpty ? first.title : first.desc.take(30).toString(),
    desc: items.map((e) => '${e.title.isNotEmpty ? e.title : e.desc.take(20)}: ${e.url}').join(' | '),
    author: first.author,
    avatar: first.avatar,
  );
}

// ==================== 下载项模型 ====================
enum DownloadType { video, image, livePhoto }

class DownloadItem {
  final String url;
  final String title;
  final DownloadType type;
  final String displayInfo;

  DownloadItem({
    required this.url,
    required this.title,
    required this.type,
    this.displayInfo = '',
  });
}

// ==================== 显示信息解析 ====================
/// 将 API 返回的 label 解析成用户可读的显示字符串
String buildDisplayInfo(String label) {
  if (label.isEmpty) return '';

  final parts = <String>[];

  // 分辨率提取
  final resMatch = RegExp(r'(\d{3,4})p', caseSensitive: false).firstMatch(label);
  if (resMatch != null) {
    final height = int.parse(resMatch.group(1)!);
    parts.add(
      switch (height) {
        >= 2160 => '4K',
        >= 1440 => '2K',
        >= 1080 => '1080p',
        >= 720 => '720p',
        >= 480 => '480p',
        _ => '${height}p',
      },
    );
  }

  // 帧率提取
  final fpsMatch = RegExp(r'(\d{2,3})fps', caseSensitive: false).firstMatch(label);
  if (fpsMatch != null) {
    parts.add('${fpsMatch.group(1)}fps');
  }

  // 码率提取
  final brMatch = RegExp(r'(\d{3,6})kbps', caseSensitive: false).firstMatch(label);
  if (brMatch != null) {
    final kbps = double.parse(brMatch.group(1)!);
    parts.add('${(kbps / 1000).toStringAsFixed(1)}Mbps');
  }

  // 编码格式
  if (label.contains('h265', ignoreCase: true) || label.contains('hevc', ignoreCase: true)) {
    parts.add('HEVC');
  } else if (label.contains('h264', ignoreCase: true) || label.contains('avc', ignoreCase: true)) {
    parts.add('H.264');
  }

  if (parts.isEmpty) {
    final simplified = label
        .replaceAll(RegExp(r'adapt_\w+_', caseSensitive: false), '')
        .replaceAll('_', ' ');
    return simplified.isNotEmpty ? simplified : label;
  }

  return parts.join(' · ');
}

// ==================== 历史记录 ====================
class HistoryEntry {
  final String url;
  final String title;
  final String type;
  final int timestamp;
  final String avatar;
  final String author;

  HistoryEntry({
    required this.url,
    required this.title,
    this.type = '',
    required this.timestamp,
    this.avatar = '',
    this.author = '',
  });

  Map<String, dynamic> toJson() => {
        'url': url,
        'title': title,
        'type': type,
        'timestamp': timestamp,
        'avatar': avatar,
        'author': author,
      };

  factory HistoryEntry.fromJson(Map<String, dynamic> json) => HistoryEntry(
        url: json['url'] ?? '',
        title: json['title'] ?? '',
        type: json['type'] ?? '',
        timestamp: json['timestamp'] ?? 0,
        avatar: json['avatar'] ?? '',
        author: json['author'] ?? '',
      );

  String get formattedDate {
    final dt = DateTime.fromMillisecondsSinceEpoch(timestamp);
    return '${dt.year}-${dt.month.toString().padLeft(2, '0')}-${dt.day.toString().padLeft(2, '0')} ${dt.hour.toString().padLeft(2, '0')}:${dt.minute.toString().padLeft(2, '0')}';
  }
}
