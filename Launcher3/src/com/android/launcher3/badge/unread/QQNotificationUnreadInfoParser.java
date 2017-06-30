/*
  * Copyright @ 2015 China Mobile Group Device Co.,Ltd.
  * All rights Reserved.
*/

package com.android.launcher3.badge.unread;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.android.launcher3.badge.unread.CmccNotificationListenerService.NotifyType;

import android.app.Notification;
import android.content.ComponentName;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.util.Log;

public class QQNotificationUnreadInfoParser extends NotificationUnreadInfoParser {

    private static final String TAG = "QQNotificationUnreadInfoParser";

    private static Pattern sPattern = Pattern.compile("\\((\\d*).*\\)$");

    public QQNotificationUnreadInfoParser() {
        super(new ComponentName("com.tencent.mobileqq",
                "com.tencent.mobileqq.activity.SplashActivity"));
    }

    @Override
    public int onParseNotifications(StatusBarNotification[] sbns, NotifyType type) {
        int unreadNum = 0;
        for (StatusBarNotification sbn : sbns) {
            if (!needHandle(sbn)) {
                continue;
            }
            Notification nt = sbn.getNotification();
            if (nt != null) {
                Bundle bd = nt.extras;
                String title = bd.getCharSequence(Notification.EXTRA_TITLE).toString();
                if (title == null) {
                    continue;
                }
                Matcher m = sPattern.matcher(title);
                if (m.find()) {
                    String numStr = m.group(1);
                    try {
                        int num = Integer.parseInt(numStr);
                        unreadNum += num;
                    } catch (NumberFormatException ex) {
                        Log.e(TAG, "parse number error, str: " + numStr);
                    }
                } else {
                    unreadNum++;
                }
            }
        }
        return unreadNum;
    }
}
