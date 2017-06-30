package com.android.launcher3;

import java.lang.reflect.Field;
import android.app.ActivityGroup;
import android.content.ComponentName;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;
import android.widget.TabHost.OnTabChangeListener;

/*******************************************************************************************************
|   when     |      who     |    keyword       |        why        |     what                          |
********************************************************************************************************
20160325     liyichong.wt   bug 155067          requirements        add class
20160405     liyichong.wt   bug 155015          requirements        add lockscreen modify
20160405     liyichong.wt   other               requirements        modify code format
20160422     liyichong.wt   bug 155015          requirements        add more wallpaper settings
20160505     liyichong.wt   bug 169427          requirements        modify enter gallary path error
20160511     liyichong.wt   bug 175381          requirements        add my pictures
20160718     liyichong.wt   bug 198702              bugs            modify tabhost style
20160719     liyichong.wt   bug 198351              bugs            modify tabhost style & wallpaper layout
20160806     liyichong.wt   bug 206702              bugs            modify tabhost style
******************************************************************************************************/
@SuppressWarnings("deprecation")
public class CmccN2HomeWallpaperTabhost extends ActivityGroup {

    static final String TAG = "CmccN2HomeWallpaperTabhost";
    /* bug 155015, liyichong.wt, ADD, 20160405 start*/
    public static final String EXTRA_WALLPAPER_TARGET = "wallpaper_target";
    public static final String TARGET_LOCKSCREEN = "lockscreen";
    protected static boolean sForLockscreen;
    /* bug 155015, liyichong.wt, ADD, 20160405 end*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);

        /* bug 155015, liyichong.wt, ADD, 20160405 start*/
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(EXTRA_WALLPAPER_TARGET)) {
            sForLockscreen = intent.getExtras().getString(EXTRA_WALLPAPER_TARGET).equals(TARGET_LOCKSCREEN);
        } else {
            sForLockscreen = false;
        }
        /* bug 155015, liyichong.wt, ADD, 20160405 end*/

        setContentView(R.layout.home_wallpaper_tabhost);
        TabHost hwTabhost = (TabHost) findViewById(R.id.tabhost);
        TextView mypictures = (TextView) findViewById(R.id.mypictures);//bug 175381, liyichong.wt, ADD, 20160511
        hwTabhost.setup();
        hwTabhost.setup(this.getLocalActivityManager());

        LayoutInflater i = LayoutInflater.from(this);
        i.inflate(R.layout.home_wallpaper, hwTabhost.getTabContentView());
        i.inflate(R.layout.live_wallpaper, hwTabhost.getTabContentView());
        final Resources res = getResources();

        /* bug 155015, liyichong.wt, ADD, 20160405 start*/
        final Intent lockWallpaper = new Intent(Intent.ACTION_SET_WALLPAPER);
        lockWallpaper.putExtra(WallpaperPickerGridViewActivity.EXTRA_WALLPAPER_TARGET,
                WallpaperPickerGridViewActivity.TARGET_LOCKSCREEN);
        lockWallpaper.setClassName("com.android.launcher3", "com.android.launcher3.WallpaperPickerGridViewActivity");
        /* bug 155015, liyichong.wt, ADD, 20160405 end*/

        Intent pickWallpaper = new Intent(Intent.ACTION_SET_WALLPAPER);
        pickWallpaper.setClassName("com.android.launcher3", "com.android.launcher3.WallpaperPickerGridViewActivity");

        Intent livePickWallpaper = new Intent(Intent.ACTION_VIEW);
        livePickWallpaper.setClassName("com.android.wallpaper.livepicker", "com.android.wallpaper.livepicker.LiveWallpaperActivity");

        /* bug 175381, liyichong.wt, DELETE, 20160511 start*/
        //Intent moreWallpaper = new Intent(Intent.ACTION_VIEW);
        //moreWallpaper.setClassName("com.android.launcher3", "com.android.launcher3.CmccN2WallpaperTypeSettings");
        /* bug 175381, liyichong.wt, DELETE, 20160511 end*/

        /* bug 155015, liyichong.wt, ADD, 20160405 start*/
        /* bug 198351, liyichong.wt, ADD, 20160719 start*/
        TabWidget tabWidget;

        Class<?> c = null;
        Object obj = null;
        Field field = null;
        int x,tabHostWidthGap = 0;
        try {
            c = Class.forName("com.android.internal.R$dimen");
            obj = c.newInstance();
            field = c.getField("status_bar_height");
            x = Integer.parseInt(field.get(obj).toString());
            tabHostWidthGap = getResources().getDimensionPixelSize(x);
        } catch (Exception e) {
            Log.i(TAG,"could not get tabHostWidthGap");
        }

        if(sForLockscreen){
            hwTabhost.addTab(hwTabhost.newTabSpec("lock_screen_wallpaper").setIndicator(res.getString(R.string.lock_screen_wallpaper)).setContent(lockWallpaper));
            //moreWallpaper.putExtra(EXTRA_WALLPAPER_TARGET,TARGET_LOCKSCREEN);//bug 175381, liyichong.wt, DELETE, 20160511
            /* bug 198702, liyichong.wt, ADD, 20160718 start*/
            tabWidget = hwTabhost.getTabWidget();
            for (int j = 0; j < tabWidget.getChildCount(); j++) {
                View view = tabWidget.getChildAt(j);
                view.setBackgroundResource(R.drawable.tab_host_selector_lockscreen);//bug 206702, liyichong.wt, MODIFY, 20160806
                view.setClickable(false);
                TextView tabHostTextView = (TextView) view.findViewById(android.R.id.title);
                tabHostTextView.getLayoutParams().width = getWindowManager().getDefaultDisplay().getWidth() - tabHostWidthGap * 2;
                tabHostTextView.setTextSize(14);
                tabHostTextView.setGravity(Gravity.CENTER);
            }
            /* bug 198702, liyichong.wt, ADD, 20160718 end*/
        }else{
            hwTabhost.addTab(hwTabhost.newTabSpec("home_wallpaper").setIndicator(res.getString(R.string.home_wallpaper)).setContent(pickWallpaper));
            hwTabhost.addTab(hwTabhost.newTabSpec("live_wallpaper").setIndicator(res.getString(R.string.live_wallpaper)).setContent(livePickWallpaper));
            tabWidget = hwTabhost.getTabWidget();
            for (int j = 0; j < tabWidget.getChildCount(); j++) {
                View view = tabWidget.getChildAt(j);
                view.setBackgroundResource(R.drawable.tab_host_selector_home);//bug 206702, liyichong.wt, ADD, 20160806
                TextView tabHostTextView = (TextView) view.findViewById(android.R.id.title);
                tabHostTextView.getLayoutParams().width = (getWindowManager().getDefaultDisplay().getWidth() - tabHostWidthGap * tabWidget.getChildCount() * 2) / tabWidget.getChildCount();
                tabHostTextView.setTextSize(14);
                tabHostTextView.setGravity(Gravity.CENTER);
            }
        }
        /* bug 198351, liyichong.wt, ADD, 20160719 end*/
        //hwTabhost.addTab(hwTabhost.newTabSpec("more_wallpaper").setIndicator(res.getString(R.string.more_wallpaper)).setContent(moreWallpaper));//bug 175381, liyichong.wt, DELETE, 20160511
        /* bug 155015, liyichong.wt, ADD, 20160405 end*/

        hwTabhost.setOnTabChangedListener(new OnTabChangeListener() {

            @Override
            public void onTabChanged(String arg0) {
                // TODO Auto-generated method stub
                if(arg0.equals(res.getString(R.string.home_wallpaper))){
                }
            }
        });
        /* bug 175381, liyichong.wt, ADD, 20160511 start*/
        mypictures.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                // TODO Auto-generated method stub
                Intent prefIntent = new Intent(Intent.ACTION_SET_WALLPAPER);
                prefIntent.setComponent(new ComponentName("com.android.gallery3d", "com.android.gallery3d.app.Wallpaper"));
                if(sForLockscreen){
                    prefIntent.putExtra("from",TARGET_LOCKSCREEN);
                }
                startActivity(prefIntent);
            }
        });
        /* bug 175381, liyichong.wt, ADD, 20160511 end*/
    }

    protected ComponentName getWallpaperPickerComponent() {
        return new ComponentName(getPackageName(), LauncherWallpaperPickerActivity.class.getName());
    }

}
