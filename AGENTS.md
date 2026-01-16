## 回复语言（高优先级）
默认且始终使用中文回复（包含后续新 session），除非用户明确要求使用其他语言。

# Repository Guidelines

## 项目结构与模块组织
这是一个 Android/Compose 工程，根目录包含 `settings.gradle.kts`、`build.gradle.kts`、`gradle/` 与 `gradlew`。主模块在 `app/`，源码与资源位于 `app/src/main/`。当前仓库已包含 `mojito/` 与 `SketchImageViewLoader/` 两个模块，但 `settings.gradle.kts` 仍声明了 `:coilimageLoader`，本地缺少该目录会导致编译失败。

## 构建、测试与开发命令
使用 Gradle Wrapper（在仓库根目录执行）：
- `.\gradlew assembleDebug` 生成 debug APK
- `.\gradlew assembleRelease` 生成 release APK（开启混淆与资源压缩）
- `.\gradlew test` 运行 JVM 单元测试
- `.\gradlew connectedAndroidTest` 运行仪器测试（需设备/模拟器）

## 编码风格与命名
项目使用 Kotlin 与 Java 17（见 `app/build.gradle.kts`）。建议：
- Kotlin/Java 使用 4 空格缩进
- 类名 UpperCamelCase，方法/变量 lowerCamelCase
- 资源名与 id 使用 `lowercase_with_underscores`

## 测试指南
将单元测试放在 `app/src/test/`，仪器测试放在 `app/src/androidTest/`。测试类以 `*Test` 命名，并保持与被测类的包结构一致。

## 缺失模块与获取信息建议
仓库已归档且 README 简短，建议按以下路径补全信息：
1) 搜索 Fork 或历史提交，看是否有人补齐 `coilimageLoader` 模块。
2) 检索代码中的包名 `net.mikaelzero.coilimageloader`，确认其上游仓库或 Maven 坐标。
3) 若无法获取源码，考虑将 `implementation(project(":coilimageLoader"))` 替换为远程依赖，并调整相关导入。

## 配置与安全
签名信息使用 `local.properties`（如 `KEYSTORE_PATH`、`KEYSTORE_PASSWORD` 等），不要提交 keystore 到仓库。

## 当前问题与改动记录（高优先级）

### 目标问题
- 静态图片全屏预览时长按无反应（无保存弹窗、无长按日志）。
- GIF 长按可以触发弹窗（已修复主题崩溃）。

### 关键现象
- GIF 长按日志会进入 MojitoView → SketchContentLoader → fragment long-tap → showSaveImgDialog。
- 静态图长按日志常停留在 ViewPager（或只见 DOWN/UP），未稳定进入 long-tap 链路。

### 已做的主要改动（涉及文件）
1) 主题/弹窗修复（GIF 崩溃）
- app/src/main/res/values/themes.xml、app/src/main/res/values/colors.xml、app/src/main/res/values-night/colors.xml：补充 Material 相关 colorSurface/colorOnSurface，保证 MaterialAlertDialogBuilder 不再抛错。
- app/src/main/java/com/example/c001apk/compose/util/ImageShowUtil.kt：弹窗使用 ThemeOverlay（ContextThemeWrapper）以确保颜色属性完整。

2) 本地模块切换（确保修改生效）
- settings.gradle.kts：启用本地 :mojito 与 :SketchImageViewLoader 模块。
- app/build.gradle.kts：依赖改为 implementation(project(":mojito")) / implementation(project(":SketchImageViewLoader")).

3) 日志与触摸链路排查
- mojito/src/main/java/net/mikaelzero/mojito/MojitoView.java：补充触摸/长按相关日志。
- SketchImageViewLoader/src/main/java/net/mikaelzero/mojito/view/sketch/SketchContentLoaderImpl.kt：补充 dispatchTouch/long-press 日志。
- mojito/src/main/java/net/mikaelzero/mojito/ui/ImageMojitoFragment.kt：补充长按/触摸日志与手动 long-press fallback。
- mojito/src/main/java/net/mikaelzero/mojito/tools/NoScrollViewPager.java：补充拦截/分发日志。

4) 状态栏/动画一致性
- 已调整为不强制隐藏状态栏（与原 release 行为一致，避免退出动画不匹配）。

5) 长按相关逻辑尝试
- ImageMojitoFragment.kt：
  - 手动长按 runnable + 放宽阈值（cancelSlop/fireSlop 提升至 scaledTouchSlop * 12）。
  - 将 showView(providerRealView) 的触摸事件转发给 MojitoView，并加递归保护 isForwardingToMojitoView。
  - 对非 GIF 增加根布局长按 GestureDetector fallback（只触发保存弹窗）。
- ImageMojitoActivity.kt：
  - 增加 lastGlobalLongPressTime 共享时间戳。
  - 增加 Android 13+ Back callback，确保返回行为正常。
  - 在 ViewPager 上加入手动长按检测（非 GIF 时触发保存弹窗）。
- NoScrollViewPager.java：
  - ACTION_DOWN 直接放行（onInterceptTouchEvent 返回 false），让子 View 更容易拿到 DOWN。

### 当前未解决的问题
- 静态图长按仍不稳定/无反应；日志显示静态图多数情况下只到 ViewPager（DOWN/UP），未进入 fragment long-tap。
- GIF 长按稳定触发保存弹窗，说明主题与弹窗逻辑已正常。

### 当前构建状态
- .\gradlew assembleDebug --warning-mode all 可通过（仅有 Gradle 弃用警告）。

### 当前需要继续排查的关键点
- 静态图触摸事件是否始终未进入 SketchImageView 的 onLongTap 链路，或被 ViewPager/Sketch 拦截。
- MojitoView/SketchContentLoader 对静态图的 isDrag/zoomScale 判定是否导致长按被取消。

### 现状更新（最新总结）
- 当前主要问题：
  1) 静态图片全屏预览无法使用任何手势（长按/双击/拖拽缩放/拖动返回均无效）。
  2) GIF 全屏预览左右滑动切换曾因触摸转发改动失效；相关改动已回退，但尚未重新验证恢复情况。
  3) 长按弹窗缺少 Material 取色效果（当前使用固定 color_surface/color_on_surface）。
  4) GIF 保存结果为静态 JPEG（即便文件名为 .gif），原始 GIF 数据未保存。
- 最近一次“触摸转发 + Material3 overlay + GIF 原始保存”的改动已全部回退；当前仍保留日志与部分长按兜底逻辑。
