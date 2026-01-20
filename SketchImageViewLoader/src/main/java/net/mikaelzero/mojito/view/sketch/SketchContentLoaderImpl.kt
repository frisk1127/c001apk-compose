package net.mikaelzero.mojito.view.sketch

import android.content.Context
import android.graphics.Rect
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.lifecycle.LifecycleObserver
import net.mikaelzero.mojito.Mojito.Companion.mojitoConfig
import net.mikaelzero.mojito.interfaces.OnMojitoViewCallback
import net.mikaelzero.mojito.loader.ContentLoader
import net.mikaelzero.mojito.loader.OnLongTapCallback
import net.mikaelzero.mojito.loader.OnTapCallback
import net.mikaelzero.mojito.tools.ScreenUtils
import net.mikaelzero.mojito.view.sketch.core.SketchImageView
import net.mikaelzero.mojito.view.sketch.core.decode.ImageSizeCalculator
import kotlin.math.abs
import kotlin.math.max


/**
 * @Author: MikaelZero
 * @CreateDate: 2020/6/10 10:01 AM
 * @Description:
 */
class SketchContentLoaderImpl : ContentLoader, LifecycleObserver {


    private lateinit var sketchImageView: SketchImageView
    private lateinit var frameLayout: FrameLayout
    private var isLongHeightImage = false
    private var isLongWidthImage = false
    private var screenHeight = 0
    private var screenWidth = 0
    private var longImageHeightOrWidth = 0
    private var onMojitoViewCallback: OnMojitoViewCallback? = null

    override fun providerRealView(): View {
        return sketchImageView
    }

    override fun providerView(): View {
        return frameLayout
    }

    override val displayRect: RectF
        get() {
            val rectF = RectF()
            sketchImageView.zoomer?.getDrawRect(rectF)
            return RectF(rectF)
        }

    override fun init(context: Context, originUrl: String, targetUrl: String?, onMojitoViewCallback: OnMojitoViewCallback?) {
        frameLayout = FrameLayout(context)
        sketchImageView = SketchImageView(context)
        sketchImageView.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        sketchImageView.isZoomEnabled = true
        sketchImageView.options.isDecodeGifImage = true
        frameLayout.addView(sketchImageView)
        screenHeight = if (mojitoConfig().transparentNavigationBar()) ScreenUtils.getScreenHeight(context) else ScreenUtils.getAppScreenHeight(context)
        screenWidth = ScreenUtils.getScreenWidth(context)
        this.onMojitoViewCallback = onMojitoViewCallback
        sketchImageView.zoomer?.blockDisplayer?.setPause(true)
    }

    override fun dispatchTouchEvent(isDrag: Boolean, isActionUp: Boolean, isDown: Boolean, isHorizontal: Boolean): Boolean {
        return when {
            isLongHeightImage -> {
                when {
                    isDrag -> {
                        return false
                    }
                    isActionUp -> {
                        val result = !isDrag
                        return result
                    }
                    else -> {
                        val zoomer = sketchImageView.zoomer
                        if (isHorizontal &&
                            zoomer != null &&
                            zoomer.zoomScale <= zoomer.fillZoomScale + 0.01f
                        ) {
                            return false
                        }
                        val rectF = Rect()
                        sketchImageView.zoomer?.getVisibleRect(rectF)
                        val drawRect = RectF()
                        sketchImageView.zoomer?.getDrawRect(drawRect)
                        onMojitoViewCallback?.onLongImageMove(abs(drawRect.top) / (longImageHeightOrWidth - screenHeight).toFloat())
                        //长图处于顶部  并且有向下滑动的趋势
                        val isTop = sketchImageView.zoomer!!.zoomScale == sketchImageView.zoomer!!.fillZoomScale && rectF.top == 0 && isDown
                        //长图不处于顶部和底部的时候
                        val isCenter = sketchImageView.zoomer!!.fillZoomScale - sketchImageView.zoomer!!.zoomScale <= 0.01f
                                && rectF.top != 0
                                && screenHeight != drawRect.bottom.toInt()
                        //长图处于缩放状态  由于库的bug 会出现 8.99999  和  9
                        //如果宽度已经充满，但还是可以放大，这种情况不应该是处于缩放状态
                        val isScale = if (sketchImageView.zoomer!!.zoomScale == sketchImageView.zoomer!!.fillZoomScale) false else sketchImageView.zoomer!!.maxZoomScale - sketchImageView.zoomer!!.zoomScale > 0.01f
                        val isBottom = (sketchImageView.zoomer!!.zoomScale == sketchImageView.zoomer!!.fillZoomScale
                                && !isDown
                                && screenHeight == drawRect.bottom.toInt())
                        val result = isTop || isCenter || isScale || isBottom
                        return result
                    }
                }
            }
            isLongWidthImage -> {
                val rectF = Rect()
                sketchImageView.zoomer?.getVisibleRect(rectF)
                val result = when {
                    isDrag && !isHorizontal -> {
                        false
                    }
                    isActionUp -> {
                        !isDrag
                    }
                    else -> {
                        val zoomer = sketchImageView.zoomer
                        if (isHorizontal &&
                            zoomer != null &&
                            zoomer.zoomScale <= zoomer.fillZoomScale + 0.01f
                        ) {
                            return false
                        }
                        val drawRect = RectF()
                        sketchImageView.zoomer?.getDrawRect(drawRect)
                        onMojitoViewCallback?.onLongImageMove(abs(drawRect.left) / abs(drawRect.right - drawRect.left))
                        val isScale = sketchImageView.zoomer!!.maxZoomScale - sketchImageView.zoomer!!.zoomScale > 0.01f
                        return isHorizontal || isScale
                    }
                }
                result
            }
            else -> {
                val zoomer = sketchImageView.zoomer
                if (zoomer == null) {
                    return false
                }
                val baseScale = max(zoomer.fullZoomScale, zoomer.fillZoomScale)
                val delta = zoomer.zoomScale - baseScale
                val result = delta > 0.05f
                result
            }
        }
    }

    override fun dragging(width: Int, height: Int, ratio: Float) {
    }

    override fun beginBackToMin(isResetSize: Boolean) {
        sketchImageView.zoomer?.blockDisplayer?.setPause(true)
        if (isLongHeightImage || isLongWidthImage) {

        } else {
            if (isResetSize) {
                sketchImageView.scaleType = ImageView.ScaleType.CENTER_CROP
            }
        }
    }

    override fun backToNormal() {
    }

    override fun loadAnimFinish() {
        if (isLongHeightImage || isLongWidthImage) {

        } else {
            sketchImageView.scaleType = ImageView.ScaleType.FIT_CENTER
        }
        sketchImageView.zoomer?.blockDisplayer?.setPause(false)
    }

    override fun needReBuildSize(): Boolean {
        return sketchImageView.zoomer!!.zoomScale >= sketchImageView.zoomer!!.fullZoomScale
    }

    override fun useTransitionApi(): Boolean {
        return isLongWidthImage || isLongHeightImage || needReBuildSize()
    }

    override fun isLongImage(width: Int, height: Int): Boolean {
        isLongHeightImage = height > width * 22f / 9f
        isLongWidthImage = width > height * 5 && width > (ScreenUtils.getScreenWidth(sketchImageView.context) * 1.5)
        sketchImageView.zoomer?.isReadMode = isLongHeightImage || isLongWidthImage
        if (isLongWidthImage) {
            longImageHeightOrWidth = width
        } else if (isLongHeightImage) {
            longImageHeightOrWidth = height
        }
        if (isLongHeightImage || isLongWidthImage) {

        } else {
            sketchImageView.scaleType = ImageView.ScaleType.CENTER_CROP
        }
        return isLongHeightImage || isLongWidthImage
    }

    override fun onTapCallback(onTapCallback: OnTapCallback) {
        sketchImageView.zoomer?.setOnViewTapListener { view, x, y ->
            onTapCallback.onTap(view, x, y)
        }
    }

    override fun onLongTapCallback(onLongTapCallback: OnLongTapCallback) {
        val longPressTimeout = ViewConfiguration.getLongPressTimeout().toLong()
        val viewConfig = ViewConfiguration.get(sketchImageView.context)
        val cancelSlop = viewConfig.scaledTouchSlop * 2
        val slowMoveSlop = viewConfig.scaledTouchSlop * 0.6f
        val interceptSlop = viewConfig.scaledTouchSlop * 2
        val fastSwipeSlop = viewConfig.scaledTouchSlop * 2
        val fastSwipeTimeout = 450L
        var downX = 0f
        var downY = 0f
        var downTime = 0L
        var manualFired = false
        val manualRunnable = Runnable {
            if (manualFired) return@Runnable
            manualFired = true
            onLongTapCallback.onLongTap(sketchImageView, downX, downY)
        }
        val updateParentIntercept = { disallow: Boolean ->
            sketchImageView.parent?.requestDisallowInterceptTouchEvent(disallow)
        }

        sketchImageView.zoomer?.setOnViewLongPressListener { view, x, y ->
            manualFired = true
            view.removeCallbacks(manualRunnable)
            onLongTapCallback.onLongTap(view, x, y)
        }
        // Fallback when ImageZoomer isn't working yet (e.g. static images or jittery touch).
        sketchImageView.setOnTouchListener { view, event ->
            val zoomer = sketchImageView.zoomer
            val baseScale = if (zoomer == null) 1f else max(zoomer.fullZoomScale, zoomer.fillZoomScale)
            val isZoomed = zoomer != null && zoomer.zoomScale - baseScale > 0.02f
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    if (event.pointerCount > 1) {
                        manualFired = true
                        view.removeCallbacks(manualRunnable)
                        updateParentIntercept(true)
                        return@setOnTouchListener false
                    }
                    manualFired = false
                    downX = event.x
                    downY = event.y
                    downTime = event.eventTime
                    // Keep initial DOWN in child so double-tap can be detected.
                    updateParentIntercept(true)
                    view.removeCallbacks(manualRunnable)
                    view.postDelayed(manualRunnable, longPressTimeout)
                }
                MotionEvent.ACTION_MOVE -> {
                    if (event.pointerCount > 1) {
                        manualFired = true
                        view.removeCallbacks(manualRunnable)
                        updateParentIntercept(true)
                        return@setOnTouchListener false
                    }
                    val dx = kotlin.math.abs(event.x - downX)
                    val dy = kotlin.math.abs(event.y - downY)
                    val elapsed = event.eventTime - downTime
                    val horizontalSwipe = dx > interceptSlop && dx > dy
                    if (dx > cancelSlop || dy > cancelSlop || dx > slowMoveSlop || dy > slowMoveSlop || horizontalSwipe) {
                        view.removeCallbacks(manualRunnable)
                    }
                    val fastHorizontalSwipe = horizontalSwipe &&
                        dx > fastSwipeSlop &&
                        elapsed < fastSwipeTimeout
                    if ((!isZoomed && horizontalSwipe) || fastHorizontalSwipe) {
                        updateParentIntercept(false)
                    } else {
                        updateParentIntercept(true)
                    }
                }
                MotionEvent.ACTION_POINTER_DOWN -> {
                    manualFired = true
                    view.removeCallbacks(manualRunnable)
                    updateParentIntercept(true)
                }
                MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    manualFired = true
                    updateParentIntercept(false)
                    view.removeCallbacks(manualRunnable)
                }
            }
            false
        }
    }

    override fun pageChange(isHidden: Boolean) {

    }

}
