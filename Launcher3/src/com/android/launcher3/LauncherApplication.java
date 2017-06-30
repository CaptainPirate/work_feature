/*
 * Copyright (C) 2013 The Android Open Source Project
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

import android.app.Application;
import com.iflytek.news.NewsSDKFactory;

public class LauncherApplication extends Application {


    @Override
    public void onCreate() {
        super.onCreate();

        /*if (Utilities.supportLeftScreen()) {
            NewsSDKFactory.createSdk("WA1NM7NA", "320838f15ccc5f127f9ee0ff0eb070da", this);
            NewsSDKFactory.setDebug(true);
        }*/
        LauncherAppState.setApplicationContext(this);
        LauncherAppState.getInstance();
        CmccDynamicCalendar.getInstance().registerBroadcastReceiver();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        LauncherAppState.getInstance().onTerminate();
        CmccDynamicCalendar.getInstance().unregisterBroadcastReceiver();
    }
}
