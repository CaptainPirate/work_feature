package com.android.launcher3.effect;

import android.view.View;

public class AccordionEffect extends Effect {

    public static final String IDENTIFY = "ce5c46b5-da93-4c8f-88e8-a26f8f395b84";

    public AccordionEffect() {
        mIdentify = IDENTIFY;
    }

    @Override
    public void applyTransform(View v, float progress, int w, int h,
            OverscrollState overscroll, boolean rtl, int index) {
        float scaleX = 1.0f - Math.abs(progress);

        v.setPivotX(progress < 0 ? 0 : v.getMeasuredWidth());
        v.setScaleX(scaleX);
        if (scaleX == 0.0f) {
            v.setAlpha(0f);
        } else if (v.getAlpha() < 1) {
            v.setAlpha(1f);
        }
    }

}
