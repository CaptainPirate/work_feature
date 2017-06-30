/*===================================================================================================*	
 *  when  |      who     |    keyword           |        why         |         what                  *	
 *===================================================================================================*	
 *20160407|mengzhiming.wt|porting A1s enjoynotes| Port_EnjoyNotes | porting A1s enjoynotes to CMCC N2*	
*====================================================================================================*/

package com.wingtech.note.editor;

import com.wingtech.note.R;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

public class NotePopupView extends FrameLayout {
    private int mArrowCenterWidth;
    private int mArrowRawX;
    private int[] mMyLocation = new int[2];
    private View mPopupArrowCenter;
    private View mPopupArrowStart;
    private View mPopupArrowEnd;
    private ViewGroup mPopupContent;

    public NotePopupView(Context paramContext, AttributeSet paramAttributeSet, int paramInt) {
        super(paramContext, paramAttributeSet, paramInt);
    }

    public NotePopupView(Context paramContext, AttributeSet paramAttributeSet) {
        this(paramContext, paramAttributeSet, 0);
    }

    public NotePopupView(Context paramContext) {
        this(paramContext, null);
    }
    public void setArrowRawX(int x){
      mArrowRawX = x;
    }
    public View setActionView(int resId) {
      View view = LayoutInflater.from(getContext()).inflate(resId, mPopupContent, false);
      mPopupContent.addView(view);
      return view;
    }
    protected void onFinishInflate(){
      super.onFinishInflate();
      mPopupContent = ((ViewGroup)findViewById(R.id.popup_content));
      mPopupArrowCenter = findViewById(R.id.popup_arrow_center);
      BitmapDrawable localBitmapDrawable = (BitmapDrawable)mPopupArrowCenter.getBackground();
      mArrowCenterWidth = localBitmapDrawable.getIntrinsicWidth();
      ((ViewGroup.MarginLayoutParams)mPopupContent.getLayoutParams()).topMargin = localBitmapDrawable.getIntrinsicHeight();
    } 
    protected void onLayout(boolean changed, int left, int top, int right, int bottom){
      int width = getMeasuredWidth();
      getLocationOnScreen(mMyLocation);
      int x = mMyLocation[0];
      if ((mArrowRawX < x) || (mArrowRawX > x + width))
        mArrowRawX = (x + width / 2);
      //+RTL,tangzihui.wt,modify,2015.08.05,discriminate between LTR and RTL.
      if (getLayoutDirection() == View.LAYOUT_DIRECTION_LTR) {
          mPopupArrowStart = findViewById(R.id.popup_arrow_left);
          mPopupArrowEnd = findViewById(R.id.popup_arrow_right);
      } else {
          mPopupArrowStart = findViewById(R.id.popup_arrow_right);
          mPopupArrowEnd = findViewById(R.id.popup_arrow_left);
      }
      mPopupArrowStart.getLayoutParams().width = mArrowRawX - x - mArrowCenterWidth / 2;
      mPopupArrowEnd.getLayoutParams().width = width - mArrowCenterWidth - mPopupArrowStart.getLayoutParams().width;
      //-RTL,tangzihui.wt,modify,2015.08.05,discriminate between LTR and RTL.
      measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
      super.onLayout(changed, left, top, right, bottom);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // TODO Auto-generated method stub
        super.onMeasure(0, 0);
    }  
      
}
