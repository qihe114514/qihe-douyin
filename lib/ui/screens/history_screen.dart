import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../model/models.dart';
import 'main_view_model.dart';

/// 历史记录页面
class HistoryScreen extends StatelessWidget {
  const HistoryScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final vm = context.watch<MainViewModel>();
    final history = vm.history;

    if (history.isEmpty) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(
              Icons.history,
              size: 64,
              color: Theme.of(context).colorScheme.onSurfaceVariant.withOpacity(0.4),
            ),
            const SizedBox(height: 16),
            Text(
              '暂无解析记录',
              style: Theme.of(context).textTheme.titleMedium?.copyWith(
                color: Theme.of(context).colorScheme.onSurfaceVariant,
              ),
            ),
            const SizedBox(height: 8),
            Text(
              '解析视频后记录将显示在这里',
              style: Theme.of(context).textTheme.bodySmall?.copyWith(
                color: Theme.of(context).colorScheme.onSurfaceVariant.withOpacity(0.6),
              ),
            ),
          ],
        ),
      );
    }

    return ListView.builder(
      padding: const EdgeInsets.all(8),
      itemCount: history.length,
      itemBuilder: (context, index) => _buildHistoryItem(context, history[index], vm),
    );
  }

  Widget _buildHistoryItem(BuildContext context, HistoryEntry entry, MainViewModel vm) {
    final theme = Theme.of(context);
    final cs = theme.colorScheme;

    return Card(
      margin: const EdgeInsets.symmetric(vertical: 4, horizontal: 8),
      child: ListTile(
        leading: CircleAvatar(
          backgroundColor: entry.platform == PlatformType.douyin
              ? cs.primaryContainer
              : cs.tertiaryContainer,
          child: Icon(
            entry.platform == PlatformType.douyin ? Icons.music_note : Icons.bookmark,
            size: 20,
            color: entry.platform == PlatformType.douyin
                ? cs.onPrimaryContainer
                : cs.onTertiaryContainer,
          ),
        ),
        title: Text(
          entry.title,
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
          style: theme.textTheme.bodyLarge,
        ),
        subtitle: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              entry.platform == PlatformType.douyin ? '抖音' : '小红书',
              style: theme.textTheme.bodySmall?.copyWith(color: cs.onSurfaceVariant),
            ),
            Text(
              entry.formattedDate,
              style: theme.textTheme.bodySmall?.copyWith(color: cs.onSurfaceVariant.withOpacity(0.6)),
            ),
          ],
        ),
        trailing: IconButton(
          icon: const Icon(Icons.download),
          onPressed: () => vm.parseUrl(entry.url),
          tooltip: '重新解析',
        ),
        onTap: () => vm.parseUrl(entry.url),
      ),
    );
  }
}
