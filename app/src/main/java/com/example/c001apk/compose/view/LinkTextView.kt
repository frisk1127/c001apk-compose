package com.example.c001apk.compose.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.text.Selection
import android.text.Spannable
import android.text.method.LinkMovementMethod
import android.text.method.Touch
import android.text.style.ClickableSpan
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.TextView
import kotlin.math.abs
import com.example.c001apk.compose.dev.DevLog
import com.example.c001apk.compose.util.SpannableStringBuilderUtil

private const val TAG = "LinkDrag"

//https://stackoverflow.com/questions/8558732
class LinkTextView : androidx.appcompat.widget.AppCompatTextView {

    override fun getHighlightColor(): Int {
        return Color.TRANSPARENT
    }

    private var dontConsumeNonUrlClicks = true
    var linkHit = false
    private var downX = 0f
    private var downY = 0f
    private var dragHandedOff = false

    private val touchSlop by lazy { ViewConfiguration.get(context).scaledTouchSlop }

    /**
     * 置为 true 后本控件不再消费任何触摸事件，手势完整交给父级，斜向滑动也能
     * 驱动父级滚动。链接点击不受影响——它由 movementMethod 在 ACTION_UP 触发，
     * 与 onTouchEvent 的返回值无关。
     */
    var dragHandoffToParent = false
        set(value) {
            if (field != value) {
                DevLog.d(TAG) { "dragHandoffToParent: $field -> $value" }
                field = value
            }
        }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) :
            super(context, attrs, defStyleAttr)

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                dragHandedOff = false
            }

            MotionEvent.ACTION_MOVE -> {
                if (dragHandoffToParent && !dragHandedOff &&
                    (abs(event.x - downX) > touchSlop || abs(event.y - downY) > touchSlop)
                ) {
                    dragHandedOff = true
                }
            }

            MotionEvent.ACTION_UP -> {
                // 已经当作拖拽出手势了，抬起时不要再触发链接点击
                if (dragHandedOff) {
                    dragHandedOff = false
                    linkHit = false
                    return false
                }
            }
        }
        linkHit = false
        val res = super.onTouchEvent(event)
        val consumed = if (dragHandoffToParent) {
            // DOWN / MOVE 一律不消费：Compose 的 interop 一旦见本控件返回过 true，
            // 就会把之后每个事件都标记成已消费，外层列表 / 弹层再也拿不到手势。
            // UP 例外 —— 命中链接时照旧消费，保留「点链接不冒泡到所在容器」的原设计。
            event.actionMasked == MotionEvent.ACTION_UP && linkHit
        } else {
            if (dontConsumeNonUrlClicks) linkHit else res
        }
        DevLog.d(TAG) {
            "action=${event.actionMasked} handoff=$dragHandoffToParent linkHit=$linkHit " +
                    "draggedOff=$dragHandedOff consumed=$consumed"
        }
        return consumed
    }

    class LocalLinkMovementMethod(private val isReply: Boolean) : LinkMovementMethod() {
        override fun onTouchEvent(
            widget: TextView,
            buffer: Spannable, event: MotionEvent
        ): Boolean {
            val action = event.action
            if (action == MotionEvent.ACTION_UP ||
                action == MotionEvent.ACTION_DOWN
            ) {
                var x = event.x.toInt()
                var y = event.y.toInt()
                x -= widget.totalPaddingLeft
                y -= widget.totalPaddingTop
                x += widget.scrollX
                y += widget.scrollY
                val layout = widget.layout
                val isOutOfLineBounds: Boolean = if (y < 0 || y > layout.height) {
                    true
                } else {
                    val line = layout.getLineForVertical(y)
                    (x < layout.getLineLeft(line) || x > layout.getLineRight(line))
                }
                if (isOutOfLineBounds) {
                    Selection.removeSelection(buffer)
                    return Touch.onTouchEvent(widget, buffer, event)
                }
                val line = layout.getLineForVertical(y)
                val off = layout.getOffsetForHorizontal(line, x.toFloat())
                val link = buffer.getSpans(
                    off, off, ClickableSpan::class.java
                )
                if (link.isNotEmpty()) {
                    if (action == MotionEvent.ACTION_UP) {
                        link[0].onClick(widget)
                    } else {
                        /*Selection.setSelection(
                            buffer,
                            buffer.getSpanStart(link[0]),
                            buffer.getSpanEnd(link[0])
                        )*/
                    }
                    val linkText =
                        buffer.substring(buffer.getSpanStart(link[0]), buffer.getSpanEnd(link[0]))
                    if (widget is LinkTextView) {
                        widget.linkHit = if (isReply) true else linkText != "查看更多"
                    }
                    return true
                } else {
                    Selection.removeSelection(buffer)
                    return Touch.onTouchEvent(widget, buffer, event)
                }
            }
            return super.onTouchEvent(widget, buffer, event)
        }

        companion object {
            private var sInstance: LocalLinkMovementMethod? = null
            private var rInstance: LocalLinkMovementMethod? = null
            val instance: LocalLinkMovementMethod?
                get() {
                    if (sInstance == null) sInstance = LocalLinkMovementMethod(false)
                    return sInstance
                }

            val instanceR: LocalLinkMovementMethod?
                get() {
                    if (rInstance == null) rInstance = LocalLinkMovementMethod(true)
                    return rInstance
                }
        }
    }

    fun setParams(
        isReply: Boolean = false,
        size: Float,
        fontScale: Float,
    ) {
        movementMethod =
            if (isReply) LocalLinkMovementMethod.instanceR else LocalLinkMovementMethod.instance
        setTextSize(TypedValue.COMPLEX_UNIT_SP, size * fontScale)
    }


    fun setSpText(
        text: String,
        color: Int,
        onShowTotalReply: (() -> Unit)? = null,
        onOpenLink: (String, String?) -> Unit,
        onShowImages: (String) -> Unit,
    ) {
        val spText = SpannableStringBuilderUtil.setText(
            context,
            text,
            textSize,
            color,
            onShowTotalReply,
            onOpenLink,
            onShowImages,
        )
        setText(spText)
    }

}
