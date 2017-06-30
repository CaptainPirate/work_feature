package com.android.launcher3.effect;

import android.view.View;

public class WindmillEffect extends Effect {

    public static final String IDENTIFY = "92a376e1-e687-4b1f-b3d1-a94d7ed6b972";

    public WindmillEffect() {
        mIdentify = IDENTIFY;
    }

    @Override
    public void applyTransform(View v, float progress, int w, int h,
            OverscrollState overscroll, boolean rtl, int index) {
        float rotation = -progress * 45.0F;
        if (overscroll == OverscrollState.LEFT) {
            v.setPivotX(1.4f * w / 2.0f);
            v.setPivotY(h * 2.2f);
            v.setRotation(rotation);
        } else if (overscroll == OverscrollState.RIGHT) {
            v.setPivotX(0.6f * w / 2.0f);
            v.setPivotY(h * 2.2f);
            v.setRotation(rotation);
        } else {
            v.setPivotY(h);
            v.setPivotX(w / 2.0f);
            v.setRotation(rotation);
        }
    }

}
