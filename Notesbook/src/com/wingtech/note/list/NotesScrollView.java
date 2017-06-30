/*===================================================================================================*	
 *  when  |      who     |    keyword           |        why         |         what                  *	
 *===================================================================================================*	
 *20160407|mengzhiming.wt|porting A1s enjoynotes| Port_EnjoyNotes | porting A1s enjoynotes to CMCC N2*	
*====================================================================================================*/

package com.wingtech.note.list;

import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.TranslateAnimation;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Rect;

import com.wingtech.note.R;

import java.util.ArrayList;
import java.util.Iterator;

public class NotesScrollView extends ViewPager {
    private Adapter mAdapter;
    private CirclePageIndicator mPageChangeListener;
    private DataSetObserver mDataSetObserver = new DataSetObserver() {
        public void onChanged() {
            refreshAllScreens();
        }

        public void onInvalidated() {
        }
    };
    private LayoutInflater mInflater;
    private int mMaxSizePerScreen;
    private AdapterView.OnItemClickListener mOnItemClickListener;
    private AdapterView.OnItemLongClickListener mOnItemLongClickListener;
    private ViewCacher mRecycler;
    private float lastX;
    private Rect mRect = new Rect();// 用来记录起始位置
    private boolean handleDefault = true;// 标志当前是否在处理首尾页
    private float preX;
    private ScrollViewAdapter pagerAdapter;
    private ArrayList<NoteGridView> viewLists;
    private static final float SCROLL_WIDTH = 0f;// 起始距离
    private static final float RATIO = 0.5f;// 摩擦系数

    public NotesScrollView(Context paramContext) {
        super(paramContext);
        initialize();
    }

    public NotesScrollView(Context paramContext, AttributeSet paramAttributeSet) {
        super(paramContext, paramAttributeSet);
        initialize();
    }

    private void initialize() {
        // TODO Auto-generated method stub
        mInflater = LayoutInflater.from(getContext());
        mMaxSizePerScreen = newGridView(null).getMaxSize();

    }

    @Override
    public void setOnPageChangeListener(OnPageChangeListener listener) {
        // TODO Auto-generated method stub
        super.setOnPageChangeListener(listener);
        mPageChangeListener = (CirclePageIndicator) listener;
    }

    public void setOnItemClickListener(AdapterView.OnItemClickListener paramOnItemClickListener) {
        mOnItemClickListener = paramOnItemClickListener;
    }

    public void setOnItemLongClickListener(
            AdapterView.OnItemLongClickListener paramOnItemLongClickListener) {
        mOnItemLongClickListener = paramOnItemLongClickListener;
    }

    public void setViewRecycler(ViewCacher paramViewCacher) {
        if (mRecycler != paramViewCacher)
            mRecycler = paramViewCacher;
    }

    public void setAdapter(Adapter adapter) {
        if (mAdapter != adapter) {
            if (mAdapter != null)
                mAdapter.unregisterDataSetObserver(mDataSetObserver);
            mAdapter = adapter;
            if (mAdapter != null)
                mAdapter.registerDataSetObserver(mDataSetObserver);
        }
        refreshAllScreens();
        pagerAdapter = new ScrollViewAdapter(viewLists);
        setAdapter(pagerAdapter);
    }

    public void setHeadView(View v) {
        if (pagerAdapter != null) {
            // pagerAdapter.notifyDataSetChanged();
            pagerAdapter.setHeaderView(v);
            removeAllViews();
            refreshAllScreens();// 此处如不刷新，会导致不能正确销毁子View
        }
    }


    private NoteGridView newGridView(ViewGroup parent) {
        return (NoteGridView) mInflater.inflate(R.layout.notes_grid_view, parent, false);
    }

    protected void refreshAllScreens() {
        if (viewLists == null)
            viewLists = new ArrayList<NoteGridView>();
        else {
            Iterator<NoteGridView> iterator = viewLists.iterator();
            while (iterator.hasNext()) {
                iterator.next().recycle();
            }
            viewLists.clear();
        }
        if (mAdapter != null) {
            int i = 0, count = mAdapter.getCount();
            while (true) {
                NoteGridView child = newGridView(this);
                child.setup(mAdapter, i, mRecycler, mOnItemClickListener,
                        mOnItemLongClickListener);
                viewLists.add(child);
                i += mMaxSizePerScreen;
                if (i > count - 1)
                    break;
            }
        }
        if (pagerAdapter != null) {
            pagerAdapter.notifyDataSetChanged();
        }
    }

    public int getCurrentItem() {
        return mPageChangeListener.getCurItem();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent arg0) {

        if (arg0.getAction() == MotionEvent.ACTION_DOWN) {
            preX = lastX = arg0.getX();
        }
        return super.onInterceptTouchEvent(arg0);
    }

    @Override
    public boolean onTouchEvent(MotionEvent arg0) {

        switch (arg0.getAction()) {

            case MotionEvent.ACTION_MOVE:
                float nowX = arg0.getX();
                if ((getCurrentItem() == 0 && preX < nowX)
                        || ((getCurrentItem() == getAdapter().getCount() - 1) && preX > nowX)) {
                    float offset = nowX - lastX;
                    lastX = nowX;
                    // 此处需考虑只有一页的情况
                    if (getCurrentItem() == 0) {
                        View child = pagerAdapter.getItem(getCurrentItem());
                        if (offset > SCROLL_WIDTH) {
                            if (mRect.isEmpty()) {
                                mRect.set(child.getLeft(), child.getTop(), child.getRight(),
                                        child.getBottom());
                            }
                            handleDefault = false;
                        }
                        if (!handleDefault)
                            child.layout(child.getLeft() + (int) (offset * RATIO),
                                    child.getTop(),
                                    child.getRight()
                                            + (int) (offset * RATIO), child.getBottom());
                    }
                    if ((getCurrentItem() == getAdapter().getCount() - 1)) {
                        View child = pagerAdapter.getItem(getCurrentItem());

                        if (offset < -SCROLL_WIDTH) {
                            if (mRect.isEmpty()) {
                                mRect.set(child.getLeft(), child.getTop(), child.getRight(),
                                        child.getBottom());
                            }
                            handleDefault = false;
                        }
                        if (!handleDefault) {
                            child.layout(child.getLeft() + (int) (offset * RATIO),
                                    child.getTop(),
                                    child.getRight()
                                            + (int) (offset * RATIO), child.getBottom());
                        }
                    }
                } else {
                    handleDefault = true;
                }
                if (!handleDefault) {
                    return true;
                }
                break;
            case MotionEvent.ACTION_UP:
            default:
                onTouchUp();
                break;
        }
        return super.onTouchEvent(arg0);
    }

    private boolean onTouchUp() {
        if (!mRect.isEmpty()) {
            recoveryPosition();
            return true;
        }
        return false;
    }

    private void recoveryPosition() {
        View child = pagerAdapter.getItem(getCurrentItem());
        TranslateAnimation ta = new TranslateAnimation(child.getLeft() - getScrollX(), mRect.left
                - getScrollX(), 0, 0);
        ta.setDuration(300);
        child.startAnimation(ta);
        child.layout(mRect.left, mRect.top, mRect.right, mRect.bottom);
        mRect.setEmpty();
        handleDefault = true;
    }
}

