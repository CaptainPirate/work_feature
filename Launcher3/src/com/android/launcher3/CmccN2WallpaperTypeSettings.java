/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.launcher3;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/*******************************************************************************************************
|   when     |      who     |    keyword       |        why        |     what                          |
********************************************************************************************************
20160422     liyichong.wt   bug 155015          requirements        add more wallpaper settings
20160505     liyichong.wt   bug 169427          requirements        modify enter gallary path error
******************************************************************************************************/
public class CmccN2WallpaperTypeSettings extends PreferenceActivity {

    /* bug 169427, liyichong.wt, ADD, 20160505 start*/
    public static final String TAG = "CmccN2WallpaperTypeSettings";
    public static final String EXTRA_WALLPAPER_TARGET = "wallpaper_target";
    public static final String TARGET_LOCKSCREEN = "lockscreen";
    protected static boolean sForLockscreen;
    /* bug 169427, liyichong.wt, ADD, 20160505 end*/

    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.wallpaper_settings);
        /* bug 169427, liyichong.wt, ADD, 20160505 start*/
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(EXTRA_WALLPAPER_TARGET)) {
            sForLockscreen = intent.getExtras().getString(EXTRA_WALLPAPER_TARGET).equals(TARGET_LOCKSCREEN);
        } else {
            sForLockscreen = false;
        }
        /* bug 169427, liyichong.wt, ADD, 20160505 end*/
        populateWallpaperTypes();
    }

    private void populateWallpaperTypes() {
        // Search for activities that satisfy the ACTION_SET_WALLPAPER action
        final Intent intent = new Intent(Intent.ACTION_SET_WALLPAPER);
        final PackageManager pm = getPackageManager();
        final List<ResolveInfo> rList = pm.queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY);

        final PreferenceScreen parent = getPreferenceScreen();
        parent.setOrderingAsAdded(false);
        // Add Preference items for each of the matching activities
        for (ResolveInfo info : rList) {
            if(info.activityInfo.packageName.contains("com.android.wallpaper.livepicker") ||
                    info.activityInfo.packageName.contains("com.android.launcher3"))continue;
            Preference pref = new Preference(this.getApplicationContext());
            Intent prefIntent = new Intent(intent);
            prefIntent.setComponent(new ComponentName(
                    info.activityInfo.packageName, info.activityInfo.name));
            prefIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            /* bug 169427, liyichong.wt, ADD, 20160505 start*/
            if(info.activityInfo.packageName.contains("com.android.gallery3d") && sForLockscreen){
                prefIntent.putExtra("from",TARGET_LOCKSCREEN);
            }
            /* bug 169427, liyichong.wt, ADD, 20160505 end*/
            pref.setIntent(prefIntent);
            CharSequence label = info.loadLabel(pm);
            if (label == null) label = info.activityInfo.packageName;
            pref.setLayoutResource(R.layout.preference_text_view);
            pref.setTitle(label);
            
            parent.addPreference(pref);
        }
    }
}
