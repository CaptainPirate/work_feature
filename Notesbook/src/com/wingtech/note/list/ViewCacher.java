/*===================================================================================================*	
 *  when  |      who     |    keyword           |        why         |         what                  *	
 *===================================================================================================*	
 *20160407|mengzhiming.wt|porting A1s enjoynotes| Port_EnjoyNotes | porting A1s enjoynotes to CMCC N2*	
*====================================================================================================*/

package com.wingtech.note.list;

import android.util.SparseArray;
import android.view.View;
import android.widget.AbsListView;

import java.util.HashSet;

public class ViewCacher {
    private SparseArray<HashSet<View>> mActiveMap = new SparseArray<HashSet<View>>();
    private AbsListView.RecyclerListener mRecyclerListener;
    private SparseArray<HashSet<View>> mScrapMap = new SparseArray<HashSet<View>>();

    private void addViewToMap(SparseArray<HashSet<View>> paramSparseArray, View view,
            int key) {
        HashSet<View> viewHS = (HashSet<View>) paramSparseArray.get(key);
        if (viewHS == null) {
            viewHS = new HashSet<View>();
        }
        viewHS.add(view);
        paramSparseArray.put(key, viewHS);
    }

    public void addActiveView(View view, int key) {
        addViewToMap(mActiveMap, view, key);
    }

    public void addScrapView(View view, int key) {
        addViewToMap(mScrapMap, view, key);
    }

    public View getScrapView(int key) {
        HashSet<View> localHashSet = (HashSet<View>) mScrapMap.get(key);
        View view;
        if ((localHashSet == null) || (localHashSet.isEmpty()))
            view = null;
        else {
            view = (View) localHashSet.iterator().next();
            localHashSet.remove(view);
        }
        return view;
    }

    public void recyleView(View view) {
        for (int i = 0; i < mActiveMap.size(); i++) {
            if (((HashSet<View>) mActiveMap.valueAt(i)).remove(view))
                addScrapView(view, mActiveMap.keyAt(i));
        }
        if (mRecyclerListener != null)
            mRecyclerListener.onMovedToScrapHeap(view);
    }

    public void setRecyclerListener(AbsListView.RecyclerListener listener) {
        mRecyclerListener = listener;
    }

}
