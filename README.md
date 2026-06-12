# 抖音无水印视频下载器

基于 [BugPk-Api](https://api.bugpk.com) 提供的抖音无水印解析服务，支持**视频、图集、实况（Live Photo）** 的解析与下载。采用 **Jetpack Compose + Material 3** 构建，适配 **Android 16（API 36）**，原生沉浸式体验。

## ✨ 功能

- **链接解析** — 粘贴抖音分享链接，一键解析出无水印视频/图集/实况
- **批量下载** — 支持主视频、备份画质、多图集、实况视频同时下载
- **保存到相册** — 通过 `MediaStore` 写入系统相册（视频存 Movies/Douyin，图片存 Pictures/Douyin）
- **自定义保存路径** — 设置中可选择其他存储目录
- **背景壁纸** — 支持图片/视频作为主页背景，可调节模糊度和透明度
- **Material You 动态取色** — 跟随系统壁纸自动适配主题色（Android 12+）

## 📱 截图

| 主页 | 解析结果 | 设置 |
|------|----------|------|
| ![主页](screenshots/main.png) | ![解析](screenshots/result.png) | ![设置](screenshots/settings.png) |

## 🛠️ 技术栈

| 组件 | 说明 |
|------|------|
| **语言** | Kotlin |
| **UI 框架** | Jetpack Compose + Material 3 (Material You) |
| **网络** | OkHttp + kotlinx.serialization |
| **图片加载** | Coil 3 |
| **本地存储** | DataStore Preferences |
| **多媒体** | Media3 ExoPlayer |
| **构建** | Gradle 8.12, AGP 8.7.2 |
| **API** | BugPk-Api (https://api.bugpk.com/api/douyin) |

## 🚀 构建与安装

### 方式一：从 GitHub Actions 下载预构建 APK

前往 [Actions 页面](https://github.com/qihe114514/qihe-douyin/actions)，选择最新的 **Build APK** 运行，下载 `douyin-downloader-debug` 工件。

### 方式二：本地编译

```
# 克隆仓库
git clone https://github.com/qihe114514/qihe-douyin.git
cd qihe-douyin
git checkout douyin

# 用 Android Studio 打开，Sync Gradle，然后 Run 'app'
# 或使用命令行（需配置 Android SDK）：
./gradlew assembleDebug
```

构建产物位于 `app/build/outputs/apk/debug/app-debug.apk`。

## 📁 项目结构

```
├── app/src/main/java/com/douyin/downloader/
│   ├── MainActivity.kt           # 入口 Activity
│   ├── MainApplication.kt        # Application
│   ├── api/
│   │   └── DouyinApiClient.kt    # BugPk API 客户端
│   ├── data/
│   │   └── SettingsDataStore.kt  # DataStore 持久化设置
│   ├── download/
│   │   └── DownloadManager.kt    # 文件下载（MediaStore）
│   ├── model/
│   │   └── Models.kt            # API 数据模型 + 下载项提取
│   └── ui/
│       ├── MainViewModel.kt      # 状态管理
│       ├── screens/
│       │   ├── MainScreen.kt     # 主界面
│       │   └── SettingsScreen.kt # 设置页
│       └── theme/
│           └── Theme.kt         # Material You 动态主题
├── app/build.gradle.kts          # 模块构建配置
├── build.gradle.kts              # 根构建配置
├── settings.gradle.kts
└── .github/workflows/build.yml   # GitHub Actions 自动构建
```

## 📄 许可证

仅供学习交流使用，请勿用于商业或违规用途。视频版权归原作者及抖音平台所有。

---
Powered by [BugPk-Api](https://api.bugpk.com) | © 2025
