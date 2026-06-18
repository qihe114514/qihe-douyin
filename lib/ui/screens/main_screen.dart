import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';
import '../../model/models.dart';
import '../../download/download_manager.dart';
import 'main_view_model.dart';
import '../theme/theme.dart';

/// 主屏幕（解析 + 下载 + 展示）
class MainScreen extends StatefulWidget {
  const MainScreen({super.key});

  @override
  State<MainScreen> createState() => _MainScreenState();
}

class _MainScreenState extends State<MainScreen> {
  final TextEditingController _urlController = TextEditingController();
  final FocusNode _urlFocusNode = FocusNode();

  @override
  void dispose() {
    _urlController.dispose();
    _urlFocusNode.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final viewModel = context.watch<MainViewModel>();
    final theme = Theme.of(context);
    final colorScheme = theme.colorScheme;

    // 壁纸背景
    Widget content = _buildBody(viewModel, theme, colorScheme);

    return WallpaperBackground(
      child: content,
      wallpaperPath: viewModel.settings.bgWallpaper,
      blurRadius: viewModel.settings.bgBlurRadius,
      opacity: viewModel.settings.bgOpacity,
    );
  }

  Widget _buildBody(MainViewModel vm, ThemeData theme, ColorScheme cs) {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          // 头部标题
          Text(
            '抖音/小红书 下载器',
            style: theme.textTheme.headlineSmall?.copyWith(
              fontWeight: FontWeight.bold,
            ),
            textAlign: TextAlign.center,
          ),
          const SizedBox(height: 8),
          Text(
            '粘贴链接即可解析下载',
            style: theme.textTheme.bodyMedium?.copyWith(
              color: cs.onSurfaceVariant,
            ),
            textAlign: TextAlign.center,
          ),
          const SizedBox(height: 20),

          // URL输入框
          _buildUrlInput(vm, theme, cs),
          const SizedBox(height: 12),

          // 解析按钮
          _buildParseButton(vm, cs),
          const SizedBox(height: 20),

          // 解析状态/结果
          _buildResultSection(vm, theme, cs),
        ],
      ),
    );
  }

  Widget _buildUrlInput(MainViewModel vm, ThemeData theme, ColorScheme cs) {
    return TextField(
      controller: _urlController,
      focusNode: _urlFocusNode,
      decoration: InputDecoration(
        hintText: '粘贴抖音/小红书分享链接...',
        prefixIcon: Icon(
          vm.detectedPlatform == PlatformType.douyin
              ? Icons.music_note
              : vm.detectedPlatform == PlatformType.xiaohongshu
                  ? Icons.bookmark
                  : Icons.link,
          color: vm.detectedPlatform != null ? cs.primary : null,
        ),
        suffixIcon: _urlController.text.isNotEmpty
            ? IconButton(
                icon: const Icon(Icons.clear),
                onPressed: () {
                  _urlController.clear();
                  vm.onInputChanged('');
                },
              )
            : null,
        filled: true,
        fillColor: cs.surfaceContainerHighest.withValues(alpha: 0.3),
      ),
      onChanged: vm.onInputChanged,
      onSubmitted: (_) => _parse(vm),
      textInputAction: TextInputAction.go,
      maxLines: 1,
    );
  }

  Widget _buildParseButton(MainViewModel vm, ColorScheme cs) {
    return FilledButton.icon(
      onPressed: vm.isParsing ? null : () => _parse(vm),
      icon: vm.isParsing
          ? const SizedBox(
              width: 20,
              height: 20,
              child: CircularProgressIndicator(strokeWidth: 2),
            )
          : const Icon(Icons.search),
      label: Text(vm.isParsing ? '解析中...' : '解析'),
      style: FilledButton.styleFrom(
        padding: const EdgeInsets.symmetric(vertical: 14),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      ),
    );
  }

  void _parse(MainViewModel vm) {
    final text = _urlController.text.trim();
    if (text.isEmpty) {
      HapticFeedback.lightImpact();
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('请输入分享链接')),
      );
      return;
    }
    vm.parseUrl(text);
  }

  // ============ 结果展示区 ============

  Widget _buildResultSection(MainViewModel vm, ThemeData theme, ColorScheme cs) {
    switch (vm.parseStatus) {
      case ParseStatus.idle:
        return const SizedBox.shrink();

      case ParseStatus.parsing:
        return _buildLoadingCard(theme, cs);

      case ParseStatus.error:
        return _buildErrorCard(vm, theme, cs);

      case ParseStatus.success:
        return _buildResultCard(vm, theme, cs);
    }
  }

  Widget _buildLoadingCard(ThemeData theme, ColorScheme cs) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          children: [
            const CircularProgressIndicator(),
            const SizedBox(height: 16),
            Text('正在解析...', style: theme.textTheme.bodyLarge),
            const SizedBox(height: 4),
            Text(
              '请稍候，正在获取视频信息',
              style: theme.textTheme.bodySmall?.copyWith(color: cs.onSurfaceVariant),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildErrorCard(MainViewModel vm, ThemeData theme, ColorScheme cs) {
    return Card(
      color: cs.errorContainer,
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          children: [
            Icon(Icons.error_outline, size: 48, color: cs.error),
            const SizedBox(height: 8),
            Text('解析失败', style: theme.textTheme.titleMedium?.copyWith(color: cs.error)),
            const SizedBox(height: 4),
            Text(
              vm.errorMessage ?? '未知错误',
              style: theme.textTheme.bodyMedium?.copyWith(color: cs.onErrorContainer),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 12),
            OutlinedButton(
              onPressed: vm.clearResult,
              child: const Text('清除'),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildResultCard(MainViewModel vm, ThemeData theme, ColorScheme cs) {
    final result = vm.parseResult!;
    final hasActiveDownloads = vm.hasActiveDownloads;

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // 平台标签 + 清除按钮
            Row(
              children: [
                Chip(
                  avatar: Icon(
                    result.platform == PlatformType.douyin ? Icons.music_note : Icons.bookmark,
                    size: 16,
                  ),
                  label: Text(result.platform == PlatformType.douyin ? '抖音' : '小红书'),
                  visualDensity: VisualDensity.compact,
                ),
                const Spacer(),
                IconButton(
                  icon: const Icon(Icons.close),
                  onPressed: vm.clearResult,
                  tooltip: '清除结果',
                ),
              ],
            ),

            // 标题
            Text(
              result.title,
              style: theme.textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w600),
              maxLines: 2,
              overflow: TextOverflow.ellipsis,
            ),
            const SizedBox(height: 4),

            // 作者
            if (result.authorName != null)
              Row(
                children: [
                  if (result.authorAvatar != null)
                    CircleAvatar(
                      radius: 12,
                      backgroundImage: NetworkImage(result.authorAvatar!),
                    ),
                  if (result.authorAvatar != null) const SizedBox(width: 8),
                  Text(
                    result.authorName!,
                    style: theme.textTheme.bodyMedium?.copyWith(color: cs.onSurfaceVariant),
                  ),
                ],
              ),
            const SizedBox(height: 4),

            // 数量
            Text(
              '共 ${result.items.length} 项',
              style: theme.textTheme.bodySmall?.copyWith(color: cs.onSurfaceVariant),
            ),
            const SizedBox(height: 12),

            // 下载全部按钮
            if (result.items.length > 1)
              Padding(
                padding: const EdgeInsets.only(bottom: 12),
                child: SizedBox(
                  width: double.infinity,
                  child: OutlinedButton.icon(
                    onPressed: hasActiveDownloads
                        ? null
                        : () => _showDownloadAllConfirm(vm),
                    icon: const Icon(Icons.download),
                    label: Text(hasActiveDownloads ? '下载中...' : '下载全部 (${result.items.length}项)'),
                    style: OutlinedButton.styleFrom(
                      padding: const EdgeInsets.symmetric(vertical: 12),
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
                    ),
                  ),
                ),
              ),

            // 下载项列表
            ...result.items.map((item) => _buildDownloadItem(vm, item, theme, cs)),
          ],
        ),
      ),
    );
  }

  Widget _buildDownloadItem(MainViewModel vm, DownloadItem item, ThemeData theme, ColorScheme cs) {
    final progress = vm.downloadProgress[item.url];
    final isCompleted = progress?.completed == true;
    final isDownloading = progress != null && !isCompleted;
    final hasError = progress?.error != null;
    final displayInfo = item.displayInfo;

    return Card(
      margin: const EdgeInsets.symmetric(vertical: 4),
      color: isCompleted ? cs.tertiaryContainer.withValues(alpha: 0.3) : null,
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                // 类型图标
                Icon(
                  item.type == DownloadType.video
                      ? (item.url.contains('music') ? Icons.audio_file : Icons.videocam)
                      : Icons.photo_library,
                  size: 20,
                  color: isCompleted ? cs.tertiary : null,
                ),
                const SizedBox(width: 8),
                // 文件名
                Expanded(
                  child: Text(
                    item.fileName,
                    style: theme.textTheme.bodyMedium?.copyWith(
                      fontWeight: FontWeight.w500,
                      decoration: isCompleted ? TextDecoration.lineThrough : null,
                    ),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  ),
                ),
                // 下载按钮/状态
                if (isCompleted)
                  const Icon(Icons.check_circle, color: Colors.green, size: 20)
                else if (isDownloading)
                  _buildDownloadControls(vm, item, progress)
                else
                  IconButton(
                    icon: const Icon(Icons.download),
                    onPressed: () => vm.downloadItem(item),
                    tooltip: '下载',
                    visualDensity: VisualDensity.compact,
                  ),
              ],
            ),

            // 显示信息（分辨率/帧率/编码等）
            if (displayInfo.isNotEmpty)
              Padding(
                padding: const EdgeInsets.only(top: 4, left: 28),
                child: Text(
                  displayInfo,
                  style: theme.textTheme.bodySmall?.copyWith(color: cs.onSurfaceVariant),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
              ),

            // 下载进度条
            if (isDownloading) ...[
              const SizedBox(height: 4),
              ClipRRect(
                borderRadius: BorderRadius.circular(4),
                child: LinearProgressIndicator(
                  value: progress.fraction,
                  minHeight: 4,
                ),
              ),
              const SizedBox(height: 2),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(
                    '${(progress.fraction * 100).toStringAsFixed(1)}%',
                    style: theme.textTheme.bodySmall,
                  ),
                  if (progress.speed.isNotEmpty)
                    Text(
                      progress.speed,
                      style: theme.textTheme.bodySmall?.copyWith(color: cs.onSurfaceVariant),
                    ),
                ],
              ),
            ],

            // 错误信息
            if (hasError)
              Padding(
                padding: const EdgeInsets.only(top: 4, left: 28),
                child: Text(
                  progress!.error!,
                  style: theme.textTheme.bodySmall?.copyWith(color: cs.error),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
              ),
          ],
        ),
      ),
    );
  }

  Widget _buildDownloadControls(MainViewModel vm, DownloadItem item, DownloadProgress progress) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Text(
          '${(progress.fraction * 100).toStringAsFixed(0)}%',
          style: const TextStyle(fontSize: 12),
        ),
        const SizedBox(width: 4),
        IconButton(
          icon: const Icon(Icons.cancel, size: 18),
          onPressed: () => vm.cancelDownload(item.url),
          tooltip: '取消下载',
          visualDensity: VisualDensity.compact,
        ),
      ],
    );
  }

  void _showDownloadAllConfirm(MainViewModel vm) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('下载全部'),
        content: Text('确定要下载全部 ${vm.parseResult!.items.length} 项内容吗？'),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: const Text('取消'),
          ),
          FilledButton(
            onPressed: () {
              Navigator.of(context).pop();
              vm.downloadAll();
            },
            child: const Text('开始下载'),
          ),
        ],
      ),
    );
  }
}
