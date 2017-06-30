/*===================================================================================================*	
 *  when  |      who     |    keyword           |        why         |         what                  *	
 *===================================================================================================*	
 *20160407|mengzhiming.wt|porting A1s enjoynotes| Port_EnjoyNotes | porting A1s enjoynotes to CMCC N2*	
*====================================================================================================*/

package com.wingtech.note.editor;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;//bug156714,tangzihui.wt
import android.graphics.Paint;

public class TouchableContactSpan extends TouchableBaseSpan {
    private final String nContactName;
    private final String nPhoneNumber;

    public TouchableContactSpan(Context context, Bitmap b, RichEditView e,String name,String number) {
        super(context, b, e);
        // TODO Auto-generated constructor stub
        nContactName = name;
        nPhoneNumber = number;
    }
    public String getContactName(){
      return nContactName;
    }

    public String getPhoneNumber(){
      return nPhoneNumber;
    }
    public int getSize(Paint paramPaint, CharSequence text, int start, int end, Paint.FontMetricsInt fm){
      int size = super.getSize(paramPaint, text, start, end, fm);
      if (fm != null){
        Paint.FontMetricsInt localFontMetricsInt = mEdit.getPaint().getFontMetricsInt();
        //+bug156714,tangzihui.wt,modify,2016.03.19,for RichText display.
        Drawable b = getCachedDrawable();
        int lh = mEdit.getLineHeight();
        int sh = 0;
        int off = lh + localFontMetricsInt.ascent;
        
        if((mHeight + off) < lh){
            sh = mHeight;
        }else{
            sh = lh * ((mHeight + off) / lh) - off;
        }
        int textLength = mEdit.getText().length();
        fm.ascent = -sh;
        fm.descent = mHeight + fm.ascent;
        fm.top = fm.ascent;
        fm.bottom = fm.descent;
        //-bug156714,tangzihui.wt,modify,2016.03.19,for RichText display.
      }
      return size;
    }

//+bug_270636, zhoupengfei.wt, ADD, 20140408
	@Override
	public void draw(Canvas canvas, CharSequence text, int start, int end,
			float x, int top, int y, int bottom, Paint paint) {
		// TODO Auto-generated method stub
		Paint.FontMetricsInt fm = mEdit.getPaint().getFontMetricsInt();
        //+bug156714,tangzihui.wt,modify,2016.03.19,for RichText display.
        Drawable b = getCachedDrawable();
        canvas.save();
        
        int transY = bottom - b.getBounds().bottom;
        if (mVerticalAlignment == ALIGN_BASELINE) {
            transY -= paint.getFontMetricsInt().descent;
            transY += fm.descent;
        } 

        canvas.translate(x, transY);
        b.draw(canvas);
        //-bug156714,tangzihui.wt,add,2016.03.19,for RichText display.
        canvas.restore();
	}
//-bug_270636, zhoupengfei.wt, ADD, 20140408    
}
