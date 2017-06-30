package com.android.launcher3.effect;

import android.view.View;

public class DofEffect extends Effect {

    public static final String IDENTIFY = "889c9225-6811-4ce4-89b8-43e8a4bff185";

    public DofEffect() {
        mIdentify = IDENTIFY;
    }

    @Override
    public void applyTransform(View v, float progress, int w, int h,
            OverscrollState overscroll, boolean rtl, int index) {
        float scale = 1.0f + (-0.2f * Math.abs(progress));

        v.setScaleX(scale);
        v.setScaleY(scale);
        if (mFadeInAdjacentScreens) {
            float alpha = 1 - Math.abs(progress);
            v.setAlpha(alpha);
        }
    }

}
