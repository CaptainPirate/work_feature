/*===================================================================================================*	
 *  when  |      who     |    keyword           |        why         |         what                  *	
 *===================================================================================================*	
 *20160407|mengzhiming.wt|porting A1s enjoynotes| Port_EnjoyNotes | porting A1s enjoynotes to CMCC N2*	
*====================================================================================================*/

package com.wingtech.note.list;

import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.GridLayout;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;
import android.view.Gravity;
import com.wingtech.note.R;

public class NoteGridView extends GridLayout implements View.OnClickListener,
        View.OnLongClickListener {
    private Adapter mAdapter;
    private AdapterView.OnItemClickListener mOnItemClickListener;
    private AdapterView.OnItemLongClickListener mOnItemLongClickListener;
    private ViewCacher mRecycler;
    private boolean mRecycled = true;
    private int mStartPos;
    private Context mContext;

    public NoteGridView(Context paramContext) {
        super(paramContext);
        //other,tangzihui.wt,add,2015.07.06,add variable.
        mContext = paramContext;
    }

    public NoteGridView(Context paramContext, AttributeSet paramAttributeSet) {
        super(paramContext, paramAttributeSet);
      //other,tangzihui.wt,add,2015.07.06,add variable.
        mContext = paramContext;
    }

    public NoteGridView(Context paramContext, AttributeSet paramAttributeSet, int paramInt) {
        super(paramContext, paramAttributeSet, paramInt);
      //other,tangzihui.wt,add,2015.07.06,add variable.
        mContext = paramContext;
    }

    public void setup(Adapter adapter, int startPos, ViewCacher recycler,
            AdapterView.OnItemClickListener onItemClickListener,
            AdapterView.OnItemLongClickListener onItemLongClickListener) {
        if ((mAdapter != adapter) || (mStartPos != startPos)
                || (mRecycler != recycler)
                || (mOnItemClickListener != onItemClickListener)
                || (mOnItemLongClickListener != onItemLongClickListener)) {
            mStartPos = startPos;
            mRecycler = recycler;
            mAdapter = adapter;
            mOnItemClickListener = onItemClickListener;
            mOnItemLongClickListener = onItemLongClickListener;
        }
    }

    public void recycle() {
        for (int i = 0; i < getChildCount(); i++) {
            mRecycler.recyleView(getChildAt(i));
        }
        removeAllViews();
        mRecycled = true;
    }

    public boolean onLongClick(View view) {
        if (mOnItemLongClickListener != null) {
            int i = mStartPos + indexOfChild(view);
            mOnItemLongClickListener.onItemLongClick(null, view, i, mAdapter.getItemId(i));
            return true;
        }
        return false;
    }

    protected void onDetachedFromWindow() {
        recycle();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onAttachedToWindow() {
        // TODO Auto-generated method stub
        super.onAttachedToWindow();
    }

    public void onClick(View view) {
        if (mOnItemClickListener != null)
        {
            int i = mStartPos + indexOfChild(view);
            mOnItemClickListener.onItemClick(null, view, i, mAdapter.getItemId(i));
        }
    }

    public int getMaxSize() {
        return getRowCount() * getColumnCount();
    }

    public void bindView() {
        if ((mRecycled) && (mAdapter != null)) {
            int maxSize = getMaxSize();
            int dispNum = Math.min(mAdapter.getCount(), maxSize + mStartPos);
            //+other,tangzihui.wt,modified,2015.07.06,for display information when no notes on the list.
            if (dispNum > 0) {
                for (int i = mStartPos; i < dispNum; i++) {
                    int viewType = mAdapter.getItemViewType(i);
                    View convertView = mRecycler.getScrapView(viewType);
                    View dispView = mAdapter.getView(i, null, this);
                    if (dispView != null) {
                        dispView.setOnClickListener(this);
                        dispView.setOnLongClickListener(this);
                        addView(dispView);
                        mRecycler.addActiveView(dispView, viewType);
                    }
                }
            } else {
                TextView mEmptyView = new TextView(mContext);
                mEmptyView.setText(R.string.none_notes);
                mEmptyView.setTextAppearance(mContext, R.style.TextAppearance);
                mEmptyView.setGravity(Gravity.CENTER);
                GridLayout.LayoutParams param = new GridLayout.LayoutParams();
                param.columnSpec = GridLayout.spec(0, 2);
                param.rowSpec = GridLayout.spec(0, 3);
                param.setGravity(Gravity.CENTER);
                mEmptyView.setLayoutParams(param);
                addView(mEmptyView);
            }
          //-other,tangzihui.wt,modified,2015.07.06,for display information when no notes on the list.
            mRecycled = false;
        }
    }
}
