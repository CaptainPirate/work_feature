/*===================================================================================================*	
 *  when  |      who     |    keyword           |        why         |         what                  *	
 *===================================================================================================*	
 *20160407|mengzhiming.wt|porting A1s enjoynotes| Port_EnjoyNotes | porting A1s enjoynotes to CMCC N2*	
*====================================================================================================*/

package com.wingtech.note.editor;

import java.lang.ref.WeakReference;//bug156714,tangzihui.wt

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.style.ImageSpan;
import android.widget.EditText;

public class TouchableBaseSpan extends ImageSpan {
    protected RichEditView mEdit;
    protected int mWidth;
    protected int mHeight;
    protected int mLeft;
    protected int mBottom;

    public TouchableBaseSpan(Context context, Bitmap b, RichEditView e) {
        super(b, ALIGN_BASELINE);
        if (b != null) {
            //bug156714,tangzihui.wt,modify,2016.03.19,for RichText display.
            Drawable d = getCachedDrawable();
            mWidth = d.getIntrinsicWidth();
            mHeight = d.getIntrinsicHeight();
        }
        mEdit = e;
    }

    @Override
    public void draw(Canvas canvas, CharSequence text, int start, int end, float x, int top, int y,
            int bottom, Paint paint) {
        mLeft = (int) x;
        mBottom = bottom;
        super.draw(canvas, text, start, end, x, top, y, bottom, paint);
    }

    public int getBottom() {
        return mBottom - mEdit.getPaint().getFontMetricsInt().descent;
    }

    public int getLeft() {
        return mLeft + mEdit.getCompoundPaddingLeft();
    }

    // 此处的x,y是相对传入的EditText的坐标
    public final boolean isTouched(int x, int y) {
        int i = getLeft();
        int j = getBottom();
        if ((x >= i) && (x <= i + mWidth) && (y >= j - mHeight) && (y <= j))
            return true;
        return false;
    }

    //+bug156714,tangzihui.wt,add,2016.03.19,for RichText display.
    protected Drawable getCachedDrawable() {
        WeakReference<Drawable> wr = mDrawableRef;
        Drawable d = null;

        if (wr != null)
            d = wr.get();

        if (d == null) {
            d = getDrawable();
            mDrawableRef = new WeakReference<Drawable>(d);
        }

        return d;
    }

    private WeakReference<Drawable> mDrawableRef;
    //-bug156714,tangzihui.wt,add,2016.03.19,for RichText display.
}

