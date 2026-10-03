# 月末周六自动闹钟：架构提案

> 状态：用户已确认需要持续响铃。工程骨架和日期计算已创建；系统时钟无法接受指定年月日的一次性 alarm，因此实现自有闹钟播放体验。

## 1. 项目现状

- 初始目录只有 `project.md`，没有 Android 工程、`README.md`、测试或有效 Git 历史。
- Gradle / Android Gradle Plugin、Kotlin / Java、compileSdk、minSdk、targetSdk 均不存在，无法报告现有版本。
- 因此目前没有既定技术栈或已有功能需要保留。工程创建时应选当时稳定且相互兼容的 Android 工具链，并在 README 和后续进度中记录实际版本。
- 已创建工程骨架：Kotlin、Android Gradle Plugin 8.13.2、Kotlin Gradle plugin 2.4.20、compileSdk/targetSdk 36、minSdk 26。选择 Android 8.0 作为最低版本，可直接使用 `java.time` 且避免额外的 core library desugaring 依赖。构建仍需 JDK 17 和 Android SDK 36。

## 2. 建议的工程结构

单 Android app module，按职责分包，不引入多模块：

```text
app/src/main/java/<package>/
  MainActivity                 首页与权限状态呈现
  ui/                          首页界面
  settings/                    配置读取和保存
  date/LastSaturdayCalculator  纯 Kotlin 月末周六计算
  alarm/AlarmScheduler         AlarmManager 封装
  alarm/AlarmReceiver          到点处理并安排下一次
  alarm/BootReceiver           重启、时区和时钟变更后的恢复
app/src/test/                  纯 JVM 日期算法测试
docs/architecture.md
docs/progress.md
README.md
```

采用 Kotlin。首页使用 Jetpack Compose 与 Material 3，系统闹钟、存储、响铃组件也都使用 Kotlin。Android 15/16 强制 edge-to-edge 时通过 Compose `Scaffold` 的 safe drawing insets 避免系统栏遮挡内容。业务范围小，不拆多模块或常驻后台服务。

## 3. UI 方案

单屏首页展示启用开关、下一个月末周六、未来三个月预览和设置项（时间、震动、标签）。改配置后立即保存并重新安排闹钟。权限未授予时在开关附近说明影响，并提供跳转到 Android 系统设置的按钮；用户返回后重新检查权限和安排状态。

## 4. 日期计算方案

使用 `java.time.YearMonth` / `LocalDate`：取月份最后一天，再按 `DayOfWeek` 向前偏移到最近的星期六。计算器保持纯 Kotlin，不依赖 Android Framework。下一个触发日期按设备当前本地日期与时间计算；已过时间点则选择下一个月。展示列表从下一个候选月份连续计算。

## 5. AlarmManager 方案

建议采用“一个未来的一次性 alarm”：启用后只注册最近的一次；广播触发时先基于当前配置计算并注册下一个日期，再发布本次提醒。这样不会为未来月份累积大量 alarm。使用显式 `PendingIntent`，固定 request code 和稳定 action，避免重复；取消、修改时间或关闭时用相同 `PendingIntent` 取消旧 alarm 后再按配置安排。

采用 `AlarmManager.setAlarmClock()`，因为这是面向闹钟时刻的 API，系统可见性高，Android 文档说明其精准触发且在低电量模式下会唤醒系统。它需要精确闹钟访问权限，并可能增加耗电。用 `setExactAndAllowWhileIdle()` 也可在 Doze 中精准触发，但系统可调整空闲期间的触发频率，且不像 `setAlarmClock()` 那样将下一个闹钟作为系统闹钟显示，因此不选作此应用的主方案。

触发时间按“本地年月日 + 用户设置的本地时分”构造，不保存固定 UTC 偏移。接收 `TIME_SET`、`TIMEZONE_CHANGED` 后取消旧 alarm 并按新的本地时间重新计算；日期算法自然支持跨月、跨年和闰年。夏令时不存在/重复的本地时刻由 `java.time` 的默认解析规则处理，并在实现与测试中明确验证。关机期间无法触发；设备启动后恢复最近一次。

## 6. 系统时钟集成与持续响铃

用户要求持续响铃，体验接近系统自带闹钟；优先考虑调用系统时钟 App，无法表达需求时由本 App 实现闹钟播放。

Android 提供 `AlarmClock.ACTION_SET_ALARM` 与时分、标签、震动、响铃、每周重复日等参数，可请求系统时钟 App 创建闹钟；但该接口没有指定年月日的参数，无法表示“2026-10-31 07:30 这一个月末周六”。设成每周六会错误地每周响铃。因此不能用系统时钟接口满足此需求。

按用户已确认的 fallback，由本 App 注册最近日期的一次性精确 alarm。到点后启动一个**仅在响铃期间运行**的前台播放 Service，循环播放系统闹钟铃声；锁屏时通过全屏闹钟通知呈现停止/稍后提醒界面，用户停止或稍后提醒后停止/重设播放。该 Service 不常驻，也不在闹钟之间运行。通知式短提醒不满足用户确认的需求，不作为响铃实现。

Android 14+ 对全屏通知权限有限制：仅提供闹钟或通话功能的应用符合使用场景；应用需检查 `canUseFullScreenIntent()`，用户关闭权限时引导前往设置，并保留高优先级普通通知与响铃播放作为退化路径。Android 12+ 背景启动前台服务受限，但精确闹钟属于允许启动前台服务的例外；Service 只承担响铃期间的媒体播放。

## 7. 权限方案

- `RECEIVE_BOOT_COMPLETED`：重启后读取本地配置并恢复一次性 alarm。系统文档说明应用至少被用户启动过一次后才能收到启动广播。
- 精确闹钟：声明 `SCHEDULE_EXACT_ALARM`，启用前检查 `canScheduleExactAlarms()`；未授权时解释用途并打开“闹钟和提醒”系统设置页。授权广播到达后重新安排。Android 13+ 新安装默认可能未授予。`USE_EXACT_ALARM` 虽自动授予但受 Google Play 严格用途政策限制，不采用。
- 通知：Android 13+ 请求 `POST_NOTIFICATIONS`。未授予通知权限、应用通知被整体关闭或闹钟通知渠道被关闭时，不安排/不启动持续响铃，避免手机响起却没有可用的停止入口；首页显示状态并提供设置入口。
- 锁屏全屏响铃界面：声明并检查 `USE_FULL_SCREEN_INTENT` / `canUseFullScreenIntent()`；未授权时解释原因并提供系统设置入口。该 App 的主要功能就是闹钟，应按闹钟应用的资格使用，分发时需遵循商店政策。
- 响铃期间播放：使用媒体播放类型的短期前台 Service，并声明当前 targetSdk 所要求的前台服务权限和类型；收到停止/稍后提醒操作后立即停止，不保持常驻。
- 不申请网络、定位、存储等与功能无关权限。

## 12. 当前实现进度

- 已建立单 app module，使用 Kotlin、AGP 8.13.2、Kotlin Android plugin 2.4.20、compileSdk/targetSdk 36、minSdk 26。minSdk 26 可直接使用 `java.time`，不需要额外的 core library desugaring。
- 已有 SharedPreferences 配置仓库、下一次月末周六计算、AlarmManager 调度、闹钟 receiver、启动/时间变化恢复 receiver、通知和响铃 Service 的首版代码，以及单屏设置 UI。
- 当前开发容器没有 JDK、Gradle 或 Android SDK，不能执行 Gradle 编译、单元测试或设备验证。首个可运行构建需要先加入 Gradle wrapper，并在具备 JDK 17 和 Android SDK 36 的环境验证。
- 首页 UI 已迁移到 Compose Material 3。Kotlin/Compose compiler plugin 统一为 2.4.20；Compose BOM 固定在 2026.06.01、Material 3 固定在 1.3.2，保持 compileSdk 36 / AGP 8.13 兼容，不采用需要 API 37 和 AGP 9.2+ 的 Compose 1.12 系列。

## 8. 重启和系统事件恢复

manifest receiver 处理 `BOOT_COMPLETED`、`TIME_SET`、`TIMEZONE_CHANGED`；精确闹钟授权变化广播也触发重算。恢复流程读取启用状态和闹钟时间，若未启用则取消；启用且权限可用则取消已有同身份 alarm 并只注册下一次。无需后台常驻进程。用户强行停止应用后，Android 会暂停其广播/任务，通常需再次手动打开应用才会恢复；此限制应记入 README。

## 9. 数据存储

四项配置 `enabled`、`alarmTime`、`vibrate`、`label` 使用原生 `SharedPreferences`。数据量很小且更新简单，不为此引入数据库或额外异步存储依赖。默认值：关闭、07:30、震动开启、“月末周六加班”。

## 10. 测试方案

- 纯 JVM 单元测试覆盖需求文档列出的 2026-10/11/12、2027-01、28/29/30/31 天月份、闰年、月末为周六以及跨年。
- 对时区切换、本地时钟变更、配置修改/关闭、receiver 重复投递和权限拒绝做 scheduler 行为验证；通知权限/渠道不可用时不得留下无法停止的响铃；可用时再进行模拟器集成验证。
- 按项目流程，进入实现阶段后运行 `./gradlew test` 和可用的 Android 构建检查，并记录真实结果。

## 11. 官方平台依据

- [Android：Schedule alarms](https://developer.android.com/develop/background-work/services/alarms)：精确闹钟 API、Doze、精确闹钟权限、开机恢复及资源影响。
- [Android 14：Schedule exact alarms are denied by default](https://developer.android.com/about/versions/14/changes/schedule-exact-alarms)：新安装应用精确闹钟访问默认状态。
- [Android：Notification runtime permission](https://developer.android.com/develop/ui/compose/notifications/notification-permission)：Android 13+ 通知运行时权限。
- [Android：Implicit broadcast exceptions](https://developer.android.com/develop/background-work/background-tasks/broadcasts/broadcast-exceptions)：启动、系统时间和时区相关广播限制。
- [Android：AlarmClock API reference](https://developer.android.com/reference/android/provider/AlarmClock)：系统时钟交互支持时分和每周重复日，没有年月日一次性日期参数。
- [Android 14：Behavior changes](https://developer.android.com/about/versions/14/behavior-changes-14)：全屏 Intent 仅供闹钟/通话等场景，并可查询和请求用户授权。
- [Android：Foreground service types](https://developer.android.com/develop/background-work/services/fgs/service-types)：媒体播放前台服务类型。
