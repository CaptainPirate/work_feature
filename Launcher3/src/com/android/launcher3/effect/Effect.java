package com.android.launcher3.effect;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.launcher3.R;

public abstract class Effect {

    public static final String IDENTIFY = "6b02dff2-c7e9-11e4-b14e-f8bc12753dec";

    protected String mName;
    protected Drawable mNormalDrawable;
    protected Drawable mSelectedDrawable;
    protected String mIdentify = IDENTIFY;
    protected TextView mTriggerView;
    protected boolean mSelected;
    protected boolean mFadeInAdjacentScreens = true;

    public enum OverscrollState {NONE, LEFT, RIGHT}

    public View getTriggerView(Context context) {
        if (mTriggerView != null) {
            return mTriggerView;
        }

        Resources res = context.getResources();
        int drawablePadding = res.getDimensionPixelSize(R.dimen.effect_textview_drawable_padding);
        int fixTextSize = res.getInteger(R.integer.launcher_text_size_in_sp);

        TextView tx = new TextView(context);
        // set attributes
        tx.setCompoundDrawablesRelativeWithIntrinsicBounds(null, mNormalDrawable, null, null);
        tx.setText(mName);
        tx.setAllCaps(true);
        tx.setCompoundDrawablePadding(drawablePadding);
        tx.setTextSize(TypedValue.COMPLEX_UNIT_SP, fixTextSize);
        tx.setTypeface(Typeface.create("sans-serif-condensed", -1));
        tx.setGravity(Gravity.CENTER_HORIZONTAL);

        mTriggerView = tx;
        return tx;
    }

    public void setName(String name) {
        mName = name;
    }

    public void setDrawable(Drawable drawable) {
        mNormalDrawable = drawable;
    }

    public Drawable getDrawable() {
        return mNormalDrawable;
    }

    public void setSelectedDrawable(Drawable drawable) {
        mSelectedDrawable = drawable;
    }

    public void setSelectState(boolean selected) {
        if (mSelected == selected || mTriggerView == null) {
            return;
        }
        mSelected = selected;
        Drawable drawable = null;
        if (mSelected) {
            drawable = mSelectedDrawable;
        } else {
            drawable = mNormalDrawable;
        }
        mTriggerView.setCompoundDrawablesRelativeWithIntrinsicBounds(null,
                drawable, null, null);
    }

    public String getIdentify() {
        return mIdentify;
    }

    public abstract void applyTransform(View v, float progress, int w,
            int h, OverscrollState overscroll, boolean rtl, int index /* for debug */);

    @Override
    public String toString() {
        return "Effect[" + mName + "], Identify: " + getIdentify();
    }

}
