package net.mikaelzero.mojito.tools;

import android.content.Context;
import android.util.AttributeSet;
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
        cancelSlop = Math.max(1, config.getScaledTouchSlop() / 2);
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
                return super.onInterceptTouchEvent(ev);
            } catch (IllegalArgumentException ex) {
                ex.printStackTrace();
                return false;
            }
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return !isLocked && super.onTouchEvent(event);
    }

    public void setLocked(boolean isLocked) {
        this.isLocked = isLocked;
    }

    public boolean isLocked() {
        return isLocked;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        handleLongPressEvent(ev);
        return super.dispatchTouchEvent(ev);
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
                float distance = (float) Math.hypot(dx, dy);
                if (distance > cancelSlop) {
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

}
