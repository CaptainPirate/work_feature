/*===================================================================================================*	
 *  when  |      who     |    keyword           |        why         |         what                  *	
 *===================================================================================================*	
 *20160407|mengzhiming.wt|porting A1s enjoynotes| Port_EnjoyNotes | porting A1s enjoynotes to CMCC N2*	
*====================================================================================================*/

package com.wingtech.note.editor;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.FontMetricsInt;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.util.DisplayMetrics;

import com.wingtech.note.AttachmentUtils;
import com.wingtech.note.loader.ImageCacheManager;
import com.wingtech.note.loader.SpannableImageJob;

import java.io.File;

public class TouchableImageSpan extends TouchableBaseSpan implements SpannableImageJob.ImageHolder {

    private BitmapDrawable nDrawable;
    private Context mContext;
    boolean nMissed;
    private int nScale;
    final String nContent;
    private Rect nBounds;
    //bug156714,tangzihui.wt,add,2016.03.19,for RichText display.
    private int vsh = 0;

    public TouchableImageSpan(Context context, RichEditView e, String name) {
        super(context, null, e);// 需要在initialize()中初始化mWidth,mHeight
        mContext = context;
        mEdit = e;
        nMissed = true;
        nContent = name;
        // TODO Auto-generated constructor stub
    }

    public void initialize() {
        File img = new File(AttachmentUtils.getAttachmentPath(mContext, nContent));
        if (img.exists()) {
            nMissed = false;
            BitmapFactory.Options option = new BitmapFactory.Options();
            option.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(img.getAbsolutePath(), option);
            mWidth = option.outWidth;
            mHeight = option.outHeight;
            if (mWidth <= 0 || mHeight <= 0) {
                nMissed = true;
            }
            else {
                DisplayMetrics localDisplayMetrics = mContext.getResources().getDisplayMetrics();
                int i = (int) (0.8F * localDisplayMetrics.widthPixels);
                int j = (int) (0.7F * localDisplayMetrics.heightPixels);
                // 先缩放高度，再缩放宽度
                if (mHeight > j) {
                    mWidth = mWidth * j / mHeight;
                    mHeight = j;
                }

                if (mWidth > i) {
                    mHeight = mHeight * i / mWidth;
                    mWidth = i;
                }
                int sw, sh;
                int lh = mEdit.getLineHeight();
                int off = lh + mEdit.getPaint().getFontMetricsInt().ascent;
                //+Bug272306,guchenghong,wt,modify,20140425,note display problem.
                if((mHeight + off) < lh){
                    sh = mHeight;
                    //bug156714,tangzihui.wt,add,2016.03.19,for RichText display.
                    vsh = 0;
                }else{
                    sh = lh * ((mHeight + off) / lh)
                         - off;
                    //bug156714,tangzihui.wt,add,2016.03.19,for RichText display.
                    vsh = off;
                }
                //-Bug272306,guchenghong,wt,modify,20140425,note display problem.
                sw = sh * mWidth / mHeight;
                nBounds = new Rect(0, 0, sw, sh);
                nScale = option.outWidth / sw;
            }
        }
        getCachedDrawable();

    }

    @Override
    public BitmapDrawable getCachedDrawable() {
        //bug 176422,  mengzhiming.wt,  add,20160518, start
        if((nDrawable != null) && (null == nDrawable.getBitmap())){
            return nDrawable;
        }
        //bug 176422,  mengzhiming.wt,  add,20160518, end
        if ((nDrawable != null) && (nDrawable.getBitmap().isRecycled()))
            nDrawable = null;
        if (nDrawable == null) {
            nDrawable = mEdit.getMissedDrawable();
            if (!nMissed) {
                SpannableImageJob localSpannableImageJob = new SpannableImageJob(mContext, this,
                        nContent, nBounds.width(), nBounds.height(), nScale);
                ImageCacheManager.getInstance(mContext).load(localSpannableImageJob);
            }
        }
        return nDrawable;
    }

    @Override
    public void draw(Canvas canvas, CharSequence text, int start, int end, float x, int top, int y,
            int bottom, Paint paint) {
        mLeft = (int) x;
        mBottom = bottom;
        BitmapDrawable cachedBmp = getCachedDrawable();
        canvas.save();
        int i = bottom - nBounds.bottom;
        if (mVerticalAlignment == 1)
            i -= paint.getFontMetricsInt().descent;
        canvas.translate(x, i);
        if (cachedBmp == null) {
            canvas.drawRect(mEdit.mImageCoverPadding.left, mEdit.mImageCoverPadding.top,
                    nBounds.width() - mEdit.mImageCoverPadding.right, nBounds.height()
                            - mEdit.mImageCoverPadding.bottom, mEdit.mMissedBackgroundPaint);
            mEdit.mImageCoverDrawable.setBounds(nBounds);
            mEdit.mImageCoverDrawable.draw(canvas);
        }
        else
            cachedBmp.draw(canvas);
        canvas.restore();
    }

    public String getName() {
        return nContent;
    }

    @Override
    public int getSize(Paint paint, CharSequence text, int start, int end, FontMetricsInt fm) {
        Rect rect = nBounds;
        if (rect == null)
            return 0;
        if (fm != null) {
            fm.ascent = (-rect.bottom);
            //bug156714,tangzihui.wt,modify,2016.03.19,for RichText display.
            fm.descent = vsh;
            fm.top = fm.ascent;
            //bug156714,tangzihui.wt,modify,2016.03.19,for RichText display.
            fm.bottom = fm.descent;
        }
        return rect.right;
    }

    public void recycle() {
        ImageCacheManager.getInstance(mContext).cancel(this);
        nDrawable = null;
    }

    public void setImage(Bitmap bmp) {
        // TODO Auto-generated method stub
        if (bmp != null) {
            nDrawable = new BitmapDrawable(mContext.getResources(), bmp);
            nDrawable.setBounds(0, 0, nDrawable.getIntrinsicWidth(),
                    nDrawable.getIntrinsicHeight());
            mEdit.invalidate();
        }

    }
}

