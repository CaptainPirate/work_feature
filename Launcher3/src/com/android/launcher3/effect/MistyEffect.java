package com.android.launcher3.effect;

import android.view.View;

public class MistyEffect extends Effect {

    private static final String TAG = "MistyEffect";

    public static final String IDENTIFY = "7899c431-24b3-486b-939c-85de912587e6";

    // Y rotation to apply to the workspace screens
    private static final float WORKSPACE_ROTATION = 24f;

    public MistyEffect() {
        mIdentify = IDENTIFY;
    }

    @Override
    public void applyTransform(View v, float progress, int w, int h,
            OverscrollState overscroll, boolean rtl, int index) {
        float rotation = WORKSPACE_ROTATION * progress;

        v.setPivotX((progress + 1) * w * 0.5f);
        v.setPivotY(h * 0.5f);
        v.setRotationY(rotation);
    }

}
