import 'dart:async';
import 'package:flutter/foundation.dart';
import 'package:http/http.dart' as http;
import '../../api/douyin_api_client.dart';
import '../../model/models.dart';
import '../../download/download_manager.dart';
import '../../data/settings_datastore.dart';

/// 主屏幕状态枚举
enum MainScreenTab {
  main,
  history,
  settings,
}

/// 解析状态
enum ParseStatus {
  idle,
  parsing,
  success,
  error,
}

/// 主ViewModel（状态管理 + 业务逻辑，替代原Kotlin MainViewModel）
class MainViewModel extends ChangeNotifier {
  final DouyinApiClient _apiClient = DouyinApiClient(client: http.Client());
  final DownloadManager _downloadManager = DownloadManager();
  final SettingsDataStore _settings = SettingsDataStore();

  // ============ 状态 ============

  /// 设置存储
  SettingsDataStore get settings => _settings;

  /// 当前标签页
  MainScreenTab _currentTab = MainScreenTab.main;
  MainScreenTab get currentTab => _currentTab;

  /// 解析状态
  ParseStatus _parseStatus = ParseStatus.idle;
  ParseStatus get parseStatus => _parseStatus;

  /// 错误信息
  String? _errorMessage;
  String? get errorMessage => _errorMessage;

  /// 输入文本（URL）
  String _inputText = '';
  String get inputText => _inputText;

  /// 解析结果
  ParseResult? _parseResult;
  ParseResult? get parseResult => _parseResult;

  /// 平台检测
  PlatformType? _detectedPlatform;
  PlatformType? get detectedPlatform => _detectedPlatform;

  /// 下载状态映射（url -> DownloadProgress）
  final Map<String, DownloadProgress> _downloadProgress = {};
  Map<String, DownloadProgress> get downloadProgress => Map.unmodifiable(_downloadProgress);

  /// 历史记录
  List<HistoryEntry> get history => _settings.parseHistory;

  /// 正在下载的取消令牌
  final Map<String, StreamSubscription> _downloadSubscriptions = {};

  /// 是否正在解析
  bool get isParsing => _parseStatus == ParseStatus.parsing;

  /// 是否有下载任务进行中
  bool get hasActiveDownloads => _downloadSubscriptions.isNotEmpty;

  // ============ 初始化 ============

  Future<void> init() async {
    await _settings.init();
  }

  // ============ 标签页切换 ============

  void switchTab(MainScreenTab tab) {
    _currentTab = tab;
    notifyListeners();
  }

  // ============ 输入处理 ============

  void onInputChanged(String text) {
    _inputText = text;
    _detectedPlatform = null;
    if (text.isNotEmpty) {
      // 简单平台检测
      if (text.contains('douyin.com') || text.contains('iesdouyin.com')) {
        _detectedPlatform = PlatformType.douyin;
      } else if (text.contains('xiaohongshu.com') || text.contains('xhslink.com')) {
        _detectedPlatform = PlatformType.xiaohongshu;
      }
    }
    notifyListeners();
  }

  // ============ 解析逻辑 ============

  /// 解析URL
  Future<void> parseUrl(String url) async {
    if (url.isEmpty) return;

    _parseStatus = ParseStatus.parsing;
    _errorMessage = null;
    _parseResult = null;
    _downloadProgress.clear();
    notifyListeners();

    try {
      // 先检测短链接
      final resolvedUrl = await _apiClient.resolveShortLink(url);
      final actualUrl = resolvedUrl ?? url;

      // 检测平台
      PlatformType platform;
      if (actualUrl.contains('douyin.com') || actualUrl.contains('iesdouyin.com')) {
        platform = PlatformType.douyin;
      } else if (actualUrl.contains('xiaohongshu.com') || actualUrl.contains('xhslink.com')) {
        platform = PlatformType.xiaohongshu;
      } else {
        throw Exception('不支持的链接类型');
      }

      if (platform == PlatformType.douyin) {
        await _parseDouyin(actualUrl);
      } else {
        await _parseXiaohongshu(actualUrl);
      }
    } catch (e) {
      _parseStatus = ParseStatus.error;
      _errorMessage = e.toString().replaceFirst('Exception: ', '');
      notifyListeners();
    }
  }

  Future<void> _parseDouyin(String url) async {
    final response = await _apiClient.parseDouyin(url);
    final items = response.data?.getAllVideoUrls() ?? [];
    if (items.isEmpty) throw Exception('未找到可下载的视频/图片');

    _parseResult = ParseResult(
      platform: PlatformType.douyin,
      title: response.data?.desc ?? response.data?.title ?? '抖音视频',
      authorName: response.data?.author?.name,
      authorAvatar: response.data?.author?.avatar,
      items: items,
      originalUrl: url,
    );

    _parseStatus = ParseStatus.success;
    notifyListeners();

    // 记录历史
    await _settings.addParseHistory(HistoryEntry(
      url: url,
      title: _parseResult!.title,
      platform: PlatformType.douyin,
      timestamp: DateTime.now().millisecondsSinceEpoch,
    ));
  }

  Future<void> _parseXiaohongshu(String url) async {
    final response = await _apiClient.parseXiaohongshu(url);
    final xhsItems = response.data ?? [];
    final items = <DownloadItem>[];
    String noteTitle = '小红书笔记';
    String? noteAuthor;
    String? noteAvatar;
    for (final xhs in xhsItems) {
      items.addAll(xhs.getAllDownloadItems());
      if (xhs.title.isNotEmpty) noteTitle = xhs.title;
      if (xhs.author.isNotEmpty) noteAuthor = xhs.author;
      if (xhs.avatar.isNotEmpty) noteAvatar = xhs.avatar;
    }
    if (items.isEmpty) throw Exception('未找到可下载的内容');

    _parseResult = ParseResult(
      platform: PlatformType.xiaohongshu,
      title: noteTitle,
      authorName: noteAuthor,
      authorAvatar: noteAvatar,
      items: items,
      originalUrl: url,
    );

    _parseStatus = ParseStatus.success;
    notifyListeners();

    await _settings.addParseHistory(HistoryEntry(
      url: url,
      title: _parseResult!.title,
      platform: PlatformType.xiaohongshu,
      timestamp: DateTime.now().millisecondsSinceEpoch,
    ));
  }

  // ============ 下载逻辑 ============

  /// 下载指定项
  Future<void> downloadItem(DownloadItem item) async {
    final url = item.url;
    if (url.isEmpty) return;

    // 初始化进度
    _downloadProgress[url] = DownloadProgress(
      fraction: 0.0,
      speed: '',
      bytesDownloaded: 0,
      totalBytes: 0,
      completed: false,
      error: null,
    );
    notifyListeners();

    try {
      final subscription = _downloadManager.downloadItem(item).listen(
        (progress) {
          _downloadProgress[url] = progress;
          notifyListeners();
        },
        onError: (error) {
          _downloadProgress[url] = DownloadProgress(
            fraction: 0.0,
            speed: '',
            bytesDownloaded: 0,
            totalBytes: 0,
            completed: false,
            error: error.toString(),
          );
          _downloadSubscriptions.remove(url);
          notifyListeners();
        },
        onDone: () {
          _downloadSubscriptions.remove(url);
          notifyListeners();
        },
        cancelOnError: false,
      );

      _downloadSubscriptions[url] = subscription;
    } catch (e) {
      _downloadProgress[url] = DownloadProgress(
        fraction: 0.0,
        speed: '',
        bytesDownloaded: 0,
        totalBytes: 0,
        completed: false,
        error: e.toString(),
      );
      notifyListeners();
    }
  }

  /// 下载所有项
  Future<void> downloadAll() async {
    if (_parseResult == null) return;
    for (final item in _parseResult!.items) {
      if (_downloadProgress[item.url]?.completed != true) {
        await downloadItem(item);
      }
    }
  }

  /// 取消下载
  void cancelDownload(String url) {
    _downloadSubscriptions[url]?.cancel();
    _downloadSubscriptions.remove(url);
    _downloadProgress.remove(url);
    notifyListeners();
  }

  /// 取消所有下载
  void cancelAllDownloads() {
    for (final sub in _downloadSubscriptions.values) {
      sub.cancel();
    }
    _downloadSubscriptions.clear();
    _downloadProgress.clear();
    notifyListeners();
  }

  // ============ 清除结果 ============

  void clearResult() {
    cancelAllDownloads();
    _parseStatus = ParseStatus.idle;
    _parseResult = null;
    _errorMessage = null;
    _inputText = '';
    _detectedPlatform = null;
    notifyListeners();
  }

  /// 刷新历史
  void refreshHistory() {
    notifyListeners();
  }

  @override
  void dispose() {
    cancelAllDownloads();
    _apiClient.dispose();
    _downloadManager.dispose();
    super.dispose();
  }
}

/// 解析结果
class ParseResult {
  final PlatformType platform;
  final String title;
  final String? authorName;
  final String? authorAvatar;
  final List<DownloadItem> items;
  final String originalUrl;

  const ParseResult({
    required this.platform,
    required this.title,
    this.authorName,
    this.authorAvatar,
    required this.items,
    required this.originalUrl,
  });
}
