/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.launcher3;

import android.animation.LayoutTransition;
import android.app.Activity;
import android.app.WallpaperInfo;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Camera;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LevelListDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.provider.Settings.System;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLayoutChangeListener;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Transformation;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.FrameLayout.LayoutParams;

import com.android.photos.BitmapRegionTileSource;
import com.android.photos.BitmapRegionTileSource.BitmapSource;

import java.io.File;
import java.util.ArrayList;

/*******************************************************************************************************
|   when     |      who     |    keyword       |        why        |     what                          |
********************************************************************************************************
20160325     liyichong.wt   bug 155067          requirements        add class
20160405     liyichong.wt   bug 155015          requirements        add lockscreen modify
20160405     liyichong.wt   other               requirements        modify code format
20160407     liyichong.wt   bug 155069          requirements        modify layout
20160412     liyichong.wt   bug 155069          requirements        modify layout
20160425     liyichong.wt   other               other               delete invalid log
20160428     liyichong.wt   bug 166538          requirements        add wallpaper selector
20160510     liyichong.wt   bug 174763          bugs                modify wallpaper selector bugs
20160627     liyichong.wt   bug 190930          bugs                modify launcher fatal when click wallpaper
******************************************************************************************************/
public class WallpaperPickerActivity extends WallpaperCropActivity {
    static final String TAG = "Launcher.WallpaperPickerActivity";

    public static final int IMAGE_PICK = 5;
    public static final int PICK_WALLPAPER_THIRD_PARTY_ACTIVITY = 6;
    public static final int PICK_LIVE_WALLPAPER = 7;
    private static final String TEMP_WALLPAPER_TILES = "TEMP_WALLPAPER_TILES";
    private static final String SELECTED_INDEX = "SELECTED_INDEX";
    private static final String WALLPAPER_ALREADY = "WALLPAPER_ALREADY";//bug 174763, liyichong.wt, ADD, 20160510
    private static final int FLAG_POST_DELAY_MILLIS = 200;

    private View mSelectedTile;
    private boolean mIgnoreNextTap;

    private GridView mWallpapersGridView;

    private Button mSetWallpaperButton;
    private Button mCancelWallpaperButton;
    private int mBtnHeight;
    private int mBtnMargin;

    private Button mOneScreenButton;
    private Button mTwoScreenButton;
    /* bug 155069, liyichong.wt, ADD, 20160412 start*/
    private ImageView mGeometryShadowTop;
    private ImageView mGeometryShadowBottom;
    /* bug 155069, liyichong.wt, ADD, 20160412 end*/

    ArrayList<Uri> mTempWallpaperTiles = new ArrayList<Uri>();
    private WallpaperInfo mLiveWallpaperInfoOnPickerLaunch;
    private int mSelectedIndex = -1;
    private WallpaperInfo mLastClickedLiveWallpaperInfo;
    private WallpaperTileInfo mInfo;
    private int mWallInfo;//bug 166538, liyichong.wt, ADD, 20160428
    private static BitmapRegionTileSource.ResourceBitmapSource mBitmapSource;//bug 190930, liyichong.wt, ADD, 20160627

    public static abstract class WallpaperTileInfo {
        protected View mView;
        public Drawable mThumb;

        public void setView(View v) {
            mView = v;
        }
        public void onClick(WallpaperPickerActivity a) {}
        public void onSave(WallpaperPickerActivity a) {}
        public void onDelete(WallpaperPickerActivity a) {}
        public boolean isSelectable() { return false; }
        public boolean isNamelessWallpaper() { return false; }
        public void onIndexUpdated(CharSequence label) {
            if (isNamelessWallpaper()) {
                mView.setContentDescription(label);
            }
        }
    }

    public static class ResourceWallpaperInfo extends WallpaperTileInfo {
        private Resources mResources;
        private int mResId;

        public ResourceWallpaperInfo(Resources res, int resId, Drawable thumb) {
            mResources = res;
            mResId = resId;
            mThumb = thumb;
        }
        @Override
        public void onClick(WallpaperPickerActivity a) {
            /* bug 190930, liyichong.wt, MODIFY, 20160627 start*/
            mBitmapSource =
                    new BitmapRegionTileSource.ResourceBitmapSource(
                            mResources, mResId, BitmapRegionTileSource.MAX_PREVIEW_SIZE);
            mBitmapSource.loadInBackground();
            BitmapRegionTileSource source = new BitmapRegionTileSource(a, mBitmapSource);
            /* bug 190930, liyichong.wt, MODIFY, 20160627 end*/
            CropView v = a.getCropView();
            v.setTileSource(source, null);
            Point wallpaperSize = WallpaperCropActivity.getDefaultWallpaperSize(
                    a.getResources(), a.getWindowManager(), true);
            RectF crop = WallpaperCropActivity.getMaxCropRect(
                    source.getImageWidth(), source.getImageHeight(),
                    wallpaperSize.x, wallpaperSize.y, false);
            v.setScale(wallpaperSize.x / crop.width());
            v.setTouchEnabled(false);
            a.setSystemWallpaperVisiblity(false);
        }
        @Override
        public void onSave(WallpaperPickerActivity a) {
            boolean finishActivityWhenDone = true;
            a.cropImageAndSetWallpaper(mResources, mResId, finishActivityWhenDone, true);
        }
        @Override
        public boolean isSelectable() {
            return true;
        }
        @Override
        public boolean isNamelessWallpaper() {
            return true;
        }
    }

    /**
     * shows the system wallpaper behind the window and hides the {@link
     * #mCropView} if visible
     * @param visible should the system wallpaper be shown
     */
    protected void setSystemWallpaperVisiblity(final boolean visible) {
        // hide our own wallpaper preview if necessary
        if(!visible) {
            mCropView.setVisibility(View.VISIBLE);
        } else {
            changeWallpaperFlags(visible);
        }
        // the change of the flag must be delayed in order to avoid flickering,
        // a simple post / double post does not suffice here
        mCropView.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(!visible) {
                    changeWallpaperFlags(visible);
                } else {
                    mCropView.setVisibility(View.INVISIBLE);
                }
            }
        }, FLAG_POST_DELAY_MILLIS);
    }

    private void changeWallpaperFlags(boolean visible) {
        int desiredWallpaperFlag = visible ? WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER : 0;
        int currentWallpaperFlag = getWindow().getAttributes().flags
                & WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER;
        if (desiredWallpaperFlag != currentWallpaperFlag) {
            getWindow().setFlags(desiredWallpaperFlag,
                    WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER);
        }
    }

    @Override
    public void setCropViewTileSource(BitmapSource bitmapSource,
                                      boolean touchEnabled,
                                      boolean moveToLeft,
                                      final Runnable postExecute) {
        // we also want to show our own wallpaper instead of the one in the background
        Runnable showPostExecuteRunnable = new Runnable() {
            @Override
            public void run() {
                if(postExecute != null) {
                    postExecute.run();
                }
                setSystemWallpaperVisiblity(false);
            }
        };
        super.setCropViewTileSource(bitmapSource,
                touchEnabled,
                moveToLeft,
                showPostExecuteRunnable);
    }

    // called by onCreate; this is subclassed to overwrite WallpaperCropActivity
    protected void init() {
        setContentView(R.layout.wallpaper_picker);
        Resources res = getResources();
        mBtnHeight = res.getDimensionPixelSize(R.dimen.btn_height);
        mBtnMargin = res.getDimensionPixelSize(R.dimen.btn_margin);

        mCropView = (CropView) findViewById(R.id.cropView);
        mCropView.setVisibility(View.INVISIBLE);

            ArrayList<WallpaperTileInfo> wallpapers = findBundledWallpapers();
            SimpleWallpapersAdapter ia = new SimpleWallpapersAdapter(this, wallpapers);
            /* bug 166538, liyichong.wt, MODIFY, 20160428 start*/
            mWallInfo = getIntent().getIntExtra("wallpaperinfo", -1);
            mInfo = ia.getItem(mWallInfo);
            /* bug 166538, liyichong.wt, MODIFY, 20160428 end*/
            mInfo.onClick(WallpaperPickerActivity.this);

        // Select the first item; wait for a layout pass so that we initialize the dimensions of
        // cropView or the defaultWallpaperView first
        mCropView.addOnLayoutChangeListener(new OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom,
                    int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if ((right - left) > 0 && (bottom - top) > 0) {
                    if (mSelectedIndex >= 0) {
                        setSystemWallpaperVisiblity(false);
                    }
                    v.removeOnLayoutChangeListener(this);
                }
            }
        });



        // Create smooth layout transitions for when items are deleted
        final LayoutTransition transitioner = new LayoutTransition();
        transitioner.setDuration(200);
        transitioner.setStartDelay(LayoutTransition.CHANGE_DISAPPEARING, 0);
        transitioner.setAnimator(LayoutTransition.DISAPPEARING, null);

        mSetWallpaperButton = (Button) findViewById(R.id.fbtn_ok);
        mSetWallpaperButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /* bug 166538, liyichong.wt, ADD, 20160428 start*/
                if(sForLockscreen){
                    //Settings.System.putInt(getContentResolver(), Settings.System.LAUNCHER_LOCKSCREEN_WALLPAPER, mWallInfo);
                }else{
                    //Settings.System.putInt(getContentResolver(), Settings.System.LAUNCHER_HOME_WALLPAPER, mWallInfo);
                }
                /* bug 174763, liyichong.wt, ADD, 20160510 start*/
                Settings.System.putInt(getContentResolver(), "launcher_live_wallpaper", -1);
                Intent wallpaperIntent = new Intent(WALLPAPER_ALREADY);
                sendBroadcast(wallpaperIntent);
                /* bug 166538, liyichong.wt, ADD, 20160428 end*/
                //if (mSelectedTile != null) {
                //  WallpaperTileInfo info = (WallpaperTileInfo) mSelectedTile.getTag();
                    mInfo.onSave(WallpaperPickerActivity.this);
                //} else {
                //  no tile was selected, so we just finish the activity and go back
                //    setResult(Activity.RESULT_OK);
                //    finish();
                //}
                /* bug 174763, liyichong.wt, ADD, 20160510 end*/
            }
        });

        mCancelWallpaperButton = (Button) findViewById(R.id.fbtn_cancel);
        mCancelWallpaperButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCancelWallpaperButton.setEnabled(false);
                onBackPressed();
            }
        });

        mOneScreenButton = (Button)findViewById(R.id.crop_one_screen);
        mTwoScreenButton = (Button)findViewById(R.id.crop_two_screen);

        /* bug 155069, liyichong.wt, ADD, 20160412 start*/
        mGeometryShadowTop = (ImageView) findViewById(R.id.geometry_shadow_top);
        mGeometryShadowBottom = (ImageView) findViewById(R.id.geometry_shadow_bottom);

        mOneScreenButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                // TODO Auto-generated method stub
                mOneScreenButton.setTextColor(getResources().getColor(R.color.crop_title_color_press));
                mTwoScreenButton.setTextColor(getResources().getColor(R.color.crop_title_color));
                mOneScreenButton.setCompoundDrawablesRelativeWithIntrinsicBounds(0, R.drawable.one_screen_press, 0, 0);
                mTwoScreenButton.setCompoundDrawablesRelativeWithIntrinsicBounds(0, R.drawable.two_screen, 0, 0);
                mGeometryShadowTop.setVisibility(View.INVISIBLE);
                mGeometryShadowBottom.setVisibility(View.INVISIBLE);
                sForOneScreen = true;
                mInfo.onClick(WallpaperPickerActivity.this);
            }
        });

        mTwoScreenButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                // TODO Auto-generated method stub
                mOneScreenButton.setTextColor(getResources().getColor(R.color.crop_title_color));
                mTwoScreenButton.setTextColor(getResources().getColor(R.color.crop_title_color_press));
                mOneScreenButton.setCompoundDrawablesRelativeWithIntrinsicBounds(0, R.drawable.one_screen, 0, 0);
                mTwoScreenButton.setCompoundDrawablesRelativeWithIntrinsicBounds(0, R.drawable.two_screen_press, 0, 0);
                mGeometryShadowTop.setVisibility(View.VISIBLE);
                mGeometryShadowBottom.setVisibility(View.VISIBLE);
                sForOneScreen = false;
                mCropView.setTouchEnabled(true);
            }
        });
        /* bug 155069, liyichong.wt, ADD, 20160412 end*/

        if (sForLockscreen) {
            mOneScreenButton.setVisibility(View.GONE);
            mTwoScreenButton.setVisibility(View.GONE);
        }

        // if lockscreen load lockscreen wallpaper
        /* bug 155015, liyichong.wt, MODIFY, 20160405 start*/
        if (false) {
        /* bug 155015, liyichong.wt, MODIFY, 20160405 end*/
            BitmapSource bitmapSource = null;
            File lockscreenFile = getLockscreenWallpaperFile();
            if (lockscreenFile != null && lockscreenFile.exists()) {
                bitmapSource = new BitmapRegionTileSource.FilePathBitmapSource(
                        lockscreenFile.getAbsolutePath(), BitmapRegionTileSource.MAX_PREVIEW_SIZE);
            } else {
                Resources sysRes = Resources.getSystem();
                String thumbResStr = "default_lockscreen_wallpaper";
                int thumbResId = sysRes.getIdentifier(thumbResStr, "drawable", "android");
                if (thumbResId != 0) {
                    bitmapSource = new BitmapRegionTileSource.ResourceBitmapSource(sysRes,
                            thumbResId, BitmapRegionTileSource.MAX_PREVIEW_SIZE);
                }
            }
            if (bitmapSource != null) {
                mCropView.setVisibility(View.VISIBLE);
                bitmapSource.loadInBackground();
                BitmapRegionTileSource source = new BitmapRegionTileSource(this, bitmapSource);
                mCropView.setTileSource(source, null);
                mCropView.setTouchEnabled(false);
            }
        }
    }

    static class FlipAnimation extends Animation {

        private final float mFromDegrees;
        private final float mToDegrees;
        private final float mCenterX;
        private final float mCenterY;
        private final float mDepthZ;
        private final boolean mReverse;
        private Camera mCamera;

        public FlipAnimation(float fromDegrees, float toDegrees, float centerX,
                float centerY, float depthZ, boolean reverse) {
            mFromDegrees = fromDegrees;
            mToDegrees = toDegrees;
            mCenterX = centerX;
            mCenterY = centerY;
            mDepthZ = depthZ;
            mReverse = reverse;
            mCamera = new Camera();
        }

        @Override
        protected void applyTransformation(float interpolatedTime,
                Transformation t) {
            final float fromDegrees = mFromDegrees;
            float degrees = fromDegrees
                    + ((mToDegrees - fromDegrees) * interpolatedTime);

            final float centerX = mCenterX;
            final float centerY = mCenterY;
            final Camera camera = mCamera;

            final Matrix matrix = t.getMatrix();
            camera.save();
            if (mReverse) {
                camera.translate(0.0f, 0.0f, mDepthZ * interpolatedTime);
            } else {
                camera.translate(0.0f, 0.0f, mDepthZ * (1.0f - interpolatedTime));
            }
            camera.rotateY(degrees);
            camera.getMatrix(matrix);
            camera.restore();

            matrix.preTranslate(-centerX, -centerY);
            matrix.postTranslate(centerX, centerY);
        }
    }

    private int numCheckedItems() {
        int numCheckedItems = 0;
        return numCheckedItems;
    }

    private void clearSelected() {
        if (mSelectedTile != null) {
            mSelectedTile.setSelected(true);
        }
    }

    @Override
    public void onBackPressed() {
        int numCheckedItems = numCheckedItems();
        if (numCheckedItems > 0) {
            clearSelected();
            return;
        }
        /* bug 190930, liyichong.wt, ADD, 20160627 start*/
        Log.i(TAG,"----Recycled----" + mBitmapSource.mPreview.isRecycled());
        if(!mBitmapSource.mPreview.isRecycled()){
        Log.i(TAG,"----recycling----");
            mBitmapSource.mPreview.recycle();
            java.lang.System.gc();
        }
        /* bug 190930, liyichong.wt, ADD, 20160627 end*/
        super.onBackPressed();
    }

    private void selectTile(View v) {
        if (mSelectedTile != null) {
            mSelectedTile.setSelected(false);
            mSelectedTile = null;
        }
        mSelectedTile = v;
        v.setSelected(true);
        // TODO: Remove this once the accessibility framework and
        // services have better support for selection state.
        v.announceForAccessibility(
                getString(R.string.announce_selection, v.getContentDescription()));
    }


    protected Bitmap getThumbnailOfLastPhoto() {
        Cursor cursor = MediaStore.Images.Media.query(getContentResolver(),
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new String[] { MediaStore.Images.ImageColumns._ID,
                    MediaStore.Images.ImageColumns.DATE_TAKEN},
                null, null, MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC LIMIT 1");

        Bitmap thumb = null;
        if (cursor != null) {
            if (cursor.moveToNext()) {
                int id = cursor.getInt(0);
                thumb = MediaStore.Images.Thumbnails.getThumbnail(getContentResolver(),
                        id, MediaStore.Images.Thumbnails.MINI_KIND, null);
            }
            cursor.close();
        }
        return thumb;
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Following code avoid some bug in FW. If we set view's visibility to INVISIBLE after
        // activity onPause than the view won't show again even if it's alpha is 1.0f and visibility
        // is VISIBLE.
    }

    protected void onStop() {
        super.onStop();
    }

    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelableArrayList(TEMP_WALLPAPER_TILES, mTempWallpaperTiles);
        outState.putInt(SELECTED_INDEX, mSelectedIndex);
    }

    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        ArrayList<Uri> uris = savedInstanceState.getParcelableArrayList(TEMP_WALLPAPER_TILES);
        for (Uri uri : uris) {
            addTemporaryWallpaperTile(uri, true);
        }
        mSelectedIndex = savedInstanceState.getInt(SELECTED_INDEX, -1);
    }

    private void populateWallpapersFromAdapter(ViewGroup parent, final BaseAdapter adapter,
            boolean addLongPressHandler) {
        
        mWallpapersGridView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1,
                    int arg2, long arg3) {
                // TODO Auto-generated method stub
                mWallpapersGridView.setVisibility(View.GONE);
                WallpaperTileInfo info = (WallpaperTileInfo) adapter.getItem(arg2);
                arg1.setTag(info);
                info.setView(arg1);

                mSetWallpaperButton.setEnabled(true);
                WallpaperTileInfo info1 = (WallpaperTileInfo) arg1.getTag();
                if (info1.isSelectable() && arg1.getVisibility() == View.VISIBLE) {
                    selectTile(arg1);
                }
                info.onClick(WallpaperPickerActivity.this);
            }
        });
        
        
        
        
    }

    private static Point getDefaultThumbnailSize(Resources res) {
        if (sForLockscreen) {
            return new Point(res.getDimensionPixelSize(R.dimen.lockscreenWallpaperThumbnailWidth),
                    res.getDimensionPixelSize(R.dimen.lockscreenWallpaperThumbnailHeight));
        } else {
            return new Point(res.getDimensionPixelSize(R.dimen.wallpaperThumbnailWidth),
                    res.getDimensionPixelSize(R.dimen.wallpaperThumbnailHeight));
        }
    }

    private static Bitmap createThumbnail(Point size, Context context, Uri uri, byte[] imageBytes,
            Resources res, int resId, int rotation, boolean leftAligned) {
        int width = size.x;
        int height = size.y;

        BitmapCropTask cropTask;
        if (uri != null) {
            cropTask = new BitmapCropTask(
                    context, uri, null, rotation, width, height, false, true, null);
        } else if (imageBytes != null) {
            cropTask = new BitmapCropTask(
                    imageBytes, null, rotation, width, height, false, true, null);
        }  else {
            cropTask = new BitmapCropTask(
                    context, res, resId, null, rotation, width, height, false, true, null);
        }
        Point bounds = cropTask.getImageBounds();
        if (bounds == null || bounds.x == 0 || bounds.y == 0) {
            return null;
        }

        Matrix rotateMatrix = new Matrix();
        rotateMatrix.setRotate(rotation);
        float[] rotatedBounds = new float[] { bounds.x, bounds.y };
        rotateMatrix.mapPoints(rotatedBounds);
        rotatedBounds[0] = Math.abs(rotatedBounds[0]);
        rotatedBounds[1] = Math.abs(rotatedBounds[1]);

        RectF cropRect = WallpaperCropActivity.getMaxCropRect(
                (int) rotatedBounds[0], (int) rotatedBounds[1], width, height, leftAligned);
        cropTask.setCropBounds(cropRect);

        if (cropTask.cropBitmap()) {
            return cropTask.getCroppedBitmap();
        } else {
            return null;
        }
    }

    private void addTemporaryWallpaperTile(final Uri uri, boolean fromRestore) {
        mTempWallpaperTiles.add(uri);
        // Add a tile for the image picked from Gallery

        // Load the thumbnail
        final Point defaultSize = getDefaultThumbnailSize(this.getResources());
        final Context context = this;
        new AsyncTask<Void, Bitmap, Bitmap>() {
            protected Bitmap doInBackground(Void...args) {
                try {
                    int rotation = WallpaperCropActivity.getRotationFromExif(context, uri);
                    return createThumbnail(defaultSize, context, uri, null, null, 0, rotation, false);
                } catch (SecurityException securityException) {
                    if (isDestroyed()) {
                        // Temporarily granted permissions are revoked when the activity
                        // finishes, potentially resulting in a SecurityException here.
                        // Even though {@link #isDestroyed} might also return true in different
                        // situations where the configuration changes, we are fine with
                        // catching these cases here as well.
                        cancel(false);
                    } else {
                        // otherwise it had a different cause and we throw it further
                        throw securityException;
                    }
                    return null;
                }
            }
        };
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == IMAGE_PICK && resultCode == RESULT_OK) {
            if (data != null && data.getData() != null) {
                Uri uri = data.getData();
                addTemporaryWallpaperTile(uri, false);
            }
        } else if (requestCode == PICK_WALLPAPER_THIRD_PARTY_ACTIVITY) {
            setResult(RESULT_OK);
            finish();
        } else if (requestCode == PICK_LIVE_WALLPAPER) {
            WallpaperManager wm = WallpaperManager.getInstance(this);
            final WallpaperInfo oldLiveWallpaper = mLiveWallpaperInfoOnPickerLaunch;
            final WallpaperInfo clickedWallpaper = mLastClickedLiveWallpaperInfo;
            WallpaperInfo newLiveWallpaper = wm.getWallpaperInfo();
            // Try to figure out if a live wallpaper was set;
            if (newLiveWallpaper != null &&
                    (oldLiveWallpaper == null
                            || !oldLiveWallpaper.getComponent()
                                    .equals(newLiveWallpaper.getComponent())
                            || clickedWallpaper.getComponent()
                                    .equals(oldLiveWallpaper.getComponent()))) {
                // Return if a live wallpaper was set
                setResult(RESULT_OK);
                finish();
            }
        }
    }

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
        if (sForLockscreen) {
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

    public CropView getCropView() {
        return mCropView;
    }

    public void onLiveWallpaperPickerLaunch(WallpaperInfo info) {
        mLastClickedLiveWallpaperInfo = info;
        mLiveWallpaperInfoOnPickerLaunch = WallpaperManager.getInstance(this).getWallpaperInfo();
    }

    static class ZeroPaddingDrawable extends LevelListDrawable {
        public ZeroPaddingDrawable(Drawable d) {
            super();
            addLevel(0, 0, d);
            setLevel(0);
        }

        @Override
        public boolean getPadding(Rect padding) {
            padding.set(0, 0, 0, 0);
            return true;
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
        if (sForLockscreen) {
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

    // In Launcher3, we override this with a method that catches exceptions
    // from starting activities; didn't want to copy and paste code into here
    public void startActivityForResultSafely(Intent intent, int requestCode) {
        startActivityForResult(intent, requestCode);
    }
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
        for (String extra : extras) {
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
        }
    }
}
