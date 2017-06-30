package com.android.launcher3;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.GridView;

public class CmccGridView extends GridView {
    private static final String TAG = "CmccGridView";

    private List<OnClickBlankPositionListener> mClickBlankPositionListeners;

    public interface OnClickBlankPositionListener {
        boolean onClickBlankPosition();
    }

    public CmccGridView(Context context) {
        super(context);
    }

    public CmccGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CmccGridView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean consumed = false;

        if (mClickBlankPositionListeners != null
                && !mClickBlankPositionListeners.isEmpty()) {
            if (!isEnabled()) {
                // A disabled view that is clickable still consumes the
                // events, it just doesn't respond to them.
                consumed = (isClickable() || isLongClickable());
            }

            if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                final int motionPosition = pointToPosition((int) event.getX(),
                        (int) event.getY());
                if (motionPosition == INVALID_POSITION) {
                    consumed = fireClickBlankPositionEvent();
                }
            }
        }

        if (!consumed) {
            return super.onTouchEvent(event);
        }

        return true;
    }

    public void setOnClickBlankPositionListener(OnClickBlankPositionListener listener) {
        if (mClickBlankPositionListeners == null) {
            mClickBlankPositionListeners = new ArrayList<OnClickBlankPositionListener>();
        }
        mClickBlankPositionListeners.add(listener);
    }

    public void removeOnClickBlankPositionListener(OnClickBlankPositionListener listener) {
        if (mClickBlankPositionListeners == null) {
            return;
        }
        mClickBlankPositionListeners.remove(listener);
    }

    private boolean fireClickBlankPositionEvent() {
        boolean result = false;
        if (mClickBlankPositionListeners == null) {
            return false;
        }
        for (OnClickBlankPositionListener listener : mClickBlankPositionListeners) {
            result = (result || listener.onClickBlankPosition());
        }
        return result;
    }

}
