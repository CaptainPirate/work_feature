/*===================================================================================================*	
 *  when  |      who     |    keyword           |        why         |         what                  *	
 *===================================================================================================*	
 *20160407|mengzhiming.wt|porting A1s enjoynotes| Port_EnjoyNotes | porting A1s enjoynotes to CMCC N2*	
*====================================================================================================*/

package com.wingtech.note.editor;

import com.wingtech.note.Utils;
import com.wingtech.note.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.PopupWindow;

public class NotePopupWindow extends PopupWindow {
    private View mAnchorView;
    protected Context mContext;
    private int[] mLocation;
    private int mLocationOffset;
    private NotePopupView mPopupView;

    public NotePopupWindow(Context context, int width, int height) {
        super(width, height);
        mContext = context;
        mLocation = new int[2];
        mLocationOffset = mContext.getResources().getDimensionPixelSize(
                R.dimen.note_popup_window_location_offset);
        setBackgroundDrawable(mContext.getResources().getDrawable(android.R.color.transparent));
        setInputMethodMode(PopupWindow.INPUT_METHOD_NOT_NEEDED);
        setFocusable(false);
        setTouchable(true);
        setOutsideTouchable(true);
        LayoutInflater inflater = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mPopupView = (NotePopupView) inflater.inflate(R.layout.popup_window, null);
        setContentView(mPopupView);
    }

    public NotePopupWindow(Context paramContext) {
        this(paramContext, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }

    protected View setActionView(int view) {
        return mPopupView.setActionView(view);
    }

    public void setArrowRawX(int arrowRawX) {
        mPopupView.setArrowRawX(arrowRawX);
    }

    public int[] getContentSize() {
        int[] size;
        if (isShowing()) {
            size = new int[2];
            size[0] = getWidth();
            size[1] = getHeight();
        }
        else
            size = Utils.measureView(getContentView());
        return size;
    }

    public void show(View view, int xoff, int yoff) {
        if (isShowing())
            update(view, xoff, yoff);
        else
            showAsDropDown(view, xoff, yoff);
    }

    public void showAsDropDown(View view, int xoff, int yoff) {
        mAnchorView = view;
        super.showAsDropDown(view, xoff, yoff - mLocationOffset);
    }

    public void update(View view, int offx, int offy) {
        mAnchorView = view;
        super.update(view, offx, offy - mLocationOffset, getWidth(), getHeight());
    }

    protected boolean touchInAttachedView(MotionEvent event) {
        mAnchorView.getLocationOnScreen(mLocation);
        if ((event.getRawX() >= mLocation[0])
                && (event.getRawX() <= mLocation[0] + mAnchorView.getWidth())
                && (event.getRawY() >= mLocation[1])
                && (event.getRawY() <= mLocation[1] + mAnchorView.getHeight()))
            return true;
        else
            return false;

    }
}
