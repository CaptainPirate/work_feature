/*===================================================================================================*	
 *  when  |      who     |    keyword           |        why         |         what                  *	
 *===================================================================================================*	
 *20160407|mengzhiming.wt|porting A1s enjoynotes| Port_EnjoyNotes | porting A1s enjoynotes to CMCC N2*	
*====================================================================================================*/

package com.wingtech.note.list;

import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;

import com.wingtech.note.data.NoteConstant;
import com.wingtech.note.R;
/*
 * 该类用于模拟生成文件夹cursor，并加入新建文件夹项
 * */
public class FolderNameCursor extends MatrixCursor {
    public static final long NEW_FOLDER_ID = -1L;
    public static final String[] PROJECTION = {
            NoteConstant.NoteColumns.ID, NoteConstant.NoteColumns.SNIPPET
    };

    public FolderNameCursor(Context context, Cursor cursor) {
        super(PROJECTION);
        String str1 = context.getString(R.string.menu_create_folder);
        String[] arrayOfString1 = new String[2];
        arrayOfString1[0] = Long.toString(NEW_FOLDER_ID);
        arrayOfString1[1] = str1;
        addRow(arrayOfString1);
        if (cursor != null) {
            try {
                cursor.moveToPosition(-1);
                while (cursor.moveToNext()) {
                    long id = cursor.getLong(0);
                    String name;
                    if (id == NoteConstant.ID_ROOT_FOLDER) {
                        name = context.getString(R.string.menu_move_folder_root);
                    }
                    else
                        name = cursor.getString(1);
                    String[] row = {
                            Long.toString(id), name
                    };
                    addRow(row);
                }
            } finally {
                cursor.close();
            }
        }
    }

    public long getFolderId(int position) {
        moveToPosition(position);
        return getLong(0);
    }

    public String getFolderName(int position) {
        moveToPosition(position);
        return getString(1);
    }

}

