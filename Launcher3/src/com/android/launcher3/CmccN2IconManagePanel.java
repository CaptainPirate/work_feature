/*
 * Copyright (C) 2011 The Android Open Source Project
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
/*******************************************************************************************************
|   when     |      who     |    keyword       |        why        |     what                          |
********************************************************************************************************
20160426     tangzhongfeng.wt   bug 167159          requirements       
20160426     tangzhongfeng.wt   bug 168965          other
20160427     tangzhongfeng.wt   bug 169462          other
20160504     tangzhongfeng.wt   bug 171954          bug
20160504     tangzhongfeng.wt   bug 171955          bug
20160824       liyichong.wt     bug 210609          bug
******************************************************************************************************/
package com.android.launcher3;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import com.android.launcher3.CmccN2TempEditItemInfo.CurrentCellInfo;
import com.android.launcher3.DragController.DragListener;
import com.android.launcher3.FolderInfo.FolderListener;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.android.launcher3.PagedView.PageSwitchListener;
public class CmccN2IconManagePanel extends SmoothPagedView
        implements DragListener, DragScroller, DragSource, DropTarget, View.OnClickListener, View.OnLongClickListener, PageSwitchListener {
    private static final String TAG = "CmccN2IconManagePanel";

    private Launcher mLauncher;
    private DragController mDragController;
    private final LayoutInflater mLayoutInflater;
    private CellLayout mCurrentScreen;

    private int mCellCountX;
    private int mCellCountY;
    private int mMaxPageCount;
    private int mCellIconSize;
    private Drawable iconBg;
    private CmccN2TempEditItemManager mTempItemsManager;

    private DropTarget.DragEnforcer mDragEnforcer;
    private Bitmap mDragOutline;
    private float[] mDragViewVisualCenter = new float[2];
    private Matrix mTempInverseMatrix;
    private int[] mTargetCell = new int[2];
    private int mDragOverX = -1;
    private int mDragOverY = -1;

    // Related to dragging, folder creation and reordering
    private static final int DRAG_MODE_NONE = 0;
    private static final int DRAG_MODE_CREATE_FOLDER = 1;
    private static final int DRAG_MODE_ADD_TO_FOLDER = 2;
    private static final int DRAG_MODE_REORDER = 3;
    private int mDragMode = DRAG_MODE_NONE;
    private int mLastReorderX = -1;
    private int mLastReorderY = -1;

    public int[] mOpenedCell = new int[2];
    public CellLayout mOpenedCellLayout = null;
    public DragObject mOpenedDragObject = null;
    public View mOpenedView = null;

    private int[] mPreviousTargetCell = new int[2];
    private Alarm mReorderAlarm = new Alarm();
    private Alarm mOnExitAlarm = new Alarm();
    private int[] mEmptyCell = new int[2];
    public static final int DRAG_BITMAP_PADDING = 2;

    private final int[] mTempXY = new int[2];

    /**
     * The CellLayout that is currently being dragged over
     */
    private CellLayout mDragTargetLayout = null;
    /**
     * The CellLayout which will be dropped to
     */
    private CellLayout mDropToLayout = null;

    /**
     * CellInfo for the cell that is currently being dragged
     */
    public CellLayout.CellInfo mDragInfo;

    private int mAddScreenAnimationTime;

    private Runnable mDelayedSnapToPageRunnable;
    
    public CmccN2IconManagePanel(Context context) {
        this(context, null);
    }

    public CmccN2IconManagePanel(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CmccN2IconManagePanel(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        //bug 171955 tangzhognfeng.wt ADD 20160504
        mContentIsRefreshable = false;
        
        mLayoutInflater = LayoutInflater.from(context);

        Resources res = context.getResources();
        mCellCountX = res.getInteger(R.integer.tempedit_cell_count_x);
        mCellCountY = res.getInteger(R.integer.tempedit_cell_count_y);
        mMaxPageCount = res.getInteger(R.integer.tempedit_max_page);
        mCellIconSize = res.getDimensionPixelSize(R.dimen.tempscreen_cell_width);
        mAddScreenAnimationTime = res.getInteger(R.integer.config_mAddScreenAnimationTime);
        iconBg = res.getDrawable(R.drawable.temp_edit_icon_bg);

        mTempItemsManager = CmccN2TempEditItemManager.getInstance(context);

        mDragEnforcer = new DropTarget.DragEnforcer(context);

        mCurrentScreen = getFirstScreen();
        mCurrentScreen.getShortcutsAndWidgets().setMotionEventSplittingEnabled(false);

        mOpenedCell[0] = -1;
        mOpenedCell[1] = -1;
        setPageSwitchListener(this);
    }
    
    public void enterFolderTempEdit(int currScreen, int maxScreen) {
        if (LauncherAppState.getInstance().getLauncherInEdit()) {
            setVisibility(View.VISIBLE);
            updateIconManagePanel(currScreen, maxScreen);
        }
    }
    public void updateIconManagePanel(int currScreen, int maxScreen) {
        finalizeEdit();
        /* bug 210609, liyichong.wt, ADD 20160824 start */
        if(getPageAt(0) != null){
            getPageAt(0).requestLayout();
        }
        /* bug 210609, liyichong.wt, ADD 20160824 end */
        ArrayList<CmccN2TempEditItemInfo> items = mTempItemsManager.getTempEditItems();
        if (items != null && items.size() > 0) {
            /*bug 169462 tangzhongfeng.wt  ADD start */
            currScreen = currScreen > maxScreen ? 0 : currScreen;
            Log.i(TAG, "tzf updateIconManagePanel maxScreen = " + maxScreen + ", currScreen = " + currScreen);
            //add sc
            for (int i = getPageCount(); i < maxScreen; i++) {//bug 204480, liyichong.wt, MODIFY 20160816
                addScreen();
            }
            for (CmccN2TempEditItemInfo item : items) {
                CurrentCellInfo info = item.getCurrentCellInfo();
                addItemInScreenLayout(item.getOriginalItemInfo().itemInfo, info.cellX, info.screen);
            }
            final int sc = currScreen;
            final Runnable onSnapToScreenRunnable = new Runnable() {
                @Override
                public void run() {
                    snapToPage(sc, null);
                }
            };
            /* bug 210609, liyichong.wt, DELETE 20160824 start */
            //if (currScreen > 0) {
                postDelayed(onSnapToScreenRunnable, 100);
            //}
            /* bug 210609, liyichong.wt, DELETE 20160824 end */
            /*bug 169462 tangzhongfeng.wt  ADD end */
        }
    }
    public void exitFolderTempEdit() {
        finalizeEdit();
    }
    
    public void setup(Launcher launcher, DragController dragController) {
        mLauncher = launcher;
        mDragController = dragController;
    }

    protected CmccN2TempEditCellLayout addScreen() {
        return addScreen(getChildCount());
    }
    protected CmccN2TempEditCellLayout addScreen(int screen) {
        CmccN2TempEditCellLayout newScreen = (CmccN2TempEditCellLayout) mLayoutInflater
                .inflate(R.layout.tempedit_screen, this, false);
        PagedView.LayoutParams lp = new LayoutParams(newScreen.getLayoutParams());
        newScreen.setLayoutParams(lp);
        newScreen.setGridSize(mCellCountX, mCellCountY);
        newScreen.setIsIconManage(true);
        addView(newScreen, screen);
        requestLayout();
        return newScreen;
    }

    private CmccN2TempEditCellLayout getFirstScreen() {
        CmccN2TempEditCellLayout currentScreen = (CmccN2TempEditCellLayout) getPageAt(0);
        if (currentScreen == null) {
            currentScreen = addScreen();
        }
        return currentScreen;
    }

    protected CellLayout getCurrentScreen() {
        //mCurrentScreen = (CmccN2TempEditCellLayout) getPageAt(getCurrentPage());
        return mCurrentScreen;
    }
    
    protected CmccN2TempEditCellLayout getLayoutAtScreen(int screen) {
        return (CmccN2TempEditCellLayout) getPageAt(screen);
    }

    /**
     * Returns a list of all the CellLayouts in the workspace.
     */
    ArrayList<CellLayout> getAllCellLayouts() {
        ArrayList<CellLayout> layouts = new ArrayList<CellLayout>();
        int screenCount = getChildCount();
        for (int screen = 0; screen < screenCount; screen++) {
            layouts.add(((CellLayout) getChildAt(screen)));
        }
        return layouts;
    }

    /**
     * Returns a specific CellLayout
     */
    CellLayout getParentCellLayoutForView(View v) {
        ArrayList<CellLayout> layouts = getAllCellLayouts();
        for (CellLayout layout : layouts) {
            if (layout.getShortcutsAndWidgets().indexOfChild(v) > -1) {
                return layout;
            }
        }
        return null;
    }

    public void clearAllCellLayout() {
        int pageCount = this.getPageCount();
        Log.i(TAG, "pageCount = " + pageCount);
        for (int i = 0; i < pageCount; i++) {
            CmccN2TempEditCellLayout page = (CmccN2TempEditCellLayout) getPageAt(i);
            page.removeAllViews();
        }
    }

    public void enterEdit(ItemInfo itemInfo) {
        mTempItemsManager.initialize(mLauncher);
        addItem(itemInfo);
    }
    
    public void exitEdit() {
        Log.i(TAG, "exitEdit");
        mLauncher.closeFolder();
        mTempItemsManager.revertItemsToWorkspace();
        // finalizeEdit();
    }

    public void finalizeEdit() {
        Log.i(TAG, "finalizeEdit");
        int page = getPageCount();
        clearAllCellLayout();
        if (page > 1) {
            removeViews(1, page - 1);
        }
        //bug 212232 tangzhongfengh.wt ADD 20160826
        mCurrentScreen = (CellLayout) getPageAt(0);
        mCurrentPage = 0;
    }
    public void addItem(ItemInfo item) {
        
        final ItemInfo itemInfo = item;
        final int[] xy = getFirstPosition();
        //bug 180356 tangzhongfeng.wt MODIFY 20160525
        addItemInScreenLayout(itemInfo, xy[0], xy[2]);
        
        final Runnable onSnapToNewScreenRunnable = new Runnable() {
            @Override
            public void run() {
                snapToPage(getChildCount() - 1, null);
            }
        };
        final Runnable onAddScreenRunnable = new Runnable() {
            @Override
            public void run() {
                int nextPage = findNextEmptyLocationPage(mCurrentPage, getChildCount());
                if (nextPage > 0 && nextPage < mMaxPageCount) {
                    snapToPage(nextPage, mAddScreenAnimationTime);
                } else if (nextPage == -2) {
                    addScreen();
                    postDelayed(onSnapToNewScreenRunnable, 100);
                } else {
                    // do nothing
                }
            }
        };
        
        final Runnable onCompleteRunnable = new Runnable() {
            @Override
            public void run() {
                final CmccN2TempEditItemInfo.OriginalCellInfo mOriginalInfo = new CmccN2TempEditItemInfo.OriginalCellInfo();
                final CmccN2TempEditItemInfo.CurrentCellInfo mCurrentInfo = new CmccN2TempEditItemInfo.CurrentCellInfo();
                mOriginalInfo.cellX = itemInfo.cellX;
                mOriginalInfo.cellY = itemInfo.cellY;
                mOriginalInfo.spanX = itemInfo.spanX;
                mOriginalInfo.spanY = itemInfo.spanY;
                mOriginalInfo.container = itemInfo.container;
                mOriginalInfo.screenId = itemInfo.screenId;
                mOriginalInfo.itemInfo = itemInfo;
                //bug 180356 tangzhongfeng.wt MODIFY 20160525
                mCurrentInfo.setInfo(xy[2], xy[0], 0, LauncherSettings.Favorites.CONTAINER_TEMPEDIT, itemInfo.itemType);
                Log.i(TAG, "enterEdit screenId = " + mOriginalInfo.screenId);
                mTempItemsManager.addItem(itemInfo, mCurrentInfo, mOriginalInfo);
                // add a new screen
                if (!mCurrentScreen.existsEmptyCell() && mCurrentPage <= mMaxPageCount) {
                    if (onAddScreenRunnable != null) {
                        onAddScreenRunnable.run();
                    }
                }
            }
        };
        
        onCompleteRunnable.run();
    }
    @Override
    public void syncPages() {
        // TODO Auto-generated method stub

    }

    @Override
    public void syncPageItems(int page, boolean immediate) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean isDropEnabled() {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public void onDrop(DragObject d) {

        Log.d(TAG, "---------onDrop---------------- ");

        mDragViewVisualCenter = mLauncher.getWorkspace().getDragViewVisualCenter(d.x, d.y, d.xOffset, d.yOffset,
                d.dragView, mDragViewVisualCenter);
        CellLayout dropTargetLayout = mDropToLayout;

        if (dropTargetLayout != null) {
            mapPointFromSelfToChild(dropTargetLayout, mDragViewVisualCenter, null);
        }

        if (d.dragSource != this) {
            final int[] touchXY = new int[] { (int) mDragViewVisualCenter[0], (int) mDragViewVisualCenter[1] };
            onDropExternal(touchXY, dropTargetLayout, true, d);
        } else if (mDragInfo != null) {
            onDropInternal(dropTargetLayout, d);
        }
    }

    // -1 : for doing nothing
    // -2 : for adding a new screen, and snap to it
    // [1 ~ mMaxPageCount] : snap to it
    private int findNextEmptyLocationPage(int currPageIndex, int currPageCount) {
        int retPage = -1;
        int index = currPageIndex + 1;
        if (currPageIndex == currPageCount - 1) {
            retPage = -2; // for add new page
        } else {
            for (index = currPageIndex + 1; index < currPageCount; index++) {
                CellLayout c = (CellLayout) getChildAt(index);
                if (c.existsEmptyCell()) {
                    retPage = index;
                    break;
                }
            }
        }
        if (index == currPageCount) {
            if (index < mMaxPageCount) {
                retPage = -2;
            } else {
                retPage = mMaxPageCount - 1;
            }
        }
        Log.i(TAG, "index=" + index + ", currPageCount=" + currPageCount + ", retPage=" + retPage);
        return retPage;
    }

    /**
     * Drop an item that didn't belong to any screen of the temp edit screens.
     * It may have come from Workspace or temp folder
     * 
     * NOTE: This can also be called when we are outside of a drag event, when
     * we want to add an item to one of the workspace screens.
     */
    private void onDropExternal(final int[] touchXY, final CellLayout cellLayout, boolean insertAtFirst, DragObject d) {
        final ItemInfo dragInfo = (ItemInfo) d.dragInfo;
        final CmccN2TempEditItemInfo.OriginalCellInfo mOriginalInfo = new CmccN2TempEditItemInfo.OriginalCellInfo();
        final CmccN2TempEditItemInfo.CurrentCellInfo mCurrentInfo = new CmccN2TempEditItemInfo.CurrentCellInfo();
        final View dragView = d.dragView;
        final DragSource dragSource = d.dragSource;
        final int screen = getCurrentPage();
        final int pageCount = this.getChildCount();
        int spanX = 1;
        int spanY = 1;
        final long container = LauncherSettings.Favorites.CONTAINER_TEMPEDIT;

        View view = null;
        final long finalTempId = dragInfo.tempId;

        PendingAddItemInfo mPendingInfo = null;

        if (finalTempId <= 0) {
            // Record the origin info
            Log.i(TAG, "onDropExternal finalTempId =" + finalTempId);
            mOriginalInfo.setInfo(dragInfo, dragView, dragSource);
            mOriginalInfo.cellX = dragInfo.cellX;
            mOriginalInfo.cellY = dragInfo.cellY;
            mOriginalInfo.spanX = dragInfo.spanX;
            mOriginalInfo.spanY = dragInfo.spanY;
            mOriginalInfo.container = dragInfo.container;
            mOriginalInfo.screenId = dragInfo.screenId;
            Log.i(TAG, "tzf onDropExternal dragInfo.sreenId = " + dragInfo.screenId);
        }

        final Runnable onAddScreenRunnable = new Runnable() {
            @Override
            public void run() {
                int nextPage = findNextEmptyLocationPage(mCurrentPage, getChildCount());
                if (nextPage > 0 && nextPage < mMaxPageCount) {
                    snapToPage(nextPage, mAddScreenAnimationTime);
                } else if (nextPage == -2) {
                    addScreen();
                    snapToPage(getChildCount() - 1, mAddScreenAnimationTime);
                } else {
                    // do nothing
                }
            }
        };
        final Runnable onCompleteRunnable = new Runnable() {
            @Override
            public void run() {
                boolean bAddNew = true;
                if (dragSource instanceof Folder) {
                    Folder mFolder = (Folder) dragSource;
                    ItemInfo mInfo = mFolder.getInfo();
                } else if (dragSource instanceof Workspace) {
                    bAddNew = true;
                }

                mCurrentInfo.setInfo(screen, mTargetCell[0], mTargetCell[1],
                        LauncherSettings.Favorites.CONTAINER_TEMPEDIT, dragInfo.itemType);
                final ItemInfo itemInfo = dragInfo;
                mTempItemsManager.addOrMoveItem(finalTempId, itemInfo, mCurrentInfo, mOriginalInfo, null);
                //bug 180264 tangzhonggfeng.wt Add 20160525
                checkMapIdAndItems();
                // add a new screen
                if (!cellLayout.existsEmptyCell() && pageCount <= mMaxPageCount && bAddNew) {
                    if (onAddScreenRunnable != null) {
                        onAddScreenRunnable.run();
                    }
                }
            }
        };

        mTargetCell = findNearestArea((int) touchXY[0], (int) touchXY[1], spanX, spanY, cellLayout, mTargetCell);
        float distance = cellLayout.getDistanceFromCell(mDragViewVisualCenter[0], mDragViewVisualCenter[1],
                mTargetCell);

        // find if can add or creat an temp folder
        boolean findNearestVacantCell = true;
        /*
         * if (dragInfo.itemType ==
         * LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT || dragInfo.itemType ==
         * LauncherSettings.Favorites.ITEM_TYPE_APPLICATION) { if
         * (willCreateTempFolder(dragInfo, cellLayout, mTargetCell, distance,
         * false) || willAddToExistingTempFolder(dragInfo, cellLayout,
         * mTargetCell, distance)) { findNearestVacantCell = false; } }
         */
        switch (dragInfo.itemType) {
        case LauncherSettings.Favorites.ITEM_TYPE_APPLICATION:
        case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT:
            view = mLauncher.createShortcut(R.layout.application, cellLayout, (ShortcutInfo) dragInfo);
            if (null == view) {
                Log.e(TAG, "create createShortcut failure");
            }
            break;
        case LauncherSettings.Favorites.ITEM_TYPE_FOLDER:
            break;
        default:
            throw new IllegalStateException("Unknown item type: " + dragInfo.itemType);
        }

        // Add to temp folder or creat an temp folder
        if (!findNearestVacantCell) {
            /*
             * if (createTempFolderIfNecessary(view, container, cellLayout,
             * mTargetCell, true, d.dragView, d, d.postAnimationRunnable)) {
             * return; }
             * 
             * if (addToExistingFolderIfNecessary(view, cellLayout, mTargetCell,
             * distance, d, true)) { return; }
             */
        } else {
            // Add or Move one cell to empty cell
            if (touchXY != null) {
                // when dragging and dropping, just find the closest free spot
                mTargetCell = cellLayout.performReorder((int) mDragViewVisualCenter[0], (int) mDragViewVisualCenter[1],
                        1, 1, 1, 1, null, mTargetCell, null, CellLayout.MODE_ON_DROP_EXTERNAL);
            } else {
                cellLayout.findCellForSpan(mTargetCell, 1, 1);
            }

            boolean ret = addInScreenLayout(view, container, screen, mTargetCell[0], mTargetCell[1], 1, 1, true, true);

            if (ret) {
                cellLayout.onDropChild(view);
                CellLayout.LayoutParams lp = (CellLayout.LayoutParams) view.getLayoutParams();
                cellLayout.getShortcutsAndWidgets().measureChild(view);

                if (d.dragSource instanceof Workspace) {
                    // LauncherModel.addOrMoveItemInDatabase(mLauncher,
                    // dragInfo, container, screen, lp.cellX, lp.cellY);
                }
                if (d.dragView != null) {
                    // mLauncher.getDragLayer().animateViewIntoPosition(d.dragView,
                    // view, onCompleteRunnable);
                    //animateViewIntoPosition(d.dragView, view, onCompleteRunnable, -1);
                    mLauncher.getDragLayer().animateViewIntoPosition(d.dragView, view, onCompleteRunnable, null);
                }
            } else {
                throwBack(d);
                if (d.dragView != null) {
                    d.dragView.remove();
                }
            }
        }
    }

    private void onDropInternal(final CellLayout dropTargetLayout, DragObject d) {
        onDropInternal(dropTargetLayout, d, false);
    }

    private void onDropInternal(final CellLayout dropTargetLayout, DragObject d, boolean bBack) {

        Log.d(TAG, "onDropInternal bBack =" + bBack);

        if (mDragInfo == null || dropTargetLayout == null)
            return;

        View cell = mDragInfo.cell;
        if (cell == null)
            return;

        boolean hasMovedLayouts = (getParentCellLayoutForView(cell) != dropTargetLayout);
        final long container = LauncherSettings.Favorites.CONTAINER_TEMPEDIT;
        int spanX = 1;
        int spanY = 1;
        int screen = (mTargetCell[0] < 0) ? (int) mDragInfo.screenId : indexOfChild(dropTargetLayout);
        int snapScreen = -1;

        // First we find the cell nearest to point at which the item is
        // dropped, without any consideration to whether there is an
        // item there.
        if (bBack) {
            mTargetCell[0] = mDragInfo.cellX;
            mTargetCell[1] = mDragInfo.cellY;
        } else {
            mTargetCell = findNearestArea((int) mDragViewVisualCenter[0], (int) mDragViewVisualCenter[1], spanX, spanY,
                    dropTargetLayout, mTargetCell);
        }
        float distance = dropTargetLayout.getDistanceFromCell(mDragViewVisualCenter[0], mDragViewVisualCenter[1],
                mTargetCell);

        // If the item being dropped is a shortcut and the nearest drop
        // cell also contains a shortcut, then create a folder with the
        // two shortcuts. TODO scrool
        /*
         * if (createTempFolderIfNecessary(cell, container, dropTargetLayout,
         * mTargetCell, false, d.dragView, d, null)) { return; }
         * 
         * if (addToExistingFolderIfNecessary(cell, dropTargetLayout,
         * mTargetCell, distance, d, false)) { return; }
         */

        // Aside from the special case where we're dropping a shortcut
        // onto a shortcut,
        // we need to find the nearest cell location that is v
        ItemInfo item = (ItemInfo) d.dragInfo;

        int minSpanX = item.spanX;
        int minSpanY = item.spanY;
        if (item.minSpanX > 0 && item.minSpanY > 0) {
            minSpanX = item.minSpanX;
            minSpanY = item.minSpanY;
        }

        int[] resultSpan = new int[2];
        if (bBack) {
            mTargetCell[0] = mDragInfo.cellX;
            mTargetCell[1] = mDragInfo.cellY;
        } else {
            mTargetCell = dropTargetLayout.performReorder((int) mDragViewVisualCenter[0],
                    (int) mDragViewVisualCenter[1], minSpanX, minSpanY, spanX, spanY, cell, mTargetCell, resultSpan,
                    CellLayout.MODE_ON_DROP);
        }

        boolean foundCell = mTargetCell[0] >= 0 && mTargetCell[1] >= 0;

        if (foundCell) {
            final ItemInfo info = (ItemInfo) cell.getTag();
            if (hasMovedLayouts) {
                Log.d(TAG, "----drop internerl hasMovedLayouts || bBack ----");
                // Reparent the view
                if (getParentCellLayoutForView(cell) != null) {
                    getParentCellLayoutForView(cell).removeView(cell);
                }
                addInScreenLayout(cell, container, screen, mTargetCell[0], mTargetCell[1], 1, 1, false, true);
            }

            // update the item's position after drop
            CellLayout.LayoutParams lp = (CellLayout.LayoutParams) cell.getLayoutParams();
            lp.cellX = lp.tmpCellX = mTargetCell[0];
            lp.cellY = lp.tmpCellY = mTargetCell[1];
            // fix the bug:drag a widget to workspace,but there is no space,it
            // will
            // occupy more than 1 cells
            lp.cellHSpan = 1;// info.spanX;
            lp.cellVSpan = 1;// info.spanY;
            lp.isLockedToGrid = true;
            cell.setId(LauncherModel.getCellLayoutChildId(container, mDragInfo.screenId, mTargetCell[0], mTargetCell[1],
                    mDragInfo.spanX, mDragInfo.spanY));

            //LauncherModel.moveItemInDatabase(mLauncher, info, container, screen, lp.cellX, lp.cellY);
            // Move in edit list
            //bug 189267 tangzhongfeng.wt MODIFY 20160620
            /*final long tmpId = info.tempId;
            mTempItemsManager.moveItem(tmpId, mTargetCell[0], mTargetCell[1], screen,
                    LauncherSettings.Favorites.CONTAINER_TEMPEDIT);*/
            checkMapIdAndItems();

        } else {
            // TODO
        }
        if (d.dragView != null && d.dragView.hasDrawn()) {
            int DJACENT_SCREEN_DROP_DURATION = 300;
            int duration = snapScreen < 0 ? -1 : DJACENT_SCREEN_DROP_DURATION;
            mLauncher.getDragLayer().animateViewIntoPosition(d.dragView, cell, duration, null, null);
            //animateViewIntoPosition(d.dragView, cell, null, duration);
        } else {
            d.deferDragViewCleanupPostAnimation = false;
            cell.setVisibility(VISIBLE);
        }
        dropTargetLayout.onDropChild(cell);
    }

    private void animateViewIntoPosition(final DragView dragView, final View child,
            final Runnable onFinishAnimationRunnable, int duration) {
        ShortcutAndWidgetContainer parentChildren = (ShortcutAndWidgetContainer) child.getParent();
        CellLayout.LayoutParams lp = (CellLayout.LayoutParams) child.getLayoutParams();
        if (parentChildren != null) {
            parentChildren.measureChild(child);
        } else {
            // just from the widget custom page
            // do nothing
        }

        Rect r = new Rect();
        mLauncher.getDragLayer().getViewRectRelativeToSelf(dragView, r);

        int coord[] = new int[2];
        float childScale = child.getScaleX();
        coord[0] = lp.x + (int) (child.getMeasuredWidth() * (1 - childScale) / 2);
        coord[1] = lp.y + (int) (child.getMeasuredHeight() * (1 - childScale) / 2);
        float scale = 1.0f;
        if (parentChildren != null) {
            scale = mLauncher.getDragLayer().getDescendantCoordRelativeToSelf((View) child.getParent(), coord);
        }
        scale *= childScale;
        int toX = coord[0];
        int toY = coord[1];
        toX -= (Math.round(scale * (dragView.getMeasuredWidth() - child.getMeasuredWidth()))) / 2;
        toY -= (Math.round(scale * (dragView.getMeasuredHeight() - child.getMeasuredHeight()))) / 2;

        final int fromX = r.left;
        final int fromY = r.top;
        child.setVisibility(INVISIBLE);
        Runnable onCompleteRunnable = new Runnable() {
            public void run() {
                child.setVisibility(VISIBLE);
                if (onFinishAnimationRunnable != null) {
                    onFinishAnimationRunnable.run();
                }
            }
        };

        // set scale pivot to center
        dragView.setPivotX(dragView.getMeasuredWidth() / 2);
        dragView.setPivotY(dragView.getMeasuredHeight() / 2);

        // scale = mWidgetPreviewWidth / dragView.getMeasuredWidth();

        mLauncher.getDragLayer().animateViewIntoPosition(dragView, fromX, fromY, toX, toY, 1, 1, 1, scale, scale,
                onCompleteRunnable, DragLayer.ANIMATION_END_DISAPPEAR, duration, null);
    }

    // TDOO need to check ....
    private void throwBack(DragObject d) {
        final ItemInfo dragInfo = (ItemInfo) d.dragInfo;
        CellLayout cellLayout = mLauncher.getCellLayout(dragInfo.container, dragInfo.screenId);

        View view = null;
        switch (dragInfo.itemType) {
        case LauncherSettings.Favorites.ITEM_TYPE_APPLICATION:
        case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT:
            view = mLauncher.createShortcut(R.layout.application, cellLayout, (ShortcutInfo) dragInfo);
            break;
        case LauncherSettings.Favorites.ITEM_TYPE_FOLDER:
            break;
        default:
            throw new IllegalStateException("Unknown item type: " + dragInfo.itemType);
        }

        View oldView = cellLayout.getChildAt(dragInfo.cellX, dragInfo.cellY);
        cellLayout.removeView(oldView);

        mLauncher.getWorkspace().addInScreen(view, dragInfo.container, dragInfo.screenId, dragInfo.cellX,
                dragInfo.cellY, dragInfo.spanX, dragInfo.spanY, false);
        cellLayout.onDropChild(view);
        cellLayout.markCellsAsOccupiedForView(view);
        // LauncherModel.addOrMoveItemInDatabase(mLauncher, dragInfo,
        // dragInfo.container, dragInfo.screenId, dragInfo.cellX,
        // dragInfo.cellY);
    }

    @Override
    public void onDragEnter(DragObject dragObject) {
        mDragEnforcer.onDragEnter();
        mCurrentScreen = getCurrentScreen();

        mDropToLayout = null;
        CellLayout layout = mCurrentScreen;
        setCurrentDropLayout(layout);

        final View dragView = dragObject.dragView;
        float scale = 1.0f;
        ItemInfo itemInfo = (ItemInfo) dragObject.dragInfo;

        if (dragObject.dragSource != this) {
            mDragOutline = null;
        }
    }

    @Override
    public void onDragOver(DragObject d) {
        // TODO Auto-generated method stub

        mDragViewVisualCenter = mLauncher.getWorkspace().getDragViewVisualCenter(d.x, d.y, d.xOffset, d.yOffset,
                d.dragView, mDragViewVisualCenter, mDragTargetLayout);

        CellLayout layout = null;
        
        if (getParent().getParent() instanceof Folder) {
            Folder f = (Folder) getParent().getParent();
            if (isPointInSelfOverIconManagePanel(d.x, d.y)) {
                layout = getCurrentScreen();
            } else {
                layout = f.mContent;
            }
        } else {
            if (isPointInSelfOverIconManagePanel(d.x, d.y)) {
                layout = getCurrentScreen();
            } else if (mLauncher.getWorkspace().isPointInSelfOverHotseat(d.x, d.y, new Rect())) {
                layout = mLauncher.getHotseat().getLayout();
            } else {
                layout = mLauncher.getWorkspace().getCurrentDropLayout();
            }
        }
        if (layout != mDragTargetLayout) {
            setCurrentDropLayout(layout);
        }
        // Handle the drag over
        if (mDragTargetLayout != null) {
            // We want the point to be mapped to the dragTarget.
            mapPointFromSelfToChild(mDragTargetLayout, mDragViewVisualCenter, null);

            ItemInfo info = (ItemInfo) d.dragInfo;

            mTargetCell = mDragTargetLayout.findNearestArea((int) mDragViewVisualCenter[0],
                    (int) mDragViewVisualCenter[1], 1, 1, mTargetCell);

            setCurrentDropOverCell(mTargetCell[0], mTargetCell[1]);

            float targetCellDistance = mDragTargetLayout.getDistanceFromCell(mDragViewVisualCenter[0],
                    mDragViewVisualCenter[1], mTargetCell);

            final View dragOverView = mDragTargetLayout.getChildAt(mTargetCell[0], mTargetCell[1]);

            Log.i(TAG, "onDragOver mTargetCell[0] = " + mTargetCell[0] + ", mTargetCell[1] = " + mTargetCell[1]);
            if (dragOverView == null) {
                Log.i(TAG, "TempEditTarget.dragOverView = null");
            } else {
                // May be can add to the temp folder.
                ItemInfo dragOverItem = (ItemInfo) dragOverView.getTag();
                // manageFolderFeedback(info, dragOverItem, mCurrentScreen,
                // mTargetCell, dragOverView, targetCellDistance);
            }

            Bitmap dragOutline = null;
            final View child = d.dragView;
            if (mDragOutline == null) {
                mDragOutline = mLauncher.getWorkspace().createDragOutline(child, DRAG_BITMAP_PADDING);
            }
            dragOutline = mDragOutline;

            boolean nearestDropOccupied = mDragTargetLayout.isNearestDropLocationOccupied(
                    (int) mDragViewVisualCenter[0], (int) mDragViewVisualCenter[1], 1, 1, child, mTargetCell);

            if (!nearestDropOccupied) {
                mDragTargetLayout.visualizeDropLocation(child, dragOutline, (int) mDragViewVisualCenter[0],
                        (int) mDragViewVisualCenter[1], mTargetCell[0], mTargetCell[1], 1, 1, false,
                        d.dragView.getDragVisualizeOffset(), d.dragView.getDragRegion());
            } else if ((mDragMode == DRAG_MODE_NONE || mDragMode == DRAG_MODE_REORDER) && !mReorderAlarm.alarmPending()
                    && (mLastReorderX != mTargetCell[0] || mLastReorderY != mTargetCell[1])) {
                //drag problem tangzhongfeng.wt ADD 20160426
                int[] resultSpan = new int[2];
                mDragTargetLayout.performReorder((int) mDragViewVisualCenter[0], (int) mDragViewVisualCenter[1], 1, 1,
                        1, 1, child, mTargetCell, resultSpan, CellLayout.MODE_SHOW_REORDER_HINT);

                ReorderAlarmListener listener = new ReorderAlarmListener(mDragViewVisualCenter, 1, 1, 1, 1, d.dragView,
                        child);
                mReorderAlarm.setOnAlarmListener(listener);
                mReorderAlarm.setAlarm(0);
            }

            if (mDragMode == DRAG_MODE_CREATE_FOLDER || mDragMode == DRAG_MODE_ADD_TO_FOLDER || !nearestDropOccupied) {
                if (mDragTargetLayout != null) {
                    mDragTargetLayout.revertTempState();
                }
            }
        }
    }

    @Override
    public void onDragExit(DragObject dragObject) {
        // TODO Auto-generated method stub
        Log.d(TAG, "---------onDragExit---------------- ");
        mDragEnforcer.onDragExit();
        mDropToLayout = mDragTargetLayout;
        setCurrentDropLayout(null);
        mCurrentScreen.clearDragOutlines();
        mReorderAlarm.cancelAlarm();
    }

    @Override
    public void onFlingToDelete(DragObject dragObject, int x, int y, PointF vec) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean acceptDrop(DragObject d) {

        Log.d(TAG, "---------acceptDrop---------------- ");

        final ItemInfo item = (ItemInfo) d.dragInfo;
        final int itemType = item.itemType;
        //bug 167159 tangzhongfeng.wt ADD 20160426
        //bug 171954 tangzhongfeng.wt MODIFY 20160504
        if (itemType != LauncherSettings.Favorites.ITEM_TYPE_APPLICATION) {
            return false;
        }
        mDragViewVisualCenter = mLauncher.getWorkspace().getDragViewVisualCenter(d.x, d.y, d.xOffset, d.yOffset,
                d.dragView, mDragViewVisualCenter);
        CellLayout dropTargetLayout = mDropToLayout;

        if (dropTargetLayout != null) {
            mapPointFromSelfToChild(dropTargetLayout, mDragViewVisualCenter, null);
        }

        int spanX = 1;
        int spanY = 1;
        int minSpanX = spanX;
        int minSpanY = spanY;

        mTargetCell = findNearestArea((int) mDragViewVisualCenter[0], (int) mDragViewVisualCenter[1], minSpanX,
                minSpanY, dropTargetLayout, mTargetCell);
        Log.i(TAG, "TempEditTarget.acceptDrop mOpenedCell[0]=" + mOpenedCell[0] + ", mOpenedCell[1] = " + mOpenedCell[1]
                + ",mTargetCell[0]=" + mTargetCell[0] + ",mTargetCell[1]=" + mTargetCell[1]);

        if ((mOpenedCell[0] != -1 && mOpenedCell[1] != -1)
                && (mTargetCell[0] == mOpenedCell[0] && mTargetCell[1] == mOpenedCell[1])) {
            return false;
        }

        if (d.dragSource != this) {
            if (existsEnableCells(item, dropTargetLayout, mTargetCell)) {
                return true;
            } else {
                mLauncher.showOutOfSpaceMessage(false);
                return false;
            }
        }
        return true;
    }

    private boolean existsEnableCells(ItemInfo item, CellLayout cellLayout, int[] touchXY) {
        View dragOverView = cellLayout.getChildAt(touchXY[0], touchXY[1]);
        if (dragOverView == null) {
            Log.i(TAG, "dragOverView is null, the place is availabe");
            return true;
        } else {
            ItemInfo dragOverInfo = (ItemInfo) dragOverView.getTag();
            if (willAddToExistingTempFolder(item, cellLayout, touchXY, 0)) {
                return true;
            }

            /*
             * if (willCreateTempFolder(item, cellLayout, touchXY, 0, false)) {
             * Log.i(TAG, "willCreateTempFolder"); return true; }
             */

            if (cellLayout.existsEmptyCell()) {
                return true;
            }
        }
        return false;
    }

    private boolean willCreateTempFolder(ItemInfo info, CellLayout target, int[] targetCell, float distance,
            boolean considerTimeout) {
        View dropOverView = target.getChildAt(targetCell[0], targetCell[1]);
        if (null == dropOverView) {
            return false;
        } else {
            CellLayout.LayoutParams lp = (CellLayout.LayoutParams) dropOverView.getLayoutParams();
            if (lp.useTmpCoords && (lp.tmpCellX != lp.cellX || lp.tmpCellY != lp.tmpCellY)) {
                return false;
            }
        }

        ItemInfo dragOverInfo = (ItemInfo) dropOverView.getTag();

        boolean hasntMoved = false;
        if (mDragInfo != null) {
            hasntMoved = dropOverView == mDragInfo.cell;
        }

        if (dropOverView == null || hasntMoved || (considerTimeout)) {
            return false;
        }

        if ((dragOverInfo.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION
                || dragOverInfo.itemType == LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT)
                && (info.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION
                        || info.itemType == LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT
                        || info instanceof ShortcutInfo)) {
            return true;
        }

        return false;
    }

    private boolean willAddToExistingTempFolder(Object dragInfo, CellLayout target, int[] targetCell, float distance) {
        View dropOverView = target.getChildAt(targetCell[0], targetCell[1]);
        if (null == dropOverView) {
            return false;
        }

        ItemInfo dragOverInfo = (ItemInfo) dropOverView.getTag();

        return willAddToExistingTempFolder((ItemInfo) dragInfo, dragOverInfo, target, targetCell, distance);

    }

    private boolean willAddToExistingTempFolder(ItemInfo info, ItemInfo dragOverInfo, CellLayout target,
            int[] targetCell, float distance) {
        // if (distance > mMaxDistanceForFolderCreation)
        // return false;

        View dropOverView = target.getChildAt(targetCell[0], targetCell[1]);

        /*
         * if ((dragOverInfo.itemType ==
         * LauncherSettings.Favorites.ITEM_TYPE_TEMP_FOLDER) && (info.itemType
         * == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION || info.itemType
         * == LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT || info instanceof
         * ShortcutInfo || info instanceof ApplicationInfo)) { if (dropOverView
         * instanceof TctTempFolderIcon) { TctTempFolderIcon fi =
         * (TctTempFolderIcon) dropOverView; if (fi.acceptDrop(info)) { return
         * true; } } }
         */

        return false;
    }

    @Override
    public void getHitRectRelativeToDragLayer(Rect outRect) {
        mLauncher.getDragLayer().getDescendantRectRelativeToSelf(this, outRect);
    }

    @Override
    public void getLocationInDragLayer(int[] loc) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean supportsFlingToDelete() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean supportsAppInfoDropTarget() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean supportsDeleteDropTarget() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public float getIntrinsicIconScaleFactor() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void onFlingToDeleteCompleted() {
        // TODO Auto-generated method stub

    }

    @Override
    public void onDropCompleted(View target, DragObject d, boolean isFlingToDelete, boolean success) {
        // TODO Auto-generated method stub
        Log.d(TAG, "tempedit onDropCompleted success =" + success);
        if (success) {
            if (target != this) {
                if (mDragInfo != null) {
                    View dragView = mDragInfo.cell;
                    ItemInfo mInfo = (ItemInfo) dragView.getTag();
                    if (/*
                         * mInfo.itemType ==
                         * LauncherSettings.Favorites.ITEM_TYPE_TEMP_FOLDER
                         */false) {
                        // TODO This is unusable
                        final DropTarget dropTarget = (DropTarget) target;
                        Log.d(TAG, "onDropCompleted 01 dropTarget =" + dropTarget);
                        // dropTempFolderCompleted(dropTarget, d, false);
                        return;
                    } else {
                        Log.d(TAG, "tempedit onDropCompleted folder  success =" + success);

                        if (getParentCellLayoutForView(mDragInfo.cell) != null) {
                            Log.d(TAG, "tempedit onDropCompleted 000 = delete");

                            getParentCellLayoutForView(mDragInfo.cell).removeView(mDragInfo.cell);
                        }
                        if (mDragInfo.cell instanceof DropTarget) {
                            mDragController.removeDropTarget((DropTarget) mDragInfo.cell);
                        }
                        boolean toTempFolder = false;
                        if (target instanceof Folder) {
                            Log.d(TAG, "tempedit onDropCompleted 1111 = delete");

                            FolderInfo mFolderInfo = ((Folder) target).getInfo();
                            // if (mFolderInfo != null)
                            // toTempFolder = mFolderInfo.itemType ==
                            // LauncherSettings.Favorites.ITEM_TYPE_TEMP_FOLDER;
                        }

                        if (toTempFolder) {
                            Log.d(TAG, "tempedit onDropCompleted 2222 = delete");

                            FolderInfo mFolderInfo = ((Folder) target).getInfo();
                            // mTempItemsManager.moveItemToTempFolder(mInfo.tempId,
                            // mFolderInfo.tempId, mFolderInfo.id);
                            mDragInfo.cell.setVisibility(INVISIBLE);
                            mDragInfo = null;
                        } else {
                            Log.d(TAG, "tempedit onDropCompleted 3333 = delete");
                            mTempItemsManager.deleteItem(mInfo.tempId);
                            mInfo.revertTempId();
                        }
                    }
                }
            }
        } else if (mDragInfo != null) {
            Log.d(TAG, "onDropCompleted unsuccessful = mDragInfo =" + mDragInfo + "mDragInfo.screen ="
                    + mDragInfo.screenId);
            //tangzhongfeng.wt MODIFY 20160421 when drop to self, workspace or folder unsuccess
            CellLayout cellLayout;
            if (target instanceof CmccN2IconManagePanel) { 
                cellLayout = mCurrentScreen;
            } else if (target instanceof Folder) {
                cellLayout = ((Folder) target).mContent;
                mLauncher.getDragLayer().animateViewIntoPosition(d.dragView, mDragInfo.cell, null, this);
            } else {
                cellLayout = mLauncher.getCellLayout(mDragInfo.container, mDragInfo.screenId);
                mLauncher.getDragLayer().animateViewIntoPosition(d.dragView, mDragInfo.cell, null, this);
            }
            if (cellLayout == null && LauncherAppState.isDogfoodBuild()) {
                throw new RuntimeException(
                        "Invalid state: cellLayout == null in " + "Workspace#onDropCompleted. Please file a bug. ");
            }
            if (cellLayout != null) {
                cellLayout.onDropChild(mDragInfo.cell);
            }
            //CellLayout cellLayout = mLauncher.getCellLayout(mDragInfo.container, mDragInfo.screenId);
            //onDropInternal(cellLayout, d, true);
        }
        if (d.cancelled && mDragInfo.cell != null) {
            mDragInfo.cell.setVisibility(VISIBLE);
        }

        mDragOutline = null;
        mDragInfo = null;
    }

    @Override
    public boolean onEnterScrollArea(int x, int y, int direction) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean onExitScrollArea() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void onDragStart(DragSource source, Object info, int dragAction) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onDragEnd() {
        Log.d(TAG, "---------onDragEnd---------------- ");
        setDragMode(DRAG_MODE_NONE);
        clearDragOver();
        mCurrentScreen.clearDragOutlines();
    }

    void clearDragOver() {
        cleanupReorder(true);
        mDragOverX = -1;
        mDragOverY = -1;
    }

    /**
     * Adds the specified child in the specified screen. The position and
     * dimension of the child are defined by x, y, spanX and spanY.
     * 
     * @param child
     *            The child to add in one of the workspace's screens.
     * @param screen
     *            The screen in which to add the child.
     * @param x
     *            The X position of the child in the screen's grid.
     * @param y
     *            The Y position of the child in the screen's grid.
     * @param spanX
     *            The number of cells spanned horizontally by the child.
     * @param spanY
     *            The number of cells spanned vertically by the child.
     * @param insert
     *            When true, the child is inserted at the beginning of the
     *            children list.
     */

    private boolean addInScreenLayout(View child, long container, int screen, int x, int y, int spanX, int spanY,
            boolean insert, boolean markCells) {
        Log.d(TAG, "Add in screen x = " + x + ", markCells = " + markCells);
        if (container != LauncherSettings.Favorites.CONTAINER_TEMPEDIT) {
            return false;
        }

        CmccN2TempEditCellLayout curScreen = getLayoutAtScreen(screen);

        if (child instanceof FolderIcon) {
            ((FolderIcon) child).setTextVisible(true);
        }

        ViewGroup.LayoutParams genericLp = (ViewGroup.LayoutParams) child.getLayoutParams();
        CellLayout.LayoutParams lp;
        if (genericLp == null || !(genericLp instanceof CellLayout.LayoutParams)) {
            lp = new CellLayout.LayoutParams(x, y, spanX, spanY);
        } else {
            lp = (CellLayout.LayoutParams) genericLp;
            lp.cellX = x;
            lp.cellY = y;
            lp.cellHSpan = spanX;
            lp.cellVSpan = spanY;
        }
        // Get the canonical child id to uniquely represent this view in this
        // screen
        int childId = LauncherModel.getCellLayoutChildId(container, screen, x, y, spanX, spanY);
        boolean markCellsAsOccupied = !(child instanceof Folder);

        Log.d(TAG, " addInScreenLayout  childId =" + childId + " lp. (cellX , cellY , cellHSpan , cellVSpan ) =" + "("
                + lp.cellX + "," + lp.cellY + "," + lp.cellHSpan + ", " + lp.cellVSpan + ")" + ", insert = " + insert);
        // remove the bg child
        /*
         * if (curScreen.getChildAt(x, y) != null) { curScreen.removeViewAt(x +
         * mCellCountX * y); }
         */
        if (!curScreen.addViewToCellLayout(child, insert ? 0 : -1, childId, lp, markCells)) {
            Log.w(TAG, "Failed to add to item at (" + lp.cellX + "," + lp.cellY + ") to CellLayout");
            return false;
        }
        if (!(child instanceof Folder)) {
            child.setHapticFeedbackEnabled(false);
            Log.d(TAG, "Add in screen set mLongClickListener");
            child.setOnClickListener(this);
            child.setOnLongClickListener(this);
        }
        if (child instanceof DropTarget) {
            Log.w(TAG, "child instanceof DropTarget");
            mDragController.addDropTarget((DropTarget) child);
        }

        return true;
    }

    private boolean addInScreenLayout(View child, int screen, int x, boolean insert, boolean markCells) {
        return addInScreenLayout(child, LauncherSettings.Favorites.CONTAINER_TEMPEDIT, screen, x, 0, 1, 1, insert,
                markCells);
    }

    public boolean addItemInScreenLayout(ItemInfo itemInfo, int x, int screen) {
        if (itemInfo == null) {
            return false;
        }
        CmccN2TempEditCellLayout cellLayout = getLayoutAtScreen(screen);
        View view = null;
        if (itemInfo.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET
                || itemInfo.itemType == LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT
                || itemInfo.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION) {
            view = mLauncher.createShortcut(R.layout.application, cellLayout, (ShortcutInfo) itemInfo);
        } else {
            Log.e(TAG, "Impossible issue, ERROR");
            return false;
        }
        boolean ret = addInScreenLayout(view, screen, x, false, true);
        return ret;
    }

    private boolean addEmptyItemInScreenLayout(int x) {

        ImageView view = new ImageView(getContext());
        view.setImageDrawable(iconBg);
        boolean ret = addInScreenLayout(view, getCurrentPage(), x, false, false);
        return ret;
    }

    void startDrag(CellLayout.CellInfo cellInfo) {
        View child = cellInfo.cell;

        // Make sure the drag was started by a long press as opposed to a long
        // click.
        if (child == null || !child.isInTouchMode()) {
            return;
        }
        mDragInfo = cellInfo;
        child.setVisibility(INVISIBLE);
        CellLayout layout = this.getCurrentScreen();
        mCurrentScreen.prepareChildForDrag(child);
        mCurrentScreen.invalidate();

        child.clearFocus();
        child.setPressed(false);

        // The outline is used to visualize where the item will land if
        // dropped
        mDragOutline = mLauncher.getWorkspace().createDragOutline(child, DRAG_BITMAP_PADDING);
        // for dragging over workspace
        mLauncher.getWorkspace().mDragOutline = mDragOutline;
        beginDragShared(child, this);
    }

    public void beginDragShared(View child, DragSource source) {
        View dragView;

        Resources r = getResources();

        dragView = child;

        // The drag bitmap follows the touch point around on the screen
        AtomicInteger padding = new AtomicInteger(DRAG_BITMAP_PADDING);
        Bitmap b = mLauncher.getWorkspace().createDragBitmap(dragView, padding);

        final int bmpWidth = b.getWidth();
        final int bmpHeight = b.getHeight();

        float scale = mLauncher.getDragLayer().getLocationInDragLayer(child, mTempXY);
        int dragLayerX = Math.round(mTempXY[0] - (bmpWidth - scale * child.getWidth()) / 2);
        int dragLayerY = Math.round(mTempXY[1] - (bmpHeight - scale * bmpHeight) / 2 - DRAG_BITMAP_PADDING / 2);

        Point dragVisualizeOffset = null;
        Rect dragRect = null;

        if (child instanceof FolderIcon) {
            // int previewSize =
            // r.getDimensionPixelSize(R.dimen.folder_preview_size);
            // dragRect = new Rect(0, 0, child.getWidth(), previewSize);
        }
        // Clear the pressed state if necessary
        if (dragView instanceof BubbleTextView) {
            BubbleTextView icon = (BubbleTextView) dragView;
            icon.clearPressedBackground();
        }

        mDragController.startDrag(b, dragLayerX, dragLayerY, source, child.getTag(), DragController.DRAG_ACTION_MOVE,
                dragVisualizeOffset, dragRect, scale);
        b.recycle();
    }

    @Override
    public void onClick(View v) {
        //tangzhongfeng.wt add click event 20160421
        if (!(v.getTag() instanceof ShortcutInfo)) {
            return;
        }
        ShortcutInfo info = (ShortcutInfo) v.getTag();
        //bug 213393 tangzhongfeng.wt ADD 20160830
        FolderInfo folderInfo = null;
        if (info.container > 0) {
            // container is folder
            folderInfo = mLauncher.getFolderById(info.container);
            if (folderInfo == null) {
                Log.e(TAG, "Error Place ,mFolderInfo is null");
                return;
            }
        }
        boolean ret = false;
        if (getParent().getParent() instanceof Folder) {
            Folder folder = (Folder) getParent().getParent();
            ret = folder.addItem(info);
        } else {
            ret = mLauncher.getWorkspace().addItemInCurrScreen(info);
        }
        if (ret) {
            mCurrentScreen.removeView(v);
            mTempItemsManager.deleteItem(info.tempId);
            info.revertTempId();
            //bug 213393 tangzhongfeng.wt ADD 20160830
            if (folderInfo != null) {
                for (int i = 0; i < folderInfo.listeners.size(); i++) {
                    FolderListener l = folderInfo.listeners.get(i);
                    if (l instanceof Folder) {
                        Folder f = (Folder) l;
                        f.setItemsInvalidated(true);
                    }
                }
            }
        } else {
            mLauncher.showOutOfSpaceMessage(false);
        }
    }

    @Override
    public boolean onLongClick(View v) {
        // Return early if this is not initiated from a touch
        if (!v.isInTouchMode())
            return false;

        if (!mLauncher.isDraggingEnabled())
            return false;
        //bug 168965 tangzhongfeng.wt ADD 20160426
        if (!allowLongPress() || mDragController.isDragging()) {
            return true;
        }

        /*
         * if (!(v instanceof CellLayout)) { if (v instanceof BubbleTextView)
         * {// app icon v = (View) v.getParent().getParent().getParent(); } else
         * {// folder and widget v = (View) v.getParent().getParent(); } }
         */
        CellLayout.CellInfo longClickCellInfo = null;
        if (v.getTag() instanceof ItemInfo) {
            ItemInfo info = (ItemInfo) v.getTag();
            longClickCellInfo = new CellLayout.CellInfo(v, info);
        }

        if (longClickCellInfo == null) {
            return true;
        }

        final View itemUnderLongClick = longClickCellInfo.cell;
        if (!(itemUnderLongClick instanceof Folder)) {
            this.startDrag(longClickCellInfo);
            mDragInfo = longClickCellInfo;
            return true;
        }
        return true;
    }

    @Override
    protected int getScrollMode() {
        return SmoothPagedView.X_LARGE_MODE;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if (!isDataReady()) {
            setDataIsReady();
            setMeasuredDimension(width, height);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
    }

    protected void snapToPage(int whichPage, Runnable r) {
        snapToPage(whichPage, SLOW_PAGE_SNAP_ANIMATION_DURATION, r);
    }

    protected void snapToPage(int whichPage, int duration, Runnable r) {
        if (mDelayedSnapToPageRunnable != null) {
            mDelayedSnapToPageRunnable.run();
        }
        mDelayedSnapToPageRunnable = r;
        snapToPage(whichPage, duration);
    }
    // In apps customize, we have a scrolling effect which emulates pulling
    // cards off of a stack.
    @Override
    protected void screenScrolled(int screenCenter) {
        // TODO check the screen scrolled
        super.screenScrolled(screenCenter);
    }

    @Override
    public void scrollLeft() {
        super.scrollLeft();
    }

    @Override
    public void scrollRight() {
        super.scrollRight();
    }

    @Override
    protected void onPageEndMoving() {
        super.onPageEndMoving();
        Log.i(TAG, "tzf onPageEndMoving");
        if (mDelayedSnapToPageRunnable != null) {
            Runnable tmp = mDelayedSnapToPageRunnable;
            mDelayedSnapToPageRunnable = null;
            tmp.run();
        }
    }

    /*
     * 
     * Convert the 2D coordinate xy from the parent View's coordinate space to
     * this CellLayout's coordinate space. The argument xy is modified with the
     * return result.
     * 
     * if cachedInverseMatrix is not null, this method will just use that matrix
     * instead of computing it itself; we use this to avoid redundant matrix
     * inversions in findMatchingPageForDragOver
     */
    void mapPointFromSelfToChild(View v, float[] xy, Matrix cachedInverseMatrix) {
        xy[0] = xy[0] - v.getLeft();
        xy[1] = xy[1] - v.getTop();
    }

    /*
     * 
     * Convert the 2D coordinate xy from this CellLayout's coordinate space to
     * the parent View's coordinate space. The argument xy is modified with the
     * return result.
     */
    void mapPointFromChildToSelf(View v, float[] xy) {
        xy[0] += v.getLeft();
        xy[1] += v.getTop();
    }

    void setCurrentDropOverCell(int x, int y) {
        if (x != mDragOverX || y != mDragOverY) {
            mDragOverX = x;
            mDragOverY = y;
            setDragMode(DRAG_MODE_NONE);
        }
    }

    void setDragMode(int dragMode) {
        if (dragMode != mDragMode) {
            if (dragMode == DRAG_MODE_NONE) {
                // cleanupAddToFolder();
                // We don't want to cancel the re-order alarm every time the
                // target cell changes
                // as this feels to slow / unresponsive.
                cleanupReorder(false);
                // cleanupFolderCreation();
            } else if (dragMode == DRAG_MODE_ADD_TO_FOLDER) {
                cleanupReorder(true);
                // cleanupFolderCreation();
            } else if (dragMode == DRAG_MODE_CREATE_FOLDER) {
                // cleanupAddToFolder();
                cleanupReorder(true);
            } else if (dragMode == DRAG_MODE_REORDER) {
                // cleanupAddToFolder();
                // cleanupFolderCreation();
            }
            mDragMode = dragMode;
        }
    }

    private void cleanupReorder(boolean cancelAlarm) {
        // Any pending reorders are canceled
        if (cancelAlarm) {
            mReorderAlarm.cancelAlarm();
        }
        mLastReorderX = -1;
        mLastReorderY = -1;
    }

    /**
     * Calculate the nearest cell where the given object would be dropped.
     * 
     * pixelX and pixelY should be in the coordinate system of layout
     */
    private int[] findNearestArea(int pixelX, int pixelY, int spanX, int spanY, CellLayout layout, int[] recycle) {
        return layout.findNearestArea(pixelX, pixelY, spanX, spanY, recycle);
    }

    public class ReorderAlarmListener implements OnAlarmListener {
        float[] dragViewCenter;
        int minSpanX, minSpanY, spanX, spanY;
        DragView dragView;
        View child;

        public ReorderAlarmListener(float[] dragViewCenter, int minSpanX, int minSpanY, int spanX, int spanY,
                DragView dragView, View child) {
            this.dragViewCenter = dragViewCenter;
            this.minSpanX = minSpanX;
            this.minSpanY = minSpanY;
            this.spanX = spanX;
            this.spanY = spanY;
            this.child = child;
            this.dragView = dragView;
        }

        public void onAlarm(Alarm alarm) {
            int[] resultSpan = new int[2];
            mTargetCell = findNearestArea((int) mDragViewVisualCenter[0], (int) mDragViewVisualCenter[1], minSpanX,
                    minSpanY, mDragTargetLayout, mTargetCell);
            mLastReorderX = mTargetCell[0];
            mLastReorderY = mTargetCell[1];

            mTargetCell = mDragTargetLayout.performReorder((int) mDragViewVisualCenter[0],
                    (int) mDragViewVisualCenter[1], minSpanX, minSpanY, spanX, spanY, child, mTargetCell, resultSpan,
                    CellLayout.MODE_DRAG_OVER);

            if (mTargetCell[0] < 0 || mTargetCell[1] < 0) {
                mDragTargetLayout.revertTempState();
            } else {
                setDragMode(DRAG_MODE_REORDER);
            }

            boolean resize = resultSpan[0] != spanX || resultSpan[1] != spanY;
            mDragTargetLayout.visualizeDropLocation(child, mDragOutline, (int) mDragViewVisualCenter[0],
                    (int) mDragViewVisualCenter[1], mTargetCell[0], mTargetCell[1], resultSpan[0], resultSpan[1],
                    resize, dragView.getDragVisualizeOffset(), dragView.getDragRegion());
        }
    }

    void setCurrentDropLayout(CellLayout layout) {
        if (mDragTargetLayout != null) {
            mDragTargetLayout.revertTempState();
            mDragTargetLayout.onDragExit();
        }
        mDragTargetLayout = layout;
        if (mDragTargetLayout != null) {
            mDragTargetLayout.onDragEnter();
        }
        cleanupReorder(true);
        setCurrentDropOverCell(-1, -1);
    }

    /**
     * the point is in icon manage panel area
     * 
     * @param x
     * @param y
     * @param r
     * @return
     */
    boolean isPointInSelfOverIconManagePanel(int x, int y) {
        int[] tmpPt = new int[2];
        tmpPt[0] = x;
        tmpPt[1] = y;
        mLauncher.getDragLayer().getDescendantCoordRelativeToSelf(this, tmpPt, true);
        LauncherAppState app = LauncherAppState.getInstance();
        DeviceProfile grid = app.getDynamicGrid().getDeviceProfile();
        Rect r = grid.getIconManagePanelRect();
        if (r.contains(tmpPt[0], tmpPt[1])) {
            return true;
        }
        return false;
    }
    
    public int[] getFirstPosition() {
        int curPages = getCurrentPage();
        if (curPages < 0) {
            return null;
        }
        int totalCount = mCellCountX * mCellCountY;
        int xy[] = new int[3];
        CellLayout cellLayout = null;
        int retCount = 0, i = 0;
        for (i = curPages; i < getPageCount(); i++) {
            cellLayout = (CellLayout) getPageAt(i);
            retCount = cellLayout.findPosition(xy, mCellCountX, mCellCountY, false);
            if (retCount < totalCount) {
                break;
            }
        }
        curPages = i;
        if (retCount == totalCount) {
            mCurrentScreen = addScreen(curPages);
            xy[0] = 0;
            xy[1] = 0;
        }
        //bug 180356 tangzhongfeng.wt MODIFY 20160525
        xy[2] = curPages;
        if (curPages != getCurrentPage()) {
            snapToPage(curPages, mAddScreenAnimationTime);
        }
        return xy;
    }

    @Override
    public void onPageSwitch(View newPage, int newPageIndex) {
        if (newPage != null) {
            mCurrentScreen = (CellLayout) newPage;
        }
    }
    /*bug 180264 tangzhongfeng.wt ADD 20160525 start */
    void checkMapIdAndItems() {
        CmccN2TempEditCellLayout cl = null;
        mTempItemsManager.resetMap();
        for (int i = 0; i < getPageCount(); i++) {
            cl = getLayoutAtScreen(i);
            ShortcutAndWidgetContainer s = cl.getShortcutsAndWidgets();
            for (int j = 0; j < s.getChildCount(); j++) {
                View v = s.getChildAt(j);
                CellLayout.LayoutParams lp = (CellLayout.LayoutParams) v.getLayoutParams();
                ItemInfo info = (ItemInfo) v.getTag();
                mTempItemsManager.checkMapIdAndItems(lp.cellX, lp.cellY, i, info.tempId);
            }
        }
    }
    /*bug 180264 tangzhongfeng.wt ADD 20160525 end */
}
