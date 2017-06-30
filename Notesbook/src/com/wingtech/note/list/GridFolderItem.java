/*===================================================================================================*	
 *  when  |      who     |    keyword           |        why         |         what                  *	
 *===================================================================================================*	
 *20160407|mengzhiming.wt|porting A1s enjoynotes| Port_EnjoyNotes | porting A1s enjoynotes to CMCC N2*	
*====================================================================================================*/

package com.wingtech.note.list;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

import com.wingtech.note.ResourceParser;
import com.wingtech.note.data.NoteConstant;
import com.wingtech.note.R;

public class GridFolderItem extends GridBaseItem {
    private TextView mNoteTitle;

    public GridFolderItem(Context context)
    {
        super(context);
    }

    public GridFolderItem(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    public GridFolderItem(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected int getContentMaxLines(boolean hadImage) {
        if (hadImage)
            return mRes.getInteger(R.integer.grid_folder_image_text_max_lines);
        else
            return mRes.getInteger(R.integer.grid_folder_text_max_lines);
    }

    @Override
    protected void setBackground(NoteItemData noteItemData) {
        // TODO Auto-generated method stub
        setBackgroundResource(ResourceParser.GridFolderBgResources.getNoteBgRes(noteItemData
                .getBgColorId()));
    }

    @Override
    protected void onFinishInflate() {
        // TODO Auto-generated method stub
        super.onFinishInflate();
        mNoteTitle = ((TextView) findViewById(R.id.noteitem_folder_title));
    }

    public void bind(Context context, NoteItemData noteItemData, String str, boolean visibility,
            boolean checked) {
        super.bind(context, noteItemData, str, visibility, checked);
        if (noteItemData.getId() == NoteConstant.ID_CALL_RECORD_FOLDER) {
            mNoteTitle.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_grid_call, 0, 0);
            StringBuilder builder = new StringBuilder().append(context
                    .getString(R.string.call_record_folder_name));
            Object[] args = new Object[1];
            args[0] = Integer.valueOf(noteItemData.getNotesCount());
            mNoteTitle.setText(context
                    .getString(R.string.format_filesinfolder_count, args));
        } else {
            mNoteTitle.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_grid_folder, 0, 0);
            StringBuilder builder = new StringBuilder().append(noteItemData.getSnippet());
            Object[] args = new Object[1];
            args[0] = Integer.valueOf(noteItemData.getNotesCount());
            mNoteTitle.setText(builder.append(context.getString(R.string.format_filesinfolder_count,
                    args)));
        }
    }

}
