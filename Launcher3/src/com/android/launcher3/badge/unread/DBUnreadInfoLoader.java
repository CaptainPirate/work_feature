package com.android.launcher3.badge.unread;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

public abstract class DBUnreadInfoLoader {

    protected Context mContext;

    public DBUnreadInfoLoader(Context context) {
        if (context == null) {
            throw new NullPointerException("Parameter context must not be null.");
        }
        mContext = context;
    }

    public int getUnreadInfoCount() {
        ContentResolver resolver = mContext.getContentResolver();
        Cursor cursor = resolver.query(getFetchUri(), getFetchProjection(),
                getFetchSelection(), getFetchSelectionArgs(),
                getFetchSortOrder());
        int unreadCount = getUnreadInfoFromCursor(cursor);
        if (cursor != null) {
            cursor.close();
        }
        return unreadCount;
    }

    /**
     * Note: cursor will close by DBUnreadInfoLoader, sub-classes no need to
     * close it.
     *
     * @param cursor
     * @return the specific unread info count.
     */
    protected abstract int getUnreadInfoFromCursor(Cursor cursor);

    public abstract Uri getFetchUri();

    public abstract Uri getContentObserverUri();

    public String[] getFetchProjection() {
        return null;
    }

    public String getFetchSelection() {
        return null;
    }

    public String[] getFetchSelectionArgs() {
        return null;
    }

    public String getFetchSortOrder() {
        return null;
    }

}
