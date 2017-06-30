package com.android.launcher3.effect;

import android.animation.TimeInterpolator;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

public class StackUpEffect extends Effect {
    private static final String TAG = "DofEffect";

    public static final String IDENTIFY = "8a5f53d8-8be4-4c6c-8193-e54828055fca";

    // define the final scale of invisible views
    private static final float SCALE_OF_INVISIBLE_VIEWS = 0.74f;
    private static float TRANSITION_PIVOT = 0.65f;
    private static float TRANSITION_MAX_ROTATION = 25;

    private ZInterpolator mZInterpolator = new ZInterpolator(0.5f);
    private AccelerateInterpolator mAlphaInterpolator = new AccelerateInterpolator(0.9f);
    private DecelerateInterpolator mLargeScreenAlphaInterpolator = new DecelerateInterpolator(4);

    public StackUpEffect() {
        mIdentify = IDENTIFY;
    }

    @Override
    public void applyTransform(View v, float progress, int w, int h,
            OverscrollState overscroll, boolean rtl, int index) {

        float maxProgress = Math.max(0, progress);
        float minProgress = Math.min(0, progress);

        float transX;
        float zInterpolate;
        if (rtl) {
            transX = maxProgress * v.getMeasuredWidth();
            zInterpolate = mZInterpolator.getInterpolation(Math.abs(maxProgress));
        } else {
            transX = minProgress * v.getMeasuredWidth();
            zInterpolate = mZInterpolator.getInterpolation(Math.abs(minProgress));
        }

        float scale = (1 - zInterpolate) + zInterpolate * SCALE_OF_INVISIBLE_VIEWS;

        float alpha;
        if (rtl && (progress > 0)) {
            alpha = mAlphaInterpolator.getInterpolation(1 - Math.abs(progress));
        } else if (!rtl && (progress < 0)) {
            alpha = mAlphaInterpolator.getInterpolation(1 - Math.abs(progress));
        } else {
            // On large screens we need to fade the page as it nears its
            // leftmost position
            alpha = mLargeScreenAlphaInterpolator.getInterpolation(1 - progress);
        }

        float xPivot = rtl ? 1f - TRANSITION_PIVOT : TRANSITION_PIVOT;

        if (overscroll == OverscrollState.LEFT) {
            v.setPivotX(xPivot * w);
            v.setRotationY(-TRANSITION_MAX_ROTATION * progress);
            scale = 1.0f;
            alpha = 1.0f;
            // On the first page, we don't want the page to have any lateral
            // motion
            transX = 0;
        } else if (overscroll == OverscrollState.RIGHT) {
            v.setPivotX((1 - xPivot) * w);
            v.setRotationY(-TRANSITION_MAX_ROTATION * progress);
            scale = 1.0f;
            alpha = 1.0f;
            // On the last page, we don't want the page to have any lateral
            // motion.
            transX = 0;
        } else {
            v.setPivotX(w / 2.0f);
            v.setPivotY(h / 2.0f);
            v.setRotationY(0f);
        }

        v.setTranslationX(transX);
        v.setScaleX(scale);
        v.setScaleY(scale);
        v.setAlpha(alpha);
    }

    public static class ZInterpolator implements TimeInterpolator {
        private float mFocal;

        public ZInterpolator(float focal) {
            mFocal = focal;
        }

        public float getInterpolation(float input) {
            return (input * (mFocal + 1)) / (mFocal + input);
        }
    }

}
