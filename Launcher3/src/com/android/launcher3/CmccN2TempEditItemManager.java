/*
 * Copyright (C) 2016 Wingtech Group.
 * Modification based on code covered by the mentioned copyright
 * and/or permission notice(s).
 */

/*
 * Copyright (C) 2016 The CMCC N2 Project
 * manage the temp edit items
 *
 */
/*******************************************************************************************************
|   when     |      who     |    keyword       |        why        |     what                          |
********************************************************************************************************
20160426     tangzhongfeng.wt   bug 163862          requirements       
******************************************************************************************************/

package com.android.launcher3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import com.android.launcher3.CmccN2TempEditItemInfo.CurrentCellInfo;

import android.content.ComponentName;
import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;
import android.util.Log;

public class CmccN2TempEditItemManager {

    public static String TAG = "CmccN2TempEditItemManager";
    private Launcher mLauncher;
    private static CmccN2TempEditItemManager instance;
    static final HashMap<Long, CmccN2TempEditItemInfo> sItemsIdMap = new HashMap<Long, CmccN2TempEditItemInfo>();
    static ArrayList<CmccN2TempEditItemInfo> mItems = new ArrayList<CmccN2TempEditItemInfo>();
    final static ArrayList<CmccN2TempEditItemInfo> mRemoveItems = new ArrayList<CmccN2TempEditItemInfo>();
    final static ArrayList<FinalCellInfo> mRearrangeItemsOrFolders = new ArrayList<FinalCellInfo>();
    final static ArrayList<FinalCellInfo> mRearrangeWidgets = new ArrayList<FinalCellInfo>();
    final static ArrayList<FinalCellInfo> mParentIsFolder = new ArrayList<FinalCellInfo>();
    static final HashMap<String, ArrayList<FinalCellInfo>> sFinalListMap = new HashMap<String, ArrayList<FinalCellInfo>>();

    private static long NO_ID = -1;

    private int mCellCountX;
    private int mCellCountY;
    private int mMaxPageCount;
    private static final boolean DEBUG_REVERT_ITEM_INFO = true;
    private static final boolean DEBUG_REVERT_INFO = true;

    public static final int REARRANGE_NEW_SCREEN_CELL_X = 0;
    public static final int REARRANGE_NEW_SCREEN_CELL_Y = 0;

    public static final int REARRANGE_SCREEN_CELLX = 4;
    public static final int REARRANGE_SCREEN_CELLY = 4;

    static long[][][] EditTargetCell;
    private static long mMaxId = NO_ID;

    private static final int MESSAGE_START = 0;
    private static final int MESSAGE_REVERT_START = 1;
    private static final int MESSAGE_ONELIST_LOADED = 2;
    private static final int MESSAGE_REARRANGE_START = 3;
    private static final int MESSAGE_REARRANGE_END = 4;
    private static final int MESSAGE_REMOVE_ITEMS = 5;
    private static final int MESSAGE_END = 6;
    private static final int MESSAGE_REVERT_PARENTFOLDER_ITEMS = 7;
    private static final int MESSAGE_CALCULATE_PARENTFOLDER_ITEMS = 8;
    private static final int MESSAGE_TOBE_END = 10;

    private static final int REARRAGE_FINISED = 1;
    private static final int REARRAGE_ITMES_PARENTFOLDER_FINISED = 4;
    private static final int FINISED = 1;

    static boolean mIsReverting;
    private final Handler sRevert = new Handler() {
        int count_load = 0;// for revert the items with kinds of screens;
        int count_reArrange = 0;// the three runnable are all done, the revert
                                // is
                                // finished
        int count_finish = 0;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_START:
                count_load = 0;
                count_reArrange = 0;
                count_finish = 0;
                buildListForScreen();
                break;
            case MESSAGE_REVERT_START:
                if (DEBUG_REVERT_INFO) {
                    Log.i(TAG, " ----MESSAGE_REVERT_START- count_load =" + count_load);
                }
                revertItems();
                // removeItemsFromDatabase();
                break;
            case MESSAGE_ONELIST_LOADED:
                if (msg.arg1 == MESSAGE_REVERT_START) {
                    count_load++;
                }
                if (DEBUG_REVERT_INFO)
                    Log.i(TAG, " ----MESSAGE_ONELIST_LOADED count_load =" + count_load);
                if (count_load == sFinalListMap.size()) {
                    // revert the items to theres own screen,start calculate new
                    // place
                    reCalculateItems(MESSAGE_ONELIST_LOADED);
                }
                break;
            case MESSAGE_REARRANGE_START:
                if (DEBUG_REVERT_INFO)
                    Log.i(TAG, " ----MESSAGE_REARRANGE_START- count_finish =" + count_finish);
                count_reArrange = 0;
                // rearrangeWidgets(msg.arg1);
                rearrangeItemsOrFolders(msg.arg1);
                break;
            case MESSAGE_REARRANGE_END:
                count_reArrange++;
                if (DEBUG_REVERT_INFO)
                    Log.i(TAG, " ----MESSAGE_REARRANGE_END- count_finish =" + count_reArrange);
                if (count_reArrange == REARRAGE_FINISED && msg.arg1 == MESSAGE_ONELIST_LOADED) {
                    Log.i(TAG, " ----end =" + count_reArrange);
                    sendMessage(sRevert.obtainMessage(MESSAGE_REVERT_PARENTFOLDER_ITEMS));
                } else if (count_reArrange == REARRAGE_FINISED && msg.arg1 == MESSAGE_CALCULATE_PARENTFOLDER_ITEMS) {
                    sendMessage(sRevert.obtainMessage(MESSAGE_TOBE_END, 0, 1));
                }
                break;
            case MESSAGE_REVERT_PARENTFOLDER_ITEMS:
                if (DEBUG_REVERT_INFO)
                    Log.i(TAG, " ----MESSAGE_REVERT_PARENTFOLDER_ITEMS");
                revertItemsToFolders();
                break;
            case MESSAGE_CALCULATE_PARENTFOLDER_ITEMS:
                if (DEBUG_REVERT_INFO)
                    Log.i(TAG, " ----MESSAGE_CALCULATE_PARENTFOLDER_ITEMS");
                reCalculateItems(MESSAGE_CALCULATE_PARENTFOLDER_ITEMS);
                break;
            case MESSAGE_REMOVE_ITEMS:
                Log.i(TAG, " ----MESSAGE_REMOVE_ITEMS----");
                sendMessage(sRevert.obtainMessage(MESSAGE_END));
                break;
            case MESSAGE_TOBE_END:
                count_finish = msg.arg2 + count_finish;
                Log.i(TAG, " ----To be end =" + count_finish);
                if (count_finish == FINISED) {
                    sendMessage(sRevert.obtainMessage(MESSAGE_END));
                }
                break;
            case MESSAGE_END:
                Log.i(TAG, " ----MESSAGE_END----");
                revertfinalize();
                break;
            }
        }
    };

    private CmccN2TempEditItemManager(Context context) {
        Resources res = context.getResources();
        mCellCountX = res.getInteger(R.integer.tempedit_cell_count_x);
        mCellCountY = res.getInteger(R.integer.tempedit_cell_count_y);
        mMaxPageCount = res.getInteger(R.integer.tempedit_max_page);
    }

    public synchronized static CmccN2TempEditItemManager getInstance(Context context) {
        if (instance == null) {
            instance = new CmccN2TempEditItemManager(context);
        }
        return instance;
    }

    private long generateNewId() {
        if (mMaxId < 0) {
            throw new RuntimeException("Error: max id was not initialized");
        }
        mMaxId += 1;
        return mMaxId;
    }

    public void initialize(Launcher launcher) {
        mLauncher = launcher;
        sItemsIdMap.clear();
        mRemoveItems.clear();
        mItems.clear();

        mRearrangeItemsOrFolders.clear();
        mRearrangeWidgets.clear();
        sFinalListMap.clear();

        EditTargetCell = new long[mCellCountX][mCellCountY][mMaxPageCount];
        for (int k = 0; k < mCellCountX; k++) {
            for (int j = 0; j < mCellCountY; j++) {
                for (int i = 0; i < mMaxPageCount; i++) {
                    EditTargetCell[k][j][i] = NO_ID;
                }
            }
        }
        mMaxId = 0;
    }

    private void revertfinalize() {
        sRevert.post(new Runnable() {
            @Override
            public void run() {
                // mLauncher.getWorkspace().removeVacantFolders();
                mLauncher.getWorkspace().exitIconManageOverviewMode(true);
                mIsReverting = false;
            }
        });

        mLauncher.getIconManagePanel().finalizeEdit();
        mMaxId = -1;
        sItemsIdMap.clear();
        mRemoveItems.clear();
        mItems.clear();
        mRearrangeItemsOrFolders.clear();
        mRearrangeWidgets.clear();
        sFinalListMap.clear();
        mParentIsFolder.clear();
        //Bug 163862 tangzhongfeng.wt MODIFY 20160426
        mLauncher.notifyFoldersWithFinalItem();
    }

    private boolean checkX(int x) {
        return x >= 0 && x < mCellCountX;
    }

    private boolean checkY(int y) {
        return y >= 0 && y < mCellCountY;
    }

    private boolean checkScreen(int s) {
        return s >= 0 && s < mMaxPageCount;
    }

    private void setToMap(int cellX, int cellY, int screen, long id) {
        if (checkX(cellX) && checkY(cellY) && checkScreen(screen)) {
            EditTargetCell[cellX][cellY][screen] = id;
        }
    }

    private void resetToMap(int cellX, int cellY, int screen) {
        if (checkX(cellX) && checkY(cellY) && checkScreen(screen)) {
            EditTargetCell[cellX][cellY][screen] = NO_ID;
        }
    }

    private void resetToMap(long id) {
        for (int k = 0; k < mCellCountX; k++) {
            for (int j = 0; j < mCellCountY; j++) {
                for (int i = 0; i < mMaxPageCount; i++) {
                    if (EditTargetCell[k][j][i] == id) {
                        EditTargetCell[k][j][i] = NO_ID;
                        return;
                    }
                }
            }
        }
    }

    private long getIDFromMap(final int cellX, final int cellY, final int screen) {
        if (checkX(cellX) && checkY(cellY) && checkScreen(screen)) {
            return EditTargetCell[cellX][cellY][screen];
        } else {
            return NO_ID;
        }
    }

    private CmccN2TempEditItemInfo getTempItemByID(final long id) {
        if (sItemsIdMap.size() == 0) {
            return null;
        }

        if (id == NO_ID)
            return null;

        CmccN2TempEditItemInfo modelItem = sItemsIdMap.get(id);

        return modelItem;
    }

    private CmccN2TempEditItemInfo getTempItemByPosition(final int cellX, final int cellY, final int screen) {
        long itemId = getIDFromMap(cellX, cellY, screen);

        if (itemId == NO_ID)
            return null;

        CmccN2TempEditItemInfo modelItem = sItemsIdMap.get(itemId);
        return modelItem;
    }
    /*bug 180264 tangzhongfeng.wt ADD 20160525 start */
    void resetMap() {
        for (int k = 0; k < mCellCountX; k++) {
            for (int j = 0; j < mCellCountY; j++) {
                for (int i = 0; i < mMaxPageCount; i++) {
                    EditTargetCell[k][j][i] = NO_ID;
                }
            }
        }
    }
    /*bug 180264 tangzhongfeng.wt ADD 20160525 end */

    public void addItem(ItemInfo info, CmccN2TempEditItemInfo.CurrentCellInfo currentInfo,
            CmccN2TempEditItemInfo.OriginalCellInfo originInfo) {
        addItem(info, currentInfo, originInfo, null, null, null);
    }

    public void addItem(ItemInfo info, CmccN2TempEditItemInfo.CurrentCellInfo currentInfo,
            CmccN2TempEditItemInfo.OriginalCellInfo originInfo, FolderInfo folderInfo) {
        addItem(info, currentInfo, originInfo, folderInfo, null, null);
    }

    public void addItem(ItemInfo info, CmccN2TempEditItemInfo.CurrentCellInfo currentInfo,
            CmccN2TempEditItemInfo.OriginalCellInfo originInfo, FolderInfo folderInfo,
            CmccN2TempEditItemInfo.CurrentCellInfo curFiInfo, CmccN2TempEditItemInfo.OriginalCellInfo oriFiInfo) {
        if (info.tempId != NO_ID) {
            return;// this is exist
        }

        final long id = generateNewId();
        if (id <= 0) {
            return; // TODO error issue
        }

        final CurrentCellInfo mCurInfo = (CurrentCellInfo) currentInfo;
        final int newCellX = mCurInfo.cellX;
        final int newCellY = mCurInfo.cellY;
        final int newScreen = mCurInfo.screen;

        // add item to CmccN2TempEditItemInfo
        CmccN2TempEditItemInfo mInfo = new CmccN2TempEditItemInfo(mLauncher, id, info);
        mInfo.setOringnalInfo(originInfo);
        mInfo.setCurrentCellInfo(currentInfo);
        mCurInfo.setTempId(id);
        info.setTempId(id);

        if (DEBUG_REVERT_ITEM_INFO) {
            Log.i(TAG,
                    "addItem ItemInfo  original cell x y[x, y] =" + originInfo.cellX + ", " + originInfo.cellY
                            + ", spanX =" + originInfo.spanX + ", spanY =" + originInfo.spanY + ", container ="
                            + originInfo.container + ", screen =" + originInfo.screenId
                            + ", originInfo.itemInfo.screen = " + originInfo.itemInfo.screenId + ", item.itemType ="
                            + originInfo.itemType + ", item.id =" + originInfo.id + ", id =" + id);
        }
        sItemsIdMap.put(id, mInfo);
        Log.i(TAG, "tzf addItem newCellX = " + newCellX + ", newCellY = " + newCellY + ", newScreen = " + newScreen);
        if (DEBUG_REVERT_ITEM_INFO) {
            long tmpId = getIDFromMap(newCellX, newCellY, newScreen);
            if (tmpId != NO_ID) {
                CmccN2TempEditItemInfo item = getTempItemByID(tmpId);
                if (item == null) {
                    Log.d(TAG, "tzf addItem exist for update");
                } else {
                    Log.w(TAG, "tzf addItem exist error");
                }
            }
        }
        //bug 204480 tangzhongfeng.wt MODIFY 20160804
        setToMap(newCellX, newCellY, newScreen, mInfo.itemID);
    }

    public void moveItem(long itemId, int cellX, int cellY, long screen, long container) {
        moveItem(itemId, cellX, cellY, screen, container, null);
    }

    public void moveItem(long itemId, int cellX, int cellY, long screen, long container, FolderInfo folderInfo) {
        /*tangzhongfeng.wt ADD 20160426 for the moveItem method start */
        final long id = itemId;
        final int newCellX = cellX;
        final int newCellY = cellY;
        final int newScreen = (int) screen;
        final long newContainer = container;

        CmccN2TempEditItemInfo mSourceInfo = getTempItemByID(id);
        if (mSourceInfo == null)
            return; // TODO error issue
        // Old position to remove
        CmccN2TempEditItemInfo.CurrentCellInfo mOldCellInfo = mSourceInfo.getCurrentCellInfo();
        long parentId = mOldCellInfo.getParentId();
        if (parentId == -1) {
            // means not from the tempFolder, is from the edit target
            boolean needToRest = true;
            if (mOldCellInfo.cellX != -1 && mOldCellInfo.cellY != -1 && mOldCellInfo.screen != -1) {
                CmccN2TempEditItemInfo mTmp = getTempItemByPosition(mOldCellInfo.cellX, mOldCellInfo.cellY,
                        mOldCellInfo.screen);
                if (mTmp != null && mTmp.itemID != mSourceInfo.itemID) {
                    // This place is be placed ,such as by swap;
                    if (DEBUG_REVERT_INFO)
                        Log.d(TAG, " -------do ot  needToRest =");
                    needToRest = false;
                }
            }
            if (needToRest) {
                resetToMap(mOldCellInfo.cellX, mOldCellInfo.cellY, mOldCellInfo.screen);
            }
        }

        CmccN2TempEditItemInfo mNewCellInfo = getTempItemByPosition(newCellX, newCellY, newScreen);
        if (mNewCellInfo == null) {
            // The position is null
            mSourceInfo.resetCurrentCellInfo(itemId, newScreen, newCellX, newCellY, newContainer);
            setToMap(newCellX, newCellY, newScreen, itemId);
        }
        /*tangzhongfeng.wt ADD 20160426 for the moveItem method end */
    }

    static final class SwapItemsInfo {
        long iTempId;
        int toCellX;
        int toCellY;
        int toScreen;
        long toContainer;
    }

    public void addOrMoveItem(ItemInfo info, CmccN2TempEditItemInfo.CurrentCellInfo currentInfo,
            CmccN2TempEditItemInfo.OriginalCellInfo originInfo, FolderInfo folderInfo) {

        Log.d(TAG, " -00---addOrMoveItem  info.tempId = " + info.tempId + ", info.id = " + info.id);

        if (info.tempId <= 0) {
            addItem(info, currentInfo, originInfo, folderInfo);// add this item
        } else {
            moveItem(info.tempId, currentInfo.cellX, currentInfo.cellY, currentInfo.screenId, currentInfo.container,
                    folderInfo);
        }
    }

    public void addOrMoveItem(long tempId, ItemInfo info, CmccN2TempEditItemInfo.CurrentCellInfo currentInfo,
            CmccN2TempEditItemInfo.OriginalCellInfo originInfo, FolderInfo folderInfo) {

        Log.d(TAG, " ---11-addOrMoveItem -- tempId = " + tempId + ", info.tempId" + info.tempId + ", info.id = "
                + info.id);
        if (info.tempId != tempId)
            info.setTempId(tempId);

        if (tempId <= 0) {
            addItem(info, currentInfo, originInfo, folderInfo);// add this item
        } else {
            moveItem(info.tempId, currentInfo.cellX, currentInfo.cellY, currentInfo.screenId, currentInfo.container,
                    folderInfo);
        }
    }

    public void addOrMoveItem(long itemId, int cellX, int cellY, int screen, int container, FolderInfo folderInfo) {
    }

    public void deleteItem(int cellX, int cellY, int screen) {
        final int x = cellX;
        final int y = cellY;
        final int s = screen;

        CmccN2TempEditItemInfo mCur = getTempItemByPosition(x, y, s);
        if (mCur != null) {
            /*
             * if (mCur instanceof TctTempFolderInfo) { TctTempFolderInfo
             * mFolder = (TctTempFolderInfo) mCur; for (CmccN2TempEditItemInfo mTmp
             * : mFolder.getChildrenInFolder()) {
             * sItemsIdMap.remove(mTmp.itemID); } mFolder.finalize(); }
             */
            sItemsIdMap.remove(mCur.itemID);
        }
        resetToMap(x, y, s);
    }

    public void deleteItem(long id) {
        sItemsIdMap.remove(id);
        resetToMap(id);
    }

    public void revertItemsToWorkspace() {
        mIsReverting = true;
        sRevert.sendMessage(sRevert.obtainMessage(MESSAGE_START));
    }
    
    public ArrayList<CmccN2TempEditItemInfo> getTempEditItems() {
        CmccN2TempEditItemInfo modelItem = null;
        ArrayList<CmccN2TempEditItemInfo> items = null;
        for (int k = 0; k < mCellCountX; k++) {
            for (int j = 0; j < mCellCountY; j++) {
                for (int i = 0; i < mMaxPageCount; i++) {
                    long tmpId = EditTargetCell[k][j][i];
                    if (tmpId > 0) {
                        if (items == null) {
                            items = new ArrayList<CmccN2TempEditItemInfo>();
                        }
                        modelItem = sItemsIdMap.get(tmpId);
                        items.add(modelItem);
                    }
                }
            }
        }
        return items;
    }
    
    private void buildListForScreen() {
        Log.d(TAG, "---Step 1 --- buildListForScreen--- ");
        mItems = getRevertItems();
        if (mItems.size() > 0) {
            sRevert.post(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "---Step 1.1 --- buildListForScreen To MESSAGE_REVERT_START with screen num--- ");
                    final long t = SystemClock.uptimeMillis();
                    for (CmccN2TempEditItemInfo mTmp : mItems) {
                        if (mTmp == null)
                            continue;
                        CmccN2TempEditItemInfo.CurrentCellInfo mCurrentInfo = mTmp.getCurrentCellInfo();
                        CmccN2TempEditItemInfo.OriginalCellInfo mOriInfo = mTmp.getOriginalItemInfo();

                        if (DEBUG_REVERT_ITEM_INFO) {
                            Log.i(TAG, "before buildListForScreen   ItemInfo  original" + " cell x y[x, y] ="
                                    + mOriInfo.cellX + ", " + mOriInfo.cellY + ", spanX =" + mOriInfo.spanX
                                    + ", spanY =" + mOriInfo.spanY + ", container =" + mOriInfo.container + ", screen ="
                                    + mOriInfo.screenId + ", originInfo.itemInfo.screen = " + mOriInfo.itemInfo.screenId
                                    + ", item.itemType =" + mOriInfo.itemType + ", item.id =" + mOriInfo.id);
                        }

                        String key = String.valueOf(-1);
                        if (mOriInfo.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
                            key = String.valueOf(mOriInfo.container);
                        } else {
                            key = String.valueOf(mOriInfo.screenId);
                        }

                        ArrayList<FinalCellInfo> mFinalList = sFinalListMap.get(key);
                        if (mFinalList == null) {
                            ArrayList<FinalCellInfo> mNewFinalList = new ArrayList<FinalCellInfo>();
                            FinalCellInfo mFinalItem = new FinalCellInfo(mOriInfo);

                            mNewFinalList.add(mFinalItem);
                            sFinalListMap.put(key, mNewFinalList);
                        } else {
                            FinalCellInfo mFinalItem = new FinalCellInfo(mOriInfo);
                            mFinalList.add(mFinalItem);
                        }
                    }
                    if (DEBUG_REVERT_ITEM_INFO) {
                        Iterator iter = sFinalListMap.entrySet().iterator();
                        while (iter.hasNext()) {
                            Entry<String, ArrayList<FinalCellInfo>> entry = (Entry<String, ArrayList<FinalCellInfo>>) iter
                                    .next();
                            String key = entry.getKey();
                            ArrayList<FinalCellInfo> modelList = entry.getValue();
                            if (modelList != null) {
                                Log.i(TAG, "BuildListForScreen  key =" + key);
                                for (FinalCellInfo tmp : modelList) {
                                    Log.i(TAG, "BuildListForScreen  ItemInfo  original" + " , key =" + key
                                            + ",cell x y[x, y] =" + tmp.cellX + ", " + tmp.cellY + ", spanX ="
                                            + tmp.spanX + ", spanY =" + tmp.spanY + ", container =" + tmp.container
                                            + ", screen =" + tmp.screenOrder + ", tmp.itemInfo.screen = "
                                            + tmp.itemInfo.screenId + ", item.info =" + tmp.itemInfo.id);
                                }
                            }
                        }
                    }
                    if (DEBUG_REVERT_INFO) {
                        Log.i(TAG, "buildListForScreen  =" + (SystemClock.uptimeMillis() - t) + "ms");
                    }
                    sRevert.sendMessage(sRevert.obtainMessage(MESSAGE_REVERT_START));
                }
            });
        } else if (mRemoveItems.size() > 0) {
            Log.d(TAG, "---Step 1.2 ---To  MESSAGE_REMOVE_ITEMS remove items--- ");
            sRevert.sendMessage(sRevert.obtainMessage(MESSAGE_REMOVE_ITEMS, MESSAGE_START, 0));
        } else {
            Log.d(TAG, "---Step 1.3 ---To  MESSAGE_END --- ");
            sRevert.sendMessage(sRevert.obtainMessage(MESSAGE_END));
        }
    }

    private void revertItems() {
        Log.i(TAG, "---Step 2 --- revert items to these own screennum --- ");
        if (sFinalListMap.size() == 0) {
            Log.i(TAG, "---Step 2.1 ---no revert items  --- ");
            sRevert.sendMessage(sRevert.obtainMessage(MESSAGE_TOBE_END, 0, 1));
            return;
        }
        Iterator iter = sFinalListMap.entrySet().iterator();
        while (iter.hasNext()) {
            Entry<String, ArrayList<FinalCellInfo>> entry = (Entry<String, ArrayList<FinalCellInfo>>) iter.next();
            final String key = entry.getKey();
            final ArrayList<FinalCellInfo> modelList = entry.getValue();
            sRevert.post(new Runnable() {
                @Override
                public void run() {
                    final long t = SystemClock.uptimeMillis();
                    if (modelList != null) {
                        for (FinalCellInfo finalInfo : modelList) {
                            if (DEBUG_REVERT_ITEM_INFO) {
                                Log.i(TAG, "revertItems  ItemInfo  original" + " , key =" + key + ",cell x y[x, y] ="
                                        + finalInfo.cellX + ", " + finalInfo.cellY + ", spanX =" + finalInfo.spanX
                                        + ", spanY =" + finalInfo.spanY + ", container =" + finalInfo.container
                                        + ", screen =" + finalInfo.screenOrder + ", finalInfo.itemInfo.screen = "
                                        + finalInfo.itemInfo.screenId + ", item.id =" + finalInfo.itemInfo.id
                                        + ", itme.type =" + finalInfo.itemInfo.itemType + ", item.title = "
                                        + finalInfo.itemInfo.title);
                            }
                            ItemInfo oriInfo = finalInfo.itemInfo;
                            boolean revert_success = false;
                            if (oriInfo.itemType == LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT
                                    || oriInfo.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION) {
                                if (finalInfo.container > 0) {
                                    // parent is folder
                                    mParentIsFolder.add(finalInfo);
                                    continue;
                                } else {
                                    revert_success = mLauncher.getWorkspace().revertApplicationShortcut(
                                            (ShortcutInfo) oriInfo, finalInfo.container, finalInfo.screenOrder,
                                            finalInfo.cellX, finalInfo.cellY);

                                    if (!revert_success) {
                                        if (DEBUG_REVERT_INFO) {
                                            Log.d(TAG, "revertItems item cant add to this screen");
                                        }
                                        mRearrangeItemsOrFolders.add(finalInfo);
                                    } else {
                                        finalInfo.bLasPosFound = true;
                                    }

                                }
                            } else if (oriInfo.itemType == LauncherSettings.Favorites.ITEM_TYPE_FOLDER) {

                            }
                        }
                        // List is loaded
                        if (DEBUG_REVERT_INFO) {
                            Log.i(TAG, "revertItems  key =" + key + " items in " + (SystemClock.uptimeMillis() - t)
                                    + "ms");
                        }
                        sRevert.sendMessage(sRevert.obtainMessage(MESSAGE_ONELIST_LOADED, MESSAGE_REVERT_START, 0));
                    }
                }
            });
        }
    }

    /**
     * revert all items to folders
     */
    private void revertItemsToFolders() {
        if (mParentIsFolder.size() != 0) {
            Log.d(TAG, "---Step 6.0 --- revert items to the parent screen --- ");
            sRevert.post(new Runnable() {
                @Override
                public void run() {
                    final long t = SystemClock.uptimeMillis();
                    mRearrangeItemsOrFolders.clear();
                    mRearrangeWidgets.clear();
                    for (FinalCellInfo finalInfo : mParentIsFolder) {
                        if (DEBUG_REVERT_ITEM_INFO) {
                            Log.i(TAG, "rearrangeItems  ItemInfo  item" + ",cell x y[x, y] =" + finalInfo.cellX + ", "
                                    + finalInfo.cellY + ", spanX =" + finalInfo.spanX + ", spanY =" + finalInfo.spanY
                                    + ", container =" + finalInfo.container + ", screen =" + finalInfo.screenOrder
                                    + ", item.id =" + finalInfo.itemInfo.id + ", itme.type ="
                                    + finalInfo.itemInfo.itemType + ", item.title =" + finalInfo.itemInfo.title);
                        }
                        ItemInfo oriInfo = finalInfo.itemInfo;
                        boolean revert_success = false;
                        if (oriInfo.itemType == LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT
                                || oriInfo.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION) {
                            revert_success = mLauncher.getWorkspace().revertApplicationShortcut((ShortcutInfo) oriInfo,
                                    finalInfo.container, finalInfo.screenOrder, finalInfo.cellX, finalInfo.cellY);

                            if (!revert_success) {
                                Log.d(TAG, "revertItems item cant add to this screen");
                                mRearrangeItemsOrFolders.add(finalInfo);
                            } else {
                                finalInfo.bLasPosFound = true;
                            }
                        }
                    }
                    // List is loaded
                    if (DEBUG_REVERT_INFO) {
                        Log.i(TAG, "rearrangeItemsOrFolders=" + (SystemClock.uptimeMillis() - t) + "ms");
                    }
                    sRevert.sendMessage(sRevert.obtainMessage(MESSAGE_CALCULATE_PARENTFOLDER_ITEMS));
                }
            });
        } else {
            sRevert.sendMessage(sRevert.obtainMessage(MESSAGE_TOBE_END, 0, 1));
        }
    }

    private ArrayList<CmccN2TempEditItemInfo> getRevertItems() {
        CmccN2TempEditItemInfo modelItem = null;
        for (int k = 0; k < mCellCountX; k++) {
            for (int j = 0; j < mCellCountY; j++) {
                for (int i = 0; i < mMaxPageCount; i++) {
                    long tmpId = EditTargetCell[k][j][i];
                    if (tmpId > 0) {
                        modelItem = sItemsIdMap.get(tmpId);
                        mItems.add(modelItem);
                    }
                }
            }
        }
        if (DEBUG_REVERT_INFO) {
            Log.d(TAG, "---getRevertItems --- mItems.size =" + mItems.size() + ", sItemsIdMap.size = " + sItemsIdMap.size());
        }
        return mItems;
    }

    private void reCalculateItems(final int state) {
        Log.i(TAG, "---Step 3 --- calutor the new place for the items which need to be rearranged --- ");

        if (mRearrangeItemsOrFolders.size() == 0 && mRearrangeWidgets.size() == 0) {
            if (mParentIsFolder.size() == 0 && state == MESSAGE_ONELIST_LOADED) {
                sRevert.sendMessage(sRevert.obtainMessage(MESSAGE_TOBE_END, state, 1));
                return;
            } else if (mParentIsFolder.size() > 0 && state == MESSAGE_ONELIST_LOADED) {
                sRevert.sendMessage(sRevert.obtainMessage(MESSAGE_REVERT_PARENTFOLDER_ITEMS));
                return;
            } else if (state == MESSAGE_CALCULATE_PARENTFOLDER_ITEMS) {
                sRevert.sendMessage(sRevert.obtainMessage(MESSAGE_TOBE_END, 0, 1));
            }
        } else {
            sRevert.post(new Runnable() {
                @Override
                public void run() {
                    final long t = SystemClock.uptimeMillis();
                    if (DEBUG_REVERT_INFO) {
                        if (mRearrangeItemsOrFolders.size() > 0) {
                            for (FinalCellInfo finalInfo : mRearrangeItemsOrFolders) {
                                if (DEBUG_REVERT_ITEM_INFO) {
                                    Log.i(TAG, "reCalculateItems  ItemInfo before arrange" + ",cell x y[x, y] ="
                                            + finalInfo.cellX + ", " + finalInfo.cellY + ", spanX =" + finalInfo.spanX
                                            + ", spanY =" + finalInfo.spanY + ", container =" + finalInfo.container
                                            + ", screen =" + finalInfo.screenOrder + ", item.id ="
                                            + finalInfo.itemInfo.id + ", itme.type =" + finalInfo.itemInfo.itemType
                                            + ", item.title = " + finalInfo.itemInfo.title);
                                }
                            }
                        }
                    }

                    int[] lasPos = new int[3];
                    int occupiedCount = mLauncher.getWorkspace().getLastPosition(lasPos);
                    int lastScreen = lasPos[2];
                    DeviceProfile profile = LauncherAppState.getInstance().getDynamicGrid().getDeviceProfile();
                    int cellX = (int) profile.numColumns;
                    int cellY = (int) profile.numRows;
                    int maxNum = cellX * cellY ;
                    if (occupiedCount == maxNum) {
                        // If the last screen is full ,add new screen;
                        // And if the mRearrangeItemsOrFolders is 0, the widgets
                        // is use this
                        occupiedCount = 0;
                        lastScreen++;
                        mLauncher.getWorkspace().addNewScreen(lastScreen);
                        lasPos[0] = 0;
                        lasPos[1] = 0;
                    }

                    for (int i = 0; i < mRearrangeItemsOrFolders.size(); i++) {
                        // for (FinalCellInfo finalInfo :
                        // mRearrangeItemsOrFolders) {
                        FinalCellInfo finalInfo = mRearrangeItemsOrFolders.get(i);
                        if (occupiedCount == maxNum) {
                            // If the screen is full ,add more one screen
                            occupiedCount = 0;
                            lastScreen++;
                            mLauncher.getWorkspace().addNewScreen(lastScreen);
                            lasPos[0] = 0;
                            lasPos[1] = 0;
                        }
                        finalInfo.container = LauncherSettings.Favorites.CONTAINER_DESKTOP;
                        finalInfo.screenOrder = mLauncher.getWorkspace().getScreenIdForPageIndex(lastScreen);
                        finalInfo.cellX = lasPos[0];
                        finalInfo.cellY = lasPos[1];
                        finalInfo.bLasPosFound = true;
                        occupiedCount++;
                        // Reset the lasPos
                        lasPos[0] = occupiedCount % cellY;
                        lasPos[1] = (occupiedCount / cellX) % cellY;
                        if ((occupiedCount == 16) && (i == mRearrangeItemsOrFolders.size() - 1)
                                && (mRearrangeWidgets.size() > 0)) {
                            lastScreen++;
                            mLauncher.getWorkspace().addNewScreen(lastScreen);
                        }
                        if (DEBUG_REVERT_ITEM_INFO) {
                            Log.i(TAG,
                                    "reCalculateItems  ItemInfo  after arrange" + ",cell x y[x, y] =" + finalInfo.cellX
                                            + ", " + finalInfo.cellY + ", spanX =" + finalInfo.spanX + ", spanY ="
                                            + finalInfo.spanY + ", container =" + finalInfo.container + ", screen ="
                                            + finalInfo.screenOrder + ", item.id =" + finalInfo.itemInfo.id
                                            + ", itme.type =" + finalInfo.itemInfo.itemType + ", item.title = "
                                            + finalInfo.itemInfo.title);
                        }
                    }
                    // reCalculateWidgets(lastScreen, lasPos);
                    sRevert.sendMessage(sRevert.obtainMessage(MESSAGE_REARRANGE_START, state, 0));
                    if (DEBUG_REVERT_INFO) {
                        Log.i(TAG, "reCalculateItems=" + (SystemClock.uptimeMillis() - t) + "ms");
                    }
                }
            });
        }
    }

    /**
     * rearrangeItemsOrFolders whitch are not find the position
     * 
     * @param state
     */
    private void rearrangeItemsOrFolders(final int state) {
        if (mRearrangeItemsOrFolders.size() != 0) {
            Log.d(TAG, "---Step 5.2 --- rearrange items to the last scree --- ");
            sRevert.post(new Runnable() {
                @Override
                public void run() {
                    final long t = SystemClock.uptimeMillis();
                    for (FinalCellInfo finalInfo : mRearrangeItemsOrFolders) {
                        if (DEBUG_REVERT_ITEM_INFO) {
                            Log.i(TAG, "rearrangeItems  ItemInfo  item" + ",cell x y[x, y] =" + finalInfo.cellX + ", "
                                    + finalInfo.cellY + ", spanX =" + finalInfo.spanX + ", spanY =" + finalInfo.spanY
                                    + ", container =" + finalInfo.container + ", screen =" + finalInfo.screenOrder
                                    + ", item.id =" + finalInfo.itemInfo.id + ", itme.type ="
                                    + finalInfo.itemInfo.itemType + ", item.title =" + finalInfo.itemInfo.title);
                        }
                        ItemInfo oriInfo = finalInfo.itemInfo;
                        boolean revert_success = false;
                        if (oriInfo.itemType == LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT
                                || oriInfo.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION) {
                            revert_success = mLauncher.getWorkspace().revertApplicationShortcut((ShortcutInfo) oriInfo,
                                    finalInfo.container, finalInfo.screenOrder, finalInfo.cellX, finalInfo.cellY);
                        } else if (oriInfo.itemType == LauncherSettings.Favorites.ITEM_TYPE_FOLDER) {
                            /*
                             * revert_success =
                             * mLauncher.getWorkspace().revertFolder((
                             * FolderInfo) oriInfo, finalInfo.container,
                             * finalInfo.screenOrder, finalInfo.cellX,
                             * finalInfo.cellY);
                             */
                        }
                    }
                    // List is loaded
                    if (DEBUG_REVERT_INFO) {
                        Log.i(TAG, "rearrangeItemsOrFolders=" + (SystemClock.uptimeMillis() - t) + "ms");
                    }
                    mRearrangeItemsOrFolders.clear();
                    sRevert.sendMessage(sRevert.obtainMessage(MESSAGE_REARRANGE_END, state, 0));
                }
            });
        } else {
            sRevert.sendMessage(sRevert.obtainMessage(MESSAGE_REARRANGE_END, state, 0));
        }
    }

    static final class FinalCellInfo {
        ItemInfo itemInfo;
        int cellX = -1;
        int cellY = -1;
        int spanX;
        int spanY;
        // Record the SreenOrder
        long screenOrder;
        long container;

        boolean bLasPosFound;

        FinalCellInfo(CmccN2TempEditItemInfo.OriginalCellInfo oriInfo) {
            itemInfo = oriInfo.itemInfo;
            cellX = oriInfo.cellX;
            cellY = oriInfo.cellY;
            spanX = oriInfo.spanX;
            spanY = oriInfo.spanY;

            screenOrder = oriInfo.screenId;
            container = oriInfo.container;

            bLasPosFound = false;
        }

        @Override
        public String toString() {
            return "Cell[view=" + (itemInfo == null ? "null" : itemInfo.getClass()) + ", x=" + cellX + ", y=" + cellY
                    + "]";
        }
    }
    /*bug 180264 tangzhongfeng.wt ADD 20160525 start */
    void checkMapIdAndItems(int x, int y, int sc, long id) {
        CmccN2TempEditItemInfo info = sItemsIdMap.get(id);
        if (info != null) {
            info.resetCurrentCellInfo(id, sc, x, y, LauncherSettings.Favorites.CONTAINER_TEMPEDIT);
            setToMap(x, y, sc, id);
        } else {
            Log.i(TAG, "tzf x = " + x + ", y = " + y + ", id = " + id + " null");
        }
    }
    /*bug 180264 tangzhongfeng.wt ADD 20160525 end */
}
