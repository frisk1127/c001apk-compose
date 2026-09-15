package com.example.c001apk.compose.logic.providable

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * 置为 true 时，容器内正文的文本控件会把拖拽手势交还给父级，位移距离由父级
 * 按垂直分量决定，不要求手势足够竖直。
 *
 * 只应提供给本身没有横向手势语义的容器（如评论详情 bottom sheet），
 * 否则会吃掉该容器内正常的横向交互。
 */
val LocalDragHandoffToParent = staticCompositionLocalOf { false }
