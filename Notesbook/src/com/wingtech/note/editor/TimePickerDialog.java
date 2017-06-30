/*===================================================================================================*	
 *  when  |      who     |    keyword           |        why         |         what                  *	
 *===================================================================================================*	
 *20160407|mengzhiming.wt|porting A1s enjoynotes| Port_EnjoyNotes | porting A1s enjoynotes to CMCC N2*	
*====================================================================================================*/

package com.wingtech.note.editor;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.wingtech.note.Utils;
import com.wingtech.note.R;

import java.io.ObjectInputStream.GetField;
import java.lang.reflect.Field;

public class TimePickerDialog extends AlertDialog implements DialogInterface.OnClickListener,
        TimePicker.OnTimeChangedListener {
    private static final int SHOW_DESCRIPTION_DELAYED = 500;
    private TextView mDescription;
    private Handler mHandler;
    private long mTimeInMillis;
    private OnTimeSetListener mTimeListener;
    private TimePicker mTimePicker;

    public TimePickerDialog(Context context, OnTimeSetListener listener) {
        super(context);
        mTimeListener = listener;
        mHandler = new Handler() {
            public void handleMessage(Message message) {
                if (Utils.isChineseLanguage()) {
                    String str1 = DateUtils.formatDateTime(getContext(), mTimeInMillis,
                            DateUtils.LENGTH_MEDIUM);
                    String str2 = DateUtils.formatDateTime(getContext(), mTimeInMillis,
                            DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_24HOUR
                                    | DateUtils.FORMAT_SHOW_WEEKDAY);
                    mDescription.setText(str1.replace(" ", "") + " " + str2);
                } else {
                    String str3 = DateUtils.formatDateTime(getContext(), mTimeInMillis,
                            DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_SHOW_DATE
                                    | DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_WEEKDAY
                                    | DateUtils.FORMAT_24HOUR);
                    mDescription.setText(str3);
                }
            }
        };
//        try {
//            Field mAlert = AlertDialog.class.getDeclaredField("mAlert");
//            mAlert.setAccessible(true);
//            Object alertController = mAlert.get(this);
//            Field mButtonPositive = alertController.getClass().getDeclaredField("mButtonPositive");
//            mButtonPositive.setAccessible(true);
//            Button positivebutton = (Button) mButtonPositive.get(mButtonPositive);
//            positivebutton.setTextColor(Color.BLACK);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        View titleView = ((LayoutInflater) context
//                .getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(
//                R.layout.dialog_title, null);
//        setCustomTitle(titleView);
        setTitle(R.string.timepicker_dialog_title);
        setButton(DialogInterface.BUTTON_POSITIVE, context.getText(android.R.string.ok), this);
        setButton(DialogInterface.BUTTON_NEGATIVE, context.getText(android.R.string.cancel),
                (OnClickListener) null);
        View localView = ((LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(
                R.layout.timepicker_dialog, null);
        setView(localView);
        mDescription = ((TextView) localView.findViewById(R.id.description));
        mTimePicker = ((TimePicker) localView.findViewById(R.id.timePicker));
        mTimePicker.setOnTimeChangedListener(this);
        mTimeInMillis = this.mTimePicker.getTimeInMillis();
        updateDescriptionDelayed(0);
    }

    private void updateDescriptionDelayed(int delayMillies) {
        mHandler.removeMessages(0);
        mHandler.sendEmptyMessageDelayed(0, delayMillies);
    }

    public static abstract interface OnTimeSetListener {
        public abstract void onTimeSet(TimePickerDialog paramTimePickerDialog, long paramLong);
    }

    public void onTimeChanged(TimePicker paramTimePicker, long timeInMillis) {
        mTimeInMillis = timeInMillis;
        updateDescriptionDelayed(500);
    }

    public void onClick(DialogInterface dialog, int which) {
        if (mTimeListener != null)
            mTimeListener.onTimeSet(this, mTimeInMillis);
    }

    public void onRestoreInstanceState(Bundle paramBundle) {
        super.onRestoreInstanceState(paramBundle);
        mTimeInMillis = mTimePicker.getTimeInMillis();
        updateDescriptionDelayed(0);
    }

    public void update(long timeInMillis) {
        mTimePicker.updateByMillis(timeInMillis);
        mTimeInMillis = mTimePicker.getTimeInMillis();
        updateDescriptionDelayed(0);
    }
}
