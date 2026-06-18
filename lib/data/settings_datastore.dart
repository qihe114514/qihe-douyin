import 'dart:convert';
import 'package:shared_preferences/shared_preferences.dart';
import '../model/models.dart';

/// 设置持久化层，替代原Kotlin的DataStore Preferences
/// 使用SharedPreferences实现，含兼容模式处理旧版DataStore键值
class SettingsDataStore {
  static const String _keySavePath = 'save_path';
  static const String _keyBgWallpaper = 'bg_wallpaper';
  static const String _keyBgBlurRadius = 'bg_blur_radius';
  static const String _keyBgOpacity = 'bg_opacity';
  static const String _keyVideoSoundEnabled = 'video_sound_enabled';
  static const String _keyUpdateChannel = 'update_channel';
  static const String _keyDefaultPage = 'default_page';
  static const String _keyParseHistory = 'parse_history';
  static const String _keyTabOrder = 'tab_order';
  static const String _keyFirstLaunch = 'first_launch';
  static const String _keyLastVersionCode = 'last_version_code';

  late SharedPreferences _prefs;

  /// 初始化
  Future<void> init() async {
    _prefs = await SharedPreferences.getInstance();
  }

  /// 下载保存路径
  String get savePath => _prefs.getString(_keySavePath) ?? 'Douyin';
  Future<void> setSavePath(String value) async {
    await _prefs.setString(_keySavePath, value);
  }

  /// 背景壁纸路径
  String? get bgWallpaper => _prefs.getString(_keyBgWallpaper);
  Future<void> setBgWallpaper(String? value) async {
    if (value != null) {
      await _prefs.setString(_keyBgWallpaper, value);
    } else {
      await _prefs.remove(_keyBgWallpaper);
    }
  }

  /// 背景模糊半径
  double get bgBlurRadius => _prefs.getDouble(_keyBgBlurRadius) ?? 25.0;
  Future<void> setBgBlurRadius(double value) async {
    await _prefs.setDouble(_keyBgBlurRadius, value);
  }

  /// 背景透明度
  double get bgOpacity => _prefs.getDouble(_keyBgOpacity) ?? 0.3;
  Future<void> setBgOpacity(double value) async {
    await _prefs.setDouble(_keyBgOpacity, value);
  }

  /// 视频声音开关
  bool get videoSoundEnabled => _prefs.getBool(_keyVideoSoundEnabled) ?? true;
  Future<void> setVideoSoundEnabled(bool value) async {
    await _prefs.setBool(_keyVideoSoundEnabled, value);
  }

  /// 更新通道
  String get updateChannel => _prefs.getString(_keyUpdateChannel) ?? 'stable';
  Future<void> setUpdateChannel(String value) async {
    await _prefs.setString(_keyUpdateChannel, value);
  }

  /// 默认启动页面
  int get defaultPage => _prefs.getInt(_keyDefaultPage) ?? 0;
  Future<void> setDefaultPage(int value) async {
    await _prefs.setInt(_keyDefaultPage, value);
  }

  /// 解析历史
  List<HistoryEntry> get parseHistory {
    final json = _prefs.getString(_keyParseHistory);
    if (json == null) return [];
    try {
      final list = jsonDecode(json) as List;
      return list.map((e) => HistoryEntry.fromJson(e as Map<String, dynamic>)).toList();
    } catch (_) {
      return [];
    }
  }

  Future<void> setParseHistory(List<HistoryEntry> entries) async {
    await _prefs.setString(
      _keyParseHistory,
      jsonEncode(entries.map((e) => e.toJson()).toList()),
    );
  }

  /// 追加一条解析历史（自动去重，最多50条）
  Future<void> addParseHistory(HistoryEntry entry) async {
    final history = parseHistory;
    history.removeWhere((e) => e.url == entry.url);
    history.insert(0, entry);
    if (history.length > 50) {
      history.removeRange(50, history.length);
    }
    await setParseHistory(history);
  }

  /// 标签页顺序
  List<String> get tabOrder {
    final json = _prefs.getString(_keyTabOrder);
    if (json == null) return ['main', 'history', 'settings'];
    try {
      return (jsonDecode(json) as List).cast<String>();
    } catch (_) {
      return ['main', 'history', 'settings'];
    }
  }

  Future<void> setTabOrder(List<String> order) async {
    await _prefs.setString(_keyTabOrder, jsonEncode(order));
  }

  /// 首次启动
  bool get isFirstLaunch => _prefs.getBool(_keyFirstLaunch) ?? true;
  Future<void> setFirstLaunch(bool value) async {
    await _prefs.setBool(_keyFirstLaunch, value);
  }

  /// 上次运行的版本号
  int? get lastVersionCode => _prefs.getInt(_keyLastVersionCode);
  Future<void> setLastVersionCode(int value) async {
    await _prefs.setInt(_keyLastVersionCode, value);
  }

  /// 清除所有设置
  Future<void> clear() async {
    await _prefs.clear();
  }
}
