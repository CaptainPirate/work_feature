/*===================================================================================================*	
 *  when  |      who     |    keyword           |        why         |         what                  *	
 *===================================================================================================*	
 *20160407|mengzhiming.wt|porting A1s enjoynotes| Port_EnjoyNotes | porting A1s enjoynotes to CMCC N2*	
*====================================================================================================*/

package com.wingtech.note.loader;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.NinePatchDrawable;
import android.util.Log;
import android.widget.ImageView;

import com.wingtech.note.AttachmentUtils;
import com.wingtech.note.Utils;
import com.wingtech.note.loader.ImageCacheManager;
import com.wingtech.note.R;

import java.lang.ref.WeakReference;

public class ListImageJob implements ImageCacheManager.ImageJob {
    private static final String TAG = "ListImageJob";
    private static WeakReference<Bitmap> sLoadingImage;
    private Context mContext;
    private ImageView nView;
    private String nImageName;
    private ImageCacheManager mCacheManager;
    private Bitmap nBitmap;

    public ListImageJob(Context context, ImageView view, String name) {
        mContext = context;
        nView = view;
        nImageName = name;
    }

    public static Bitmap getLoadingImage(Context context) {
        Bitmap bitmap;
        if (sLoadingImage == null) {
            bitmap = BitmapFactory.decodeResource(context.getResources(),
                    R.drawable.grid_image_cover);
        }
        else {
            bitmap = sLoadingImage.get();
            sLoadingImage = new WeakReference<Bitmap>(bitmap);
        }
        return bitmap;
    }

    public static Bitmap getNotFoundImage(Context context) {
        return getLoadingImage(context);
    }

    public void callback() {
        if (mCacheManager.isValidJob(this)) {
            if (nBitmap != null)
                setImageResult(nBitmap);
            else
                setImageResult(getNotFoundImage(mContext));
            mCacheManager.cancel(nView);
        }
    }

    public void run() {
        NinePatchDrawable drawable = (NinePatchDrawable) mContext.getResources().getDrawable(
                R.drawable.grid_image_cover);
        nBitmap = Utils.clipImageAttachment(
                AttachmentUtils.getAttachmentPath(mContext, nImageName),
                drawable);
        if (nBitmap == null)
            Log.d(TAG, "failed to load image: " + nImageName);
        else {
            Log.d(TAG, "successed to load image: " + nImageName);
            mCacheManager.putCache(getImageKey(), nBitmap);
        }

    }

    public String getImageKey() {
        return nImageName;
    }

    public Object getJobKey() {
        return nView;
    }

    public void setCacheManager(ImageCacheManager imageCachemanager) {
        mCacheManager = imageCachemanager;
    }

    public void setImageResult(Bitmap bitMap) {
        if (bitMap == null)
            nView.setImageBitmap(getLoadingImage(mContext));
        else
            nView.setImageBitmap(bitMap);
    }

}
