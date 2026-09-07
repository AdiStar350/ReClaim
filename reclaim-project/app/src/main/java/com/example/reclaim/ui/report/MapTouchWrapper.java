package com.example.reclaim.ui.report;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Lets the map pan inside a scrolling page.
 * While a finger is on the map, the page does not steal the drag.
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
