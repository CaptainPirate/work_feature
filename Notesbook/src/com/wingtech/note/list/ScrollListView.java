/*====================================================================================================*
*20160531|mengzhiming.wt|   customer req       | customer req    | customer req                      *
 *===================================================================================================*/


package com.wingtech.note.list;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ListView;

public class ScrollListView extends ListView {

    public ScrollListView(Context context, AttributeSet attrs) {
        super(context,attrs);
    }


    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int mExpandSpec = MeasureSpec.makeMeasureSpec(Integer.MAX_VALUE >> 2, MeasureSpec.AT_MOST);
        super.onMeasure(widthMeasureSpec, mExpandSpec);
    }
}
