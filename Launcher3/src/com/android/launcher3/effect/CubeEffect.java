package com.android.launcher3.effect;

import android.view.View;

public class CubeEffect extends Effect {

    public static final String IDENTIFY = "8c35a209-911f-4b48-a495-b704908c2d1f";

    private static float CAMERA_DISTANCE = 2880;

    public CubeEffect() {
        mIdentify = IDENTIFY;
    }

    @Override
    public void applyTransform(View v, float progress, int w, int h,
            OverscrollState overscroll, boolean rtl, int index) {
        float rotation = -90.0f * progress;
        float alpha = 1 - Math.abs(progress);

        v.setCameraDistance(v.getResources().getDisplayMetrics().density * CAMERA_DISTANCE);

        v.setPivotX(progress < 0 ? 0 : w);
        v.setPivotY(h * 0.5f);
        v.setRotationY(rotation);
        v.setAlpha(alpha);
    }

}
