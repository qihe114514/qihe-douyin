package com.douyin.downloader.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Liquid Glass 调色板 — 半透明基调 + 虹彩色
private val LiquidLightColorScheme = lightColorScheme(
    primary = Color(0xFF6C5CE7),               // 柔紫
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEEECFF),
    onPrimaryContainer = Color(0xFF1A1050),
    secondary = Color(0xFF00CECE),             // 青绿
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD5F5F5),
    onSecondaryContainer = Color(0xFF003030),
    tertiary = Color(0xFFFF6B8A),              // 粉红
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE0E8),
    onTertiaryContainer = Color(0xFF4D0018),
    background = Color(0xFFF2EFFC),            // 浅紫白底
    onBackground = Color(0xFF1A1050),
    surface = Color(0xAAFFFFFF),               // 半透明白
    onSurface = Color(0xFF1A1050),
    surfaceVariant = Color(0x99EDE9F5),
    onSurfaceVariant = Color(0xFF4A3F6B),
    outline = Color(0xFFB0A8D0),
    outlineVariant = Color(0xFFD5D0EA)
)

private val LiquidDarkColorScheme = darkColorScheme(
    primary = Color(0xFFB5A5FF),               // 浅紫
    onPrimary = Color(0xFF1A1050),
    primaryContainer = Color(0xFF3B2E80),
    onPrimaryContainer = Color(0xFFEEECFF),
    secondary = Color(0xFF6BF5F5),             // 亮青
    onSecondary = Color(0xFF003030),
    secondaryContainer = Color(0xFF004D4D),
    onSecondaryContainer = Color(0xFFD5F5F5),
    tertiary = Color(0xFFFF9EB5),              // 浅粉
    onTertiary = Color(0xFF4D0018),
    tertiaryContainer = Color(0xFF6B2035),
    onTertiaryContainer = Color(0xFFFFE0E8),
    background = Color(0xFF0F0A1E),            // 深紫黑底
    onBackground = Color(0xFFE5E0F5),
    surface = Color(0xAA1A1533),               // 半透明深紫
    onSurface = Color(0xFFE5E0F5),
    surfaceVariant = Color(0x992A2445),
    onSurfaceVariant = Color(0xFFB0A8D0),
    outline = Color(0xFF6A5F90),
    outlineVariant = Color(0xFF3B3260)
)

@Composable
fun DouyinDownloaderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> LiquidDarkColorScheme
        else -> LiquidLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
