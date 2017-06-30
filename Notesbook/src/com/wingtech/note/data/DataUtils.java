/*===================================================================================================*
 *  when  |      who     |    keyword           |        why         |         what                  *
 *===================================================================================================*
 *20160407|mengzhiming.wt|porting A1s enjoynotes| Port_EnjoyNotes | porting A1s enjoynotes to CMCC N2*
*====================================================================================================*
*20160531|mengzhiming.wt|   customer req       | customer req    | customer req                      *
 *===================================================================================================*/
package com.wingtech.note.data;

import android.content.AsyncQueryHandler;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.util.Log;

import com.wingtech.note.Utils;
import com.wingtech.note.data.NoteConstant.CallNote;
import com.wingtech.note.data.NoteConstant.DataColumns;
import com.wingtech.note.data.NoteConstant.NoteColumns;
import com.wingtech.note.data.NoteConstant.AttachmentColumns;

import java.util.ArrayList;
import java.util.HashSet;

public class DataUtils {
    public static final String TAG = "DataUtils";

    public static boolean batchDeleteNotes(ContentResolver resolver, HashSet<Long> ids) {
        if (ids == null) {
            Log.d(TAG, "the ids is null");
            return true;
        }
        if (ids.size() == 0) {
            Log.d(TAG, "no id is in the hashset");
            return true;
        }
        ArrayList<ContentProviderOperation> operationList = new ArrayList<ContentProviderOperation>();
        for (long id : ids) {
            if (id == NoteConstant.ID_ROOT_FOLDER) {
                Log.e(TAG, "Don't delete system folder root");
                continue;
            }
            ContentProviderOperation.Builder builder = ContentProviderOperation
                    .newDelete(ContentUris.withAppendedId(NoteConstant.CONTENT_NOTE_URI, id));
            operationList.add(builder.build());
        }
        try {
            ContentProviderResult[] results = resolver.applyBatch(NoteConstant.AUTHORITY,
                    operationList);
            if (results == null || results.length == 0 || results[0] == null) {
                Log.d(TAG, "delete notes failed, ids:" + ids.toString());
                return false;
            }
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, String.format("%s: %s", e.toString(), e.getMessage()));
        } catch (OperationApplicationException e) {
            Log.e(TAG, String.format("%s: %s", e.toString(), e.getMessage()));
        }
        return false;
    }

    public static void moveNoteToFoler(ContentResolver resolver, long id, long srcFolderId,
            long desFolderId) {
        ContentValues values = new ContentValues();
        values.put(NoteColumns.PARENT_ID, desFolderId);
        values.put(NoteColumns.ORIGIN_PARENT_ID, srcFolderId);
        values.put(NoteColumns.LOCAL_MODIFIED, 1);
        resolver.update(ContentUris.withAppendedId(NoteConstant.CONTENT_NOTE_URI, id), values,
                null, null);
    }

    public static boolean batchMoveToFolder(ContentResolver resolver, HashSet<Long> ids,
            long folderId) {
        if (ids == null) {
            Log.d(TAG, "the ids is null");
            return true;
        }

        ArrayList<ContentProviderOperation> operationList = new ArrayList<ContentProviderOperation>();
        for (long id : ids) {
            ContentProviderOperation.Builder builder = ContentProviderOperation
                    .newUpdate(ContentUris.withAppendedId(NoteConstant.CONTENT_NOTE_URI, id));
            builder.withValue(NoteColumns.PARENT_ID, folderId);
            builder.withValue(NoteColumns.LOCAL_MODIFIED, 1);
            operationList.add(builder.build());
        }

        try {
            ContentProviderResult[] results = resolver.applyBatch(NoteConstant.AUTHORITY,
                    operationList);
            if (results == null || results.length == 0 || results[0] == null) {
                Log.d(TAG, "delete notes failed, ids:" + ids.toString());
                return false;
            }
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, String.format("%s: %s", e.toString(), e.getMessage()));
        } catch (OperationApplicationException e) {
            Log.e(TAG, String.format("%s: %s", e.toString(), e.getMessage()));
        }
        return false;
    }

    /**
     * Get the all folder count except system folders
     * {@link NoteConstant#TYPE_SYSTEM}
     */
    public static int getUserFolderCount(ContentResolver resolver) {
        Cursor cursor = resolver.query(
                NoteConstant.CONTENT_NOTE_URI,
                new String[] {
                    "COUNT(*)"
                },
                NoteColumns.TYPE + "=? AND " + NoteColumns.PARENT_ID + "<>?",
                new String[] {
                        String.valueOf(NoteConstant.TYPE_FOLDER),
                        String.valueOf(NoteConstant.ID_TRASH_FOLER)
                },
                null);

        int count = 0;
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    count = cursor.getInt(0);
                }
            } catch (IndexOutOfBoundsException e) {
                Log.e(TAG, "get folder count failed:" + e.toString());
            } finally {
                cursor.close();
            }
        }
        return count;
    }

    /*
     * 判断该note是否可见
     */
    public static boolean visibleInNoteDatabase(ContentResolver resolver, long noteId, int type) {
        Cursor cursor = resolver.query(
                ContentUris.withAppendedId(NoteConstant.CONTENT_NOTE_URI, noteId),
                null,
                NoteColumns.TYPE + "=? AND " + NoteColumns.PARENT_ID + "<>"
                        + NoteConstant.ID_TRASH_FOLER,
                new String[] {
                    String.valueOf(type)
                },
                null);

        boolean exist = false;
        try {
            if (cursor != null) {
                if (cursor.getCount() > 0) {
                    exist = true;
                }

            }
        } catch (Exception e) {
            // TODO: handle exception
        } finally {
            cursor.close();
        }
        return exist;
    }

    /*
     * 判断该note是否存在于note数据库中
     */
    public static boolean existInNoteDatabase(ContentResolver resolver, long noteId) {
        Cursor cursor = resolver.query(
                ContentUris.withAppendedId(NoteConstant.CONTENT_NOTE_URI, noteId),
                null, null, null, null);

        boolean exist = false;
        try {
            if (cursor != null) {
                if (cursor.getCount() > 0) {
                    exist = true;
                }
                cursor.close();
            }
        } catch (Exception e) {
            // TODO: handle exception
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return exist;
    }

    /*
     * 判断该记录是否在data数据库中
     */
    public static boolean existInDataDatabase(ContentResolver resolver, long dataId) {
        Cursor cursor = resolver.query(
                ContentUris.withAppendedId(NoteConstant.CONTENT_DATA_URI, dataId),
                null, null, null, null);

        boolean exist = false;
        try {
            if (cursor != null) {
                if (cursor.getCount() > 0) {
                    exist = true;
                }
            }
        } catch (Exception e) {
            // TODO: handle exception
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return exist;
    }

    /*
     * 判断传进来的文件夹名称是否存在
     */
    public static boolean checkVisibleFolderName(ContentResolver resolver, String name) {
        Cursor cursor = resolver.query(NoteConstant.CONTENT_NOTE_URI, null,
                NoteColumns.TYPE + "=" + NoteConstant.TYPE_FOLDER +
                        " AND " + NoteColumns.PARENT_ID + "<>" + NoteConstant.ID_TRASH_FOLER +
                        " AND " + NoteColumns.SNIPPET + "=?",
                new String[] {
                    name
                }, null);
        boolean exist = false;
        try {
            if (cursor != null) {
                if (cursor.getCount() > 0) {
                    exist = true;
                }
            }
        } catch (Exception e) {
            // TODO: handle exception
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return exist;
    }

    public static String getCallNumberByNoteId(ContentResolver resolver, long noteId) {
        Cursor cursor = resolver.query(NoteConstant.CONTENT_DATA_URI,
                new String[] {
                    CallNote.PHONE_NUMBER
                },
                CallNote.NOTE_ID + "=? AND " + CallNote.MIME_TYPE + "=?",
                new String[] {
                        String.valueOf(noteId), CallNote.CONTENT_ITEM_TYPE
                },
                null);

        if (cursor != null) {
            try {
                if (cursor.moveToFirst())
                    return cursor.getString(0);
            } catch (IndexOutOfBoundsException e) {
                Log.e(TAG, "Get call number fails " + e.toString());
            } finally {
                cursor.close();
            }
        }
        return "";
    }

    public static long getNoteIdByPhoneNumberAndCallDate(ContentResolver resolver,
            String phoneNumber, long callDate) {
        Cursor cursor = resolver.query(NoteConstant.CONTENT_DATA_URI,
                new String[] {
                    CallNote.NOTE_ID
                },
                CallNote.CALL_DATE + "=? AND " + CallNote.MIME_TYPE + "=? AND PHONE_NUMBERS_EQUAL("
                        + CallNote.PHONE_NUMBER + ",?)",
                new String[] {
                        String.valueOf(callDate), CallNote.CONTENT_ITEM_TYPE, phoneNumber
                },
                null);

        if (cursor != null) {
            try {
                if (cursor.moveToFirst())
                    return cursor.getLong(0);
            } catch (IndexOutOfBoundsException e) {
                Log.e(TAG, "Get call note id fails " + e.toString());
            } finally {
                cursor.close();
            }
        }
        return 0;
    }

    public static String getSnippetById(ContentResolver resolver, long noteId) {
        Cursor cursor = resolver.query(NoteConstant.CONTENT_NOTE_URI,
                new String[] {
                    NoteColumns.SNIPPET
                },
                NoteColumns.ID + "=?",
                new String[] {
                    String.valueOf(noteId)
                },
                null);
        try {
            if (cursor != null) {
                String snippet = "";
                if (cursor.moveToFirst()) {
                    snippet = cursor.getString(0);
                }

                return snippet;
            }
        } catch (Exception e) {
            // TODO: handle exception
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    public static String getFormattedSnippet(String snippet) {
        if (snippet != null) {
            snippet = snippet.trim();
            int index = snippet.indexOf('\n');
            if (index != -1) {
                snippet = snippet.substring(0, index);
            }
        }
        return snippet;
    }

    public static int[] getWidgetInfoByNoteId(Context context, long noteId) {
        ContentResolver contentResolver = context.getContentResolver();
        Uri url = NoteConstant.CONTENT_NOTE_URI;
        String[] projection = {
                "widget_id", "widget_type"
        };
        Cursor cursor = contentResolver.query(url, projection, "_id=" + noteId, null, null);
        int[] results = null;
        try {
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    results = new int[2];
                    results[0] = cursor.getInt(0);
                    results[1] = cursor.getInt(1);
                }

            }
        } catch (Exception e) {
            // TODO: handle exception
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return results;
    }

    public static String verifyNoteAlert(Context context, long id, long alertDate) {
        String str1 = "(parent_id>=0 OR parent_id=-2) AND _id=" + id + " AND " + "alert_date" + "="
                + alertDate + " AND " + "type" + "=" + 0;
        ContentResolver localContentResolver = context.getContentResolver();
        String[] arrayOfString = new String[1];
        arrayOfString[0] = "snippet";
        Cursor localCursor = localContentResolver.query(NoteConstant.CONTENT_NOTE_URI,
                arrayOfString,
                str1, null, null);
        if (localCursor != null) {
            try {
                if (localCursor.moveToNext()) {
                    return localCursor.getString(0);
                }
            } finally {
                localCursor.close();
            }
        }
        return null;
    }

    public static boolean checkValidFolderName(ContentResolver contentResolver, String str,
            long id) {

        Uri localUri = NoteConstant.CONTENT_NOTE_URI;
        String[] arrayOfString = {
                str, Long.toString(id)
        };
        Cursor localCursor = contentResolver.query(localUri, null,
                "(type=1 AND parent_id<>-3) AND snippet=? AND _id!=?", arrayOfString, null);
        boolean bool = false;
        if (localCursor != null) {
            try {
                if (localCursor.getCount() > 0)
                    bool = true;
            } catch (Exception e) {
                // TODO: handle exception
            } finally {
                localCursor.close();
            }
        }
        return bool;
    }

    public static long insertNoteFolder(Context context, String name) {
        ContentValues localContentValues = new ContentValues();
        localContentValues.put(NoteConstant.NoteColumns.SNIPPET, name);
        localContentValues.put(NoteConstant.NoteColumns.TYPE,
                Integer.valueOf(NoteConstant.TYPE_FOLDER));
        localContentValues.put(NoteConstant.NoteColumns.LOCAL_MODIFIED, Integer.valueOf(1));
        return ContentUris.parseId(context.getContentResolver().insert(
                NoteConstant.CONTENT_NOTE_URI,
                localContentValues));
    }

    public static boolean restoreNoteFolder(Context context, String name) {
        ContentValues localContentValues = new ContentValues();
        localContentValues.put(NoteConstant.NoteColumns.SNIPPET, name);
        localContentValues.put(NoteConstant.NoteColumns.LOCAL_MODIFIED, Integer.valueOf(1));
        localContentValues.put(NoteConstant.NoteColumns.PARENT_ID, Integer.valueOf(0));
        long curTime = System.currentTimeMillis();
        localContentValues.put(NoteConstant.NoteColumns.CREATED_DATE, Long.valueOf(curTime));
        localContentValues.put(NoteConstant.NoteColumns.MODIFIED_DATE, Long.valueOf(curTime));
        String[] arrayOfString = new String[3];
        arrayOfString[0] = Integer.toString(NoteConstant.TYPE_FOLDER);
        arrayOfString[1] = name;
        arrayOfString[2] = Long.toString(NoteConstant.ID_TRASH_FOLER);
        if (context.getContentResolver().update(NoteConstant.CONTENT_NOTE_URI, localContentValues,
                "type=? AND snippet=? AND parent_id=?", arrayOfString) > 0) {
            return true;
        }
        return false;
    }

    public static long getVisibleFolderIdByName(Context context, String name) {
        String[] arrayOfString1 = new String[3];
        arrayOfString1[0] = Integer.toString(NoteConstant.TYPE_NOTE);
        arrayOfString1[1] = name;
        arrayOfString1[2] = Long.toString(NoteConstant.ID_TRASH_FOLER);
        ContentResolver localContentResolver = context.getContentResolver();
        Uri localUri = NoteConstant.CONTENT_NOTE_URI;
        String[] arrayOfString2 = new String[1];
        arrayOfString2[0] = "_id";
        Cursor localCursor = localContentResolver.query(localUri, arrayOfString2,
                "type=? AND snippet=? AND parent_id<>?", arrayOfString1, null);
        if (localCursor != null) {
            try
            {
                if (localCursor.moveToNext()) {
                    long id = localCursor.getLong(0);
                    return id;
                }
            } finally {
                localCursor.close();
            }
        }
        return -1L;
    }

    public static boolean renameNoteFolder(Context context,
            long folderId, String newName) {
        ContentValues localContentValues = new ContentValues();
        localContentValues.put(NoteConstant.NoteColumns.SNIPPET, newName);
        localContentValues.put(NoteConstant.NoteColumns.LOCAL_MODIFIED, 1);
        try {
            ContentResolver localContentResolver = context.getContentResolver();
            Uri localUri = NoteConstant.CONTENT_NOTE_URI;
            String[] arrayOfString = new String[2];
            arrayOfString[0] = Long.toString(folderId);
            arrayOfString[1] = Integer.toString(NoteConstant.TYPE_FOLDER);
            int k = localContentResolver.update(localUri, localContentValues, "_id=? AND type=?",
                    arrayOfString);
            if (k > 0)
                return true;
        } catch (Exception e) {
            Log.e(TAG, "Rename folder failed", e);
        }
        return false;
    }

    public static void stickNotes(Context context,
            HashSet<Long> ids, boolean mode) {
        ContentValues localContentValues = new ContentValues();
        long timeMillis;
        if (mode)
            timeMillis = System.currentTimeMillis();
        else
            timeMillis = 0;
        localContentValues.put("stick_date", Long.valueOf(timeMillis));
        localContentValues.put("local_modified", Integer.valueOf(1));
        String str = "_id IN (" + Utils.congregateAsString(ids, ",") + ")";
        context.getContentResolver().update(NoteConstant.CONTENT_NOTE_URI, localContentValues, str,
                null);
    }

    public static int updateNoteWidgetId(Context context, long noteId, int widgetId) {
        ContentValues localContentValues = new ContentValues(2);
        localContentValues.put("widget_id", Long.valueOf(widgetId));
        localContentValues.put("widget_type", Integer.valueOf(1));
        return context.getContentResolver().update(NoteConstant.CONTENT_NOTE_URI,
                localContentValues,
                "_id=" + noteId, null);
    }

    public static void search(AsyncQueryHandler handler, int token,
            Object cookie, String[] projection, long folderId, String queryString) {
        StringBuilder argsBuilder = new StringBuilder();
        argsBuilder.append("type").append("=").append(NoteConstant.TYPE_NOTE);
        argsBuilder.append(" AND ");
        if (folderId == 0L)
            argsBuilder.append("(parent_id>=0 OR parent_id=-2)");
        else
            argsBuilder.append("parent_id").append("=").append(folderId);

        String str = queryString.replace("%", "\\%").replace("'", "\\'");
        argsBuilder.append(" AND ");
        argsBuilder.append("LIKE('%").append(str).append("%',").append("snippet")
                .append(",'\\')");
        handler.startQuery(token, cookie, NoteConstant.CONTENT_NOTE_URI,
                projection, argsBuilder.toString(), null,
                "stick_date DESC, modified_date DESC");
    }

    public static void startQueryForAll(AsyncQueryHandler handler, int token,
            Object cookie, String[] projection) {
        handler.cancelOperation(token);
        StringBuilder argsBuilder = new StringBuilder();
        argsBuilder.append("type").append("=").append(NoteConstant.TYPE_NOTE);
        argsBuilder.append(" AND ");
        argsBuilder.append("(parent_id>=0 OR parent_id=-2)");
               argsBuilder.append(" AND ");
        argsBuilder.append("LIKE('%").append("%',").append("snippet")
                .append(",'\\')");
        handler.startQuery(token, cookie, NoteConstant.CONTENT_NOTE_URI,
                projection, argsBuilder.toString(), null,"stick_date DESC, modified_date DESC");
    }
}
