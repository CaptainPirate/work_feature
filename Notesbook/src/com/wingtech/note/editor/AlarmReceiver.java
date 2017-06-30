/*===================================================================================================*	
 *  when  |      who     |    keyword           |        why         |         what                  *	
 *===================================================================================================*	
 *20160407|mengzhiming.wt|porting A1s enjoynotes| Port_EnjoyNotes | porting A1s enjoynotes to CMCC N2*	
*====================================================================================================*/

package com.wingtech.note.editor;

import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;

import com.wingtech.note.data.DataUtils;
import com.wingtech.note.data.NoteConstant;

public class AlarmReceiver extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {
        String str = DataUtils.verifyNoteAlert(context,
                ContentUris.parseId(intent.getData()),
                intent.getLongExtra(NoteConstant.INTENT_EXTRA_ALERT_DATE, 0L));
        if (str != null) {
            intent.setClass(context, AlarmAlertDialog.class);
            intent.addFlags(0x10000000);// Intent.FLAG_RECEIVER_FOREGROUND
            intent.putExtra("com.wingtech.note.snippet", str);
            context.startActivity(intent);
        }
    }
}
