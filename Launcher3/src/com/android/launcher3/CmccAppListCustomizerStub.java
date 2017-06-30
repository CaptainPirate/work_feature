/*
  * Copyright @ 2015 China Mobile Group Device Co.,Ltd.
  * All rights Reserved.
*/

package com.android.launcher3;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.ComponentName;
import android.content.Context;
import android.content.res.Resources;
import android.text.TextUtils;
import android.util.Log;

public class CmccAppListCustomizerStub {
    private static final String TAG = "CmccCustomizeAppListStub";

    protected boolean mHasCustomizeData = false;
    protected boolean mInitialized = false;

    private static CmccAppListCustomizerStub sInstance;

    public static CmccAppListCustomizerStub getInstance() {
        if (sInstance == null) {
            sInstance = getInstanceFromConfigFile();
        }

        // Return empty stub
        if (sInstance == null) {
            sInstance = new CmccAppListCustomizerStub();
        }

        if (sInstance != null && !sInstance.isInitialized()) {
            Context ctx = LauncherAppState.getInstance().getContext();
            sInstance.init(ctx);
        }

        return sInstance;
    }

    private static CmccAppListCustomizerStub getInstanceFromConfigFile() {
        // Get class name from config file
        Context ctx = LauncherAppState.getInstance().getContext();
        if (ctx == null) {
            return null;
        }
        Resources res = ctx.getResources();
        String className = res.getString(R.string.app_list_customizer);
        if (TextUtils.isEmpty(className)) {
            return null;
        }

        // Reflect to instance the customizer
        try {
            Class clazz = Class.forName(className);
            Object instance = clazz.newInstance();
            if (instance instanceof CmccAppListCustomizerStub) {
                return (CmccAppListCustomizerStub) instance;
            }
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
        } catch (InstantiationException ex) {
            ex.printStackTrace();
        } catch (IllegalAccessException ex) {
            ex.printStackTrace();
        }

        return null;
    }

    public final boolean hasCustomizeData() {
        return mHasCustomizeData;
    }

    protected void onSortApps(ArrayList<ComponentName> componentNames) {
        // empty implementation
        Log.d(TAG, "onSortApps empty implementation.");
    }

    /**
     * Note: sub-class should call super.init(...) to mark initialized status
     * after all initialized work done.
     * @param context
     */
    protected void init(Context context) {
        mInitialized = true;
    }

    public final void sortApps(ArrayList<AppInfo> apps) {
        if (!hasCustomizeData()) {
            return;
        }

        ArrayList<ComponentName> sortedCNs = new ArrayList<ComponentName>();
        HashMap<ComponentName, AppInfo> maps = new HashMap<ComponentName, AppInfo>();

        for (AppInfo appInfo : apps) {
            sortedCNs.add(appInfo.componentName);
            maps.put(appInfo.componentName, appInfo);
        }

        onSortApps(sortedCNs);

        // refresh mApps
        apps.clear();
        for (ComponentName cn : sortedCNs) {
            apps.add(maps.get(cn));
        }
    }

    public boolean isInitialized() {
        return mInitialized;
    }
}
