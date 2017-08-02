/*
* Copyright (C) 2014,2015 Thundersoft Corporation
* All rights Reserved
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

package com.android.camera;

import android.content.Context;
import android.content.res.Resources;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.LinearLayout.LayoutParams;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Toast;

import com.android.camera.ui.RotateLayout;
import com.android.camera.ui.RotateTextToast;
import com.android.camera.util.CameraUtil;

import org.codeaurora.snapcam.R;

public class TsMakeupManager implements OnSeekBarChangeListener {
    private static final String TAG = "TsMakeupManager";
    private PhotoUI mUI;
    private PhotoMenu mMenu;
    private CameraActivity mActivity;
    private PreferenceGroup mPreferenceGroup;
    private View mTsMakeupSwitcher;
    //add by wangshenxing for camera ui 2017/05/27 start
    private SeekBar seekBarClean;
    private SeekBar seekBarWhiten;
    private SeekBar seekBarFace;
    private SeekBar seekBarEye;
    private ImageView imageViewArrowDown;
    private ImageView imageViewArrowUp;
    //add by wangshenxing for camera ui 2017/05/27 end

    private RelativeLayout mMakeupLayoutRoot;
    //add by wangshenxing for camera ui 2017/05/27 start
    private LinearLayout mMakeupSoftenLinearLayout;
    private LinearLayout mMakeupCleanLayout;
    private LinearLayout mMakeupWhitenLayout;
    private LinearLayout mMakeupFaceLayout;
    private LinearLayout mMakeupEyeLayout;
    //add by wangshenxing for camera ui 2017/05/27 end
    private LinearLayout mMakeupLevelRoot;
    private LinearLayout mMakeupSingleRoot;

    public static final String MAKEUP_ON = "On";
    public static final String MAKEUP_OFF = "Off";
    public static final String MAKEUP_NONE = "none";

    private static final int MODE_NONE = 0;
    private static final int MODE_WHITEN = 1;
    private static final int MODE_CLEAN = 2;
    private int mMode = MODE_NONE;
    private int mSingleSelectedIndex = MODE_NONE;

    private static final int MAKEUP_UI_STATUS_NONE = 0;
    private static final int MAKEUP_UI_STATUS_ON = 1;
    private static final int MAKEUP_UI_STATUS_OFF = 2;
    private static final int MAKEUP_UI_STATUS_DISMISS = 3;
    private int mMakeupUIStatus = MAKEUP_UI_STATUS_NONE;

    private static final int CLICK_THRESHOLD = 200;

    public static final boolean HAS_TS_MAKEUP = android.os.SystemProperties.getBoolean("persist.ts.rtmakeup", true);

    private MakeupLevelListener mMakeupLevelListener;

    interface MakeupLevelListener {
        void onMakeupLevel(String key, String value);
    }

    public void setMakeupLevelListener(MakeupLevelListener l) {
        mMakeupLevelListener = l;
    }

    public TsMakeupManager(CameraActivity activity, PhotoMenu menu, PhotoUI ui, PreferenceGroup preferenceGroup, View makeupSwitcher) {
        mActivity = activity;
        mUI = ui;
        mMenu = menu;
        mPreferenceGroup = preferenceGroup;
        mTsMakeupSwitcher = makeupSwitcher;

        mMakeupLayoutRoot = (RelativeLayout) mUI.getRootView().findViewById(R.id.id_tsmakeup_level_layout_root);
        //add by wangshenxing for camera ui 2017/05/27 start
        mMakeupSoftenLinearLayout = (LinearLayout) mUI.getRootView().findViewById(R.id.ts_makeup_level_view_port_soften);
        mMakeupCleanLayout = (LinearLayout) mUI.getRootView().findViewById(R.id.id_makeup_clean_layout);
        mMakeupWhitenLayout = (LinearLayout) mUI.getRootView().findViewById(R.id.id_makeup_whiten_layout);
        mMakeupFaceLayout = (LinearLayout) mUI.getRootView().findViewById(R.id.id_makeup_face_layout);
        mMakeupEyeLayout = (LinearLayout) mUI.getRootView().findViewById(R.id.id_makeup_eye_layout);
        //add by wangshenxing for camera ui 2017/05/27 end
        mMakeupUIStatus = MAKEUP_UI_STATUS_NONE;
    }

    public View getMakeupLayoutRoot() {
        return mMakeupLayoutRoot;
    }

    public boolean isShowMakeup() {
        return mMakeupLayoutRoot != null && mMakeupLayoutRoot.isShown();
    }

    public void removeAllViews() {
        if (mMakeupSingleRoot != null) {
            mMakeupSingleRoot.removeAllViews();
            mMakeupSingleRoot = null;
        }
        if (mMakeupLevelRoot != null) {
            mMakeupLevelRoot.removeAllViews();
            mMakeupLevelRoot = null;
        }
        if (mMakeupLayoutRoot != null) {
            mMakeupLayoutRoot.removeAllViews();
        }
    }

    public void dismissMakeupUI() {
        mMakeupUIStatus = MAKEUP_UI_STATUS_DISMISS;
        removeAllViews();
        if (mMakeupLayoutRoot != null) {
            mMakeupLayoutRoot.setVisibility(View.GONE);
        }
        //add by wangshenxing for camera ui 2017/05/27 start
        if (mMakeupSoftenLinearLayout != null) {
            mMakeupSoftenLinearLayout.setVisibility(View.GONE);
        }
        if (mMakeupCleanLayout != null) {
            mMakeupCleanLayout.setVisibility(View.GONE);
        }
        if (mMakeupWhitenLayout != null) {
            mMakeupWhitenLayout.setVisibility(View.GONE);
        }
        if (mMakeupFaceLayout != null) {
            mMakeupFaceLayout.setVisibility(View.GONE);
        }
        if (mMakeupEyeLayout != null) {
            mMakeupEyeLayout.setVisibility(View.GONE);
        }
        //add by wangshenxing for camera ui 2017/05/27 end
    }

    public void resetMakeupUIStatus() {
        mMakeupUIStatus = MAKEUP_UI_STATUS_NONE;
    }

    private void changeMakeupIcon(String value) {
        if (!TextUtils.isEmpty(value)) {
            String prefValue = MAKEUP_ON;
            if (MAKEUP_OFF.equals(value)) {
                prefValue = MAKEUP_OFF;
            }
            final IconListPreference pref = (IconListPreference) mPreferenceGroup
                    .findPreference(CameraSettings.KEY_TS_MAKEUP_UILABLE);
            if (pref == null)
                return;
            pref.setValue(prefValue);
            int index = pref.getCurrentIndex();
            ImageView iv = (ImageView) mTsMakeupSwitcher;
            iv.setImageResource(((IconListPreference) pref).getLargeIconIds()[index]);
            pref.setMakeupSeekBarValue(prefValue);
        }
    }

    public void hideMakeupUI() {
        final IconListPreference pref = (IconListPreference) mPreferenceGroup
                .findPreference(CameraSettings.KEY_TS_MAKEUP_UILABLE);
        if (pref == null)
            return;
        mMakeupUIStatus = MAKEUP_UI_STATUS_NONE;
        String tsMakeupOn = pref.getValue();
        Log.d(TAG, "TsMakeupManager.hideMakeupUI(): tsMakeupOn is " + tsMakeupOn);
        if (MAKEUP_ON.equals(tsMakeupOn)) {
            int index = pref.findIndexOfValue(pref.getValue());
            CharSequence[] values = pref.getEntryValues();
            index = (index + 1) % values.length;
            pref.setMakeupSeekBarValue((String) values[index]);
            ImageView iv = (ImageView) mTsMakeupSwitcher;
            iv.setImageResource(((IconListPreference) pref).getLargeIconIds()[index]);
            mMakeupLevelListener.onMakeupLevel(CameraSettings.KEY_TS_MAKEUP_LEVEL, pref.getValue());

            IconListPreference levelPref = (IconListPreference) mPreferenceGroup
                    .findPreference(CameraSettings.KEY_TS_MAKEUP_LEVEL);
            levelPref.setValueIndex(0); //Turn Off the Makeup feature;


            mMakeupLayoutRoot.setVisibility(View.GONE);
            //add by wangshenxing for camera ui 2017/05/27 start
            mMakeupSoftenLinearLayout.setVisibility(View.GONE);
            mMakeupCleanLayout.setVisibility(View.GONE);
            mMakeupWhitenLayout.setVisibility(View.GONE);
            if (mMakeupEyeLayout != null)
                mMakeupEyeLayout.setVisibility(View.GONE);
            if (mMakeupFaceLayout != null)
                mMakeupFaceLayout.setVisibility(View.GONE);
            //add by wangshenxing for camera ui 2017/05/27 end
            mMakeupLayoutRoot.removeAllViews();
            if (mMakeupSingleRoot != null) {
                mMakeupSingleRoot.removeAllViews();
                mMakeupSingleRoot = null;
            }
            if (mMakeupLevelRoot != null) {
                mMakeupLevelRoot.removeAllViews();
                mMakeupLevelRoot = null;
            }
        }
    }

    public void showMakeupView() {
        mMakeupUIStatus = MAKEUP_UI_STATUS_OFF;
        mMakeupLayoutRoot.setVisibility(View.GONE);
        mMakeupLayoutRoot.removeAllViews();
        if (mMakeupSingleRoot != null) {
            mMakeupSingleRoot.removeAllViews();
            mMakeupSingleRoot = null;
        }
        if (mMakeupLevelRoot != null) {
            mMakeupLevelRoot.removeAllViews();
            mMakeupLevelRoot = null;
        }

        if (mMakeupSingleRoot != null && mMakeupSingleRoot.getVisibility() == View.VISIBLE) {
            showSingleView(MAKEUP_NONE);
            return;
        }

        if (mMakeupUIStatus == MAKEUP_UI_STATUS_DISMISS)
            return;
        //add by wangshenxing for camera ui 2017/05/27 start
        initSeekBarAndImageView();
        if (com.huaqin.common.featureoption.FeatureOption.HQ_1520_AMAZON_MODIFY_FLASHMODE_UI) {
            mMakeupSoftenLinearLayout.setVisibility(View.VISIBLE);
            imageViewArrowUp.setVisibility(View.VISIBLE);
        }
        mMakeupLayoutRoot.setVisibility(View.VISIBLE);
        mMakeupCleanLayout.setVisibility(View.GONE);
        mMakeupWhitenLayout.setVisibility(View.GONE);
        if (mMakeupFaceLayout != null)
            mMakeupFaceLayout.setVisibility(View.GONE);
        if (mMakeupEyeLayout != null)
            mMakeupEyeLayout.setVisibility(View.GONE);
        imageViewArrowDown.setVisibility(View.GONE);
        //add by wangshenxing for camera ui 2017/05/27 end
        final IconListPreference pref = (IconListPreference) mPreferenceGroup
                .findPreference(CameraSettings.KEY_TS_MAKEUP_LEVEL);
        // add by muxudong for max makeup value start
        final ListPreference whitenPref = (ListPreference) mPreferenceGroup.findPreference(CameraSettings.KEY_TS_MAKEUP_LEVEL_WHITEN);
        final ListPreference cleanPref = (ListPreference) mPreferenceGroup.findPreference(CameraSettings.KEY_TS_MAKEUP_LEVEL_CLEAN);
        final ListPreference facePref = (ListPreference) mPreferenceGroup.findPreference(CameraSettings.KEY_TS_MAKEUP_LEVEL_FACE);
        final ListPreference eyePref = (ListPreference) mPreferenceGroup.findPreference(CameraSettings.KEY_TS_MAKEUP_LEVEL_EYE);
        if (pref == null || whitenPref == null || cleanPref == null || facePref == null || eyePref == null)
            return;
        // add by muxudong for max makeup value end

        if (mMakeupLevelRoot != null) {
            mMakeupLevelRoot.removeAllViews();
            mMakeupLevelRoot = null;
        }
        mMakeupLayoutRoot.removeAllViews();

        mMakeupUIStatus = MAKEUP_UI_STATUS_ON;

        int rotation = CameraUtil.getDisplayRotation(mActivity);
        boolean mIsDefaultToPortrait = CameraUtil.isDefaultToPortrait(mActivity);
        if (!mIsDefaultToPortrait) {
            rotation = (rotation + 90) % 360;
        }
        CharSequence[] entries = pref.getEntries();
        int[] thumbnails = pref.getThumbnailIds();

        WindowManager wm = (WindowManager) mActivity.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        int width = display.getWidth();
        int height = display.getHeight();
        Resources r = mActivity.getResources();
        int margin = (int) (r.getDimension(R.dimen.tsmakeup_mode_paddingBottom));
        int levelBgSize = (int) (r.getDimension(R.dimen.tsmakeup_mode_level_size));

        Log.d(TAG, "TsMakeupManager.showMakeupView(): rotation is " + rotation + ", WH is (" + width + ", " + height + "), margin is "
                + margin + ", levelBgSize is " + levelBgSize);

        int gridRes = 0;
        boolean portrait = (rotation == 0) || (rotation == 180);
        int size = height;
        if (portrait) {
            gridRes = R.layout.ts_makeup_level_view_port;
            size = width;
        } else {
            gridRes = R.layout.ts_makeup_level_view_land;
            size = height;
        }
        int itemWidth = size / entries.length;

        LayoutInflater inflater = (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        LinearLayout layout = (LinearLayout) inflater.inflate(gridRes, null, false);
        mMakeupLevelRoot = layout;
        mUI.setMakeupMenuLayout(layout);

        LinearLayout.LayoutParams params = null;
        if (portrait) {
            params = new LayoutParams(itemWidth, itemWidth);
            params.gravity = Gravity.CENTER_VERTICAL;
        } else {
            params = new LayoutParams(itemWidth, itemWidth);
            params.gravity = Gravity.CENTER_HORIZONTAL;
        }

        RelativeLayout.LayoutParams rootParams = null;
        if (rotation == 0) {
            rootParams = new RelativeLayout.LayoutParams(size, levelBgSize);
//            rootParams.bottomMargin = margin;
            rootParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        } else if (rotation == 90) {
            rootParams = new RelativeLayout.LayoutParams(levelBgSize, size);
//            rootParams.rightMargin = margin;
            rootParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        } else if (rotation == 180) {
            rootParams = new RelativeLayout.LayoutParams(size, levelBgSize);
//            rootParams.topMargin = margin;
            rootParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        } else if (rotation == 270) {
            rootParams = new RelativeLayout.LayoutParams(levelBgSize, size);
//            rootParams.leftMargin = margin;
            rootParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        }

        final View[] views = new View[entries.length];
        int init = pref.getCurrentIndex();
        for (int i = 0; i < entries.length; i++) {
            RotateLayout layout2 = (RotateLayout) inflater.inflate(
                    R.layout.ts_makeup_item_view, null, false);

            ImageView imageView = (ImageView) layout2.findViewById(R.id.image);
            TextView label = (TextView) layout2.findViewById(R.id.label);
            final int j = i;

            layout2.setOnTouchListener(new View.OnTouchListener() {
                private long startTime;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        startTime = System.currentTimeMillis();
                    } else if (event.getAction() == MotionEvent.ACTION_UP) {
                        if (System.currentTimeMillis() - startTime < CLICK_THRESHOLD) {
                            pref.setValueIndex(j);
                            // add by muxudong for max makeup value start
                            cleanPref.setValueIndex(j);
                            whitenPref.setValueIndex(j);
                            facePref.setValueIndex(j);
                            eyePref.setValueIndex(j);
                            // add by muxudong for max makeup value end
                            changeMakeupIcon(pref.getValue());
                            //add by wangshenxing for camera ui 2017/05/27 start
                            if (com.huaqin.common.featureoption.FeatureOption.HQ_1520_AMAZON_MODIFY_FLASHMODE_UI) {
                                if (pref.getValue().equals(MAKEUP_OFF)) {
                                    seekBarClean.setProgress(0);
                                    seekBarWhiten.setProgress(0);
                                    if (seekBarFace != null)
                                        seekBarFace.setProgress(0);
                                    if (seekBarEye != null)
                                        seekBarEye.setProgress(0);
                                } else {
                                    // modified by muxudong for max makeup value start
                                    seekBarClean.setProgress(Integer.parseInt(cleanPref.getValue()));
                                    seekBarWhiten.setProgress(Integer.parseInt(whitenPref.getValue()));
                                    if (seekBarFace != null)
                                        seekBarFace.setProgress(Integer.parseInt(facePref.getValue()));
                                    if (seekBarEye != null)
                                        seekBarEye.setProgress(Integer.parseInt(eyePref.getValue()));
                                    // modified by muxudong for max makeup value end
                                }
                            }
                            //add by wangshenxing for camera ui 2017/05/27 end
                            // add by muxudong for max makeup value start
                            mMakeupLevelListener.onMakeupLevel(cleanPref.getKey(), cleanPref.getValue());
                            mMakeupLevelListener.onMakeupLevel(whitenPref.getKey(), whitenPref.getValue());
                            mMakeupLevelListener.onMakeupLevel(facePref.getKey(), facePref.getValue());
                            mMakeupLevelListener.onMakeupLevel(eyePref.getKey(), eyePref.getValue());
                            // add by muxudong for max makeup value end
                            for (View v1 : views) {
                                v1.setSelected(false);
                            }
                            View border = v.findViewById(R.id.image);
                            border.setSelected(true);

                            showSingleView(pref.getValue());
                            mUI.adjustOrientation();
                            if (!pref.getValue().equalsIgnoreCase("off")) {
                                String toast = mActivity.getString(
                                        R.string.text_tsmakeup_beautify_toast);
                                RotateTextToast.makeText(mActivity, toast, Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                    return true;
                }
            });

            View border = layout2.findViewById(R.id.image);
            views[j] = border;
            if (i == init) {
                border.setSelected(true);
            }
            imageView.setImageResource(thumbnails[i]);
            label.setText(entries[i]);
            layout.addView(layout2, params);
        }
        mMakeupLayoutRoot.addView(layout, rootParams);
        //add by wangshenxing for camera ui 2017/05/27 start
        seekBarClean.setOnSeekBarChangeListener(cleanSeekListener);
        seekBarWhiten.setOnSeekBarChangeListener(whitenSeekListener);
        if (seekBarFace != null)
            seekBarFace.setOnSeekBarChangeListener(faceSeekListener);
        if (seekBarEye != null)
            seekBarEye.setOnSeekBarChangeListener(eyeSeekListener);
        imageViewArrowDown.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mMakeupCleanLayout.setVisibility(View.GONE);
                mMakeupWhitenLayout.setVisibility(View.GONE);
                if (mMakeupFaceLayout != null)
                    mMakeupFaceLayout.setVisibility(View.GONE);
                if (mMakeupEyeLayout != null)
                    mMakeupEyeLayout.setVisibility(View.GONE);
                imageViewArrowUp.setVisibility(View.VISIBLE);
                imageViewArrowDown.setVisibility(View.GONE);
            }
        });
        imageViewArrowUp.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mMakeupCleanLayout.setVisibility(View.VISIBLE);
                mMakeupWhitenLayout.setVisibility(View.VISIBLE);
                if (false) {//com.huaqin.common.featureoption.FeatureOption.HQ_1520_AMAZON_CAMERA_ARC_ALGORITHM
                    if (mMakeupFaceLayout != null)
                        mMakeupFaceLayout.setVisibility(View.VISIBLE);
                    if (mMakeupEyeLayout != null)
                        mMakeupEyeLayout.setVisibility(View.VISIBLE);
                }
                imageViewArrowDown.setVisibility(View.VISIBLE);
                imageViewArrowUp.setVisibility(View.GONE);
            }
        });
        //add by wangshenxing for camera ui 2017/05/27 end
    }

    private void showSingleView(String value) {
        if (MAKEUP_NONE.equals(value)) {
            if (mMakeupSingleRoot != null) {
                mMakeupSingleRoot.removeAllViews();
                mMakeupSingleRoot = null;
            }
            mMakeupLayoutRoot.removeAllViews();
            int rotation = CameraUtil.getDisplayRotation(mActivity);
            boolean mIsDefaultToPortrait = CameraUtil.isDefaultToPortrait(mActivity);
            if (!mIsDefaultToPortrait) {
                rotation = (rotation + 90) % 360;
            }

            WindowManager wm = (WindowManager) mActivity.getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();
            int width = display.getWidth();
            int height = display.getHeight();
            Resources r = mActivity.getResources();
            int margin = (int) (r.getDimension(R.dimen.tsmakeup_mode_paddingBottom));
            int levelBgSize = (int) (r.getDimension(R.dimen.tsmakeup_mode_level_size));

            Log.d(TAG, "TsMakeupManager.showSingleView(): rotation is " + rotation + ", WH is (" + width + ", " + height + "), margin is "
                    + margin + ", levelBgSize is " + levelBgSize);

            int gridRes = R.layout.ts_makeup_single_level_view_port;
            int size = width;

            LayoutInflater inflater = (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            final LinearLayout layout = (LinearLayout) inflater.inflate(gridRes, null, false);
            mMakeupSingleRoot = layout;
            mUI.setMakeupMenuLayout(layout);

            RelativeLayout.LayoutParams rootParams = new RelativeLayout.LayoutParams(size, android.widget.RelativeLayout.LayoutParams.WRAP_CONTENT);
            rootParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);

            mMakeupLayoutRoot.addView(layout, rootParams);
            final SeekBar seekBar = (SeekBar) layout.findViewById(R.id.seekbar_makeup_level);
            seekBar.setOnSeekBarChangeListener(this);
            setSingleView(layout);

            mMode = MODE_NONE;

            layout.findViewById(R.id.id_layout_makeup_back).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mMakeupSingleRoot.removeAllViews();
                    mMakeupLayoutRoot.removeView(mMakeupSingleRoot);
                    mMakeupSingleRoot = null;

                    mSingleSelectedIndex = MODE_NONE;
                    mMode = MODE_NONE;

                    showMakeupView();
                    mUI.adjustOrientation();
                }
            });

            layout.findViewById(R.id.id_layout_makeup_whiten).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mMode == MODE_WHITEN) {
                        seekBar.setVisibility(View.GONE);
                        mMode = MODE_NONE;
                        return;
                    }
                    mSingleSelectedIndex = MODE_WHITEN;
                    seekBar.setVisibility(View.VISIBLE);
                    seekBar.setProgress(getPrefValue(CameraSettings.KEY_TS_MAKEUP_LEVEL_WHITEN));
                    mMode = MODE_WHITEN;
                    setSingleView(layout);
                }
            });

            layout.findViewById(R.id.id_layout_makeup_clean).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mMode == MODE_CLEAN) {
                        seekBar.setVisibility(View.GONE);
                        mMode = MODE_NONE;
                        return;
                    }
                    mSingleSelectedIndex = MODE_CLEAN;
                    seekBar.setVisibility(View.VISIBLE);
                    seekBar.setProgress(getPrefValue(CameraSettings.KEY_TS_MAKEUP_LEVEL_CLEAN));
                    mMode = MODE_CLEAN;
                    setSingleView(layout);
                }
            });
        }
    }

    private void setSingleView(LinearLayout layout) {
        if (mSingleSelectedIndex == MODE_WHITEN) {
            layout.findViewById(R.id.id_iv_makeup_whiten).setSelected(true);
            layout.findViewById(R.id.id_iv_makeup_clean).setSelected(false);
        } else if (mSingleSelectedIndex == MODE_CLEAN) {
            layout.findViewById(R.id.id_iv_makeup_whiten).setSelected(false);
            layout.findViewById(R.id.id_iv_makeup_clean).setSelected(true);
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        setSeekbarValue(seekBar.getProgress());
    }

    //add by wangshenxing for camera ui 2017/05/27 start
    private void initSeekBarAndImageView () {
        imageViewArrowDown = (ImageView) mMakeupSoftenLinearLayout.findViewById(R.id.id_iv_makeup_arrow_down);
        imageViewArrowUp = (ImageView) mMakeupSoftenLinearLayout.findViewById(R.id.id_iv_makeup_arrow_up);
        seekBarClean = (SeekBar) mMakeupSoftenLinearLayout.findViewById(R.id.clean_seekbar_makeup_level);
        seekBarWhiten = (SeekBar) mMakeupSoftenLinearLayout.findViewById(R.id.whiten_seekbar_makeup_level);
        seekBarFace = (SeekBar) mMakeupSoftenLinearLayout.findViewById(R.id.face_seekbar_makeup_level);
        seekBarEye = (SeekBar) mMakeupSoftenLinearLayout.findViewById(R.id.eye_seekbar_makeup_level);
        // add by muxudong for max makeup value start
        if (false) {//com.huaqin.common.featureoption.FeatureOption.HQ_1520_AMAZON_CAMERA_ARC_ALGORITHM
            seekBarClean.setMax(50);
            seekBarWhiten.setMax(50);
            seekBarEye.setMax(10);
            seekBarFace.setMax(6);
        }
        // add by muxudong for max makeup value end
    }
    //add by wangshenxing for camera ui 2017/05/27 end

    private void setSeekbarValue(int value) {
        String key = CameraSettings.KEY_TS_MAKEUP_LEVEL_WHITEN;
        if (mMode == MODE_CLEAN) {
            key = CameraSettings.KEY_TS_MAKEUP_LEVEL_CLEAN;
        }
        Log.d(TAG, "TsMakeupManager.onStopTrackingTouch(): value is " + value + ", key is " + key);
        setEffectValue(key, String.valueOf(value));
    }

    private void setEffectValue(String key, String value) {
        final ListPreference pref = (ListPreference) mPreferenceGroup.findPreference(key);
        if (pref == null)
            return;

        pref.setMakeupSeekBarValue(value);
        mMakeupLevelListener.onMakeupLevel(key, value);
    }

    private int getPrefValue(String key) {
        ListPreference pref = mPreferenceGroup.findPreference(key);
        String value = (pref != null) ? pref.getValue() : null;
        Log.d(TAG, "TsMakeupManager.getPrefValue(): value is " + value + ", key is " + key);
        if (TextUtils.isEmpty(value)) {
            value = mActivity.getString(R.string.pref_camera_tsmakeup_level_default);
        }
        return Integer.parseInt(value);
    }

    //add by wangshenxing for camera ui 2017/05/27 start
    private void setSeekbarValueByKey(int value,String key) {
        Log.d(TAG, "TsMakeupManager.onStopTrackingTouch(): value is " + value + ", key is " + key);
        setEffectValue(key, String.valueOf(value));
    }

    private OnSeekBarChangeListener whitenSeekListener = new OnSeekBarChangeListener() {
        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            setSeekbarValueByKey(seekBar.getProgress(),CameraSettings.KEY_TS_MAKEUP_LEVEL_WHITEN);
            if (seekBar.getProgress() == 0 && getPrefValue(CameraSettings.KEY_TS_MAKEUP_LEVEL_CLEAN) == 0
                    && getPrefValue(CameraSettings.KEY_TS_MAKEUP_LEVEL_FACE) == 0
                    && getPrefValue(CameraSettings.KEY_TS_MAKEUP_LEVEL_EYE) == 0) {
                changeMakeupIcon(MAKEUP_OFF);
            } else {
                changeMakeupIcon(MAKEUP_ON);
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress,
                                      boolean fromUser) {
        }
    };

    private OnSeekBarChangeListener cleanSeekListener = new OnSeekBarChangeListener() {
        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            setSeekbarValueByKey(seekBar.getProgress(),CameraSettings.KEY_TS_MAKEUP_LEVEL_CLEAN);
            if (seekBar.getProgress() == 0 && getPrefValue(CameraSettings.KEY_TS_MAKEUP_LEVEL_WHITEN) == 0
                    && getPrefValue(CameraSettings.KEY_TS_MAKEUP_LEVEL_FACE) == 0
                    && getPrefValue(CameraSettings.KEY_TS_MAKEUP_LEVEL_EYE) == 0) {
                changeMakeupIcon(MAKEUP_OFF);
            } else {
                changeMakeupIcon(MAKEUP_ON);
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress,
                                      boolean fromUser) {
        }
    };

    private OnSeekBarChangeListener faceSeekListener = new OnSeekBarChangeListener() {
        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            setSeekbarValueByKey(seekBar.getProgress(),CameraSettings.KEY_TS_MAKEUP_LEVEL_FACE);
            if (seekBar.getProgress() == 0 && getPrefValue(CameraSettings.KEY_TS_MAKEUP_LEVEL_WHITEN) == 0
                    && getPrefValue(CameraSettings.KEY_TS_MAKEUP_LEVEL_CLEAN) == 0
                    && getPrefValue(CameraSettings.KEY_TS_MAKEUP_LEVEL_EYE) == 0) {
                changeMakeupIcon(MAKEUP_OFF);
            } else {
                changeMakeupIcon(MAKEUP_ON);
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress,
                                      boolean fromUser) {
        }
    };
    private OnSeekBarChangeListener eyeSeekListener = new OnSeekBarChangeListener() {
        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            setSeekbarValueByKey(seekBar.getProgress(),CameraSettings.KEY_TS_MAKEUP_LEVEL_EYE);
            if (seekBar.getProgress() == 0 && getPrefValue(CameraSettings.KEY_TS_MAKEUP_LEVEL_WHITEN) == 0
                    && getPrefValue(CameraSettings.KEY_TS_MAKEUP_LEVEL_CLEAN) == 0
                    && getPrefValue(CameraSettings.KEY_TS_MAKEUP_LEVEL_FACE) == 0) {
                changeMakeupIcon(MAKEUP_OFF);
            } else {
                changeMakeupIcon(MAKEUP_ON);
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress,
                                      boolean fromUser) {
        }
    };
    //add by wangshenxing for camera ui 2017/05/27 end

}
