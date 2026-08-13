package com.example.reclaim.ui.report;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Wraps the embedded map so it can be panned while inside a ScrollView.
 * <p>
 * ScrollView normally intercepts vertical drag gestures, which makes the
 * screen scroll instead of the map. This wrapper asks all ancestor views
 * to not intercept touch events for the duration of any gesture that
 * starts on the map, then releases the lock when the gesture ends.
 * </p>
 */
public class MapTouchWrapper extends FrameLayout {

    public MapTouchWrapper(@NonNull Context context) {
        super(context);
    }

    public MapTouchWrapper(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public MapTouchWrapper(@NonNull Context context, @Nullable AttributeSet attrs,
                           int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                getParent().requestDisallowInterceptTouchEvent(true);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                getParent().requestDisallowInterceptTouchEvent(false);
                break;
            default:
                break;
        }
        return super.dispatchTouchEvent(ev);
    }
}
