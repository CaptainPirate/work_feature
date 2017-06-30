/*===================================================================================================*
 *  when  |      who     |    keyword           |        why         |         what                  *
 *===================================================================================================*
 *20160407|mengzhiming.wt|porting A1s enjoynotes| Port_EnjoyNotes | porting A1s enjoynotes to CMCC N2*
*====================================================================================================*/

package com.wingtech.note.editor;

import com.wingtech.note.R;
import com.wingtech.note.Utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.text.util.Linkify;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ScrollView;

public class RichEditor extends ScrollView {

    private RichEditView mEdit;
    private boolean mScrollInSketch = false;// 涂鸦模式下滚动

    public RichEditor(Context context) {
        this(context, null);
    }

    public RichEditor(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.editTextStyle);
    }

    public RichEditor(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        View v = LayoutInflater.from(context).inflate(R.layout.richeditor, this, false);
        mEdit = (RichEditView) v.findViewById(R.id.richeditview);
        mEdit.setHint(R.string.hint_create_note);//bug188112,mengzhiming.wt,20160616
        //mEdit.setAutoLinkMask(Linkify.ALL);//modified number linked,mengzhiming.wt,20160602
        mEdit.setLinksClickable(false);
        mEdit.setGravity(Gravity.TOP);
        mEdit.setSingleLine(false);
        addView(v, new
                FrameLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
        LayoutParams params = (LayoutParams) mEdit.getLayoutParams();
        params.setMargins(20, 0, 20, 0);//bug183435,mengzhiming.wt,20160602
        mEdit.setLayoutParams(params);
        setPadding(0, 0, 0, 0);
    }

    // 滚动时加载上下文图片，优化内存
    public void computeScroll() {
        super.computeScroll();
        mEdit.preLoadImages(getHeight() - getPaddingTop() - getPaddingBottom(), getScrollY());
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        // TODO Auto-generated method stub
        super.onLayout(changed, l, t, r, b);
    }

    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int i = View.MeasureSpec.getMode(heightMeasureSpec);
        if (i == View.MeasureSpec.AT_MOST || i == View.MeasureSpec.EXACTLY) {
            mEdit.setMinHeight(View.MeasureSpec.getSize(heightMeasureSpec));
        } else {
            mEdit.setMinHeight(0);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    public void setCursorVisible(boolean visibility) {
        mEdit.setCursorVisible(visibility);
    }

    //+bug84140,tangzihui.wt,add,2015.07.30,request ChildView focus.
    public void requestChildFocus() {
        mEdit.requestFocus();
    }
    //-bug84140,tangzihui.wt,add,2015.07.30,request ChildView focus.

    public CharSequence getRichText() {
        return mEdit.getRichText();
    }

    public void setEditorClickListener(View.OnClickListener paramOnClickListener) {
        mEdit.setOnClickListener(paramOnClickListener);
        RichEditView localRichEditView = this.mEdit;
        if (paramOnClickListener == null)
            localRichEditView.setReadonly(true);
    }

    public void insertContact(Uri[] uris) {
        mEdit.insertContact(uris);
    }

    public void insertContact(Uri uri) {
        mEdit.insertContact(uri);
    }

    public void insertImage(Uri uri) {
        mEdit.insertImage(uri);
    }

    public void setFontSizeId(int size) {
        mEdit.setFontSizeId(size);
    }

    public void setNoteId(long id) {
        mEdit.setNoteId(id);
    }

    public void setRichText(CharSequence richText) {
        mEdit.setRichText(richText);
    }

    public void setTextPadding(int left, int right) {
        mEdit.setPadding(left, 0, right, 0);
    }

    public void setQuery(String rex) {
        mEdit.setQueryPattern(rex);
    }

    public void showInputMethod() {
        Utils.showSoftInput(mEdit);
    }

    public void hideInputMethod() {
        Utils.hideSoftInput(mEdit);
    }

    public int getCurrentSketchMarginTop() {
        return getPaddingTop() + computeVerticalScrollOffset();
    }

    public int[] getCurrentTextSelection() {
        int[] selection = new int[2];
        selection[0] = mEdit.getSelectionStart();
        selection[1] = mEdit.getSelectionEnd();
        return selection;
    }

    public void setCurrentTextSelection(int[] selection) {
        mEdit.setSelection(selection[0], selection[1]);
    }

    public void setCurrentTextSelection(int selection) {
        mEdit.setSelection(selection);
    }

    public void insertSketchImage(Bitmap bmp) {
        // TODO Auto-generated method stub
        mEdit.insertSketchImage(bmp);
    }

    //bug183561,mengzhiming.wt,modified 20160608,start
    public RichEditView getRichEditView() {
        return mEdit;
    }
    //bug183561,mengzhiming.wt,modified 20160608,end
}
