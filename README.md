# 月末周六闹钟

一个离线运行的 Android 闹钟应用，自动计算每个月最后一个周六，并在指定时间响铃。

应用面向需要每月固定提醒、但不想手动重复设置闹钟的场景。所有配置保存在本地，不需要账号、网络或云同步。

## 功能

- 自动计算每个月最后一个周六
- 自定义响铃时间，默认 `07:30`
- 开启/关闭月末周六闹钟
- 系统默认闹钟铃声循环播放
- 可选震动
- 自定义闹钟标签
- 预览未来三个月的闹钟安排
- 闹钟响铃时支持停止和稍后提醒
- 手机重启、时区变化和系统时间变化后自动重新安排
- 首次打开时引导完成必要权限设置
- 完全离线运行，不申请网络、定位、存储或账号权限

## 权限说明

应用会根据 Android 版本检查以下权限：

- Android 12+：精确闹钟权限，用于按指定时间触发
- Android 13+：通知权限，用于显示闹钟状态和锁屏提醒
- Android 14+：全屏通知权限，用于锁屏时显示响铃界面
- 开机广播权限，用于重启后恢复闹钟
- 前台媒体播放服务权限，仅在响铃期间播放声音

首次打开应用时会依次引导授权。权限未完成前不会结束首次授权流程；之后可以在首页的权限状态区域重新打开系统设置。

## 技术栈

- Kotlin
- Jetpack Compose
- Material 3
- Android Gradle Plugin `9.4.1`
- Kotlin `2.4.20`
- Compose BOM `2026.06.01`
- `compileSdk 36`
- `targetSdk 36`
- `minSdk 26`
- JDK 17+

## 项目结构

```text
app/src/main/java/com/example/saturdayalarm/
├── alarm/       AlarmManager、广播接收器和响铃服务
├── date/        月末周六日期计算
├── settings/    本地设置存储
├── ui/          Compose 页面和主题
└── MainActivity.kt

app/src/test/    日期计算单元测试
docs/            架构和开发进度文档
```

核心调度逻辑使用“一次性闹钟 + 触发后计算下一次日期”的方式，避免把月末周六错误实现成每周重复闹钟。

## 构建

需要安装：

- Android Studio
- JDK 17 或更高版本
- Android SDK 36

在项目根目录执行：

```bash
./gradlew test
./gradlew assembleDebug
```

Windows PowerShell：

```powershell
.\gradlew.bat test
.\gradlew.bat assembleDebug
```

Debug APK 输出路径：

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 验证建议

建议至少验证以下场景：

1. 首次打开并完成权限引导
2. 开启和关闭闹钟
3. 修改时间和标签
4. 跨月、跨年和闰年日期计算
5. 手机重启后闹钟恢复
6. 时区或系统时间变化后重新安排
7. 通知权限、通知渠道或全屏权限被关闭时的降级行为
8. 响铃界面的停止和稍后提醒

## 文档

- [架构说明](docs/architecture.md)
- [开发进度](docs/progress.md)
- [项目需求与设计约束](project.md)

## 已知限制

- Android 厂商的后台限制和电池优化策略可能影响开机恢复或后台调度，需要在真实设备上验证。
- Android 14+ 的全屏通知权限受系统策略和应用分发渠道限制；未授权时仍会使用普通高优先级通知和响铃服务。
- 应用只安排最近的一次闹钟，并在触发后计算下一次日期，不会预先创建大量系统闹钟。

## License

当前项目尚未指定开源许可证。如需公开发布，建议根据项目用途补充合适的 License 文件。
