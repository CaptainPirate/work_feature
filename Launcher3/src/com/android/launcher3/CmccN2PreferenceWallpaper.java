package com.android.launcher3;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.Toast;
import java.io.File;
import java.util.ArrayList;

/*******************************************************************************************************
|   when     |      who     |    keyword       |        why        |     what                          |
********************************************************************************************************
20160325     liyichong.wt   bug 155067          requirements        add class
20160405     liyichong.wt   bug 155015          requirements        add lockscreen modify
20160405     liyichong.wt   other               requirements        modify code format
20160413     liyichong.wt   other               bugs                fixed thumb Null
20160426     liyichong.wt   bug 166387          requirements        add string
20160504     liyichong.wt   bug 165731          requirements        add lockscreen function
20160620     liyichong.wt   bug 185772          bugs                add thumb when live wallpaper set success
20160718     liyichong.wt   bug 198637          bugs                modify Perference list style
20160823     liyichong.wt   bug 211238          bugs                modify Bugs
******************************************************************************************************/
public class CmccN2PreferenceWallpaper extends PreferenceActivity {

    /* bug 165731, liyichong.wt, ADD, 20160504 start*/
    private static final String TAG = "CmccN2PreferenceWallpaper";
    private String LOCKSCREEN_SERVICE = "lockscreen_service";
    /* bug 165731, liyichong.wt, ADD, 20160504 end*/
    private ImageView lock_screen_wallpaper;
    private ImageView home_wallpaper;
    private WallpaperManager mWallpaperManager;
    private Drawable mWallpaperDrawable;
    /* bug 155015, liyichong.wt, ADD, 20160405 start*/
    private Drawable mLockScreenWallpaper;
    /* bug 155015, liyichong.wt, ADD, 20160405 end*/
    private SwitchPreference  mRandomWallpaper;
    private static final int REQUEST_PICK_WALLPAPER = 10;

    private AlarmManager alarmManager;
    private Intent intent;
    private PendingIntent pi;
    /* bug 185772, liyichong.wt, ADD, 20160620 start*/
    private int sCurrentHomeWallpaper;
    private int sCurrentLiveWallpaper;
    /* bug 211238, liyichong.wt, ADD, 20160823 start*/
    private int sCurrentLockscreenWallpaper;
    private int sRandomLockscreenServer;
    /* bug 211238, liyichong.wt, ADD, 20160823 end*/
    /* bug 185772, liyichong.wt, ADD, 20160620 end*/

    /* other_bugs, liyichong.wt, ADD, 20160413 start*/
    private File mDefaultLockScreenWallpaper;
    /* other_bugs, liyichong.wt, ADD, 20160413 end*/
    /* bug 211238, liyichong.wt, ADD, 20160823 start*/
    private ArrayList<Integer> mImages;
    Resources resources;
    /* bug 211238, liyichong.wt, ADD, 20160823 end*/

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);

        setContentView(R.layout.wallpaper_set);
        addPreferencesFromResource(R.xml.preference_wallpaper_settings);
        lock_screen_wallpaper = (ImageView) findViewById(R.id.lock_screen_wallpaper);
        home_wallpaper = (ImageView) findViewById(R.id.home_wallpaper);

        intent = new Intent(CmccN2PreferenceWallpaper.this,CmccN2PreferenceWallpaperServer.class);
        pi=PendingIntent.getService(CmccN2PreferenceWallpaper.this, 0, intent, 0);
        alarmManager=(AlarmManager)getSystemService(Service.ALARM_SERVICE);
        findWallpapersThumb();//bug 211238, liyichong.wt, ADD, 20160823

        /* bug 165731, liyichong.wt, ADD, 20160504 start*/
        int lockscreeb_type = 0;
        /**
        try {
            lockscreeb_type = Settings.System.getInt(getContentResolver(), Settings.System.LOCKSCREEN_TYPE);
        } catch (SettingNotFoundException e) {
            // TODO Auto-generated catch block
            Log.i(TAG,"Setting Not Found Exception");
        }
        */
        Log.i(TAG,"lockscreeb_type = " + lockscreeb_type);
        mRandomWallpaper = (SwitchPreference) findPreference("randomWallpaper");
        if(lockscreeb_type == 0){
            mRandomWallpaper.setEnabled(true);
            //mRandomWallpaper.setSummaryOff(R.string.randomchange_off);
        }else{
            mRandomWallpaper.setEnabled(false);
            mRandomWallpaper.setChecked(false);
            //mRandomWallpaper.setSummaryOff(R.string.randomchange_enable_false);
            stopService(intent);
            Settings.System.putInt(getContentResolver(), "random_lockscreen", 0);//bug 165731, liyichong.wt, ADD, 20160613
            SharedPreferences sp = getSharedPreferences(LOCKSCREEN_SERVICE, Context.MODE_PRIVATE);
            Editor editor = sp.edit();
            editor.putBoolean(LOCKSCREEN_SERVICE, false);
            editor.commit();
        }
        /* bug 165731, liyichong.wt, ADD, 20160504 end*/
        mRandomWallpaper.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference arg0, Object arg1) {
                if("true".equals(arg1.toString())){
                    /* bug 165731, liyichong.wt, MODIFY, 20160504 start*/
                    //launchChangeService();
                    Log.d(TAG, "startService");
                    startService(intent);
                    Settings.System.putInt(getContentResolver(), "random_lockscreen", 1);//bug 165731, liyichong.wt, ADD, 20160613
                    SharedPreferences sp = getSharedPreferences(LOCKSCREEN_SERVICE, Context.MODE_PRIVATE);
                    Editor editor = sp.edit();
                    editor.putBoolean(LOCKSCREEN_SERVICE, true);
                    editor.commit();
                    /* bug 165731, liyichong.wt, MODIFY, 20160504 end*/
                }else {
                    /* bug 165731, liyichong.wt, MODIFY, 20160504 start*/
                    //alarmManager.cancel(pi);
                    Log.d(TAG, "stopService");
                    stopService(intent);
                    Settings.System.putInt(getContentResolver(), "random_lockscreen", 0);//bug 165731, liyichong.wt, ADD, 20160613
                    SharedPreferences sp = getSharedPreferences(LOCKSCREEN_SERVICE, Context.MODE_PRIVATE);
                    Editor editor = sp.edit();
                    editor.putBoolean(LOCKSCREEN_SERVICE, false);
                    editor.commit();
                    /* bug 165731, liyichong.wt, MODIFY, 20160504 end*/
                }
                return true;
            }
        });

        mWallpaperManager = WallpaperManager
                .getInstance(this);
        /* bug 185772, liyichong.wt, ADD, 20160620 start*/
        /**
        try {
            sCurrentHomeWallpaper = Settings.System.getInt(getContentResolver(), Settings.System.LAUNCHER_HOME_WALLPAPER);
            sCurrentLockscreenWallpaper = Settings.System.getInt(getContentResolver(), Settings.System.LAUNCHER_LOCKSCREEN_WALLPAPER);//bug 211238, liyichong.wt, ADD, 20160823
            sCurrentLiveWallpaper = Settings.System.getInt(getContentResolver(), "launcher_live_wallpaper");
        } catch (SettingNotFoundException e) {
            // TODO Auto-generated catch block
            Log.i(TAG,"Setting Not Found Exception");
        }
        */
        if(sCurrentHomeWallpaper == -1 && sCurrentLiveWallpaper != -1){
            mWallpaperDrawable = getResources().getDrawable(R.drawable.live_wallpaper_thumb);
        }else{
            mWallpaperDrawable = mWallpaperManager.getDrawable();
        }
        /* bug 185772, liyichong.wt, ADD, 20160620 end*/
        /* bug 155015, liyichong.wt, MODIFY, 20160405 start*/
        /* other_bugs, liyichong.wt, MODIFY, 20160413 start*/
        mDefaultLockScreenWallpaper = new File("data/system/users/0", "lockscreenwallpaper");
        /**
        mLockScreenWallpaper = new BitmapDrawable(mWallpaperManager.getLockscreenBitmap());
        /* bug 211238, liyichong.wt, MODIFY, 20160823 start*/
        /**
        try {
            sRandomLockscreenServer = Settings.System.getInt(getContentResolver(), "random_lockscreen");
        } catch (SettingNotFoundException e) {
            // TODO Auto-generated catch block
            Log.i(TAG,"Setting Not Found Exception");
        }
        */
        if(mDefaultLockScreenWallpaper.exists() && sRandomLockscreenServer == 0){
            lock_screen_wallpaper.setBackgroundDrawable(mLockScreenWallpaper);
        }else if(sRandomLockscreenServer == 1){
            if(sCurrentLockscreenWallpaper >= 0 && sCurrentLockscreenWallpaper <= (mImages.size() - 1)){
                lock_screen_wallpaper.setBackgroundResource(mImages.get(sCurrentLockscreenWallpaper));
            }else{
                lock_screen_wallpaper.setBackgroundDrawable(mLockScreenWallpaper);
            }
        }
        /* bug 211238, liyichong.wt, MODIFY, 20160823 end*/
        /* other_bugs, liyichong.wt, MODIFY, 20160413 end*/
        /* bug 155015, liyichong.wt, MODIFY, 20160405 end*/
        home_wallpaper.setBackgroundDrawable(mWallpaperDrawable);
        
        lock_screen_wallpaper.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                // TODO Auto-generated method stub
                final Intent pickWallpaper = new Intent(Intent.ACTION_SET_WALLPAPER);
                /* bug 155015, liyichong.wt, MODIFY, 20160405 start*/
                pickWallpaper.putExtra(CmccN2HomeWallpaperTabhost.EXTRA_WALLPAPER_TARGET,
                        CmccN2HomeWallpaperTabhost.TARGET_LOCKSCREEN);
                pickWallpaper.setClassName("com.android.launcher3", "com.android.launcher3.CmccN2HomeWallpaperTabhost");
                /* bug 155015, liyichong.wt, MODIFY, 20160405 end*/
                startActivity(pickWallpaper);
            }
        });

        home_wallpaper.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                // TODO Auto-generated method stub
                Intent tabhostIntent = new Intent(Intent.ACTION_VIEW, null);
                tabhostIntent.setClassName("com.android.launcher3", "com.android.launcher3.CmccN2HomeWallpaperTabhost");
                startActivity(tabhostIntent);
            }
        });
    }

    protected ComponentName getWallpaperPickerComponent() {
        return new ComponentName(getPackageName(), LauncherWallpaperPickerActivity.class.getName());
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        /* bug 185772, liyichong.wt, ADD, 20160620 start*/
        /**
        try {
            sCurrentHomeWallpaper = Settings.System.getInt(getContentResolver(), Settings.System.LAUNCHER_HOME_WALLPAPER);
            sCurrentLockscreenWallpaper = Settings.System.getInt(getContentResolver(), Settings.System.LAUNCHER_LOCKSCREEN_WALLPAPER);//bug 211238, liyichong.wt, ADD, 20160823
            sCurrentLiveWallpaper = Settings.System.getInt(getContentResolver(), "launcher_live_wallpaper");
        } catch (SettingNotFoundException e) {
            // TODO Auto-generated catch block
            Log.i(TAG,"Setting Not Found Exception");
        }
        */
        if(sCurrentHomeWallpaper == -1 && sCurrentLiveWallpaper != -1){
            mWallpaperDrawable = getResources().getDrawable(R.drawable.live_wallpaper_thumb);
        }else{
            mWallpaperDrawable = mWallpaperManager.getDrawable();
        }
        /* bug 185772, liyichong.wt, ADD, 20160620 end*/
        /* bug 155015, liyichong.wt, MODIFY, 20160405 start*/
        //mLockScreenWallpaper = new BitmapDrawable(mWallpaperManager.getLockscreenBitmap());
        /* other_bugs, liyichong.wt, MODIFY, 20160413 start*/
        /* bug 211238, liyichong.wt, MODIFY, 20160823 start*/
        /**
        try {
            sRandomLockscreenServer = Settings.System.getInt(getContentResolver(), "random_lockscreen");
        } catch (SettingNotFoundException e) {
            // TODO Auto-generated catch block
            Log.i(TAG,"Setting Not Found Exception");
        }
        */
        if(mDefaultLockScreenWallpaper.exists() && sRandomLockscreenServer == 0){
            lock_screen_wallpaper.setBackgroundDrawable(mLockScreenWallpaper);
        }else if(sRandomLockscreenServer == 1){
            if(sCurrentLockscreenWallpaper >= 0 && sCurrentLockscreenWallpaper <= (mImages.size() - 1)){
                lock_screen_wallpaper.setBackgroundResource(mImages.get(sCurrentLockscreenWallpaper));
            }else{
                lock_screen_wallpaper.setBackgroundDrawable(mLockScreenWallpaper);
            }
        }
        /* bug 211238, liyichong.wt, MODIFY, 20160823 end*/
        /* other_bugs, liyichong.wt, MODIFY, 20160413 end*/
        /* bug 155015, liyichong.wt, MODIFY, 20160405 start*/
        home_wallpaper.setBackgroundDrawable(mWallpaperDrawable);
    }

    protected void launchChangeService() {
        // TODO Auto-generated method stub
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP
                        , 0, alarmManager.INTERVAL_HOUR, pi);
        /* bug 166387, libaoyin.wt, ADD, 20160426 */
        Toast.makeText(CmccN2PreferenceWallpaper.this, R.string.wallpaper_time_change,
                    Toast.LENGTH_SHORT).show();
    }

    /* bug 211238, liyichong.wt, MODIFY, 20160823 start */
    private void findWallpapersThumb() {
        mImages = new ArrayList<Integer>();

        resources = getResources();
        final String packageName = resources.getResourcePackageName(R.array.lockscreen_wallpapers);

        addWallpapers(resources, packageName, R.array.lockscreen_wallpapers);
    }

    private void addWallpapers(Resources resources, String packageName, int list) {
        final String[] extras = resources.getStringArray(list);
        for (String extra : extras) {
            int res = resources.getIdentifier(extra + "_small", "drawable", packageName);
            if (res != 0) {
                mImages.add(res);
            }
        }
    }
    /* bug 211238, liyichong.wt, MODIFY, 20160823 end */
}
