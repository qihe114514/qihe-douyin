import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';
import 'ui/screens/main_view_model.dart';
import 'ui/screens/main_screen.dart';
import 'ui/screens/history_screen.dart';
import 'ui/screens/settings_screen.dart';
import 'ui/theme/theme.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  SystemChrome.setPreferredOrientations([
    DeviceOrientation.portraitUp,
    DeviceOrientation.portraitDown,
  ]);
  runApp(const DouyinDownloaderApp());
}

class DouyinDownloaderApp extends StatelessWidget {
  const DouyinDownloaderApp({super.key});

  @override
  Widget build(BuildContext context) {
    return ChangeNotifierProvider(
      create: (_) => MainViewModel()..init(),
      child: MaterialApp(
        title: '抖音/小红书 下载器',
        debugShowCheckedModeBanner: false,
        theme: AppTheme.lightTheme(null),
        darkTheme: AppTheme.darkTheme(null),
        themeMode: ThemeMode.system,
        home: const MainShell(),
      ),
    );
  }
}

/// 主页面壳（底部导航 + 页面切换）
class MainShell extends StatefulWidget {
  const MainShell({super.key});

  @override
  State<MainShell> createState() => _MainShellState();
}

class _MainShellState extends State<MainShell> {
  /// 页面列表
  final List<Widget> _pages = const [
    MainScreen(),
    HistoryScreen(),
    SettingsScreen(),
  ];

  /// 页面标题
  final List<String> _titles = const ['下载', '历史', '设置'];

  @override
  Widget build(BuildContext context) {
    final vm = context.watch<MainViewModel>();
    final currentIndex = vm.currentTab.index;

    return Scaffold(
      appBar: AppBar(
        title: Text(_titles[currentIndex]),
        actions: [
          // 有活跃下载时显示计数
          if (vm.hasActiveDownloads)
            Padding(
              padding: const EdgeInsets.only(right: 8),
              child: Badge(
                label: Text('${vm.downloadProgress.length}'),
                child: IconButton(
                  icon: const Icon(Icons.downloading),
                  onPressed: () => vm.switchTab(MainScreenTab.main),
                  tooltip: '下载进度',
                ),
              ),
            ),
          // 设置按钮
          if (currentIndex != 2)
            IconButton(
              icon: const Icon(Icons.settings_outlined),
              onPressed: () => vm.switchTab(MainScreenTab.settings),
              tooltip: '设置',
            ),
        ],
      ),
      body: IndexedStack(
        index: currentIndex,
        children: _pages,
      ),
      bottomNavigationBar: NavigationBar(
        selectedIndex: currentIndex,
        onDestinationSelected: (index) {
          vm.switchTab(MainScreenTab.values[index]);
        },
        destinations: const [
          NavigationDestination(
            icon: Icon(Icons.download_outlined),
            selectedIcon: Icon(Icons.download),
            label: '下载',
          ),
          NavigationDestination(
            icon: Icon(Icons.history_outlined),
            selectedIcon: Icon(Icons.history),
            label: '历史',
          ),
          NavigationDestination(
            icon: Icon(Icons.settings_outlined),
            selectedIcon: Icon(Icons.settings),
            label: '设置',
          ),
        ],
      ),
    );
  }
}
