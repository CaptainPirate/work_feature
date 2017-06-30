/*
  * Copyright @ 2015 China Mobile Group Device Co.,Ltd.
  * All rights Reserved.
*/

package com.android.launcher3;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.os.Build;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pair;
import android.util.Xml;

public class CmccAppListCustomizer extends CmccAppListCustomizerStub {
    private static final String TAG = "CmccAppListCustomizer";

    private static final String ROOT_TAG = "apps";
    private static final String APP_TAG = "app";

    private HashMap<Integer, Pair<String, String>> mCustomizePositions;

    private static final long INVALID_INSTALL_TIME = -1L;

    private PackageManager mPakcageManager;

    private boolean mSortByInstalledTime;

    @Override
    protected void init(Context context) {
        mPakcageManager = context.getPackageManager();
        mSortByInstalledTime = context.getResources().getBoolean(
                R.bool.uncustomized_app_sort_by_installed_time);
        mCustomizePositions = loadCustomizeAppPositions(context);
        if (mCustomizePositions != null) {
            mHasCustomizeData = mCustomizePositions.size() > 0;
        }
        super.init(context);
    }

    @Override
    protected void onSortApps(ArrayList<ComponentName> componentNames) {
        Log.d(TAG, "onSortApps customize implementation.");
        TreeMap<Integer, ComponentName> sortedMaps = new TreeMap<Integer, ComponentName>();

        // find the customize component in componentNames
        Pair<String, String> pair = null;
        Entry<Integer, Pair<String, String>> entry = null;

        for (ComponentName cn : componentNames) {
            Iterator<Entry<Integer, Pair<String, String>>> it = mCustomizePositions.entrySet().iterator();
            while(it.hasNext()) {
                entry = it.next();
                pair = entry.getValue();

                if (pair.first.equals(cn.getPackageName())) {
                    if (pair.second == null || pair.second.equals(cn.getClassName())) {
                        sortedMaps.put(entry.getKey(), cn);
                        break;
                    }
                }
            }
        }

        // remove the found component
        Iterator<Entry<Integer, ComponentName>> it = sortedMaps.entrySet().iterator();
        while(it.hasNext()) {
            componentNames.remove(it.next().getValue());
        }

        if (mSortByInstalledTime) {
            // sort un-customized app by installed time
            TreeMap<Long, ArrayList<ComponentName>> sortedByInstalledTime = new TreeMap<Long, ArrayList<ComponentName>>();
            int leftCount = componentNames.size();
            for (int i = 0; i < leftCount; i++) {
                // get first installed time, API level must > 9
                ComponentName cn = componentNames.get(i);
                long installedTime = installedTimeFromPackageManager(mPakcageManager,
                        cn.getPackageName(), true);
                if (sortedByInstalledTime.containsKey(installedTime)) {
                    sortedByInstalledTime.get(installedTime).add(cn);
                } else {
                    ArrayList<ComponentName> arr = new ArrayList<ComponentName>();
                    arr.add(cn);
                    sortedByInstalledTime.put(installedTime, arr);
                }
            }
            componentNames.clear();
            Entry<Long, ArrayList<ComponentName>> ent2 = null;
            Iterator<Entry<Long, ArrayList<ComponentName>>> it2 = sortedByInstalledTime.entrySet()
                    .iterator();
            while (it2.hasNext()) {
                ent2 = it2.next();
                componentNames.addAll(ent2.getValue());
            }
        }

        int position = 0;
        Entry<Integer, ComponentName> ent = null;
        it = sortedMaps.entrySet().iterator();
        while(it.hasNext()) {
            ent = it.next();
            componentNames.add(position, ent.getValue());
            position++;
        }
        /*
        // insert at the customize position
        Entry<Integer, ComponentName> ent = null;
        it = sortedMaps.entrySet().iterator();
        while(it.hasNext()) {
            ent = it.next();
            if (ent.getKey() > componentNames.size()) {
                // append to last position
                componentNames.add(ent.getValue());
            } else {
                // insert at specific position
                componentNames.add(ent.getKey(), ent.getValue());
            }
        }
        */
    }

    /**
     * Get customize app's position. The result is a map, the key indicate the
     * customize position, and the value is a pair of package name and class name.
     *
     * @param context
     * @return
     */
    private HashMap<Integer, Pair<String, String>> loadCustomizeAppPositions(Context context) {
        HashMap<Integer, Pair<String, String>> customizePositions = new HashMap<Integer, Pair<String, String>>();
        try {
            XmlResourceParser parser = context.getResources().getXml(R.xml.app_positions);
            AttributeSet attrs = Xml.asAttributeSet(parser);

            int position = 0;
            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if(eventType == XmlPullParser.START_TAG) {
                    String tagName = parser.getName();
                    if (APP_TAG.equals(tagName)) {
                        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CustomizeAppList);
                        String pkgName = a.getString(R.styleable.CustomizeAppList_pkgName);
                        String clsName = a.getString(R.styleable.CustomizeAppList_clsName);

                        // package name must not be null or empty
                        if (!TextUtils.isEmpty(pkgName)) {
                            customizePositions.put(position++,
                                    new Pair<String, String>(pkgName, clsName));
                        }
                        a.recycle();
                    }
                }
                eventType = parser.next();
            }
        } catch (XmlPullParserException e) {
            Log.w(TAG, "parse xml failed", e);
        } catch (IOException e) {
            Log.w(TAG, "parse xml failed", e);
        } catch (RuntimeException e) {
            Log.w(TAG, "parse xml failed", e);
        }
        return customizePositions;
    }

    private long installedTimeFromPackageManager(PackageManager packageManager, String packageName,
            boolean useUpdateTime) {
        // API level 9 and above have the "firstInstallTime" field.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            PackageInfo info;
            try {
                info = packageManager.getPackageInfo(packageName, 0);
                return info.firstInstallTime;
            } catch (NameNotFoundException e) {
                // ignore
            }
        }
        if (useUpdateTime) {
            return apkUpdateTime(packageManager, packageName);
        } else {
            return INVALID_INSTALL_TIME;
        }
    }

    private long apkUpdateTime(PackageManager packageManager, String packageName) {
        try {
            ApplicationInfo info = packageManager.getApplicationInfo(packageName, 0);
            File apkFile = new File(info.sourceDir);
            return apkFile.exists() ? apkFile.lastModified() : INVALID_INSTALL_TIME;
        } catch (NameNotFoundException e) {
            return INVALID_INSTALL_TIME;
        }
    }
}
