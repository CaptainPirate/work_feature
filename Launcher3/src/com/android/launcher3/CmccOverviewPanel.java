/*
  * Copyright @ 2015 China Mobile Group Device Co.,Ltd.
  * All rights Reserved.
*/

package com.android.launcher3;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;

public class CmccOverviewPanel extends FrameLayout {

    private static final String TAG = "CmccOverviewPanel";

    private ViewGroup mMenusPanel;
    private CmccPagedView mEffectsPanel;

    private AccelerateDecelerateInterpolator mInterpolator = new AccelerateDecelerateInterpolator();
    private static final int PANEL_SWITCH_DURATION = 500;

    private boolean mIsShowingEffects;

    private Launcher mLauncher;

    // 0 horizontal
    // 1 vertical
    private static final int SWITCH_DIRECTION_HORIZONTAL = 0;
    private static final int SWITCH_DIRECTION_VERTICAL = 1;
    private int mSwitchDirection = SWITCH_DIRECTION_HORIZONTAL;

    public CmccOverviewPanel(Context context) {
        super(context);
    }

    public CmccOverviewPanel(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CmccOverviewPanel(Context context, AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mMenusPanel = (ViewGroup) findViewById(R.id.overview_menus);
        mEffectsPanel = (CmccPagedView) findViewById(R.id.overview_switch_effects);
        // for animtion
        setClipChildren(false);
    }

    public void setup(Launcher launcher) {
        mLauncher = launcher;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right,
            int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        int w = getMeasuredWidth();
        int h = getMeasuredHeight();

        int childCount = getChildCount();

        // layout vertically
        if (mSwitchDirection == SWITCH_DIRECTION_VERTICAL) {
            int t = getPaddingTop();
            int l = getPaddingLeft();
            for (int i = 0; i < childCount; i++) {
                View child = getChildAt(i);
                child.layout(l, t, l + w, t + h);
                t += h;
            }
        } else {
            // layout horizontally
            final boolean isRtl = isLayoutRtl();
            final int startIndex = isRtl ? childCount - 1 : 0;
            final int endIndex = isRtl ? -1 : childCount;
            final int delta = isRtl ? -1 : 1;

            int l = isRtl ? -(childCount -1) * w : 0;

            for (int i = startIndex; i != endIndex; i += delta) {
                View child = getChildAt(i);
                child.layout(l, 0, l + w, h);
                l += w;
            }
        }
    }

    public void showEffectSelectPanel(boolean show) {
        if (mLauncher != null) {
            Workspace workspace = mLauncher.getWorkspace();
            if (workspace != null) {
                workspace.prepareForEffectPreview(show);
            }
        }
        showEffectSelectPanel(show, true);
    }

    public void showEffectSelectPanel(boolean show, boolean animate) {
        int sign = (isLayoutRtl()) ? 1 : -1;
        int transX = show ? (sign * getMeasuredWidth()) : 0;
        float menusAlpha = show ? 0f : 1f;
        float effectsAlpha = show ? 1f : 0f;
        mIsShowingEffects = show;

        int duration = PANEL_SWITCH_DURATION;

        int transY = (show && mSwitchDirection == SWITCH_DIRECTION_VERTICAL) ? -getMeasuredHeight()
                : 0;
        int hiddenChildHeight = 0;
        if (mLauncher != null) {
            duration = mLauncher.getDurationForShowEffectSelectPanel();
            CmccChildHiddenPanel cchp = mLauncher.getChildHiddenPanel();
            if (cchp != null) {
                hiddenChildHeight = cchp.getMeasuredHeight();
                cchp.visibleChild(show, animate, duration);
            }
            mLauncher.prepareForEffectPreview(show);
        }
        int selfTransY = (show && mSwitchDirection == SWITCH_DIRECTION_VERTICAL) ? -hiddenChildHeight
                : 0;

        if (animate) {
            animate().translationY(selfTransY)
                     .setInterpolator(mInterpolator)
                     .setDuration(duration);

            mMenusPanel.animate().alpha(menusAlpha)
                    .translationX(transX)
                    .translationY(transY)
                    .setInterpolator(mInterpolator)
                    .setDuration(duration);

            mEffectsPanel.animate().alpha(effectsAlpha)
                    .translationX(transX)
                    .translationY(transY)
                    .setInterpolator(mInterpolator)
                    .setDuration(duration)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            mEffectsPanel.resetScroll(false);
                        }
                    });
        } else {
            // Note: don't call animate.cancel() on CmccOverviewPanle here, this
            // will affect animation start by state change of Workspace.
            mMenusPanel.animate().cancel();
            mEffectsPanel.animate().cancel();

            setTranslationY(selfTransY);
            mMenusPanel.setTranslationY(transY);
            mEffectsPanel.setTranslationY(transY);

            mMenusPanel.setTranslationX(transX);
            mEffectsPanel.setTranslationX(transX);

            mMenusPanel.setAlpha(menusAlpha);
            mEffectsPanel.setAlpha(effectsAlpha);
            if (!show) {
                mEffectsPanel.resetScroll(false);
            }
        }
    }

    public boolean isShowingEffectsPanel() {
        return mIsShowingEffects;
    }

    public CmccPagedView getEffectsPanel() {
        return mEffectsPanel;
    }

    /**
     * Note: this is a reimplementation of View.isLayoutRtl() since that is currently hidden api.
     */
    public boolean isLayoutRtl() {
        return (getLayoutDirection() == LAYOUT_DIRECTION_RTL);
    }

}
