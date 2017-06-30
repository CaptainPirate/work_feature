/*===================================================================================================*
 *  when  |      who     |    keyword           |        why         |         what                  *
 *===================================================================================================*
 *20160407|mengzhiming.wt|porting A1s enjoynotes| Port_EnjoyNotes | porting A1s enjoynotes to CMCC N2*
*====================================================================================================*
*20160531|mengzhiming.wt|   customer req       | customer req    | customer req                      *
 *===================================================================================================*/

 package com.wingtech.note.list;

import android.animation.Animator;
import android.app.Activity;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.provider.Settings;
import com.wingtech.note.Utils;
import com.wingtech.note.data.DataUtils;
import com.wingtech.note.data.NoteConstant;
import com.wingtech.note.loader.ImageCacheManager;
import com.wingtech.note.R;
import android.util.Log;

import com.wingtech.note.list.ScrollListView;

import android.widget.Button;
import android.graphics.Color;
import android.app.AlertDialog;
import android.content.DialogInterface;
import com.wingtech.note.data.NoteConstant.NoteColumns;

//import com.wingtech.note.ViewHolder;

import android.widget.TextView;
import android.app.ActionBar;
import android.view.Gravity;

import java.util.HashSet;
//import android.widget.CompoundButton.OnCheckedChangeListener;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;



public class NotesGridFragment extends NotesBaseFragment implements
        OnItemClickListener, OnItemLongClickListener,LoaderManager.LoaderCallbacks<Cursor>/*, OnPageChangeListener*/ {//bug 187447,mengzhiming.wt modified,20160615
    private static final int TOKEN_SEARCH_NOTE = 1;
    private static final int TOKEN_SEARCH_DEL_NOTE = 2;
    private NotesListActivity mActivity;
    private NotesGridAdapter mAdapter;
    private ViewCacher mRecycler;
    private int mRootScreenIndex;
    private NotesScrollView mScrollView;
    private CirclePageIndicator mIndicator;
    private View mFragment;
    private NotesSRListAdapter mListAdapter;
    private BackgroundQueryHandler mBackgroundQueryHandler;
    //private TitleBarView mTitleBar;//bug198629,mengzhiming.wt,del,20160718
    private String mSeachChar = null;
    private int mtoken = TOKEN_SEARCH_NOTE;
    private boolean mIsMultiChoiceMode = false;
    private View mBottomPanel;
    private Button mDeleteButton;
    private Button mDeleteCancelButton;
    private TextView mTitleText;
    private Button mSelectAll;
    private boolean mIsSelectedAll = false;
    private int mImportantCount = 0;
    private TextView mNoNoteText;// bug186676,mengzhiming.wt,add 20160612
    private EditText msearch_input;//bug 186504,mengzhiming.wt,add,monkey 20160613

    private TextWatcher mSearchTextWatcher = new TextWatcher() {
        public void afterTextChanged(Editable e) {
            String str = e.toString();
            mSeachChar = str;
            onQueryTextChange(str);
        }

        public void beforeTextChanged(CharSequence s, int start, int count,
                int after) {
            // TODO Auto-generated method stub
            //EditText search_input = (EditText)mSearchBar.findViewById(R.id.search_input);//bug 186504,mengzhiming.wt,del,monkey 20160613
        }

        public void onTextChanged(CharSequence s, int start, int before,
                int count) {
            // TODO Auto-generated method stub

        }
    };

    private View mSearchScreen;
    private ScrollListView mListView;
    //private ScrollListView mSelectedList;
    private SearchBarView mSearchBar;

    public NotesGridFragment() {
        super();
    }

    @Override
    public NotesBaseAdapter getAdapter() {
        return mAdapter;
    }

    @Override
    public void onDataSetChanged(Cursor paramCursor) {
        refresh();
        mIndicator.notifyDataSetChanged();
    }

    public ActionMode startActionMode(ActionMode.Callback callback) {
        return mScrollView.startActionMode(callback);
    }

    @Override
    public int translatePosition(int paramInt) {
        // TODO Auto-generated method stub
        return paramInt;
    }

    @Override
    public void onAttach(Activity activity) {
        // TODO Auto-generated method stub
        mSeachChar = "";
        mtoken = TOKEN_SEARCH_NOTE;
        super.onAttach(activity);
        mActivity = ((NotesListActivity) activity);
        mAdapter = new NotesGridAdapter(mActivity);
        mRecycler = new ViewCacher();
        mListAdapter = new NotesSRListAdapter(mActivity);
        mBackgroundQueryHandler = new BackgroundQueryHandler(
                mActivity.getContentResolver());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mFragment = inflater.inflate(R.layout.notes_grid_fragment, container,
                false);
        mScrollView = (NotesScrollView) mFragment.findViewById(R.id.pager);
        mBottomPanel = mFragment.findViewById(R.id.mbottom_panel);
        mIndicator = (CirclePageIndicator) mFragment
                .findViewById(R.id.indicator);
        mIndicator.setOrientation(CirclePageIndicator.HORIZONTAL);
        initialize();
        ImageCacheManager.getInstance(mActivity).start();
        getLoaderManager().initLoader(0, null, this);//bug 187447,mengzhiming.wt add,20160615
        return mFragment;
    }

    @Override
    public void onResume() {
        Log.e("meng", "onResume mSeachChar=" + mSeachChar);
        onQueryTextChange(mSeachChar);
        super.onResume();
        getLoaderManager().initLoader(0, null, this);//bug 187447,mengzhiming.wt add,20160615
    }

    private void initialize() {
        //bug198629,mengzhiming.wt,modified,20160718,start
        // mTitleBar = ((TitleBarView)
        // getActivity().getActionBar().getCustomView());
        //mTitleBar = ((TitleBarView) getActivity().findViewById(
        //        R.id.notes_title_bar));
        setupActionBar();
        //bug198629,mengzhiming.wt,modified,20160718,end
        /*mSearchScreen = LayoutInflater.from(mActivity).inflate(
                R.layout.notes_search_screen, mScrollView, false);
        */
        mSearchScreen = mFragment.findViewById(R.id.notes_search_screen);
        mSearchBar = ((SearchBarView) mSearchScreen
                .findViewById(R.id.notes_search_bar));
        mSearchBar.addSearchTextChangedListener(mSearchTextWatcher);
        //bug 186504,mengzhiming.wt,add,monkey 20160613,begin
        msearch_input = (EditText)mSearchBar.findViewById(R.id.search_input);
        msearch_input.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                // do nothing
            }
        });
        //bug 186504,mengzhiming.wt,add,monkey 20160613,end
        //bug198611 ,mengzhiming.wt,add,monkey 20160715,start
        msearch_input.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    // Set focusable to false so that onClick event can be passed in.
                    v.setFocusable(true);
                    v.setFocusableInTouchMode(true);
                }
                return false;
            }
        });
        //bug198611 ,mengzhiming.wt,add,monkey 20160715,end
        mNoNoteText = (TextView)mSearchScreen.findViewById(R.id.no_note);// bug186676,mengzhiming.wt,add 20160612

        mRecycler.setRecyclerListener(new AbsListView.RecyclerListener() {
            public void onMovedToScrapHeap(View view) {
                if ((view instanceof INotesListItem))
                    ((INotesListItem) view).onRecycle();
            }
        });
        mScrollView.setOnItemClickListener(this);
        mScrollView.setOnItemLongClickListener(this);
        mScrollView.setViewRecycler(mRecycler);
        mScrollView.setAdapter(mAdapter);
        //mScrollView.setOnPageChangeListener(mIndicator);
        mIndicator.setViewPager(mScrollView);
        //mIndicator.setOnPageChangeListener(this);
        mDeleteCancelButton = (Button) mBottomPanel.findViewById(R.id.cancel_button);
        mDeleteButton = (Button) mBottomPanel.findViewById(R.id.import_button);

        mDeleteCancelButton.setOnClickListener(new android.view.View.OnClickListener() {

            public void onClick(View arg0) {
                exitMultiSelectView();
            }
        });


        mDeleteButton.setOnClickListener(new android.view.View.OnClickListener() {
            public void onClick(View arg0) {
                //String text = "";
                //int token = 0;
                if (mListAdapter.getSelectedCount() > 0 ){
                    showDeleteDialog();
                }
            }
        });

        mListView = ((ScrollListView) mSearchScreen.findViewById(R.id.list));
        //((ViewGroup.MarginLayoutParams) mListView.getLayoutParams()).height = ViewGroup.MarginLayoutParams.WRAP_CONTENT;
        // mListView.setBackgroundResource(R.drawable.grid_list_bg);
        //mListView.setDivider(null);
        mListView.setSelector(R.drawable.listselector);
        mListView.setAdapter(mListAdapter);
        mListView.setEmptyView(mNoNoteText);// bug186676,mengzhiming.wt,add 20160612
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {
                // TODO Auto-generated method stub
                if (id >= 0) {
                    if (mIsMultiChoiceMode) {
                        NotesSRListItem item = null;
                        item = (NotesSRListItem) mListView.getChildAt(position);

                        boolean checkedState = item.getCheckBox().isChecked();
                        item.setSelectedBackGroud(!checkedState);

                        mListAdapter.setCheckedItem(position,!checkedState);

                         refreshButton();

                    } else {
                        String str = mSearchBar.getSearchText().trim();
                        mActivity.openNote(id, str);
                    }
                }
            }
        });
        mScrollView.setHeadView(mSearchScreen);
        mIndicator.setCurrentItem(1);
    }

    private void showDeleteDialog() {
            AlertDialog.Builder localBuilder = new AlertDialog.Builder(getActivity());
            localBuilder.setTitle(R.string.alert_title_delete);
            //localBuilder.setIconAttribute(android.R.attr.alertDialogIcon);
            localBuilder.setMessage(R.string.alert_message_delete_note);
            localBuilder.setPositiveButton(android.R.string.ok,
                    new DialogInterface.OnClickListener() {
                        public void onClick(
                                DialogInterface paramAnonymousDialogInterface,
                                int paramAnonymousInt) {
                            deleteSelectedItems();
                            refreshButton();
                            exitMultiSelectView();
                            //mActiveDialog.dismiss();
                            //mModeCallBack.finishActionMode();
                        }
                    });
            localBuilder.setNegativeButton(android.R.string.cancel, null);
            localBuilder.show();
        }

    public void deleteSelectedItems() {
        HashSet<Long> delNoteIds = mListAdapter.getSelectedNoteIds();
        DataUtils.batchDeleteNotes(mActivity.getContentResolver(), delNoteIds);
    }

    public boolean onQueryTextChange(String queryString) {
        String str = queryString.trim();
        mListAdapter.setSearchToken(str);
        int token = getToken();
        if (str.isEmpty()) {

            //mListAdapter.changeCursor(null);

            DataUtils.startQueryForAll(mBackgroundQueryHandler, token, null,
                    NoteItemData.PROJECTION);
        } else
            DataUtils.search(mBackgroundQueryHandler, token, null,
                    NoteItemData.PROJECTION, 0L, str);
        return true;
    }

    public Animator getInAnimator(long duration) {
        return Utils.buildFadeInAnimator(mScrollView, duration);
    }

    public Animator getOutAnimator(long duration) {
        return Utils.buildFadeOutAnimator(mScrollView, duration);
    }

    public void onItemClick(AdapterView<?> parent, View view, int position,
            long id) {
        if (mOnItemClickListener != null)
            mOnItemClickListener.onItemClick(parent, view, position, id);
    }

    @Override
    public void enterEditMode() {
        // TODO Auto-generated method stub
        super.enterEditMode();
        if (mFolderId == 0L) {
            int i = mScrollView.getCurrentItem() - 1;
            mScrollView.setHeadView(null);
            mIndicator.setCurrentItem(i);
        }
    }

    @Override
    public void exitEditMode() {
        // TODO Auto-generated method stub
        super.exitEditMode();
        if (mFolderId == 0L) {
            int i = mScrollView.getCurrentItem() + 1;
            mScrollView.setHeadView(mSearchScreen);
            mIndicator.setCurrentItem(i);
            //bug198629,mengzhiming.wt,del,20160718,start
            //mTitleBar.setAppearenceMode(TitleBarView.APPEARENCE_MODE_GRID_ROOT);
            // ������ʱ�����ⶶ������
            //bug198629,mengzhiming.wt,del,20160718,end
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public void enterSubFolderMode(long paramLong) {
        super.enterSubFolderMode(paramLong);
        mRootScreenIndex = mScrollView.getCurrentItem();
        mScrollView.setHeadView(null);
        mIndicator.setCurrentItem(0);
    }

    public void exitSubFolderMode() {
        super.exitSubFolderMode();
        if (mFolderId == 0) {
            int i = 1 + mScrollView.getCurrentItem();
            mScrollView.setHeadView(mSearchScreen);
            mIndicator.setCurrentItem(i);
        }
    }

    public void restoreRootPosition() {
        mIndicator.setCurrentItem(mRootScreenIndex);
    }

    private final class BackgroundQueryHandler extends AsyncQueryHandler {
        public BackgroundQueryHandler(ContentResolver contentResolver) {
            super(contentResolver);
        }

        protected void onQueryComplete(int paramInt, Object paramObject,
                Cursor paramCursor) {
			/*porting A1s enjoynotes, mengzhiming.wt,modified 20160405
			boolean b = Settings.Global.getInt(mActivity.getContentResolver(),
                    Settings.Global.VISITOR_MODE_ON, 0) == 1 ? true : false;
            if (b)
                return;
			*/
            if (paramInt == TOKEN_SEARCH_NOTE) {
                mListAdapter.changeCursor(paramCursor);
            } else if (paramInt == TOKEN_SEARCH_DEL_NOTE) {
                mListAdapter.changeCursor(paramCursor);
                enterMultiSelectView();
            }
            mActivity.updateDeleteMenu(paramCursor);//bug 187447,mengzhiming.wt add,20160615
        }
    }
/*
    public void onPageScrollStateChanged(int arg0) {

    }

    public void onPageScrolled(int position, float positionOffset,
            int positionOffsetPixels) {
        if (!((ScrollViewAdapter) mScrollView.getAdapter()).hasHeadView()) {
            mSearchBar.setInterpolation(1);
            return;
        }
        if (position == 0) {
            mSearchBar.setInterpolation(positionOffset);
        } else
            mSearchBar.setInterpolation(1);
    }

    public void onPageSelected(int arg0) {
        if (arg0 == 0 && mFolderId == 0 && !mIsInEditMode) {
            mSearchBar.showInputMethodForSearch();
        } else {
            mSearchBar.hideInputMethodForSearch();
        }

    }*/

    public void refresh() {
        // TODO Auto-generated method stub
        mScrollView.refreshAllScreens();
    }

    public void setToken(int token) {
        mtoken = token;
    }


    public int getToken() {
        return mtoken;
    }

    public boolean onQueryDelList() {
        mtoken = TOKEN_SEARCH_DEL_NOTE;
        return onQueryTextChange(mSeachChar);
    }


    private void enterMultiSelectView() {
         if (!mIsMultiChoiceMode) {
            mIsMultiChoiceMode = true;
            mSearchBar.setVisibility(View.GONE);
            mBottomPanel.setVisibility(View.VISIBLE);
            mDeleteButton.setClickable(false);
            mDeleteButton.setTextColor(Color.rgb(177, 177, 177));//bug 185672,mengzhiming.wt modified,20160606
            mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
            mListView.setClickable(false);
            mListView.setLongClickable(false);
            mListView.invalidateViews();
            mListAdapter.setChoiceMode(true);
            setupMulSelectActionBar();
            mActivity.setMenuVisible(false);

        }
    }

    private void exitMultiSelectView() {
        if (mIsMultiChoiceMode) {

            mIsMultiChoiceMode = false;
            mSearchBar.setVisibility(View.VISIBLE);
            mBottomPanel.setVisibility(View.GONE);
            unCheckAll();
            mListView.setChoiceMode(ListView.CHOICE_MODE_NONE);
            mListAdapter.setChoiceMode(false);
            setupActionBar();
            mActivity.setMenuVisible(true);
            mtoken = TOKEN_SEARCH_NOTE;
            //mActivity.invalidateOptionsMenu();
        }
    }

    private void setDeteleButtonText(int deleteCount) {
        if (deleteCount == 0) {
            mDeleteButton.setText(getString(R.string.button_del));
        } else {
            mDeleteButton.setText(getString(R.string.button_del) + "(" + deleteCount + ")");
        }
    }


    public void setupMulSelectActionBar() {
        ActionBar actionBar = getActivity().getActionBar();
        View v = LayoutInflater.from(getActivity()).inflate(
                R.layout.notes_actionmode_title, null);
        actionBar.setCustomView(v,
                new ActionBar.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT,
                        ActionBar.LayoutParams.WRAP_CONTENT,
                        Gravity.CENTER));
        mTitleText = (TextView) v.findViewById(R.id.am_title);
        mSelectAll = (Button) v.findViewById(R.id.am_selectall);
        mTitleText.setText(R.string.app_name);
        mSelectAll.setVisibility(View.VISIBLE);
        mSelectAll.setText(R.string.button_selectall);
        mSelectAll.setBackground(null);
        //bug 187447 ,mengzhiming.wt modified,20160615,begin
        if(mListAdapter.getCount()==0){
            //mSelectAll.setClickable(false);
            mSelectAll.setEnabled(false);
            mSelectAll.setTextColor(Color.rgb(177, 177, 177));
        }
        //bug 187447 ,mengzhiming.wt modified,20160615,end
        mSelectAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub

                int selectedCount = mListAdapter.getSelectedCount();
                int notesCount = mListAdapter.getTotalCount();
                HashSet<Long> selectedNotes = mListAdapter.getSelectedNoteIds();

                if ((selectedCount > 0) && (mListView.getCount() == selectedNotes.size())) {
                    setDeselectAll();
                } else {
                    setSelectAll();
                }

            }
        });
    }



    private void setupActionBar() {
        ActionBar actionBar = getActivity().getActionBar();
        View v = LayoutInflater.from(getActivity()).inflate(
                R.layout.notes_actionmode_title, null);
        actionBar.setCustomView(v,
                new ActionBar.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT,
                        ActionBar.LayoutParams.WRAP_CONTENT,
                        Gravity.CENTER));
        mTitleText = (TextView) v.findViewById(R.id.am_title);
        mSelectAll = (Button) v.findViewById(R.id.am_selectall);
        mTitleText.setText(R.string.app_name);
        mSelectAll.setVisibility(View.GONE);
        }

    private void setSelectAll() {
            markCheckedState(true);
    }

    private void setDeselectAll() {
        if (mListAdapter.getSelectedCount() > 0) {
            markCheckedState(false);
        }
    }


    private void markCheckedState(boolean checkedState) {
        int count = mListView.getChildCount();
        NotesSRListItem item = null;
        Cursor cursor = mListAdapter.getCursor();

        try {
            if (cursor == null) {
                Log.d("meng", "newcursor is null");
                return;
            }
            int position = cursor.getPosition();
            int locked = 0;

            cursor.moveToPosition(position);
            for (int i = 0; i < count; i++) {
                item = (NotesSRListItem) mListView.getChildAt(i);
                if (item == null && item.getCheckBox().isChecked()) {
                    Log.d("meng", "markCheckedState already marked");
                    continue;
                }

                item.setSelectedBackGroud(checkedState);
                Log.d("meng", "markCheckedState checkedState="+checkedState +" i="+i);
                mListAdapter.setCheckedItem(i,checkedState);

            }


            if (mListAdapter.getSelectedCount() == mListAdapter.getCount()) {
                mSelectAll.setText(R.string.button_unselectall);
                Log.d("meng", "mListAdapter mSelectAll");
            } else {
                mSelectAll.setText(R.string.button_selectall);
                Log.d("meng", "mListAdapter mSelectAll 001");
            }
            refreshButton();
        } catch (Exception e) {
            // TODO: handle exception
        } finally {

        }

    }

    public boolean ExitMultiMode(){
       boolean flag = false;
       if(mIsMultiChoiceMode){
           exitMultiSelectView();
           flag = true;
       }
       return flag;
    }

    public void unCheckAll() {
        NotesSRListItem item = null;
        int count = mListView.getCount();

        Log.e("xxx", "unCheckAll count="+count);

        for (int i = 0; i < count; i++) {
            mListView.setItemChecked(i, false);
            mListAdapter.setCheckedItem(i,false);
            item = (NotesSRListItem) mListView.getChildAt(i);
            item.setSelectedBackGroud(false);
        }

        mListAdapter.notifyDataSetChanged();
        refreshButton();
    }


    private void refreshButton(){

        int CheckedNumber = mListAdapter.getSelectedCount();
        if (CheckedNumber > 0) {
            mDeleteButton.setClickable(true);
            mDeleteButton.setTextColor(Color.rgb(209,78,70));//bug 185672,mengzhiming.wt modified,20160606
        } else {
            mDeleteButton.setClickable(false);
            mDeleteButton.setTextColor(Color.rgb(177, 177, 177));//bug 185672,mengzhiming.wt modified,20160606
        }
        setDeteleButtonText(CheckedNumber);

        if (mListAdapter.getSelectedCount() == mListAdapter.getCount()) {
            mSelectAll.setText(R.string.button_unselectall);
        } else {
            mSelectAll.setText(R.string.button_selectall);
        }
    }

    //bug 187447,mengzhiming.wt add,20160615,start

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        //Log.e("meng", "onCreateLoader");

        final String where = NoteColumns.ID + ">0";
        return new CursorLoader(mActivity, NoteConstant.CONTENT_NOTE_URI,
                null, where, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> arg0, Cursor cursor) {
        //Log.e("meng", "onLoadFinished s");
        Menu mMenu = mActivity.getListMenu();
        if (mMenu == null) {
            Log.w("meng", "onLoadFinished mMenu is null");
            return;
        }

        MenuItem mDelMenuItem = mMenu.findItem(R.id.menu_del_note);
        if (mDelMenuItem != null) {
            //Log.e("meng", "onLoadFinished s2 cursor.getCount()="+cursor.getCount());
            if ((cursor == null) || (cursor.getCount() == 0)) {
                mDelMenuItem.setEnabled(false);
                mDelMenuItem.setIcon(R.drawable.menu_note_del_p);
            } else {
                mDelMenuItem.setEnabled(true);
                mDelMenuItem.setIcon(R.drawable.del_notes_selector);
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> arg0) {
        // Do nothing.
        return;
    }

    public  void getNoteLoaderManager() {
        getLoaderManager().initLoader(0, null, this);
    }

    //bug 187447,mengzhiming.wt add,20160615,end

    //bug 198611, mengzhiming.wt add,20160715,start
    @Override
    public void onPause() {
        super.onPause();
        msearch_input.setFocusable(false);
        msearch_input.setFocusableInTouchMode(false);
    }
    //bug198611,mengzhiming.wt add,20160715,end
}
