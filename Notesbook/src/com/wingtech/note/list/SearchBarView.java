/*===================================================================================================*
 *  when  |      who     |    keyword           |        why         |         what                  *
 *===================================================================================================*
 *20160407|mengzhiming.wt|porting A1s enjoynotes| Port_EnjoyNotes | porting A1s enjoynotes to CMCC N2*
*====================================================================================================*
*20160531|mengzhiming.wt|   customer req       | customer req    | customer req                      *
 *===================================================================================================*/

package com.wingtech.note.list;

import android.content.Context;
import android.graphics.Canvas;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.wingtech.note.R;
import android.util.Log;

public class SearchBarView extends FrameLayout {
    private TextView mSearchInput;
    private ViewGroup mSearchPanel;
    private InputMethodManager mInputMethodManager;
    private float mInterpolation;

    public SearchBarView(Context paramContext) {
        super(paramContext);
    }

    public SearchBarView(Context paramContext, AttributeSet paramAttributeSet) {
        super(paramContext, paramAttributeSet);
    }

    public SearchBarView(Context paramContext, AttributeSet paramAttributeSet,
            int paramInt) {
        super(paramContext, paramAttributeSet, paramInt);
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        mInputMethodManager = ((InputMethodManager) getContext()
                .getSystemService("input_method"));
        mInterpolation = 1.0F;
        mSearchInput = ((TextView) findViewById(R.id.search_input));
        mSearchPanel = ((ViewGroup) findViewById(R.id.search_panel));
    }

    public void addSearchTextChangedListener(TextWatcher paramTextWatcher) {
        mSearchInput.addTextChangedListener(paramTextWatcher);
    }

    public void hideInputMethodForSearch() {
        mInputMethodManager.hideSoftInputFromWindow(
                mSearchInput.getWindowToken(), 0);
    }

    public void showInputMethodForSearch() {
        mSearchInput.requestFocus();
        mInputMethodManager.showSoftInput(mSearchInput, 0);
    }

    public void setInterpolation(float paramFloat) {
        if ((paramFloat >= -1) && (paramFloat <= 1)) {
            mInterpolation = paramFloat;
            // if((3 * mInterpolation - 2)<=0){
            // invalidate();
            // }
            invalidate();
        }
    }

    public void setSearchInputAlpha(float paramFloat) {
        mSearchInput.setAlpha(paramFloat);
    }

    private void updateChildStaticTransformation() {
        mSearchPanel.setTranslationX(-mSearchPanel.getMeasuredWidth()
                * mInterpolation);
        float f = Math.max(0, 1 - 3 * mInterpolation);
        mSearchPanel.setAlpha(f);
    }

    public String getSearchText() {
        return mSearchInput.getText().toString();
    }

    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        //updateChildStaticTransformation();
        return super.drawChild(canvas, child, drawingTime);
    }

}
