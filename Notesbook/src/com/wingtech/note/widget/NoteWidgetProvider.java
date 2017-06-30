/*===================================================================================================*	
 *  when  |      who     |    keyword           |        why         |         what                  *	
 *===================================================================================================*	
 *20160407|mengzhiming.wt|porting A1s enjoynotes| Port_EnjoyNotes | porting A1s enjoynotes to CMCC N2*	
*====================================================================================================*/

package com.wingtech.note.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.NinePatchDrawable;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;
import android.provider.Settings;

import com.wingtech.note.AttachmentUtils;
import com.wingtech.note.ResourceParser;
import com.wingtech.note.Utils;
import com.wingtech.note.data.DataUtils;
import com.wingtech.note.data.NoteConstant;
import com.wingtech.note.data.NoteConstant.NoteColumns;
import com.wingtech.note.editor.NoteEditActivity;
import com.wingtech.note.list.NotesListActivity;
import com.wingtech.note.spannableparser.SpanUtils;
import com.wingtech.note.R;

public class NoteWidgetProvider extends AppWidgetProvider {
    public static final int COLUMN_ID = 0;
    public static final int COLUMN_BG_COLOR_ID = 1;
    public static final int COLUMN_SNIPPET = 2;
    public static final String[] PROJECTION = {
            NoteColumns.ID, NoteColumns.BG_COLOR_ID, NoteColumns.SNIPPET
    };
    private static final String TAG = "NoteWidgetProvider";

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO Auto-generated method stub
        super.onReceive(context, intent);
        String action = intent.getAction();
		/*porting A1s enjoynotes, mengzhiming.wt,modified 20160405
        if ("android.password.action.visitor.mode".equals(action)) {
            AppWidgetManager am = AppWidgetManager.getInstance(context);
            boolean b = Settings.Global.getInt(context.getContentResolver(),
                    Settings.Global.VISITOR_MODE_ON, 0) == 1 ? true : false;

            int[] widgetIds = am.getAppWidgetIds(new ComponentName(context, getClass()));
            if (widgetIds.length > 0)
                update(context, am, widgetIds, b);
        } else */if (("com.android.launcher.action.INSTALL_WIDGET_COMPLETED".equals(action))) {
            long noteId = intent.getLongExtra("com.android.launcher.extra.widget.data", 0L);
            int widgetId = intent.getIntExtra("com.android.launcher.extra.widget.id", 0);
            //bug153130,tangzihui.wt,add,2016.03.15,for returning a result.
            int result = intent.getIntExtra("com.android.launcher.extra.widget.result",0);
            if (widgetId == 0) {// 添加失败
                //+bug153130,tangzihui.wt,modify,2016.03.15,for returning a result.
                if (result == -1) {
                    Toast.makeText(context, R.string.widget_note_add_no_space_fail, 0).show();
                } else {
                    Toast.makeText(context, R.string.widget_note_add_fail, 0).show();
                }
                //-bug153130,tangzihui.wt,modify,2016.03.15,for returning a result.
            } else if (DataUtils.updateNoteWidgetId(context, noteId, widgetId) > 0) {
                AppWidgetManager am = AppWidgetManager.getInstance(context);
                int[] widgetIds = new int[1];
                widgetIds[0] = widgetId;
                onUpdate(context, am, widgetIds);
                Toast.makeText(context, R.string.widget_note_sendto_desktop, 0).show();
            }
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // TODO Auto-generated method stub
        /*porting A1s enjoynotes, mengzhiming.wt,modified 20160405
		boolean b = Settings.Global.getInt(context.getContentResolver(),
                Settings.Global.VISITOR_MODE_ON, 0) == 1 ? true : false;
        update(context, appWidgetManager, appWidgetIds, b);
		*/
		update(context, appWidgetManager, appWidgetIds, false);//porting A1s enjoynotes, mengzhiming.wt,modified 20160405
    }

    // 参数mode对应隐私保护，如果有该功能，可以屏蔽在桌面上的显示
    private void update(Context context, AppWidgetManager am, int[] widgetIds,
            boolean mode) {
        for (int widgetId : widgetIds) {
            if (widgetId <= 0)
                continue;
            int resId = ResourceParser.getDefaultBgId(context);
            String content;
            Intent intent = new Intent(context, NoteEditActivity.class);
            Intent intent2 = new Intent(context, NotesListActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.putExtra(NoteConstant.INTENT_EXTRA_WIDGET_ID, widgetId);
            intent.putExtra(NoteConstant.INTENT_EXTRA_WIDGET_TYPE, getWidgetType());
            Cursor cursor = getNoteWidgetInfo(context, widgetId);
            try {
                if ((cursor == null) || (!cursor.moveToFirst())) {
                    content = context.getResources().getString(R.string.widget_empty_content);
                    intent.setAction(Intent.ACTION_INSERT_OR_EDIT);
                }
                else if (cursor.getCount() > 1) {
                    Log.e(TAG, "Widget id :" + widgetId + "repeated.");
                    continue;
                } else {
                    resId = cursor.getInt(COLUMN_BG_COLOR_ID);
                    content = cursor.getString(COLUMN_SNIPPET);
                    intent.putExtra(Intent.EXTRA_UID, cursor.getLong(COLUMN_ID));
                    intent.setAction(Intent.ACTION_VIEW);
                }
                RemoteViews localRemoteViews = new RemoteViews(context.getPackageName(),
                        getLayoutId());
                localRemoteViews.setImageViewResource(R.id.widget_bg_image, getBgResourceId(resId));
                // localRemoteViews.setImageViewResource(R.id.widget_text_cover,
                // getTextCoverResourceId(resId));
                intent.putExtra(NoteConstant.INTENT_EXTRA_BACKGROUND_ID, resId);
                PendingIntent localPendingIntent;
				/*porting A1s enjoynotes, mengzhiming.wt,modified 20160405
                if (mode) {
					Log.d(TAG, "osborn: private mode = true");
                    localPendingIntent = PendingIntent.getActivity(context, widgetId,
                            intent2, PendingIntent.FLAG_UPDATE_CURRENT);
                    localRemoteViews.setViewVisibility(R.id.widget_image, View.GONE);
                    localRemoteViews
                            .setTextViewText(R.id.widget_text,
                                    context.getString(R.string.widget_under_visit_mode));
                }
                else*/ {
                    localPendingIntent = PendingIntent.getActivity(context, widgetId,
                            intent, PendingIntent.FLAG_UPDATE_CURRENT);
                    SpanUtils.NoteImgInfo info = new SpanUtils.NoteImgInfo();
                    CharSequence showText = SpanUtils.parseStrikethroughSpan(Utils
                            .trimEmptyLineSequence(SpanUtils
                                    .normalizeSnippet(content, info)));
                    Bitmap localBitmap = null;
                    if (info.firstImgName != null) {
                        NinePatchDrawable imgCover = (NinePatchDrawable) context
                                .getResources().getDrawable(getImageCoverResourceId());
                        localBitmap = Utils.clipImageAttachment(
                                AttachmentUtils.getAttachmentPath(context, info.firstImgName),
                                imgCover);
                    }
                    if (localBitmap != null) {
                        localRemoteViews.setViewVisibility(R.id.widget_image, View.VISIBLE);
                        localRemoteViews
                                .setBitmap(R.id.widget_image, "setImageBitmap", localBitmap);
                        localRemoteViews.setTextViewText(R.id.widget_text, showText);
                    } else {
                        localRemoteViews.setViewVisibility(R.id.widget_image, View.GONE);
                        localRemoteViews.setTextViewText(R.id.widget_text, showText);
                    }
                }
                localRemoteViews.setOnClickPendingIntent(R.id.widget_layout, localPendingIntent);
                am.updateAppWidget(widgetId, localRemoteViews);
            } finally {
                if (cursor != null)
                    cursor.close();
            }
        }

    }

    // 删除便签，更新数据库对用note项的widget_id为0
    public void onDeleted(Context context, int[] appWidgetIds) {
        ContentValues localContentValues = new ContentValues();
        localContentValues.put(NoteConstant.NoteColumns.WIDGET_ID, Integer.valueOf(0));
        for (int i = 0; i < appWidgetIds.length; i++) {
            ContentResolver localContentResolver = context.getContentResolver();
            Uri localUri = NoteConstant.CONTENT_NOTE_URI;
            String[] args = new String[1];
            args[0] = String.valueOf(appWidgetIds[i]);
            localContentResolver.update(localUri, localContentValues, "widget_id=?", args);
        }
    }

    private Cursor getNoteWidgetInfo(Context context, int widgetId) {
        ContentResolver contentResolver = context.getContentResolver();
        Uri localUri = NoteConstant.CONTENT_NOTE_URI;
        String[] projection = PROJECTION;
        String[] args = new String[1];
        args[0] = String.valueOf(widgetId);
        return contentResolver.query(localUri, projection,
                "widget_id=? AND (parent_id>=0 OR parent_id=-2)", args, null);
    }

    private int getBgResourceId(int resId) {
        return ResourceParser.WidgetResources.getWidgetBgResource(resId);
    }

    private int getImageCoverResourceId() {
        return R.drawable.widget_image_cover;
    }

    private int getLayoutId() {
        return R.layout.widget;
    }

    private int getWidgetType() {
        return 1;
    }
}

