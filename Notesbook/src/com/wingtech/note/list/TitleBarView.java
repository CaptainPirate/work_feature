/*===================================================================================================*	
 *  when  |      who     |    keyword           |        why         |         what                  *	
 *===================================================================================================*	
 *20160407|mengzhiming.wt|porting A1s enjoynotes| Port_EnjoyNotes | porting A1s enjoynotes to CMCC N2*	
*====================================================================================================*/

package com.wingtech.note.list;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import com.wingtech.note.R;

public class TitleBarView extends FrameLayout {
    public static final int APPEARENCE_MODE_GRID_ROOT = 1;
    public static final int APPEARENCE_MODE_GRID_SUB = 2;
    private ImageView mTitleBack;
    private TextView mTitleText;

    public TitleBarView(Context paramContext) {
        super(paramContext);
    }

    public TitleBarView(Context paramContext, AttributeSet paramAttributeSet) {
        super(paramContext, paramAttributeSet);
    }

    public TitleBarView(Context paramContext, AttributeSet paramAttributeSet,
            int paramInt) {
        super(paramContext, paramAttributeSet, paramInt);
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        mTitleBack = ((ImageView) findViewById(R.id.title_back));
        mTitleText = ((TextView) findViewById(R.id.title_text));
    }

    public void setBackButtonListener(View.OnClickListener listener) {
        mTitleBack.setOnClickListener(listener);
        mTitleText.setOnClickListener(listener);
    }

    public void setTitleText(int paramInt) {
        mTitleText.setText(paramInt);
    }

    public void setTitleText(CharSequence paramCharSequence) {
        mTitleText.setText(paramCharSequence);
    }

    public CharSequence getTitleText() {
        if (mTitleText.getVisibility() == VISIBLE)
            return mTitleText.getText();
        else
            return null;
    }

    public void setAppearenceMode(int mode) {
        switch (mode) {
        case APPEARENCE_MODE_GRID_SUB:
            setBackgroundResource(R.drawable.bg_notes_list);
            this.setVisibility(VISIBLE);
            break;
        default:
        case APPEARENCE_MODE_GRID_ROOT:
            this.setVisibility(GONE);
            break;
        }
    }
    /*
    public void setAppearenceMode(int mode) {
        switch (mode) {
        case APPEARENCE_MODE_GRID_SUB:
            setBackgroundResource(R.drawable.bg_notes_list);
            mTitlePanel.setVisibility(VISIBLE);
            mTitleBack.setVisibility(VISIBLE);
            mTitleText.setVisibility(VISIBLE);
            mTitleBack.setImageResource(R.drawable.list_title_back);
            mTitleText.setTextColor(getResources().getColor(
                    android.R.color.black));
            // mTitlePanel.setBackgroundResource(0);
            break;
        default:
        case APPEARENCE_MODE_GRID_ROOT:
            //setBackgroundResource(R.drawable.bg_notes_list);
            //setBackgroundColor(Color.TRANSPARENT);
            mTitlePanel.setVisibility(GONE);
            mTitleBack.setVisibility(GONE);
            mTitleText.setVisibility(GONE);
            break;
        }
    }
    */
}
