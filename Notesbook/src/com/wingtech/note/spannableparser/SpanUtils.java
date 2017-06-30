/*===================================================================================================*	
 *  when  |      who     |    keyword           |        why         |         what                  *	
 *===================================================================================================*	
 *20160407|mengzhiming.wt|porting A1s enjoynotes| Port_EnjoyNotes | porting A1s enjoynotes to CMCC N2*	
*====================================================================================================*/

package com.wingtech.note.spannableparser;

import java.io.File;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.wingtech.note.editor.TouchableContactSpan;
import com.wingtech.note.editor.TouchableImageSpan;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ImageSpan;
import android.text.style.StrikethroughSpan;

public class SpanUtils {
    public static final char OBJ_CHAR = '\uFFFC';
    public static final char NEWLINE_CHAR = '\n';
    public static final char IMG_START_CHAR = '\u263A';
    public static final char IMG_END_CHAR = '\u263B';
    public static final char CONTARCT_START_CHAR = '\u263E';
    public static final char CONTARCT_END_CHAR = '\u263D';
    public static final char CONTARCT_SPEATROR_CHAR = '\u263C';

    public static final char CHECK_CHAR = '\u221A';
    public static final char BLANK_CHAR = ' ';
    private static final String REGEX_STRIKE_SPAN = "^" + CHECK_CHAR + "([^\n" + CHECK_CHAR
            + "]+)$";

    private static final String REGEX_IMAGE_SPAN = IMG_START_CHAR + "([A-Za-z0-9]+)" + IMG_END_CHAR;
    private static final String REGEX_CONTACT_SPAN = CONTARCT_START_CHAR + "([0-9]+)"
            + CONTARCT_SPEATROR_CHAR + "([^"
            + IMG_START_CHAR + IMG_END_CHAR + CONTARCT_START_CHAR + CONTARCT_END_CHAR
            + CONTARCT_SPEATROR_CHAR + CHECK_CHAR + "]+)"
            + CONTARCT_END_CHAR;

    public static Spanned parseSpannable(String text, IMediaHandler handler) {
        SpannableStringBuilder builder = new SpannableStringBuilder(text);
        int start, end;
        Pattern p1 = Pattern.compile(REGEX_STRIKE_SPAN, Pattern.MULTILINE);
        Matcher m1 = p1.matcher(builder);

        while (m1.find()) {
            String content = m1.group(1);
            start = m1.start();
            end = m1.end();
            handler.handleStrike(builder, start, end);
            builder.replace(start, end, content);
            m1.reset(builder);
        }
        Pattern p2 = Pattern.compile(REGEX_CONTACT_SPAN, Pattern.MULTILINE);
        Matcher m2 = p2.matcher(builder);
        while (m2.find()) {
            String phone = m2.group(1);
            String name = m2.group(2);

            start = m2.start();
            end = m2.end();
            handler.handleContact(builder, start, end, name, phone);
            //reset the Matcher,wangweiping
            m2.reset(builder);
        }
        Pattern p3 = Pattern.compile(REGEX_IMAGE_SPAN, Pattern.MULTILINE);
        Matcher m3 = p3.matcher(builder);

        while (m3.find()) {
            String imgName = m3.group(1);
            start = m3.start();
            end = m3.end();
            handler.handleImage(builder, start, end, imgName);
            //reset the Matcher,wangweiping
            m3.reset(builder);
        }

        return builder;
    }

    public static Spanned parseStrikethroughSpan(CharSequence showText) {
        SpannableStringBuilder builder = new SpannableStringBuilder(showText);
        Pattern p = Pattern.compile(REGEX_STRIKE_SPAN, Pattern.MULTILINE);
        Matcher m = p.matcher(builder);
        int start, end;

        while (m.find()) {
            String content = m.group(1);
            start = m.start();
            end = m.end();
            builder.setSpan(new StrikethroughSpan(), start, end, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            builder.replace(start, end, content);
            m.reset(builder);
        }
        return builder;
    }

    public static CharSequence buildSpannable(Editable editable) {
        // 此处传入的是当前edit窗口的editable的引用，不能修改，含有span对象，创建新的editable对象时需将其序列化�?传入
        Editable out = Editable.Factory.getInstance().newEditable(
                editable.subSequence(0, editable.length()));
        int start, end;
        String content;
        TouchableContactSpan[] contacts = out.getSpans(0, out.length(), TouchableContactSpan.class);
        for (int i = 0; i < contacts.length; i++) {
            start = out.getSpanStart(contacts[i]);
            end = out.getSpanEnd(contacts[i]);
            content = CONTARCT_START_CHAR + contacts[i].getPhoneNumber() + CONTARCT_SPEATROR_CHAR
                    + contacts[i].getContactName()
                    + CONTARCT_END_CHAR;
            out.replace(start, end, content);
        }
        TouchableImageSpan[] images = out.getSpans(0, out.length(), TouchableImageSpan.class);
        for (int i = 0; i < images.length; i++) {
            start = out.getSpanStart(images[i]);
            end = out.getSpanEnd(images[i]);
            content = IMG_START_CHAR + images[i].getName() + IMG_END_CHAR;
            if (start >= 0 && end >= 0)
                out.replace(start, end, content);
        }
        StrikethroughSpan[] spans = out.getSpans(0, out.length(), StrikethroughSpan.class);
        for (int i = 0; i < spans.length; i++) {
            start = out.getSpanStart(spans[i]);
            end = out.getSpanEnd(spans[i]);
            content = out.subSequence(start, end).toString();
            if (start >= 0)
                out.replace(start, end, CHECK_CHAR + content);
        }
        return out;
    }

    public static class SimpleBuilder extends SpannableStringBuilder {
        public SimpleBuilder(String text) {
            super(text);
        }

        public SimpleBuilder() {
            super();
        }

        public boolean isEmpty() {
            return TextUtils.isEmpty(this.toString());
        }

        public void appendNewLine() {
            this.append("\n");
        }

        public void appendImage(String name) {
            // File img = new File(name);
            // if (img.exists()) {
            // BitmapFactory.Options option = new BitmapFactory.Options();
            // option.inJustDecodeBounds = false;
            // Bitmap bmp = BitmapFactory.decodeFile(img.getAbsolutePath(),
            // option);
            // // ImageSpan spans = new ImageSpan(bmp);
            // TouchableImageSpan span = new TouchableImageSpan(mContext, this,
            // imgName);
            // span.initialize();
            // // ImageSpan span = new ImageSpan(getMissedDrawable());
            // int start = this.length();
            // this.append(OBJ_CHAR);
            // this.setSpan(span, start, end,
            //
            // this.setSpan(spans, start, start + 1,
            // Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            // Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            // }
            this.append(IMG_START_CHAR + name + IMG_END_CHAR);
        }

        public void appendText(String text) {
            this.append(text);
        }

    }

    public static ArrayList<String> retrieveImages(Activity mActivity, CharSequence content) {
        ArrayList<String> out = new ArrayList<String>();
        Pattern p = Pattern.compile(REGEX_IMAGE_SPAN, Pattern.MULTILINE);
        Matcher m = p.matcher(content);
        while (m.find()) {
            String name = m.group(1);
            out.add(name);
        }
        return out;
    }

    // 将图片信息删掉，将联系人信息重新组织输出
    public static String normalizeSnippet(String content, NoteImgInfo info) {
        Pattern p = Pattern.compile(REGEX_IMAGE_SPAN, Pattern.MULTILINE);
        Matcher m = p.matcher(content);
        boolean bool = false;
        while (m.find()) {
            String name = m.group(1);
            if (!bool) {
                bool = true;
                if (info != null)
                    info.firstImgName = name;
            }
        }
        return normalizeSnippet(null, content).toString();
    }

    public static class NoteImgInfo {
        public String firstImgName;

        public NoteImgInfo() {
            firstImgName = null;
        }
    }

    public static CharSequence normalizeSnippet(String content) {
        return parseStrikethroughSpan(normalizeSnippet(null, content));
    }

    public static CharSequence normalizeSnippet(Context context, String content) {
        String out = new StringBuilder(content).toString();
        Pattern p1 = Pattern.compile(REGEX_CONTACT_SPAN, Pattern.MULTILINE);
        Matcher m1 = p1.matcher(out);
        //+bug118182,tangzihui.wt,modify,2015.11.13,replace string correctly.
        while (m1.find()) {
            out = m1.replaceFirst("[" + m1.group(2) + ": " + m1.group(1) + "]");
            m1 = p1.matcher(out);
        }
        //-bug118182,tangzihui.wt,modify,2015.11.13,replace string correctly.
        Pattern p2 = Pattern.compile(REGEX_IMAGE_SPAN, Pattern.MULTILINE);
        Matcher m2 = p2.matcher(out);
        if (m2.find())
            out = m2.replaceAll("");
        return out;
    }

    public static boolean findImageInSnippet(String content) {
        Pattern p = Pattern.compile(REGEX_IMAGE_SPAN, Pattern.MULTILINE);
        Matcher m = p.matcher(content);
        if (m.find())
            return true;
        return false;
    }

    public static ArrayList<String> collectImageNames(String content) {
        ArrayList<String> names = new ArrayList<String>();
        Pattern p = Pattern.compile(REGEX_IMAGE_SPAN, Pattern.MULTILINE);
        Matcher m = p.matcher(content);
        while (m.find()) {
            names.add(m.group(1));
        }
        return names;
    }
}
