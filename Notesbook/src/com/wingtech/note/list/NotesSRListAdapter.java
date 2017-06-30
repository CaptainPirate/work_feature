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

import com.wingtech.note.R;

public class NotesSRListAdapter extends NotesBaseAdapter {
    private LayoutInflater mInflater;

    public NotesSRListAdapter(Context context) {
        super(context);
        mInflater = LayoutInflater.from(context);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return mInflater.inflate(R.layout.note_result_list_item, parent, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        if ((view instanceof NotesSRListItem)) {
            NoteItemData localNoteItemData = new NoteItemData(context, cursor);
            ((NotesSRListItem) view).bind(context, localNoteItemData, getSearchToken(),
                    isInChoiceMode(), isSelectedItem(cursor.getPosition()));
        }
    }
}
