/*
  * Copyright @ 2015 China Mobile Group Device Co.,Ltd.
  * All rights Reserved.
*/

package com.android.launcher3;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;

public class CmccChildHiddenPanel extends FrameLayout {

    private static final String TAG = "CmccChildHiddenPanel";

    private AccelerateDecelerateInterpolator mInterpolator = new AccelerateDecelerateInterpolator();

    private int mFixedChildWidth;
    private int mFixedChildHeight;

    private Button mSwitchEffectOk;

    private Rect mTmpRect = new Rect();

    public CmccChildHiddenPanel(Context context) {
        super(context);
    }

    public CmccChildHiddenPanel(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CmccChildHiddenPanel(Context context, AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        // for animtion
        setClipChildren(false);

        mSwitchEffectOk = (Button) findViewById(R.id.page_switch_effect_ok);
    }

    public void setup(Launcher launcher) {
        mSwitchEffectOk.setOnClickListener(launcher);
        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View paramView) {
                if (mSwitchEffectOk != null) {
                    mSwitchEffectOk.performClick();
                }
            }
        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mSwitchEffectOk != null) {
            mTmpRect.set(0, 0, mSwitchEffectOk.getMeasuredWidth(),
                    mSwitchEffectOk.getMeasuredHeight());
            getChildVisibleRect(mSwitchEffectOk, mTmpRect, null);
            int x = (int) event.getX();
            if (x < mTmpRect.left || x > mTmpRect.right) {
                // ignore
            } else {
                int action = event.getActionMasked();
                switch(action) {
                    case MotionEvent.ACTION_DOWN:
                        mSwitchEffectOk.setPressed(true);
                        mSwitchEffectOk.invalidate();
                        break;
                    case MotionEvent.ACTION_UP:
                        mSwitchEffectOk.setPressed(false);
                        mSwitchEffectOk.invalidate();
                        break;
                }
                super.onTouchEvent(event);
            }
        }
        return true;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        int pL = getPaddingLeft();
        int pT = getPaddingTop();
        int pR = getPaddingRight();
        int pB = getPaddingBottom();

        setMeasuredDimension(mFixedChildWidth + pL + pR, mFixedChildHeight + pT + pB);

        if (getChildCount() > 0) {
            View child = getChildAt(0);
            int wms = MeasureSpec.makeMeasureSpec(mFixedChildWidth, MeasureSpec.EXACTLY);
            int hms = MeasureSpec.makeMeasureSpec(mFixedChildHeight, MeasureSpec.EXACTLY);
            child.measure(wms, hms);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right,
            int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        int childCount = getChildCount();
        if (childCount <= 0) {
            return;
        }

        int l = getPaddingLeft();
        int t = getPaddingTop() + getMeasuredHeight();

        // only handle one child
        View child = getChildAt(0);
        if (child.getVisibility() != View.GONE) {
            int mw = child.getMeasuredWidth();
            int mh = child.getMeasuredHeight();
            child.layout(l, t, l + mw, t + mh);
        }
    }

    public void visibleChild(boolean visible, boolean animate, int duration) {
        int childCount = getChildCount();
        if (childCount <= 0) {
            return;
        }

        View child = getChildAt(0);

        float alpha = visible ? 1f : 0f;
        int transY = visible ? -getMeasuredHeight() : 0;

        if (animate) {
            child.animate()
                .alpha(alpha)
                .translationY(transY)
                .setInterpolator(mInterpolator)
                .setDuration(duration)
                .start();
        } else {
            child.animate().cancel();
            child.setAlpha(alpha);
            child.setTranslationY(transY);
        }
    }

    public void fixChildDimention(int w, int h) {
        mFixedChildWidth = w;
        mFixedChildHeight = h;
    }
}
