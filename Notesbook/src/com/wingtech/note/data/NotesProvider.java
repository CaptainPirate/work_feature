/*===================================================================================================*	
 *  when  |      who     |    keyword           |        why         |         what                  *	
 *===================================================================================================*	
 *20160407|mengzhiming.wt|porting A1s enjoynotes| Port_EnjoyNotes | porting A1s enjoynotes to CMCC N2*	
*====================================================================================================*/

package com.wingtech.note.data;

import java.io.File;
import java.io.FileNotFoundException;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;
import android.util.Log;

import com.wingtech.note.AttachmentUtils;
import com.wingtech.note.data.NoteConstant.DataColumns;
import com.wingtech.note.data.NoteConstant.NoteColumns;
import com.wingtech.note.data.NoteConstant.AttachmentColumns;
import com.wingtech.note.data.NotesDatabaseHelper.TABLE;
import com.wingtech.note.R;

public class NotesProvider extends ContentProvider {
    private static final String TAG = "NotesProvider";
    private static final UriMatcher mMatcher;

    private NotesDatabaseHelper mHelper;

    private static final int URI_NOTE = 1;
    private static final int URI_NOTE_ITEM = 2;
    private static final int URI_DATA = 3;
    private static final int URI_DATA_ITEM = 4;

    private static final int URI_SEARCH = 5;
    private static final int URI_SEARCH_SUGGEST = 6;
    private static final int URI_SCRAP = 7;
    private static final int URI_IMAGE = 8;
    private static final int URI_ACCOUNT = 9;
    private static final int URI_ACCOUNT_ITEM = 10;
    private static final int URI_DATA_MISSED = 11;
    private static final int URI_NOTE_ATOMIC = 12;
    private static final int URI_NOTE_ATOMIC_ITEM = 13;
    private static final int URI_ATTACHMENT = 14;
    private static final int URI_ATTACHMENT_ITEM = 15;
    private static final String IMAGE_TYPE = "image";
    private static final String TMP_TYPE = "tmp";
    private static final String SKETCH_TYPE = "sketch";

    static {
        mMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        mMatcher.addURI(NoteConstant.AUTHORITY, "note", URI_NOTE);
        mMatcher.addURI(NoteConstant.AUTHORITY, "note/#", URI_NOTE_ITEM);
        mMatcher.addURI(NoteConstant.AUTHORITY, "data", URI_DATA);
        mMatcher.addURI(NoteConstant.AUTHORITY, "data/#", URI_DATA_ITEM);
        mMatcher.addURI(NoteConstant.AUTHORITY, "search", URI_SEARCH);
        mMatcher.addURI(NoteConstant.AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY, URI_SEARCH_SUGGEST);
        mMatcher.addURI(NoteConstant.AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY + "/*",
                URI_SEARCH_SUGGEST);
        mMatcher.addURI(NoteConstant.AUTHORITY, "scrap", URI_SCRAP);
        // mMatcher.addURI(Notes.AUTHORITY, "data/image/", URI_IMAGE);
        mMatcher.addURI(NoteConstant.AUTHORITY, "account", URI_ACCOUNT);
        mMatcher.addURI(NoteConstant.AUTHORITY, "account/#", URI_ACCOUNT_ITEM);
        mMatcher.addURI(NoteConstant.AUTHORITY, "data/missed", URI_DATA_MISSED);
        mMatcher.addURI(NoteConstant.AUTHORITY, "note/atomic", URI_NOTE_ATOMIC);
        mMatcher.addURI(NoteConstant.AUTHORITY, "note/atomic/#", URI_NOTE_ATOMIC_ITEM);
        mMatcher.addURI(NoteConstant.AUTHORITY, "attachment", URI_ATTACHMENT);
        mMatcher.addURI(NoteConstant.AUTHORITY, "attachment/#", URI_ATTACHMENT_ITEM);
    }

    /**
     * x'0A' represents the '\n' character in sqlite. For title and content in
     * the search result, we will trim '\n' and white space in order to show
     * more information.
     */
    private static final String NOTES_SEARCH_PROJECTION = NoteColumns.ID + ","
            + NoteColumns.ID + " AS " + SearchManager.SUGGEST_COLUMN_INTENT_EXTRA_DATA + ","
            + "TRIM(REPLACE(" + NoteColumns.SNIPPET + ", x'0A','')) AS "
            + SearchManager.SUGGEST_COLUMN_TEXT_1 + ","
            + "TRIM(REPLACE(" + NoteColumns.SNIPPET + ", x'0A','')) AS "
            + SearchManager.SUGGEST_COLUMN_TEXT_2 + ","
            + R.drawable.search_result + " AS " + SearchManager.SUGGEST_COLUMN_ICON_1 + ","
            + "'" + Intent.ACTION_VIEW + "' AS " + SearchManager.SUGGEST_COLUMN_INTENT_ACTION + ","
            + "'" + NoteConstant.TextNote.CONTENT_TYPE + "' AS "
            + SearchManager.SUGGEST_COLUMN_INTENT_DATA;

    private static String NOTES_SNIPPET_SEARCH_QUERY = "SELECT " + NOTES_SEARCH_PROJECTION
            + " FROM " + TABLE.NOTE
            + " WHERE " + NoteColumns.SNIPPET + " LIKE ?"
            + " AND " + NoteColumns.PARENT_ID + "<>" + NoteConstant.ID_TRASH_FOLER
            + " AND " + NoteColumns.TYPE + "=" + NoteConstant.TYPE_NOTE;

    private static final String COLUMN_MEDIA_NAME = "xmn_media_name";
    private static final String COLUMN_MEDIA_PATH = "xmn_media_path";
    public static final Uri SCRAP_CONTENT_URI = Uri.parse("content://notes/scrap");

    @Override
    public boolean onCreate() {
        mHelper = NotesDatabaseHelper.getInstance(getContext());
        return true;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        // TODO Auto-generated method stub
        String type = uri.getPathSegments().get(1);
        if (type.equals(IMAGE_TYPE)) {
            String name = uri.getPathSegments().get(2);
            String path = AttachmentUtils.getAttachmentPath(getContext(), name);
            File file = new File(path);
            ParcelFileDescriptor localParcelFileDescriptor;
            if ((file.exists()) && (file.isFile())) {
                localParcelFileDescriptor = ParcelFileDescriptor.open(file,
                        ParcelFileDescriptor.MODE_READ_ONLY);
                return localParcelFileDescriptor;
            } else
                return super.openFile(uri, mode);
        } else if (type.equals(TMP_TYPE)) {
            String tmpFile = AttachmentUtils.getTmpFile(getContext());
            File file = new File(tmpFile);
            ParcelFileDescriptor localParcelFileDescriptor = ParcelFileDescriptor.open(file,
                    ParcelFileDescriptor.MODE_WRITE_ONLY | ParcelFileDescriptor.MODE_CREATE
                            | ParcelFileDescriptor.MODE_TRUNCATE);
            return localParcelFileDescriptor;

        } else if (type.equals(SKETCH_TYPE)) {
            String name = uri.getPathSegments().get(2);
            String path = AttachmentUtils.getAttachmentPath(getContext(), name);
            File file = new File(path);
            ParcelFileDescriptor localParcelFileDescriptor = ParcelFileDescriptor.open(file,
                    ParcelFileDescriptor.MODE_WORLD_WRITEABLE
                            | ParcelFileDescriptor.MODE_WORLD_READABLE);
            return localParcelFileDescriptor;
        }
        else
            return super.openFile(uri, mode);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        Cursor c = null;
        SQLiteDatabase db = mHelper.getReadableDatabase();
        String id = null;
        switch (mMatcher.match(uri)) {
            case URI_NOTE:
                c = db.query(TABLE.NOTE, projection, selection, selectionArgs, null, null,
                        sortOrder);
                break;
            case URI_NOTE_ITEM:
                id = uri.getPathSegments().get(1);
                c = db.query(TABLE.NOTE, projection, NoteColumns.ID + "=" + id
                        + parseSelection(selection), selectionArgs, null, null, sortOrder);
                break;
            case URI_DATA:
                c = db.query(TABLE.DATA, projection, selection, selectionArgs, null, null,
                        sortOrder);
                break;
            case URI_DATA_ITEM:
                id = uri.getPathSegments().get(1);
                c = db.query(TABLE.DATA, projection, DataColumns.ID + "=" + id
                        + parseSelection(selection), selectionArgs, null, null, sortOrder);
                break;
            case URI_ATTACHMENT:
                c = db.query(TABLE.ATTACHMENT, projection, selection, selectionArgs, null, null,
                        sortOrder);
                break;
            case URI_ATTACHMENT_ITEM:
                id = uri.getPathSegments().get(1);
                c = db.query(TABLE.ATTACHMENT, projection, AttachmentColumns.ID + "=" + id
                        + parseSelection(selection), selectionArgs, null, null, sortOrder);
                break;
            case URI_SEARCH:
            case URI_SEARCH_SUGGEST:
                if (sortOrder != null || projection != null) {
                    throw new IllegalArgumentException(
                            "do not specify sortOrder, selection, selectionArgs, or projection"
                                    + "with this query");
                }

                String searchString = null;
                if (mMatcher.match(uri) == URI_SEARCH_SUGGEST) {
                    if (uri.getPathSegments().size() > 1) {
                        searchString = uri.getPathSegments().get(1);
                    }
                } else {
                    searchString = uri.getQueryParameter("pattern");
                }

                if (TextUtils.isEmpty(searchString)) {
                    return null;
                }

                try {
                    searchString = String.format("%%%s%%", searchString);// 输出格式如%searchString%
                    c = db.rawQuery(NOTES_SNIPPET_SEARCH_QUERY,
                            new String[] {
                                searchString
                            });
                } catch (IllegalStateException ex) {
                    Log.e(TAG, "got exception: " + ex.toString());
                }
                break;
            case URI_IMAGE:
                c = getFakeCursorForImage(db, uri, projection, selection,
                        selectionArgs, sortOrder);

                break;
            default:
                String type = uri.getPathSegments().get(1);
                if (type.equals(IMAGE_TYPE)) {
                    c = getFakeCursorForImage(db, uri, projection, selection, selectionArgs,
                            sortOrder);
                }
                else
                    throw new IllegalArgumentException("Unknown URI " + uri);
        }
        if (c != null) {
            c.setNotificationUri(getContext().getContentResolver(), uri);
        }
        return c;
    }

    private Cursor getFakeCursorForImage(SQLiteDatabase db, Uri uri, String[] projection,
            String selection, String[] selectionArgs, String sortOrder) {

        String name = uri.getPathSegments().get(2);
        return buildFakeCursorForImage(name);
    }

    private Cursor buildFakeCursorForImage(String name) {
        // TODO Auto-generated method stub
        String[] arrayOfString = {
                "_size",
                "_data",
                "mime_type",
                "width",
                "height",
        };
        String path = AttachmentUtils.getAttachmentPath(getContext(), name);
        String[] attr = mesureImageSize(path);
        if (!isImageType(attr[2])) {
            Log.e("NotesProvider", "Unsupport MimeType: " + attr[2]);
            return null;
        }
        MatrixCursor localMatrixCursor = new MatrixCursor(arrayOfString, 1);
        MatrixCursor.RowBuilder localRowBuilder = localMatrixCursor.newRow();
        for (String str : arrayOfString) {
            if ("_size".equals(str)) {
                localRowBuilder.add(Long.valueOf(new File(path).length()));
            } else if ("_data".equals(str)) {
                localRowBuilder.add(path);
            } else if ("mime_type".equals(str)) {
                localRowBuilder.add(attr[2]);
            } else if ("width".equals(str)) {
                localRowBuilder.add(attr[0]);
            } else if ("height".equals(str)) {
                localRowBuilder.add(attr[1]);
            } else {
                localRowBuilder.add(null);
            }

        }
        return localMatrixCursor;
    }

    private String[] mesureImageSize(String path) {
        String[] attribute = new String[3];
        BitmapFactory.Options localOptions = new BitmapFactory.Options();
        localOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, localOptions);
        attribute[0] = Integer.toString(localOptions.outWidth);
        attribute[1] = Integer.toString(localOptions.outHeight);
        attribute[2] = localOptions.outMimeType;
        return attribute;
    }

    private boolean isImageType(String type) {
        if ((type != null) && (type.startsWith("image/")))
            return true;
        return false;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        long dataId = 0, noteId = 0, insertedId = 0, attachmentId = 0;
        switch (mMatcher.match(uri)) {
            case URI_NOTE:
                insertedId = noteId = db.insert(TABLE.NOTE, null, values);
                break;
            case URI_DATA:
                if (values.containsKey(DataColumns.NOTE_ID)) {
                    noteId = values.getAsLong(DataColumns.NOTE_ID);
                } else {
                    Log.d(TAG, "Wrong data format without note id:" + values.toString());
                }
                insertedId = dataId = db.insert(TABLE.DATA, null, values);
                break;
            case URI_ATTACHMENT:
                if (values.containsKey(AttachmentColumns.NOTE_ID)) {
                    noteId = values.getAsLong(AttachmentColumns.NOTE_ID);
                } else {
                    Log.d(TAG, "Wrong sketch format without note id:" + values.toString());
                }
                insertedId = attachmentId = db.insert(TABLE.ATTACHMENT, null, values);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        if (noteId > 0) {
            getContext().getContentResolver().notifyChange(
                    ContentUris.withAppendedId(NoteConstant.CONTENT_NOTE_URI, noteId), null);
        }

        if (dataId > 0) {
            getContext().getContentResolver().notifyChange(
                    ContentUris.withAppendedId(NoteConstant.CONTENT_DATA_URI, dataId), null);
        }
        if (attachmentId > 0) {
            getContext().getContentResolver().notifyChange(
                    ContentUris.withAppendedId(NoteConstant.CONTENT_ATTACHMENT_URI, attachmentId), null);
        }
        return ContentUris.withAppendedId(uri, insertedId);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int count = 0;
        String id = null;
        SQLiteDatabase db = mHelper.getWritableDatabase();
        boolean deleteData = false;
        switch (mMatcher.match(uri)) {
            case URI_NOTE:
                selection = "(" + selection + ") AND " + NoteColumns.ID + ">0 ";
                count = db.delete(TABLE.NOTE, selection, selectionArgs);
                break;
            case URI_NOTE_ITEM:
                id = uri.getPathSegments().get(1);

                long noteId = Long.valueOf(id);
                if (noteId <= 0) { // 不能删除小于0的ID，即系统文件夹
                    break;
                }
                count = db.delete(TABLE.NOTE,
                        NoteColumns.ID + "=" + id + parseSelection(selection), selectionArgs);
                break;
            case URI_DATA:
                count = db.delete(TABLE.DATA, selection, selectionArgs);
                deleteData = true;
                break;
            case URI_DATA_ITEM:
                id = uri.getPathSegments().get(1);
                count = db.delete(TABLE.DATA,
                        DataColumns.ID + "=" + id + parseSelection(selection), selectionArgs);
                deleteData = true;
                break;
            case URI_ATTACHMENT:
                count = db.delete(TABLE.ATTACHMENT, selection, selectionArgs);
                deleteData = true;
                break;
            case URI_ATTACHMENT_ITEM:
                id = uri.getPathSegments().get(1);
                count = db.delete(TABLE.ATTACHMENT,
                        AttachmentColumns.ID + "=" + id + parseSelection(selection), selectionArgs);
                deleteData = true;
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        if (count > 0) {
            if (deleteData) {
                getContext().getContentResolver().notifyChange(NoteConstant.CONTENT_NOTE_URI, null);
            }
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int count = 0;
        String id = null;
        SQLiteDatabase db = mHelper.getWritableDatabase();
        boolean updateData = false;
        switch (mMatcher.match(uri)) {
            case URI_NOTE:
                // increaseNoteVersion(-1, selection, selectionArgs);
                count = db.update(TABLE.NOTE, values, selection, selectionArgs);
                break;
            case URI_NOTE_ITEM:
                id = uri.getPathSegments().get(1);
                // increaseNoteVersion(Long.valueOf(id), selection,
                // selectionArgs);
                count = db.update(TABLE.NOTE, values, NoteColumns.ID + "=" + id
                        + parseSelection(selection), selectionArgs);
                break;
            case URI_DATA:
                count = db.update(TABLE.DATA, values, selection, selectionArgs);
                updateData = true;
                break;
            case URI_DATA_ITEM:
                id = uri.getPathSegments().get(1);
                count = db.update(TABLE.DATA, values, DataColumns.ID + "=" + id
                        + parseSelection(selection), selectionArgs);
                updateData = true;
                break;
            case URI_ATTACHMENT:
                count = db.update(TABLE.ATTACHMENT, values, selection, selectionArgs);
                updateData = true;
                break;
            case URI_ATTACHMENT_ITEM:
                id = uri.getPathSegments().get(1);
                count = db.update(TABLE.ATTACHMENT, values,
                        AttachmentColumns.ID + "=" + id + parseSelection(selection), selectionArgs);
                updateData = true;
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        if (count > 0) {
            if (updateData) {
                getContext().getContentResolver().notifyChange(NoteConstant.CONTENT_NOTE_URI, null);
            }
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
    }

    private String parseSelection(String selection) {
        return (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : "");
    }

    public static Uri getImageFileUri(String name) {
        return Uri.parse("content://" + NoteConstant.AUTHORITY + "/data/" + IMAGE_TYPE + "/"
                + name);
    }

    public static Uri getTempFileUri() {
        return Uri.parse("content://" + NoteConstant.AUTHORITY + "/data/" + TMP_TYPE);
    }

    public static String getScrapPath(Context context) {
        return context.getCacheDir().getAbsolutePath() + "/.temp.jpg";
    }

    public static Uri getSketchFileUri(String name) {
        return Uri.parse("content://" + NoteConstant.AUTHORITY + "/data/" + SKETCH_TYPE + "/" + name);
    }

    @Override
    public String getType(Uri uri) {
        // TODO Auto-generated method stub
        return null;
    }

}

