/*===================================================================================================*
 *  when  |      who     |    keyword           |        why         |         what                  *
 *===================================================================================================*
 *20160407|mengzhiming.wt|porting A1s enjoynotes| Port_EnjoyNotes | porting A1s enjoynotes to CMCC N2*
*====================================================================================================*
*20160531|mengzhiming.wt|   customer req       | customer req    | customer req                      *
*===================================================================================================*/
package com.wingtech.note.data;

import android.appwidget.AppWidgetManager;
import android.content.ContentProviderOperation;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.wingtech.note.ResourceParser.NoteBgResources;
import com.wingtech.note.Utils;
import com.wingtech.note.data.NoteConstant.CallNote;
import com.wingtech.note.data.NoteConstant.DataColumns;
import com.wingtech.note.data.NoteConstant.DataConstants;
import com.wingtech.note.data.NoteConstant.NoteColumns;
import com.wingtech.note.data.NoteConstant.AttachmentColumns;
import com.wingtech.note.data.NoteConstant.TextNote;
import com.wingtech.note.spannableparser.SpanUtils;

import java.util.ArrayList;

public class WorkingNote {
    public static final String[] DATA_PROJECTION = new String[] {
            DataColumns.ID,
            DataColumns.CONTENT,
            DataColumns.MIME_TYPE,
            DataColumns.DATA1,
            DataColumns.DATA2,
            DataColumns.DATA3,
            DataColumns.DATA4,
    };

    public static final String[] NOTE_PROJECTION = new String[] {
            NoteColumns.PARENT_ID,
            NoteColumns.ALERTED_DATE,
            NoteColumns.BG_COLOR_ID,
            NoteColumns.WIDGET_ID,
            NoteColumns.WIDGET_TYPE,
            NoteColumns.MODIFIED_DATE
    };

    private static final int DATA_ID_COLUMN = 0;

    private static final int DATA_CONTENT_COLUMN = 1;

    private static final int DATA_MIME_TYPE_COLUMN = 2;

    private static final int DATA_MODE_COLUMN = 3;

    private static final int NOTE_PARENT_ID_COLUMN = 0;

    private static final int NOTE_ALERTED_DATE_COLUMN = 1;

    private static final int NOTE_BG_COLOR_ID_COLUMN = 2;

    private static final int NOTE_WIDGET_ID_COLUMN = 3;

    private static final int NOTE_WIDGET_TYPE_COLUMN = 4;

    private static final int NOTE_MODIFIED_DATE_COLUMN = 5;

    private static final String TAG = "WorkingNote";

    private Context mContext;

    private long mAlertDate;

    private long mModifiedDate;

    private long mFolderId;

    private Note mNote;
    private long mNoteId;

    private boolean mIsDeleted;
    private boolean mDiscarded;

    private int mMode;

    private int mWidgetType;

    private int mBgColorId;

    private int mWidgetId;

    private String mContent;
    private NoteSettingChangedListener mNoteSettingStatusListener;
    private boolean mReadonly;
    private BackupNote backupNote;

    // New note construct
    private WorkingNote(Context context, long folderId) {
        mContext = context;
        mAlertDate = 0;
        mModifiedDate = System.currentTimeMillis();
        mFolderId = folderId;
        mNote = new Note();
        mNoteId = 0;
        mIsDeleted = false;
        mDiscarded = false;
        mMode = 0;
        mWidgetType = NoteConstant.TYPE_WIDGET_INVALIDE;
        backupNote = new BackupNote();
    }

    // Existing note construct
    private WorkingNote(Context context, long noteId, long folderId) {
        mContext = context;
        mNoteId = noteId;
        mFolderId = folderId;
        mIsDeleted = false;
        mNote = new Note();
        loadNote();
        backupNote = new BackupNote();
    }

    public boolean getReadonly() {
        return mReadonly;
    }

    public void setReadonly(boolean paramBoolean) {
        mReadonly = paramBoolean;
    }

    private void loadNote() {
        Cursor cursor = mContext.getContentResolver().query(
                ContentUris.withAppendedId(NoteConstant.CONTENT_NOTE_URI, mNoteId),
                NOTE_PROJECTION, null,
                null, null);

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    mFolderId = cursor.getLong(NOTE_PARENT_ID_COLUMN);
                    mBgColorId = cursor.getInt(NOTE_BG_COLOR_ID_COLUMN);
                    mWidgetId = cursor.getInt(NOTE_WIDGET_ID_COLUMN);
                    mWidgetType = cursor.getInt(NOTE_WIDGET_TYPE_COLUMN);
                    mAlertDate = cursor.getLong(NOTE_ALERTED_DATE_COLUMN);
                    mModifiedDate = cursor.getLong(NOTE_MODIFIED_DATE_COLUMN);
                }
            } finally {
                cursor.close();
            }
        } else {
            Log.e(TAG, "No note with id:" + mNoteId);
            throw new IllegalArgumentException("Unable to find note with id " + mNoteId);
        }
        loadNoteData();
    }

    private void loadNoteData() {
        Cursor cursor = mContext.getContentResolver().query(NoteConstant.CONTENT_DATA_URI,
                DATA_PROJECTION,
                DataColumns.NOTE_ID + "=?", new String[] {
                    String.valueOf(mNoteId)
                }, null);

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    do {
                        String type = cursor.getString(DATA_MIME_TYPE_COLUMN);
                        if (DataConstants.NOTE.equals(type)) {
                            mContent = cursor.getString(DATA_CONTENT_COLUMN);
                            mMode = cursor.getInt(DATA_MODE_COLUMN);
                            mNote.setTextDataId(cursor.getLong(DATA_ID_COLUMN));
                        } else if (DataConstants.CALL_NOTE.equals(type)) {
                            mNote.setCallDataId(cursor.getLong(DATA_ID_COLUMN));
                        } else {
                            Log.d(TAG, "Wrong note type with type:" + type);
                        }
                    } while (cursor.moveToNext());
                }
            } finally {
                cursor.close();
            }
        } else {
            Log.e(TAG, "No data with id:" + mNoteId);
            throw new IllegalArgumentException("Unable to find note's data with id " + mNoteId);
        }
    }

    public static WorkingNote createEmptyNote(Context context, long folderId, int widgetId,
            int widgetType, int defaultBgColorId) {
        WorkingNote note = new WorkingNote(context, folderId);
        note.setBgColorId(defaultBgColorId);
        note.setWidgetId(widgetId);
        note.setWidgetType(widgetType);
        return note;
    }

    public static WorkingNote load(Context context, long id) {
        return new WorkingNote(context, id, 0);
    }

    public synchronized boolean saveNote() {
        // 此处先保存涂鸦

        if (isWorthSaving()) {
            if (!existInDatabase()) {
                if ((mNoteId = Note.getNewNoteId(mContext, mFolderId)) == 0) {
                    Log.e(TAG, "Create new note fail with id:" + mNoteId);
                    return false;
                }
            }

            mNote.syncNote(mContext, mNoteId);

            /**
             * 同时更新该note对应的widget
             */
            if (mWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID
                    && mWidgetType != NoteConstant.TYPE_WIDGET_INVALIDE
                    && mNoteSettingStatusListener != null) {
                mNoteSettingStatusListener.onWidgetChanged();
            }
            /*
             * 此处将附件信息放入数据库
             */
            updateAttachmentInfo(mContent);
            return true;
        } else {
            return false;
        }
    }

    private void updateAttachmentInfo(String content) {
        // TODO Auto-generated method stub
        if (TextUtils.isEmpty(content)) {
            return;
        }
        ArrayList<String> names = SpanUtils.collectImageNames(content);
        String[] projection = {
                AttachmentColumns.ID, AttachmentColumns.NAME
        };
        String selection = AttachmentColumns.NOTE_ID + "=? AND " + AttachmentColumns.TYPE
                + "=?";
        String[] args = {
                Long.toString(mNoteId), "image"
        };
        Cursor cursor = mContext.getContentResolver().query(NoteConstant.CONTENT_ATTACHMENT_URI,
                projection, selection, args, null);
        ArrayList<Long> toBeDelIds = new ArrayList<Long>();
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    do {
                        String name = cursor.getString(1);
                        boolean exist = false;
                        // 看数据库里的文件名现在是否还存在与
                        for (String str : names) {
                            if (name.equals(str)) {
                                exist = true;
                                break;
                            }
                        }
                        int i = 0;
                        for (; i < names.size(); i++) {
                            if (name.equals(names.get(i))) {
                                exist = true;
                                break;
                            }
                        }
                        if (exist) {
                            // 存在，则将names中对应项删掉，后续不需要再插入到数据库中
                            names.remove(i);
                        } else {
                            // 不存在,需要删除,保存ID，待后续删除
                            toBeDelIds.add(cursor.getLong(0));
                        }
                    } while (cursor.moveToNext());
                }
            } finally {
                cursor.close();
            }
        }
        selection = AttachmentColumns.ID + " in (" + Utils.congregateAsString(toBeDelIds, ",") + ")";
        if (toBeDelIds.size() > 0) {
            mContext.getContentResolver().delete(NoteConstant.CONTENT_ATTACHMENT_URI, selection,
                    null);
        }
        for (String name : names) {
            ContentValues value = new ContentValues();
            value.put(AttachmentColumns.NOTE_ID, mNoteId);
            value.put(AttachmentColumns.NAME, name);
            value.put(AttachmentColumns.TYPE, "image");
            mContext.getContentResolver().insert(NoteConstant.CONTENT_ATTACHMENT_URI, value);
        }
    }

    public boolean existInDatabase() {
        return mNoteId > 0;
    }

    public void setOnSettingStatusChangedListener(NoteSettingChangedListener l) {
        mNoteSettingStatusListener = l;
    }

    public boolean isWorthSaving() {
        if (mReadonly || mIsDeleted || mDiscarded
                || (!existInDatabase() && TextUtils.isEmpty(mContent))
                || (existInDatabase() && !mNote.isLocalModified())) {
            return false;
        } else {
            return true;
        }
    }

    public void setBgColorId(int id) {
        if (id != mBgColorId) {
            mBgColorId = id;
            if (mNoteSettingStatusListener != null) {
                mNoteSettingStatusListener.onBackgroundColorChanged();
            }
            mNote.setNoteValue(NoteColumns.BG_COLOR_ID, String.valueOf(id));
        }
    }

    public void setWidgetId(int id) {
        if (id != mWidgetId) {
            mWidgetId = id;
            mNote.setNoteValue(NoteColumns.WIDGET_ID, String.valueOf(mWidgetId));
        }
    }

    public void setWidgetType(int type) {
        if (type != mWidgetType) {
            mWidgetType = type;
            mNote.setNoteValue(NoteColumns.WIDGET_TYPE, String.valueOf(mWidgetType));
        }
    }

    public void setWorkingText(String text) {
        if (!TextUtils.equals(mContent, text)) {
            mContent = text;
            mNote.setTextData(DataColumns.CONTENT, mContent);
        }
    }

    public void convertToCallNote(String phoneNumber, long callDate) {
        mNote.setCallData(CallNote.CALL_DATE, String.valueOf(callDate));
        mNote.setCallData(CallNote.PHONE_NUMBER, phoneNumber);
        mNote.setNoteValue(NoteColumns.PARENT_ID,
                String.valueOf(NoteConstant.ID_CALL_RECORD_FOLDER));
    }

    public void setCheckListMode(int mode) {
        if (mMode != mode) {
            if (mNoteSettingStatusListener != null) {
                mNoteSettingStatusListener.onCheckListModeChanged(mMode, mode);
            }
            mMode = mode;
            mNote.setTextData(TextNote.MODE, String.valueOf(mMode));
        }
    }

    public void setAlertDate(long date, boolean set) {
        if (date != mAlertDate) {
            mAlertDate = date;
            mNote.setNoteValue(NoteColumns.ALERTED_DATE, String.valueOf(mAlertDate));
        }
        if (mNoteSettingStatusListener != null) {
            mNoteSettingStatusListener.onClockAlertChanged(date, set);
        }
        //bug127312,tangzihui.wt,2015.12.09,add,save data.
        //saveNote(); //bug185832,mengzhiming.wt,modified 20160608
    }

    public void markDeleted(boolean mark) {
        mIsDeleted = mark;
        if (mWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID
                && mWidgetType != NoteConstant.TYPE_WIDGET_INVALIDE
                && mNoteSettingStatusListener != null) {
            mNoteSettingStatusListener.onWidgetChanged();
        }
    }

    public boolean hasClockAlert() {
        return (mAlertDate > 0 ? true : false);
    }

    public String getContent() {
        return mContent;
    }

    public long getAlertDate() {
        return mAlertDate;
    }

    public long getModifiedDate() {
        return mModifiedDate;
    }

    public int getBgColorResId() {
        return NoteBgResources.getEditPanelBgResource(mBgColorId);
    }

    public int getBgColor() {
        return mContext.getResources().getColor(NoteBgResources.getEditPanelBgColor(mBgColorId));
    }

    public int getBgColorId() {
        return mBgColorId;
    }

    public int getTitleBgResId() {
        return NoteBgResources.getEditTitleBgResource(mBgColorId);
    }

    public int getCheckListMode() {
        return mMode;
    }

    public long getNoteId() {
        return mNoteId;
    }

    public long getFolderId() {
        return mFolderId;
    }

    public int getWidgetId() {
        return mWidgetId;
    }

    public int getWidgetType() {
        return mWidgetType;
    }

    public boolean hasDiscarded() {
        return mDiscarded;
    }

    public void setDiscarded(boolean b) {
        mDiscarded = b;
    }

    // 不保存修改
    public boolean discardChanges() {
        if (backupNote.nNoteId <= 0 && mNoteId > 0) {
            // 为新建的note，需删除此时的note
            markDeleted(true);
            Uri uri = ContentUris.withAppendedId(NoteConstant.CONTENT_NOTE_URI, mNoteId);
            mContext.getContentResolver().delete(uri, null, null);
            return true;
        } else {
            setBgColorId(backupNote.nBgColorId);
            if (backupNote.nAlertDate <= 0)
                setAlertDate(backupNote.nAlertDate, false);
            else
                setAlertDate(backupNote.nAlertDate, true);

            setWorkingText(backupNote.nContent);
            setWidgetId(backupNote.nWidgetId);
            setWidgetType(backupNote.nWidgetType);
            mNote.restoreNoteModification(backupNote.nModifiedDate);
            return mNote.syncNote(mContext, mNoteId);
        }
    }

    private class BackupNote {
        private long nNoteId = 0L;
        private long nAlertDate = 0L;
        private int nBgColorId = 0;
        private String nContent = "";
        private long nModifiedDate = System.currentTimeMillis();
        private int nWidgetId = 0;
        private int nWidgetType = -1;

        public BackupNote() {
            nNoteId = mNoteId;
            nBgColorId = mBgColorId;
            nWidgetId = mWidgetId;
            nWidgetType = mWidgetType;
            nAlertDate = mAlertDate;
            nModifiedDate = mModifiedDate;
            nContent = mContent;
        }

    }

    /*
     * 当前设置改变时调用的listener
     */
    public interface NoteSettingChangedListener {
        /**
         * Called when the background color of current note has just changed
         */
        void onBackgroundColorChanged();

        /**
         * Called when user set clock
         */
        void onClockAlertChanged(long date, boolean set);

        /**
         * Call when user create note from widget
         */
        void onWidgetChanged();

        /**
         * Call when switch between check list mode and normal mode
         *
         * @param oldMode is previous mode before change
         * @param newMode is new mode
         */
        void onCheckListModeChanged(int oldMode, int newMode);
    }
}
