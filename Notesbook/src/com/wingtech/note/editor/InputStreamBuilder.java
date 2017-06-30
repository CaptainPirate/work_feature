/*===================================================================================================*	
 *  when  |      who     |    keyword           |        why         |         what                  *	
 *===================================================================================================*	
 *20160407|mengzhiming.wt|porting A1s enjoynotes| Port_EnjoyNotes | porting A1s enjoynotes to CMCC N2*	
*====================================================================================================*/

package com.wingtech.note.editor;

import android.content.Context;
import android.net.Uri;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class InputStreamBuilder {

    private Context nContext;
    private String nFilePath;
    private int nResId;

    public InputStreamBuilder(Context context, int resId) {
        nContext = context;
        nResId = resId;
    }

    public InputStreamBuilder(String filePath) {
        nFilePath = filePath;
    }

    public InputStream getInputStream()
            throws FileNotFoundException {
        if (nFilePath != null) {
            return new FileInputStream(nFilePath);
        }
        else {
            Uri url = Uri.parse("android.resource://" + nContext.getPackageName() + "/"
                    + nResId);
            return nContext
                    .getContentResolver().openInputStream(url);
        }
    }
}
