/*
  * Copyright @ 2015 China Mobile Group Device Co.,Ltd.
  * All rights Reserved.
*/

package com.android.launcher3.badge.unread;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.android.launcher3.badge.unread.CmccNotificationListenerService.NotifyType;

import android.app.Notification;
import android.content.ComponentName;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.util.Log;

public class FetionNotificationUnreadInfoParser extends NotificationUnreadInfoParser {

    private static final String TAG = "FetionNotificationUnreadInfoParser";

    private static Pattern sPattern = Pattern.compile("[^\\d]+(\\d+)");

    // TODO: need to compatible with Multi-Language
    private static Pattern sPattern4ConfirmMsg = Pattern.compile("^验证消息$");
    private static Pattern sPattern4TitleFetion = Pattern.compile("^飞信$");

    private HashMap<String, Integer> mTitleMap = new HashMap<String, Integer>();

    public FetionNotificationUnreadInfoParser() {
        super(new ComponentName("cn.com.fetion",
                "cn.com.fetion.activity.LaunchActivity"));
    }

    @Override
    public int onParseNotifications(StatusBarNotification[] sbns, NotifyType type) {
        int unreadNum = 0;
        boolean handleNothing = true;
        for (StatusBarNotification sbn : sbns) {
            if (!needHandle(sbn)) {
                continue;
            }
            handleNothing = false;
            Notification nt = sbn.getNotification();
            if (nt != null) {
                Bundle bd = nt.extras;
                String title = bd.getCharSequence(Notification.EXTRA_TITLE).toString();
                if (title == null) {
                    continue;
                }
                if (sPattern4ConfirmMsg.matcher(title).find()) {
                    // confirm msg
                    unreadNum++;
                    continue;
                }

                String text = bd.getCharSequence(Notification.EXTRA_TEXT).toString();
                if (text == null) {
                    continue;
                }

                if (sPattern4TitleFetion.matcher(title).find()) {
                    Matcher m = sPattern.matcher(text);
                    if (m.find()) {
                        String numStr = m.group(1);
                        try {
                            int num = Integer.parseInt(numStr);
                            unreadNum += num;
                        } catch (NumberFormatException ex) {
                            Log.e(TAG, "parse number error, str: *" + numStr + "*");
                        }
                    } else {
                        unreadNum++;
                    }
                } else {
                    switch(type) {
                    case CONNECTED:
                    case POST:
                        // check title exist
                        if (mTitleMap.containsKey(title)) {
                            mTitleMap.put(title, (mTitleMap.get(title) + 1));
                        } else {
                            mTitleMap.put(title, 1);
                        }
                        unreadNum += mTitleMap.get(title);
                        break;
                    case REMOVED:
                        if (mTitleMap.containsKey(title)) {
                            unreadNum -= mTitleMap.remove(title);
                        }
                        break;
                    }
                }
            }
        }
        if (handleNothing) {
            mTitleMap.clear();
        }
        return unreadNum;
    }

}
