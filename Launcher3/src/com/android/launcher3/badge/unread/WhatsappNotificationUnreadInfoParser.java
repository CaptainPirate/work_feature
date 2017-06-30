/*
  * Copyright @ 2015 China Mobile Group Device Co.,Ltd.
  * All rights Reserved.
*/

package com.android.launcher3.badge.unread;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Notification;
import android.content.ComponentName;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.android.launcher3.badge.unread.CmccNotificationListenerService.NotifyType;

public class WhatsappNotificationUnreadInfoParser extends NotificationUnreadInfoParser {

    private static final String TAG = "WhatsappNotificationUnreadInfoParser";

    private static Pattern sPattern = Pattern.compile("[^\\d]*(\\d+).*$");
    private static Pattern sPattern4SinglePeopel = Pattern.compile("^\\d+@s.whatsapp.net$");

    public WhatsappNotificationUnreadInfoParser() {
        super(new ComponentName("com.whatsapp", "com.whatsapp.Main"));
    }

    @Override
    public int onParseNotifications(StatusBarNotification[] sbns,
            NotifyType type) {
        int unreadNum = 0;

        for (StatusBarNotification sbn : sbns) {
            if (!needHandle(sbn)) {
                continue;
            }
            Notification nt = sbn.getNotification();
            if (nt != null) {
                Bundle bd = nt.extras;
                CharSequence csSummery = bd.getCharSequence(Notification.EXTRA_SUMMARY_TEXT);
                if (csSummery == null) {
                    continue;
                }
                String summery = csSummery.toString();
                Matcher m = sPattern.matcher(summery);
                if (m.find()) {
                    String numStr = m.group(1);
                    try {
                        int num = Integer.parseInt(numStr);
                        unreadNum += num;
                    } catch (NumberFormatException ex) {
                        Log.e(TAG, "parse number error, str: " + numStr);
                    }
                }
            }
        }
        return unreadNum;
    }

}
