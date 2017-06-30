/*===================================================================================================*	
 *  when  |      who     |    keyword           |        why         |         what                  *	
 *===================================================================================================*	
 *20160407|mengzhiming.wt|porting A1s enjoynotes| Port_EnjoyNotes | porting A1s enjoynotes to CMCC N2*	
*====================================================================================================*/

package com.wingtech.note.editor;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;

import com.wingtech.note.data.NoteConstant;

public class AlarmInitReceiver extends BroadcastReceiver {
    private static final int COLUMN_ALERTED_DATE = 1;
    private static final int COLUMN_ID = 0;
    private static final String[] PROJECTION = {
            "_id", "alert_date"
    };

    // ¿ª»úÊ±³õÊ¼»¯ÄÖÖÓreceiver
    public void onReceive(Context context, Intent data) {
        long nowTime = System.currentTimeMillis();
        ContentResolver contentResolver = context.getContentResolver();
        Uri localUri = NoteConstant.CONTENT_NOTE_URI;
        String[] args = {
                String.valueOf(nowTime)
        };
        Cursor cursor = contentResolver.query(localUri, PROJECTION,
                "alert_date>? AND type=0", args, null);
        try {
            if (cursor.moveToFirst())
                do {
                    long alretTime = cursor.getLong(COLUMN_ALERTED_DATE);
                    Intent intent = new Intent(context, AlarmReceiver.class);
                    intent.setData(ContentUris.withAppendedId(NoteConstant.CONTENT_NOTE_URI,
                            cursor.getLong(COLUMN_ID)));
                    intent.putExtra(NoteConstant.INTENT_EXTRA_ALERT_DATE, alretTime);
                    PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0,
                            intent, PendingIntent.FLAG_UPDATE_CURRENT);
                    ((AlarmManager) context.getSystemService(Context.ALARM_SERVICE)).set(
                            AlarmManager.RTC_WAKEUP, alretTime,
                            pendingIntent);
                } while (cursor.moveToNext());
        } catch (Exception e) {

        } finally {
            if (cursor != null)
                cursor.close();
        }
    }
}

