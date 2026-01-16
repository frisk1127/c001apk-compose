package net.mikaelzero.mojito.tools;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

import androidx.viewpager.widget.ViewPager;

import net.mikaelzero.mojito.ui.ImageMojitoActivity;

/**
 * @Author: MikaelZero
 * @CreateDate: 2020/6/13 11:56 AM
 * @Description:
 */
public class NoScrollViewPager extends ViewPager {
    private boolean isLocked;
    private int longPressTimeout;
    private int cancelSlop;
    private boolean longPressActive;
    private float downX;
    private float downY;
    private final Runnable longPressRunnable = new Runnable() {
        @Override
        public void run() {
            if (!longPressActive) {
                return;
            }
            longPressActive = false;
            if (getContext() instanceof ImageMojitoActivity) {
                ((ImageMojitoActivity) getContext()).tryDispatchLongPress(downX, downY);
            }
        }
    };

    public NoScrollViewPager(Context context) {
        super(context);
        initTouchConfig();
    }

    public NoScrollViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        initTouchConfig();
    }

    private void initTouchConfig() {
        ViewConfiguration config = ViewConfiguration.get(getContext());
        longPressTimeout = ViewConfiguration.getLongPressTimeout();
        cancelSlop = config.getScaledTouchSlop() * 4;
    }

    @Override
    public void setCurrentItem(int item, boolean smoothScroll) {
        super.setCurrentItem(item, smoothScroll);
    }

    @Override
    public void setCurrentItem(int item) {
        super.setCurrentItem(item, false);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (!isLocked) {
            try {
                boolean result = super.onInterceptTouchEvent(ev);
                Log.d("MojitoLongPress", "ViewPager intercept result=" + result + " " + formatEvent(ev));
                return result;
            } catch (IllegalArgumentException ex) {
                ex.printStackTrace();
                Log.d("MojitoLongPress", "ViewPager intercept exception");
                return false;
            }
        }
        Log.d("MojitoLongPress", "ViewPager intercept locked " + formatEvent(ev));
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean result = !isLocked && super.onTouchEvent(event);
        Log.d("MojitoLongPress", "ViewPager touch result=" + result + " locked=" + isLocked + " " + formatEvent(event));
        return result;
    }

    public void setLocked(boolean isLocked) {
        this.isLocked = isLocked;
        Log.d("MojitoLongPress", "ViewPager setLocked=" + isLocked);
    }

    public boolean isLocked() {
        return isLocked;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        handleLongPressEvent(ev);
        boolean handled = super.dispatchTouchEvent(ev);
        Log.d("MojitoLongPress", "ViewPager dispatch locked=" + isLocked
                + " handled=" + handled + " childCount=" + getChildCount() + " " + formatEvent(ev));
        return handled;
    }

    private void handleLongPressEvent(MotionEvent ev) {
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (ev.getPointerCount() != 1) {
                    longPressActive = false;
                    removeCallbacks(longPressRunnable);
                    return;
                }
                longPressActive = true;
                downX = ev.getX();
                downY = ev.getY();
                removeCallbacks(longPressRunnable);
                postDelayed(longPressRunnable, longPressTimeout);
                break;
            case MotionEvent.ACTION_MOVE:
                if (!longPressActive) {
                    return;
                }
                float dx = Math.abs(ev.getX() - downX);
                float dy = Math.abs(ev.getY() - downY);
                if (dx > cancelSlop || dy > cancelSlop) {
                    longPressActive = false;
                    removeCallbacks(longPressRunnable);
                }
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                longPressActive = false;
                removeCallbacks(longPressRunnable);
                break;
        }
    }

    private static String formatEvent(MotionEvent event) {
        int toolType = event.getPointerCount() > 0 ? event.getToolType(0) : -1;
        return "action=" + event.getActionMasked()
                + " source=0x" + Integer.toHexString(event.getSource())
                + " toolType=" + toolType
                + " buttonState=0x" + Integer.toHexString(event.getButtonState())
                + " pointers=" + event.getPointerCount();
    }
}
