/*===================================================================================================*	
 *  when  |      who     |    keyword           |        why         |         what                  *	
 *===================================================================================================*	
 *20160407|mengzhiming.wt|porting A1s enjoynotes| Port_EnjoyNotes | porting A1s enjoynotes to CMCC N2*	
*====================================================================================================*/

package com.wingtech.note.list;

import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager.SavedState;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

public class ScrollViewAdapter extends PagerAdapter {
    private ArrayList<NoteGridView> lists;
    private View mHeaderView;

    public ScrollViewAdapter(ArrayList<NoteGridView> views) {
        lists = views;
    }

    public int getCount() {
        return lists.size() + ((mHeaderView == null) ? 0 : 1);
    }

    @Override
    public void startUpdate(View container) {

    }

    @Override
    public Object instantiateItem(View container, int position) {
        if (mHeaderView != null) {
            if (position == 0) {
                ((ViewGroup) container).addView(mHeaderView);
                return mHeaderView;
            }
            else {
                NoteGridView v = lists.get(position - 1);
                v.bindView();
                ((ViewGroup) container).addView(v);
                return v;
            }
        }
        else {
            NoteGridView v = lists.get(position);
            v.bindView();
            ((ViewGroup) container).addView(v);
            return v;
        }
    }

    public boolean isHadChild(ViewGroup parent, View v) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            if (v == parent.getChildAt(i))
                return true;
        }
        return false;
    }

    // �˴���ֹԽ�����
    @Override
    //+other,tangzihui.wt,modify,2015.07.16,remove ViewPager item.
    public void destroyItem(ViewGroup container, int position, Object object) {
        View v = ( View) object;
        if(v instanceof NoteGridView ){
            NoteGridView noteGridView = (NoteGridView)object;
            noteGridView.recycle();
            container.removeView(noteGridView);
        }else{
            container.removeView(v);
        }
       /* if (position <= getCount() - 1) {
            if (mHeaderView != null) {
                if (position == 0) {
                    container.removeView((View)object);
                }
                else {
                    NoteGridView v = lists.get(position - 1);
                    v.recycle();
                    container.removeView((View)object);
                }
            }
            else {
                NoteGridView v = lists.get(position);
                v.recycle();
                container.removeView((View)object);
            }
        }*/
    }
    //-other,tangzihui.wt,modify,2015.07.16,remove ViewPager item.

    @Override
    public void finishUpdate(View container) {

    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public Parcelable saveState() {
        return null;
    }

    @Override
    public void restoreState(Parcelable state, ClassLoader loader) {

    }

    @Override
    public int getItemPosition(Object object) {
        // TODO Auto-generated method stub
        return POSITION_NONE;
    }

    public void setHeaderView(View v) {
        mHeaderView = v;
    }

    public View getHeaderView() {
        return mHeaderView;
    }

    public View getItem(int pos) {
        if (mHeaderView == null)
            return lists.get(pos);
        else if (pos == 0)
            return mHeaderView;
        else
            return lists.get(pos - 1);

    }

    public boolean hasHeadView() {
        return mHeaderView == null ? false : true;
    }
}

