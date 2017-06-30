package com.android.launcher3;
/*
 * Copyright (C) 2016 Wingtech Group.
 * Modification based on code covered by the mentioned copyright
 * and/or permission notice(s).
 */

/*
 * Copyright (C) 2016 The CMCC N2 Project
 * the temp edit items
 *
 */
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.View;

public class CmccN2TempEditItemInfo {
    private static final String TAG = "CmccN2TempEditItemInfo";
    private static boolean DEBUG = true;

    private Context mContext;
    private CurrentCellInfo mCurrentCellInfo = new CurrentCellInfo();
    private OriginalCellInfo mOriginalItemInfo = new OriginalCellInfo();

    static final int NO_ID = -1;

    public long itemID = NO_ID;

    public CmccN2TempEditItemInfo(Context context, long id) {
        mContext = context;
        itemID = id;
    }

    public CmccN2TempEditItemInfo(Context context, long id, ItemInfo itemInfo) {
        mContext = context;
        itemID = id;
        itemInfo.tempId = id;
        mOriginalItemInfo.itemInfo = itemInfo;
    }

    public void setOringnalInfo(CmccN2TempEditItemInfo.OriginalCellInfo info) {
        if (DEBUG)
            Log.d(TAG, "setOringnalInfo by info");

        if (null == mOriginalItemInfo) {
            mOriginalItemInfo = new OriginalCellInfo();
        }

        mOriginalItemInfo.itemID = itemID;

        mOriginalItemInfo.cellX = info.cellX;
        mOriginalItemInfo.cellY = info.cellY;
        mOriginalItemInfo.spanX = info.spanX;
        mOriginalItemInfo.spanY = info.spanY;
        mOriginalItemInfo.container = info.container;
        mOriginalItemInfo.screenId = info.screenId;
        mOriginalItemInfo.itemType = info.itemType;
        mOriginalItemInfo.view = info.view;
        mOriginalItemInfo.itemInfo = info.itemInfo;
        mOriginalItemInfo.dragSource = info.dragSource;
        Log.d(TAG, "CmccN2TempEditItemInfo setOringnalInfo screenId = " + mOriginalItemInfo.screenId);
        if (info.itemInfo instanceof ShortcutInfo) {
            mOriginalItemInfo.intent = ((ShortcutInfo) info.itemInfo).intent;
        } else if (info.itemInfo instanceof FolderInfo) {
        }

    }

    public void setOringnalInfo(ItemInfo itemInfo, View view, DragSource dragSource) {
        itemInfo.setTempId(itemID);

        if (null == mOriginalItemInfo) {
            mOriginalItemInfo = new OriginalCellInfo();
        }

        mOriginalItemInfo.itemID = itemID;

        mOriginalItemInfo.cellX = itemInfo.cellX;
        mOriginalItemInfo.cellY = itemInfo.cellY;
        mOriginalItemInfo.spanY = itemInfo.spanX;
        mOriginalItemInfo.spanY = itemInfo.spanY;
        mOriginalItemInfo.container = itemInfo.container;
        mOriginalItemInfo.screenId = itemInfo.screenId;
        mOriginalItemInfo.itemType = itemInfo.itemType;
        mOriginalItemInfo.view = view;
        mOriginalItemInfo.itemInfo = itemInfo;
        mOriginalItemInfo.dragSource = dragSource;

        if (itemInfo instanceof ShortcutInfo) {
            mOriginalItemInfo.intent = ((ShortcutInfo) itemInfo).intent;
        } else if (itemInfo instanceof FolderInfo) {
        }
    }

    public void setOringnalCellInfo(int cellX, int cellY, int spanX, int spanY, long screenId, long container) {
        mOriginalItemInfo.cellX = cellX;
        mOriginalItemInfo.cellY = cellY;
        mOriginalItemInfo.spanX = spanX;
        mOriginalItemInfo.spanY = spanY;
        mOriginalItemInfo.container = container;
        mOriginalItemInfo.screenId = screenId;
    }

    public void reSetOringalCellInfo(int cellX, int cellY, long screenId) {
        mOriginalItemInfo.cellX = cellX;
        mOriginalItemInfo.cellY = cellY;
        mOriginalItemInfo.screenId = screenId;
    }

    public void setCurrentCellInfo(CmccN2TempEditItemInfo.CurrentCellInfo info) {
        if (null == mCurrentCellInfo) {
            mCurrentCellInfo = new CurrentCellInfo();
        }

        mCurrentCellInfo.itemID = itemID;

        mCurrentCellInfo.cellX = info.cellX;
        mCurrentCellInfo.cellY = info.cellY;
        mCurrentCellInfo.container = info.container;
        mCurrentCellInfo.screen = info.screen;
        mCurrentCellInfo.itemType = info.itemType;
    }

    public void setCurrentCellInfo(int screen, int x, int y, long Container, int itemType) {
        if (null == mCurrentCellInfo) {
            mCurrentCellInfo = new CurrentCellInfo();
        }

        mCurrentCellInfo.itemID = itemID;

        mCurrentCellInfo.cellX = x;
        mCurrentCellInfo.cellY = y;
        mCurrentCellInfo.container = Container;
        mCurrentCellInfo.screen = screen;
        mCurrentCellInfo.itemType = itemType;
    }

    public boolean resetCurrentCellInfo(long id, int screen, int x, int y, long Container) {
        if (id != mCurrentCellInfo.itemID)
            return false;

        mCurrentCellInfo.resetInfo(id, screen, x, y, Container);
        return true;
    }

    public void setParentId(long id) {
        mCurrentCellInfo.parentID = id;
    }

    public long getParentId() {
        return mCurrentCellInfo.parentID;
    }

    public void setInfo(ItemInfo itemInfo) {
        itemInfo.tempId = itemID;
        mOriginalItemInfo.itemInfo = itemInfo;
    }

    public void resetItemID() {
        if (null == mOriginalItemInfo.itemInfo) {
            return;
        }
        mOriginalItemInfo.itemInfo.tempId = NO_ID;
    }

    public CmccN2TempEditItemInfo.CurrentCellInfo getCurrentCellInfo() {
        return mCurrentCellInfo;
    }

    public CmccN2TempEditItemInfo.OriginalCellInfo getOriginalItemInfo() {
        return mOriginalItemInfo;
    }

    static final class OriginalCellInfo extends ItemInfo {
        Intent intent;
        Bitmap iconBitmap;
        ComponentName componentName;
        ItemInfo itemInfo;
        View view;
        DragSource dragSource;

        long itemID = NO_ID;

        public void setInfo(ItemInfo itemInfo, View view, DragSource dragSource) {

            if (DEBUG)
                Log.d(TAG, "Original Info set Info");

            this.cellX = itemInfo.cellX;
            this.cellY = itemInfo.cellY;
            this.cellX = itemInfo.spanX;
            this.cellY = itemInfo.spanY;
            this.container = itemInfo.container;
            this.screenId = itemInfo.screenId;
            this.itemType = itemInfo.itemType;
            this.view = view;
            this.itemInfo = itemInfo;
            this.dragSource = dragSource;

            if (itemInfo instanceof ShortcutInfo) {
                this.intent = ((ShortcutInfo) itemInfo).intent;
            } else if (itemInfo instanceof FolderInfo) {
            }
        }

    }

    static final class CurrentCellInfo extends ItemInfo {
        long itemID = NO_ID;
        long parentID = NO_ID;
        int screen;

        public void setInfo(int screen, int x, int y, long Container, int itemType) {
            if (DEBUG)
                Log.d(TAG, "CurrentCell Info set Info");

            this.cellX = x;
            this.cellY = y;
            this.container = Container;
            this.screen = screen;
            this.itemType = itemType;
        }

        public boolean resetInfo(long id, int screen, int x, int y, long Container) {
            if (id != this.itemID)
                return false;

            this.cellX = x;
            this.cellY = y;
            this.container = Container;
            this.screen = screen;

            return true;
        }

        public void setParentId(long id) {
            parentID = id;
        }

        public long getParentId() {
            return parentID;
        }

    }

}
