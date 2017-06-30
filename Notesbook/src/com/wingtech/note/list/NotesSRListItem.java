/*===================================================================================================*
 *  when  |      who     |    keyword           |        why         |         what                  *
 *===================================================================================================*
 *20160407|mengzhiming.wt|porting A1s enjoynotes| Port_EnjoyNotes | porting A1s enjoynotes to CMCC N2*
*====================================================================================================*
*20160531|mengzhiming.wt|   customer req       | customer req    | customer req                      *
 *===================================================================================================*/

package com.wingtech.note.list;

import android.content.Context;
import android.graphics.Color;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.wingtech.note.R;
import com.wingtech.note.ResourceParser;
import com.wingtech.note.Utils;
import com.wingtech.note.spannableparser.SpanUtils;
import android.widget.CheckBox;
import android.util.Log;
import android.media.ThumbnailUtils;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap;
import com.wingtech.note.AttachmentUtils;

import java.util.ArrayList;
import android.widget.LinearLayout;


public class NotesSRListItem extends FrameLayout implements INotesListItem {
    private NoteItemData mItemData;

    private ImageView mAlertIcon;
    private ImageView mImageIcon;
    private View mStick;
    private TextView mTime;
    private SnippetView mContent;
    private View mListView;
    //private View mOther;
    private CheckBox mCheckBox;
    private LinearLayout mTextDisplay;

    public NotesSRListItem(Context paramContext) {
        this(paramContext, null);
    }

    public NotesSRListItem(Context paramContext, AttributeSet paramAttributeSet) {
        this(paramContext, paramAttributeSet, 0);
    }

    public NotesSRListItem(Context paramContext, AttributeSet paramAttributeSet,
            int paramInt) {
        super(paramContext, paramAttributeSet, paramInt);
    }

     public void setSelectedBackGroud(boolean selected) {
        if (selected) {
            mCheckBox.setChecked(true);
            mCheckBox.setBackgroundDrawable(null);
        } else {
            mCheckBox.setChecked(false);
            mCheckBox.setBackgroundDrawable(null);
        }
     }

    public void bind(Context context, NoteItemData noteItemData, String string, boolean mode,
            boolean checked) {
        // TODO Auto-generated method stub
        mItemData = noteItemData;

        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams)mTextDisplay.getLayoutParams();
        if(mode){
            mCheckBox.setVisibility(VISIBLE);

            lp.weight = 18;
        }else{
            mCheckBox.setVisibility(GONE);
            lp.weight = 21;
        }
        mTextDisplay.setLayoutParams(lp);


        CharSequence content = SpanUtils.normalizeSnippet(noteItemData.getSnippet());
        if (TextUtils.isEmpty(string)) {
            mContent.setText(content, "");
        } else
            mContent.setText(content, string);
        mTime.setVisibility(VISIBLE);
        mTime.setText(Utils.formatTime(noteItemData.getModifiedDate(),null));
        mTime.setTextSize(12);
        if (noteItemData.hasAlert()) {
            mAlertIcon.setVisibility(VISIBLE);
        } else
            mAlertIcon.setVisibility(GONE);
        if (SpanUtils.findImageInSnippet(noteItemData.getSnippet())) {
            mImageIcon.setVisibility(VISIBLE);
            ArrayList<String> names = SpanUtils.collectImageNames(noteItemData.getSnippet());
            if (names != null) {
                mImageIcon.setVisibility(VISIBLE);
                for (String str : names) {
                    String path = AttachmentUtils.getAttachmentPath(getContext(), str);
                    mImageIcon.setImageBitmap(getImageThumbnail(path, 64, 64));
                    break;
                }
            }
        } else {
            mImageIcon.setVisibility(GONE);
        }

        if (noteItemData.getStickDate() > 0) {
            mStick.setVisibility(VISIBLE);
        } else
            mStick.setVisibility(GONE);
        setBackgroundColor(Color.TRANSPARENT);

        /*mListView.setBackgroundResource(ResourceParser.NoteSRItemBgResources
                                .getOtherBgRes(noteItemData
                                        .getBgColorId()));*/

        //bug198629,mengzhiming.wt,del,20160718,start
        /*mListView.setBackgroundColor(getResources().getColor(
                ResourceParser.NoteSRItemBgResources.getOtherBg(noteItemData
                                        .getBgColorId())));
        */
        //bug198629,mengzhiming.wt,del,20160718,end

    }


    public CheckBox getCheckBox() {
        return mCheckBox;
    }

    public NoteItemData getItemData() {
        // TODO Auto-generated method stub
        return mItemData;
    }

    public void onRecycle() {
        // TODO Auto-generated method stub

    }

    @Override
    protected void onFinishInflate() {
        // TODO Auto-generated method stub
        super.onFinishInflate();
        mTextDisplay = (LinearLayout)findViewById(R.id.text_display) ;
        mTime = (TextView) findViewById(R.id.tv_time);
        mContent = (SnippetView) findViewById(R.id.tv_content);
        mAlertIcon = (ImageView) findViewById(R.id.iv_alert_icon);
        mImageIcon = (ImageView) findViewById(R.id.thumbnail);// iv_image_icon
        mStick = findViewById(R.id.stick);
        //mOther = findViewById(R.id.tv_others);

        mListView=(View)findViewById(R.id.list_item);
        mCheckBox=(CheckBox)findViewById(R.id.check_box_select);
    }



    private Bitmap getImageThumbnail(String imagePath, int width, int height) {
        Bitmap bitmap = null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        bitmap = BitmapFactory.decodeFile(imagePath, options);
        options.inJustDecodeBounds = false;
        int h = options.outHeight;
        int w = options.outWidth;
        int beWidth = w / width;
        int beHeight = h / height;
        int be = 1;
        if (beWidth < beHeight) {
            be = beWidth;
        } else {
            be = beHeight;
        }
        if (be <= 0) {
            be = 1;
        }
        options.inSampleSize = be;

        bitmap = BitmapFactory.decodeFile(imagePath, options);
        bitmap = ThumbnailUtils.extractThumbnail(bitmap, width, height,
                ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
        return bitmap;
    }

}
