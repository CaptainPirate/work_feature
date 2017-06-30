/*
  * Copyright @ 2015 China Mobile Group Device Co.,Ltd.
  * All rights Reserved.
*/

package com.android.launcher3;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.UriMatcher;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.provider.BaseColumns;
import android.provider.CalendarContract;
import android.provider.CallLog;
import android.provider.CalendarContract.CalendarAlerts;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.android.launcher3.badge.unread.CalendarUndismissedAlertLoader;
import com.android.launcher3.badge.unread.CmccNotificationListenerService.NotifyType;
import com.android.launcher3.badge.unread.FetionNotificationUnreadInfoParser;
import com.android.launcher3.badge.unread.MicroMsgNotificationUnreadInfoParser;
import com.android.launcher3.badge.unread.NotificationUnreadInfoParser;
import com.android.launcher3.badge.unread.QQNotificationUnreadInfoParser;
import com.android.launcher3.badge.unread.WhatsappNotificationUnreadInfoParser;
import com.android.launcher3.compat.UserHandleCompat;
/*******************************************************************************************************
|   when     |      who     |    keyword       |        why        |     what                          |
********************************************************************************************************
20160620      liyichong.wt   bug 186853         requirements        add ota unread info manager
20160702      liyichong.wt   bug 186853         bugs                modify URIMatcher
20160720      sunwenping.wt  bug 200118         bugs                modify sql string
******************************************************************************************************/

public class CmccUnreadInfoManager extends ContentObserver {

    private static final String TAG = "UnreadInfoManager";
    private static final boolean DEBUG = false;

    public static String sExceedText = "999+";
    public static int sMaxUnreadCount = 999;

    // Apps that show in Launcher workspace or in all apps, currently we only
    // use Mms and Phone, we will show unread info num on their icon.
    private static final ComponentName sMmsComponentName = new ComponentName("com.android.mms",
            "com.android.mms.ui.ModeChooser");
    private static final ComponentName sDialerComponentName = new ComponentName("com.android.contacts",
            "com.android.dialer.DialtactsActivity");
    private static final ComponentName sEmailComponentName = new ComponentName("com.android.email",
            "com.android.email.activity.Welcome");
    private static final ComponentName sCalendarComponentName = new ComponentName(
            "com.android.calendar", "com.android.calendar.AllInOneActivity");
    private static final ComponentName sSettingsComponentName = new ComponentName(
            "com.android.settings", "com.android.settings.Settings");//bug 186853, liyichong.wt, MODIFY, 20160620

    private static final ComponentName sOrigMmsComponentName = new ComponentName("com.android.mms",
            "com.android.mms.ui.ConversationList");
    private static final ComponentName sOrigDialerComponentName = new ComponentName("com.android.dialer",
            "com.android.dialer.DialtactsActivity");

    private ArrayList<ComponentName> mBaseComps;
    private final HashMap<ComponentName, ArrayList<ComponentName>> mShadowComps;

    private static final int MATCH_CALL = 1;
    private static final int MATCH_MMSSMS = 2;
    private static final int MATCH_EMAIL = 3;
    private static final int MATCH_CALENDAR = 4;
    private static final int MATCH_SETTINGS = 5;

    private static final int MSG_RETRY_FOR_CALENDAR = 999;
    private static final int DELAY_FOR_RETRY = 5000; // 5s

    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    public static final String ACTION_UNREAD_INFO_CHANGED = "com.android.launcher3.action.UNREAD_INFO_CHANGED";
    public static final String ACTION_ADD_COMPONENT_FOR_UNREAD_INFO = "com.android.launcher3.action.ADD_COMPONENT_FOR_UNREAD_INFO";
    public static final String EXTRA_COMPONENT_NAME = "extra_component_name";
    public static final String EXTRA_UNREAD_INFO_NUM = "extra_unread_info_num";

    static {
        sURIMatcher.addURI("call_log", "calls", MATCH_CALL);
        sURIMatcher.addURI("mms-sms", null, MATCH_MMSSMS);
        sURIMatcher.addURI("sms", "iccsms", MATCH_MMSSMS);
        sURIMatcher.addURI("com.android.email.provider", null, MATCH_EMAIL);
        sURIMatcher.addURI(CalendarContract.AUTHORITY, null, MATCH_CALENDAR);
        sURIMatcher.addURI("settings", "system/com_android_ota_mtk_unread", MATCH_SETTINGS);//bug 186853, liyichong.wt, MODIFY, 20160702
    }

    private static final Uri MMSSMS_CONTENT_URI = Uri.parse("content://mms-sms");
    private static final Uri CALLS_CONTENT_URI = CallLog.Calls.CONTENT_URI;
    private static final Uri MMS_CONTENT_URI = Uri.parse("content://mms");
    private static final Uri SMS_CONTENT_URI = Uri.parse("content://sms");
    private static final Uri SIMSMS_CONTENT_URI = Uri.parse("content://sms/iccsms");
    private static final Uri ICC1_SMS_URI = Uri.parse("content://sms/icc1");
    private static final Uri ICC2_SMS_URI = Uri.parse("content://sms/icc2");

    private static final Uri EMAIL_CONTENT_URI = Uri.parse("content://com.android.email.provider/mailbox");

    /* bug 186853, liyichong.wt, MODIFY, 20160620 start*/
    private static final Uri SETTINGS_CONTENT_URI = Settings.System.getUriFor("com_android_ota_mtk_unread");//Uri.parse("content://com.hmct.contentProvider/updateState")
    /* bug 186853, liyichong.wt, MODIFY, 20160620 end*/

    private static final String[] PROJECTION_FOR_EMAIL = new String[] {"_id", "type", "unreadCount"};

    //bug 200118, sunwenping.wt, modify 20160720
    //private static final String MISSED_CALLS_SELECTION =
	//	"("+ CallLog.Calls.TYPE + " = " + CallLog.Calls.MISSED_TYPE + " or "  + CallLog.Calls.TYPE + " = " + CallLog.Calls.MISSED_IMS_TYPE + ")  AND " + ///CallLog.Calls.NEW + " = 1";
		
    private static final String MISSED_CALLS_SELECTION = "";

    private Context mContext;
    private WeakReference<Launcher> mLauncherRef;
    private LauncherAppState mAppState;
    private HashMap<ComponentName, UnreadInfo> mUnreadInfoCache = new HashMap<ComponentName, UnreadInfo>();
    private HashMap<ComponentName, Boolean> mUnreadInfoChangedCache = new HashMap<ComponentName, Boolean>();

    // assume the unit is dp
    private float mLargeTextSize = 14;
    private float mMiddleTextSize = 12;
    private float mSmallTextSize = 10;
    private Drawable mBackground;

    private int mBeyondSize = 12;

    private boolean mUpdateUnreadInfoTheFirstTime;

    private ArrayList<NotificationUnreadInfoParser> mParsers;
    private boolean mNotificationParserEnabled = false;

    public CmccUnreadInfoManager(LauncherAppState appState) {
        super(null);

        mAppState = appState;
        mContext = appState.getContext();

        Resources res = mContext.getResources();
        mLargeTextSize = res.getDimension(R.dimen.unread_info_large_text_size);
        mMiddleTextSize = res.getDimension(R.dimen.unread_info_middle_text_size);
        mSmallTextSize = res.getDimension(R.dimen.unread_info_small_text_size);
        mBackground = res.getDrawable(R.drawable.unread_info_background);

        mBeyondSize = (int) res.getDimension(R.dimen.unread_info_bmp_beyond_size);

        sExceedText = res.getString(R.string.exceed_max_unread_count_text);
        sMaxUnreadCount = res.getInteger(R.integer.max_unread_count);

        mBaseComps = new ArrayList<ComponentName>();

        // TODO: need refactor, component need read from xml, this will allow
        // easy config, they need a type indicate the component is use database
        // or notification to get unread info.

        // TODO: we should unregister all component for update unread info when
        // the component uninstalled.

        // monitor unread info based on database
        monitorUnreadInfoForComponent(sMmsComponentName, true);
        monitorUnreadInfoForComponent(sDialerComponentName, true);
        monitorUnreadInfoForComponent(sEmailComponentName, true);
        monitorUnreadInfoForComponent(sCalendarComponentName, true);
        monitorUnreadInfoForComponent(sSettingsComponentName, true);

        // monitor unread info based on notification
        mNotificationParserEnabled = res.getBoolean(R.bool.support_third_party_unread_badge);
        if (mNotificationParserEnabled) {
            mParsers = new ArrayList<NotificationUnreadInfoParser>();
            NotificationUnreadInfoParser parser = new MicroMsgNotificationUnreadInfoParser();
            registerNotificationUnreadInfoParser(parser);
            parser = new QQNotificationUnreadInfoParser();
            registerNotificationUnreadInfoParser(parser);
            parser = new FetionNotificationUnreadInfoParser();
            registerNotificationUnreadInfoParser(parser);
            parser = new WhatsappNotificationUnreadInfoParser();
            registerNotificationUnreadInfoParser(parser);
        }

        // register content observer only once for the LauncherApplication
        ContentResolver resolver = mContext.getContentResolver();
        resolver.registerContentObserver(CALLS_CONTENT_URI, true, this);
        resolver.registerContentObserver(MMSSMS_CONTENT_URI, true, this);
        resolver.registerContentObserver(SIMSMS_CONTENT_URI, true, this);
        resolver.registerContentObserver(EMAIL_CONTENT_URI, true, this);
        resolver.registerContentObserver(CalendarAlerts.CONTENT_URI, true, this);
        resolver.registerContentObserver(SETTINGS_CONTENT_URI, true, this);

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_ADD_COMPONENT_FOR_UNREAD_INFO);
        filter.addAction(ACTION_UNREAD_INFO_CHANGED);
        mContext.registerReceiver(mReceiver, filter);

        mShadowComps = new HashMap<ComponentName, ArrayList<ComponentName>>();
        mapShadowComponent(sMmsComponentName, sOrigMmsComponentName);
        mapShadowComponent(sDialerComponentName, sOrigDialerComponentName);

        mUpdateUnreadInfoTheFirstTime = true;
    }

    public void bindLauncher(Launcher launcher) {
        mLauncherRef = new WeakReference<Launcher>(launcher);
    }

    public void terminate() {
        ContentResolver resolver = mContext.getContentResolver();
        resolver.unregisterContentObserver(this);
    }

    /**
     * Note: this method be called in Launcher.onResume to check if there has
     * any unread info need to be updated, then update these unread info all at
     * once.
     */
    public void updateUnreadInfoIfNeeded() {
        if (mUnreadInfoChangedCache.size() > 0
                && mLauncherRef != null && mLauncherRef.get() != null) {
            mLauncherRef.get().updateUnreadInfo();
        }
    }

    /**
     * Note: this method is called by Working thread in LauncherModel before load
     * and bind any icon into Workspace or AppsCustomizePagedView.
     * 
     * Prepare the unread data and bitmap and then apply these bitmap when
     * new BubbleTextView need to be created.
     * 
     * This method should not be called in UI thread for it manipulate database.
     */
    public void prepareUnreadInfo() {
        if (mUpdateUnreadInfoTheFirstTime) {
            mUpdateUnreadInfoTheFirstTime = false;
            for(ComponentName cn : mBaseComps) {
                int unreadNum = loadUnreadInfoCount(cn);
                updateUnreadInfoCache(cn, unreadNum, false);
            }
        }
    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {
        super.onChange(selfChange, uri);
        if (DEBUG) {
            Log.d(TAG, String.format("onChange: uri=%s selfChange=%b", uri.toString(), selfChange));
        }

        int delay = 1000;
        ComponentName cn = null;
        int what = sURIMatcher.match(uri);
        switch (what) {
            case MATCH_CALL:
                cn = sDialerComponentName;
                break;
            case MATCH_MMSSMS:
                cn = sMmsComponentName;
                break;
            case MATCH_EMAIL:
                cn = sEmailComponentName;
                break;
            case MATCH_CALENDAR:
                // minimize times that access database, 1100 is an empirical value,
                // different app have difference update frequent, this value should
                // be chosen accordingly.
                delay = 1100;
                cn = sCalendarComponentName;

                /* Special case for calendar.
                 * Table CalendarAlerts in CalendarProvider2 is deleted by trigger,
                 * so we can not get onChange callback when the data changed in this
                 * table in some times. But the good news is that CalendarAlerts table
                 * change is triggered by other data, we can receive onChange for
                 * those data. We make a trick to delay for retrieve data in table
                 * CalendarAlerts. This solution do harm to Launcher but can sync the
                 * badge for calendar even if it has some delay.
                 * */
                sHandler.removeMessages(MSG_RETRY_FOR_CALENDAR);
                Message msg = sHandler.obtainMessage(MSG_RETRY_FOR_CALENDAR, cn);
                sHandler.sendMessageDelayed(msg, DELAY_FOR_RETRY);
                break;
            case MATCH_SETTINGS:
                cn = sSettingsComponentName;
                break;
        }
        if (cn != null && what != UriMatcher.NO_MATCH) {
            sHandler.removeMessages(what);
            Message msg = sHandler.obtainMessage(what, cn);
            sHandler.sendMessageDelayed(msg, delay);
        }
    }

    public boolean updateBubbleTextViewUnreadInfo(BubbleTextView bubble) {
        if (bubble != null) {
            Object tag = bubble.getTag();
            if (tag instanceof ShortcutInfo
                    || tag instanceof AppInfo) {
                ComponentName cn = ((ItemInfo) tag).getIntent().getComponent();
                bubble.updateUnreadInfo(getUnreadInfoInternal(cn));
                return hasComponentUnreadInfoChanged(cn);
            }
        }
        return false;
    }

    public UnreadInfo getUnreadInfo(ShortcutInfo info) {
        ComponentName cn = info.getIntent().getComponent();
        return getUnreadInfoInternal(cn);
    }

    public UnreadInfo getUnreadInfo(AppInfo info) {
        ComponentName cn = info.getIntent().getComponent();
        return getUnreadInfoInternal(cn);
    }

    public UnreadInfo getUnreadInfoInternal(ComponentName cn) {
        if (mUnreadInfoCache.containsKey(cn)) {
            return mUnreadInfoCache.get(cn);
        }
        return null;
    }

    private boolean hasComponentUnreadInfoChanged(ComponentName cn) {
        if (mUnreadInfoChangedCache.containsKey(cn)) {
            return mUnreadInfoChangedCache.get(cn);
        }
        return false;
    }

    private void asyncGetUnreadInfoAndTriggerUpdate(final ComponentName cn) {
        new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... params) {
                return loadUnreadInfoCount(cn);
            }

            @Override
            protected void onPostExecute(Integer unreadNum) {
                sendBroadcastForUnreadInfoChanged(cn, unreadNum);
            }
        }.execute();
    }

    private void sendBroadcastForUnreadInfoChanged(ComponentName cn,
            int unreadNum) {
        Intent intent = new Intent(ACTION_UNREAD_INFO_CHANGED);
        intent.putExtra(EXTRA_COMPONENT_NAME, cn);
        intent.putExtra(EXTRA_UNREAD_INFO_NUM, unreadNum);
        mContext.sendStickyBroadcastAsUser(intent, UserHandleCompat.myUserHandle().getUser());
    }

    private int loadUnreadInfoCount(ComponentName component) {
        if (component.equals(sMmsComponentName)) {
            return getUnreadMessageCount();
        } else if (component.equals(sDialerComponentName)) {
            return getMissedCallCount();
        } else if (sEmailComponentName.equals(component)) {
            return getUnreadEmailCount();
        } else if (sCalendarComponentName.equals(component)) {
            return getUnreadCalendarCount();
        } else if (sSettingsComponentName.equals(component)) {
            return isUpdateAvailable();
        } else {
            return 0;
        }
    }

    private int getUnreadMessageCount() {
        int unreadSms = 0;
        int unreadSim1Mms = 0;
        int unreadSim2Mms = 0;
        int unreadMms = 0;

        try {
            Cursor cursor = null;
            ContentResolver resolver = mContext.getContentResolver();

            // get unread sms count
            cursor = resolver.query(SMS_CONTENT_URI, new String[] { BaseColumns._ID },
                    "type = 1 AND read = 0", null, null);
            if (cursor != null) {
                unreadSms = cursor.getCount();
                cursor.close();
                Log.d(TAG, String.format("getUnreadMessageCount unreadSms=%d", unreadSms));
            }

            // get unread sms number in SIM.
            // status_on_icc = 3 indicates unread, status_on_icc = 1 indicates read.
            cursor = resolver.query(ICC1_SMS_URI, new String[] { BaseColumns._ID },
                    "status_on_icc = 3", null, null);
            if (cursor != null) {
                for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                    int status_on_icc = cursor.getInt(cursor.getColumnIndex("status_on_icc"));
                    if (status_on_icc == 3) {
                        unreadSim1Mms++;
                    }
                }
                cursor.close();
                if (DEBUG) {
                    Log.d(TAG, String.format("getUnreadMessageCount unreadSim1Sms=%d", unreadSim1Mms));
                }
            } else {
                Log.w(TAG, "getUnreadMessageCount unreadSim1Sms cursor null");
            }

            cursor = resolver.query(ICC2_SMS_URI, new String[] { BaseColumns._ID },
                    "status_on_icc = 3", null, null);
            if (cursor != null) {
                for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                    int status_on_icc = cursor.getInt(cursor.getColumnIndex("status_on_icc"));
                    if (status_on_icc == 3) {
                        unreadSim2Mms++;
                    }
                }
                cursor.close();
                if (DEBUG) {
                    Log.d(TAG, String.format("getUnreadMessageCount unreadSim2Sms=%d", unreadSim2Mms));
                }
            } else {
                Log.w(TAG, "getUnreadMessageCount unreadSim2Sms cursor null");
            }

            // if the following values redefined in module telephony-common, then we
            // need to redefined them here.
            // MESSAGE_TYPE_NOTIFICATION_IND = 0x82
            // MESSAGE_TYPE_RETRIEVE_CONF = 0x84

            // get unread mms count
            cursor = resolver.query(MMS_CONTENT_URI, new String[] { BaseColumns._ID },
                    "msg_box = 1 AND read = 0 AND ( m_type = 130 OR m_type = 132 ) AND thread_id > 0",
                    null, null);

            if (cursor != null) {
                unreadMms = cursor.getCount();
                cursor.close();
                if (DEBUG) {
                    Log.i(TAG, String.format("getUnreadMessageCount unreadMms=%d", unreadMms));
                }
            }
        } catch (SQLiteException ex) {
            Log.e(TAG, "Can not get unread message count.", ex);
        } catch (NullPointerException ex) {
            Log.e(TAG, "Can not get unread message count.", ex);
        }

        return (unreadSms + unreadSim1Mms + unreadSim2Mms + unreadMms);
    }

    private int getMissedCallCount() {
        int missedCalls = 0;

        Cursor cursor = null;
        ContentResolver resolver = mContext.getContentResolver();

        cursor = resolver.query(CALLS_CONTENT_URI, new String[] { BaseColumns._ID },
                MISSED_CALLS_SELECTION, null, null);
        if (cursor != null) {
            missedCalls = cursor.getCount();
            cursor.close();
            if (DEBUG) {
                Log.i(TAG, String.format("getMissedCallCount missedCalls=%d", missedCalls));
            }
        }

        return missedCalls;
    }

    private int getUnreadEmailCount() {
        int unreadCount = 0;
        try {
            ContentResolver resolver = mContext.getContentResolver();
            Cursor cursor = resolver.query(EMAIL_CONTENT_URI, PROJECTION_FOR_EMAIL,
                    null, null, null);
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    int type = cursor.getInt(1);
                    int unread = cursor.getInt(2);
                    // type 0 represent Inbox
                    if (type == 0) {
                        unreadCount += unread;
                    }
                }
                cursor.close();
            }
        } catch (SecurityException ex) {
            Log.e(TAG, "getUnreadEmailCount fail: " + ex.getMessage());
        }
        return unreadCount;
    }

    private int getUnreadCalendarCount() {
        CalendarUndismissedAlertLoader loader = new CalendarUndismissedAlertLoader(
                mContext);
        return loader.getUnreadInfoCount();
    }

    private int isUpdateAvailable() {
        int need = 0;
        /* bug 186853, liyichong.wt, MODIFY, 20160620 start*/
        /*Cursor cursor = null;
        try {
            ContentResolver resolver = mContext.getContentResolver();
            cursor = resolver.query(SETTINGS_CONTENT_URI, new String[] {"numb"},
                    "_id=1", null, null);
            if (cursor != null && cursor.moveToFirst()) {
                need = cursor.getInt(0);
            }
        } catch (SecurityException ex) {
            Log.e(TAG, "isUpdateAvailable() fail: " + ex.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            Log.d(TAG, "isUpdateAvailable() return " + need);
            return need;
        }*/
        try {
            need = Settings.System.getInt(mContext.getContentResolver(), "com_android_ota_mtk_unread");
        } catch (SettingNotFoundException e) {
            Log.i(TAG,"Setting Not Found Exception");
        }
        Log.d(TAG, "isUpdateAvailable() return " + need);
        return need;
        /* bug 186853, liyichong.wt, MODIFY, 20160620 end*/
    }

    private boolean updateUnreadInfoCache(ComponentName cn, int unreadNum, boolean addToChangedCache) {
        if (mUnreadInfoCache.containsKey(cn)) {
            UnreadInfo info = mUnreadInfoCache.get(cn);
            if (info.mUnreadInfoNum != unreadNum
                    && !(info.mUnreadInfoNum > sMaxUnreadCount && unreadNum > sMaxUnreadCount)) {
                if (unreadNum == 0) {
                    info.mIconWithNum = null;
                } else {
                    // TODO: need optimize, delay it.
                    info.mIconWithNum = createBitmapWithUnreadInfo(unreadNum);
                }
                info.mUnreadInfoNum = unreadNum;

                // update shadow components
                if (mShadowComps.containsKey(cn)) {
                    ArrayList<ComponentName> shadows = mShadowComps.get(cn);
                    for (ComponentName shadow : shadows) {
                        // don't need to test mUnreadInfoCache contains shadow,
                        // we know that all shadow will be add to mUnreadInfoCache
                        // in mapShadowComponent(...).
                        mUnreadInfoCache.get(shadow).shadowCopy(info);

                        if (addToChangedCache) {
                            mUnreadInfoChangedCache.put(shadow, true);
                        }
                    }
                }

                if (addToChangedCache) {
                    mUnreadInfoChangedCache.put(cn, true);
                }
                return true;
            }
        }
        return false;
    }

    public Bitmap createBitmapWithUnreadInfo(int unreadNum) {
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(Color.WHITE);

        String finalText;
        if (unreadNum <= sMaxUnreadCount) {
            finalText = String.valueOf(unreadNum);
        } else {
            finalText = sExceedText;
        }
        switch (finalText.length()) {
        case 1:
            paint.setTextSize(mLargeTextSize);
            break;
        case 2:
            paint.setTextSize(mMiddleTextSize);
            break;
        default:
            paint.setTextSize(mSmallTextSize);
            break;
        }

        int bgWidth = mBackground.getIntrinsicWidth();
        int bgHeight = mBackground.getIntrinsicHeight();

        Rect textBounds = new Rect();
        paint.getTextBounds(finalText, 0, finalText.length(), textBounds);

        // Why we not use textBounds.width() as textWidth?
        // After test on devices, use the measured width is more precise to center
        // the number in the background circle.
        int textWidth = (int) paint.measureText(finalText, 0, finalText.length());

        // TODO: if textWidth >= bgWidth or textHeight >= bgHeight,
        // if textWidth >= circleWidth or textHeight >= circleHeight,
        // we must reduce the font size until fit the previous condition.

        Bitmap compoundBmp = Bitmap.createBitmap(bgWidth, bgHeight,
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(compoundBmp);

        mBackground.setBounds(0, 0, bgWidth, bgHeight);
        // draw background
        mBackground.draw(canvas);

        int x = (bgWidth - textWidth) / 2;
        int y = (int) ((bgHeight - (paint.descent() + paint.ascent())) / 2);

        // draw number
        canvas.drawText(finalText, x, y, paint);

        canvas.setBitmap(null);
        return compoundBmp;
    }

    public void resetComponentsUnreadInfoChangedValue() {
        mUnreadInfoChangedCache.clear();
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_UNREAD_INFO_CHANGED.equals(action)) {
                ComponentName cn = intent.getParcelableExtra(EXTRA_COMPONENT_NAME);
                int unreadNum = intent.getIntExtra(EXTRA_UNREAD_INFO_NUM, -1);
                if (cn != null && unreadNum >= 0) {
                    updateUnreadInfoCache(cn, unreadNum, true);
                    updateUnreadInfoIfNeeded();
                }
            } else if (ACTION_ADD_COMPONENT_FOR_UNREAD_INFO.equals(action)) {
                ComponentName cn = intent.getParcelableExtra(EXTRA_COMPONENT_NAME);
                if (cn != null) {
                    monitorUnreadInfoForComponent(cn, false);
                }
            }
        }
    };

    private static Handler sHandler = new Handler() {
        private CmccUnreadInfoManager mManager;

        @Override
        public void handleMessage(Message msg) {
            if (mManager == null) {
                mManager = LauncherAppState.getInstance().getUnreadInfoManager();
            }
            switch (msg.what) {
            case MATCH_CALL:
            case MATCH_MMSSMS:
            case MATCH_EMAIL:
            case MATCH_CALENDAR:
            case MATCH_SETTINGS:
                mManager.asyncGetUnreadInfoAndTriggerUpdate((ComponentName) msg.obj);
                break;
            /* special case */
            case MSG_RETRY_FOR_CALENDAR:
                mManager.asyncGetUnreadInfoAndTriggerUpdate((ComponentName) msg.obj);
                break;
            }
        }

    };

    private void mapShadowComponent(ComponentName cn, ComponentName shadow) {
        if (cn != null && shadow != null && !cn.equals(shadow)) {
            ArrayList<ComponentName> arr = null;
            if (mShadowComps.containsKey(cn)) {
                arr = mShadowComps.get(cn);
            } else {
                arr = new ArrayList<ComponentName>();
                mShadowComps.put(cn, arr);
            }
            arr.add(shadow);

            if (!mUnreadInfoCache.containsKey(shadow)) {
                monitorUnreadInfoForComponent(shadow, false);
            }
        }
    }

    public int getBeyondSize() {
        return mBeyondSize;
    }

    public class UnreadInfo {
        public int mUnreadInfoNum;
        public Bitmap mIconWithNum;

        public UnreadInfo(int num) {
            mUnreadInfoNum = num;
            mIconWithNum = null;
        }

        public void shadowCopy(UnreadInfo info) {
            if (info == null) {
                return;
            }
            mUnreadInfoNum = info.mUnreadInfoNum;
            mIconWithNum = info.mIconWithNum;
        }
    }

    private void monitorUnreadInfoForComponent(ComponentName cn, boolean base) {
        if (cn == null) {
            Log.w(TAG, "can't monitor unread info for null component.");
            return;
        }
        mUnreadInfoCache.put(cn, new UnreadInfo(0));
        if (base) {
            mBaseComps.add(cn);
        }
        if (DEBUG) {
            Log.d(TAG, "monitor unread info base: " + base + ", for: " + cn);
        }
    }

    private boolean isNotificationUnreadInfoParserExist(
            NotificationUnreadInfoParser parser) {
        if (!mNotificationParserEnabled || parser == null) {
            return false;
        }
        ComponentName cn = parser.getComponentName();
        for (NotificationUnreadInfoParser p : mParsers) {
            if (cn.equals(p.getComponentName())) {
                return true;
            }
        }
        return false;
    }

    public void registerNotificationUnreadInfoParser(
            NotificationUnreadInfoParser parser) {
        if (!mNotificationParserEnabled || parser == null) {
            Log.w(TAG,
                    "can not register null as notification unread info parser.");
            return;
        }
        if (!isNotificationUnreadInfoParserExist(parser)) {
            mParsers.add(parser);
        }
        monitorUnreadInfoForComponent(parser.getComponentName(), false);
    }

    public void parseNotifications(StatusBarNotification[] sbns, NotifyType type) {
        if (!mNotificationParserEnabled) {
            return;
        }

        for (NotificationUnreadInfoParser parser : mParsers) {
            int num = parser.parseNotifications(sbns, type);
            ComponentName cn = parser.getComponentName();
            sendBroadcastForUnreadInfoChanged(cn, num);
        }
    }
}
