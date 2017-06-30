package com.android.launcher3.badge.unread;

import java.util.HashSet;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract.Attendees;
import android.provider.CalendarContract.CalendarAlerts;
import android.util.Log;

public class CalendarUndismissedAlertLoader extends DBUnreadInfoLoader {

    private static final String TAG = "CalendarUndismissedAlertLoader";

    private static final Uri FETCH_URI = CalendarAlerts.CONTENT_URI;

    private static final String[] FETCH_PROJECTION = new String[] {
            CalendarAlerts.EVENT_ID, CalendarAlerts.SELF_ATTENDEE_STATUS };

    private static final String[] FETCH_SELECTION_ARGS = new String[] {
            Integer.toString(CalendarAlerts.STATE_FIRED),
            Integer.toString(CalendarAlerts.STATE_SCHEDULED) };

    private static final String FETCH_SELECTION = "(" + CalendarAlerts.STATE
            + "=? OR " + CalendarAlerts.STATE + "=?) AND "
            + CalendarAlerts.ALARM_TIME + "<=";

    public CalendarUndismissedAlertLoader(Context context) {
        super(context);
    }

    @Override
    public int getUnreadInfoFromCursor(Cursor cursor) {
        int count = 0;
        if (cursor == null) {
            return count;
        }
        HashSet<Long> ids = new HashSet<Long>();
        while(cursor.moveToNext()) {
            long id = cursor.getLong(0);
            int status = cursor.getInt(1);
            if (!(status == Attendees.ATTENDEE_STATUS_DECLINED)
                    && !ids.contains(id)) {
                ids.add(id);
                count++;
            }
        }
        return count;
    }

    @Override
    public Uri getFetchUri() {
        return FETCH_URI;
    }

    @Override
    public Uri getContentObserverUri() {
        return FETCH_URI;
    }

    @Override
    public String[] getFetchProjection() {
        return FETCH_PROJECTION;
    }

    @Override
    public String getFetchSelection() {
        return FETCH_SELECTION + System.currentTimeMillis();
    }

    @Override
    public String[] getFetchSelectionArgs() {
        return FETCH_SELECTION_ARGS;
    }

}
