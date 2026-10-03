# 开发进度

## 已完成

- 检查项目目录与现存文件。
- 阅读 `project.md` 并核对 Android 官方 AlarmManager、精确闹钟、通知权限与广播文档。
- 完成初步项目状态分析和架构提案，写入 `docs/architecture.md`。
- 用户确认响铃需要持续播放，体验接近系统闹钟；系统无法设置特定年月日的一次性 alarm 后，授权采用本 App 实现响铃。
- 建立单模块 Android Gradle 工程骨架，并实现纯 Kotlin 月末周六计算器与日期单元测试。

## 当前状态

- 已有 Android 工程配置、日期算法与测试源码、SharedPreferences 设置存储、下一次触发时间计算、AlarmManager 调度、闹钟 receiver、响铃 Service、响铃界面和基础首页。
- 首页包含启用开关、日期预览、时间/震动/标签编辑，以及精确闹钟、通知、全屏界面权限状态与系统设置入口。
- 闹钟触发后立即登记下个月 occurrence，并在闹钟月份 token 中保留触发日期，防止时钟回拨造成当月重复调度。
- 通知权限或应用闹钟通知渠道不可用时不登记持续响铃，避免响铃后没有可见的停止入口；关闭总开关会取消月度闹钟和稍后提醒并停止当前播放。
- XML 资源和清单已通过本地解析，代码引用的 app 字符串资源完整。
- 首页已从平台原生 Switch/按钮迁移到 Kotlin Jetpack Compose + Material 3，加入动态配色、卡片式日期概览/未来安排/设置行、标签对话框和权限卡片。
- Android 16 修复方向：targetSdk 36 强制 edge-to-edge，Compose Scaffold 使用 safeDrawing insets，首页开关不再绘制在系统状态栏下。
- Compose compiler plugin 与 Kotlin Gradle plugin 统一为 2.4.20；Material 3 选 API 36 兼容稳定版。
- 核心触发链路和配置首页代码已落地，尚未编译或在设备验证。
- Compose UI 依赖固定为兼容 compileSdk 36 / 当前 AGP 的稳定版本；需在用户 Android 16 设备重新运行确认布局。
- 工具环境未安装 Java/JDK、Gradle 或 Android SDK，当前无法运行 Gradle 测试或构建；没有生成 Gradle wrapper。
- 架构提案已按用户选择更新：系统 AlarmClock Intent 不支持指定年月日，因此使用 `setAlarmClock()`，到点后用仅响铃期间运行的前台媒体播放 Service 循环播放铃声。

## 下一步

1. 在具备 JDK 17 和 Android SDK 36 的环境生成/加入 Gradle wrapper 并验证工程配置与日期测试。
2. 处理编译发现的问题，验证开关、修改时间、关闭、重启、时区切换、Doze 和锁屏响铃。
3. 补充真实设备/厂商后台策略验证，并按结果完善 README。

## 已知问题

- 系统时钟 `ACTION_SET_ALARM` 没有年月日参数，不能为每个月设置特定日期的一次性闹钟。
- Android 14+ 全屏响铃需要全屏通知访问，并受闹钟应用资格和应用商店政策限制；用户关闭权限时无法保证锁屏全屏界面。
- 闹钟声音需要一个仅在响铃期间活动的播放 Service；它不常驻运行。
- 闹钟只在精确闹钟访问和通知可用时登记，若通知权限/渠道后续被关闭，触发时不会启动持续响铃，避免没有可见停止入口。
- 当前执行环境没有 `java`、Gradle 或 Android SDK，无法运行构建和测试；也没有生成 Gradle wrapper。

## 技术决策

- 采用 Kotlin、单 app module、Jetpack Compose + Material 3、纯 Kotlin 日期算法和 SharedPreferences。
- 采用 `setAlarmClock()` 注册单个未来一次性 Alarm，在触发/重启/时区或系统时间变化时重算。
- 采用响铃期间运行的媒体播放前台 Service 和闹钟全屏通知；Android 13+ 通知权限、Android 14+ 全屏闹钟权限需检查与引导。
