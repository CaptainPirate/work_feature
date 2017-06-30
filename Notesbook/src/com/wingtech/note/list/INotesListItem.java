/*===================================================================================================*	
 *  when  |      who     |    keyword           |        why         |         what                  *	
 *===================================================================================================*	
 *20160407|mengzhiming.wt|porting A1s enjoynotes| Port_EnjoyNotes | porting A1s enjoynotes to CMCC N2*	
*====================================================================================================*/

package com.wingtech.note.list;

import android.content.Context;

public abstract interface INotesListItem {
    public abstract void bind(Context context, NoteItemData noteItemData,
            String string, boolean mode, boolean checked);

    public abstract NoteItemData getItemData();

    public abstract void onRecycle();
}
