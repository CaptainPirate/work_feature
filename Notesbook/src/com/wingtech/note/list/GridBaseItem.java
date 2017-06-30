/*===================================================================================================*
 *  when  |      who     |    keyword           |        why         |         what                  *
 *===================================================================================================*
 *20160407|mengzhiming.wt|porting A1s enjoynotes| Port_EnjoyNotes | porting A1s enjoynotes to CMCC N2*
*====================================================================================================*
*20160531|mengzhiming.wt|   customer req       | customer req    | customer req                      *
 *===================================================================================================*/
package com.wingtech.note.list;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
//import android.view.WindowManagerGlobal;

import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.wingtech.note.R;

import java.nio.CharBuffer;

public abstract class GridBaseItem extends RelativeLayout
        implements INotesListItem {
    protected ImageView mAlert;
    protected ImageView mCheckBox;
    protected ImageView mImage;
    protected NoteItemData mItemData;
    protected Resources mRes;
    protected TextView mTime;

    public GridBaseItem(Context context) {
        this(context, null);
    }

    public GridBaseItem(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GridBaseItem(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mRes = context.getResources();
    }

    public void bind(Context context, NoteItemData noteItemData, String string, boolean visibility,
            boolean checked) {
        // TODO Auto-generated method stub
        mItemData = noteItemData;
        if (visibility) {
            mCheckBox.setVisibility(VISIBLE);
            int srcId = checked ? R.drawable.ic_list_check_on : R.drawable.ic_list_check_off;
            mCheckBox.setImageResource(srcId);
        } else
            mCheckBox.setVisibility(GONE);

        ((ViewGroup.MarginLayoutParams) mTime.getLayoutParams()).rightMargin = mRes
                .getDimensionPixelSize(R.dimen.grid_indicator_time_margin_right);
        setBackground(noteItemData);
        try {
          //  if (WindowManagerGlobal.getWindowManagerService().getNavBarHide())
         //       getLayoutParams().height = mRes.getDimensionPixelSize(R.dimen.grid_item_height_ex);
         //   else
                getLayoutParams().height = mRes.getDimensionPixelSize(R.dimen.grid_item_height);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public NoteItemData getItemData() {
        // TODO Auto-generated method stub
        return mItemData;
    }

    public void onRecycle() {
        // TODO Auto-generated method stub

    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        mAlert = ((ImageView) findViewById(R.id.iv_alert_icon));
        mImage = ((ImageView) findViewById(R.id.thumbnail));//iv_image_icon
        mTime = ((TextView) findViewById(R.id.tv_time));
        mCheckBox = ((ImageView) findViewById(R.id.tv_checkbox));
    }

    protected abstract int getContentMaxLines(boolean hadImage);

    protected abstract void setBackground(NoteItemData noteItemData);

}
