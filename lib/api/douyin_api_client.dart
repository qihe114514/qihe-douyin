import 'dart:convert';
import 'package:http/http.dart' as http;
import '../model/models.dart';

class DouyinApiClient {
  static const String _douyinApi = 'https://api.bugpk.com/api/douyin';
  static const String _xhsApi = 'https://api.bugpk.com/api/xhs';

  final http.Client _client;

  DouyinApiClient({http.Client? client}) : _client = client ?? http.Client();

  /// 解析抖音视频链接
  Future<ApiResponse> parseVideo(String shareUrl) async {
    try {
      final response = await _client.post(
        Uri.parse(_douyinApi),
        body: {'url': shareUrl},
        headers: {
          'User-Agent': 'DouyinDownloader/2.0 (Flutter)',
          'Accept': 'application/json',
        },
      );

      if (response.statusCode != 200) {
        throw Exception('HTTP ${response.statusCode}: ${response.body}');
      }

      final json = jsonDecode(response.body) as Map<String, dynamic>;
      final apiResponse = ApiResponse.fromJson(json);

      if (apiResponse.code != 200) {
        throw Exception(apiResponse.msg);
      }

      return apiResponse;
    } catch (e) {
      rethrow;
    }
  }

  /// 解析小红书链接
  Future<XhsApiResponse> parseXiaohongshu(String shareUrl) async {
    try {
      final uri = Uri.parse(_xhsApi).replace(queryParameters: {'url': shareUrl});
      final response = await _client.get(
        uri,
        headers: {
          'User-Agent': 'DouyinDownloader/2.0 (Flutter)',
          'Accept': 'application/json',
        },
      );

      if (response.statusCode != 200) {
        throw Exception('HTTP ${response.statusCode}: ${response.body}');
      }

      final json = jsonDecode(response.body) as Map<String, dynamic>;
      final xhsResponse = XhsApiResponse.fromJson(json);

      if (xhsResponse.code != 200) {
        throw Exception(xhsResponse.msg);
      }

      return xhsResponse;
    } catch (e) {
      rethrow;
    }
  }

  /// 解小红书短链接
  static Future<String> resolveXhsShortLink(String shortUrl) async {
    try {
      final client = http.Client();
      try {
        final request = http.Request('HEAD', Uri.parse(shortUrl));
        request.headers.addAll({
          'User-Agent': 'DouyinDownloader/2.0 (Flutter)',
        });
        final streamedResponse = await client.send(request);
        final location = streamedResponse.headers['location'] ?? '';
        streamedResponse.discard();
        if (location.isNotEmpty) {
          return location.startsWith('http')
              ? location
              : 'https://www.xiaohongshu.com$location';
        }
      } finally {
        client.close();
      }
    } catch (_) {}
    return shortUrl;
  }

  void dispose() {
    _client.close();
  }
}
