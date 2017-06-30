/*===================================================================================================*	
 *  when  |      who     |    keyword           |        why         |         what                  *	
 *===================================================================================================*	
 *20160407|mengzhiming.wt|porting A1s enjoynotes| Port_EnjoyNotes | porting A1s enjoynotes to CMCC N2*	
*====================================================================================================*/

package com.wingtech.note.list;

import android.view.ActionMode;
import android.view.View;
import android.widget.AbsListView.MultiChoiceModeListener;

import android.app.Fragment;
import android.database.Cursor;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import com.wingtech.note.data.NoteConstant;

public abstract class NotesBaseFragment extends Fragment
        implements AdapterView.OnItemLongClickListener {
    protected long mFolderId = 0L;
    public OnItemClickListener mOnItemClickListener;
    public MultiChoiceModeListener mModeCallBack;
    protected boolean mIsInEditMode;

    public abstract NotesBaseAdapter getAdapter();

    public abstract void onDataSetChanged(Cursor paramCursor);

    public abstract ActionMode startActionMode(ActionMode.Callback paramCallback);

    public abstract int translatePosition(int paramInt);

    public NotesBaseFragment() {
        super();
    }

    public void setup(AdapterView.OnItemClickListener listener,
            AbsListView.MultiChoiceModeListener selectListener) {
        mOnItemClickListener = listener;
        mModeCallBack = selectListener;
    }

    public void enterEditMode() {
        if (!mIsInEditMode)
            mIsInEditMode = true;
    }

    public void exitEditMode() {
        if (mIsInEditMode)
            mIsInEditMode = false;
    }
//�����ѡģʽ
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position,
            long id) {
        if (mIsInEditMode)
            return false;
        else {
            startActionMode(mModeCallBack);
            mModeCallBack.onItemCheckedStateChanged(null, position, id,
                    true);
            return true;
        }
    }

    public void restoreRootPosition() {
    }

    public void enterSubFolderMode(long foderId) {
        mFolderId = foderId;
    }

    public void exitSubFolderMode() {
        mFolderId =NoteConstant.ID_ROOT_FOLDER;
    }
}
