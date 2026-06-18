import 'dart:async';
import 'dart:io';
import 'package:http/http.dart' as http;
import 'package:path_provider/path_provider.dart';
import '../model/models.dart';
import 'package:image_gallery_saver_plus/image_gallery_saver_plus.dart';

class DownloadManager {
  final http.Client _client;

  DownloadManager({http.Client? client}) : _client = client ?? http.Client();

  Stream<DownloadProgress> downloadItem(DownloadItem item) async* {
    final extension = switch (item.type) {
      DownloadType.video || DownloadType.livePhoto => '.mp4',
      DownloadType.image => '.jpg',
    };
    final safeTitle = item.title.replaceAll(RegExp(r'[/\\:*?"<>|]'), '_');
    final fileName = '${safeTitle.length > 80 ? safeTitle.substring(0, 80) : safeTitle}$extension';

    // 获取临时目录
    final tempDir = await getTemporaryDirectory();
    final tempFile = File('${tempDir.path}/downloads/$fileName');
    await tempFile.parent.create(recursive: true);

    try {
      final request = http.Request('GET', Uri.parse(item.url));
      final streamedResponse = await _client.send(request);

      if (streamedResponse.statusCode != 200) {
        throw Exception('下载失败 HTTP ${streamedResponse.statusCode}');
      }

      final contentLength = streamedResponse.contentLength ?? -1;
      final sink = tempFile.openWrite();
      int totalBytesRead = 0;
      int lastBytes = 0;
      int lastTime = DateTime.now().millisecondsSinceEpoch;

      await for (final chunk in streamedResponse.stream) {
        sink.add(chunk);
        totalBytesRead += chunk.length;

        final now = DateTime.now().millisecondsSinceEpoch;
        final elapsed = now - lastTime;

        if (elapsed >= 500) {
          final fraction = contentLength > 0
              ? (totalBytesRead / contentLength).clamp(0.0, 1.0)
              : -1.0;
          final speedStr = elapsed > 0
              ? _formatSpeed((totalBytesRead - lastBytes) * 1000 ~/ elapsed)
              : '—';
          lastBytes = totalBytesRead;
          lastTime = now;

          yield DownloadProgress(
            fraction: fraction,
            speed: speedStr,
            bytesDownloaded: totalBytesRead,
            totalBytes: contentLength,
          );
        }
      }

      await sink.flush();
      await sink.close();

      // 保存到相册
      final result = await _saveToGallery(tempFile, fileName, item.type);

      // 删除临时文件
      await tempFile.delete();

      yield DownloadProgress(
        fraction: 1.0,
        speed: '',
        bytesDownloaded: totalBytesRead,
        totalBytes: contentLength,
        completed: true,
        savedPath: result,
      );
    } catch (e) {
      // 清理临时文件
      if (await tempFile.exists()) {
        await tempFile.delete();
      }
      yield DownloadProgress(
        fraction: 0,
        speed: '',
        bytesDownloaded: 0,
        totalBytes: 0,
        completed: true,
        error: e.toString(),
      );
    }
  }

  Future<String> _saveToGallery(File file, String fileName, DownloadType type) async {
    final mimeType = switch (type) {
      DownloadType.video || DownloadType.livePhoto => 'video/mp4',
      DownloadType.image => 'image/jpeg',
    };

    final result = await ImageGallerySaverPlus.saveFile(
      file.path,
      name: fileName,
      isReturnPathOfIOS: true,
    );

    if (result == null) {
      throw Exception('保存到相册失败');
    }

    return result.toString();
  }

  String _formatSpeed(int bytesPerSecond) {
    if (bytesPerSecond < 1024) return '$bytesPerSecond B/s';
    if (bytesPerSecond < 1024 * 1024) {
      return '${(bytesPerSecond / 1024).toStringAsFixed(1)} KB/s';
    }
    if (bytesPerSecond < 1024 * 1024 * 1024) {
      return '${(bytesPerSecond / (1024 * 1024)).toStringAsFixed(1)} MB/s';
    }
    return '${(bytesPerSecond / (1024 * 1024 * 1024)).toStringAsFixed(2)} GB/s';
  }

  void dispose() {
    _client.close();
  }
}

class DownloadProgress {
  final double fraction;
  final String speed;
  final int bytesDownloaded;
  final int totalBytes;
  final bool completed;
  final String? savedPath;
  final String? error;

  DownloadProgress({
    required this.fraction,
    required this.speed,
    required this.bytesDownloaded,
    required this.totalBytes,
    this.completed = false,
    this.savedPath,
    this.error,
  });

  bool get isError => error != null;
  bool get isDone => completed && error == null;
}

extension _StringTake on String {
  String take(int maxLength) {
    if (length <= maxLength) return this;
    return substring(0, maxLength);
  }
}
