package com.android.launcher3;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

import android.app.Service;
import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;
import com.android.gallery3d.common.Utils;

/*******************************************************************************************************
|   when     |      who     |    keyword       |        why        |     what                          |
********************************************************************************************************
20160325      liyichong.wt   bug 155067         requirements        add class
20160405      liyichong.wt   other              requirements        modify code format
20160504      liyichong.wt   bug 165731         requirements        add lockscreen function
20160520      liyichong.wt   bug 178836         requirements        modify lockscreen size
20160705      liyichong.wt   bug 194922         bugs                modify null exception
20160718      liyichong.wt   bug 165731         bugs                modify lockscreen wallpaper display incorrect
******************************************************************************************************/
public class CmccN2PreferenceWallpaperServer extends Service {

    private static final String TAG = "CmccN2PreferenceWallpaperServer";
    /* bug 210047, liyichong.wt, ADD, 20160819 start*/
    private static final int SETTING_LOCKSCREEN_WALLPAPER = 1;
    private static final int RETURN_LOCKSCREEN_SUCCESS = 2;
    private boolean mLockAlready = true;
    /* bug 210047, liyichong.wt, ADD, 20160819 end*/
    private ArrayList<Integer> mImages;
    int current = 0;
    Resources resources;
    public void onCreate() {
        // TODO Auto-generated method stub
        super.onCreate();
        findWallpapers();
        /* bug 165731, liyichong.wt, ADD, 20160504 start*/
        final IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);//bug 210047, liyichong.wt, MODIFY, 20160819
        BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                // TODO Auto-generated method stub
                final String action = intent.getAction();
                int lockscreeb_type = 0;
                /**
                try {
                    lockscreeb_type = Settings.System.getInt(getContentResolver(), Settings.System.LOCKSCREEN_TYPE);
                } catch (SettingNotFoundException e) {
                    // TODO Auto-generated catch block
                    Log.i(TAG,"Setting Not Found Exception");
                }
                */
                if(Intent.ACTION_SCREEN_OFF.equals(action) && lockscreeb_type == 0){//bug 210047, liyichong.wt, MODIFY, 20160819
                    if(current > mImages.size() - 1){//bug 211232, liyichong.wt, MODIFY, 20160823
                        current = 0;
                    }
                    /* bug 210047, liyichong.wt, MODIFY, 20160819 start*/
                    if(mLockAlready){
                        sHandler.sendMessage(sHandler.obtainMessage(SETTING_LOCKSCREEN_WALLPAPER,current++));
                    }
                    /* bug 210047, liyichong.wt, MODIFY, 20160819 end*/
                }
            }
        };
        registerReceiver(mBroadcastReceiver, filter);
        /* bug 165731, liyichong.wt, ADD, 20160504 end*/
    }

    private void findWallpapers() {
        mImages = new ArrayList<Integer>(24);
        
         resources = getResources();
        /* bug 165731, liyichong.wt, MODIFY, 20160718 start*/
        final String packageName = resources.getResourcePackageName(R.array.lockscreen_wallpapers);

        addWallpapers(resources, packageName, R.array.lockscreen_wallpapers);
        /* bug 165731, liyichong.wt, MODIFY, 20160718 end*/
    }

    private void addWallpapers(Resources resources, String packageName, int list) {
        final String[] extras = resources.getStringArray(list);
        for (String extra : extras) {
            int res = resources.getIdentifier(extra, "drawable", packageName);
            if (res != 0) {
                mImages.add(res);
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // TODO Auto-generated method stub
        /* bug 165731, liyichong.wt, MODIFY, 20160504 start*/
        /* bug 194922, liyichong.wt, MODIFY, 20160705 start*/
        if(intent != null){
            final String action = intent.getAction();
            int lockscreeb_type = 0;
            /**
            try {
                lockscreeb_type = Settings.System.getInt(getContentResolver(), Settings.System.LOCKSCREEN_TYPE);
            } catch (SettingNotFoundException e) {
                // TODO Auto-generated catch block
                Log.i(TAG,"Setting Not Found Exception");
            }
            */
            if(Intent.ACTION_SCREEN_OFF.equals(action) && lockscreeb_type == 0){//bug 210047, liyichong.wt, MODIFY, 20160819
                if(current > mImages.size() - 1){//bug 211232, liyichong.wt, MODIFY, 20160823
                    current = 0;
                }
                /* bug 210047, liyichong.wt, MODIFY, 20160819 start*/
                if(mLockAlready){
                    sHandler.sendMessage(sHandler.obtainMessage(SETTING_LOCKSCREEN_WALLPAPER,current++));
                }
                /* bug 210047, liyichong.wt, MODIFY, 20160819 end*/
            }
        }
        /* bug 194922, liyichong.wt, MODIFY, 20160705 end*/
        /* bug 165731, liyichong.wt, MODIFY, 20160504 end*/
        return START_STICKY;
    }

    private void selectWallpaper(int position) {
        /* bug 211238, liyichong.wt, ADD, 20160823 start*/
        ByteArrayOutputStream tmpOut = null;
        ByteArrayInputStream tmpInt = null;
        /* bug 211238, liyichong.wt, ADD, 20160823 end*/
        try {
        Drawable mDrawable=resources.getDrawable(mImages.get(position));
        /* bug 178836, liyichong.wt, MODIFY, 20160520 start*/
        Bitmap mBitmap = Bitmap.createBitmap(1080, 1920, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(mBitmap);
        mDrawable.setBounds(new Rect(0,0 ,1080,1920));
        /* bug 178836, liyichong.wt, MODIFY, 20160520 end*/
        mDrawable.draw(c);
        c.setBitmap(null);
    
        //WallpaperManager wpm = (WallpaperManager) getActivity().getSystemService(
        //        Context.WALLPAPER_SERVICE);
        WallpaperManager wpm = WallpaperManager.getInstance(this);
        //wpm.setResource(mImages.get(position));
        /* bug 165731, liyichong.wt, MODIFY, 20160504 start*/
        /* bug 211238, liyichong.wt, MODIFY, 20160823 start*/
        //wpm.setLockscreenBitmap(mBitmap);
        tmpOut = new ByteArrayOutputStream(2048);
        mBitmap.compress(CompressFormat.JPEG, 100, tmpOut);
        byte[] outByteArray = tmpOut.toByteArray();
        tmpInt = new ByteArrayInputStream(outByteArray);
        //wpm.setLockscreenStream(tmpInt);
        //Settings.System.putInt(getContentResolver(), Settings.System.LAUNCHER_LOCKSCREEN_WALLPAPER, position);
        sHandler.sendMessageDelayed(sHandler.obtainMessage(RETURN_LOCKSCREEN_SUCCESS), 500);//bug 211232, liyichong.wt, MODIFY, 20160823
        /* bug 165731, liyichong.wt, MODIFY, 20160504 end*/

        } catch (Exception e) {
            Log.e(TAG, "Failed to set wallpaper: " + e);
        } finally {
            Utils.closeSilently(tmpOut);
            Utils.closeSilently(tmpInt);
        }
        /* bug 211238, liyichong.wt, MODIFY, 20160823 end*/
    }

    @Override
    public IBinder onBind(Intent arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    /* bug 210047, liyichong.wt, ADD, 20160819 start*/
    private Handler sHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case SETTING_LOCKSCREEN_WALLPAPER:
                final int MessCount = (Integer) msg.obj ;
                new Thread(){
                    public void run() {
                        mLockAlready = false;
                        selectWallpaper(MessCount);
                    };
                }.start();
                break;
            /* special case */
            case RETURN_LOCKSCREEN_SUCCESS:
                mLockAlready = true;
                break;
            default:
                break;
            }
        }
    };
    /* bug 210047, liyichong.wt, ADD, 20160819 end*/
}
