/*===================================================================================================*
 *  when  |      who     |    keyword           |        why         |         what                  *
 *===================================================================================================*
 *20160610|mengzhiming.wt|bug183570,add |bug183570,add | bug183570,add                                                                                                                      *
*====================================================================================================*/

package com.wingtech.note.editor;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.wingtech.note.Utils;
import com.wingtech.note.R;

import java.io.ObjectInputStream.GetField;
import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.Locale;

import android.app.DatePickerDialog.OnDateSetListener;
import android.app.TimePickerDialog;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.view.View.OnClickListener;
import android.app.DatePickerDialog;
import android.widget.DatePicker;
import android.widget.TimePicker;


public class DateTimePickerDialog extends AlertDialog implements
        DialogInterface.OnClickListener/*,DialogInterface.OnCancelListener, OnItemSelectedListener,
        RecurrencePickerDialog.OnRecurrenceSetListener,
        TimeZonePickerDialog.OnTimeZoneSetListener*/ {
    private static final int SHOW_DESCRIPTION_DELAYED = 500;
    private TextView mDateSet;
    private TextView mTimeSet;
    private Handler mHandler;
    private OnDateTimeSetListener mDateTimeListener;
    private final Calendar mResult;
    private int mStartDayOfYear;
    private int mStartHour;
    private int mStartMinute;
    private long mStartTime;
    private int mStartYear;
    private int mMonth;
    private int mDay;
    private final Time mSetDateTime;
    private final Calendar mTempCal;
    private static final int MINUTE_INTERVAL = 1;
    private TimePickerDialog mTimePickerDialog;
    private DatePickerDialog mDatePickerDialog;

    private String TAG = "DateTimePickerDialog";
    //private View mView;
    private Activity mActivity;
    LinearLayout mDateParent;
    LinearLayout mTimeParent;


    public DateTimePickerDialog(Activity act, OnDateTimeSetListener listener) {
        super(act);
        Context context = act;
        mActivity = act;
        mDateTimeListener = listener;
        setTitle(R.string.timepicker_dialog_title);
        //bug199000,mengzhiming.wt,modified,20160720
        setButton(DialogInterface.BUTTON_POSITIVE, context.getText(R.string.note_alert_ok), this);
        setButton(DialogInterface.BUTTON_NEGATIVE, context.getText(android.R.string.cancel),
                (OnClickListener) null);
        View localView = ((LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(
                R.layout.date_time_picker_dialog, null);
        setView(localView);
        mDateParent = (LinearLayout) localView.findViewById(R.id.start_date_row);
        mTimeParent = (LinearLayout) localView.findViewById(R.id.start_time_row);
        mDateSet = ((TextView) localView.findViewById(R.id.date_set));
        mTimeSet = ((TextView) localView.findViewById(R.id.time_set));
        mDateSet.setTextColor(0xffa58b54);//bug199000,mengzhiming.wt,modified,20160720
        mTimeSet.setTextColor(0xff3c3c3c);//bug199000,mengzhiming.wt,modified,20160720

        mDateParent.setOnClickListener(new DateClickListener());
        mTimeParent.setOnClickListener(new TimeClickListener());


        Calendar localCalendar = Calendar.getInstance();
        mResult = localCalendar;
        mTempCal = Calendar.getInstance();
        mTempCal.clear();
        localCalendar.add(Calendar.MINUTE, MINUTE_INTERVAL);
        mStartDayOfYear = localCalendar.get(Calendar.DAY_OF_YEAR);
        mStartHour = localCalendar.get(Calendar.HOUR_OF_DAY);
        mStartMinute = (localCalendar.get(Calendar.MINUTE) / MINUTE_INTERVAL);
        mStartYear = localCalendar.get(Calendar.YEAR);
        mMonth = localCalendar.get(Calendar.MONTH);
        mDay = localCalendar.get(Calendar.DAY_OF_MONTH);
        mStartTime = getTimeInMillis(mStartYear, mStartDayOfYear, mStartHour,
                MINUTE_INTERVAL * mStartMinute);

        mSetDateTime = new Time();
        mSetDateTime.year = mStartYear;
        mSetDateTime.month = mMonth;
        mSetDateTime.monthDay = mDay;
        mSetDateTime.hour = mStartHour;
        mSetDateTime.minute = mStartMinute;


        mHandler = new Handler() {
            public void handleMessage(Message message) {
                String str1 = DateUtils.formatDateTime(getContext(), mStartTime,
                        DateUtils.LENGTH_MEDIUM);
                String str2 = DateUtils.formatDateTime(getContext(), mStartTime,
                        DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_24HOUR);
                mDateSet.setText(str1);
                mTimeSet.setText(str2);
            }
        };

        updateDescriptionDelayed(0);
    }

    private void updateDescriptionDelayed(int delayMillies) {
        mHandler.removeMessages(0);
        mHandler.sendEmptyMessageDelayed(0, delayMillies);
    }

    public static abstract interface OnDateTimeSetListener {
        public abstract void onDateTimeSet(DateTimePickerDialog paramDateTimePickerDialog, long paramLong);
    }

    public void onClick(DialogInterface dialog, int which) {
        if (mDateTimeListener != null) {
            mDateTimeListener.onDateTimeSet(this, mStartTime);
        }
    }

    public void update(long timeInMillis) {
        mStartTime = timeInMillis;
        updateByMillis(mStartTime);
        updateDescriptionDelayed(20);
    }

    public void updateByMillis(long time) {
        Calendar localCalendar = mTempCal;
        localCalendar.setTimeInMillis(time);
        mStartDayOfYear = localCalendar.get(Calendar.DAY_OF_YEAR);
        mMonth = localCalendar.get(Calendar.MONTH);
        mDay = localCalendar.get(Calendar.DAY_OF_MONTH);
        mStartHour = localCalendar.get(Calendar.HOUR_OF_DAY);
        mStartMinute = (localCalendar.get(Calendar.MINUTE) / MINUTE_INTERVAL);
        mSetDateTime.year = mStartYear;
        mSetDateTime.month = mMonth;
        mSetDateTime.monthDay = mDay;
        mSetDateTime.hour = mStartHour;
        mSetDateTime.minute = mStartMinute;
    }

    private long getTimeInMillis(int year, int day, int hour, int minute) {
        mResult.clear();
        mResult.set(Calendar.YEAR, year);
        mResult.set(Calendar.DAY_OF_YEAR, day);
        mResult.set(Calendar.HOUR_OF_DAY, hour);
        mResult.set(Calendar.MINUTE, minute);
        return mResult.getTimeInMillis();
    }


    private class DateListener implements OnDateSetListener {
        View mView;

        public DateListener(View view) {
            mView = view;
        }

        @Override
        public void onDateSet(DatePicker view, int year, int month, int monthDay) {
            Log.d(TAG, "onDateSet: " + year + " " + month + " " + monthDay);

            if (mView == mDateParent) {

                mSetDateTime.year = year;
                mSetDateTime.month = month;
                mSetDateTime.monthDay = monthDay;
                mStartTime = mSetDateTime.normalize(true);
                mDateSet.setTextColor(0xff3c3c3c);//bug199000,mengzhiming.wt,modified,20160720
                mTimeSet.setTextColor(0xffa58b54);//bug199000,mengzhiming.wt,modified,20160720
                mHandler.sendEmptyMessageDelayed(0, 0);

            }
        }
    }

    private class TimeListener implements OnTimeSetListener {
        private View mView;

        public TimeListener(View view) {
            mView = view;
        }

        @Override
        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            Log.d(TAG, "onTimeSet: " + hourOfDay + " " + minute);

            if (mView == mTimeParent) {
                mSetDateTime.hour = hourOfDay;
                mSetDateTime.minute = minute;
                mStartTime = mSetDateTime.normalize(true);
                mHandler.sendEmptyMessageDelayed(0, 0);

            }

        }
    }

    private class DateClickListener implements View.OnClickListener {
        private Time mTime;

        @Override
        public void onClick(View v) {
            if (v == mDateParent) {

                final DateListener listener = new DateListener(v);
                if (mDatePickerDialog != null) {
                    mDatePickerDialog.dismiss();
                }
                mDatePickerDialog = new DatePickerDialog(mActivity, listener, mStartYear, mMonth, mDay);
                Utils.configureDatePicker(mDatePickerDialog.getDatePicker());
                mDatePickerDialog.show();
            }
        }
    }

    private class TimeClickListener implements View.OnClickListener {
        private Time mTime;

        @Override
        public void onClick(View v) {
            if (v == mTimeParent) {
                mTimePickerDialog = new TimePickerDialog(mActivity, new TimeListener(v), mStartHour, mStartMinute, DateFormat.is24HourFormat(mActivity));
                mTimePickerDialog.show();
            }
        }
    }

}
