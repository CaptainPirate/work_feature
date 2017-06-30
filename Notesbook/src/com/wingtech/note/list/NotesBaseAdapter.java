/*===================================================================================================*
 *  when  |      who     |    keyword           |        why         |         what                  *
 *===================================================================================================*
 *20160407|mengzhiming.wt|porting A1s enjoynotes| Port_EnjoyNotes | porting A1s enjoynotes to CMCC N2*
*====================================================================================================*
*20160531|mengzhiming.wt|   customer req       | customer req    | customer req                      *
 *===================================================================================================*/
package com.wingtech.note.list;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;

import com.wingtech.note.data.NoteConstant;
import com.wingtech.note.list.NotesBaseAdapter.AppWidgetAttribute;

import java.util.HashSet;

public abstract class NotesBaseAdapter extends CursorAdapter {

    private int mNotesCount;
    private int mFoldersCount;
    private SparseArray<Long> mSelectedIndex = new SparseArray<Long>();
    private OnContentChangedListener mOnContentChangedListener;
    private Context mContext;
    private boolean mChoiceMode;
    private String mSearchToken;

    public NotesBaseAdapter(Context context) {
        super(context, null, true);
    }

    public abstract View newView(Context context, Cursor cursor, ViewGroup parent);

    protected void onContentChanged() {
        super.onContentChanged();
        calcNotesCount(getCursor());
        updateSelectedIndex();
        if (mOnContentChangedListener != null)
            mOnContentChangedListener.onContentChanged(this);
    }

    public void setOnContentChangedListener(OnContentChangedListener listener) {
        mOnContentChangedListener = listener;
    }

    public String getSearchToken() {
        return mSearchToken;
    }

    public void setSearchToken(String token) {
        mSearchToken = token;
    }

    public void setCheckedItem(int positon, boolean mode) {
        if (mode)
            mSelectedIndex.put(positon, Long.valueOf(getItemId(positon)));
        else {
            mSelectedIndex.remove(positon);
        }
        //notifyDataSetChanged();
    }

    protected void calcNotesCount(Cursor cursor) {
        mNotesCount = 0;
        mFoldersCount = 0;
        if (cursor != null) {
            cursor.moveToPosition(-1);
            while (cursor.moveToNext())
                if (NoteItemData.getNoteType(cursor) == NoteConstant.TYPE_NOTE)
                    mNotesCount++;
                else
                    mFoldersCount++;
        } else
            Log.e("NotesBaseAdapter", "Invalid cursor");
    }

    public int getFoldersCount() {
        return mFoldersCount;
    }

    public int getNotesCount() {
        return mNotesCount;
    }

    public int getSelectedCount() {
        return mSelectedIndex.size();
    }

    // ���ѡ�е�folder ID
    public HashSet<Long> getSelectedFolderIds() {
        HashSet<Long> ids = new HashSet<Long>();
        for (int i = 0; i < mSelectedIndex.size(); i++)
            if (NoteItemData.getNoteType((Cursor) getItem(mSelectedIndex.keyAt(i))) != NoteConstant.TYPE_NOTE)
                ids.add(mSelectedIndex.valueAt(i));
        return ids;
    }

    // ���ѡ�е�Note ID
    public HashSet<Long> getSelectedNoteIds() {
        HashSet<Long> ids = new HashSet<Long>();
        for (int i = 0; i < mSelectedIndex.size(); i++)
            if (NoteItemData.getNoteType((Cursor) getItem(mSelectedIndex.keyAt(i))) == NoteConstant.TYPE_NOTE)
                ids.add(mSelectedIndex.valueAt(i));
        return ids;
    }

    public HashSet<Long> getSelectedStickyNotesIds() {
        HashSet<Long> localHashSet = new HashSet<Long>();
        if (mSelectedIndex.size() > 0) {
            for (int i = 0; i < mSelectedIndex.size(); i++) {
                Cursor localCursor = (Cursor) getItem(mSelectedIndex.keyAt(i));
                if (localCursor != null)
                    if (NoteItemData.getNoteType(localCursor) == NoteConstant.TYPE_NOTE)
                        if (NoteItemData.getStickDate(localCursor) > 0L) {
                            localHashSet.add(mSelectedIndex.valueAt(i));
                        }
            }
        }
        return localHashSet;
    }

    public HashSet<AppWidgetAttribute> getSelectedWidget() {
        HashSet<AppWidgetAttribute> widgets = new HashSet<AppWidgetAttribute>();

        if (mSelectedIndex.size() > 0) {
            for (int i = 0; i < mSelectedIndex.size(); i++) {
                Cursor cursor = (Cursor) getItem(mSelectedIndex.keyAt(i));
                if (cursor != null) {
                    if (NoteItemData.getNoteType(cursor) != 0) {

                        AppWidgetAttribute widget = new AppWidgetAttribute();
                        NoteItemData itemData = new NoteItemData(mContext, cursor);
                        widget.widgetId = itemData.getWidgetId();
                        widget.widgetType = itemData.getWidgetType();
                        widgets.add(widget);
                    }
                }
                else {
                    Log.e("NotesBaseAdapter", "Invalid cursor");
                    widgets = null;
                }
            }
        } else {
            Log.e("NotesBaseAdapter", "No selected widget");
            widgets = null;
        }
        return widgets;
    }

    public int getTotalCount() {
        return mNotesCount + mFoldersCount;
    }

    private void updateSelectedIndex() {
        SparseArray<Long> tempSelectedIndex = mSelectedIndex;
        mSelectedIndex = new SparseArray<Long>();
        Cursor cursor = getCursor();
        if ((cursor != null) && (cursor.getCount() > 0) && (tempSelectedIndex.size() > 0)) {
            cursor.moveToPosition(-1);
            while (cursor.moveToNext()) {
                long id = NoteItemData.getId(cursor);
                if (tempSelectedIndex.indexOfValue(Long.valueOf(id)) >= 0)
                    mSelectedIndex.append(cursor.getPosition(), Long.valueOf(id));
            }
        }
    }

    public void selectAll(boolean mode) {
        mSelectedIndex.clear();
        if ((mode) && (getCursor() != null) && (getCursor().getCount() > 0)) {
            getCursor().moveToPosition(-1);
            while (getCursor().moveToNext())
                mSelectedIndex
                        .put(getCursor().getPosition(),
                                Long.valueOf(NoteItemData.getId(getCursor())));
        }
        notifyDataSetChanged();
    }

    public boolean isAllSelected() {
        return getSelectedCount() == getTotalCount();
    }

    public static class AppWidgetAttribute {
        public int widgetId;
        public int widgetType;
    }

    public boolean isInChoiceMode() {
        return mChoiceMode;
    }

    public void setChoiceMode(boolean mode) {
        mSelectedIndex.clear();
        mChoiceMode = mode;
    }

    public boolean isSelectedItem(int pos) {
        if (mSelectedIndex.indexOfKey(pos) >= 0)
            return true;
        else
            return false;
    }

    public static abstract interface OnContentChangedListener {
        public abstract void onContentChanged(NotesBaseAdapter adapter);
    }
}
