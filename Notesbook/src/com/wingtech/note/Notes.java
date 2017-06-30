/*===================================================================================================*	
 *  when  |      who     |    keyword           |        why         |         what                  *	
 *===================================================================================================*	
 *20160407|mengzhiming.wt|porting A1s enjoynotes| Port_EnjoyNotes | porting A1s enjoynotes to CMCC N2*	
*====================================================================================================*/

package com.wingtech.note;

import android.app.Application;

public class Notes extends Application {
    private static final String TAG = "Notes";

    public void onCreate() {
        AttachmentUtils.checkAttachmentDir(getApplicationContext());
        new Thread(new Runnable() {

            public void run() {
                // TODO Auto-generated method stub
                AttachmentUtils.checkAttachmentFile(getApplicationContext());

            }
        }).start();
    }

}
