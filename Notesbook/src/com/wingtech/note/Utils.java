/*===================================================================================================*
 *  when  |      who     |    keyword           |        why         |         what                  *
 *===================================================================================================*
 *20160407|mengzhiming.wt|porting A1s enjoynotes| Port_EnjoyNotes | porting A1s enjoynotes to CMCC N2*
*====================================================================================================*
*20160531|mengzhiming.wt|   customer req       | customer req    | customer req                      *
 *===================================================================================================*/

package com.wingtech.note;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.NinePatchDrawable;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.Spannable;
import android.text.Spanned;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.text.style.StrikethroughSpan;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.inputmethod.InputMethodManager;
import android.widget.DatePicker;
import android.widget.TextView;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.CharBuffer;
import java.security.MessageDigest;
import java.security.MessageDigestSpi;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Locale;

import com.wingtech.note.data.NotesProvider;
import com.wingtech.note.editor.RichEditView;
import com.wingtech.note.R;


public class Utils {

    public static <T> String congregateAsString(Iterable<T> arrayList, String paramString) {
        String str;
        if (arrayList == null)
            str = "";

        Iterator<T> localIterator = arrayList.iterator();
        if (localIterator.hasNext()) {
            StringBuilder localStringBuilder = new StringBuilder();
            localStringBuilder.append(localIterator.next());
            while (localIterator.hasNext())
                localStringBuilder.append(paramString).append(localIterator.next());
            str = localStringBuilder.toString();
        }
        else {
            str = "";
        }
        return str;
    }

    public static Bitmap clipImageAttachment(String filePath, NinePatchDrawable drawable) {
        BitmapFactory.Options option = new BitmapFactory.Options();
        option.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, option);
        if ((option.outWidth <= 0) || (option.outHeight <= 0))
        {
            return null;
        }
        Rect paddingRect = new Rect();
        drawable.getPadding(paddingRect);
        int width = drawable.getIntrinsicWidth() - paddingRect.left - paddingRect.right;
        int height = drawable.getIntrinsicHeight() - paddingRect.top - paddingRect.bottom;

        int sampleSize;
        if (width * option.outHeight > height * option.outWidth)
            sampleSize = option.outWidth / width;
        else
            sampleSize = option.outHeight / height;
        option.inJustDecodeBounds = false;
        option.inSampleSize = sampleSize;
        Bitmap src = BitmapFactory.decodeFile(filePath, option);
        if (src != null) {
            Bitmap targetBmp = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                    drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(targetBmp);
            Rect srcRect;
            if (src.getWidth() * height > src.getHeight() * width) {
                int gap = src.getWidth() - width * src.getHeight() / height;
                srcRect = new Rect(gap / 2, 0, src.getWidth() - gap, src.getHeight());
            }
            else {
                int gap = src.getHeight() - height * src.getWidth() / width;
                srcRect = new Rect(0, gap / 2, src.getWidth(), src.getHeight() - gap / 2);
            }
            Rect dstRect = new Rect(paddingRect.left, paddingRect.top, width + paddingRect.left,
                    height + paddingRect.top);

            canvas.drawBitmap(src, srcRect, dstRect, null);
            drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
            drawable.draw(canvas);
            src.recycle();
            return targetBmp;
        }
        else
            return null;
    }

    public static Bitmap resizeImageAttachment(int width, int height, int sampleSize,
            InputStream inputStream, NinePatchDrawable cover) {
        BitmapFactory.Options option = new BitmapFactory.Options();
        option.inJustDecodeBounds = false;
        option.inSampleSize = sampleSize;
        Bitmap src = BitmapFactory.decodeStream(inputStream, null, option);
        Bitmap targetBmp;
        if (src == null)
            return null;
        Rect rect = new Rect();
        cover.getPadding(rect);
        targetBmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(targetBmp);
        canvas.drawBitmap(src, null, new Rect(rect.left, rect.top, width
                - rect.right, height - rect.bottom), null);
        cover.setBounds(0, 0, targetBmp.getWidth(), targetBmp.getHeight());
        cover.draw(canvas);
        src.recycle();
        return targetBmp;
    }

    public static Bitmap createThumbnail(String path) {
        BitmapFactory.Options option = new BitmapFactory.Options();
        Bitmap target = null;
        option.inJustDecodeBounds = false;
        Bitmap src = BitmapFactory.decodeFile(path, option);
        if ((option.outWidth <= 0) || (option.outHeight <= 0)) {
            return null;
        }
        int width = option.outWidth, height = option.outHeight;
        // Bitmap target = null;
        if (option.outWidth > 1280 || option.outHeight > 1280) {
            if (width > 1280) {

                height = 1280 * height / width;
                width = 1280;
            }
            if (height > 1280) {
                width = 1280 * width / height;
                height = 1280;
            }
            // option.inJustDecodeBounds = false;
            // target = ThumbnailUtils.extractThumbnail(src, width, height,
            // ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
            target = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(target);
            Rect from = new Rect(0, 0, option.outWidth, option.outHeight);
            Rect to = new Rect(0, 0, width, height);
            canvas.drawBitmap(src, from, to, new Paint(Paint.FILTER_BITMAP_FLAG));
        }
        if (target == null)
            return src;
        else
            return target;
    }

    public static String getFileSha1(String path) {
        try {
            FileInputStream in = new FileInputStream(path);
            try {
                MessageDigest digest;
                digest = MessageDigest.getInstance("SHA-1");
                byte[] buffer = new byte[1024 * 1024];// 1M
                int len = 0;
                while ((len = in.read(buffer)) > 0) {
                    digest.update(buffer, 0, len);
                }
                return byte2String(digest.digest());
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } finally {

                try {
                    in.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return null;

    }

    public static String byte2String(byte[] buffer) {
        StringBuffer sb = new StringBuffer(buffer.length);

        String stmp = "";
        int len = buffer.length;
        for (int n = 0; n < len; n++) {
            stmp = Integer.toHexString(buffer[n] & 0xff);
            if (stmp.length() == 1)
                sb.append("0").append(stmp);
            else
                sb.append(stmp);
        }
        return sb.toString();
    }

    public static String formatTime(long date,Context context) {
        Time time = new Time();
        time.setToNow();
        int curYear = time.year;
        int curDay = time.yearDay;
        time.set(date);
        int year = time.year;
        int day = time.yearDay;
        String formatStr, str;
        //if ((curYear == year) && (curDay == day)) {
//            Integer[] args = {
//                    time.hour, time.minute
//            };
//            formatStr = "%02d:%02d";
//            str = String.format(Locale.US, formatStr, args);
            if(context!=null&&!DateFormat.is24HourFormat(context))
                str= DateUtils.formatDateRange(context, date, date, DateUtils.FORMAT_12HOUR
                    | DateUtils.FORMAT_SHOW_TIME );
            else
                str= DateUtils.formatDateRange(context, date, date, DateUtils.FORMAT_24HOUR
                        | DateUtils.FORMAT_SHOW_TIME);
        //}
        /*else if (curYear == year) {
            Integer[] args = {
                    1 + time.month, time.monthDay
            };
            formatStr = "%02d/%02d";
            str = String.format(Locale.US, formatStr, args);
        }*/
    //    else {
            Integer[] args = {
                    time.year, 1 + time.month, time.monthDay
            };

            formatStr = "%04d-%02d-%02d";
            str = String.format(Locale.US, formatStr, args)+" "+str;
        //}

        return str;
    }

    public static void showSoftInput(View view) {
        // TODO Auto-generated method stub
        InputMethodManager iMM = (InputMethodManager) view.getContext()
                .getSystemService("input_method");
        if (iMM != null)
            iMM.showSoftInput(view, 0);
    }

    public static void hideSoftInput(View view) {
        InputMethodManager iMM = (InputMethodManager) view.getContext().getSystemService(
                "input_method");
        if (iMM != null)
            iMM.hideSoftInputFromWindow(view.getWindowToken(),
                    0);
    }

    public static void takePhoto(Activity activity, int paramInt) {
        Intent localIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        localIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        //bug 199966,mengzhiming.wt,add,20160722,start
        localIntent.setAction(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE);
        localIntent.setAction(MediaStore.ACTION_IMAGE_CAPTURE_SECURE);
        //bug 199966,mengzhiming.wt,add,20160722,end
        localIntent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT,
                NotesProvider.getTempFileUri());
        // Uri.fromFile(new File(AttachmentUtils.getTmpFile(activity))));
        activity.startActivityForResult(localIntent, paramInt);
    }

    public static int[] measureView(View contentView) {
        contentView.measure(0, 0);
        int[] arrayOfInt = new int[2];
        arrayOfInt[0] = contentView.getMeasuredWidth();
        arrayOfInt[1] = contentView.getMeasuredHeight();
        return arrayOfInt;
    }

    public static void selectImage(Activity activity, int code) {
        // TODO Auto-generated method stub
        Intent localIntent = new Intent("android.intent.action.GET_CONTENT");
        localIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        localIntent.setType("image/*");
        activity.startActivityForResult(localIntent, code);
    }

    public static void selectContact(Activity activity, int code) {
        // TODO Auto-generated method stub
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        intent.setType("vnd.android.cursor.dir/phone_v2");
        activity.startActivityForResult(intent, code);
    }

    public static boolean isChineseLanguage() {
        return Locale.getDefault().getLanguage().equals(Locale.CHINESE.getLanguage());
    }

    public static void setTextWithRelativeTime(TextView paramTextView, long paramLong) {
        CharBuffer localCharBuffer = getRelativeTimeText(paramTextView.getContext(),
                null, paramLong);
        paramTextView.setText(localCharBuffer.array(), 0, localCharBuffer.position());
    }

    public static CharBuffer getRelativeTimeText(Context paramContext, CharBuffer paramCharBuffer,
            long paramLong) {
        if (paramCharBuffer == null)
            paramCharBuffer = CharBuffer.allocate(50);
        formatRelativeTimeString(paramContext, paramLong, paramCharBuffer);
        return paramCharBuffer;
    }

    private static void formatRelativeTimeString(Context context, long inMills, CharBuffer out) {
        Calendar c = Calendar.getInstance();
        int curYear = c.get(Calendar.YEAR);
        int curDay = c.get(Calendar.DAY_OF_YEAR);
        int curHour = c.get(Calendar.HOUR_OF_DAY);
        int curMin = c.get(Calendar.MINUTE);

        if (c.getTimeInMillis() < inMills) {
            c.setTimeInMillis(inMills);
            int alertYear = c.get(Calendar.YEAR);
            int alertDay = c.get(Calendar.DAY_OF_YEAR);
            int alertHour = c.get(Calendar.HOUR_OF_DAY);
            SimpleDateFormat formater;
            if (alertYear > curYear) {
                formater = new SimpleDateFormat("yyyy" + context.getString(R.string.separater_year)
                        + "MM" + context.getString(R.string.separater_month) + "dd"
                        + context.getString(R.string.separater_day));
                out.put(formater.format(c.getTime()));
            } else if (alertDay > curDay) {
                formater = new SimpleDateFormat("MM" + context.getString(R.string.separater_month)
                        + "dd"
                        + context.getString(R.string.separater_day));
                out.put(formater.format(c.getTime()));
            } else if (alertHour > curHour) {
// +Bug 273538   , zhoupengfei.wt, MODIFY, 20140502
             //   formater = new SimpleDateFormat("hh:mm");
             //   out.put(formater.format(c.getTime()));
                Integer[] args = {c.getTime().getHours(),c.getTime().getMinutes()};
                out.put(String.format("%02d:%02d", args));
// -Bug 273538   , zhoupengfei.wt, MODIFY, 20140502
            } else {
                int diffMin = c.get(Calendar.MINUTE) - curMin;
                out.put(diffMin + context.getString(R.string.minutes_later));
            }
        }
        else {
            out.put(context.getString(R.string.note_alert_expired));
        }
    }

    public static CharSequence trimEmptyLineSequence(CharSequence in) {
        String str = in.toString().trim();
        if (str.isEmpty())
            return "";
        else {

            int i = in.toString().indexOf(str);
            return in.subSequence(
                    1 + in.toString().lastIndexOf('\n', i), i + str.length());
        }
    }

    public static CharSequence getFormattedSnippet(CharSequence in) {
        if (in != null) {
            in = trimEmptyLineSequence(in);
            int i = in.toString().indexOf('\n');
            if (i >= 0)
                in = in.subSequence(0, i);
        }
        return in;
    }

    public static Animator buildFadeInAnimator(View view, long duration) {
        float[] arrayOfFloat = {
                0.0F, 1.0F
        };
        ObjectAnimator localObjectAnimator = ObjectAnimator.ofFloat(view, "alpha", arrayOfFloat);
        localObjectAnimator.setDuration(duration);
        return localObjectAnimator;
    }

    public static Animator buildFadeOutAnimator(View view, long duration) {
        float[] arrayOfFloat = {
                1.0F, 0.0F
        };

        ObjectAnimator localObjectAnimator = ObjectAnimator.ofFloat(view, "alpha",
                arrayOfFloat);
        localObjectAnimator.setDuration(duration);
        return localObjectAnimator;
    }

    /**
     * Copy data from a source stream to destFile. Return true if succeed,
     * return false if failed.
     */
    public static boolean copyToFile(InputStream inputStream, File destFile) {
        try {
            if (destFile.exists()) {
                destFile.delete();
            }
            FileOutputStream out = new FileOutputStream(destFile);
            try {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) >= 0) {
                    out.write(buffer, 0, bytesRead);
                }
            } finally {
                out.flush();
                try {
                    out.getFD().sync();
                } catch (IOException e) {
                }
                out.close();
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static void setStrikeSpan(Spannable spannable, int start, int end) {
        spannable.setSpan(new StrikethroughSpan(), start, end, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
    }

    public static byte[] serialize(Object object) {
        ObjectOutputStream out = null;
        ByteArrayOutputStream bout = null;
        try {
            bout = new ByteArrayOutputStream();
            out = new ObjectOutputStream(bout);
            out.writeObject(object);
            return bout.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }

    public static Object unSerialize(byte[] bytes) {
        ByteArrayInputStream bin = null;
        try {
            bin = new ByteArrayInputStream(bytes);
            ObjectInputStream oin = new ObjectInputStream(bin);
            return oin.readObject();
        } catch (Exception e) {
            return null;
        }

    }

    public static Bitmap getCachedBmpByView(View v) {
        Bitmap bmp = Bitmap.createBitmap(v.getMeasuredWidth(),
                v.getMeasuredHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        canvas.drawColor(Color.WHITE);
        v.draw(canvas);
        return bmp;
    }
    //bug183570,mengzhiming.wt,add 20160610,start
    public static void configureDatePicker(DatePicker datePicker) {
        // The system clock can't represent dates outside this range.
        Calendar t = Calendar.getInstance();
        t.clear();
        t.set(1970, Calendar.JANUARY, 1);
        datePicker.setMinDate(t.getTimeInMillis());
        t.clear();
        t.set(2037, Calendar.DECEMBER, 31);
        datePicker.setMaxDate(t.getTimeInMillis());
    }
    //bug183570,mengzhiming.wt,add 20160610,end
}
