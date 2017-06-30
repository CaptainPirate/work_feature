/*===================================================================================================*
 *  when  |      who     |    keyword           |        why         |         what                  *
 *===================================================================================================*
 *20160407|mengzhiming.wt|porting A1s enjoynotes| Port_EnjoyNotes | porting A1s enjoynotes to CMCC N2*
*====================================================================================================*
*20160531|mengzhiming.wt|   customer req       | customer req    | customer req                      *
 *===================================================================================================*/


package com.wingtech.note.list;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.appwidget.AppWidgetManager;
import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.provider.Settings;

import com.wingtech.note.ResourceParser;
import com.wingtech.note.SettingsActivity;
import com.wingtech.note.Utils;
import com.wingtech.note.data.DataUtils;
import com.wingtech.note.data.NoteConstant;
import com.wingtech.note.editor.NoteEditActivity;
import com.wingtech.note.widget.NoteWidgetProvider;
import com.wingtech.note.R;

import java.util.HashSet;
import java.util.Iterator;

import android.widget.LinearLayout;
import android.view.Gravity;
//bug197153,mengzhiming.wt,add,20160712,start
import android.content.res.Resources;
import android.content.res.Configuration;
//bug197153,mengzhiming.wt,add,20160712,end


public class NotesListActivity extends Activity implements
        NotesBaseAdapter.OnContentChangedListener, OnClickListener {
    private static final int FOLDER_LIST_QUERY_TOKEN = 1;
    private static final int NOTE_LIST_QUERY_TOKEN = 0;
    private static final String NORMAL_SELECTION = "parent_id=?";
    private static final int REQUEST_CODE_NEW_NODE = 101;
    private static final int REQUEST_CODE_OPEN_NODE = 102;
    private static final String ROOT_FOLDER_SELECTION = "(type<>2 AND parent_id=?) OR (_id=-2 AND notes_count>0)";
    private static final String TAG = "NotesListActivity";
    private Dialog mActiveDialog;
    private BackgroundQueryHandler mBackgroundQueryHandler;
    private ContentResolver mContentResolver;
    private long mCurrentFolderId;
    private NotesBaseFragment mFragment;
    private ModeCallback mModeCallBack;
    private int mNewNoteColorId;
    private NotesBaseAdapter mNotesListAdapter;
    private ListEditState mState;
    private TitleBarView mTitleBarView;
    private Button mNewNoteButton;
    LinearLayout mBottomPanel;
    private Menu mMenu;
    private TextView mTitleText;
    private Button mSelectAll;
    private MenuItem mDelMenuItem = null; //bug 187447,mengzhiming.wt add,20160615
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /* REQ_155091,zhanghu.wt,add,20160415,start */
        resetFlagIfSupportMagazineLockScreen();
        /* REQ_155091,zhanghu.wt,add,20160415,end */

        setContentView(R.layout.notes_list);
        initResources();
    }

    protected void onRestart() {
        super.onRestart();
    }

    protected void onStart() {
        super.onStart();
        startAsyncNotesListQuery(null);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if ((resultCode == Activity.RESULT_OK)
                && ((requestCode == REQUEST_CODE_NEW_NODE) || (requestCode == REQUEST_CODE_OPEN_NODE)))
            mNotesListAdapter.changeCursor(null);
        else
            super.onActivityResult(requestCode, resultCode, data);
    }

    protected void onStop() {
        if (mActiveDialog != null) {
            if (mActiveDialog.isShowing())
                mActiveDialog.dismiss();
            mActiveDialog = null;
        }
        super.onStop();
    }

    private void initResources() {
        mContentResolver = getContentResolver();
        mBackgroundQueryHandler = new BackgroundQueryHandler(
                getContentResolver());
        mCurrentFolderId = 0L;
        mNewNoteColorId = ResourceParser.getDefaultBgId(this);
        /*
         * ActionBar actionBar = getActionBar();
         * actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
         * actionBar.setCustomView(R.layout.notes_title_bar); mTitleBarView =
         * ((TitleBarView) actionBar.getCustomView());
         *
         * LayoutInflater inflater = LayoutInflater.from(this); View action_bar
         * = inflater.inflate(R.layout.notes_title_bar, null);
         * actionBar.setCustomView(action_bar);
         */
        mTitleBarView = ((TitleBarView) findViewById(R.id.notes_title_bar));
        mTitleBarView.setBackButtonListener(this);
        //mNewNoteButton = (Button) findViewById(R.id.new_note_button);
        //mNewNoteButton.setOnClickListener(this);

        mState = ListEditState.NOTE_LIST;
        mModeCallBack = new ModeCallback();
        addFragment();
        IntentFilter f = new IntentFilter();
        f.addAction("android.intent.action.NAVBAR_SHOW");
        f.addAction("android.password.action.visitor.mode");
        registerReceiver(new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                // TODO Auto-generated method stub
                if (mFragment instanceof NotesGridFragment) {
                    startAsyncNotesListQuery(null);
                    ((NotesGridFragment) mFragment).refresh();
                }

            }
        }, new IntentFilter(f));
    }

   /* @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        Log.e("meng","-----onPrepareOptionsMenu");
        if (mFragment instanceof NotesGridFragment)
            Log.e("meng","-----onPrepareOptionsMenu 01");{
            if (((NotesGridFragment) mFragment).getIsMultiChoiceMode()) {
                Log.e("meng","-----onPrepareOptionsMenu 02");
                menu.clear();
                return true;
            }
        }
        Log.e("meng","-----onPrepareOptionsMenu 03");
        return true;
    }*/


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.notes_list_menu, menu);
        //bug 187447,mengzhiming.wt add,20160615,start
        mDelMenuItem = (MenuItem)menu.findItem(R.id.menu_del_note);
        mMenu = menu;
        ((NotesGridFragment) mFragment).getNoteLoaderManager();
        //bug 187447,mengzhiming.wt add,20160615
        return true;
    }

    public void updateDeleteMenu(Cursor cursor){
        if(mDelMenuItem == null){
           return;
        }
        if ((cursor == null) || (cursor.getCount() == 0)){
            mDelMenuItem.setEnabled(false);
            mDelMenuItem.setIcon(R.drawable.menu_note_del_p);
        } else {
            mDelMenuItem.setEnabled(true);
            mDelMenuItem.setIcon(R.drawable.del_notes_selector);
        }
    }

    public boolean onOptionsItemSelected(MenuItem menu) {
        /*porting A1s enjoynotes, mengzhiming.wt,modified 20160405
	    boolean b = Settings.Global.getInt(getContentResolver(),
                Settings.Global.VISITOR_MODE_ON, 0) == 1 ? true : false;
        if (b) {
            Toast.makeText(NotesListActivity.this, R.string.private_mode_hint,
                    Toast.LENGTH_LONG).show();
            return true;
        }*/
        switch (menu.getItemId()) {
        case R.id.menu_new_folder:
            mActiveDialog = new NewFolderDialog(this);
            mActiveDialog.show();
            break;
        case R.id.menu_settings:
            startSettingsActivity();
            break;
        case R.id.menu_new_note:
            createNewNote();
            break;
        case R.id.menu_rename_folder:
            mActiveDialog = new RenameFolderDialog(this);
            mActiveDialog.show();
            break;
        case R.id.menu_del_note:
            Log.e("meng","menu_del_note");
            if (mFragment instanceof NotesGridFragment) {
                ((NotesGridFragment) mFragment).onQueryDelList();
            }
            break;
        default:
            return true;
        }
        return true;
    }

    public void setMenuVisible(boolean isVisible){
        mMenu.setGroupVisible(R.id.mgroup, isVisible);
    }

    //bug 187447,mengzhiming.wt add,20160615,start
    public Menu getListMenu(){
        return mMenu;
    }
    //bug 187447,mengzhiming.wt add,20160615,end

    private void startSettingsActivity() {
        // TODO Auto-generated method stub
        Intent intent = new Intent();
        intent.setClass(NotesListActivity.this, SettingsActivity.class);
        startActivity(intent);
    }

    private class NewFolderDialog extends FolderNameDialog {
        private long nFolderId;
        private String nFolderName;

        protected NewFolderDialog(Context context) {
            super(context);
        }

        protected void initialize() {
            super.initialize();
            nFolderEditor.setText(getString(R.string.hint_default_folder_name));
            nFolderEditor.setSelection(nFolderEditor.getText().length());
            setTitle(getString(R.string.menu_create_folder));
        }

        protected void persist() {
            String name = nFolderEditor.getText().toString();
            nFolderName = name;
            if (DataUtils.checkValidFolderName(mContentResolver, name, 0L)) {
                String[] strArray = { name };
                Toast.makeText(NotesListActivity.this,
                        getString(R.string.hint_folder_exist, strArray), 1)
                        .show();
                nFolderEditor.setSelection(0, this.nFolderEditor.length());
            } else {
                nFolderId = DataUtils.insertNoteFolder(NotesListActivity.this,
                        name);
                if ((nFolderId <= 0L)
                        && (DataUtils.restoreNoteFolder(NotesListActivity.this,
                                name)))
                    nFolderId = DataUtils.getVisibleFolderIdByName(
                            NotesListActivity.this, name);
                dismiss();
            }
        }
    }

    private class RenameFolderDialog extends NotesListActivity.FolderNameDialog {
        protected RenameFolderDialog(Context context) {
            super(context);
        }

        protected void initialize() {
            super.initialize();
            nFolderEditor.setText(mTitleBarView.getTitleText());
            nFolderEditor.setSelection(nFolderEditor.getText().length());
            setTitle(getString(R.string.menu_folder_change_name));
        }

        protected void persist() {
            String str = nFolderEditor.getText().toString();
            if (str.equals(mTitleBarView.getTitleText()))
                dismiss();
            else {
                if (DataUtils.checkValidFolderName(mContentResolver, str,
                        mCurrentFolderId)) {
                    String[] arrayOfString = { str };
                    Toast.makeText(
                            NotesListActivity.this,
                            getString(R.string.hint_folder_exist, arrayOfString),
                            1).show();
                    nFolderEditor.setSelection(0, nFolderEditor.length());
                } else {
                    if (DataUtils.renameNoteFolder(NotesListActivity.this,
                            mCurrentFolderId, str))
                        mTitleBarView.setTitleText(str);
                    dismiss();
                }
            }
        }
    }

    private abstract class FolderNameDialog extends AlertDialog {
        protected EditText nFolderEditor;
        protected Button nPositiveButton;

        protected FolderNameDialog(Context context) {
            super(context);
            initialize();
        }

        public void dismiss() {
            Utils.hideSoftInput(nFolderEditor);
            super.dismiss();
        }

        protected void initialize() {
            View localView = LayoutInflater.from(NotesListActivity.this)
                    .inflate(R.layout.dialog_edit_text, null);
            nFolderEditor = ((EditText) localView
                    .findViewById(R.id.et_foler_name));
            nFolderEditor.addTextChangedListener(new TextWatcher() {

                public void onTextChanged(CharSequence s, int start, int count,
                        int after) {
                    if (nPositiveButton != null) {
                        if (!TextUtils.isEmpty(nFolderEditor.getText()))
                            nPositiveButton.setEnabled(true);
                        else
                            nPositiveButton.setEnabled(false);

                    }
                }

                public void beforeTextChanged(CharSequence s, int start,
                        int count, int after) {
                    // TODO Auto-generated method stub

                }

                public void afterTextChanged(Editable s) {
                    // TODO Auto-generated method stub

                }
            });
            setView(localView);
            setButton(BUTTON_POSITIVE,
                    NotesListActivity.this.getString(android.R.string.ok),
                    (DialogInterface.OnClickListener) null);
            setButton(BUTTON_NEGATIVE,
                    NotesListActivity.this.getString(android.R.string.cancel),
                    (DialogInterface.OnClickListener) null);
        }

        protected abstract void persist();

        public void show() {
            super.show();
            nPositiveButton = getButton(BUTTON_POSITIVE);
            nPositiveButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View paramAnonymousView) {
                    persist();
                }
            });
            nFolderEditor.requestFocus();
            nFolderEditor.post(new Runnable() {
                public void run() {
                    Utils.showSoftInput(nFolderEditor);
                }
            });
        }
    }

    private void startAsyncNotesListQuery(Object cookie) {
        String selection;
        if (mCurrentFolderId == 0L)
            selection = ROOT_FOLDER_SELECTION;
        else
            selection = NORMAL_SELECTION;
        Uri uri = NoteConstant.CONTENT_NOTE_URI;
        String[] projection = NoteItemData.PROJECTION;
        String[] selectionArgs = { String.valueOf(mCurrentFolderId) };
        mBackgroundQueryHandler.startQuery(NOTE_LIST_QUERY_TOKEN, cookie, uri,
                projection, selection, selectionArgs,
                "type DESC, stick_date DESC,modified_date DESC");

    }

    private final class BackgroundQueryHandler extends AsyncQueryHandler {
        public BackgroundQueryHandler(ContentResolver contentResolver) {
            super(contentResolver);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            super.onQueryComplete(token, cookie, cursor);
            /*porting A1s enjoynotes, mengzhiming.wt,modified 20160405
			boolean b = Settings.Global.getInt(getContentResolver(),
                    Settings.Global.VISITOR_MODE_ON, 0) == 1 ? true : false;
            if (b) {
                Log.d(TAG, "onQueryComplete:private mode on");
                mNotesListAdapter.changeCursor(null);
                Log.d(TAG,
                        "onQueryComplete:cursor="
                                + mNotesListAdapter.getCursor());
                notifyDataSetChanged(mNotesListAdapter.getCursor());
                return;
            }*/
            if (token == NOTE_LIST_QUERY_TOKEN) {// 显示文件夹及便签
                if ((cookie != null)
                        && ((cookie instanceof NotesListActivity.ChangeFolderAnimator))) {
                    ((ChangeFolderAnimator) cookie).onQueryComplete(cursor);
                } else {
                    mNotesListAdapter.changeCursor(cursor);
                    notifyDataSetChanged(mNotesListAdapter.getCursor());
                }
            } else if (token == FOLDER_LIST_QUERY_TOKEN) {// 显示文件夹列表
                FolderNameCursor fCurosr = new FolderNameCursor(
                        NotesListActivity.this, cursor);
                showFolderListMenu(fCurosr);
            }
        }
    }

    public void notifyDataSetChanged(Cursor cursor) {
        mFragment.onDataSetChanged(cursor);
    }

    public void showFolderListMenu(final FolderNameCursor fCursor) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.menu_select_folder_title);
        builder.setSingleChoiceItems(fCursor, -1,
                NoteConstant.NoteColumns.SNIPPET,
                new DialogInterface.OnClickListener() {
                    private void moveNotesToFolder(HashSet<Long> ids,
                            long folderId, String folderName) {
                        DataUtils.batchMoveToFolder(mContentResolver, ids,
                                folderId);
                        Object[] args = new Object[2];
                        args[0] = Integer.valueOf(mNotesListAdapter
                                .getSelectedCount());
                        args[1] = folderName;
                        Toast.makeText(
                                NotesListActivity.this,
                                getString(
                                        R.string.hint_move_notes_to_folder_format,
                                        args), 0).show();
                        mModeCallBack.finishActionMode();
                        if ((mNotesListAdapter.isAllSelected())
                                && (mState != ListEditState.NOTE_LIST))
                            onBackPressed();
                    }

                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        final HashSet<Long> selectedNotes = mNotesListAdapter
                                .getSelectedNoteIds();
                        if (which == 0) {
                            mActiveDialog = new NewFolderDialog(
                                    NotesListActivity.this);
                            mActiveDialog
                                    .setOnDismissListener(new DialogInterface.OnDismissListener() {
                                        public void onDismiss(
                                                DialogInterface dialog2) {
                                            NewFolderDialog curDialog = (NewFolderDialog) dialog2;
                                            if (curDialog.nFolderId > 0) {
                                                moveNotesToFolder(
                                                        selectedNotes,
                                                        curDialog.nFolderId,
                                                        curDialog.nFolderName);
                                            } else {
                                                showFolderListMenu(fCursor);
                                            }
                                        }
                                    });
                            mActiveDialog.show();
                        } else {
                            moveNotesToFolder(selectedNotes,
                                    fCursor.getFolderId(which),
                                    fCursor.getFolderName(which));
                        }

                    }
                });
        mActiveDialog = builder.show();
    }

    private abstract class ChangeFolderAnimator {
        protected Cursor nCursor;
        protected final long nInDuration;
        protected final long nOutDuration;
        protected long nSubFolderId;

        public ChangeFolderAnimator(long id) {
            nSubFolderId = id;
            nInDuration = getResources().getInteger(
                    R.integer.animation_duration_medium);
            nOutDuration = getResources().getInteger(
                    R.integer.animation_duration_short);
        }

        public abstract void fadeIn();

        public abstract void fadeOut();

        public void onQueryComplete(Cursor paramCursor) {
            nCursor = paramCursor;
            fadeIn();
        }
    }

    private class ReturnRootFolderAnimator extends
            NotesListActivity.ChangeFolderAnimator {
        public ReturnRootFolderAnimator(long id) {
            super(id);
        }

        public void fadeIn() {
            mNotesListAdapter.setOnContentChangedListener(null);
            mFragment.exitSubFolderMode();
            mNotesListAdapter = mFragment.getAdapter();

            mNotesListAdapter
                    .setOnContentChangedListener(NotesListActivity.this);
            mNotesListAdapter.changeCursor(nCursor);
            mFragment.restoreRootPosition();
            notifyDataSetChanged(mNotesListAdapter.getCursor());
            invalidateOptionsMenu();
        }

        public void fadeOut() {
            mTitleBarView
                    .setAppearenceMode(TitleBarView.APPEARENCE_MODE_GRID_ROOT);
            invalidateOptionsMenu();
            startAsyncNotesListQuery(this);
        }
    }

    private class OpenSubFolderAnimator extends ChangeFolderAnimator {
        public OpenSubFolderAnimator(long id) {
            super(id);
        }

        public void fadeIn() {
            mNotesListAdapter.setOnContentChangedListener(null);
            mFragment.enterSubFolderMode(mCurrentFolderId);
            mNotesListAdapter = mFragment.getAdapter();
            mNotesListAdapter
                    .setOnContentChangedListener(NotesListActivity.this);
            mNotesListAdapter.changeCursor(nCursor);
            notifyDataSetChanged(mNotesListAdapter.getCursor());
            invalidateOptionsMenu();
        }

        public void fadeOut() {
            mTitleBarView
                    .setAppearenceMode(TitleBarView.APPEARENCE_MODE_GRID_SUB);
            invalidateOptionsMenu();
            startAsyncNotesListQuery(this);
        }
    }

    public void onContentChanged(NotesBaseAdapter adapter) {
        notifyDataSetChanged(adapter.getCursor());
    }

    private void createNewNote() {
        Intent localIntent = new Intent(this, NoteEditActivity.class);
        localIntent.setAction(Intent.ACTION_INSERT_OR_EDIT);
        localIntent.putExtra(NoteConstant.INTENT_EXTRA_FOLDER_ID,
                mCurrentFolderId);
        mNewNoteColorId = ResourceParser.getDefaultBgId(this);
        localIntent.putExtra(NoteConstant.INTENT_EXTRA_BACKGROUND_ID,
                mNewNoteColorId);
        startActivityForResult(localIntent, REQUEST_CODE_NEW_NODE);
    }

    private NotesBaseFragment createFragment() {
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager
                .beginTransaction();
        NotesBaseFragment fragment = new NotesGridFragment();
        fragmentTransaction.add(R.id.fragment_container, fragment);
        fragmentTransaction.commit();
        fragmentManager.executePendingTransactions();
        return fragment;
    }

    private void addFragment() {
        NotesBaseFragment fragment = createFragment();
        Cursor localCursor;

        if (mFragment != fragment) {
            mFragment = fragment;
            if (mNotesListAdapter == null) {
                localCursor = null;
            } else
                localCursor = mNotesListAdapter.swapCursor(null);
            mNotesListAdapter = mFragment.getAdapter();
            mNotesListAdapter.setOnContentChangedListener(this);
            mFragment.setup(new OnListItemClickListener(), mModeCallBack);
            if (localCursor == null)
                startAsyncNotesListQuery(null);
            else
                mNotesListAdapter.changeCursor(localCursor);
        }
        mTitleBarView.setAppearenceMode(TitleBarView.APPEARENCE_MODE_GRID_ROOT);
    }

    private class OnListItemClickListener implements
            AdapterView.OnItemClickListener {

        public void onItemClick(AdapterView<?> parent, View view, int position,
                long id) {
            if (view instanceof INotesListItem) {
                NoteItemData data = ((INotesListItem) view).getItemData();
                if (!mNotesListAdapter.isInChoiceMode()) {
                    if (data.getType() == NoteConstant.TYPE_NOTE)
                        openNote(data.getId());
                    else if (data.getType() == NoteConstant.TYPE_FOLDER
                            || data.getType() == NoteConstant.TYPE_SYSTEM)
                        openFolder(data);
                } else {
                    int pos = mFragment.translatePosition(position);
                    if (mNotesListAdapter.isSelectedItem(pos))
                        mModeCallBack.onItemCheckedStateChanged(null, pos, id,
                                false);
                    else
                        mModeCallBack.onItemCheckedStateChanged(null, pos, id,
                                true);
                }
            }
        }
    }

    protected void openNote(long noteId) {
        openNote(noteId, null);
    }

    // 这个函数用于搜索返回的列表打开便签的借口
    protected void openNote(long noteId, String queryString) {
        Intent localIntent = new Intent(this, NoteEditActivity.class);
        localIntent.setAction(Intent.ACTION_VIEW);
        localIntent.putExtra(Intent.EXTRA_UID, noteId);
        if (!TextUtils.isEmpty(queryString)) {
            localIntent.putExtra(Intent.EXTRA_TEXT, queryString);
        }
        startActivityForResult(localIntent, REQUEST_CODE_OPEN_NODE);
    }

    private void openFolder(NoteItemData nodeItemData) {
        mCurrentFolderId = nodeItemData.getId();
        if (mCurrentFolderId == -2L) {
            mState = ListEditState.CALL_RECORD_FOLDER;
            mTitleBarView.setTitleText(R.string.call_record_folder_name);
        } else {
            mState = ListEditState.SUB_FOLDER;
            mTitleBarView.setTitleText(nodeItemData.getSnippet());
        }
        new OpenSubFolderAnimator(this.mCurrentFolderId).fadeOut();

    }

    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.title_back:
            returnRootFolder();
            break;
        /*case R.id.new_note_button:
            createNewNote();
            break;*/
        case R.id.title_text:
            mActiveDialog = new RenameFolderDialog(this);
            mActiveDialog.show();
            break;
        default:
            break;
        }
    }

    public void onBackPressed() {
        /*switch (mState.ordinal()) {
        case 1:
        case 2:
            returnRootFolder();
            break;
        default:
            super.onBackPressed();
            break;*/

        if(!((NotesGridFragment) mFragment).ExitMultiMode())
              super.onBackPressed();
    }

    private void returnRootFolder() {
        ReturnRootFolderAnimator localReturnRootFolderAnimator = new ReturnRootFolderAnimator(
                mCurrentFolderId);
        mCurrentFolderId = NoteConstant.ID_ROOT_FOLDER;
        mState = ListEditState.NOTE_LIST;
        localReturnRootFolderAnimator.fadeOut();
    }

    private static enum ListEditState {
        NOTE_LIST, SUB_FOLDER, CALL_RECORD_FOLDER
    }

    private class ModeCallback implements AbsListView.MultiChoiceModeListener,
            OnClickListener {
        private MenuItem mDeleteMenu;
        private MenuItem mMoveMenu;
        private MenuItem mStickMenu;
        private TextView mTitleText;
        private Button mSelectAll;
        private ActionMode mActionMode;

        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // TODO Auto-generated method stub
            getMenuInflater().inflate(R.menu.notes_list_options, menu);
            mActionMode = mode;
            mMoveMenu = menu.findItem(R.id.menu_move);
            if (mState == ListEditState.CALL_RECORD_FOLDER)
                mMoveMenu.setVisible(false);
            else
                mMoveMenu.setVisible(true);
            mDeleteMenu = menu.findItem(R.id.menu_delete);
            mStickMenu = menu.findItem(R.id.menu_stick);
            View v = LayoutInflater.from(NotesListActivity.this).inflate(
                    R.layout.notes_actionmode_title, null);
            mTitleText = (TextView) v.findViewById(R.id.am_title);
            mSelectAll = (Button) v.findViewById(R.id.am_selectall);
            mSelectAll.setOnClickListener(this);
            mode.setCustomView(v);
            mNotesListAdapter.setChoiceMode(true);
            mNotesListAdapter.notifyDataSetChanged();
            mNotesListAdapter.onContentChanged();
            mFragment.enterEditMode();
            // getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NEEDS_MENU_KEY);
            // //lgj sdk5.1 not supprot
            return true;
        }

        public void finishActionMode() {
            mActionMode.finish();
        }

        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            updateMenu();
            return true;
        }

        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            boolean ret = false;
            switch (item.getItemId()) {
            case R.id.menu_delete:
                showDeleteDialog();
                break;
            case R.id.menu_move:
                startQueryDesFolders();
                break;
            case R.id.menu_stick:
                stickSelectedItems();
                finishActionMode();
                break;
            default:
                break;
            }
            updateMenu();
            return ret;
        }

        public void onDestroyActionMode(ActionMode mode) {
            mNotesListAdapter.setChoiceMode(false);
            mNotesListAdapter.notifyDataSetChanged();
            mFragment.exitEditMode();
            // getWindow().addFlags(WindowManager.LayoutParams.FLAG_NEEDS_MENU_KEY);//lgj
            // sdk5.1 not supprot
        }

        public void onItemCheckedStateChanged(ActionMode mode, int position,
                long id, boolean checked) {
            // TODO Auto-generated method stub
            mNotesListAdapter.setCheckedItem(position, checked);
            updateMenu();
        }

        private void updateMenu() {
            int selectedCount = mNotesListAdapter.getSelectedCount();
            HashSet<Long> selectedNotes = mNotesListAdapter
                    .getSelectedNoteIds();
            mTitleText.setText(getTitleText(selectedCount));
            if ((selectedCount > 0) && (selectedCount == selectedNotes.size())) {// 全部为便签，没有选中文件
                mMoveMenu.setEnabled(true);

                mDeleteMenu.setEnabled(true);

                mTitleText.setText(getTitleText(selectedCount));
                if (mNotesListAdapter.getSelectedStickyNotesIds().size() == selectedNotes
                        .size()) {// 全部为置顶便签

                    mStickMenu.setTitle(R.string.menu_unstick);
                    mStickMenu.setIcon(R.drawable.ic_menu_unstick);
                    mStickMenu.setEnabled(true);
                } else {
                    mStickMenu.setTitle(R.string.menu_stick);
                    mStickMenu.setIcon(R.drawable.ic_menu_stick);
                    mStickMenu.setEnabled(true);
                }

            } else {// 包含文件夹，不能使用移动和置顶菜单项
                mMoveMenu.setEnabled(false);
                if (selectedCount > 0)
                    mDeleteMenu.setEnabled(true);
                else
                    mDeleteMenu.setEnabled(false);
                mStickMenu.setEnabled(false);

            }
            if (!mNotesListAdapter.isAllSelected()) {
                mSelectAll.setText(R.string.button_selectall);
            } else {
                mSelectAll.setText(R.string.button_unselectall);
            }
        }

        private String getTitleText(int count) {
            if (count == 0) {
                return getResources().getString(
                        R.string.notes_am_title_unselected);
            } else {
                Integer[] args = { count };
                String str = getResources().getQuantityString(
                        R.plurals.notes_am_title_selected, count);
                return String.format(str, args);
            }
        }

        public void onClick(View v) {
            if (v.getId() == R.id.am_selectall) {
                if (mNotesListAdapter.isAllSelected())
                    mNotesListAdapter.selectAll(false);
                else
                    mNotesListAdapter.selectAll(true);
                mNotesListAdapter.onContentChanged();
            }
            updateMenu();
        }
    }

    private void showDeleteDialog() {
        AlertDialog.Builder localBuilder = new AlertDialog.Builder(this);
        localBuilder.setTitle(R.string.alert_title_delete);
        localBuilder.setIconAttribute(android.R.attr.alertDialogIcon);
        localBuilder.setMessage(R.string.alert_message_delete_note);
        localBuilder.setPositiveButton(android.R.string.ok,
                new DialogInterface.OnClickListener() {
                    public void onClick(
                            DialogInterface paramAnonymousDialogInterface,
                            int paramAnonymousInt) {
                        deleteSelectedItems();
                        mActiveDialog.dismiss();
                        mModeCallBack.finishActionMode();
                    }
                });
        localBuilder.setNegativeButton(android.R.string.cancel, null);
        mActiveDialog = localBuilder.show();
    }

    private void stickSelectedItems() {
        HashSet<Long> selectedIds = mNotesListAdapter.getSelectedNoteIds();
        HashSet<Long> selectedStickyIds = mNotesListAdapter
                .getSelectedStickyNotesIds();
        if (selectedIds.size() == selectedStickyIds.size())
            DataUtils.stickNotes(this, selectedStickyIds, false);
        else {
            Iterator<Long> iterator = selectedStickyIds.iterator();
            while (iterator.hasNext())
                selectedIds.remove(iterator.next());
            DataUtils.stickNotes(this, selectedIds, true);
        }
    }

    public void startQueryDesFolders() {
        String selection = "type=? AND parent_id<>? AND _id<>?";
        if (mState != ListEditState.NOTE_LIST)
            selection = "(" + selection + ") OR (" + "_id" + "=" + 0 + ")";

        String[] projection = { String.valueOf(NoteConstant.NoteColumns.ID),
                String.valueOf(NoteConstant.NoteColumns.SNIPPET) };
        String[] selectionArgs = { String.valueOf(NoteConstant.TYPE_FOLDER),
                String.valueOf(NoteConstant.ID_TRASH_FOLER),
                String.valueOf(mCurrentFolderId) };
        mBackgroundQueryHandler.startQuery(FOLDER_LIST_QUERY_TOKEN, null,
                NoteConstant.CONTENT_NOTE_URI, projection, selection,
                selectionArgs, "modified_date DESC");
    }

    private void updateWidget(Context paramContext, int[] widgetIds) {
        Intent localIntent = new Intent(
                "android.appwidget.action.APPWIDGET_UPDATE");
        localIntent.setClass(paramContext, NoteWidgetProvider.class);
        localIntent.putExtra("appWidgetIds", widgetIds);
        paramContext.sendBroadcast(localIntent);
    }

    private void updateAllWidgets() {
        ComponentName localComponentName = new ComponentName(this,
                NoteWidgetProvider.class);
        int[] arrayOfInt = AppWidgetManager.getInstance(this).getAppWidgetIds(
                localComponentName);
        updateWidget(this, arrayOfInt);
    }

    public void deleteSelectedItems() {
        HashSet<Long> delFolderIds = mNotesListAdapter.getSelectedFolderIds();
        HashSet<Long> delNoteIds = mNotesListAdapter.getSelectedNoteIds();
        DataUtils.batchDeleteNotes(getContentResolver(), delFolderIds);
        DataUtils.batchDeleteNotes(getContentResolver(), delNoteIds);
        updateAllWidgets();
    }

    /* REQ_155091,zhanghu.wt,add,20160415,start */
    private static final String LOG_TAG = NotesListActivity.class.getSimpleName();
    private static final String LAUNCH_SOURCE = "magazinelockscreen";
    private static final String EXTRA_LAUNCH_SOURCE = "com.android.systemui.launch_source";
    private static String mMagazineLockScreenLaunchSource = "";

    private void resetFlagIfSupportMagazineLockScreen() {
        Intent mIntent = getIntent();
        if (mIntent != null) {
            mMagazineLockScreenLaunchSource = mIntent.getStringExtra(EXTRA_LAUNCH_SOURCE);
            Log.d(LOG_TAG, "mMagazineLockScreenLaunchSource=" + mMagazineLockScreenLaunchSource);
            if (!TextUtils.isEmpty(mMagazineLockScreenLaunchSource)
                    && mMagazineLockScreenLaunchSource.equals(LAUNCH_SOURCE)) {
                final Window win = getWindow();
                win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        // | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                        | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);
            }
        }
    }
    /* REQ_155091,zhanghu.wt,add,20160415,end */

    //bug197153,mengzhiming.wt,add,20160712,start
    @Override
    public Resources getResources() {
        Resources res = super.getResources();
        Configuration config = new Configuration();
        config.setToDefaults();
        res.updateConfiguration(config,res.getDisplayMetrics());
        return res;
    }
    //bug197153,mengzhiming.wt,add,20160712,end
}
