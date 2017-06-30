/*===================================================================================================*	
 *  when  |      who     |    keyword           |        why         |         what                  *	
 *===================================================================================================*	
 *20160407|mengzhiming.wt|porting A1s enjoynotes| Port_EnjoyNotes | porting A1s enjoynotes to CMCC N2*	
*====================================================================================================*/

package com.wingtech.note.spannableparser;

import android.text.SpannableStringBuilder;

public abstract interface IMediaHandler {

    public abstract void handleContact(SpannableStringBuilder builder, int start, int end,
            String name,
            String phone);

    public abstract void handleImage(SpannableStringBuilder builder, int start, int end, String name);
    public abstract void handleStrike(SpannableStringBuilder builder, int start, int end);

}
