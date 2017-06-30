/*
  * Copyright @ 2015 China Mobile Group Device Co.,Ltd.
  * All rights Reserved.
*/

package com.android.launcher3;

import java.lang.ref.WeakReference;
import java.util.Calendar;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.util.Log;

public class CmccDynamicCalendar {

    private static final String TAG = "CmccDynamicCalendar";

    private LayerDrawable mBackground;
    private boolean mNeedUpdate;
    private int mLastDate = getLocalDate();

    private static ComponentName sCalendarComponentName = new ComponentName(
            "com.android.calendar", "com.android.calendar.AllInOneActivity");

    private WeakReference<Launcher> mLauncherRef;

    private CmccDynamicCalendar() {
    }

    private static class SingletonHolder {
        private static final CmccDynamicCalendar INSTANCE = new CmccDynamicCalendar();
    }

    public static CmccDynamicCalendar getInstance() {
        return SingletonHolder.INSTANCE;
    }

    public static int getLocalDate() {
        Calendar calendar = Calendar.getInstance();
        int date = calendar.get(Calendar.DAY_OF_MONTH);
        return date;
    }

    public Bitmap getCalendarIcon() {
        int date = getLocalDate();
        return getCalendarIcon(date);
    }

    public Bitmap getCalendarIcon(int date) {
        Bitmap result = null;
        if (date < 1 || date > 31) {
            return result;
        }
        // get resources from context
        Context context = LauncherAppState.getInstance().getContext();
        if (context == null) {
            return result;
        }

        Resources res = context.getResources();
        // bug 178661 tangzhongfeng.wt MODIFY 20160519
        IconCache cache = LauncherAppState.getInstance().getIconCache();
        Drawable dateDrawable = cache.getCalendarDrawableFromTheme("calendar_" + date);
        Drawable bgDrawable = cache.getCalendarDrawableFromTheme("calendar_bg");
        
        if (mBackground == null) {
            mBackground = (LayerDrawable) res
                    .getDrawable(R.drawable.dynamic_calendar);
        }
        mBackground.mutate();
        mBackground.setDrawableByLayerId(R.id.today, dateDrawable);
        mBackground.setDrawableByLayerId(R.id.today_icon_background, bgDrawable);

        return Utilities.createIconBitmap(mBackground, context);
    }

    public static boolean isCalendarComponentName(ComponentName cn) {
        return sCalendarComponentName.equals(cn);
    }

    public void setLauncher(Launcher launcher) {
        mLauncherRef = new WeakReference<Launcher>(launcher);
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            int currentDate = getLocalDate();
            // if mNeedUpdate set to true then we need Launcher reset it to false
            // to avoid last onReceive action rewrite the value.
            if (!mNeedUpdate) {
                mNeedUpdate = mLastDate != currentDate;
            }
            if (mNeedUpdate) {
                mLastDate = currentDate;
            }
            updateDynamicCalendarIfNeeded();
        }
    };

    public void registerBroadcastReceiver() {
        Context context = LauncherAppState.getInstance().getContext();
        if (context != null) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_DATE_CHANGED);
            filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
            filter.addAction(Intent.ACTION_TIME_CHANGED);
            filter.addAction(Intent.ACTION_LOCALE_CHANGED);
            //bug 185885 tangzhongfeng.wt ADD 20160613
            filter.addAction(Intent.ACTION_TIME_TICK);
            context.registerReceiver(mReceiver, filter);
        }
    }

    public void unregisterBroadcastReceiver() {
        Context context = LauncherAppState.getInstance().getContext();
        if (context != null) {
            context.unregisterReceiver(mReceiver);
        }
    }

    public boolean updateCalendarIcon(BubbleTextView bubble) {
        if (bubble != null) {
            Object tag = bubble.getTag();
            if (tag instanceof ShortcutInfo
                    || tag instanceof AppInfo) {
                ComponentName cn = ((ItemInfo) tag).getIntent().getComponent();
                if (isCalendarComponentName(cn)) {
                    Bitmap icon = getCalendarIcon();
                    if (icon != null) {
                        bubble.updateIcon(icon);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void resetUpdateStatus() {
        mNeedUpdate = false;
    }

    public void updateDynamicCalendarIfNeeded() {
        if (mNeedUpdate) {
            if (mLauncherRef != null && mLauncherRef.get() != null) {
                mLauncherRef.get().updateDynamicCalendar();
            }
        }
    }
}
