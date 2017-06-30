/*===================================================================================================*	
 *  when  |      who     |    keyword           |        why         |         what                  *	
 *===================================================================================================*	
 *20160407|mengzhiming.wt|porting A1s enjoynotes| Port_EnjoyNotes | porting A1s enjoynotes to CMCC N2*	
*====================================================================================================*/

package com.wingtech.note.data;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;
import com.wingtech.note.R;

public class TextVCardContact {
    private Context mContext;
    protected String mName = "";
    protected List<String> mNumbers = new ArrayList<String>();
    protected List<String> mOmails = new ArrayList<String>();
    protected List<String> mOrganizations = new ArrayList<String>();
    protected String mNickname = "";
    protected List<String> mAddresses = new ArrayList<String>();
    protected String mNote = "";
    protected List<String> mWebsites = new ArrayList<String>();
    protected List<String> mIMs = new ArrayList<String>();

    public TextVCardContact(Context context) {
        mContext = context;
    }

    protected void reset() {
        mName = "";
        mNumbers.clear();
        mOmails.clear();
        mOrganizations.clear();
    }

    @Override
    public String toString() {
        String textVCardString = "";
        int i = 1;
        if (mName != null && !mName.equals("")) {
            textVCardString += mContext.getString(R.string.nameLabelsGroup) + ": "
                    + mName + "\n";
        }
        if (mNickname != null && !mNickname.equals("")) {
            textVCardString += mContext.getString(R.string.vcard_nickname) + ": "
                    + mNickname + "\n";
        }
        if (!mNumbers.isEmpty()) {
            if (mNumbers.size() > 1) {
                i = 1;
                for (String number : mNumbers) {
                	textVCardString += mContext.getString(R.string.phoneLabelsGroup)  + i + ": " + number + "\n";
                    i++;
                }
            } else {
            	textVCardString += mContext.getString(R.string.phoneLabelsGroup)  + ": " + mNumbers.get(0) + "\n";
            }
        }
        if (!mOmails.isEmpty()) {
            if (mOmails.size() > 1) {
                i = 1;
                for (String email : mOmails) {
                    textVCardString += mContext.getString(R.string.email_other) + i
                            + ": " + email + "\n";
                    i++;
                }
            } else {
                textVCardString += mContext.getString(R.string.email_other) + ": "
                        + mOmails.get(0) + "\n";
            }
        }
        if (!mOrganizations.isEmpty()) {
            if (mOrganizations.size() > 1) {
                i = 1;
                for (String organization : mOrganizations) {
                    textVCardString += mContext.getString(R.string.organizationLabelsGroup)
                            + i + ": " + organization + "\n";
                    i++;
                }
            } else {
                textVCardString += mContext.getString(R.string.organizationLabelsGroup)
                        + ": " + mOrganizations.get(0) + "\n";
            }
        }
        if (!mIMs.isEmpty()) {
            if (mIMs.size() > 1) {
                i = 1;
                for (String im : mIMs) {
                    textVCardString += mContext.getString(R.string.vcard_im)
                            + i + ": " + im + "\n";
                    i++;
                }
            } else {
                textVCardString += mContext.getString(R.string.vcard_im)
                        + ": " + mIMs.get(0) + "\n";
            }
        }      
        if (!mAddresses.isEmpty()) {
            if (mAddresses.size() > 1) {
                i = 1;
                for (String address : mAddresses) {
                    textVCardString += mContext.getString(R.string.vcard_address)
                            + i + ": " + address + "\n";
                    i++;
                }
            } else {
                textVCardString += mContext.getString(R.string.vcard_address)
                        + ": " + mAddresses.get(0) + "\n";
            }
        }  
        if (mNote != null && !mNote.equals("")) {
            textVCardString += mContext.getString(R.string.vcard_note) + ": "
                    + mNote + "\n";
        }
        if (!mWebsites.isEmpty()) {
            if (mWebsites.size() > 1) {
                i = 1;
                for (String address : mWebsites) {
                    textVCardString += mContext.getString(R.string.vcard_website)
                            + i + ": " + address + "\n";
                    i++;
                }
            } else {
                textVCardString += mContext.getString(R.string.vcard_website)
                        + ": " + mWebsites.get(0) + "\n";
            }
        }  
        return textVCardString;
    }
}
