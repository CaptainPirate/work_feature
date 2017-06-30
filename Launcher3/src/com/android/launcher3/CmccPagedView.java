/*
  * Copyright @ 2015 China Mobile Group Device Co.,Ltd.
  * All rights Reserved.
*/

package com.android.launcher3;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Interpolator;

public class CmccPagedView extends ViewGroup implements ViewGroup.OnHierarchyChangeListener {

    private static final String TAG = "CmccPagedView";

    private int mCountX;
    private int mCountY;
    private int mCountInOnePage;

    private int mCellWidth;
    private int mCellHeight;

    private int mVerticalGap = 0;
    private int mHorizontalGap = 0;

    private static final int INVALID_PAGE = -1;
    private static final int INVALID_INDEX = -1;
    private static final int INVALID_POINTER = -1;

    private static final int MIN_SNAP_VELOCITY = 1500;
    private static final int MIN_LENGTH_FOR_FLING = 25;
    private static final int FLING_THRESHOLD_VELOCITY = 500;
    private static final float SIGNIFICANT_MOVE_THRESHOLD = 0.4f;
    private static final float RETURN_TO_ORIGINAL_PAGE_THRESHOLD = 0.33f;
    private static final int PAGE_SNAP_ANIMATION_DURATION = 750;

    private boolean mReturnToFirstWhenOverScrollAtLastPage = false;

    private LauncherScroller mScroller;
    private VelocityTracker mVelocityTracker;

    private float mDensity;
    private int mMaximumVelocity;
    private int mMinSnapVelocity;
    private int mFlingThresholdVelocity;

    private final static int TOUCH_STATE_REST = 0;
    private final static int TOUCH_STATE_SCROLLING = 1;

    private int mTouchState = TOUCH_STATE_REST;
    private int mTouchSlop;
    private int mActivePointerId = INVALID_POINTER;

    private float mTotalMotionX;
    private float mDownMotionX;
    private float mLastMotionX;
    private float mLastMotionXRemainder;
    private int mUnboundedScrollX;
    private int mMaxScrollX;

    private int mCurrentPage;
    private int mNextPage = INVALID_PAGE;

    private boolean mEnableArrow = true;
    private int mPageCount;
    private int mArrowWidth;
    private int mArrowPaddingTop;
    private int mArrowMarginHorizontal;
    private Drawable mLeftArrowDrawable;
    private Drawable mRightArrowDrawable;

    public CmccPagedView(Context context) {
        super(context);
        init(context);
    }

    public CmccPagedView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public CmccPagedView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setWillNotDraw(false);
        Resources res = context.getResources();
        mArrowWidth = res.getDimensionPixelSize(R.dimen.effect_arrow_width);
        mArrowPaddingTop = res.getDimensionPixelSize(R.dimen.effect_arrow_padding_top);
        mArrowMarginHorizontal = res.getDimensionPixelSize(R.dimen.effect_arrow_margin_horizontal);
        mLeftArrowDrawable = res.getDrawable(R.drawable.ic_home_effect_scroll_left);
        mRightArrowDrawable = res.getDrawable(R.drawable.ic_home_effect_scroll_right);
    }

    @Override
    protected void onFinishInflate() {
        // TODO: should read from xml file
        mCountX = 4;
        mCountY = 1;
        mCountInOnePage = mCountX * mCountY;

        final ViewConfiguration configuration = ViewConfiguration.get(getContext());
        mTouchSlop = configuration.getScaledPagingTouchSlop();
        setOnHierarchyChangeListener(this);

        mScroller = new LauncherScroller(getContext());
        mScroller.setInterpolator(new ScrollInterpolator());

        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
        mDensity = getResources().getDisplayMetrics().density;
        mFlingThresholdVelocity = (int) (FLING_THRESHOLD_VELOCITY * mDensity);
        mMinSnapVelocity = (int) (MIN_SNAP_VELOCITY * mDensity);
    }

    @Override
    public void onChildViewAdded(View parent, View child) {
        updateMaxScrollX();
        mPageCount = computePageCount();
    }

    @Override
    public void onChildViewRemoved(View parent, View child) {
        updateMaxScrollX();
        mPageCount = computePageCount();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize =  MeasureSpec.getSize(heightMeasureSpec);

        int childWidthSize = widthSize - (getPaddingLeft() + getPaddingRight())
                - (mCountX - 1) * mHorizontalGap;
        int childHeightSize = heightSize
                - (getPaddingTop() + getPaddingBottom()) - (mCountY - 1)
                * mVerticalGap;

        LauncherAppState app = LauncherAppState.getInstance();
        DeviceProfile grid = app.getDynamicGrid().getDeviceProfile();

        mCellWidth = grid.calculateCellWidth(childWidthSize, mCountX);
        mCellHeight= grid.calculateCellHeight(childHeightSize, mCountY);

        int childCount = getChildCount();
        for (int i=0; i<childCount; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != View.GONE) {
                LayoutParams lp = child.getLayoutParams();
                lp.width = mCellWidth;
                lp.height = mCellHeight;

                int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(
                        lp.width, MeasureSpec.EXACTLY);
                int childheightMeasureSpec = MeasureSpec.makeMeasureSpec(
                        lp.height, MeasureSpec.EXACTLY);
                child.measure(childWidthMeasureSpec, childheightMeasureSpec);
            }
        }

        updateMaxScrollX();
        setMeasuredDimension(widthSize, heightSize);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        // TODO: need compatible with RTL
        int childCount = getChildCount();

        int pagePaddingLeft = getPaddingLeft();
        int pagePaddingTop = getPaddingTop();

        int pageStartX = 0;
        int pageX = 0;
        int pageY = pagePaddingTop;

        int pageIndex = 0;
        int indexInOnePage = 0;
        int xIndex = 0;
        int yIndex = 0;

        boolean centerHorizontal = false;
        boolean centerVertical = false;
        if (childCount < mCountInOnePage) {
            if (childCount < mCountX) {
                centerHorizontal = true;
                centerVertical = (mCountY > 1);
            } else {
                // get rows
                int rows = (childCount - 1) / mCountX + 1;
                rows = Math.max(0, rows);
                centerVertical = rows < mCountY;
            }
        }

        for (int i=0; i<childCount; i++) {
            View child = getChildAt(i);

            LayoutParams lp = child.getLayoutParams();

            pageIndex = getChildPageIndex(i);
            indexInOnePage = i % mCountInOnePage;
            xIndex = indexInOnePage % mCountX;
            yIndex = indexInOnePage / mCountX;

            pageStartX = pageIndex * getMeasuredWidth();
            pageX = pageStartX + pagePaddingLeft;

            if (centerHorizontal) {
                pageX += (getMeasuredWidth() - childCount * lp.width + (childCount - 1)
                        * mHorizontalGap) / 2;
            }
            if (centerVertical) {
                pageY += (getMeasuredHeight() - childCount * lp.height + (childCount - 1)
                        * mVerticalGap) / 2;
            }

            int left = pageX + xIndex * (mHorizontalGap + lp.width);
            int right = left + lp.width;
            int top = pageY + yIndex * (mVerticalGap + lp.height);
            int bottom = top + lp.height;

            child.layout(left, top, right, bottom);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        int action = ev.getAction() & MotionEvent.ACTION_MASK;
        acquireVelocityTrackerAndAddMovement(ev);

        switch (action) {
        case MotionEvent.ACTION_DOWN:
            final float x = ev.getX();
            final float y = ev.getY();
            // Remember location of down touch
            mActivePointerId = ev.getPointerId(0);
            mLastMotionX = mDownMotionX = x;
            mLastMotionXRemainder = 0;
            mTotalMotionX = 0;

            final boolean finishedScrolling = true;
            if (finishedScrolling) {
                mTouchState = TOUCH_STATE_REST;
            } else {
                mTouchState = TOUCH_STATE_SCROLLING;
            }

            break;

        case MotionEvent.ACTION_MOVE:
            // determine scroll
            if (mActivePointerId != INVALID_POINTER) {
                determineScrollingStart(ev);
            }
            break;

        case MotionEvent.ACTION_UP:
        case MotionEvent.ACTION_CANCEL:
            resetTouchState();
            break;

        case MotionEvent.ACTION_POINTER_UP:
            onSecondaryPointerUp(ev);
            releaseVelocityTracker();
            break;
        }

        return mTouchState != TOUCH_STATE_REST;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction() & MotionEvent.ACTION_MASK;

        acquireVelocityTrackerAndAddMovement(event);

        switch(action) {
        case MotionEvent.ACTION_DOWN:
            // Remember location of down touch
            mActivePointerId = event.getPointerId(0);
            mLastMotionX = mDownMotionX = event.getX();
            mLastMotionXRemainder = 0;
            mTotalMotionX = 0;
            // always handle touch event
            return true;
        case MotionEvent.ACTION_MOVE:
            if (mTouchState == TOUCH_STATE_SCROLLING) {
                // Scroll to follow the motion event
                final int pointerIndex = event.findPointerIndex(mActivePointerId);
                if (pointerIndex == INVALID_INDEX){
                    return true;
                }

                final float x = event.getX(pointerIndex);
                final float deltaX = mLastMotionX + mLastMotionXRemainder - x;

                float absDeltaX = Math.abs(deltaX);
                mTotalMotionX += absDeltaX;

                // need to scroll the view
                if (absDeltaX >= 1.0f) {
                    int motionDelta = (int)deltaX;
                    scrollBy(motionDelta, 0);
                    mLastMotionX = x;
                    mLastMotionXRemainder = deltaX - (int) deltaX;
                }
            } else {
                determineScrollingStart(event);
            }
            break;

        case MotionEvent.ACTION_UP:
            if (mTouchState == TOUCH_STATE_SCROLLING) {
                final int activePointerId = mActivePointerId;
                final int pointerIndex = event.findPointerIndex(activePointerId);

                final int pageWidth = getMeasuredWidth();
                final float x = event.getX(pointerIndex);
                final int deltaX = (int) (x - mDownMotionX);

                boolean isSignificantMove = Math.abs(deltaX) > pageWidth * SIGNIFICANT_MOVE_THRESHOLD;

                mTotalMotionX += Math.abs(mLastMotionX + mLastMotionXRemainder - x);

                final VelocityTracker velocityTracker = mVelocityTracker;
                velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                int velocityX = (int) velocityTracker.getXVelocity(activePointerId);

                boolean isFling = mTotalMotionX > MIN_LENGTH_FOR_FLING &&
                        Math.abs(velocityX) > mFlingThresholdVelocity;

                // determine whether fling or restore
                boolean returnToOriginalPage = false;
                if (mReturnToFirstWhenOverScrollAtLastPage
                        && Math.abs(deltaX) > pageWidth * RETURN_TO_ORIGINAL_PAGE_THRESHOLD
                        && Math.signum(velocityX) != Math.signum(deltaX) && isFling) {
                    returnToOriginalPage = true;
                }

                int finalPage;
                final boolean isRtl = isLayoutRtl();
                boolean isDeltaXLeft = isRtl ? deltaX > 0 : deltaX < 0;
                boolean isVelocityXLeft = isRtl ? velocityX > 0 : velocityX < 0;

                if (((isSignificantMove && !isDeltaXLeft && !isFling) ||
                        (isFling && !isVelocityXLeft)) && (mCurrentPage > 0)) {
                    finalPage = returnToOriginalPage ? mCurrentPage : mCurrentPage - 1;
                    snapToPageWithVelocity(finalPage, velocityX);
                } else if (((isSignificantMove && isDeltaXLeft && !isFling) ||
                        (isFling && isVelocityXLeft)) && (mCurrentPage < (getPageCount() - 1))) {
                    finalPage = returnToOriginalPage ? mCurrentPage : mCurrentPage + 1;
                    snapToPageWithVelocity(finalPage, velocityX);
                } else {
                    snapToDestination();
                }
            }
            resetTouchState();
            break;

        case MotionEvent.ACTION_CANCEL:
            if (mTouchState == TOUCH_STATE_SCROLLING) {
                snapToDestination();
            }
            resetTouchState();
            break;

        case MotionEvent.ACTION_POINTER_UP:
            onSecondaryPointerUp(event);
            releaseVelocityTracker();
            break;
        }
        return super.onTouchEvent(event);
    }

    private void onSecondaryPointerUp(MotionEvent ev) {
        final int pointerIndex = (ev.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK)
                >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
        final int pointerId = ev.getPointerId(pointerIndex);
        if (pointerId == mActivePointerId) {
            // This was our active pointer going up. Choose a new
            // active pointer and adjust accordingly.
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            mLastMotionX = mDownMotionX = ev.getX(newPointerIndex);
            mActivePointerId = ev.getPointerId(newPointerIndex);
            mLastMotionXRemainder = 0;
        }
    }

    private boolean determineScrollingStart(MotionEvent ev) {
        // Disallow scrolling if we don't have a valid pointer index
        final int pointerIndex = ev.findPointerIndex(mActivePointerId);
        if (pointerIndex == INVALID_INDEX) return false;

        final float x = ev.getX(pointerIndex);
        final float y = ev.getY(pointerIndex);

        final int xDiff = (int) Math.abs(x - mDownMotionX);
        final boolean xMoved = xDiff > mTouchSlop;

        if (xMoved) {
            mTouchState = TOUCH_STATE_SCROLLING;
            mLastMotionXRemainder = 0;
            mTotalMotionX += Math.abs(mLastMotionX - x);
            mLastMotionX = x;

            // take determine distance into account?
            /*
            int motionDelta = (int)(mDownMotionX - x);
            scrollBy(motionDelta, 0);
            mLastMotionXRemainder = xDiff - (int) xDiff;
            */

            mLeftArrowDrawable.setAlpha(0x80);
            mRightArrowDrawable.setAlpha(0x80);

            return true;
        }

        return false;
    }

    private void acquireVelocityTrackerAndAddMovement(MotionEvent ev) {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(ev);
    }

    private void releaseVelocityTracker() {
        if (mVelocityTracker != null) {
            mVelocityTracker.clear();
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    private void resetTouchState() {
        releaseVelocityTracker();
        mActivePointerId = INVALID_POINTER;
        mTouchState = TOUCH_STATE_REST;
    }

    private static class ScrollInterpolator implements Interpolator {
        public ScrollInterpolator() {
        }

        public float getInterpolation(float t) {
            t -= 1.0f;
            return t*t*t*t*t + 1;
        }
    }

    /**
     * Note: this is a reimplementation of View.isLayoutRtl() since that is currently hidden api.
     */
    public boolean isLayoutRtl() {
        return (getLayoutDirection() == LAYOUT_DIRECTION_RTL);
    }

    private int computePageCount() {
        int childCount = getChildCount();
        if (childCount == 0) {
            return 0;
        }
        return ((childCount - 1) / mCountInOnePage + 1);
    }

    public int getPageCount() {
        return mPageCount;
    }

    private void snapToDestination() {
        // get page
        int whichPage = getDestinationPageIndex();
        // get duration
        int duration = getPageSnapDuration();
        // get delta
        int newX = getScrollForPage(whichPage);
        final int delta = newX - mUnboundedScrollX;

        snapToPage(whichPage, delta, duration, false);
    }

    private void snapToPageWithVelocity(int whichPage, int velocity) {
        final int newX = getScrollForPage(whichPage);
        int delta = newX - mUnboundedScrollX;
        int duration = 0;

        int halfScreenSize = getMeasuredWidth() / 2;

        velocity = Math.abs(velocity);
        velocity = Math.max(mMinSnapVelocity, velocity);

        float distanceRatio = Math.min(1f, 1.0f * Math.abs(delta) / getMeasuredWidth());
        float distance = halfScreenSize + halfScreenSize * distanceInfluenceForSnapDuration(distanceRatio);

        duration = 4 * Math.round(1000 * Math.abs(distance / velocity));

        snapToPage(whichPage, delta, duration, false);
    }

    private void snapToPage(int whichPage, int delta, int duration, boolean immediate) {
        mNextPage = whichPage;

        if (!mScroller.isFinished()) {
            abortScrollerAnimation(false);
        }

        mScroller.startScroll(mUnboundedScrollX /* startX */, 0 /* startY */,
                delta /* dx */, 0 /* dy */, duration);

        // here must to call invalidate, this will drive computeScroll be called during scroll
        invalidate();
    }

    private int getDestinationPageIndex() {
        int fullScreenSize = getMeasuredWidth();
        int halfScreenSize = fullScreenSize / 2;
        int boundary = halfScreenSize;

        int pageCount = getPageCount();
        for (int i=0; i<pageCount; i++) {
            if (mUnboundedScrollX < boundary) {
                return i;
            }
            boundary += fullScreenSize;
        }
        int page = mReturnToFirstWhenOverScrollAtLastPage ? 0 : (pageCount - 1);
        return Math.max(0, page);
    }

    private int getPageSnapDuration() {
        return PAGE_SNAP_ANIMATION_DURATION;
    }

    private int getScrollForPage(int pageIndex) {
        return getMeasuredWidth() * pageIndex;
    }

    // We want the duration of the page snap animation to be influenced by the distance that
    // the screen has to travel, however, we don't want this duration to be effected in a
    // purely linear fashion. Instead, we use this method to moderate the effect that the distance
    // of travel has on the overall snap duration.
    private float distanceInfluenceForSnapDuration(float f) {
        f -= 0.5f; // center the values about 0.
        f *= 0.3f * Math.PI / 2.0f;
        return (float) Math.sin(f);
    }

    private void abortScrollerAnimation(boolean resetNextPage) {
        mScroller.abortAnimation();
        // We need to clean up the next page here to avoid computeScrollHelper from
        // updating current page on the pass.
        if (resetNextPage) {
            mNextPage = INVALID_PAGE;
        }
    }

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            // scroll not yet finished
            scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
            // here must to call invalidate, this will drive computeScroll be
            // called during scroll
            invalidate();
        } else if (mNextPage != INVALID_PAGE) {
            // scroll stopped set current page
            mCurrentPage = mNextPage;
            updateArrowsAlpha();
            mNextPage = INVALID_PAGE;
        }
    }

    @Override
    public void scrollBy(int x, int y) {
        scrollTo(mUnboundedScrollX + x, getScrollY() + y);
    }

    @Override
    public void scrollTo(int x, int y) {
        mUnboundedScrollX = x;

        final boolean isRtl = isLayoutRtl();
        boolean isXBeforeFirstPage = isRtl ? (x > mMaxScrollX) : (x < 0);
        boolean isXAfterLastPage = isRtl ? (x < 0) : (x > mMaxScrollX);

        if (isXBeforeFirstPage) {
            if (isRtl) {
                super.scrollTo(mMaxScrollX, y);
            } else {
                super.scrollTo(0, y);
            }
        } else if (isXAfterLastPage) {
            if (isRtl) {
                super.scrollTo(0, y);
            } else {
                super.scrollTo(mMaxScrollX, y);
            }
        } else {
            super.scrollTo(x, y);
        }
    }

    private void updateMaxScrollX() {
        int pageCount = getPageCount();
        mMaxScrollX = getMeasuredWidth() * (pageCount - 1);
        mMaxScrollX = Math.max(0, mMaxScrollX);
    }

    private int getChildPageIndex(int index) {
        return index / mCountInOnePage;
    }

    public void resetScroll(boolean animate) {
        if (animate) {
            // TODO:
            scrollTo(0, 0);
        } else {
            scrollTo(0, 0);
        }
        mCurrentPage = 0;
        updateArrowsAlpha();
    }

    public void enableArrow(boolean enable) {
        mEnableArrow = enable;
    }

    @Override
    public void draw(Canvas canvas) {
        if (mEnableArrow && mPageCount > 1) {
            if (mCurrentPage > 0) {
                int left = getScrollX() + mArrowMarginHorizontal;
                mLeftArrowDrawable.setBounds(left, mArrowPaddingTop, left + mArrowWidth,
                        mArrowPaddingTop + mArrowWidth);
                mLeftArrowDrawable.draw(canvas);
            }
            if (mCurrentPage < mPageCount - 1) {
                int left = getScrollX() + getWidth() - mArrowMarginHorizontal - mArrowWidth;
                mRightArrowDrawable.setBounds(left, mArrowPaddingTop, left + mArrowWidth,
                        mArrowPaddingTop + mArrowWidth);
                mRightArrowDrawable.draw(canvas);
            }
        }
        super.draw(canvas);
    }

    private void updateArrowsAlpha() {
        if (mCurrentPage == 0) {
            mLeftArrowDrawable.setAlpha(0);
        } else {
            mLeftArrowDrawable.setAlpha(0xFF);
        }
        if (mCurrentPage == (mPageCount - 1)) {
            mRightArrowDrawable.setAlpha(0);
        } else {
            mRightArrowDrawable.setAlpha(0xFF);
        }
        invalidate();
    }

}
