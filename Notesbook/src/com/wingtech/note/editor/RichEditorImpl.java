/*===================================================================================================*
 *  when  |      who     |    keyword           |        why         |         what                  *
 *===================================================================================================*
 *20160407|mengzhiming.wt|porting A1s enjoynotes| Port_EnjoyNotes | porting A1s enjoynotes to CMCC N2*
 *===================================================================================================*
 *20160531|mengzhiming.wt|   customer req       | customer req    | customer req                     *
 *===================================================================================================*
 *20160531|mengzhiming.wt|   bug183418          | bug183418        | bug183418                       *
 *===================================================================================================*
 *20160708|lilei.wt      |   bug194992          | bug194992        | bug194992                       *
 *===================================================================================================*
 */
package com.wingtech.note.editor;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.text.style.TextAppearanceSpan;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.wingtech.note.AttachmentUtils;
import com.wingtech.note.PreferenceUtils;
import com.wingtech.note.ResourceParser;
import com.wingtech.note.Utils;
import com.wingtech.note.data.Contact;
import com.wingtech.note.data.DataUtils;
import com.wingtech.note.data.NoteConstant;
import com.wingtech.note.data.NotesProvider;
import com.wingtech.note.data.WorkingNote;
import com.wingtech.note.list.NotesListActivity;
import com.wingtech.note.loader.ImageCacheManager;
import com.wingtech.note.sketch.SketchView;
import com.wingtech.note.spannableparser.SpanUtils;
import com.wingtech.note.widget.NoteWidgetProvider;
import com.wingtech.note.R;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import android.widget.Button;
import java.util.Locale;
import android.widget.ImageButton;

public class RichEditorImpl implements View.OnClickListener, WorkingNote.NoteSettingChangedListener {

    private static final String TAG = "NoteEditImpl";
    public static WorkingNote mWorkingNote;
    private Activity mActivity;
    private NoteContentObserver mObserver;
    private ViewGroup mHeader;
    private TextView mHeaderAlert;
    private ImageView mHeaderAttachment;
    private ImageView mHeaderColor;
    private TextView mHeaderDate;
    private TextView mHeaderTime;
    private ImageView mHeaderUp;
    private View mPanel;
    private NotePopupWindow mPopupWindow;
    private String mQueryString;
    private Dialog mActiveDialog;
    private RichEditor mEditor;
    private SketchView mSketcher;

    private ImageButton  mAttachmentCamera;
    private ImageButton  mAttachmentGalerry;
    private ImageButton  mAttachmentAlert;
    private ImageButton  mAttachmentColor;
    private ImageButton  mAttachmentOnOff;
    private boolean openAttachment = false;

    private View mAlertFill;
    private View mAlertState;
    private TextView mAlertTxt;
    private ImageButton mAlertDel;//bug198669,mengzhiming.wt,add 20160729
    private View mNoAlertmargeUp;
    //bug187783,mengzhiming.wt,modified 20160615,start
    private long alertDate;
    private boolean alertSet;
    private boolean alertChange=false;
    //bug187783,mengzhiming.wt,modified 20160615,end
    private boolean saveEnable=false;//bug188011,mengzhiming.wt,add 20160621

    private Button mEditCancel;
    private Button mEditSave;
    private View mAttachment_all;//bug199000,mengzhiming.wt,modified,20160720
    private static final int REQUEST_CODE_GALLERY = 1;
    private static final int REQUEST_CODE_CAMERA = 2;
    private static final int REQUEST_CODE_CONTACT = 4;
    private static final SparseIntArray sBgSelectorBtnsMap = new SparseIntArray();
    private static final SparseIntArray sBgSelectorSelectionMap = new SparseIntArray();
    static {
        sBgSelectorBtnsMap.append(R.id.color_picker_0, 0);
        sBgSelectorBtnsMap.append(R.id.color_picker_1, 1);
        sBgSelectorBtnsMap.append(R.id.color_picker_2, 2);
        sBgSelectorBtnsMap.append(R.id.color_picker_3, 3);
        sBgSelectorBtnsMap.append(R.id.color_picker_4, 4);
        sBgSelectorBtnsMap.append(R.id.color_picker_5, 5);
        sBgSelectorSelectionMap.append(0, R.id.iv_bg_picker0_select);
        sBgSelectorSelectionMap.append(1, R.id.iv_bg_picker1_select);
        sBgSelectorSelectionMap.append(2, R.id.iv_bg_picker2_select);
        sBgSelectorSelectionMap.append(3, R.id.iv_bg_picker3_select);
        sBgSelectorSelectionMap.append(4, R.id.iv_bg_picker4_select);
        sBgSelectorSelectionMap.append(5, R.id.iv_bg_picker5_select);
    }
    private View.OnClickListener mEditorClickListener = new View.OnClickListener() {
        public void onClick(View paramAnonymousView) {
            confirmEditAnyway(new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface paramAnonymous2DialogInterface,
                        int paramAnonymous2Int) {
                    // setReadonly(false);
                    Utils.showSoftInput(RichEditorImpl.this.mEditor.getFocusedChild());
                    mEditor.setCursorVisible(true);
                }
            });
        }
    };
    private boolean mBackPressed = false;
//    private View mCover;

    public boolean onCreate(Intent paramIntent, Bundle paramBundle) {
        boolean bool = false;

        if ((handleIntent(paramIntent)) || (handleInstanceState(paramBundle)))
            bool = true;
        if (bool) {
            ImageCacheManager.getInstance(this.mActivity).start();// 防止从桌面widget直接进入时
            initializeViews();
            updateEditor();
            alertChange=false;//bug187783,mengzhiming.wt,modified 20160615
            saveEnable = false;//bug188011,mengzhiming.wt,add 20160621
        }

        return bool;
    }

    public boolean onPrepareOptionsMenu(Menu paramMenu) {
        if (mActivity.isFinishing())
            return false;
        else {
            dismissPopupWindow();
            paramMenu.clear();
            if (mWorkingNote.getFolderId() == NoteConstant.ID_CALL_RECORD_FOLDER)
                mActivity.getMenuInflater().inflate(R.menu.call_note_edit, paramMenu);
            else
                mActivity.getMenuInflater().inflate(R.menu.note_edit, paramMenu);

            return true;
        }
    }

    public boolean onOptionsItemSelected(MenuItem paramMenuItem) {
        boolean bool;
        switch (paramMenuItem.getItemId()) {
            case R.id.menu_new_note:
                createNewNote();
                bool = true;
                break;
            case R.id.menu_delete:
                showDeleteDialog();
                bool = true;
                break;
            case R.id.menu_discard:
                mWorkingNote.setDiscarded(true);
                if (mWorkingNote.isWorthSaving()) {
                    showDiscardDialog();
                } else {
                    mActivity.finish();
                }
                bool = true;
                break;
            case R.id.menu_font_size:
                showFontSizeDialog();
                bool = true;
                break;
            case R.id.menu_send:
                sendTo();
                bool = true;
                break;
            case R.id.menu_send_to_desktop:
                sendToDesktop();
                bool = true;
                break;
            default:
                bool = false;
        }
        return bool;
    }

    private void sendAsPicture() {
        // TODO Auto-generated method stub
        // updateWorkingText();
        //saveNote();//bug185832,mengzhiming.wt,modified 20160608
        Bitmap bmp = getPageBmp();

        String path = AttachmentUtils.saveSketchBmp(mActivity, bmp, mWorkingNote.getNoteId());
        Intent localIntent = new Intent(Intent.ACTION_SEND);
        if (path != null)
            localIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(path)));
        else
            localIntent
                    .putExtra(
                            Intent.EXTRA_STREAM,
                            NotesProvider.getSketchFileUri(Long.toString(mWorkingNote.getNoteId())
                                    + ".jpg"));
        localIntent.setType("image/jpeg");
        mActivity.startActivity(localIntent);
    }

    private void savePageSketch() {
        Bitmap bmp = getPageBmp();
        mEditor.insertSketchImage(bmp);
    }

    private HashMap<String, AttachmentUtils.ImageInfo>
            getImageInfosFromSnippet(long noteId, CharSequence content) {
        ArrayList<String> localArrayList = SpanUtils.retrieveImages(mActivity,
                content);
        return AttachmentUtils.getImageInfosByName(mActivity, noteId,
                localArrayList);
    }

    private void sendTo() {
        updateWorkingText();
        String content = mWorkingNote.getContent();
        String outText = SpanUtils.normalizeSnippet(content, null);
        HashMap<String, AttachmentUtils.ImageInfo> localHashMap = getImageInfosFromSnippet(
                mWorkingNote.getNoteId(), mEditor.getRichText());
        String subject = Utils.getFormattedSnippet(outText).toString().trim();
        if (subject.length() > 30)// 截取主题
            subject = subject.substring(0, 30);
        ArrayList<Uri> localArrayList = new ArrayList<Uri>();
        if ((localHashMap != null) && (localHashMap.size() > 0)) {
            Iterator<AttachmentUtils.ImageInfo> localIterator = localHashMap.values().iterator();
            while (localIterator.hasNext()) {
                AttachmentUtils.ImageInfo localImageInfo = (AttachmentUtils.ImageInfo) localIterator
                        .next();
                localArrayList.add(NotesProvider.getImageFileUri(localImageInfo.imageName));
            }
        }
        Intent localIntent = new Intent();
        localIntent.putExtra("android.intent.extra.SUBJECT", subject);
        localIntent.putExtra("android.intent.extra.TEXT", outText);
        localIntent.putExtra("sms_body", outText);

        if ((localArrayList == null) || (localArrayList.isEmpty())) {
            localIntent.setAction("android.intent.action.SEND");
            localIntent.setType("text/plain");
        } else if (localArrayList.size() == 1) {
            Uri localUri = (Uri) localArrayList.get(0);
            String type = ((AttachmentUtils.ImageInfo) localHashMap.values().iterator().next()).mimeType;
            localIntent.setAction("android.intent.action.SEND");
            localIntent.setType(type);
            localIntent.putExtra("android.intent.extra.STREAM", localUri);
            localIntent.putExtra("mms_body", localUri);
        } else {
            localIntent.setAction("android.intent.action.SEND_MULTIPLE");
            localIntent.setType("image/*");
            localIntent.putParcelableArrayListExtra("android.intent.extra.STREAM", localArrayList);
        }

        mActivity.startActivity(Intent.createChooser(localIntent,
                mActivity.getString(R.string.menu_send)));
    }

    public void onPause() {
        //saveNote();//bug185832,mengzhiming.wt,modified 20160608
        dismissPopupWindow();
        unregisterObserver();
    }

    public void onResume() {
        updateBackground();
        updateTimeHeader();
        updateAlertHeader();
        updateAlertTime();
        registerObserver();
        mObserver.onChange(false);
    }

    public void onSaveInstanceState(Bundle paramBundle) {
            //bug185832,mengzhiming.wt,modified 20160608 ,start
        /*if (!mWorkingNote.existInDatabase())
            saveNote();*/
            //bug185832,mengzhiming.wt,modified 20160608,end
        if (mWorkingNote.getNoteId() > 0L)
        {
            Log.d(TAG, "Save working note id: " + this.mWorkingNote.getNoteId()
                    + " onSaveInstanceState");
            paramBundle.putLong(Intent.EXTRA_UID, mWorkingNote.getNoteId());
            mActivity.getIntent().setAction("");
        }
    }

    public void onStop() {
        dismissDialog();
        dismissPopupWindow();
        hideInputMethod();
    }

    protected void updateEditor() {
        mEditor.setNoteId(mWorkingNote.getNoteId());
        //bug198634,mengzhiming.wt,modified, 20160719
        mEditor.setFontSizeId(PreferenceUtils.getFontSize(mActivity, 0));
        mEditor.setQuery(mQueryString);
        String str = mWorkingNote.getContent();
        mEditor.setRichText(str);
        setSketchPenColor(PreferenceUtils.getSketchColor(mActivity, mActivity
                .getResources().getColor(ResourceParser.SketchResources.getSketchColorResource(7))));
        setSketchPenWidth(PreferenceUtils.getSketchWidth(
                mActivity,
                mActivity.getResources().getDimensionPixelSize(
                        ResourceParser.SketchResources.getSketchWidthResource(2))));
        //+bug84140,tangzihui.wt,add,2015.07.30,request ChildView focus.
        if (TextUtils.isEmpty(str)) {
            mEditor.requestChildFocus();
        }
        //-bug84140,tangzihui.wt,add,2015.07.30,request ChildView focus.
        //bug84140,tangzihui.wt,delete,2015.07.24,for display cursor.
        mEditor.setCursorVisible(true);
    }

    private void updateEditInfo(boolean paramBoolean) {
        /*
         * if (paramBoolean) { if (mInfo != null)
         * mInfo.setVisibility(View.VISIBLE); } else { if (mInfo != null)
         * mInfo.setVisibility(View.GONE); }
         */
    }

    private void confirmEditAnyway(DialogInterface.OnClickListener listener) {
        mActiveDialog = new AlertDialog.Builder(mActivity)
                .setTitle(R.string.alert_title_edit_anyway)
                .setMessage(R.string.alert_message_edit_anyway)
                .setPositiveButton(R.string.alert_action_edit_anyway, listener)
                .setNegativeButton(R.string.alert_action_edit_cancel, null).show();
    }

    private void createNewNote() {
        //saveNote();//bug185832,mengzhiming.wt,modified 20160608
        mActivity.finish();
        Intent intent = new Intent(mActivity, NoteEditActivity.class);
        intent.setAction(Intent.ACTION_INSERT_OR_EDIT);
        intent.putExtra("com.miui.notes.folder_id", mWorkingNote.getFolderId());
        mActivity.startActivity(intent);
    }

    private void gotoNotesList() {
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.LAUNCHER");
        intent.setClass(mActivity, NotesListActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        mActivity.startActivity(intent);
    }

    private void deleteCurrentNote() {
        HashSet<Long> localHashSet;
        if (mWorkingNote.existInDatabase()) {
            localHashSet = new HashSet<Long>();
            long noteId = mWorkingNote.getNoteId();
            if (noteId == 0L)
                Log.d(TAG, "Wrong note id, should not happen");
            localHashSet.add(Long.valueOf(noteId));
            if (!DataUtils.batchMoveToFolder(mActivity.getContentResolver(), localHashSet, -3L))
                Log.e(TAG, "Move notes to trash folder error, should not happens");
            if (!DataUtils.batchDeleteNotes(mActivity.getContentResolver(), localHashSet))
                Log.e(TAG, "Delete Note error");
        }
        mWorkingNote.markDeleted(true);
    }

    private boolean dismissDialog() {
        if (mActiveDialog != null) {
            if (mActiveDialog.isShowing())
                mActiveDialog.dismiss();
            mActiveDialog = null;
            return true;
        }
        return false;
    }

    public boolean dismissPopupWindow() {
        if (mPopupWindow != null) {
            if (mPopupWindow.isShowing()) {
                mPopupWindow.dismiss();
                return true;
            }
            return false;
        }
        return false;
    }

    private CharSequence[] getStyledFontSizeNames() {
        String[] arrayOfString = mActivity.getResources().getStringArray(R.array.font_size_names);
        CharSequence[] spans = new CharSequence[arrayOfString.length];
        for (int i = 0; i < arrayOfString.length; i++) {
            SpannableString textSpan = SpannableString.valueOf(arrayOfString[i]);
            int textAppearance = ResourceParser.TextAppearanceResources.getTexAppearanceResourceDialog(i);
            textSpan.setSpan(new TextAppearanceSpan(mActivity, textAppearance), 0,
                    textSpan.length(),
                    Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            /*
             * textSpan.setSpan(new TextAppearanceSpan(mActivity,
             * R.style.TextAppearance_Editor_Text_White), 0, textSpan.length(),
             * Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
             */
            spans[i] = textSpan;
        }
        return spans;
    }

    private boolean saveNote() {
          //bug194992,lilei.wt, modified,20160707 ,start
          String str = mEditor.getRichText().toString();
          if(str != null && !str.trim().equals("")){
              updateWorkingText();
              long noteId = mWorkingNote.getNoteId();
              boolean result = mWorkingNote.saveNote();
              //bug187783,mengzhiming.wt,modified 20160615,start
              if(alertChange){
                  saveClockAlert();
              }
              //bug187783,mengzhiming.wt,modified 20160615,end
              if ((noteId <= 0L) && (result)) {
                  mEditor.setNoteId(mWorkingNote.getNoteId());
                  mActivity.setResult(-1);
                  if (mActivity != null)
                      registerObserver();
              }
              return result;
          }else{
              Toast.makeText(mActivity, R.string.edit_null_message , Toast.LENGTH_SHORT).show();
              return false;
          }
        //bug194992,lilei.wt, modified,20160707  ,end
    }
    private void registerObserver() {
        if ((mWorkingNote != null) && (mWorkingNote.getNoteId() > 0L)) {
            Uri localUri = ContentUris.withAppendedId(NoteConstant.CONTENT_NOTE_URI,
                    mWorkingNote.getNoteId());
            mActivity.getContentResolver().registerContentObserver(localUri, false, mObserver);
        }
    }

    private void unregisterObserver() {
        mActivity.getContentResolver().unregisterContentObserver(mObserver);
    }

    private void sendToDesktop() {
        //bug185832,mengzhiming.wt,modified 20160608,start
        /*if (!mWorkingNote.existInDatabase())
            saveNote();*/
        //bug185832,mengzhiming.wt,modified 20160608,end
        if (mWorkingNote.getNoteId() > 0L) {
            int[] arrayOfInt = DataUtils.getWidgetInfoByNoteId(mActivity, mWorkingNote.getNoteId());
            if (arrayOfInt != null) {
                if (mWorkingNote.getWidgetId() != arrayOfInt[0])
                    mWorkingNote.setWidgetId(arrayOfInt[0]);
                if (mWorkingNote.getWidgetType() != arrayOfInt[1])
                    mWorkingNote.setWidgetType(arrayOfInt[1]);
            }
            if ((mWorkingNote.getWidgetId() != 0)
                    && (AppWidgetManager.getInstance(mActivity).getAppWidgetInfo(
                            mWorkingNote.getWidgetId()) != null))
                Toast.makeText(this.mActivity, R.string.error_widget_exist_for_send_to_desktop, 0)
                        .show();
            else {
                Intent localIntent = new Intent();
                localIntent.putExtra(Intent.EXTRA_UID, mWorkingNote.getNoteId());
                localIntent.putExtra("com.android.launcher.extra.widget.COMPONENT",
                        new ComponentName(
                                mActivity, NoteWidgetProvider.class));
                localIntent.setAction("com.android.launcher.action.INSTALL_WIDGET");
                mActivity.sendBroadcast(localIntent);
            }
        } else {
            Log.e(TAG, "Send to desktop error");
            Toast.makeText(mActivity, R.string.error_note_empty_for_send_to_desktop, 0).show();
        }
    }

    private void setReadonly(boolean paramBoolean) {
        if (paramBoolean == mWorkingNote.getReadonly())
            return;
        mWorkingNote.setReadonly(paramBoolean);
        RichEditor localRichEditor = mEditor;
        View.OnClickListener localOnClickListener = null;
        if (paramBoolean) {
            localOnClickListener = mEditorClickListener;
        }

        localRichEditor.setEditorClickListener(localOnClickListener);
        updateEditInfo(paramBoolean);

    }

    private void showDeleteDialog() {
        AlertDialog.Builder localBuilder = new AlertDialog.Builder(this.mActivity);
        localBuilder.setTitle(R.string.alert_title_delete);
        localBuilder.setIconAttribute(android.R.attr.alertDialogIcon);
        localBuilder.setMessage(R.string.alert_message_delete_note);
        localBuilder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface paramAnonymousDialogInterface, int paramAnonymousInt) {
                deleteCurrentNote();
                mActivity.finish();
            }
        });
        localBuilder.setNegativeButton(android.R.string.cancel, null);
        mActiveDialog = localBuilder.show();
    }

    private void showDiscardDialog() {
        AlertDialog.Builder localBuilder = new AlertDialog.Builder(mActivity);
        localBuilder.setTitle(R.string.alert_title_discard);
        localBuilder.setIconAttribute(android.R.attr.alertDialogIcon);
        localBuilder.setMessage(R.string.alert_message_discard_changes);
        localBuilder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface paramAnonymousDialogInterface, int paramAnonymousInt) {
                updateWorkingText();
                mWorkingNote.discardChanges();
                mActivity.finish();
            }
        });
        localBuilder.setNegativeButton(android.R.string.cancel, null);
        mActiveDialog = localBuilder.show();
    }

    private void showFontSizeDialog() {
        //bug198634,mengzhiming.wt,modified 20160719
        int i = PreferenceUtils.getFontSize(mActivity, 0);
        AlertDialog.Builder localBuilder = new AlertDialog.Builder(this.mActivity);
        // localBuilder.setView(view);
        localBuilder.setTitle(R.string.menu_font_size);
        localBuilder.setSingleChoiceItems(getStyledFontSizeNames(), i,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface paramAnonymousDialogInterface,
                            int paramAnonymousInt) {
                        PreferenceUtils.setFontSize(mActivity, paramAnonymousInt);
                        mEditor.setFontSizeId(paramAnonymousInt);
                        updateAllWidgets();
                    }
                });
        localBuilder.setPositiveButton(R.string.close, null);
        mActiveDialog = localBuilder.show();
    }

    private void updateAllWidgets() {
        ComponentName localComponentName = new ComponentName(mActivity, NoteWidgetProvider.class);
        int[] arrayOfInt = AppWidgetManager.getInstance(mActivity).getAppWidgetIds(
                localComponentName);
        updateWidget(mActivity, arrayOfInt);
    }

    protected boolean handleEditIntent(Intent intent) {
        long folderId = intent.getLongExtra(NoteConstant.INTENT_EXTRA_FOLDER_ID, 0L);
        int widgetId = intent.getIntExtra(NoteConstant.INTENT_EXTRA_WIDGET_ID, 0);
        int widgetType = intent.getIntExtra(NoteConstant.INTENT_EXTRA_WIDGET_TYPE, -1);
        int bgId = intent.getIntExtra(NoteConstant.INTENT_EXTRA_BACKGROUND_ID,
                ResourceParser.getDefaultBgId(mActivity));
        String phoneNumber = intent.getStringExtra("android.intent.extra.PHONE_NUMBER");
        long callDate = intent.getLongExtra(NoteConstant.INTENT_EXTRA_CALL_DATE, 0L);
        if ((callDate != 0L) && (phoneNumber != null)) {
            if (TextUtils.isEmpty(phoneNumber))
                Log.w(TAG, "The call record number is null");
            long existId = DataUtils.getNoteIdByPhoneNumberAndCallDate(
                    mActivity.getContentResolver(), phoneNumber, callDate);
            if (existId > 0L) {
                mWorkingNote = WorkingNote.load(mActivity, existId);
                if (mWorkingNote == null) {
                    Log.e(TAG, "load call note failed with note id" + existId);
                    return false;
                }
            } else {
                mWorkingNote = WorkingNote.createEmptyNote(mActivity, folderId, widgetId,
                        widgetType, bgId);
                mWorkingNote.convertToCallNote(phoneNumber, callDate);
            }
            if (mWorkingNote.getNoteId() == 0L)
                mActivity.getWindow().setSoftInputMode(
                        WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                                | WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        } else {
            mWorkingNote = WorkingNote.createEmptyNote(this.mActivity, folderId, widgetId,
                    widgetType, bgId);
            mActivity.getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                            | WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        }
        if (mWorkingNote != null)
            return true;
        else
            return false;
    }

    protected boolean handleViewIntent(Intent intent) {
        boolean bool = false;
        long uid = intent.getLongExtra(Intent.EXTRA_UID, 0L);
        if (intent.hasExtra(Intent.EXTRA_TEXT)) {
            mQueryString = intent.getStringExtra(Intent.EXTRA_TEXT);
            Log.d(TAG, "The query string is:" + mQueryString);
        }
        if (!DataUtils.visibleInNoteDatabase(mActivity.getContentResolver(), uid, 0)) {
            gotoNotesList();
            Toast.makeText(mActivity, R.string.error_note_not_exist, 0).show();
            if (intent.getIntExtra(NoteConstant.INTENT_EXTRA_WIDGET_ID, 0) > 0)
                updateAllWidgets();
        } else {
            mWorkingNote = WorkingNote.load(mActivity, uid);
            if (mWorkingNote == null) {
                Log.e(TAG, "load note failed with note id" + uid);
            } else {
                mActivity.getWindow().setSoftInputMode(
                        WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                                | WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
                bool = true;
            }
        }
        return bool;
    }

    protected boolean handleSendIntent(Intent paramIntent) {
        boolean bool = false;
        Log.d(TAG, "handleSendIntent");

        mWorkingNote = WorkingNote.createEmptyNote(mActivity, 0L, 0, -1,
                ResourceParser.getDefaultBgId(mActivity));
        SpanUtils.SimpleBuilder localSimpleBuilder = new SpanUtils.SimpleBuilder();
        String str1 = Contact.parseContactTextFromIntent(mActivity, paramIntent);
        if (!TextUtils.isEmpty(str1)) {
            localSimpleBuilder.appendText(str1);
            bool = true;
        }
        String str2 = paramIntent.getStringExtra("android.intent.extra.TEXT");
        if (!TextUtils.isEmpty(str2)) {
            localSimpleBuilder.appendText(str2);
            bool = true;
        }
        Uri localUri = (Uri) paramIntent.getParcelableExtra("android.intent.extra.STREAM");
        String type = paramIntent.getType();
        if (localUri != null&&type!=null&&type.startsWith("image/")) {
            String imageName = AttachmentUtils.saveImageFile(mActivity, localUri);
            if (imageName != null) {
                if (!localSimpleBuilder.isEmpty())
                    localSimpleBuilder.appendNewLine();
                localSimpleBuilder.appendImage(imageName);
                localSimpleBuilder.appendNewLine();
                bool = true;
            }
        }
        if (bool)
            mWorkingNote.setWorkingText(localSimpleBuilder.toString());
        return bool;
    }

    protected boolean handleInstanceState(Bundle bundle) {
        if ((bundle != null) && (bundle.containsKey(Intent.EXTRA_UID)))
        {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.putExtra(Intent.EXTRA_UID, bundle.getLong(Intent.EXTRA_UID));
            if (handleIntent(intent))
                Log.d("NoteEditImpl", "Restoring from killed activity");
            return true;
        }
        return false;
    }

    protected boolean handleIntent(Intent intent) {
        mWorkingNote = null;
        mQueryString = "";
        String action = intent.getAction();
        boolean bool;
        if (TextUtils.equals(Intent.ACTION_VIEW, action)) {
            bool = handleViewIntent(intent);
        }
        else if (TextUtils.equals(Intent.ACTION_INSERT_OR_EDIT, action)) {
            bool = handleEditIntent(intent);
        }
        else if ("android.intent.action.SEND".equals(action)) {
            bool = handleSendIntent(intent);
        } else {
            Log.e(TAG, "Intent not specified action, should not support");
            bool = false;
        }
        if (bool)
            mWorkingNote.setOnSettingStatusChangedListener(this);
        return bool;
    }

    private void updateWidget(Context paramContext, int[] widgetIds) {
        Intent localIntent = new Intent("android.appwidget.action.APPWIDGET_UPDATE");
        localIntent.setClass(paramContext, NoteWidgetProvider.class);
        localIntent.putExtra("appWidgetIds", widgetIds);
        paramContext.sendBroadcast(localIntent);
    }
    private void updateWorkingText() {
        String str = mEditor.getRichText().toString();
        mWorkingNote.setWorkingText(str);
    }
    public void hideInputMethod() {
        mEditor.hideInputMethod();
    }

    public void showInputMethod() {
        mEditor.showInputMethod();
    }

    public void disableHardwareAccelerated() {
        // mDisableHardwareAccelerated = true;
        if (mEditor != null)
            mEditor.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    protected void initializeEditor(View viewGroup) {
        mPanel = viewGroup.findViewById(R.id.editor);
        mEditor = ((RichEditor) viewGroup.findViewById(R.id.rich_editor));
        //bug183561,mengzhiming.wt,modified 20160608,start
        mEditor.getRichEditView().addTextChangedListener(new TextWatcher(){

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                    if(editable.length()>0){
                        mEditSave.setClickable(true);
                        mEditSave.setTextColor(Color.rgb(142, 195, 31));//bug 183561,mengzhiming.wt modified,20160608
                        saveEnable = true;//bug188011,mengzhiming.wt,add 20160621
                    }else{
                        mEditSave.setClickable(false);
                        mEditSave.setTextColor(Color.rgb(177, 177, 177));//bug 183561,mengzhiming.wt modified,20160608
                        saveEnable = false;//bug188011,mengzhiming.wt,add 20160621
                    }
                }
            });
            //bug183561,mengzhiming.wt,modified 20160608,end

            mSketcher = (SketchView) viewGroup.findViewById(R.id.sketch_view);
//        mCover = viewGroup.findViewById(R.id.edit_cover);
    }


    protected void initializeAlert(View viewGroup) {
        mAlertFill = ((View) viewGroup.findViewById(R.id.alert_fill));
        mAlertState = ((View) viewGroup.findViewById(R.id.alert_state));
        mAlertTxt = ((TextView) viewGroup.findViewById(R.id.alert_time));
        mNoAlertmargeUp = ((View) viewGroup.findViewById(R.id.no_alert_marge_up));
        //bug198669,mengzhiming.wt,add 20160729,start
        mAlertDel = ((ImageButton)viewGroup.findViewById(R.id.alert_del));
        mAlertDel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mWorkingNote.setAlertDate(0L, false);
                mAlertState.setVisibility(View.GONE);
                mNoAlertmargeUp.setVisibility(View.VISIBLE);
            }
        });
        //bug198669,mengzhiming.wt,add 20160729,end
    }

    protected void initializeButton(View viewGroup) {
        openAttachment = false;
        //bug199000,mengzhiming.wt,modified,20160720
        mAttachment_all = (View)viewGroup.findViewById(R.id.attachment_all);
        mAttachmentCamera = ((ImageButton)viewGroup.findViewById(R.id.attachment_camera1));
        mAttachmentGalerry = ((ImageButton)viewGroup.findViewById(R.id.attachment_galerry1));
        mAttachmentAlert = ((ImageButton)viewGroup.findViewById(R.id.attachment_alart));
        mAttachmentColor = ((ImageButton)viewGroup.findViewById(R.id.attachment_color));
        mAttachmentOnOff = ((ImageButton)viewGroup.findViewById(R.id.attachment_onoff));
        setAttachmentBg();//bug199000,mengzhiming.wt,modified,20160720,
        mAttachmentCamera.setOnClickListener(new OnClickListener() {

            public void onClick(View arg0) {
                Utils.takePhoto(mActivity, REQUEST_CODE_CAMERA);
           }
        });

        mAttachmentGalerry.setOnClickListener(new OnClickListener() {

            public void onClick(View arg0) {
                Utils.selectImage(mActivity, REQUEST_CODE_GALLERY);
            }
        });

        mAttachmentAlert.setOnClickListener(new OnClickListener() {

            public void onClick(View arg0) {
                //bug198669,mengzhiming.wt,modified 20160729,start
                /*if (mWorkingNote.hasClockAlert())
                    showPopupWindow(mAttachmentAlert, mEditor, new AlertPopupWindow(mActivity));
                else*/
                //bug198669,mengzhiming.wt,modified 20160729,start
                    showReminderDialog();
            }
        });

        mAttachmentColor.setOnClickListener(new OnClickListener() {

            public void onClick(View arg0) {
                showPopupWindow(mAttachmentColor, mEditor, new ColorPickerPopupWindow(mActivity));
            }
        });

        mAttachmentOnOff.setOnClickListener(new OnClickListener() {

            public void onClick(View arg0) {
                if(openAttachment){
                    mAttachmentCamera.setVisibility(View.GONE);
                    mAttachmentGalerry.setVisibility(View.GONE);
                    mAttachmentAlert.setVisibility(View.GONE);
                    mAttachmentColor.setVisibility(View.GONE);
                    mAttachmentOnOff.setImageResource(R.drawable.ic_attachment_on);
                    openAttachment = false;
                }else{
                    mAttachmentCamera.setVisibility(View.VISIBLE);
                    //bug173254,mengzhiming.wt, modified,20160606 ,start
                    if(!((NoteEditActivity)mActivity).isKeyguardLocked()){
                        mAttachmentGalerry.setVisibility(View.VISIBLE);
                    }
                    //bug173254,mengzhiming.wt, modified,20160606 ,end

                    mAttachmentAlert.setVisibility(View.VISIBLE);
                    mAttachmentColor.setVisibility(View.VISIBLE);
                    mAttachmentOnOff.setImageResource(R.drawable.ic_attachment_off);
                    openAttachment = true;
                }
            }
        });
        mEditCancel = ((Button)viewGroup.findViewById(R.id.cancel_edit));
        mEditSave = ((Button) viewGroup.findViewById(R.id.save_edit));
        mEditCancel.setOnClickListener(new OnClickListener() {

            public void onClick(View arg0) {
                gotoNotesList();
                mActivity.finish();
            }
        });

        mEditSave.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                     //bug 206039,mengzhiming.wt,add,20160805,start
                     //bug 210414 ,mengzhiming.wt,modified,20160818
                     if (alertSet && alertChange && System.currentTimeMillis() >= alertDate) {
                         CreatReminderPromptOrToast();
                         return;
                     }
                     //bug 206039,mengzhiming.wt,add,20160805,end
                     saveNote();
                     //bug173254,mengzhiming.wt, modified,20160606 ,start
                     if(((NoteEditActivity)mActivity).isKeyguardLocked()){
                         //do nothing
                         }else{
                         gotoNotesList();
                         }
                     //bug173254,mengzhiming.wt, modified,20160606 ,end
                     mActivity.finish();
            }
        });
    }

    protected void initializeHeader(View paramView) {
        mHeader = ((ViewGroup) paramView.findViewById(R.id.header));
        mHeaderUp = ((ImageView) paramView.findViewById(R.id.up));
        mHeaderDate = ((TextView) paramView.findViewById(R.id.date));
        mHeaderTime = ((TextView) paramView.findViewById(R.id.time));
        mHeaderAlert = ((TextView) paramView.findViewById(R.id.alert));
        mHeaderAttachment = ((ImageView) paramView.findViewById(R.id.attachment));
        mHeaderColor = ((ImageView) paramView.findViewById(R.id.color));
        View[] arrayOfView = new View[4];
        arrayOfView[0] = mHeaderUp;
        arrayOfView[1] = mHeaderAlert;
        arrayOfView[2] = mHeaderAttachment;
        arrayOfView[3] = mHeaderColor;
        for (int i = 0; i < arrayOfView.length; i++) {
            View localView = arrayOfView[i];
            if (localView != null)
                localView.setOnClickListener(this);
        }
    }

    protected void initializeViews() {
        View localView = mActivity.getWindow().getDecorView();
        initializeHeader(localView);
        initializeAlert(localView);
        initializeEditor(localView);
        initializeButton(localView);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK)
            switch (requestCode) {
                case REQUEST_CODE_GALLERY:
                    mEditor.insertImage(data.getData());
                    //saveNote();//bug185832,mengzhiming.wt,modified 20160608,end
                    break;
                case REQUEST_CODE_CAMERA:
                    File localFile = new File(AttachmentUtils.getTmpFile(mActivity));
                    mEditor.insertImage(Uri.fromFile(localFile));
                    //saveNote();//bug185832,mengzhiming.wt,modified 20160608,end
                    break;
                case REQUEST_CODE_CONTACT:
                    Uri contactData = data.getData();
                    if (contactData != null) {
                        mEditor.insertContact(contactData);
                        saveNote();
                    }
                    break;
                default:
                    mActivity.getWindow().setSoftInputMode(
                            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
                                    | WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                    break;
            }

    }

    public boolean onBackPressed() {
        /*if (dismissPopupWindow()) {
            return true;
        } else {
            saveNote();
            return false;
        }*/
        //bug188011,mengzhiming.wt,add 20160621,start
        if(saveEnable){
            //bug 206039,mengzhiming.wt,add,20160805,start
            //bug 210414 ,mengzhiming.wt,modified,20160818
            if (alertSet && alertChange && System.currentTimeMillis() >= alertDate) {
                CreatReminderPromptOrToast();
                return false;
            }
            //bug 206039,mengzhiming.wt,add,20160805,end
            saveNote();
        }
        //bug188011,mengzhiming.wt,add 20160621,end
        //bug185832,mengzhiming.wt,modified 20160608,start
        if(((NoteEditActivity)mActivity).isKeyguardLocked()){
            //do nothing
        }else{
            gotoNotesList();
        }
        //bug185832,mengzhiming.wt,modified 20160608,end
        mActivity.finish();
        return true;
    }

    private void showPopupWindow(View popView, View parentView, NotePopupWindow popWindow) {
        int[] arrayOfInt = new int[2];
        int width = popView.getWidth();
        popView.getLocationOnScreen(arrayOfInt);
        int x = arrayOfInt[0];
        popWindow.setArrowRawX(x + width / 2);
        int xoff = popWindow.getContentSize()[0];
        parentView.getLocationOnScreen(arrayOfInt);
        //+RTL,tangzihui.wt,modify,2015.11.20,discriminate between LTR and RTL.
        int i = popView.getId();
        switch (i) {
            case R.id.color:
            case R.id.attachment:
                if (parentView.getLayoutDirection() == View.LAYOUT_DIRECTION_LTR) {
                    popWindow.show(popView, arrayOfInt[0] - x + parentView.getWidth() - xoff, 0);
                } else {
                    popWindow.show(popView, arrayOfInt[0] - x - width, 0);
                }
                break;
            default:
                if (parentView.getLayoutDirection() == View.LAYOUT_DIRECTION_LTR) {
                    popWindow.show(popView, (width - xoff) / 2, -188);
                } else {
                    popWindow.show(popView, (width - xoff) / 2 - width, -188);
                }
                break;
        }
        //-RTL,tangzihui.wt,modify,2015.11.20,discriminate between LTR and RTL.
        mPopupWindow = popWindow;
    }

    public void onClick(View v) {
        // TODO Auto-generated method stub
        int i = v.getId();
        switch (i) {
            case R.id.color:
                showPopupWindow(mHeaderColor, mEditor, new ColorPickerPopupWindow(mActivity));
                break;
            case R.id.attachment:
                showPopupWindow(mHeaderAttachment, mEditor, new AttachmentPopupWindow(mActivity));
                break;
            case R.id.alert:
                if (mWorkingNote.hasClockAlert())
                    showPopupWindow(mHeaderAlert, mEditor, new AlertPopupWindow(mActivity));
                else
                    showReminderDialog();
                break;
            case R.id.up:
                saveNote();
                gotoNotesList();
                mActivity.finish();
                break;
            default:
                break;
        }

    }

    public void onBackgroundColorChanged() {
        // TODO Auto-generated method stub
        updateBackground();

    }

    private void updateBackground() {
        //mHeader.setBackgroundResource(mWorkingNote.getTitleBgResId());
        //mPanel.setBackgroundResource(mWorkingNote.getBgColorResId());
        mEditor.setBackgroundColor(mWorkingNote.getBgColor());
        mAlertFill.setBackgroundColor(mWorkingNote.getBgColor());
    }

    public void onClockAlertChanged(long date, boolean set) {
        // TODO Auto-generated method stub
        // bug 187783 ,mengzhiming.wt,modified 20160615,start
        /*if (!mWorkingNote.existInDatabase())
            saveNote();

        PendingIntent pendingIntent;
        AlarmManager localAlarmManager;

        if (mWorkingNote.getNoteId() > 0L) {
            Intent localIntent = new Intent(mActivity, AlarmReceiver.class);
            localIntent.setData(ContentUris.withAppendedId(NoteConstant.CONTENT_NOTE_URI,
                    mWorkingNote.getNoteId()));
            localIntent.putExtra(NoteConstant.INTENT_EXTRA_ALERT_DATE, date);
            pendingIntent = PendingIntent.getBroadcast(mActivity, 0, localIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            localAlarmManager = (AlarmManager) mActivity.getSystemService(Context.ALARM_SERVICE);
            updateAlertHeader();
            if (!set)
                localAlarmManager.cancel(pendingIntent);
            else {
                localAlarmManager.set(AlarmManager.RTC_WAKEUP, date, pendingIntent);

            }
        } else {
            if (set) {
                Log.e(TAG, "Clock alert setting error");
                mWorkingNote.setAlertDate(0L, false);
                Toast.makeText(this.mActivity, R.string.error_note_empty_for_clock, 0).show();
            }
        }*/
        alertDate=date;
        alertSet=set;
        alertChange=true;
        updateAlertHeader();
        //bug187783,mengzhiming.wt,modified 20160615,end
    }

    //bug187783,mengzhiming.wt,modified 20160615,start
    private void saveClockAlert() {
        PendingIntent pendingIntent;
        AlarmManager localAlarmManager;
        Intent localIntent = new Intent(mActivity, AlarmReceiver.class);
        localIntent.setData(ContentUris.withAppendedId(NoteConstant.CONTENT_NOTE_URI,
                mWorkingNote.getNoteId()));
        localIntent.putExtra(NoteConstant.INTENT_EXTRA_ALERT_DATE, alertDate);
        pendingIntent = PendingIntent.getBroadcast(mActivity, 0, localIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        localAlarmManager = (AlarmManager) mActivity.getSystemService(Context.ALARM_SERVICE);

        if (!alertSet)
            localAlarmManager.cancel(pendingIntent);
        else {
            localAlarmManager.set(AlarmManager.RTC_WAKEUP, alertDate, pendingIntent);
        }
        alertChange=false;
    }
    //bug187783,mengzhiming.wt,modified 20160615,end
    private void updateAlertHeader() {
        mHeaderAlert.setText("");
        if (mWorkingNote.hasClockAlert())
            if (System.currentTimeMillis() > mWorkingNote.getAlertDate())
                mHeaderAlert.setText(R.string.note_alert_expired);
            else
                Utils.setTextWithRelativeTime(mHeaderAlert, mWorkingNote.getAlertDate());
    }


    private void updateAlertTime() {
        if (mWorkingNote.hasClockAlert()){
            mAlertState.setVisibility(View.VISIBLE);
            mNoAlertmargeUp.setVisibility(View.GONE);
            if (System.currentTimeMillis() > mWorkingNote.getAlertDate()){
                mAlertTxt.setText(R.string.note_alert_expired);
            }else{
                //bug198634,mengzhiming.wt,modified 20160719
                mAlertTxt.setText(formatTimeforAlerm(Utils.formatTime(mWorkingNote.getAlertDate(), null)));
            }
        }else{
            mNoAlertmargeUp.setVisibility(View.VISIBLE);
        }
    }


    private void updateTimeHeader() {
        long m = mWorkingNote.getModifiedDate();
        Time modified = new Time();
        modified.set(m);
        Time now = new Time();
        now.setToNow();
        //String date;
        //if (modified.year == now.year)
        //    date = DateUtils.formatDateRange(mActivity, m, m, DateUtils.FORMAT_NO_YEAR);
        //else
        //date = DateUtils.formatDateRange(mActivity, m, m, DateUtils.FORMAT_NUMERIC_DATE);
        //mHeaderDate.setText(date);
        Integer[] args = {
                    modified.year, 1 + modified.month, modified.monthDay
            };
        //bug198634,mengzhiming.wt,modified 20160719,start
        //String formatStr = "%04d-%02d-%02d";
        String formatStr = "%04d" + mActivity.getString(R.string.separater_year)
                        + "%02d" + mActivity.getString(R.string.separater_month)
                        + "%02d" + mActivity.getString(R.string.separater_day);
        //bug198634,mengzhiming.wt,admodified 20160719,end

        String str = String.format(Locale.US, formatStr, args);
        mHeaderDate.setText(str);
        String time;
        if(DateFormat.is24HourFormat(mActivity))
            time= DateUtils.formatDateRange(mActivity, m, m, DateUtils.FORMAT_24HOUR
                | DateUtils.FORMAT_SHOW_TIME /*| DateUtils.FORMAT_SHOW_WEEKDAY*/);
        else
            time= DateUtils.formatDateRange(mActivity, m, m, DateUtils.FORMAT_12HOUR
                    | DateUtils.FORMAT_SHOW_TIME /*| DateUtils.FORMAT_SHOW_WEEKDAY*/);
        mHeaderTime.setText(time);
    }

    public void onWidgetChanged() {
        if (mWorkingNote.getWidgetType() != 1)
            Log.e("NoteEditImpl", "Unspported widget type");
        else {
            int[] widgetId = new int[1];
            widgetId[0] = mWorkingNote.getWidgetId();
            updateWidget(mActivity, widgetId);
        }
    }

    public void onCheckListModeChanged(int oldMode, int newMode) {
        // TODO Auto-generated method stub

    }

    public RichEditorImpl(Activity activity) {
        mActivity = activity;
        mObserver = new NoteContentObserver();
    }

    private void showReminderDialog() {
        long curTime = System.currentTimeMillis();
        if (curTime < mWorkingNote.getAlertDate())
            curTime = mWorkingNote.getAlertDate();
        //bug183570,mengzhiming.wt,modified 20160610,start

        /*TimePickerDialog localTimePickerDialog = new TimePickerDialog(mActivity,
                new TimePickerDialog.OnTimeSetListener() {
                    public void onTimeSet(TimePickerDialog dialog,
                            long time) {
                        mWorkingNote.setAlertDate(time, true);

                        mAlertState.setVisibility(View.VISIBLE);
                        mNoAlertmargeUp.setVisibility(View.GONE);
                        mAlertTxt.setText(Utils.formatTime(time,null));
                    }
                });
        localTimePickerDialog.update(curTime);
        localTimePickerDialog.show();
        mActiveDialog = localTimePickerDialog;*/
        DateTimePickerDialog dateTimePickerDialog = new DateTimePickerDialog(mActivity,
                new DateTimePickerDialog.OnDateTimeSetListener() {
                    public void onDateTimeSet(DateTimePickerDialog dialog,long time) {
                        //bug 206039,mengzhiming.wt,add,20160805,start
                        if (System.currentTimeMillis() >= time) {
                            CreatReminderPromptOrToast();
                            return;
                        }
                        //bug 206039,mengzhiming.wt,add,20160805,end
                        mWorkingNote.setAlertDate(time, true);

                        mAlertState.setVisibility(View.VISIBLE);
                        mNoAlertmargeUp.setVisibility(View.GONE);
                        //bug198634,mengzhiming.wt,modified 20160719
                        mAlertTxt.setText(formatTimeforAlerm(Utils.formatTime(time, null)));
                    }
                });
        dateTimePickerDialog.update(curTime);
        dateTimePickerDialog.show();
        mActiveDialog = dateTimePickerDialog;
        //bug183570,mengzhiming.wt,modified 20160610,end
    }

    private class NoteContentObserver extends ContentObserver {
        public NoteContentObserver() {
            super(null);
        }

        public void onChange(boolean paramBoolean) {
            if ((mWorkingNote != null) && (mWorkingNote.getNoteId() > 0L)) {
                int[] widget = DataUtils.getWidgetInfoByNoteId(mActivity, mWorkingNote.getNoteId());
                if (widget != null) {
                    if (mWorkingNote.getWidgetId() != widget[0]) {
                        mWorkingNote.setWidgetId(widget[0]);
                        mWorkingNote.setWidgetType(widget[1]);
                    }
                }
            }
        }

    }

    public class ColorPickerPopupWindow extends NotePopupWindow implements View.OnClickListener {

        public ColorPickerPopupWindow(Context context)
        {
            super(context);
            View localView;
            localView = setActionView(R.layout.pop_content_pick_color);
            for (int k = 0; k < sBgSelectorBtnsMap.size(); k++)
                localView.findViewById(sBgSelectorBtnsMap.keyAt(k))
                        .setOnClickListener(this);
            localView.findViewById(
                    sBgSelectorSelectionMap.get(mWorkingNote
                            .getBgColorId())).setVisibility(View.VISIBLE);
        }

        public void onClick(View v) {
            // TODO Auto-generated method stub
            int i = v.getId();
            if (sBgSelectorBtnsMap.indexOfKey(i) >= 0) {
                getContentView().findViewById(
                        sBgSelectorSelectionMap.get(mWorkingNote
                                .getBgColorId())).setVisibility(View.GONE);
                mWorkingNote.setBgColorId(sBgSelectorBtnsMap.get(i));
                setAttachmentBg();//bug199000,mengzhiming.wt,modified,20160720,
            }
            dismiss();
        }
    }

    private class AttachmentPopupWindow extends NotePopupWindow implements View.OnClickListener {
        public AttachmentPopupWindow(Context context) {
            super(context);
            View localView = setActionView(R.layout.pop_content_attachment);
            localView.findViewById(R.id.attachment_gallery).setOnClickListener(this);
            localView.findViewById(R.id.attachment_camera).setOnClickListener(this);
            localView.findViewById(R.id.attachment_contact).setOnClickListener(this);
            localView.findViewById(R.id.attachment_sketch).setOnClickListener(this);
            localView.setMinimumHeight(mActivity.getResources().getDimensionPixelSize(
                    R.dimen.popup_view_min_height_edit));
        }

        public void onClick(final View paramView) {
            if (mWorkingNote.getReadonly())
                confirmEditAnyway(new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface paramAnonymousDialogInterface,
                            int paramAnonymousInt) {
                        setReadonly(false);

                    }
                });
            switch (paramView.getId()) {
                case R.id.attachment_gallery:
                    Utils.selectImage(mActivity, REQUEST_CODE_GALLERY);
                    break;
                case R.id.attachment_camera:
                    Utils.takePhoto(mActivity, REQUEST_CODE_CAMERA);
                    break;
                case R.id.attachment_contact:
                    Utils.selectContact(mActivity, REQUEST_CODE_CONTACT);
                    break;
                case R.id.attachment_sketch:
                    mActivity.startActionMode(new SketchModeCallBack());
                    break;
                default:
                    break;
            }
            dismiss();
        }
    }

    private class AlertPopupWindow extends NotePopupWindow implements View.OnClickListener {
        public AlertPopupWindow(Context context) {
            super(context);
            View localView = setActionView(R.layout.pop_content_alert_edit);
            localView.findViewById(R.id.update_alert).setOnClickListener(this);
            localView.findViewById(R.id.delete_alert).setOnClickListener(this);
            localView.setMinimumHeight(RichEditorImpl.this.mActivity.getResources()
                    .getDimensionPixelSize(R.dimen.popup_view_min_height_edit));

        }

        public void onClick(View paramView) {
            switch (paramView.getId()) {
                default:
                case R.id.update_alert:
                    showReminderDialog();
                    break;
                case R.id.delete_alert:
                    mWorkingNote.setAlertDate(0L, false);
                    mAlertState.setVisibility(View.GONE);
                    mNoAlertmargeUp.setVisibility(View.VISIBLE);
                    break;
            }
            dismiss();
        }
    }

    public void onNewIntent(Intent paramIntent) {
        // TODO Auto-generated method stub
        //saveNote(); //bug185832,mengzhiming.wt,modified 20160608,end
        if (!handleIntent(paramIntent)) {
            Log.e(TAG, "Fail to handle intent in onNewIntent, intent=" + paramIntent + ", bundle="
                    + paramIntent.getExtras());
            mWorkingNote = WorkingNote.createEmptyNote(mActivity, 0L, 0, -1,
                    ResourceParser.getDefaultBgId(mActivity));
        }
        updateEditor();
    }

    private class SketchColorPickWindow extends NotePopupWindow implements View.OnClickListener {
        private int[][] views = {
                {
                        R.id.sketch_colorpick1, R.color.sketch_pencolor1
                },
                {
                        R.id.sketch_colorpick2, R.color.sketch_pencolor2
                },
                {
                        R.id.sketch_colorpick3, R.color.sketch_pencolor3
                },
                {
                        R.id.sketch_colorpick4, R.color.sketch_pencolor4
                },
                {
                        R.id.sketch_colorpick5, R.color.sketch_pencolor5
                },
                {
                        R.id.sketch_colorpick6, R.color.sketch_pencolor6
                },
                {
                        R.id.sketch_colorpick7, R.color.sketch_pencolor7
                },
                {
                        R.id.sketch_colorpick8, R.color.sketch_pencolor8
                },
        };

        public SketchColorPickWindow(Context context) {
            super(context);
            View localView = setActionView(R.layout.sketch_pickcolor);
            int color = getSketchPenColor();
            for (int[] info : views) {
                View v = localView.findViewById(info[0]);
                v.setBackgroundResource(info[1]);
                if (color == mActivity.getResources().getColor(info[1])) {
                    v.findViewById(R.id.iv_color_selector).setVisibility(View.VISIBLE);
                }
                v.setOnClickListener(this);
            }
            localView.setMinimumHeight(mActivity.getResources().getDimensionPixelSize(
                    R.dimen.popup_view_min_height_edit));
        }

        public void onClick(final View paramView) {
            int id = paramView.getId();
            for (int[] info : views) {
                if (id == info[0]) {
                    setSketchPenColor(mActivity.getResources().getColor(info[1]));
                    PreferenceUtils.setSketchColor(mActivity,
                            mActivity.getResources().getColor(info[1]));
                    break;
                }
            }
            dismiss();
        }
    }

    private class SketchWidthPickWindow extends NotePopupWindow implements View.OnClickListener {
        private int[][] views = {
                {
                        R.id.sketch_widthpick1, R.dimen.sketch_widthpicker_item1
                },
                {
                        R.id.sketch_widthpick2, R.dimen.sketch_widthpicker_item2
                },
                {
                        R.id.sketch_widthpick3, R.dimen.sketch_widthpicker_item3
                },
                {
                        R.id.sketch_widthpick4, R.dimen.sketch_widthpicker_item4
                },
                {
                        R.id.sketch_widthpick5, R.dimen.sketch_widthpicker_item5
                },
        };

        public SketchWidthPickWindow(Context context) {
            super(context);
            View localView = setActionView(R.layout.sketch_pickwidth);
            int width = getSketchPenWidth();
            for (int[] info : views) {
                View v = localView.findViewById(info[0]);
                if (width == mActivity.getResources().getDimensionPixelSize(info[1])) {
                    // v.setBackgroundColor(color.black);
                    v.setBackgroundResource(R.drawable.sketch_width_picker_selected);
                }
                v.setOnClickListener(this);
            }
            localView.setMinimumHeight(mActivity.getResources().getDimensionPixelSize(
                    R.dimen.popup_view_min_height_edit));
        }

        public void onClick(final View paramView) {
            int id = paramView.getId();
            for (int[] info : views) {
                if (id == info[0]) {
                    setSketchPenWidth(mActivity.getResources().getDimensionPixelSize(
                            info[1]));
                    PreferenceUtils.setSketchWidth(mActivity, mActivity.getResources()
                            .getDimensionPixelSize(
                                    info[1]));
                    break;
                }
            }
            dismiss();
        }
    }

    public void openColorPickWindow(View v) {
        // TODO Auto-generated method stub
        showPopupWindow(v, mEditor, new SketchColorPickWindow(mActivity));
    }

    public void openWidthPickWindow(View v) {
        // TODO Auto-generated method stub
        showPopupWindow(v, mEditor, new SketchWidthPickWindow(mActivity));

    }

    private void enterSketchMode() {
        hideInputMethod();
        setSketchState(true);
    }

    private void exitSketchMode() {
        setSketchState(false);
    }

    private class SketchModeCallBack implements ActionMode.Callback, OnClickListener {
        private ActionMode mActionMode;
        private ImageView mColor;
        private ImageView mWidth;
        private ImageView mDelete;
        private ImageView mCancel;
        private ImageView mRecover;
        private ImageView mSend;

        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            View v = LayoutInflater.from(mActivity).inflate(
                    R.layout.sketch_edit, null);
            mColor = (ImageView) v.findViewById(R.id.sketch_color);
            mWidth = (ImageView) v.findViewById(R.id.sketch_width);
            mDelete = (ImageView) v.findViewById(R.id.sketch_delete);
            mCancel = (ImageView) v.findViewById(R.id.sketch_cancel);
            mRecover = (ImageView) v.findViewById(R.id.sketch_recover);
            mSend = (ImageView) v.findViewById(R.id.sketch_send);
            mColor.setOnClickListener(this);
            mWidth.setOnClickListener(this);
            mDelete.setOnClickListener(this);
            mCancel.setOnClickListener(this);
            mRecover.setOnClickListener(this);
            mSend.setOnClickListener(this);
            mode.setCustomView(v);
            mActionMode = mode;
            enterSketchMode();
            //mActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NEEDS_MENU_KEY); //lgj sdk5.1 not supprot
            mBackPressed = false;
            return true;
        }

        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            // TODO Auto-generated method stub
            return true;
        }

        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            boolean ret = false;
            return ret;
        }

        public void onDestroyActionMode(ActionMode mode) {
            dismissPopupWindow();
            exitSketchMode();
            //mActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_NEEDS_MENU_KEY);//lgj sdk5.1 not supprot

            if (!mBackPressed)
                savePageSketch();
        }

        public void onClick(View v) {
            // TODO Auto-generated method stub
            switch (v.getId()) {
                case R.id.sketch_color:
                    openColorPickWindow(mColor);
                    if (getSketchDeleteMode()) {
                        setSketchDeleteMode();
                        mDelete.setImageResource(R.drawable.ic_sketch_delete);
                    }
                    break;

                case R.id.sketch_width:
                    openWidthPickWindow(mWidth);
                    if (getSketchDeleteMode()) {
                        setSketchDeleteMode();
                        mDelete.setImageResource(R.drawable.ic_sketch_delete);
                    }

                    break;

                case R.id.sketch_delete:
                    setSketchDeleteMode();
                    if (getSketchDeleteMode()) {
                        mDelete.setImageResource(R.drawable.sketch_delete_mode);
                    } else {
                        mDelete.setImageResource(R.drawable.ic_sketch_delete);
                    }
                    break;
                case R.id.sketch_cancel:
                    cancelPath();
                    break;
                case R.id.sketch_recover:
                    recoverPath();
                    break;
                case R.id.sketch_send:
                    if (getSketchDeleteMode()) {
                        setSketchDeleteMode();
                        mDelete.setImageResource(R.drawable.ic_sketch_delete);
                    }
                    sendAsPicture();
                    break;
                default:
                    break;
            }
        }

    }

    public void setSketchState(boolean b) {
        mSketcher.setSketchState(b);
        // 开启画板模式，设定surfaceview
        if (b) {
            mSketcher.clearBoard();
            mSketcher.setVisibility(View.VISIBLE);
 //           mCover.setVisibility(View.GONE);
        } else {
            mSketcher.setVisibility(View.GONE);
//            mCover.setVisibility(View.VISIBLE);
        }
    }

    public boolean getSketchState() {
        return mSketcher.getSketchState();
    }

    public void setSketchDeleteMode() {
        // TODO Auto-generated method stub
        mSketcher.setSketchDeleteMode();
    }

    public boolean getSketchDeleteMode() {
        // TODO Auto-generated method stub
        return mSketcher.getSketchDeleteMode();
    }

    public void setSketchPenColor(int color) {
        // TODO Auto-generated method stub
        mSketcher.setPaintColor(color);
    }

    public int getSketchPenColor() {
        return mSketcher.getPaintColor();
    }

    public void setSketchPenWidth(int width) {
        // TODO Auto-generated method stub
        mSketcher.setLineWidth(width);
    }

    public int getSketchPenWidth() {
        return mSketcher.getLineWidth();
    }

    public void cancelPath() {
        mSketcher.cancelPath();
    }

    public void recoverPath() {
        mSketcher.recoverPath();

    }

    public Bitmap getPageBmp() {
        // TODO Auto-generated method stub
        Bitmap bmp = Bitmap.createBitmap(mEditor.getMeasuredWidth(),
                mEditor.getMeasuredHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        canvas.drawColor(Color.WHITE);// 此处背景设为白色
        mSketcher.drawPathes(canvas);
        return bmp;
    }

    public void insertSketchImage(Bitmap bmp) {
        // TODO Auto-generated method stub
        mEditor.insertSketchImage(bmp);
    }

    public void setBackKeyPressed(boolean b) {
        // TODO Auto-generated method stub
        mBackPressed = b;
    }

    //bug198634,mengzhiming.wt,added 20160719,start
    private String formatTimeforAlerm(String date_time) {
        String[] strs = date_time.split(" ");
        String[] times = strs[0].split("-");
        String displaytime = times[0] + mActivity.getString(R.string.separater_year)
                + times[1] + mActivity.getString(R.string.separater_month)
                + times[2] + mActivity.getString(R.string.separater_day)
                + " " + strs[1];
        return displaytime;
    }
    //bug198634,mengzhiming.wt,added 20160719,end

    //bug199000,mengzhiming.wt,modified,20160720,start
    private void setAttachmentBg() {
        int color = mWorkingNote.getBgColor();
        mAttachment_all.setBackgroundColor(color);
        mAttachmentCamera.setBackgroundColor(color);
        mAttachmentGalerry.setBackgroundColor(color);
        mAttachmentAlert.setBackgroundColor(color);
        mAttachmentColor.setBackgroundColor(color);
        mAttachmentOnOff.setBackgroundColor(color);
    }
    //bug199000,mengzhiming.wt,modified,20160720,end

    //bug 206039,mengzhiming.wt,add,20160805,start
    private void CreatReminderPromptOrToast() {
        if(((NoteEditActivity)mActivity).isKeyguardLocked()) {
            AlertDialog alertDialog = new AlertDialog.Builder(mActivity)
                    .setTitle(mActivity.getString(R.string.alert_prompt_title))
                    .setMessage(mActivity.getString(R.string.alert_expired_prompt))
                    .setNegativeButton(android.R.string.ok, null)
                    .create();
            alertDialog.show();
        }else{
            Toast.makeText(mActivity, R.string.alert_expired_prompt, Toast.LENGTH_SHORT).show();
        }
    }
    //bug 206039,mengzhiming.wt,add,20160805,end

}
