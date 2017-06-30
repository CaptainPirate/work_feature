/*
  * Copyright @ 2015 China Mobile Group Device Co.,Ltd.
  * All rights Reserved.
*/

package com.android.launcher3.badge.unread;

import com.android.launcher3.badge.unread.CmccNotificationListenerService.NotifyType;

import android.content.ComponentName;
import android.service.notification.StatusBarNotification;

public abstract class NotificationUnreadInfoParser {

    protected ComponentName mComponentName;

    public NotificationUnreadInfoParser(ComponentName cn) {
        if (cn == null) {
            throw new IllegalArgumentException("Parameter cn must not be null.");
        }
        mComponentName = cn;
    }

    public final ComponentName getComponentName() {
        return mComponentName;
    }

    protected boolean needHandle(StatusBarNotification sbn) {
        if (sbn == null) {
            return false;
        }
        String pkg = sbn.getPackageName();
        if (mComponentName != null && pkg != null
                && pkg.equals(mComponentName.getPackageName())) {
            return true;
        }
        return false;
    }

    public int parseNotifications(StatusBarNotification[] sbns, NotifyType type) {
        if (sbns == null || sbns.length == 0) {
            return 0;
        }
        return onParseNotifications(sbns, type);
    }

    public abstract int onParseNotifications(StatusBarNotification[] sbns, NotifyType type);
}
