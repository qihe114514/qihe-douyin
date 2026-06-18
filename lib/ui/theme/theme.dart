import 'package:flutter/material.dart';

/// App主题配置
/// 支持Material You动态取色 + 深色模式 + 自定义背景壁纸
class AppTheme {
  /// 亮色主题
  static ThemeData lightTheme(ColorScheme? dynamicColor) {
    final colorScheme = dynamicColor ?? ColorScheme.fromSeed(seedColor: Colors.blue);
    return ThemeData(
      useMaterial3: true,
      colorScheme: colorScheme,
      brightness: Brightness.light,
      appBarTheme: AppBarTheme(
        centerTitle: true,
        elevation: 0,
        backgroundColor: colorScheme.surface,
        foregroundColor: colorScheme.onSurface,
      ),
      navigationBarTheme: NavigationBarThemeData(
        backgroundColor: colorScheme.surface,
        indicatorColor: colorScheme.secondaryContainer,
      ),
      cardTheme: CardTheme(
        elevation: 2,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      ),
      inputDecorationTheme: InputDecorationTheme(
        border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
        contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
      ),
      floatingActionButtonTheme: FloatingActionButtonThemeData(
        backgroundColor: colorScheme.primaryContainer,
        foregroundColor: colorScheme.onPrimaryContainer,
      ),
      dialogTheme: DialogTheme(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      ),
      snackBarTheme: SnackBarThemeData(
        behavior: SnackBarBehavior.floating,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
      ),
    );
  }

  /// 暗色主题
  static ThemeData darkTheme(ColorScheme? dynamicColor) {
    final colorScheme = dynamicColor ?? ColorScheme.fromSeed(
      seedColor: Colors.blue,
      brightness: Brightness.dark,
    );
    return ThemeData(
      useMaterial3: true,
      colorScheme: colorScheme,
      brightness: Brightness.dark,
      appBarTheme: AppBarTheme(
        centerTitle: true,
        elevation: 0,
        backgroundColor: colorScheme.surface,
        foregroundColor: colorScheme.onSurface,
      ),
      navigationBarTheme: NavigationBarThemeData(
        backgroundColor: colorScheme.surface,
        indicatorColor: colorScheme.secondaryContainer,
      ),
      cardTheme: CardTheme(
        elevation: 2,
        color: colorScheme.surfaceVariant,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      ),
      inputDecorationTheme: InputDecorationTheme(
        border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
        contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
        filled: true,
        fillColor: colorScheme.surfaceVariant.withOpacity(0.3),
      ),
      floatingActionButtonTheme: FloatingActionButtonThemeData(
        backgroundColor: colorScheme.primaryContainer,
        foregroundColor: colorScheme.onPrimaryContainer,
      ),
      dialogTheme: DialogTheme(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      ),
      snackBarTheme: SnackBarThemeData(
        behavior: SnackBarBehavior.floating,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
      ),
    );
  }
}

/// 壁纸背景装饰盒（用于主屏幕背景覆盖）
class WallpaperBackground extends StatelessWidget {
  final Widget child;
  final String? wallpaperPath;
  final double blurRadius;
  final double opacity;

  const WallpaperBackground({
    super.key,
    required this.child,
    this.wallpaperPath,
    this.blurRadius = 25.0,
    this.opacity = 0.3,
  });

  @override
  Widget build(BuildContext context) {
    if (wallpaperPath == null) return child;

    return Stack(
      children: [
        // 背景壁纸
        Positioned.fill(
          child: Image.file(
            File(wallpaperPath!),
            fit: BoxFit.cover,
            color: Colors.black.withOpacity(1.0 - opacity),
            colorBlendMode: BlendMode.darken,
          ),
        ),
        // 模糊层
        Positioned.fill(
          child: BackdropFilter(
            filter: ImageFilter.blur(sigmaX: blurRadius, sigmaY: blurRadius),
            child: Container(color: Colors.transparent),
          ),
        ),
        // 内容
        child,
      ],
    );
  }
}
