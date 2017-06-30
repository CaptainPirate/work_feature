/*===================================================================================================*	
 *  when  |      who     |    keyword           |        why         |         what                  *	
 *===================================================================================================*	
 *20160407|mengzhiming.wt|porting A1s enjoynotes| Port_EnjoyNotes | porting A1s enjoynotes to CMCC N2*	
*====================================================================================================*/

package com.wingtech.note.list;

import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;

import com.wingtech.note.data.Contact;
import com.wingtech.note.data.DataUtils;
import com.wingtech.note.data.NoteConstant;

public class NoteItemData {
    private static final int COLUMN_ID = 0;
    private static final int COLUMN_ALERTED_DATE = 1;
    private static final int COLUMN_BG_COLOR_ID = 2;
    private static final int COLUMN_CREATED_DATE = 3;
    private static final int COLUMN_HAS_ATTACHMENT = 4;
    private static final int COLUMN_MODIFIED_DATE = 5;
    private static final int COLUMN_NOTES_COUNT = 6;
    private static final int COLUMN_PARENT_ID = 7;
    private static final int COLUMN_SNIPPET = 8;
    private static final int COLUMN_TYPE = 9;
    private static final int COLUMN_WIDGET_ID = 10;
    private static final int COLUMN_WIDGET_TYPE = 11;
    private static final int COLUMN_STICK_DATE = 12;
    public static final String[] PROJECTION = {
            "_id",
            "alert_date",
            "bg_color_id",
            "created_date",
            "has_attachment",
            "modified_date",
            "notes_count",
            "parent_id",
            "snippet",
            "type",
            "widget_id",
            "widget_type",
            "stick_date",
    };
    // private static final int COLUMN_SUBJECT = 12;
    private long mId;
    private long mAlertDate;
    private int mBgColorId;
    private long mCreatedDate;
    private boolean mHasAttachment;
    private long mModifiedDate;
    private int mNotesCount;
    private long mParentId;
    private String mSnippet;
    private int mType;
    private int mWidgetId;
    private String mSubject;
    private int mWidgetType;
    private long mStickDate;
    private String mPhoneNumber;
    private String mName;

    public NoteItemData(Context context, Cursor cursor) {
        mId = cursor.getLong(COLUMN_ID);
        mAlertDate = cursor.getLong(COLUMN_ALERTED_DATE);
        mBgColorId = cursor.getInt(COLUMN_BG_COLOR_ID);
        mCreatedDate = cursor.getLong(COLUMN_CREATED_DATE);
        mHasAttachment = (cursor.getInt(COLUMN_HAS_ATTACHMENT) > 0) ? true : false;
        mModifiedDate = cursor.getLong(COLUMN_MODIFIED_DATE);
        mNotesCount = cursor.getInt(COLUMN_NOTES_COUNT);
        mParentId = cursor.getLong(COLUMN_PARENT_ID);
        mSnippet = cursor.getString(COLUMN_SNIPPET);
        mType = cursor.getInt(COLUMN_TYPE);
        mWidgetId = cursor.getInt(COLUMN_WIDGET_ID);
        mWidgetType = cursor.getInt(COLUMN_WIDGET_TYPE);
        mStickDate = cursor.getLong(COLUMN_STICK_DATE);
        mPhoneNumber = "";
        if (mParentId == NoteConstant.ID_CALL_RECORD_FOLDER) {
            mPhoneNumber = DataUtils.getCallNumberByNoteId(context.getContentResolver(), mId);
            if (!TextUtils.isEmpty(mPhoneNumber))
            {
                mName = Contact.getContact(context, mPhoneNumber);
                if (mName == null)
                    mName = mPhoneNumber;
            }
        }
        if (mName == null)
            mName = "";
        // checkPostion(cursor);
    }

    // private void checkPostion(Cursor cursor) {
    // mIsLastItem = cursor.isLast() ? true : false;
    // mIsFirstItem = cursor.isFirst() ? true : false;
    // mIsOnlyOneItem = (cursor.getCount() == 1);
    // mIsMultiNotesFollowingFolder = false;
    // mIsOneNoteFollowingFolder = false;
    //
    // if (mType == Notes.TYPE_NOTE && !mIsFirstItem) {
    // int position = cursor.getPosition();
    // if (cursor.moveToPrevious()) {
    // if (cursor.getInt(COLUMN_TYPE) == Notes.TYPE_FOLDER
    // || cursor.getInt(COLUMN_TYPE) == Notes.TYPE_SYSTEM) {
    // if (cursor.getCount() > (position + 1)) {
    // mIsMultiNotesFollowingFolder = true;
    // } else {
    // mIsOneNoteFollowingFolder = true;
    // }
    // }
    // if (!cursor.moveToNext()) {
    // throw new
    // IllegalStateException("cursor move to previous but can't move back");
    // }
    // }
    // }
    // }

    public static long getId(Cursor paramCursor) {
        return paramCursor.getLong(COLUMN_ID);
    }

    public static int getNoteType(Cursor paramCursor) {
        return paramCursor.getInt(COLUMN_TYPE);
    }

    public static long getParentId(Cursor paramCursor) {
        return paramCursor.getLong(COLUMN_PARENT_ID);
    }

    public static long getStickDate(Cursor paramCursor) {
        return paramCursor.getLong(COLUMN_STICK_DATE);
    }

    public long getAlertDate() {
        return mAlertDate;
    }

    public int getBgColorId() {
        return mBgColorId;
    }

    public String getCallName() {
        return mName;
    }

    public long getCreatedDate() {
        return mCreatedDate;
    }

    public long getFolderId() {
        return mParentId;
    }

    public long getId() {
        return mId;
    }

    public long getModifiedDate() {
        return mModifiedDate;
    }

    public int getNotesCount() {
        return mNotesCount;
    }

    public long getParentId() {
        return mParentId;
    }

    public String getSnippet() {
        return mSnippet;
    }

    public long getStickDate() {
        return mStickDate;
    }

    public int getType() {
        return mType;
    }

    public int getWidgetId() {
        return mWidgetId;
    }

    public int getWidgetType() {
        return mWidgetType;
    }

    public boolean hasAlert() {
        if (mAlertDate > 0)
            return true;
        else
            return false;
    }

    public boolean hasAttachment() {
        return mHasAttachment;
    }

    public boolean isCallRecord() {
        if ((mParentId == NoteConstant.ID_CALL_RECORD_FOLDER) && (!TextUtils.isEmpty(mPhoneNumber)))
            return true;
        else
            return false;
    }

}
