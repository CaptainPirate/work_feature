/*===================================================================================================*	
 *  when  |      who     |    keyword           |        why         |         what                  *	
 *===================================================================================================*	
 *20160407|mengzhiming.wt|porting A1s enjoynotes| Port_EnjoyNotes | porting A1s enjoynotes to CMCC N2*	
*====================================================================================================*/

package com.wingtech.note.list;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.wingtech.note.data.NoteConstant;
import com.wingtech.note.R;

public class NotesGridAdapter extends NotesBaseAdapter {

    private LayoutInflater mInflater;

    public NotesGridAdapter(Context context) {
        super(context);
        mInflater = LayoutInflater.from(context);
    }


    private final int getItemViewType(Cursor cursor) {
        return NoteItemData.getNoteType(cursor);
    }

    public int getItemViewType(int position) {
        return getItemViewType((Cursor) getItem(position));
    }

    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        if ((view instanceof INotesListItem)) {
            NoteItemData localNoteItemData = new NoteItemData(context, cursor);
            ((INotesListItem) view).bind(context, localNoteItemData, getSearchToken(),
                    isInChoiceMode(), isSelectedItem( cursor.getPosition()));
        }
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View localView;
        if (getItemViewType(cursor) == NoteConstant.TYPE_NOTE) {
            localView = mInflater.inflate(R.layout.grid_item_note, parent, false);
        } else {
            localView = mInflater.inflate(R.layout.grid_item_folder, parent, false);
        }
        return localView;
    }

}
