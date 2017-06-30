package com.android.launcher3.effect;

import android.view.View;

public class CylinderInEffect extends Effect {

    public static final String IDENTIFY = "275db82a-1510-4200-861b-26c3736329f3";

    public CylinderInEffect() {
        mIdentify = IDENTIFY;
    }

    @Override
    public void applyTransform(View v, float progress, int w, int h,
            OverscrollState overscroll, boolean rtl, int index) {
        if (progress >= 0) {
            v.setAlpha(1 - progress);
        } else {
            v.setAlpha(1 + progress);
        }
    }

}
