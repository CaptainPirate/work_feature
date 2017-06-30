/*
  * Copyright @ 2015 China Mobile Group Device Co.,Ltd.
  * All rights Reserved.
*/

package com.android.launcher3;

import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

/*******************************************************************************************************
|   when     |      who     |    keyword       |        why        |     what                          |
********************************************************************************************************
20160420     liyichong.wt   bug 155071          requirements        add search modify
******************************************************************************************************/
public class CmccN2CmccSearchAppsProvider extends ContentProvider {
    private static final String TAG = "CmccN2CmccSearchAppsProvider";

    private DatabaseHelper mDBHelper;

    private static final UriMatcher mUriMatcher;
    private static final int MATCH_FREQUENT_USED_APPS = 1;

    public static final String AUTHORITY = "com.android.launcher3.cmccsearchapps";

    static {
        mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        mUriMatcher.addURI(AUTHORITY, CmccSearchApps.TABLE_NAME,
                MATCH_FREQUENT_USED_APPS);
    }

    @Override
    public boolean onCreate() {
        mDBHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();

        switch (mUriMatcher.match(uri)) {
        case MATCH_FREQUENT_USED_APPS:
            builder.setTables(CmccSearchApps.TABLE_NAME);
            break;

        default:
            throw new IllegalArgumentException("query, uri can not match, uri: " + uri);
        }

        SQLiteDatabase db = mDBHelper.getReadableDatabase();
        return builder.query(db, projection, selection, selectionArgs, null, null, sortOrder);
    }

    @Override
    public String getType(Uri uri) {
        switch (mUriMatcher.match(uri)) {
        case MATCH_FREQUENT_USED_APPS:
            return "vnd.android.cursor.dir/" + CmccSearchApps.TABLE_NAME;

        default:
            return null;
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        if (values == null) {
            return null;
        }

        long rowId = -1;
        SQLiteDatabase db = mDBHelper.getWritableDatabase();

        switch (mUriMatcher.match(uri)) {
        case MATCH_FREQUENT_USED_APPS:
            rowId = db.insert(CmccSearchApps.TABLE_NAME, null, values);
            break;

        default:
            Log.d(TAG, "insert, uri can not match, uri: " + uri);
        }

        if (rowId > 0) {
            Uri insertedUri = ContentUris.withAppendedId(uri, rowId);
            getContext().getContentResolver().notifyChange(insertedUri, null);
            return insertedUri;
        }

        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int count = 0;
        SQLiteDatabase db = mDBHelper.getWritableDatabase();

        switch (mUriMatcher.match(uri)) {
        case MATCH_FREQUENT_USED_APPS:
            count = db.delete(CmccSearchApps.TABLE_NAME, selection, selectionArgs);
            break;

        default:
            break;
        }

        if (count > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        int count = 0;
        SQLiteDatabase db = mDBHelper.getWritableDatabase();

        switch (mUriMatcher.match(uri)) {
        case MATCH_FREQUENT_USED_APPS:
            count = db.update(CmccSearchApps.TABLE_NAME, values, selection, selectionArgs);
            break;

        default:
            break;
        }

        if (count > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return count;
    }

    public static class CmccSearchApps implements BaseColumns {
        public static final String TABLE_NAME = "frequent_used_apps";

        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY
                + "/" + CmccSearchApps.TABLE_NAME);

        public static final String COMPONENT_NAME = "component_name";
        public static final String USED_COUNT = "used_count";
        public static final String TIMESTAMP = "timestamp";

        public static final int ID_INDEX = 0;
        public static final int COMPONENT_NAME_INDEX = 1;
        public static final int USED_COUNT_INDEX = 2;
        public static final int TIMESTAMP_INDEX = 3;
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {

        private static final String DATABASE_NAME = "frequent_used_apps.db";
        private static final int DATABASE_VERSION = 1;

        public DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("create table IF NOT EXISTS " + CmccSearchApps.TABLE_NAME
                    + "("
                    + CmccSearchApps._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + CmccSearchApps.COMPONENT_NAME + " TEXT,"
                    + CmccSearchApps.USED_COUNT + " INTEGER DEFAULT 1,"
                    + CmccSearchApps.TIMESTAMP + " BIGINT)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (oldVersion != DATABASE_VERSION) {
                // drop current tables
                db.execSQL("DROP TABLE IF EXISTS " + CmccSearchApps.TABLE_NAME);
                // recreate database
                onCreate(db);
            }
        }
    }

    public static int deleteFrequentUsedAppsRecord(ContentResolver resolver, long id) {
        if (id < 0) {
            Log.w(TAG, "deleteFrequentUsedAppsRecord, parameter id: " + id);
            return 0;
        }

        return resolver.delete(CmccSearchApps.CONTENT_URI,
                CmccSearchApps._ID + "=?", new String[] {String.valueOf(id)});
    }

    public static void insertOrUpdateFrequentUsedAppsRecord(ContentResolver resolver, Intent intent) {
        if (intent == null) {
            Log.w(TAG, "intent is null, not a valaid parameter.");
            return;
        }

        long foundId = -1;
        int usedCount = 0;
        ComponentName cn = intent.getComponent();
        if (cn == null) {
            Log.w(TAG, "Currently can not statistic usage data, compnent is null for intent: " + intent);
            return;
        }

        String cnStr = cn.flattenToString();

        // get content resolver
        Cursor cursor = resolver.query(CmccSearchApps.CONTENT_URI, null,
                CmccSearchApps.COMPONENT_NAME + "=?", new String[]{cnStr}, null);

        if (cursor != null) {
            if (cursor.moveToNext()) {
                foundId = cursor.getLong(CmccSearchApps.ID_INDEX);
                usedCount = cursor.getInt(CmccSearchApps.USED_COUNT_INDEX);
            }
            cursor.close();
        }

        long timestamp = System.currentTimeMillis();

        ContentValues values = new ContentValues();
        values.put(CmccSearchApps.TIMESTAMP, timestamp);
        if (foundId == -1) {
            // insert new record
            values.put(CmccSearchApps.COMPONENT_NAME, cnStr);
            values.put(CmccSearchApps.TIMESTAMP, timestamp);
            resolver.insert(CmccSearchApps.CONTENT_URI, values);
        } else {
            // update exist record
            values.put(CmccSearchApps.USED_COUNT, usedCount + 1);
            resolver.update(CmccSearchApps.CONTENT_URI, values,
                    CmccSearchApps._ID + "=?",
                    new String[] {String.valueOf(foundId)});
        }
    }

}
