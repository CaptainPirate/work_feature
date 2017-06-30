package com.android.launcher3;

import java.util.ArrayList;

import com.android.launcher3.WallpaperPickerActivity.WallpaperTileInfo;
import com.android.launcher3.WallpaperPickerActivity.ZeroPaddingDrawable;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.LayoutInflater.Filter;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.AdapterView.OnItemClickListener;
/*******************************************************************************************************
|   when     |      who     |    keyword       |        why        |     what                          |
********************************************************************************************************
20160325     liyichong.wt   bug 155067          requirements        add class
20160405     liyichong.wt   bug 155015          requirements        add lockscreen modify
20160405     liyichong.wt   other               requirements        modify code format
20160428     liyichong.wt   bug 166538          requirements        add wallpaper selector
20160510     liyichong.wt   bug 174763          bugs                modify wallpaper selector bugs
******************************************************************************************************/
public class WallpaperPickerGridViewActivity extends Activity {

    static final String TAG = "Launcher.WallpaperPickerGridViewActivity";
    private GridView mWallpapersGridView;
    private int mGridImageViewWidth;
    private int mGridImageViewHeight;
    private int mGridImageViewNumLine = 3;
    private int mGridImageViewhorizontalSpacing = 70;
    private double mDefaultDisplayWHRatio = 1.64;

    // bug 197684, gaobin.wt, add, start
    private String theme_name = "";// after picker Theme,this is pickering-theme name
    // bug 203299, gaobin.wt, modify, start
    private static final int THEME_DEFAULT_POSITION = -101;
    private static final int THEME_USINESS_POSITION = -102;
    private static final int THEME_SWEET_POSITION = -103;
    // bug 203299, gaobin.wt, modify, end
    private static final String THEME_DEFAULT_WALLPAPER = "lanucher_01_default";
    private static final String THEME_BUSINESS_WALLPAPER = "launcher_33_business";
    private static final String THEME_SWEET_WALLPAPER = "launcher_36_sweet";
    private int WALLPAPER_POSTION = 0;
    private int THEME_WALLPAPER_POSTION = 0;
    // bug 197684, gaobin.wt, add, end

    /* bug 155015, liyichong.wt, ADD, 20160405 start*/
    public static final String EXTRA_WALLPAPER_TARGET = "wallpaper_target";
    public static final String TARGET_LOCKSCREEN = "lockscreen";
    protected static boolean sForLockscreen;
    /* bug 174763, liyichong.wt, ADD, 20160510 start*/
    private IntentFilter mFilter;
    private static final String WALLPAPER_ALREADY = "WALLPAPER_ALREADY";
    /* bug 174763, liyichong.wt, ADD, 20160510 end*/
    /* bug 155015, liyichong.wt, ADD, 20160405 end*/

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // TODO Auto-generated method stub

        /* bug 155015, liyichong.wt, ADD, 20160405 start*/
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(EXTRA_WALLPAPER_TARGET)) {
            sForLockscreen = intent.getExtras().getString(EXTRA_WALLPAPER_TARGET).equals(TARGET_LOCKSCREEN);
        } else {
            sForLockscreen = false;
        }
        /* bug 155015, liyichong.wt, ADD, 20160405 end*/

        /* bug 174763, liyichong.wt, ADD, 20160510 start*/
        mFilter = new IntentFilter();
        mFilter.addAction(WALLPAPER_ALREADY);
        registerReceiver(mWallpaperReceiver, mFilter);
        /* bug 174763, liyichong.wt, ADD, 20160510 end*/

        setContentView(R.layout.wallpaper_picker_gridview);

        mGridImageViewWidth = (getWindowManager().getDefaultDisplay().getWidth() -mGridImageViewhorizontalSpacing) / mGridImageViewNumLine;
        mGridImageViewHeight = (int) (mGridImageViewWidth * mDefaultDisplayWHRatio);

        // Populate the built-in wallpapers
        ArrayList<WallpaperTileInfo> wallpapers = findBundledWallpapers();
        mWallpapersGridView = (GridView) findViewById(R.id.GridView);
        SimpleWallpapersAdapter ia = new SimpleWallpapersAdapter(this, wallpapers);
        mWallpapersGridView.setAdapter(new wallpaperAdapter(this));
        //bug 166538, liyichong.wt, DELETE, 20160428
        //populateWallpapersFromAdapter(mWallpapersGridView, ia, false);
    }

    /* bug 174763, liyichong.wt, ADD, 20160510 start*/
    BroadcastReceiver mWallpaperReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub
            String action = intent.getAction();
            if(action.equalsIgnoreCase(WALLPAPER_ALREADY)){
                unregisterReceiver(mWallpaperReceiver);
                finish();
            }
        }};
    /* bug 174763, liyichong.wt, ADD, 20160510 end*/

    private ArrayList<WallpaperTileInfo> findBundledWallpapers() {
        //final PackageManager pm = getPackageManager();
        final ArrayList<WallpaperTileInfo> bundled = new ArrayList<WallpaperTileInfo>(24);

        Pair<ApplicationInfo, Integer> r = getWallpaperArrayResourceId();
        if (r != null) {
            try {
                Resources wallpaperRes = getPackageManager().getResourcesForApplication(r.first);
                addWallpapers(bundled, wallpaperRes, r.first.packageName, r.second);
            } catch (PackageManager.NameNotFoundException e) {
            }
        }

        return bundled;
    }

    private void addWallpapers(ArrayList<WallpaperTileInfo> known, Resources res,
            String packageName, int listResId) {
        final String[] extras = res.getStringArray(listResId);
        // bug 197684, gaobin.wt, add, start
        WALLPAPER_POSTION = 0;
        /**
        try {
            THEME_WALLPAPER_POSTION = Settings.System.getInt(
                    getContentResolver(),
                    Settings.System.LAUNCHER_HOME_WALLPAPER);
        } catch (SettingNotFoundException e) {
            Log.i(TAG, "Setting Not Found Exception");
        }
        */
        // bug 197684, gaobin.wt, add, end
        for (String extra : extras) {
            // bug 197684, gaobin.wt, add, start
            /**
            if (THEME_WALLPAPER_POSTION < 0) {
                if (THEME_WALLPAPER_POSTION == THEME_DEFAULT_POSITION
                        && extra.equals(THEME_DEFAULT_WALLPAPER)) {
                    Settings.System.putInt(getContentResolver(),
                            Settings.System.LAUNCHER_HOME_WALLPAPER,
                            WALLPAPER_POSTION);
                } else if (THEME_WALLPAPER_POSTION == THEME_USINESS_POSITION
                        && extra.equals(THEME_BUSINESS_WALLPAPER)) {
                    Settings.System.putInt(getContentResolver(),
                            Settings.System.LAUNCHER_HOME_WALLPAPER,
                            WALLPAPER_POSTION);
                } else if (THEME_WALLPAPER_POSTION == THEME_SWEET_POSITION
                        && extra.equals(THEME_SWEET_WALLPAPER)) {
                    Settings.System.putInt(getContentResolver(),
                            Settings.System.LAUNCHER_HOME_WALLPAPER,
                            WALLPAPER_POSTION);
                }
            }
            */
            // bug 197684, gaobin.wt, add, end
            int resId = res.getIdentifier(extra, "drawable", packageName);
            if (resId != 0) {
                final int thumbRes = res.getIdentifier(extra + "_small", "drawable", packageName);

                if (thumbRes != 0) {
                    WallpaperPickerActivity.ResourceWallpaperInfo wallpaperInfo =
                            new WallpaperPickerActivity.ResourceWallpaperInfo(res, resId, res.getDrawable(thumbRes));
                    known.add(wallpaperInfo);
                    // Log.d(TAG, "add: [" + packageName + "]: " + extra + " (" + res + ")");
                }
            } else {
                Log.e(TAG, "Couldn't find wallpaper " + extra);
            }
            // bug 197684, gaobin.wt, add, start
            WALLPAPER_POSTION++;
            // bug 197684, gaobin.wt, add, end
        }
    }

    class wallpaperAdapter extends BaseAdapter{
        ArrayList<WallpaperTileInfo> wallpapers = findBundledWallpapers();
        View view;
        LayoutInflater i;

        wallpaperAdapter(Context context){
            i=LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            // TODO Auto-generated method stub
            return wallpapers.size();
        }

        @Override
        public Object getItem(int arg0) {
            // TODO Auto-generated method stub
            return wallpapers.get(arg0);
        }

        @Override
        public long getItemId(int arg0) {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public View getView(int arg0, View arg1, ViewGroup arg2) {
            /* bug 166538, liyichong.wt, MODIFY, 20160428 start*/
            // TODO Auto-generated method stub
            view = arg1;
            ViewHolder holder;
            final int position = arg0;
            if (view == null) {
                holder = new ViewHolder();
                view = i.inflate(R.layout.wallpaper_picker_item, null);
                setWallpaperItemPaddingToZero((FrameLayout) view);
                holder.imageView = (ImageView) view.findViewById(R.id.wallpaper_image);
                holder.imageSelector = (ImageView) view.findViewById(R.id.wallpaper_selector);
                //imageView.setLayoutParams(new GridView.LayoutParams(mGridImageViewWidth, mGridImageViewHeight));

                LayoutParams lp = (LayoutParams) holder.imageView.getLayoutParams();
                lp.width = mGridImageViewWidth;
                lp.height = mGridImageViewHeight;
                holder.imageView.requestLayout();

                holder.imageView.setAdjustViewBounds(false);
                holder.imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

                //imageView.setPadding(8, 8, 8, 8);
                view.setTag(holder);
            }
            else {
                holder = (ViewHolder) view.getTag();
            }
            holder.imageView.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    // TODO Auto-generated method stub
                    WallpaperTileInfo info = (WallpaperTileInfo) getItem(position);
                    v.setTag(info);
                    info.setView(v);
                    Intent intent = new Intent(Intent.ACTION_SET_WALLPAPER);
                    intent.setClassName("com.android.launcher3", "com.android.launcher3.WallpaperPickerActivity");
                    intent.putExtra("wallpaperinfo", position);
                    if(sForLockscreen){
                        intent.putExtra(WallpaperPickerGridViewActivity.EXTRA_WALLPAPER_TARGET,
                                WallpaperPickerGridViewActivity.TARGET_LOCKSCREEN);
                    }
                    startActivity(intent);
                }
            });
            WallpaperTileInfo thumb = (WallpaperTileInfo) getItem(arg0);
            holder.imageView.setImageDrawable(thumb.mThumb);
            if(isSelectWallpaper() == arg0){
                holder.imageSelector.setVisibility(View.VISIBLE);
            }else{
                holder.imageSelector.setVisibility(View.INVISIBLE);
            }
            return view;
            /* bug 166538, liyichong.wt, MODIFY, 20160428 end*/
        }
    }

    /* bug 166538, liyichong.wt, ADD, 20160428 start*/
    private int isSelectWallpaper(){
        int wallpaper_selector_index = -1;
        /**
        try {
            if(sForLockscreen){
                wallpaper_selector_index = Settings.System.getInt(getContentResolver(), Settings.System.LAUNCHER_LOCKSCREEN_WALLPAPER);
            }else{
                wallpaper_selector_index = Settings.System.getInt(getContentResolver(), Settings.System.LAUNCHER_HOME_WALLPAPER);
            }
        } catch (SettingNotFoundException e) {
            // TODO Auto-generated catch block
            Log.i(TAG,"Setting Not Found Exception");
        }
        */
        return wallpaper_selector_index;
    }
    /* bug 166538, liyichong.wt, ADD, 20160428 end*/

    static void setWallpaperItemPaddingToZero(FrameLayout frameLayout) {
        frameLayout.setPadding(0, 0, 0, 0);
        frameLayout.setForeground(new ZeroPaddingDrawable(frameLayout.getForeground()));
    }

    public Pair<ApplicationInfo, Integer> getWallpaperArrayResourceId() {
        // Context.getPackageName() may return the "original" package name,
        // com.android.launcher3; Resources needs the real package name,
        // com.android.launcher3. So we ask Resources for what it thinks the
        // package name should be.
        int arrayId = R.array.wallpapers;
        if (sForLockscreen) {//bug 155015, liyichong.wt, MODIFY, 20160405
            arrayId = R.array.lockscreen_wallpapers;
        }
        final String packageName = getResources().getResourcePackageName(arrayId);
        try {
            ApplicationInfo info = getPackageManager().getApplicationInfo(packageName, 0);
            return new Pair<ApplicationInfo, Integer>(info, arrayId);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    private static class SimpleWallpapersAdapter extends ArrayAdapter<WallpaperTileInfo> {
        private final LayoutInflater mLayoutInflater;

        SimpleWallpapersAdapter(Activity activity, ArrayList<WallpaperTileInfo> wallpapers) {
            super(activity, R.layout.wallpaper_picker_item, wallpapers);
            mLayoutInflater = activity.getLayoutInflater();
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            Drawable thumb = getItem(position).mThumb;
            if (thumb == null) {
                Log.e(TAG, "Error decoding thumbnail for wallpaper #" + position);
            }
            return createImageTileView(mLayoutInflater, convertView, parent, thumb);
        }
    }

    public static View createImageTileView(LayoutInflater layoutInflater,
            View convertView, ViewGroup parent, Drawable thumb) {
        View view;

        int layout_id = R.layout.wallpaper_picker_item;
        if (sForLockscreen) {//bug 155015, liyichong.wt, MODIFY, 20160405
            layout_id = R.layout.lockscreen_wallpaper_picker_item;
        }
        if (convertView == null) {
            view = layoutInflater.inflate(layout_id, parent, false);
        } else {
            view = convertView;
        }

        setWallpaperItemPaddingToZero((FrameLayout) view);

        ImageView image = (ImageView) view.findViewById(R.id.wallpaper_image);

        if (thumb != null) {
            image.setImageDrawable(thumb);
            thumb.setDither(true);
        }

        return view;
    }

    private void populateWallpapersFromAdapter(ViewGroup parent, final BaseAdapter adapter,
            boolean addLongPressHandler) {
        
        mWallpapersGridView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1,
                    int arg2, long arg3) {
                // TODO Auto-generated method stub
                WallpaperTileInfo info = (WallpaperTileInfo) adapter.getItem(arg2);
                arg1.setTag(info);
                info.setView(arg1);
                Intent intent = new Intent(Intent.ACTION_SET_WALLPAPER);
                intent.setClassName("com.android.launcher3", "com.android.launcher3.WallpaperPickerActivity");
                intent.putExtra("wallpaperinfo", arg2);
                /* bug 155015, liyichong.wt, ADD, 20160405 start*/
                if(sForLockscreen){
                    intent.putExtra(WallpaperPickerGridViewActivity.EXTRA_WALLPAPER_TARGET,
                            WallpaperPickerGridViewActivity.TARGET_LOCKSCREEN);
                }
                /* bug 155015, liyichong.wt, ADD, 20160405 start*/
                startActivity(intent);
                //info.onClick(WallpaperPickerActivity.this);
            }
        });
    }

    /* bug 166538, liyichong.wt, ADD, 20160428 start*/
    class ViewHolder {
        ImageView imageView;
        ImageView imageSelector;
    }
    /* bug 166538, liyichong.wt, ADD, 20160428 end*/
}
