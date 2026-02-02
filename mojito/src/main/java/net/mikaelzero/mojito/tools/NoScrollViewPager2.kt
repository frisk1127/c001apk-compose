package net.mikaelzero.mojito.tools

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import net.mikaelzero.mojito.ui.ImageMojitoActivity
import kotlin.math.hypot
import kotlin.math.max

class NoScrollViewPager2 @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {

    val viewPager: ViewPager2 = ViewPager2(context)
    private var isLocked = false
    private var longPressTimeout = 0
    private var cancelSlop = 0
    private var longPressActive = false
    private var downX = 0f
    private var downY = 0f

    private val longPressRunnable = Runnable {
        if (!longPressActive) return@Runnable
        longPressActive = false
        (context as? ImageMojitoActivity)?.tryDispatchLongPress(downX, downY)
    }

    init {
        viewPager.layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT
        )
        if (viewPager.id == View.NO_ID) {
            viewPager.id = View.generateViewId()
        }
        addView(viewPager)
        initTouchConfig()
    }

    private fun initTouchConfig() {
        val config = ViewConfiguration.get(context)
        longPressTimeout = ViewConfiguration.getLongPressTimeout()
        cancelSlop = max(1, config.scaledTouchSlop / 2)
    }

    var adapter: RecyclerView.Adapter<*>?
        get() = viewPager.adapter
        set(value) {
            viewPager.adapter = value
        }

    var currentItem: Int
        get() = viewPager.currentItem
        set(value) {
            viewPager.setCurrentItem(value, false)
        }

    fun setCurrentItem(item: Int, smoothScroll: Boolean) {
        viewPager.setCurrentItem(item, smoothScroll)
    }

    fun registerOnPageChangeCallback(callback: ViewPager2.OnPageChangeCallback) {
        viewPager.registerOnPageChangeCallback(callback)
    }

    fun unregisterOnPageChangeCallback(callback: ViewPager2.OnPageChangeCallback) {
        viewPager.unregisterOnPageChangeCallback(callback)
    }

    fun setLocked(locked: Boolean) {
        isLocked = locked
        viewPager.isUserInputEnabled = !locked
    }

    fun isLocked(): Boolean = isLocked

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        handleLongPressEvent(ev)
        return super.dispatchTouchEvent(ev)
    }

    private fun handleLongPressEvent(ev: MotionEvent) {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (ev.pointerCount != 1) {
                    longPressActive = false
                    removeCallbacks(longPressRunnable)
                    return
                }
                longPressActive = true
                downX = ev.x
                downY = ev.y
                removeCallbacks(longPressRunnable)
                postDelayed(longPressRunnable, longPressTimeout.toLong())
            }
            MotionEvent.ACTION_MOVE -> {
                if (!longPressActive) return
                val dx = kotlin.math.abs(ev.x - downX)
                val dy = kotlin.math.abs(ev.y - downY)
                val distance = hypot(dx.toDouble(), dy.toDouble()).toFloat()
                if (distance > cancelSlop) {
                    longPressActive = false
                    removeCallbacks(longPressRunnable)
                }
            }
            MotionEvent.ACTION_POINTER_DOWN,
            MotionEvent.ACTION_POINTER_UP,
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                longPressActive = false
                removeCallbacks(longPressRunnable)
            }
        }
    }
}
