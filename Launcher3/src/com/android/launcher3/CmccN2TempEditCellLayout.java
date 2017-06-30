/*
 * Copyright (C) 2016 Wingtech Group.
 * Modification based on code covered by the mentioned copyright
 * and/or permission notice(s).
 */

/*
 * Copyright (C) 2016 The CMCC N2 Project
 * the contaner of temp edit items
 *
 */

/*******************************************************************************************************
|   when     |      who     |    keyword       |        why        |     what                          |
********************************************************************************************************
20160519     tangzhongfeng   bug 178287         bug        
******************************************************************************************************/


package com.android.launcher3;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

public class CmccN2TempEditCellLayout extends CellLayout implements Page {
    //bug 178287 tangzhongfeng.wt ADD 20160519
    private Drawable mBg;
    public CmccN2TempEditCellLayout(Context context) {
        this(context, null);
    }

    public CmccN2TempEditCellLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CmccN2TempEditCellLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        //bug 178287 tangzhongfeng.wt ADD 20160519
        mBg = getResources().getDrawable(R.drawable.temp_edit_icon_bg);
    }

    @Override
    public void removeAllViewsOnPage() {
        removeAllViews();
    }

    @Override
    public void removeViewOnPageAt(int index) {
        removeViewAt(index);
    }

    @Override
    public int getPageChildCount() {
        return getChildCount();
    }

    @Override
    public View getChildOnPageAt(int i) {
        return getChildAt(i);
    }

    @Override
    public int indexOfChildOnPage(View v) {
        return indexOfChild(v);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
    }
    /*bug 178287 tangzhongfeng.wt ADD 20160519 start */
    @Override
    protected void dispatchDraw(Canvas canvas) {
        int cellWidth = getCellWidth();
        int cellHeight = getCellHeight();
        int widthGap = getWidthGap();
        //bug 186273 tangzhongfeng.wt ADD 20160614
        DeviceProfile d = LauncherAppState.getInstance().getDynamicGrid().getDeviceProfile();
        int padding = d.iconDrawablePaddingPx + 13;
        Rect dr = Utilities.getIconRect();
        String pkgname = Settings.Global.getString(getContext().getContentResolver(), "theme_package_name");
        /*if ("com.wt.theme3".equals(pkgname)) {
            padding += 2;
            dr.right -= 2;
            dr.bottom -= 2;
        }*/
        Rect r = new Rect();
        for (int i = 0; i < getCountX(); i++) {
            //bug 192406 tangzhongfeng.wt DEL getLeft() 20160707
            r.left = i * (cellWidth + widthGap) + padding + dr.left;
            r.top = padding + getTop() + dr.top;
            r.right = r.left + (dr.right - dr.left);
            r.bottom = r.top + (dr.bottom - dr.top);
            mBg.setBounds(r);
            mBg.draw(canvas);
        }
        super.dispatchDraw(canvas);
    }
    /*bug 178287 tangzhongfeng.wt ADD 20160519 end */
}
