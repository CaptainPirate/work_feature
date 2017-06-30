/*===================================================================================================*	
 *  when  |      who     |    keyword           |        why         |         what                  *	
 *===================================================================================================*	
 *20160407|mengzhiming.wt|porting A1s enjoynotes| Port_EnjoyNotes | porting A1s enjoynotes to CMCC N2*	
*====================================================================================================*/

package com.wingtech.note.loader;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.NinePatchDrawable;
import android.util.Log;

import com.wingtech.note.AttachmentUtils;
import com.wingtech.note.Utils;
import com.wingtech.note.editor.InputStreamBuilder;
import com.wingtech.note.loader.ImageCacheManager;
import com.wingtech.note.R;

import java.io.FileNotFoundException;

public class SpannableImageJob implements ImageCacheManager.ImageJob {
    private static final String TAG = "SpanImageJob";
    private Bitmap mBitmap;
    private int mScale;
    private Context mContext;
    private ImageHolder mHolder;
    private String mImageName;
    private int mHeight;
    private int mWidth;
    private ImageCacheManager mCacheManager;

    public SpannableImageJob(Context context, ImageHolder imageHolder,
            String imageName, int width, int height, int scale) {
        mContext = context;
        mHolder = imageHolder;
        mImageName = imageName;
        mWidth = width;
        mHeight = height;
        mScale = scale;
    }

    public void callback() {
        if (mCacheManager.isValidJob(this)){
            if (mBitmap != null)
                setImageResult(mBitmap);
            else
                setImageResult(null);
            mCacheManager.cancel(getJobKey());
        }
    }

    public void run() {
        Log.w("OSBORN", "DO running SpannableImageJob");

        NinePatchDrawable cover = (NinePatchDrawable) mContext.getResources().getDrawable(
                R.drawable.grid_image_cover);
        InputStreamBuilder image = new InputStreamBuilder(AttachmentUtils.getAttachmentPath(
                mContext, mImageName));

        try {
            mBitmap = Utils.resizeImageAttachment(mWidth, mHeight, mScale, image.getInputStream(),
                    cover);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "load image fail: cannot find image file");
        }
        if (mBitmap == null) {
            Log.d(TAG, "failed to load image: " + mImageName);
        } else {
            Log.d(TAG, "successed to load image: " + mImageName);
            mCacheManager.putCache(getImageKey(), mBitmap);
        }
    }

    public String getImageKey() {
        return "SpanImageJob" + mImageName;
    }

    public Object getJobKey() {
        return mHolder;
    }

    public void setCacheManager(ImageCacheManager imageCachemanager) {
        mCacheManager = imageCachemanager;
    }

    public void setImageResult(Bitmap bitMap) {
        if (bitMap == null)
            mHolder.setImage(null);
        else if ((bitMap.getWidth() == mWidth) && (bitMap.getHeight() == mHeight)) {
            mHolder.setImage(bitMap);
        }
        else {// 这一步是尺寸不对，重新load
            ImageCacheManager imagemanager = ImageCacheManager.getInstance(mContext);
            imagemanager.invalidCache(getImageKey());
            imagemanager.load(this);
        }
    }

    public static abstract interface ImageHolder {
        public abstract void setImage(Bitmap bmp);
    }
}

