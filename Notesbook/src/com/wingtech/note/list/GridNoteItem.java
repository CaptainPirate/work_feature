/*===================================================================================================*	
 *  when  |      who     |    keyword           |        why         |         what                  *	
 *===================================================================================================*	
 *20160407|mengzhiming.wt|porting A1s enjoynotes| Port_EnjoyNotes | porting A1s enjoynotes to CMCC N2*	
*====================================================================================================*/

package com.wingtech.note.list;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.wingtech.note.ResourceParser;
import com.wingtech.note.Utils;
import com.wingtech.note.loader.ImageCacheManager;
import com.wingtech.note.loader.ListImageJob;
import com.wingtech.note.spannableparser.SpanUtils;
import com.wingtech.note.R;

public class GridNoteItem extends GridBaseItem {
    protected TextView mNoteContent;
    protected ImageView mNoteImage;
    private View mStick;

    public GridNoteItem(Context context) {
        this(context, null);
    }

    public GridNoteItem(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GridNoteItem(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected int getContentMaxLines(boolean hadImage) {
        if (hadImage) {
            return mRes.getInteger(R.integer.grid_note_image_text_max_lines);
        }
        else {
            return mRes.getInteger(R.integer.grid_note_text_max_lines);
        }
    }

    @Override
    protected void setBackground(NoteItemData noteItemData) {
        // TODO Auto-generated method stub
        setBackgroundResource(ResourceParser.GridNoteBgResources.getNoteBgRes(noteItemData
                .getBgColorId()));
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        mNoteImage = ((ImageView) findViewById(R.id.note_image));
        mNoteContent = ((TextView) findViewById(R.id.note_content));
        mStick = findViewById(R.id.stick);
    }

    public void onRecycle() {
        ImageCacheManager.getInstance(getContext()).cancel(mNoteImage);
    }

    @Override
    public void bind(Context context, NoteItemData noteItemData, String string, boolean visibility,
            boolean checked) {
        // TODO Auto-generated method stub
        super.bind(context, noteItemData, string, visibility, checked);
        if (noteItemData.hasAlert()) {
            mAlert.setImageResource(R.drawable.ic_grid_alert);
            mAlert.setVisibility(VISIBLE);
        } else {
            mAlert.setVisibility(GONE);
        }
        if (noteItemData.getStickDate() <= 0L)
            mStick.setVisibility(GONE);
        else
            mStick.setVisibility(VISIBLE);
        mTime.setText(Utils.formatTime(noteItemData.getModifiedDate(),context));
        String content = noteItemData.getSnippet();
        SpanUtils.NoteImgInfo info = new SpanUtils.NoteImgInfo();
        CharSequence showText = SpanUtils.parseStrikethroughSpan(Utils
                .trimEmptyLineSequence(SpanUtils
                        .normalizeSnippet(content, info)));

        if (info.firstImgName != null) {
            mNoteImage.setVisibility(VISIBLE);
            mNoteContent.setMaxLines(getContentMaxLines(true));
            mNoteContent.setText(showText);
            ListImageJob imgJob = new ListImageJob(getContext(), mNoteImage,
                    info.firstImgName);
            ImageCacheManager.getInstance(getContext()).load(imgJob);
        } else {
            mNoteImage.setVisibility(GONE);
            mNoteContent.setMaxLines(getContentMaxLines(false));
            mNoteContent.setText(showText);
        }
    }

}
