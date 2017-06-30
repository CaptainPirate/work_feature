/*===================================================================================================*	
 *  when  |      who     |    keyword           |        why         |         what                  *	
 *===================================================================================================*	
 *20160407|mengzhiming.wt|porting A1s enjoynotes| Port_EnjoyNotes | porting A1s enjoynotes to CMCC N2*	
*====================================================================================================*/

package com.wingtech.note.editor;

import java.util.Calendar;
import java.util.Locale;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
//import android.widget.NumberPicker;
//import android.widget.NumberPicker.Formatter;
//import android.widget.NumberPicker.OnValueChangeListener;
import com.wingtech.note.widget.wtNumberPicker;
import com.wingtech.note.widget.wtNumberPicker.Formatter;
import com.wingtech.note.widget.wtNumberPicker.OnValueChangeListener;
import com.wingtech.note.Utils;
import com.wingtech.note.R;

public class TimePicker extends LinearLayout {
    private static final int DAY_DISPLAY_CACHE = 5;
    private static final int DAY_DISPLAY_RANGE = 50;
    private static final int DAY_INITIALIZE_RANGE = 5;
    private static final int MINUTE_INTERVAL = 1;
    private int mDay;
    private String[] mDayDisplayedValues;
    private DayFormatter mDayFormatter;
    private wtNumberPicker mDayPicker;
    private int mHour;
    private wtNumberPicker mHourPicker;
    private OnTimeChangedListener mListener;
    private int mMinute;
    private String[] mMinuteDisplayedValues;
    private wtNumberPicker mMinutePicker;
    private int mPickerMinDay;
    private final Calendar mResult;
    private final int mStartDayOfYear;
    private final int mStartHour;
    private final int mStartMinute;
    private final long mStartTime;
    private final int mStartYear;
    private final Calendar mTempCal;

    public static abstract interface OnTimeChangedListener {
        public abstract void onTimeChanged(TimePicker paramTimePicker, long paramLong);
    }

    public TimePicker(Context context) {
        this(context, null, 0);
    }

    public TimePicker(Context context, AttributeSet attr) {
        this(context, attr, 0);
    }

    public TimePicker(Context context, AttributeSet attr, int defStyle) {
        super(context, attr, defStyle);
        ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(
                R.layout.timepicker, this, true);
        PickerValueChangeListener localPickerValueChangeListener = new PickerValueChangeListener();
        Calendar localCalendar = Calendar.getInstance();
        mResult = localCalendar;
        mTempCal = Calendar.getInstance();
        mTempCal.clear();
        localCalendar.add(Calendar.MINUTE, MINUTE_INTERVAL);
        mStartDayOfYear = localCalendar.get(Calendar.DAY_OF_YEAR);
        mStartHour = localCalendar.get(Calendar.HOUR_OF_DAY);
        mStartMinute = (localCalendar.get(Calendar.MINUTE) / MINUTE_INTERVAL);
        mStartYear = localCalendar.get(Calendar.YEAR);
        mStartTime = getTimeInMillis(mStartYear, mStartDayOfYear, mStartHour,
                MINUTE_INTERVAL * mStartMinute);
        mDayPicker = ((wtNumberPicker) findViewById(R.id.day));
        mDayPicker.setOnValueChangedListener((OnValueChangeListener) localPickerValueChangeListener);
        int i = mStartDayOfYear;
        mPickerMinDay = i;
        mDay = i;
        mDayFormatter = new DayFormatter();
        updateDayPicker(true);
        mHourPicker = ((wtNumberPicker) findViewById(R.id.hour));
        mHourPicker.setOnValueChangedListener((OnValueChangeListener) localPickerValueChangeListener);
        mHourPicker.setFormatter((Formatter) new TwoDigitFormatter());
        mHourPicker.setMaxValue(23);
        mHour = mStartHour;
        updateHourPicker();
        mMinutePicker = ((wtNumberPicker) findViewById(R.id.minute));
        mMinutePicker.setOnValueChangedListener((OnValueChangeListener) localPickerValueChangeListener);
        mMinutePicker.setMaxValue(59 / MINUTE_INTERVAL);
        mMinute = this.mStartMinute;
        updateMinutePicker();
    }

    // @param mode = true,�½�һ���߳�������飬mode = false,�ڱ��߳�ֱ�����
    private void updateDayPicker(boolean mode) {
        if ((mDayPicker.getDisplayedValues() != null)
                && (mDayPicker.getMinValue() == mPickerMinDay))
            // ֵ�ı䣬�������Сֵ
            if (mDay != mDayPicker.getValue()) {
                mDayPicker.setValue(mDay);
                mPickerMinDay = mDay - DAY_DISPLAY_RANGE / 2;
            }
        int len = mode ? DAY_INITIALIZE_RANGE : DAY_DISPLAY_RANGE;
        if ((mDayDisplayedValues == null) || (mDayDisplayedValues.length != len))
            mDayDisplayedValues = new String[len];
        for (int i = mPickerMinDay; i < len + mPickerMinDay; i++)
            mDayDisplayedValues[(i - mPickerMinDay)] = mDayFormatter
                    .format(i);
        if (mode)
            new DayFiller(mPickerMinDay, DAY_DISPLAY_RANGE).execute();
        updateDayPickerDisplay(mDayDisplayedValues);
    }

    private void updateDayPickerDisplay(String[] dispalyStr) {
        mDayPicker.setDisplayedValues(null);
        mDayPicker.setMinValue(mPickerMinDay);
        mDayPicker.setMaxValue(mPickerMinDay + dispalyStr.length - 1);
        mDayPicker.setWrapSelectorWheel(false);
        mDayDisplayedValues = dispalyStr;
        mDayPicker.setValue(mDay);
        mDayPicker.setDisplayedValues(mDayDisplayedValues);
        mDayPicker.invalidate();
    }

    private void updateHourPicker() {
        if (mStartDayOfYear == mDay)
            mHourPicker.setMinValue(mStartHour);
        else
            mHourPicker.setMinValue(0);
        mHourPicker.setValue(mHour);
    }

    private void updateMinutePicker() {
        int i;
        if ((mStartDayOfYear == mDay) && (mStartHour == mHour)) {
            i = mStartMinute;
        } else {
            i = 0;
        }
        mMinutePicker.setDisplayedValues(null);
        mMinutePicker.setMinValue(i);
        mMinuteDisplayedValues = new String[59 / MINUTE_INTERVAL + 1 - i];
        Object[] arg0 = new Object[1];
        for (int j = i; j < 59 / MINUTE_INTERVAL + 1; j++) {
            arg0[0] = j * MINUTE_INTERVAL;
            mMinuteDisplayedValues[(j - i)] = String.format("%02d", arg0);
        }
        mMinutePicker.setValue(mMinute);
        mMinutePicker.setDisplayedValues(mMinuteDisplayedValues);
        mMinutePicker.invalidate();
    }

    private void adjustHour() {
        if (mStartDayOfYear == mDay)
            mHour = Math.max(mHour, mStartHour);
    }

    private void adjustMinute() {
        if ((mStartDayOfYear == mDay) && (mStartHour == mHour))
            mMinute = Math.max(mMinute, mStartMinute);
    }

    protected void dispatchRestoreInstanceState(SparseArray<Parcelable> container) {
        dispatchThawSelfOnly(container);
    }

    protected Parcelable onSaveInstanceState() {
        return new SavedState(super.onSaveInstanceState(), getTimeInMillis());
    }

    protected void onRestoreInstanceState(Parcelable paramParcelable) {
        SavedState localSavedState = (SavedState) paramParcelable;
        super.onRestoreInstanceState(localSavedState.getSuperState());
        updateByMillis(localSavedState.getTimeInMillis());
    }

    public void setOnTimeChangedListener(OnTimeChangedListener listener) {
        mListener = listener;
    }

    private int getDayOff(Calendar calendar, long time) {
        calendar.setTimeInMillis(time);
        int year = calendar.get(Calendar.YEAR);
        int day = calendar.get(Calendar.DAY_OF_YEAR);
        int k = 0;
        calendar.clear();
        for (int i = mStartYear; i < year; i++) {
            calendar.set(i, 11, 31);// ����Ϊ12��31��
            k += calendar.get(Calendar.DAY_OF_YEAR);
        }
        return k + day - mStartDayOfYear;
    }

    private long getTimeInMillis(int year, int day, int hour, int minute) {
        mResult.clear();
        mResult.set(Calendar.YEAR, year);
        mResult.set(Calendar.DAY_OF_YEAR, day);
        mResult.set(Calendar.HOUR_OF_DAY, hour);
        mResult.set(Calendar.MINUTE, minute);
        return mResult.getTimeInMillis();
    }

    public long getTimeInMillis() {
        return getTimeInMillis(mStartYear, mDay, mHour, MINUTE_INTERVAL * mMinute);
    }

    public void updateByMillis(long time) {
        long v = Math.max(time, mStartTime);
        Calendar localCalendar = mTempCal;
        localCalendar.setTimeInMillis(v);
        mHour = localCalendar.get(Calendar.HOUR_OF_DAY);
        mMinute = (localCalendar.get(Calendar.MINUTE) / MINUTE_INTERVAL);
        mDay = (getDayOff(mTempCal, v) + mStartDayOfYear);
        adjustHour();
        adjustMinute();
        updateDayPicker(true);
        updateHourPicker();
        updateMinutePicker();
    }

	private class PickerValueChangeListener implements wtNumberPicker.OnValueChangeListener {
        private PickerValueChangeListener() {
        }

        private void notifyTimeChanged(TimePicker timePicker) {
            if (mListener != null)
                mListener.onTimeChanged(timePicker, getTimeInMillis());
        }

		public void onValueChange(wtNumberPicker numberPicker, int oldVal, int newVal) {
            if (numberPicker == mDayPicker) {
                mDay = newVal;
                adjustHour();
                adjustMinute();
                notifyTimeChanged(TimePicker.this);
                updateHourPicker();
                updateMinutePicker();
                if (mDayPicker.getMaxValue() - mDay < DAY_DISPLAY_CACHE
                        || (mDay - mDayPicker.getMinValue() < DAY_DISPLAY_CACHE)) {
                    mPickerMinDay =Math.max(mDay - DAY_DISPLAY_RANGE / 2, mStartDayOfYear) ;
                    updateDayPicker(false);
                }
            } else if (numberPicker == TimePicker.this.mHourPicker) {
                mHour = newVal;
                adjustMinute();
                notifyTimeChanged(TimePicker.this);
                updateMinutePicker();
            }
            else if (numberPicker == TimePicker.this.mMinutePicker) {
                mMinute = newVal;
                notifyTimeChanged(TimePicker.this);
            }

        }
    }

	private class DayFormatter implements wtNumberPicker.Formatter {

        private Calendar nCalendar = Calendar.getInstance();

        public DayFormatter() {
        }

        private String getRelativeFutureDateSpanString(long time) {
            int i = getDayOff(nCalendar, time);
            String outStr;
            if (i == 0)
                outStr = getContext().getString(R.string.timepicker_today);

            else if (i == 1)
                outStr = getContext().getString(R.string.timepicker_tomorrow);
            else if (Utils.isChineseLanguage())
            {
                String str2 = DateUtils.formatDateTime(getContext(),
                        time, DateUtils.FORMAT_ABBREV_MONTH | DateUtils.FORMAT_SHOW_DATE
                                | DateUtils.FORMAT_NO_YEAR);
                String str3 = DateUtils.formatDateTime(getContext(),
                        time, DateUtils.FORMAT_ABBREV_WEEKDAY | DateUtils.FORMAT_SHOW_WEEKDAY);
                outStr = str2.replace(" ", "") + " " + str3;
            }
            else
            {
                outStr = DateUtils.formatDateTime(getContext(),
                        time, DateUtils.FORMAT_ABBREV_MONTH | DateUtils.FORMAT_ABBREV_WEEKDAY
                                | DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NO_YEAR
                                | DateUtils.FORMAT_SHOW_WEEKDAY);
            }
            return outStr;
        }

        public String format(int dayOfYear) {
            nCalendar.clear();
            nCalendar.set(Calendar.YEAR, mStartYear);
            nCalendar.set(Calendar.DAY_OF_YEAR, dayOfYear);
            return getRelativeFutureDateSpanString(nCalendar.getTimeInMillis());
        }
    }

    private static class SavedState extends View.BaseSavedState {
        private long nTimeInMillis;

        public SavedState(Parcel superState) {
            super(superState);
            nTimeInMillis = superState.readLong();
        }

        public SavedState(Parcelable superState, long time) {
            super(superState);
            this.nTimeInMillis = time;
        }

        public long getTimeInMillis() {
            return nTimeInMillis;
        }

        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeLong(nTimeInMillis);
        }
    }

    public static final Parcelable.Creator<SavedState> CREATOR = new Creator<SavedState>() {
        public SavedState createFromParcel(Parcel in) {
            return new SavedState(in);
        }

        public SavedState[] newArray(int size) {
            return new SavedState[size];
        }
    };

    /**
     * Use a custom wtNumberPicker formatting callback to use two-digit minutes
     * strings like "01". Keeping a static formatter etc. is the most efficient
     * way to do this; it avoids creating temporary objects on every call to
     * format().
     */
    private static class TwoDigitFormatter implements wtNumberPicker.Formatter {
        final StringBuilder mBuilder = new StringBuilder();

        java.util.Formatter mFmt;

        final Object[] mArgs = new Object[1];

        TwoDigitFormatter() {
            final Locale locale = Locale.getDefault();
            init(locale);
        }

        private void init(Locale locale) {
            mFmt = createFormatter(locale);
        }

        public String format(int value) {
            mArgs[0] = value;
            mBuilder.delete(0, mBuilder.length());
            mFmt.format("%02d", mArgs);
            return mFmt.toString();
        }

        private java.util.Formatter createFormatter(Locale locale) {
            return new java.util.Formatter(mBuilder, locale);
        }
    }

    private class DayFiller extends AsyncTask<Void, Void, String[]> {
        private int nMinDay;
        private int nRange;

        public DayFiller(int minDay, int range) {
            nMinDay = minDay;
            nRange = range;
        }

        protected String[] doInBackground(Void... arg0) {
            String[] arrayOfString = new String[nRange];
            DayFormatter localDayFormatter = new DayFormatter();
            for (int i = nMinDay; i < nMinDay + nRange; i++)
                arrayOfString[(i - nMinDay)] = localDayFormatter.format(i);
            return arrayOfString;
        }

        protected void onPostExecute(String[] paramArrayOfString) {
            if (mPickerMinDay == nMinDay)
                updateDayPickerDisplay(paramArrayOfString);
        }

    }

}

