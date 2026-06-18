import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'main_view_model.dart';

/// 设置页面
class SettingsScreen extends StatefulWidget {
  const SettingsScreen({super.key});

  @override
  State<SettingsScreen> createState() => _SettingsScreenState();
}

class _SettingsScreenState extends State<SettingsScreen> {
  @override
  Widget build(BuildContext context) {
    final vm = context.watch<MainViewModel>();
    final settings = vm.settings;
    final theme = Theme.of(context);

    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        // 壁纸背景设置
        _buildSectionHeader(theme, '背景设置'),
        Card(
          child: Column(
            children: [
              // 背景壁纸路径
              ListTile(
                leading: const Icon(Icons.wallpaper),
                title: const Text('背景壁纸'),
                subtitle: Text(
                  settings.bgWallpaper ?? '未设置',
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
                trailing: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    if (settings.bgWallpaper != null)
                      IconButton(
                        icon: const Icon(Icons.close, size: 18),
                        onPressed: () => _clearWallpaper(vm),
                        tooltip: '清除壁纸',
                      ),
                    IconButton(
                      icon: const Icon(Icons.image),
                      onPressed: () => _pickWallpaper(vm),
                      tooltip: '选择壁纸',
                    ),
                  ],
                ),
              ),
              // 模糊半径
              ListTile(
                leading: const Icon(Icons.blur_on),
                title: const Text('模糊半径'),
                subtitle: Text('${settings.bgBlurRadius.toStringAsFixed(0)}'),
                trailing: SizedBox(
                  width: 160,
                  child: Slider(
                    value: settings.bgBlurRadius,
                    min: 0,
                    max: 50,
                    divisions: 50,
                    label: settings.bgBlurRadius.toStringAsFixed(0),
                    onChanged: (v) => vm.settings.setBgBlurRadius(v),
                  ),
                ),
              ),
              // 透明度
              ListTile(
                leading: const Icon(Icons.opacity),
                title: const Text('背景透明度'),
                subtitle: Text('${(settings.bgOpacity * 100).toStringAsFixed(0)}%'),
                trailing: SizedBox(
                  width: 160,
                  child: Slider(
                    value: settings.bgOpacity,
                    min: 0.0,
                    max: 1.0,
                    divisions: 20,
                    label: '${(settings.bgOpacity * 100).toStringAsFixed(0)}%',
                    onChanged: (v) => vm.settings.setBgOpacity(v),
                  ),
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 16),

        // 下载设置
        _buildSectionHeader(theme, '下载设置'),
        Card(
          child: Column(
            children: [
              SwitchListTile(
                secondary: const Icon(Icons.volume_up),
                title: const Text('视频声音'),
                subtitle: const Text('下载含声音的视频源'),
                value: settings.videoSoundEnabled,
                onChanged: (v) => settings.setVideoSoundEnabled(v),
              ),
              ListTile(
                leading: const Icon(Icons.folder),
                title: const Text('保存路径'),
                subtitle: Text('${settings.savePath}'),
                onTap: () => _showSavePathDialog(context, vm),
              ),
            ],
          ),
        ),
        const SizedBox(height: 16),

        // 更新设置
        _buildSectionHeader(theme, '其他设置'),
        Card(
          child: Column(
            children: [
              ListTile(
                leading: const Icon(Icons.update),
                title: const Text('更新通道'),
                subtitle: Text(settings.updateChannel == 'stable' ? '稳定版' : '测试版'),
                trailing: DropdownButton<String>(
                  value: settings.updateChannel,
                  underline: const SizedBox(),
                  items: const [
                    DropdownMenuItem(value: 'stable', child: Text('稳定版')),
                    DropdownMenuItem(value: 'beta', child: Text('测试版')),
                  ],
                  onChanged: (v) {
                    if (v != null) settings.setUpdateChannel(v);
                  },
                ),
              ),
              ListTile(
                leading: const Icon(Icons.info_outline),
                title: const Text('默认启动页'),
                trailing: DropdownButton<int>(
                  value: settings.defaultPage,
                  underline: const SizedBox(),
                  items: const [
                    DropdownMenuItem(value: 0, child: Text('主页')),
                    DropdownMenuItem(value: 1, child: Text('历史')),
                    DropdownMenuItem(value: 2, child: Text('设置')),
                  ],
                  onChanged: (v) {
                    if (v != null) settings.setDefaultPage(v);
                  },
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 16),

        // 关于
        _buildSectionHeader(theme, '关于'),
        Card(
          child: Column(
            children: [
              ListTile(
                leading: const Icon(Icons.info),
                title: const Text('版本'),
                subtitle: const Text('v2.0.0 (Flutter重构版)'),
              ),
              ListTile(
                leading: const Icon(Icons.code),
                title: const Text('技术栈'),
                subtitle: const Text('Flutter · Material 3 · BugPk-Api'),
              ),
              ListTile(
                leading: const Icon(Icons.person),
                title: const Text('作者'),
                subtitle: const Text('其核 @ B站'),
                onTap: () {},
              ),
            ],
          ),
        ),
        const SizedBox(height: 32),
      ],
    );
  }

  Widget _buildSectionHeader(ThemeData theme, String title) {
    return Padding(
      padding: const EdgeInsets.only(left: 4, bottom: 8, top: 8),
      child: Text(
        title,
        style: theme.textTheme.titleSmall?.copyWith(
          color: theme.colorScheme.primary,
          fontWeight: FontWeight.w600,
        ),
      ),
    );
  }

  void _pickWallpaper(MainViewModel vm) {
    // 简化版：显示输入对话框让用户输入壁纸路径
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('设置背景壁纸'),
        content: TextField(
          decoration: const InputDecoration(
            hintText: '输入图片文件路径',
            helperText: '例如: /storage/emulated/0/Pictures/wallpaper.jpg',
          ),
          onSubmitted: (value) {
            if (value.isNotEmpty) {
              vm.settings.setBgWallpaper(value);
            }
            Navigator.of(context).pop();
          },
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: const Text('取消'),
          ),
          FilledButton(
            onPressed: () => Navigator.of(context).pop(),
            child: const Text('确定'),
          ),
        ],
      ),
    );
  }

  void _clearWallpaper(MainViewModel vm) {
    vm.settings.setBgWallpaper(null);
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('壁纸已清除')),
    );
  }

  void _showSavePathDialog(BuildContext context, MainViewModel vm) {
    final controller = TextEditingController(text: vm.settings.savePath);
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('保存路径'),
        content: TextField(
          controller: controller,
          decoration: const InputDecoration(
            hintText: 'Douyin',
            helperText: '相册中的子文件夹名',
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: const Text('取消'),
          ),
          FilledButton(
            onPressed: () {
              vm.settings.setSavePath(controller.text.trim());
              Navigator.of(context).pop();
            },
            child: const Text('保存'),
          ),
        ],
      ),
    );
  }
}
