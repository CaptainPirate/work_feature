/*===================================================================================================*	
 *  when  |      who     |    keyword           |        why         |         what                  *	
 *===================================================================================================*	
 *20160407|mengzhiming.wt|porting A1s enjoynotes| Port_EnjoyNotes | porting A1s enjoynotes to CMCC N2*	
*====================================================================================================*/

package com.wingtech.note.data;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Contacts.Data;
import android.provider.ContactsContract.Profile;
import android.provider.ContactsContract.RawContacts;

import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;

import com.wingtech.note.Utils;
import com.wingtech.note.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;

public class Contact {
    private static final String TAG = "Contact";
    private static final int CONTACT_COLUMN_ID_INDEX = 0;
    private static final int CONTACT_COLUMN_NUMBER_INDEX = 1;
    private static final int CONTACT_COLUMN_DISPLAY_NAME_INDEX = 2;
    private static final String[] CONTACT_PROJECTION = {
            "_id", "data1", "display_name"
    };
    private static final int IDENTITY_COLUMN_LOOKUP_KEY_INDEX = 0;
    private static final int IDENTITY_COLUMN_CONTACT_ID_INDEX = 1;

    private static HashMap<String, String> sContactCache;
    private static HashMap<String, String> sContactKeyMap;
    private static Locale sLocale;
    private String mName;
    private String mPhoneNumber;

    private static final String[] IDENTITY_PROJECTION = {
            "lookup", "contact_id"
    };
    private static final String[] CONTACTS_PROJECTION = new String[] {
            Contacts._ID, // 0
            Contacts.DISPLAY_NAME_PRIMARY, // 1
            Contacts.DISPLAY_NAME_ALTERNATIVE, // 2
            Contacts.SORT_KEY_PRIMARY, // 3
            Contacts.DISPLAY_NAME, // 4
    };
    private static final int PHONE_ID_COLUMN_INDEX = 0;

    public Contact(String name, String phoneNumber) {
        mName = name;
        mPhoneNumber = phoneNumber;
    }

    private static final String CALLER_ID_SELECTION = "PHONE_NUMBERS_EQUAL(" + Phone.NUMBER
            + ",?) AND " + Data.MIMETYPE + "='" + Phone.CONTENT_ITEM_TYPE + "'"
            + " AND " + Data.RAW_CONTACT_ID + " IN "
            + "(SELECT raw_contact_id "
            + " FROM phone_lookup"
            + " WHERE min_match = '+')";

    public static String getContact(Context context, String phoneNumber) {
        if (sContactCache == null) {
            sContactCache = new HashMap<String, String>();
        }

        if (sContactCache.containsKey(phoneNumber)) {
            return sContactCache.get(phoneNumber);
        }

        String selection = CALLER_ID_SELECTION.replace("+",
                PhoneNumberUtils.toCallerIDMinMatch(phoneNumber));
        Cursor cursor = context.getContentResolver().query(
                ContactsContract.Data.CONTENT_URI,
                new String[] {
                    Phone.DISPLAY_NAME
                },
                selection,
                new String[] {
                    phoneNumber
                },
                null);
        try {
            if (cursor != null && cursor.moveToFirst()) {

                String name = cursor.getString(0);
                sContactCache.put(phoneNumber, name);
                return name;

            } else {
                Log.d(TAG, "No contact matched with number:" + phoneNumber);
                return null;
            }
        } catch (Exception e) {
            // TODO: handle exception
            return null;
        } finally {
            if (cursor != null)
                cursor.close();
        }

    }

    public static ArrayList<Contact> getContactListByUris(Context paramContext,
            Uri[] uris) {
        if ((uris == null) || (uris.length == 0))
            return null;
        ArrayList<Long> ids = new ArrayList<Long>(uris.length);
        for (int j = 0; j < uris.length; j++)
            ids
                    .add(Long.valueOf(Long.parseLong(uris[j].getLastPathSegment())));
        ArrayList<Contact> contacts = new ArrayList<Contact>(uris.length);
        String[] protections = new String[] {
                "_id", Utils.congregateAsString(ids, ",")
        };
        String str = String.format("%s IN (%s)", (Object[]) protections);
        Cursor localCursor = paramContext.getContentResolver().query(
                ContactsContract.Data.CONTENT_URI, CONTACT_PROJECTION, str, null, null);
        if (localCursor != null)
            try {
                if (localCursor.moveToNext())
                    contacts.add(new Contact(localCursor
                            .getString(CONTACT_COLUMN_DISPLAY_NAME_INDEX), localCursor
                            .getString(CONTACT_COLUMN_NUMBER_INDEX)));
            } finally {
                localCursor.close();
            }
        return contacts;
    }

    public static Contact getContactListByUri(Context paramContext,
            Uri uri) {
        if (uri == null)
            return null;
        Long id = Long.valueOf(Long.parseLong(uri.getLastPathSegment()));
        Contact contact = null;
        String[] protections = new String[] {
                "_id", Long.toString(id)
        };
        String str = String.format("%s IN (%s)", (Object[]) protections);
        Cursor localCursor = paramContext.getContentResolver().query(
                ContactsContract.Data.CONTENT_URI, CONTACT_PROJECTION, str, null, null);
        if (localCursor != null)
            try {
                if (localCursor.moveToNext())
                    contact = new Contact(localCursor
                            .getString(CONTACT_COLUMN_DISPLAY_NAME_INDEX), localCursor
                            .getString(CONTACT_COLUMN_NUMBER_INDEX));
            } finally {
                localCursor.close();
            }
        return contact;
    }

    public String getName() {
        return mName;
    }

    public String getPhoneNumber() {
        return mPhoneNumber;
    }

    private static Cursor getContactCursor(Context context, String name, String phoneNumber) {
        Object[] arrayOfObject = new Object[2];
        arrayOfObject[0] = "display_name";
        arrayOfObject[1] = "data1";
        String str = String.format(Locale.US, "%s=? AND PHONE_NUMBERS_EQUAL(%s,?)", arrayOfObject);
        String[] arrayOfString = new String[] {
                name, phoneNumber
        };
        return context.getContentResolver().query(ContactsContract.Data.CONTENT_URI,
                IDENTITY_PROJECTION, str, arrayOfString, null);
    }

    public static Uri getContactUri(Context context, String name, String phoneNumber) {
        Cursor cursor = getContactCursor(context, name, phoneNumber);
        Uri uri = null;
        if (cursor != null) {
            try {
                if (cursor.moveToNext()) {
                    String str = cursor.getString(0);
                    long l = cursor.getLong(1);
                    uri = ContactsContract.Contacts.CONTENT_LOOKUP_URI.buildUpon().appendPath(str)
                            .appendPath(Long.toString(l)).build();
                }
            } catch (Exception e) {
                // TODO: handle exception
            } finally {
                if (cursor != null)
                    cursor.close();
            }
        }
        return uri;
    }

    public static String parseContactTextFromIntent(Context context, Intent intent) {
        if (!Contacts.CONTENT_VCARD_TYPE.equals(intent.getType()))
            return null;
        StringBuilder contactsID = new StringBuilder();
        int curIndex = 0;
        Cursor cursor = null;
        String id = null;
        String textVCard = "";
        String contactId = intent.getStringExtra("contactId");
        int singleContactId = -1;
        final Uri extraUri = (Uri) intent.getExtras().get(Intent.EXTRA_STREAM);
        String lookupUri = null;
        if (null != extraUri) {
            lookupUri = extraUri.getLastPathSegment();
        }
        if (extraUri != null) {
            String userProfile = intent.getStringExtra("userProfile");
            boolean mUserProfile = false;
            if (userProfile != null && "true".equals(userProfile)) {
                mUserProfile = true;
            }
            if (contactId != null && !"".equals(contactId)) {
                singleContactId = Integer.parseInt(contactId);
            }
            Uri shareUri = intent.getData();
            if (mUserProfile) {
                cursor = context.getContentResolver().query(
                        Profile.CONTENT_URI.buildUpon().appendPath("data").build(),
                        new String[] {
                                RawContacts.CONTACT_ID, Data.MIMETYPE, Data.DATA1
                        }, null, null, null);
                if (cursor != null) {
                    textVCard = getVCardString(cursor, context);
                    cursor.close();
                }
            } else {
                if (singleContactId == -1) {
                    String[] tempUris = lookupUri.split(":");
                    StringBuilder selection = new StringBuilder(Contacts.LOOKUP_KEY
                            + " in (");
                    int index = 0;
                    for (int i = 0; i < tempUris.length; i++) {
                        selection.append("'" + tempUris[i] + "'");
                        if (index != tempUris.length - 1) {
                            selection.append(",");
                        }
                        index++;
                    }

                    selection.append(")");
                    cursor = context.getContentResolver().query(
                            /* dataUri */Contacts.CONTENT_URI, CONTACTS_PROJECTION,
                            selection.toString(), null, Contacts.SORT_KEY_PRIMARY);
                    Log.i(TAG, "cursor is " + cursor);
                    if (null != cursor) {
                        while (cursor.moveToNext()) {
                            if (cursor != null) {
                                id = cursor.getString(PHONE_ID_COLUMN_INDEX);
                            }
                            if (curIndex++ != 0) {
                                contactsID.append("," + id);
                            } else {
                                contactsID.append(id);
                            }
                        }
                        cursor.close();
                    }
                } else {
                    id = Integer.toString(singleContactId);
                    contactsID.append(id);
                }
                long[] contactsIds = null;
                if (contactsID.toString() != null && !contactsID.toString().equals("")) {
                    String[] vCardConIds = contactsID.toString().split(",");
                    contactsIds = new long[vCardConIds.length];
                    try {
                        for (int i = 0; i < vCardConIds.length; i++) {
                            contactsIds[i] = Long.parseLong(vCardConIds[i]);
                        }
                    } catch (NumberFormatException e) {
                        contactsIds = null;
                    }
                }
                if (contactsIds != null && contactsIds.length > 0) {
                    Log.d(TAG, "contactsIds.length() = "
                            + contactsIds.length);
                    StringBuilder sb = new StringBuilder("");
                    for (long sContactId : contactsIds) {
                        if (sContactId == contactsIds[contactsIds.length - 1]) {
                            sb.append(sContactId);
                        } else {
                            sb.append(sContactId + ",");
                        }
                    }
                    String selection = RawContacts.CONTACT_ID + " in (" + sb.toString() + ")";

                    Log.d(TAG, "addTextVCard(): selection = " + selection);
                    Uri shareDataUri = Uri.parse("content://com.android.contacts/data");
                    Log.d(TAG, "Before query to build contact name and number string ");
                    Cursor c = context.getContentResolver()
                            .query(
                                    shareDataUri, // URI
                                    new String[] {
                                            RawContacts.CONTACT_ID, Data.MIMETYPE,
                                            Data.DATA1
                                    }, // projection
                                    selection, // selection
                                    null, // selection args
                                    Contacts.SORT_KEY_PRIMARY + " , " + RawContacts.CONTACT_ID); // sortOrder
                    Log.d(TAG, "After query to build contact name and number string ");
                    if (c != null) {
                        Log.d(TAG, "Before getVCardString ");
                        textVCard = getVCardString(c, context);
                        Log.d(TAG, "After getVCardString ");
                        c.close();
                    }
                }
            }
        }
        return textVCard;
    }

    private static String getVCardString(Cursor cursor, Context context) {
        final int dataContactId = 0;
        final int dataMimeType = 1;
        final int dataString = 2;
        long contactId = 0l;
        long contactCurrentId = 0l;
        int i = 1;
        String mimeType;
        TextVCardContact tvc = new TextVCardContact(context);
        StringBuilder vcards = new StringBuilder();
        while (cursor.moveToNext()) {
            contactId = cursor.getLong(dataContactId);
            mimeType = cursor.getString(dataMimeType);
            if (contactCurrentId == 0l) {
                contactCurrentId = contactId;
            }
            if (contactId != contactCurrentId) {
                contactCurrentId = contactId;
                vcards.append(tvc.toString());
                tvc.reset();
            }

            if (CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
                    .equals(mimeType)) {
                tvc.mName = cursor.getString(dataString);
            } else if (CommonDataKinds.Phone.CONTENT_ITEM_TYPE.equals(mimeType)) {
                tvc.mNumbers.add(cursor.getString(dataString));
            } else if (CommonDataKinds.Email.CONTENT_ITEM_TYPE.equals(mimeType)) {
                tvc.mOmails.add(cursor.getString(dataString));
            } else if (CommonDataKinds.Organization.CONTENT_ITEM_TYPE.equals(mimeType)) {
                tvc.mOrganizations.add(cursor.getString(dataString));
            } else if (CommonDataKinds.Im.CONTENT_ITEM_TYPE.equals(mimeType)) {
                tvc.mIMs.add(cursor.getString(dataString));
            } else if (CommonDataKinds.Nickname.CONTENT_ITEM_TYPE.equals(mimeType)) {
                tvc.mNickname = cursor.getString(dataString);
            } else if (CommonDataKinds.Note.CONTENT_ITEM_TYPE.equals(mimeType)) {
                tvc.mNote = cursor.getString(dataString);
            } else if (CommonDataKinds.Relation.CONTENT_ITEM_TYPE.equals(mimeType)) {
                tvc.mOrganizations.add(cursor.getString(dataString));
            } else if (CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE.equals(mimeType)) {
                tvc.mAddresses.add(cursor.getString(dataString));
            } else if (CommonDataKinds.Website.CONTENT_ITEM_TYPE.equals(mimeType)) {
                tvc.mWebsites.add(cursor.getString(dataString));
            }
            if (cursor.isLast()) {
                vcards.append(tvc.toString());
            }
        }
        return vcards.toString();
    }

}

