/*===================================================================================================*	
 *  when  |      who     |    keyword           |        why         |         what                  *	
 *===================================================================================================*	
 *20160407|mengzhiming.wt|porting A1s enjoynotes| Port_EnjoyNotes | porting A1s enjoynotes to CMCC N2*	
*====================================================================================================*/

package com.wingtech.note.loader;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.util.Log;
import android.util.LruCache;

import com.wingtech.note.R;

import java.util.HashMap;

public class ImageCacheManager {
    private static final String TAG = "ImageCacheManager";
    private static ImageCacheManager sInstance;
    private LruCache<String, Bitmap> mCache;
    private Context mContext;
    private HashMap<Object, ImageJob> mJobMap;
    private Resources mRes;
    private LoadingWorker mWorker;

    private ImageCacheManager(Context context) {
        mContext = context.getApplicationContext();
        mRes = context.getResources();
        mCache = new LruCache<String, Bitmap>(mRes.getInteger(R.integer.image_cache_limit)) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getByteCount();
            }
        };
        mWorker = new LoadingWorker(mContext, TAG);
        mJobMap = new HashMap<Object, ImageJob>();
    }

    public static ImageCacheManager getInstance(Context context) {
        if (sInstance == null)
            sInstance = new ImageCacheManager(context);
        return sInstance;
    }

    public void cancel(Object jobKey) {
        ImageJob iamgeJob = mJobMap.remove(jobKey);
        if (iamgeJob != null) {
            Log.d(TAG, "cancel loading image: " + iamgeJob.getImageKey());
            mWorker.removeJob(iamgeJob);
        }
    }

    public void invalidCache(String imageKye) {
        mCache.remove(imageKye);
    }

    public boolean isValidJob(ImageJob imageJob) {
        if (mJobMap.get(imageJob.getJobKey()) == imageJob)
            return true;
        else
            return false;
    }

    public void load(ImageJob imageJob) {
        Log.d(TAG, "load image: " + imageJob.getImageKey());
        Bitmap bitMap = mCache.get(imageJob.getImageKey());
        if (bitMap != null) {
            Log.d(TAG, "find image in cache: " + imageJob.getImageKey());
            imageJob.setImageResult(bitMap);
        }
        else {
            imageJob.setImageResult(null);
            cancel(imageJob.getJobKey());
            Log.d(TAG, "schedule job in queue: " + imageJob.getImageKey());
            imageJob.setCacheManager(this);
            mJobMap.put(imageJob.getJobKey(), imageJob);
            mWorker.addJob(imageJob);
        }
    }

    public void pause() {
        mWorker.pause();
    }

    public void putCache(String cachekey, Bitmap bitMap) {
        mCache.put(cachekey, bitMap);
    }

    public void resume() {
        mWorker.resume();
    }

    public void start() {
        mWorker.start();
    }

    public void stop() {
        mWorker.stop();
        mJobMap.clear();
    }

    public static abstract interface ImageJob extends LoadingWorker.Job {
        public abstract String getImageKey();

        public abstract Object getJobKey();

        public abstract void setCacheManager(ImageCacheManager imageCachemanager);

        public abstract void setImageResult(Bitmap bitMap);
    }
}
