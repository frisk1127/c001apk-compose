package com.example.c001apk.compose.ui.component

import android.view.ViewTreeObserver
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.RippleDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalView

/**
 * 抑制 Material3 ripple 的 focus 状态层。
 *
 * 背景：M3 会为 Focus 画一层 10% 的状态层，这是无障碍规范要求的「焦点可见」，
 * 本身没有错。但 Compose 的 focus 系统缺了 Android 原生 View 那层 touch mode 保护
 * —— 原生 View 在 touch mode 下根本不会进入 focused 状态，而 Compose 里任何一次
 * requestFocus 成功都会停在 Focus 状态，于是这层外设专用的反馈泄漏到了纯触摸场景，
 * 表现就是左上角元素看起来「一直按着」。实测那层高亮的亮度增量是 +14，反解
 * alpha≈0.10，正是 Focused token（Hovered 是 0.08，对不上），所以只清 focus。
 *
 * ⚠️⚠️ 这里的 RippleConfiguration 必须是**恒定实例**，绝对不能随任何状态切换。⚠️⚠️
 *
 * material3 1.3.0（compose-bom 2024.09.01）的 DelegatingThemeAwareRippleNode 实现了
 * ModifierLocalConsumer：只要 LocalRippleConfiguration 在运行中变化一次，就会触发
 * updateConfiguration()，而它在 R8 之后必然抛：
 *
 *     java.lang.IllegalStateException: Could not find delegate: AndroidRippleNode@...
 *       at androidx.compose.ui.node.DelegatingNode.undelegate()
 *       at DelegatingThemeAwareRippleNode.updateConfiguration()
 *       at BackwardsCompatNodeKt$updateModifierLocalConsumer$1.invoke()
 *       at androidx.compose.runtime.Recomposer.applyAndCheck()
 *
 * 同构案例见 https://stackoverflow.com/q/78974733 —— 同样是 BOM 2024.09.01 + material3
 * 1.3.0，同样是「在某个值和 null 之间条件切换 LocalRippleConfiguration」，一样崩。
 *
 * 所以「触摸模式下不显示焦点框、外设模式下正常显示」这种动态切换，在当前依赖版本下
 * 做不到，只能全局静态关闭。想恢复这个能力需要升级 material3（该 bug 在更新的
 * material3 里已修），那属于依赖升级，需单独评估。
 *
 * 静态关闭其实也贴近 Android 原生语义 —— 原生 View 在 touch mode 下本来就不显示焦点框。
 * 代价只有键盘 / 手柄用户看不到焦点框，对本项目（纯触摸手机端）实际影响很小。
 *
 * 只动 focusedAlpha：press(0.10)、drag(0.16)、hover(0.08) 全部原样保留，
 * 按压 / 滑动反馈一点没变，接鼠标时 hover 也照旧。
 * 要回退：删掉 Theme.kt 里的这层包裹即可。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuppressFocusStateLayer(content: @Composable () -> Unit) {
    // remember 不带 key —— 保证实例恒定，永不触发上面那条 updateConfiguration 路径。
    val rippleConfiguration = remember {
        val base = RippleDefaults.RippleAlpha
        RippleConfiguration(
            rippleAlpha = RippleAlpha(
                draggedAlpha = base.draggedAlpha,
                focusedAlpha = 0f,
                hoveredAlpha = base.hoveredAlpha,
                pressedAlpha = base.pressedAlpha,
            )
        )
    }
    CompositionLocalProvider(
        LocalRippleConfiguration provides rippleConfiguration,
        content = content,
    )
}

/**
 * 清掉「窗口重新获得焦点」时被 Compose 自动补上的假焦点。
 *
 * 触发链路：本项目用 ActivityResultLauncher 打开半透明的 ReplyActivity，底下 Activity
 * 的窗口会先失去焦点、返回时再重新获得焦点，此时 Compose 会把焦点交给视图树里第一个
 * 可聚焦节点 —— 详情页是左上角返回按钮，首页是第一个 Tab。该节点随后一直保持 Focus。
 *
 * 与 SuppressFocusStateLayer 的分工：那个负责「高亮画不画」，这个负责让 FocusOwner
 * 的状态保持干净（焦点不该停在这些节点上，否则键盘导航的起点也会跟着跑偏）。
 *
 * 只在触摸输入下清：键盘用户按方向键离开再回来时，焦点是真实且需要保留的，
 * 无条件清掉会让他们丢失导航位置。这里读 inputMode 发生在组合之外（回调里），
 * 不会订阅状态，因此不会引入额外的重组，也不涉及上面那个 CompositionLocal 的坑。
 *
 * 输入框的焦点都是自己 requestFocus 的，且 IME 弹出不会让本窗口失焦，
 * 所以正常输入不受影响。
 */
@Composable
fun SuppressWindowFocusHighlight() {
    val view = LocalView.current
    val focusManager = LocalFocusManager.current
    val inputModeManager = LocalInputModeManager.current
    DisposableEffect(view, focusManager, inputModeManager) {
        val listener = ViewTreeObserver.OnWindowFocusChangeListener { hasFocus ->
            if (hasFocus && inputModeManager.inputMode == InputMode.Touch) {
                view.post { focusManager.clearFocus(force = true) }
            }
        }
        view.viewTreeObserver.addOnWindowFocusChangeListener(listener)
        onDispose { view.viewTreeObserver.removeOnWindowFocusChangeListener(listener) }
    }
}
