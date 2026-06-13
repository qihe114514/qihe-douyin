# 抖音无水印视频下载器 v2.0 🎉

基于 [BugPk-Api](https://api.bugpk.com) 提供的抖音无水印解析服务，支持**视频、图集、实况（Live Photo）** 的解析与下载。采用 **Jetpack Compose + Material 3** 构建，适配 **Android 16（API 36）**，原生沉浸式体验。

## 📦 最新版本

**v2.0** (`versionCode=5`) · 包名 `com.douyin.downloaderqh` · Release 签名

👉 [**下载最新 APK**](https://github.com/qihe114514/qihe-douyin/actions) — 选择最新 **Build APK** → 下载 `douyin-downloader-release`

## ✨ 核心功能

- 🔗 **链接解析** — 粘贴抖音分享链接，一键解析无水印视频/图集/实况
- 📹 **视频信息展示** — 分辨率、帧率、码率、编码格式等可读属性
- ⬇️ **批量下载** — 主视频、备份画质、多图集、实况视频并行下载
- 📊 **实时进度** — 圆形进度条 + 百分比 + 实时网速（纯 suspend + onProgress 回调，彻底避免卡顿）
- 📳 **震动反馈** — 点击下载 30ms 轻触震动
- 🖼️ **相册入库** — MediaStore 写入系统相册（视频→Movies/Douyin，图片→Pictures/Douyin）
- 🎨 **Material You 动态取色** — 跟随系统壁纸自动适配主题（Android 12+）
- 🌄 **背景壁纸** — 图片/视频背景，可调节模糊度和透明度
- 📜 **解析历史** — 自动记录链接，可回溯重解析
- ⚙️ **自定义设置** — 保存路径、声音开关、深色模式等
- 📱 **沉浸式 UI** — Material 3 蓝紫主题，列表淡入滑入动画，进度平滑过渡

## 🛠️ 技术栈

| 组件 | 版本 | 说明 |
|------|------|------|
| **语言** | Kotlin 2.1.0 | — |
| **UI 框架** | Jetpack Compose + Material 3 | BOM 2024.12.01 |
| **导航** | Navigation Compose 2.8.5 | 页面切换淡入淡出动画 |
| **网络** | OkHttp 4.12.0 | API 客户端 + 流式下载 |
| **序列化** | kotlinx.serialization 1.7.3 | JSON 解析 |
| **图片加载** | Coil 3.1.0 | 异步加载 |
| **视频播放** | Media3 ExoPlayer 1.4.0 | 背景视频播放 |
| **本地存储** | DataStore Preferences 1.1.2 | 设置及历史持久化 |
| **构建** | Gradle 8.12 · AGP 8.7.2 | — |
| **编译目标** | Android 16 (API 36) | targetSdk / minSdk = 36 |

## 🚀 构建与安装

### 📲 从 GitHub Actions 直接下载

1. 打开 [Actions 页面](https://github.com/qihe114514/qihe-douyin/actions)
2. 点击最新 **Build APK** 运行（确保是 ✅ 绿色成功）
3. 在 **Artifacts** 中下载 `douyin-downloader-release`
4. ⚠️ 安装前**务必卸载旧版**（包名已变），否则签名冲突

### 🔨 本地编译

```bash
git clone https://github.com/qihe114514/qihe-douyin.git
cd qihe-douyin
./gradlew assembleRelease
# 产物：app/build/outputs/apk/release/app-release.apk
```

## 📁 项目结构

```
app/src/main/java/com/douyin/downloaderqh/
├── MainActivity.kt              # 入口 + NavHost
├── api/
│   └── DouyinApiClient.kt       # BugPk API 客户端
├── data/
│   └── SettingsDataStore.kt     # DataStore 持久化（含历史）
├── download/
│   └── DownloadManager.kt       # suspend + onProgress 流式下载 → MediaStore
├── model/
│   └── Models.kt               # API 数据模型 + 下载项提取
└── ui/
    ├── MainViewModel.kt         # 状态管理 + 震动反馈
    ├── screens/
    │   ├── MainScreen.kt        # 主界面
    │   ├── SettingsScreen.kt    # 设置页
    │   └── HistoryScreen.kt     # 解析历史
    └── theme/
        └── Theme.kt            # Material You 主题 + 沉浸式状态栏
```

## 🏗️ 迭代历程

| 版本 | 关键进展 |
|------|------|
| v1.0 | 项目搭建、API 集成、基础 UI、Gradle 构建 |
| v1.1 | 解析历史、下载进度/网速、震动反馈、粘贴按钮、视频背景 |
| v1.2 | Material You 动态取色、模糊度/透明度调节、文件属性可读显示、滚动优化 |
| v1.3 | 下载器重写：OkHttp 流式 → MediaStore 相册入库，防 OOM，实时进度 |
| v1.5 | 🎨 全面美化：矢量图标、蓝紫主题、沉浸状态栏、列表淡入、进度平滑过渡 |
| v2.0-beta | 🚀 包名升级 `com.douyin.downloaderqh`，全新定制图标，正式版发布 |
| **v2.0** | 💎 **下载器稳定终极版**：抛弃 callbackFlow/awaitClose 实验性 API，纯 `suspend` + `onProgress` 回调，彻底根除大文件卡死/闪退 |

## 🙋 关于

- **开发者**：其核 [@qihe114514](https://github.com/qihe114514)
- **B站**：[https://space.bilibili.com/1049283248](https://space.bilibili.com/1049283248)
- **抖音**：[https://www.douyin.com/user/MS4wLjABAAAAuUtKOArTFKTBm4C6o5MwDQuGMNZ9-0CWZfUay6U9wUI](https://www.douyin.com/user/MS4wLjABAAAAuUtKOArTFKTBm4C6o5MwDQuGMNZ9-0CWZfUay6U9wUI)
- **API 服务**：[BugPk-Api](https://api.bugpk.com)

## 📄 许可证

仅供学习交流使用，请勿用于商业或违规用途。视频版权归原作者及抖音平台所有。