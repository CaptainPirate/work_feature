/*
  * Copyright @ 2015 China Mobile Group Device Co.,Ltd.
  * All rights Reserved.
*/

package com.android.launcher3;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.ImageView;

public class CmccInsettableImageView extends ImageView implements Insettable {

    public CmccInsettableImageView(Context context) {
        super(context);
    }

    public CmccInsettableImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public CmccInsettableImageView(Context context, AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setInsets(Rect insets) {
        // do nothing, just avoid DragLayer to set margin of this ImageView
    }

}
