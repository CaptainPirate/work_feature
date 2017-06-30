/*
  * Copyright @ 2015 China Mobile Group Device Co.,Ltd.
  * All rights Reserved.
*/

package com.android.launcher3.badge.unread;

import com.android.launcher3.CmccUnreadInfoManager;
import com.android.launcher3.LauncherAppState;

import android.app.Notification;
import android.content.Intent;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

public class CmccNotificationListenerService extends NotificationListenerService {

    private static final String TAG = "CmccNotificationListenerService";
    private static final boolean DEBUG = false;

    public enum NotifyType {
        POST, REMOVED, CONNECTED
    };

    @Override
    public void onDestroy() {
        // TODO: need to reenable notification listener service.
        super.onDestroy();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        parseUnreadInfo(NotifyType.POST);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        parseUnreadInfo(NotifyType.REMOVED);
    }

    @Override
    public void onListenerConnected() {
        parseUnreadInfo(NotifyType.CONNECTED);
    }

    private void parseUnreadInfo(NotifyType type) {
        StatusBarNotification[] sbns = getActiveNotifications();

        if (DEBUG) {
            for (StatusBarNotification sbn : sbns) {
                helpFunc(sbn);
            }
        }

        CmccUnreadInfoManager manager = LauncherAppState.getInstance()
                .getUnreadInfoManager();
        if (manager != null) {
            manager.parseNotifications(sbns, type);
        }
    }

    private void helpFunc(StatusBarNotification sbn) {
        Log.d(TAG, "***************************** sbn *************************\n" + sbn);
        Notification nt = sbn.getNotification();
        if (nt != null) {
            Bundle bd = nt.extras;
            if (bd != null) {
                String title = bd.getString(Notification.EXTRA_TITLE);
                String titlebig = bd.getString(Notification.EXTRA_TITLE_BIG);
                CharSequence text = bd.getCharSequence(Notification.EXTRA_TEXT);
                String subtext = bd.getString(Notification.EXTRA_SUB_TEXT);
                String infotext = bd.getString(Notification.EXTRA_INFO_TEXT);
                String summarytext = bd.getString(Notification.EXTRA_SUMMARY_TEXT);
                Log.d(TAG, "title: " + title + ", titlebig: " + titlebig
                        + ", text: " + text + ", subtext: " + subtext
                        + ", infotext: " + infotext + ", summarytext: "
                        + summarytext);
            }
        }
    }

}
