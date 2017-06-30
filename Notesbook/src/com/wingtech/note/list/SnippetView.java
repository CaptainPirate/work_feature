/*===================================================================================================*	
 *  when  |      who     |    keyword           |        why         |         what                  *	
 *===================================================================================================*	
 *20160407|mengzhiming.wt|porting A1s enjoynotes| Port_EnjoyNotes | porting A1s enjoynotes to CMCC N2*	
*====================================================================================================*/

package com.wingtech.note.list;

import android.app.ActionBar.LayoutParams;
import android.content.Context;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.TextAppearanceSpan;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import com.wingtech.note.R;

public class SnippetView extends TextView {
    private CharSequence mFullText;
    private String mToken;

    public SnippetView(Context context) {
        super(context);
        // TODO Auto-generated constructor stub
    }

    public SnippetView(Context context, AttributeSet attr) {
        super(context, attr);
    }

    public SnippetView(Context context, AttributeSet attr, int defStyle) {
        super(context, attr, defStyle);
    }

    public void setText(CharSequence paramCharSequence, String paramString) {
        mFullText = paramCharSequence;
        mToken = paramString;
        requestLayout();
    }

    private CharSequence highlightText(CharSequence fullText, String token) {
        if (TextUtils.isEmpty(fullText))
            return fullText;
        String lowerToken = token.toLowerCase();
        String[] strs = fullText.toString().split("\n");
        for (int i = 0; i < strs.length; i++) {
            String str = strs[i];
            String lowerStr = str.toLowerCase();
            int index = lowerStr.indexOf(lowerToken);
            if (index >= 0) {
                SpannableString localSpannableString = new SpannableString(fullText.subSequence(i,
                        i + lowerStr.length()));
                while (index >= 0) {
                    localSpannableString.setSpan(new TextAppearanceSpan(getContext(),
                            R.style.TextAppearance_HighLight), index, index + token.length(),
                            Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                    index = lowerStr.indexOf(lowerToken, index + token.length());
                }
                return localSpannableString;
            }
        }
        if (strs.length > 0)
            return fullText.subSequence(0, strs[0].length());
        return "";
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if ((View.MeasureSpec.getSize(widthMeasureSpec) <= 0) || (TextUtils.isEmpty(mToken))) {
            setText(mFullText);
        } else {
            CharSequence localCharSequence = highlightText(mFullText, mToken);
            setText(localCharSequence);
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            int i = localCharSequence.toString().indexOf(mToken);
            if (i >= 0) {
                TextPaint localTextPaint = getPaint();
                float tokenLen = localTextPaint.measureText(mToken);
                float lineLen = localTextPaint.measureText(localCharSequence, 0,
                        localCharSequence.length());
                float maxWidth = getMeasuredWidth() - getPaddingLeft() - getPaddingRight();
                if (lineLen > maxWidth) {
                    float newWidth = maxWidth - localTextPaint.measureText(getContext().getString(R.string.str_more)) * 2;
                    int start = i;
                    int end = i + mToken.length();
                    if (tokenLen > maxWidth) {// 检出的字符串长度就超出
                        while (localTextPaint.measureText(localCharSequence, start,
                                end) > newWidth) {
                            start = Math.max(start + 1, end);
                            end = Math.min(end - 1, start);
                        }
                    }
                    else {
                        while (localTextPaint.measureText(localCharSequence, start,
                                end) < newWidth) {
                            start = Math.max(start - 1, 0);
                            end = Math.min(end + 1, localCharSequence.length());
                        }
                    }
                    SpannableStringBuilder localSpannableStringBuilder = new SpannableStringBuilder(
                            localCharSequence);
                    localSpannableStringBuilder.delete(end, localSpannableStringBuilder.length());
                    localSpannableStringBuilder.delete(0, start);
                    if (start != 0)
                        localSpannableStringBuilder.insert(0, getContext().getString(R.string.str_more));
                    if (end != localCharSequence.length())
                        localSpannableStringBuilder.append(getContext().getString(R.string.str_more));
                    setText(localSpannableStringBuilder);
                }
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

    }
}
