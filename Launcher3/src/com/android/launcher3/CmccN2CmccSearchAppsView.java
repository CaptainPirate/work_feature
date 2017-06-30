/*
  * Copyright @ 2015 China Mobile Group Device Co.,Ltd.
  * All rights Reserved.
*/

package com.android.launcher3;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import com.android.launcher3.CmccN2CmccSearchAppsProvider.CmccSearchApps;
import com.android.launcher3.CmccGridView.OnClickBlankPositionListener;
import com.android.launcher3.badge.unread.UnreadBadgeContainer;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

/*******************************************************************************************************
|   when     |      who     |    keyword       |        why        |     what                          |
********************************************************************************************************
20160420      liyichong.wt   bug 155071         requirements        add search modify
20160425      liyichong.wt   bug 155071         requirements        add search modify
20160630      liyichong.wt   bug 191961         bugs                modify SearchBar Bugs
20160711      liyichong.wt   bug 196318         bugs                modify SearchBar Bugs
******************************************************************************************************/
public class CmccN2CmccSearchAppsView extends RelativeLayout implements
    OnClickBlankPositionListener, OnClickListener, UnreadBadgeContainer 
    , TextWatcher, TextView.OnEditorActionListener{
    private static final String TAG = "CmccN2CmccSearchAppsView";

    private CmccGridView mGridView;
    private TextView mEmptyHintView;

    private Launcher mLauncher;
    private IconCache mIconCache;
    private LayoutInflater mLayoutInflater;

    private ArrayList<AppInfo> mApps;
    private ArrayList<AppInfo> mCmccSearchAllApps;
    private ArrayList<ShortcutInfo> mShortcuts;
    private ArrayList<Entry> mEntries;
    private FrequentUsedAppsAdapter mAdapter;

    private boolean mCloseWhenLauncherOnResume;

    private boolean mOpened;

    /* bug 155071, liyichong.wt, ADD, 20160425 start*/
    private boolean mCmccSearchApps = false;
    private InputMethodManager mInputMethodManager;
    /* bug 155071, liyichong.wt, ADD, 20160425 end*/

    private static final int SHOW_HIDE_ANIMATION_DURATION = 200;
    private static final float HIDE_SCALE_X = 0.1f;
    private static final float HIDE_SCALE_Y = 0.1f;
    private static final float HIDE_ALPHA = 0f;
    private static final float SHOW_SCALE_X = 1.0f;
    private static final float SHOW_SCALE_Y = 1.0f;
    private static final float SHOW_ALPHA = 1f;

    private static final Pattern SPLIT_PATTERN = Pattern.compile("[\\s|\\p{javaSpaceChar}]+");
    private int mMaxShowItemCount = 20;
    private static final boolean DEBUG = false;

    private CmccN2ExtendedEditText mSearchBarEditView;

    public static final Comparator<Entry> REVERSE_USED_COUNT_COMPARATOR = new Comparator<Entry>() {
        @Override
        public int compare(Entry lhs, Entry rhs) {
            // first compare used count and than compare last used time
            int result = rhs.usedCount - lhs.usedCount;
            if (result == 0) {
                result = (int) (rhs.timestamp - lhs.timestamp);
            }
            return result;
        }
    };

    public CmccN2CmccSearchAppsView(Context context) {
        super(context);
        init(context);
    }

    public CmccN2CmccSearchAppsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public CmccN2CmccSearchAppsView(Context context, AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        mMaxShowItemCount = context.getResources().getInteger(
                R.integer.frequent_used_apps_max_item_count);
        /* bug 155071, liyichong.wt, ADD, 20160425 start*/
        mInputMethodManager = (InputMethodManager)
                context.getSystemService(Context.INPUT_METHOD_SERVICE);
        /* bug 155071, liyichong.wt, ADD, 20160425 end*/
    }

    @Override
    protected void onFinishInflate() {
        mSearchBarEditView = (CmccN2ExtendedEditText) findViewById(R.id.search_box_input);
        mSearchBarEditView.addTextChangedListener(this);
        mSearchBarEditView.setOnEditorActionListener(this);
        mSearchBarEditView.setOnBackKeyListener(
                new CmccN2ExtendedEditText.OnBackKeyListener() {
                    @Override
                    public boolean onBackKey() {
                        // Only hide the search field if there is no query, or if there
                        // are no filtered results
                        mSearchBarEditView.setCursorVisible(false);//bug 196318, liyichong.wt, ADD, 20160711
                        String query = Utilities.trim(
                                mSearchBarEditView.getEditableText().toString());
                        return false;
                    }
                });
        /* bug 196318, liyichong.wt, ADD, 20160711 start*/
        mSearchBarEditView.setOnClickListener(
                new OnClickListener() {

                    @Override
                    public void onClick(View arg0) {
                        mSearchBarEditView.setCursorVisible(true);
                    }
                });
        /* bug 196318, liyichong.wt, ADD, 20160711 end*/

        mGridView = (CmccGridView) findViewById(R.id.frequent_used_apps_gridview);
        mEmptyHintView = (TextView) findViewById(R.id.frequent_used_apps_empty_hint);

        mApps = new ArrayList<AppInfo>();
        mCmccSearchAllApps = new ArrayList<AppInfo>();
        mShortcuts = new ArrayList<ShortcutInfo>();
        mEntries = new ArrayList<Entry>();

        mAdapter = new FrequentUsedAppsAdapter();
        mGridView.setAdapter(mAdapter);
        mGridView.setOnClickBlankPositionListener(this);

        setOnClickListener(this);

        super.onFinishInflate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // handle all touch event, not let it pass to other views in DragLayer
        // call super.onTouchEvent(event) to allow onClick callback.
        super.onTouchEvent(event);
        return true;
    }

    @Override
    public boolean onClickBlankPosition() {
        hide(true);
        return true;
    }

    public void onResume() {
        if (mCloseWhenLauncherOnResume && isVisible()) {
            hide(false);
        }
        mCloseWhenLauncherOnResume = false;
    }

    public boolean isVisible() {
        return getVisibility() == View.VISIBLE;
    }

    public void setup(Launcher launcher) {
        mLauncher = launcher;
        mLayoutInflater = mLauncher.getInflater();

        mLauncher.getContentResolver().registerContentObserver(CmccSearchApps.CONTENT_URI,
                true, mContentObserver);

        mIconCache = LauncherAppState.getInstance().getIconCache();
    }

    public void destroy() {
        mLauncher.getContentResolver().unregisterContentObserver(mContentObserver);
    }

    public void show(boolean animate, final ArrayList<AppInfo> apps) {
        /* bug 155071, liyichong.wt, ADD, 20160425 start*/
        //mCmccSearchAllApps = apps;
        /* bug 191961, liyichong.wt, ADD, 20160630 start*/
        mLauncher.getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        mLauncher.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        /* bug 191961, liyichong.wt, ADD, 20160630 end*/
        mAdapter.notifyDataSetChanged();
        /* bug 155071, liyichong.wt, ADD, 20160425 end*/
        /* bug 196318, liyichong.wt, ADD, 20160711 start*/
        mSearchBarEditView.setCursorVisible(true);
        mInputMethodManager.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
        /* bug 196318, liyichong.wt, ADD, 20160711 end*/
        if (mLauncher != null) {
            mLauncher.enableBlurredBackground(true, animate, SHOW_HIDE_ANIMATION_DURATION);
        }
        bringToFront();
        mOpened = true;
        if (animate) {
            LauncherViewPropertyAnimator showAnimation = new LauncherViewPropertyAnimator(this);
            showAnimation.scaleX(SHOW_SCALE_X).scaleY(SHOW_SCALE_Y)
                .alpha(SHOW_ALPHA).setDuration(SHOW_HIDE_ANIMATION_DURATION)
                .setInterpolator(new AccelerateDecelerateInterpolator());
            showAnimation.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    setVisibility(View.VISIBLE);
                }
            });
            showAnimation.start();
        } else {
            setVisibility(View.VISIBLE);
            setScaleX(SHOW_SCALE_X);
            setScaleY(SHOW_SCALE_Y);
            setAlpha(SHOW_ALPHA);
        }
    }

    public void hide(boolean animate) {
        /* bug 191961, liyichong.wt, ADD, 20160630 start*/
        mLauncher.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        mLauncher.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        /* bug 191961, liyichong.wt, ADD, 20160630 end*/
        mSearchBarEditView.setCursorVisible(false);//bug 196318, liyichong.wt, ADD, 20160711
        if (mLauncher != null) {
            mLauncher.enableBlurredBackground(false, animate, SHOW_HIDE_ANIMATION_DURATION);
            mLauncher.enablePrepareUnlockAnimState(true);
        }
        mOpened = false;
        if (animate) {
            LauncherViewPropertyAnimator hideAnimation = new LauncherViewPropertyAnimator(this);
            hideAnimation.scaleX(HIDE_SCALE_X).scaleY(HIDE_SCALE_Y)
                .alpha(HIDE_ALPHA).setDuration(SHOW_HIDE_ANIMATION_DURATION)
                .setInterpolator(new AccelerateDecelerateInterpolator());
            hideAnimation.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    setVisibility(View.GONE);
                }
            });
            hideAnimation.start();
        } else {
            setScaleX(HIDE_SCALE_X);
            setScaleY(HIDE_SCALE_Y);
            setAlpha(HIDE_ALPHA);
            setVisibility(View.GONE);
        }
        /* bug 155071, liyichong.wt, ADD, 20160425 start*/
        mInputMethodManager.hideSoftInputFromWindow(
                mSearchBarEditView.getWindowToken(), 0);
        mCmccSearchApps =false;
        mSearchBarEditView.setText("");
        /* bug 155071, liyichong.wt, ADD, 20160425 end*/
    }

    public void setApps(ArrayList<AppInfo> list) {
        mApps.clear();
        if (list != null && list.size() != 0) {
            mApps.addAll(list);
        }
        reloadFromDatabase();
    }

    public void addApps(ArrayList<AppInfo> list) {
        if (list == null || list.size() == 0) {
            return;
        }
        addAppsNoUpdate(list);
    }

    public void removeApps(ArrayList<AppInfo> list) {
        if (list == null || list.size() == 0) {
            return;
        }
        removeAppsNoUpdate(list, true);
    }

    public void updateApps(ArrayList<AppInfo> list) {
        if (list == null || list.size() == 0) {
            return;
        }

        // NOTE: package name won't change, so no need change mEntries.
        // We remove and re-add the updated applications list because it's
        // properties may have changed (ie. the title)
        removeAppsNoUpdate(list, false);
        addAppsNoUpdate(list);

        // No database change, so we need manually notify data set changed.
        mAdapter.notifyDataSetChanged();
    }

    public void recordShortcutInfo(ShortcutInfo shortcut) {
        if (!mShortcuts.contains(shortcut)) {
            mShortcuts.add(shortcut);
        }
    }

    private void addAppsNoUpdate(ArrayList<AppInfo> list) {
        for (AppInfo info : list) {
            mApps.add(info);
        }
    }

    private void removeAppsNoUpdate(ArrayList<AppInfo> list, boolean touchEntryList) {
        for (AppInfo info : list) {
            int removeIndex = findAppByComponent(mApps, info);
            if (removeIndex > -1) {
                mApps.remove(removeIndex);
            }

            if (touchEntryList) {
                String removedPkg = info.getIntent().getComponent().getPackageName();
                removeEntriesByPackageName(removedPkg);
                removeShortcutByPackageName(removedPkg);
            }
        }
    }

    private void removeShortcutByPackageName(String pkgName) {
        if (pkgName == null) {
            Log.w(TAG, "removeShortcutByPackageName, parameter pkgName is null.");
            return;
        }

        Iterator<ShortcutInfo> it = mShortcuts.iterator();
        while(it.hasNext()) {
            ShortcutInfo info = it.next();
            ComponentName target = info.getTargetComponent();
            if (target != null && pkgName.equals(target.getPackageName())) {
                it.remove();
            }
        }
    }

    private void removeEntriesByPackageName(String pkgName) {
        if (pkgName == null) {
            Log.w(TAG, "removeEntriesByPackageName, parameter pkgName is null.");
            return;
        }

        List<Entry> removedEntries = new ArrayList<Entry>();
        Iterator<Entry> it = mEntries.iterator();
        while(it.hasNext()) {
            Entry entry = it.next();
            if (pkgName.equals(entry.component.getPackageName())) {
                it.remove();
                removedEntries.add(entry);
            }
        }

        // TODO: async operate on database
        if (!removedEntries.isEmpty()) {
            for (Entry entry : removedEntries) {
                removeEntryFromDatabase(entry);
            }
        }
    }

    private void removeEntryFromDatabase(Entry entry) {
        if (entry == null) {
            Log.w(TAG, "removeEntryFromDatabase, parameter entry is null.");
            return;
        }

        CmccN2CmccSearchAppsProvider.deleteFrequentUsedAppsRecord(mLauncher.getContentResolver(),
                entry.id);
    }

    private int findAppByComponent(List<AppInfo> list, AppInfo item) {
        ComponentName removeComponent = item.intent.getComponent();
        int length = list.size();
        for (int i = 0; i < length; ++i) {
            AppInfo info = list.get(i);
            if (info.user.equals(item.user)
                    && info.intent.getComponent().equals(removeComponent)) {
                return i;
            }
        }
        return -1;
    }

    private BubbleTextView createBubbleTextView() {
        return  (BubbleTextView) mLayoutInflater.inflate(
                R.layout.apps_customize_application, mGridView, false);
    }

    private AppInfo getAppInfoFromEntries(Entry entry) {
        for (AppInfo info : mApps) {
            if (entry.component.equals(info.getIntent().getComponent())) {
                return info;
            }
        }
        return null;
    }

    private AppInfo getAppInfoFromEntries(String pkgName) {
        if (pkgName == null) {
            Log.w(TAG, "getAppInfoFromEntries, parameter pkgName is null.");
            return null;
        }

        for (AppInfo info : mApps) {
            String packageName = info.getIntent().getPackage();
            ComponentName cn = info.getIntent().getComponent();
            if (pkgName.equals(packageName)
                    || pkgName.equals(cn.getPackageName())) {
                return info;
            }
        }
        return null;
    }

    private ShortcutInfo getShortcutInfoFromShortcuts(Entry entry) {
        for (ShortcutInfo info : mShortcuts) {
            if (entry.component.equals(info.getTargetComponent())) {
                return info;
            }
        }
        return null;
    }

    private void reloadFromDatabase() {
        new FrequentUsedAppsDataLoadder().execute();
    }

    private class FrequentUsedAppsDataLoadder extends
        AsyncTask<Void, Integer, List<Entry>> {

        @Override
        protected List<Entry> doInBackground(
                Void... params) {
            // TODO: show progress bar
            List<Entry> entries = loadFromDB();
            Collections.sort(entries, REVERSE_USED_COUNT_COMPARATOR);
            return entries;
        }

        protected void onPostExecute(List<Entry> entries) {
            if (entries != null && entries.size() > 0) {
                mEntries.clear();
                mEntries.addAll(entries);

                mAdapter.notifyDataSetChanged();

                // hide empty hint view
                mEmptyHintView.setVisibility(View.INVISIBLE);
                // show grid view
                mGridView.setVisibility(View.VISIBLE);
            } else {
                // hide grid view
                mEmptyHintView.setVisibility(View.VISIBLE);
                // show empty hint view
                mGridView.setVisibility(View.INVISIBLE);
            }
        }

        private List<Entry> loadFromDB() {
            ContentResolver resolver = mLauncher.getContentResolver();

            List<Entry> entries = new ArrayList<Entry>();
            Cursor cursor = null;
            try {
                cursor = resolver.query(CmccSearchApps.CONTENT_URI, null,
                        null, null, CmccSearchApps.USED_COUNT + " DESC, "
                        + CmccSearchApps.TIMESTAMP + " DESC LIMIT " + mMaxShowItemCount);
                if (cursor != null) {
                    if (DEBUG) {
                        Log.d(TAG, "loadFromDB, row count: " + cursor.getCount());
                    }

                    while (cursor.moveToNext()) {
                        long id = cursor.getLong(CmccSearchApps.ID_INDEX);
                        String cnStr = cursor.getString(CmccSearchApps.COMPONENT_NAME_INDEX);
                        int usedCount = cursor.getInt(CmccSearchApps.USED_COUNT_INDEX);
                        long timestamp = cursor.getLong(CmccSearchApps.TIMESTAMP_INDEX);
                        entries.add(new Entry(id, cnStr, usedCount, timestamp));
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Load frequent used apps from db faied", e);
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
            return entries;
        }
    }

    private ContentObserver mContentObserver = new  ContentObserver(null) {
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
        }

        @Override
        public void onChange(boolean selfChange) {
            reloadFromDatabase();
        }
    };

    private class FrequentUsedAppsAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            /* bug 155071, liyichong.wt, MODIFY, 20160425 start*/
            return  mCmccSearchApps == false ? mEntries.size() : mCmccSearchAllApps.size();
            /* bug 155071, liyichong.wt, MODIFY, 20160425 end*/
        }

        @Override
        public Object getItem(int position) {
            /* bug 155071, liyichong.wt, MODIFY, 20160425 start*/
            return mCmccSearchApps == false ? mEntries.get(position) : mCmccSearchAllApps.get(position);
            /* bug 155071, liyichong.wt, MODIFY, 20160425 end*/
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            BubbleTextView bubble = null;
            if (convertView == null) {
                // a brand new BubbleTextView instance, has no text or drawable
                bubble = createBubbleTextView();
                bubble.setTextColor(Color.WHITE);
                //bubble.setOnClickListener(mLauncher);
                bubble.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mLauncher.onClick(v);
                        mCloseWhenLauncherOnResume = true;
                    }});
            } else {
                bubble = (BubbleTextView) convertView;
            }

            /* bug 155071, liyichong.wt, MODIFY, 20160425 start*/
            if(!mCmccSearchApps){
                Entry entry = mEntries.get(position);

                boolean filledOK = false;
                // find from apps info by component name
                AppInfo info = getAppInfoFromEntries(entry);
                if (info != null) {
                    filledOK = true;
                    bubble.applyFromApplicationInfo(info);
                }

                // find from shortcuts info by component name
                if (!filledOK) {
                    ShortcutInfo shortcutInfo = getShortcutInfoFromShortcuts(entry);
                    if (shortcutInfo != null) {
                        filledOK = true;
                        bubble.applyFromShortcutInfo(shortcutInfo, mIconCache, true);
                    }
                }

                // find from apps info by package name
                if (!filledOK) {
                    info = getAppInfoFromEntries(entry.component.getPackageName());
                    if (info != null) {
                        filledOK = true;
                        bubble.applyFromApplicationInfo(info);
                    }
                }
            }else{
                AppInfo searchInfo = mCmccSearchAllApps.get(position);
                bubble.applyFromApplicationInfo(searchInfo);
            }
            /* bug 155071, liyichong.wt, MODIFY, 20160425 end*/
            return bubble;
        }
    }

    private class Entry {
        // TODO: support multiple user in 5.0

        public long id;
        public ComponentName component;
        public int usedCount;
        public long timestamp;

        public Entry(long id, String cnStr, int usedCount, long timestamp) {
            this.id = id;
            this.usedCount = usedCount;
            this.component = ComponentName.unflattenFromString(cnStr);
            this.timestamp = timestamp;
        }

        @Override
        public String toString() {
            return String.format("FrequentUsedAppsEntry, ID[%d], ComponentName[%s], Used count[%d], Timestamp[%d]",
                    id, component, usedCount, timestamp);
        }
    }

    @Override
    public void onClick(View v) {
        hide(true);
    }

    public boolean isShowing() {
        return mOpened;
    }

    @Override
    public void updateUnreadBadge(CmccUnreadInfoManager manager) {
        int count = mGridView.getChildCount();
        for (int i = 0; i < count; i++) {
            View bubble = mGridView.getChildAt(i);
            if (bubble instanceof BubbleTextView) {
                manager.updateBubbleTextViewUnreadInfo((BubbleTextView) bubble);
            }
        }
    }

    @Override
    public boolean onEditorAction(TextView arg0, int arg1, KeyEvent arg2) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void afterTextChanged(final Editable s) {
        String query = s.toString();
        if(!query.isEmpty()){
            final ArrayList<AppInfo> result = getTitleMatchResult(query);
        /* bug 155071, liyichong.wt, MODIFY, 20160425 start*/
            mCmccSearchAllApps = result;
            mCmccSearchApps =true;
        }else{
            mCmccSearchApps =false;
        }
        /* bug 155071, liyichong.wt, MODIFY, 20160425 end*/
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        // Do nothing
        
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        // Do nothing

    }

    protected ArrayList<AppInfo> getTitleMatchResult(String query) {
        // Do an intersection of the words in the query and each title, and filter out all the
        // apps that don't match all of the words in the query.
        final String queryTextLower = query.toLowerCase();
        final String[] queryWords = SPLIT_PATTERN.split(queryTextLower);

        final ArrayList<AppInfo> result = new ArrayList<AppInfo>();
        /* bug 155071, liyichong.wt, MODIFY, 20160425 start*/
        for (AppInfo info : mApps) {
        /* bug 155071, liyichong.wt, MODIFY, 20160425 end*/
            if (matches(info, queryWords)) {
                result.add(info);
            }
        }
        return result;
    }

    protected boolean matches(AppInfo info, String[] queryWords) {
        String title = info.title.toString();
        String[] words = SPLIT_PATTERN.split(title.toLowerCase());
        for (int qi = 0; qi < queryWords.length; qi++) {
            boolean foundMatch = false;
            for (int i = 0; i < words.length; i++) {
                if (words[i].startsWith(queryWords[qi])) {
                    foundMatch = true;
                    break;
                }
            }
            if (!foundMatch) {
                // If there is a word in the query that does not match any words in this
                // title, so skip it.
                return false;
            }
        }
        return true;
    }
}
