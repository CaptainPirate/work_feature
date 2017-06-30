/*===================================================================================================*
 *  when  |      who     |    keyword           |        why         |         what                  *
 *===================================================================================================*
 *20160407|mengzhiming.wt|porting A1s enjoynotes| Port_EnjoyNotes | porting A1s enjoynotes to CMCC N2*
*====================================================================================================*/

package com.wingtech.note.editor;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.NinePatchDrawable;
import android.net.Uri;
import android.os.SystemClock;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.StrikethroughSpan;
import android.text.style.TextAppearanceSpan;
import android.text.style.URLSpan;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.wingtech.note.AttachmentUtils;
import com.wingtech.note.ResourceParser;
import com.wingtech.note.Utils;
import com.wingtech.note.data.Contact;
import com.wingtech.note.data.NotesProvider;
import com.wingtech.note.spannableparser.IMediaHandler;
import com.wingtech.note.spannableparser.SpanUtils;
import com.wingtech.note.R;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RichEditView extends EditText implements OnMenuItemClickListener {

    private static final String TAG = "RichEditView";
    private BitmapDrawable mMissedDrawable;
    private Context mContext;
    NinePatchDrawable mImageCoverDrawable;
    Rect mImageCoverPadding;
    Paint mMissedBackgroundPaint;
    private boolean mCursorVisible;
    private ClickContactDetector mClickContactDetector;
    private ClickImageDetector mClickImageDetector;
    private MediaHandler mMediaHandler;
    private boolean mReadonly;
    private long mNoteId;
    private Pattern mUserQueryPattern;
    private float mLastTouchDownX = -1.0F;
    private float mLastTouchDownY = -1.0F;
    private StrokeDetector mStrokeDetector;
    public boolean mStrikeState;

    public RichEditView(Context context) {
        this(context, null);
    }

    public RichEditView(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.editTextStyle);
    }

    public RichEditView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        mImageCoverDrawable = (NinePatchDrawable) (mContext.getResources().getDrawable(
                R.drawable.picture_frame));
        mImageCoverPadding = new Rect();
        mImageCoverDrawable.getPadding(mImageCoverPadding);
        mMissedBackgroundPaint = new Paint();
        mMissedBackgroundPaint.setColor(mContext.getResources().getColor(
                R.color.missed_image_background));
        mCursorVisible = true;
        addTextChangedListener(new RichTextWatcher());
        mClickImageDetector = new ClickImageDetector();
        mClickContactDetector = new ClickContactDetector();
        mStrokeDetector = new StrokeDetector();
        mMediaHandler = new MediaHandler();
        // setTextColor((getResources().getColor(android.R.color.black)));
        setTextAppearance(mContext, R.style.TextAppearance_Editor_Normal);
    }

    /*
     * 如果dispatchTouchEvent返回true
     * ，则交给这个view的onTouchEvent处理，如果dispatchTouchEvent返回 false ，则交给这个 view 的
     * interceptTouchEvent 方法来决定是否要拦截这个事件，如果 interceptTouchEvent 返回 true
     * ，也就是拦截掉了，则交给它的 onTouchEvent 来处理，如果 interceptTouchEvent 返回 false ，那么就传递给子
     * view ，由子 view 的 dispatchTouchEvent 再来开始这个事件的分发。如果事件传递到某一层的子 view 的
     * onTouchEvent 上了，这个方法返回了 false ，那么这个事件会从这个 view 往上传递，都是 onTouchEvent
     * 来接收。而如果传递到最上面的 onTouchEvent 也返回 false 的话，这个事件就会“消失”，而且接收不到下一次事件。
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if ((mClickImageDetector.handleMotionEvent(event))
                || (mClickContactDetector.handleMotionEvent(event))
                || mStrokeDetector.handleMotionEvent(event)
                || (super.dispatchTouchEvent(event)))
            return true;
        return false;
    }

    public boolean onTouchEvent(MotionEvent event) {
        if (super.onTouchEvent(event)) {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                mLastTouchDownX = event.getX();
                mLastTouchDownY = event.getY();
            }
            return true;
        }
        return false;
    }

    public void setReadonly(boolean paramBoolean) {
        mReadonly = paramBoolean;
    }

    public boolean onCheckIsTextEditor() {
        if ((!mReadonly) && (super.onCheckIsTextEditor()))
            return true;
        return false;
    }

    public void setNoteId(long id) {
        if (mNoteId != id)
            mNoteId = id;
    }

    public void setQueryPattern(String paramString) {
        if (!TextUtils.isEmpty(paramString))
            //bug105798,tangzihui.wt,modify,2015.10.12,for the key characters replaced with literal characters.
            mUserQueryPattern = Pattern.compile(Pattern.quote(paramString));
        else
            mUserQueryPattern = null;
    }

    // 通过触摸坐标计算出当前文本偏移量
    private int computeTextOffset(float x, float y) {
        if ((x < 0.0F) || (y < 0.0F))
            return -1;
        int lineIndex = getLayout().getLineForVertical((int) y);
        return getLayout().getOffsetForHorizontal(lineIndex, x);
    }

    @Override
    public boolean performClick() {
        boolean bool = super.performClick();
        if ((!bool) && (!mCursorVisible)) {
            setCursorVisible(true);
        }
        return bool;
    }

    @Override
    public boolean performLongClick() {
        if (!mStrikeState)
            return super.performLongClick();
        return true;
    }

    public void onCreateContextMenu(ContextMenu menu) {
        int position = computeTextOffset(this.mLastTouchDownX, this.mLastTouchDownY);
        if (position < 0)
            return;
        final URLSpan[] arrayOfURLSpan = (URLSpan[]) getText().getSpans(position, position,
                URLSpan.class);
        if (arrayOfURLSpan != null && arrayOfURLSpan.length > 0) {
            new MenuInflater(this.mContext).inflate(R.menu.richeditor_link_action, menu);
            String str = arrayOfURLSpan[0].getURL();
            MenuItem item1;
            if (str.startsWith("tel:"))
                item1 = menu.findItem(R.id.menu_action_call);
            else if (str.startsWith("http:"))
                item1 = menu.findItem(R.id.menu_action_web);
            else if (str.startsWith("mailto:"))
                item1 = menu.findItem(R.id.menu_action_email);
            else
                item1 = menu.findItem(R.id.menu_action_other);
            item1.setVisible(true);
            if (str.startsWith("tel:")) {
                item1.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem item) {
                        try {
                            Intent dialIntent = new Intent(Intent.ACTION_CALL, Uri
                                    .parse(arrayOfURLSpan[0].getURL()));
                            getContext().startActivity(dialIntent);
                            return true;
                        }
                        catch (ActivityNotFoundException localActivityNotFoundException) {
                            Log.e(TAG, localActivityNotFoundException.getMessage());
                            return false;
                        }
                    }
                });

            } else {
                item1.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem item) {
                        try {
                            arrayOfURLSpan[0].onClick(RichEditView.this);
                            return true;
                        }
                        catch (ActivityNotFoundException localActivityNotFoundException) {
                            Log.e(TAG, localActivityNotFoundException.getMessage());
                            return false;
                        }
                    }
                });
            }
            for (int i = 0; i < menu.size(); i++) {
                MenuItem item = menu.getItem(i);
                if ((item != item1) && (item.isVisible()))
                    item.setOnMenuItemClickListener(this);
            }
        } else
            super.onCreateContextMenu(menu);
    }

    private void analogClickEvent(float x, float y) {
        analogMotionEvent(x, y, MotionEvent.ACTION_DOWN);
        analogMotionEvent(x, y, MotionEvent.ACTION_UP);
    }

    private void analogMotionEvent(float x, float y, int action) {
        MotionEvent event = MotionEvent.obtain(SystemClock.uptimeMillis(),
                SystemClock.uptimeMillis(), action, x, y, 0);
        try {
            super.dispatchTouchEvent(event);
            return;
        } finally {
            event.recycle();
        }
    }

    // 该函数是在滚动视图时用于加载图片
    public void preLoadImages(int height, int offY) {
        Layout localLayout = getLayout();
        int startLine = localLayout.getLineForVertical(Math.max(0, offY - height));
        int endLine = localLayout.getLineForVertical(Math.min(getHeight(), offY + height * 2));
        int startPosition = localLayout.getLineStart(startLine);
        int endPositon = localLayout.getLineEnd(endLine);
        Editable localEditable = getText();
        TouchableImageSpan[] imgSpans = (TouchableImageSpan[]) localEditable.getSpans(0,
                localEditable.length(), TouchableImageSpan.class);
        for (int i = 0; i < imgSpans.length; i++) {
            if (localEditable.getSpanStart(imgSpans[i]) >= startPosition
                    && localEditable.getSpanStart(imgSpans[i]) <= endPositon) {
                imgSpans[i].getCachedDrawable();
            }
            else
                imgSpans[i].recycle();
        }
    }

    private static abstract interface IActionDetector {
        public abstract boolean handleMotionEvent(MotionEvent event);
    }

    private void viewImage(TouchableImageSpan span) {
        if (!span.nMissed) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.addFlags(Intent.EXTRA_DOCK_STATE_DESK);
            String path = AttachmentUtils
                    .getAttachmentPath(mContext, span.getName());
            InputStreamBuilder builder = new InputStreamBuilder(path);
            try {
                String type = AttachmentUtils.getImageMimeType(builder.getInputStream());
                Uri uri = NotesProvider.getImageFileUri(span.getName());
                intent.setDataAndType(uri, type);
                mContext.startActivity(intent);
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }
    }

    private void viewContact(TouchableContactSpan span) {
        Uri localUri = Contact
                .getContactUri(mContext, span.getContactName(), span.getPhoneNumber());
        if (localUri != null)
            ContactsContract.QuickContact.showQuickContact(getContext(), this, localUri,
                    ContactsContract.QuickContact.MODE_LARGE, null);
    }

    private class StrokeDetector implements IActionDetector {
        private boolean nTraking;
        private Rect mActiveBound = new Rect();
        private int mActiveIndex;
        private float mStartX;

        private int getActiveParagraphIndex(MotionEvent event) {
            int offset = computeTextOffset(event.getX(), event.getY());
            String text = getText().toString();
            int index = 0;
            while (offset >= 0) {
                offset = text.lastIndexOf('\n', offset) - 1;
                if (offset >= -1)
                    index++;
            }
            return index;
        }

        public boolean handleMotionEvent(MotionEvent event) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    mActiveIndex = getActiveParagraphIndex(event);
                    mStartX = event.getX();
                    getLayout().getLineBounds(getLayout().getLineForVertical((int) event.getY()),
                            mActiveBound);
                    nTraking = true;
                    break;
                case MotionEvent.ACTION_UP:
                    if (nTraking && (event.getY() > mActiveBound.top - 0.5 * mActiveBound.height())
                            && (event.getY() < mActiveBound.bottom + 0.5 * mActiveBound.height())
                            && (Math.abs(event.getX() - mStartX) > 2 * getPaint().getTextSize())) {
                        setStrikeSpan(mActiveIndex);
                        mStrikeState = true;
                        return true;
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    if ((event.getY() < mActiveBound.top - 0.5 * mActiveBound.height())
                            || (event.getY() > mActiveBound.bottom + 0.5
                                    * mActiveBound.height()))
                        nTraking = false;
                    // else if (nTraking &&(Math.abs(event.getX() - mStartX) >
                    // getPaint().getTextSize())) {
                    // return true;
                    // }
                    break;
            }
            return false;
        }

    }

    private void setStrikeSpan(int index) {
        Editable editable = getText();
        String text = editable.toString();
        int i = 0, start = 0;
        while ((start < text.length()) && (i < index)) {
            start = 1 + text.indexOf('\n', start);
            i++;
        }
        int end = text.indexOf('\n', start + 1);
        if (end < 0)
            end = text.length();
        StrikethroughSpan[] spans = editable.getSpans(start, end, StrikethroughSpan.class);
        if (spans != null && spans.length > 0)
            for (StrikethroughSpan span : spans) {
                editable.removeSpan(span);
            }
        else
            editable.setSpan(new StrikethroughSpan(), start, end, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
    }

    private StrikethroughSpan getStrikeSpanEndAt(int end) {
        Editable editable = getText();
        StrikethroughSpan[] spans = editable
                .getSpans(0, editable.length(), StrikethroughSpan.class);
        for (StrikethroughSpan span : spans) {
            if (editable.getSpanEnd(span) == end)
                return span;
        }
        return null;
    }

    private class ClickImageDetector implements IActionDetector {
        private TouchableImageSpan nActiveSpan;
        private boolean nTraking;

        private ClickImageDetector() {
        }

        private TouchableImageSpan getActiveSpan(MotionEvent event)
        {
            Editable editable = getText();
            TouchableImageSpan[] spans = (TouchableImageSpan[]) editable.getSpans(0,
                    editable.length(), TouchableImageSpan.class);

            for (int i = 0; i < spans.length; i++) {
                if (isTouchInImageSpan(spans[i], event))
                    return spans[i];
            }
            return null;
        }

        private boolean isTouchInImageSpan(TouchableImageSpan paramSmartImageSpan,
                MotionEvent paramMotionEvent) {
            if (paramSmartImageSpan != null)
                return paramSmartImageSpan.isTouched((int) paramMotionEvent.getX(),
                        (int) paramMotionEvent.getY());
            return false;
        }

        public boolean handleMotionEvent(MotionEvent paramMotionEvent) {
            boolean bool = false;
            switch (paramMotionEvent.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    nActiveSpan = getActiveSpan(paramMotionEvent);
                    if (nActiveSpan != null) {
                        nTraking = true;
                        bool = true;
                    }
                    else
                        nTraking = false;
                    break;
                case MotionEvent.ACTION_UP:
                    if (nTraking && isTouchInImageSpan(nActiveSpan, paramMotionEvent)) {
                        viewImage(nActiveSpan);
                        nTraking = false;
                        nActiveSpan = null;
                        bool = true;
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (nTraking)
                        break;
                case MotionEvent.ACTION_CANCEL:
                    if (!nTraking)
                        break;
                default:
                    nTraking = false;
                    nActiveSpan = null;
                    break;
            }
            return bool;
        }
    }

    private class ClickContactDetector implements IActionDetector {
        private TouchableContactSpan nActiveSpan;
        private boolean nTraking;

        private ClickContactDetector() {
        }

        private TouchableContactSpan getActiveSpan(MotionEvent event) {
            Editable editable = getText();
            TouchableContactSpan[] spans = (TouchableContactSpan[]) editable.getSpans(0,
                    editable.length(), TouchableContactSpan.class);

            for (int i = 0; i < spans.length; i++) {
                if (isTouchInContactSpan(spans[i], event))
                    return spans[i];
            }
            return null;
        }

        private boolean isTouchInContactSpan(TouchableContactSpan span,
                MotionEvent event) {
            if (span != null)
                return span.isTouched((int) event.getX(),
                        (int) event.getY());
            return false;
        }

        public boolean handleMotionEvent(MotionEvent event) {
            boolean bool = false;
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    nActiveSpan = getActiveSpan(event);
                    if (nActiveSpan != null) {
                        nTraking = true;
                        bool = true;
                    }
                    else
                        nTraking = false;
                    break;
                case MotionEvent.ACTION_UP:
                    if (nTraking && isTouchInContactSpan(nActiveSpan, event)) {
                        viewContact(nActiveSpan);
                        nTraking = false;
                        nActiveSpan = null;
                        bool = true;
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (nTraking)
                        break;
                case MotionEvent.ACTION_CANCEL:
                    if (!nTraking)
                        break;
                default:
                    nTraking = false;
                    nActiveSpan = null;
                    break;
            }
            return bool;
        }

    }

    public void setFontSizeId(int size) {
        setTextAppearance(mContext,
                ResourceParser.TextAppearanceResources.getTexAppearanceResource(size));
// +other, zhoupengfei.wt, MODIFY, 20140401, adjust cursor position
//        setLineSpacing(0.2F * getTextSize(), 1);
        setLineSpacing(0, 1);
// -other, zhoupengfei.wt, MODIFY, 20140401, adjust cursor position
        //setupBackground();//del edittext background line,mengzhiming.wt,20160602
        mMissedDrawable = null;
        setRichText(getRichText());
    }

    private void setupBackground() {
        int lineHeight = getLineHeight();
        Bitmap bgLine = BitmapFactory.decodeResource(getResources(),
                R.drawable.rich_text_editor_bg_line);
        Bitmap targetBmp = Bitmap.createBitmap(bgLine.getWidth(), lineHeight,
                Bitmap.Config.ARGB_8888);
        new Canvas(targetBmp).drawBitmap(bgLine, 0.0F, lineHeight - bgLine.getHeight(), null);
        bgLine.recycle();
        BitmapDrawable drawable = new BitmapDrawable(getResources(), targetBmp);
        drawable.setTileModeXY(Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
        setBackgroundDrawable(drawable);
    }

    BitmapDrawable getMissedDrawable() {
        if (mMissedDrawable == null) {
            Bitmap bmp = adjustBitmapSize(R.drawable.picture_missing);
            mMissedDrawable = new BitmapDrawable(mContext.getResources(), bmp);
            mMissedDrawable.setBounds(0, 0, bmp.getWidth(), bmp.getHeight());
        }
        return mMissedDrawable;
    }

    private Bitmap adjustBitmapSize(int resId) {
        return adjustBitmapSize(new InputStreamBuilder(mContext, resId));
    }

    private Bitmap adjustBitmapSize(InputStreamBuilder builder) {
        InputStream inputStream = null;
        Bitmap bmp = null;
        try {
            inputStream = builder.getInputStream();

            Point point = new Point();
            int scale = measureBitmapSize(inputStream, point);
            if (scale <= 0) {
                Log.w(TAG, "measure image failed");
                bmp = null;
            } else {
// +Bug 273852    , zhoupengfei.wt, ADD, 20140507
                inputStream.reset();
// -Bug 273852    , zhoupengfei.wt, ADD, 20140507
                bmp = Utils.resizeImageAttachment(point.x, point.y, scale, inputStream,
                        mImageCoverDrawable);
            }
        } catch (FileNotFoundException e) {
            Log.w(TAG, "image file not found");
// +Bug 273852    , zhoupengfei.wt, ADD, 20140507
            e.printStackTrace();
        }catch (IOException e) {
// -Bug 273852    , zhoupengfei.wt, ADD, 20140507
            e.printStackTrace();
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return bmp;
    }

    private int measureBitmapSize(InputStream inputStream, Point point) {
        DisplayMetrics localDisplayMetrics = mContext.getResources().getDisplayMetrics();
        int i = (int) (0.8F * localDisplayMetrics.widthPixels);
        int j = (int) (0.7F * localDisplayMetrics.heightPixels);
        int scale;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(inputStream, null, options);
        int width = options.outWidth;
        int height = options.outHeight;
        if ((width <= 0) || (height <= 0)) {
            scale = -1;
        }
        else {
	try{
            // 先缩放高度，再缩放宽度
            if (height > j) {
                width = width * j / height;
                height = j;
            }

            if (width > i) {
                height = height * i / width;
                width = i;
            }
            // 使其缩放至能匹配行高
            int sw, sh;
            int lh = getLineHeight();
            sh = lh * ((height + lh + getPaint().getFontMetricsInt().ascent) / lh) - lh;
            sw = sh * width / height;
            point.set(sw, sh);
            scale = options.outWidth / sw;
	}catch(Exception e){
		e.printStackTrace();
		Log.e(TAG, "measureBitmapSize error");
		scale=-1;
	}
        }
        return scale;
    }

    private int getPreferSelectionStart() {
        if (!mCursorVisible)
            setSelection(getText().length());
        return getSelectionStart();
    }

    //+bug156714,tangzihui.wt,add,2016.03.19,for RichText display.
    private int getCurrentLine() {
        int selectionStart = getPreferSelectionStart();

        if (!(selectionStart == -1)) {
            return getLayout().getLineForOffset(selectionStart);
        }

        return -1;
    }
    //-bug156714,tangzihui.wt,add,2016.03.19,for RichText display.

    // 检查该start位置是否正是图片的起始位置
    private boolean isImageBeginAt(int start) {
        Editable editable = getText();
        if (start >= editable.length())
            return false;

        int end = editable.toString().indexOf('\n', start);
        if (end >= 0) {
            TouchableImageSpan[] arrayOfSmartImageSpan = (TouchableImageSpan[]) editable.getSpans(
                    start, end, TouchableImageSpan.class);
            for (int i = 0; i < arrayOfSmartImageSpan.length; i++) {
                if (editable.getSpanStart(arrayOfSmartImageSpan[i]) == start) {
                    return true;
                }
            }
        }
        return false;
    }

    // 插入联系人
    public void insertContact(Uri uri) {
        Contact contact = Contact.getContactListByUri(mContext, uri);
        if ((contact == null))
            return;
        Editable editable = getText();
        // 获得输入的起始位置，如果后面是图片，则插入换行符号
        int start = getPreferSelectionStart();
        // if (isImageBeginAt(start)) {
        // editable.insert(start, "\n");
        // start++;
        // }

        String objStr = "\uFFFC";
        int end = start + objStr.length();
        editable.insert(start, objStr);
        setContactSpan(editable, contact.getName(), contact.getPhoneNumber(), start, end);
        //bug156714,tangzihui.wt,modify,2016.03.19,for RichText display.
        setSelection(end);
        setCursorVisible(true);
    }

    public void insertContact(Uri[] uris) {
        ArrayList<Contact> localArrayList = Contact.getContactListByUris(mContext, uris);
        if ((localArrayList == null) || (localArrayList.isEmpty()))
            return;
        Editable editable = getText();
        // 获得输入的起始位置，如果后面是图片，则插入换行符号
        int start = getPreferSelectionStart();
        if (isImageBeginAt(start)) {
            editable.insert(start, "\n");
            start++;
        }
        Iterator<Contact> localIterator = localArrayList.iterator();
        while (localIterator.hasNext()) {
            Contact contact = (Contact) localIterator.next();
            String objStr = String.valueOf(0xFFFC);
            int end = start + objStr.length();
            editable.insert(start, objStr);
            setContactSpan(editable, contact.getName(), contact.getPhoneNumber(), start, end);
            start = end;
        }
        setSelection(start);
        setCursorVisible(true);
    }

    private void setContactSpan(Editable paramEditable, String name, String number,
            int start, int end) {
        Bitmap localBitmap = getContactImage(name);

        paramEditable.setSpan(new TouchableContactSpan(mContext, localBitmap, this, name,
                number), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private Bitmap getBitmapFromView(View view) {
        FrameLayout localFrameLayout = new FrameLayout(mContext);
        localFrameLayout.addView(view);
        localFrameLayout.setDrawingCacheEnabled(true);
        localFrameLayout.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        localFrameLayout.layout(0, 0, localFrameLayout.getMeasuredWidth(),
                localFrameLayout.getMeasuredHeight());
        return localFrameLayout.getDrawingCache();
    }

    private Bitmap getContactImage(String paramString) {
        TextView localTextView = (TextView) LayoutInflater.from(mContext).inflate(
                R.layout.contact_span_view, null);
        localTextView.setText(paramString);
        //bug150410,tangzihui.wt,modify,2016.03.02,modify text size unit.
        localTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, getTextSize());
        //bug156714,tangzihui.wt,add,2016.03.19,for RichText display.
        localTextView.setMaxWidth(2 * getMeasuredWidth());
        return getBitmapFromView(localTextView);
    }

    // 插入图片
    public void insertImage(Uri uri) {
        String imgName = AttachmentUtils.saveImageFile(mContext, uri);
        if (TextUtils.isEmpty(imgName)) {
            Toast.makeText(mContext, R.string.toast_message_add_image_fail, 0).show();
            return;
        }
        Editable localEditable = getText();
        int i = getPreferSelectionStart();
        if ((i > 0) && (localEditable.charAt(i - 1) != '\n')) {
            localEditable.insert(i, "\n");
            i++;
        }
        String objStr = "\uFFFC";
        localEditable.insert(i, objStr);
        setImageSpan(localEditable, i, i + objStr.length(), imgName);
        i += objStr.length();
        if (i < localEditable.length() && (localEditable.charAt(i) != '\n')) {
            localEditable.insert(i, "\n");
        } else if (i == localEditable.length()) {
            localEditable.insert(i, "\n");
        }
        setSelection(i + 1);
        setCursorVisible(true);
    }

    // 插入涂鸦图片
    public void insertSketchImage(Bitmap bmp) {
        String imgName = AttachmentUtils.saveImageFile(mContext, bmp);
        if (TextUtils.isEmpty(imgName)) {
            Toast.makeText(mContext, R.string.toast_message_add_image_fail, 0).show();
            return;
        }
        Editable localEditable = getText();
        int i = getPreferSelectionStart();
        if ((i > 0) && (localEditable.charAt(i - 1) != '\n')) {
            localEditable.insert(i, "\n");
            i++;
        }
        String objStr = "\uFFFC";
        localEditable.insert(i, objStr);
        setImageSpan(localEditable, i, i + objStr.length(), imgName);
        i += objStr.length();
        if (i < localEditable.length() && (localEditable.charAt(i) != '\n')) {
            localEditable.insert(i, "\n");
        } else if (i == localEditable.length()) {
            localEditable.insert(i, "\n");
        }
        setSelection(i + 1);
        setCursorVisible(true);
    }

    public void setCursorVisible(boolean visble) {
        mCursorVisible = visble;
        super.setCursorVisible(visble);
    }

    private void setImageSpan(Editable paramEditable, int start, int end, String imgName) {
        TouchableImageSpan span = new TouchableImageSpan(mContext, this, imgName);
        span.initialize();
        paramEditable.setSpan(span, start, end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

    }

    private void clearText() {
        Editable localEditable = getText();
        //Bug 48527 , zoujinghua.wt, MODIFY, 20150402
        localEditable.clearSpans();
        Object[] allSpans = localEditable.getSpans(0, localEditable.length(), Object.class);
        for (int i = 0; i < allSpans.length; i++)
            localEditable.removeSpan(allSpans[i]);
        setText("");
    }

    public CharSequence getRichText() {
        return SpanUtils.buildSpannable(getText());
    }

    //bug156714,tangzihui.wt,modify,2016.03.19,for RichText display.
    public void setRichText(final CharSequence paramCharSequence) {
        clearText();
        if (!TextUtils.isEmpty(paramCharSequence)) {
            //+bug156714,tangzihui.wt,modify,2016.03.19,for RichText display.
            post(new Runnable() {
                public void run() {
                    android.util.Log.d(TAG, "getMeasuredWidth() = " + getMeasuredWidth() + "    getWidth() = "
                            + getWidth());
                    Spanned localSpanned = SpanUtils.parseSpannable(paramCharSequence.toString(), mMediaHandler);
                    setText(localSpanned);
                }
            });
            //-bug156714,tangzihui.wt,modify,2016.03.19,for RichText display.
            Editable localEditable = getText();
            TouchableBaseSpan[] spans = localEditable.getSpans(0, localEditable.length(),
                    TouchableBaseSpan.class);
            int[][] spanBounds = new int[spans.length][2];// span边界
            for (int i = 0; i < spans.length; i++) {
                spanBounds[i][0] = localEditable.getSpanStart(spans[i]);
                spanBounds[i][1] = localEditable.getSpanEnd(spans[i]);
            }
            if (mUserQueryPattern != null) {
                Matcher localMatcher = mUserQueryPattern.matcher(localEditable);
                for (int i = 0; localMatcher.find(i); i = localMatcher.end()) {
                    // 需防止找到的字符在图片span或联系人span内
                    boolean b = true;
                    for (int j = 0; j < spanBounds.length; j++) {
                        if (!(localMatcher.start() > spanBounds[j][1] || localMatcher.end() < spanBounds[j][0]))
                            b = false;
                    }
                    if (b)// 只有不落在span区域内才能设高亮
                        localEditable.setSpan(new TextAppearanceSpan(mContext,
                                R.style.TextAppearance_HighLight), localMatcher.start(),
                                localMatcher
                                        .end(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                }
            }
        }
        setSelection(0);
        setCursorVisible(false);
    }

    private TouchableImageSpan[] getImageSpansAt(int position) {
        Editable localEditable = getText();
        TouchableImageSpan[] spans = null;
        if ((position >= 0) && (position < localEditable.length()))
            spans = (TouchableImageSpan[]) localEditable.getSpans(position, position,
                    TouchableImageSpan.class);

        return spans;
    }

    private TouchableImageSpan getImageSpanStartAt(int position) {
        TouchableImageSpan[] spans = getImageSpansAt(position);
        if (spans != null) {
            Editable localEditable = getText();

            for (int i = 0; i < spans.length; i++) {
                if (localEditable.getSpanStart(spans[i]) == position)
                    return spans[i];
            }
        }
        return null;
    }

    private TouchableImageSpan getImageSpanEndAt(int position) {
        TouchableImageSpan[] spans = getImageSpansAt(position);
        int j;
        if (spans != null) {
            Editable localEditable = getText();

            for (int i = 0; i < spans.length; i++) {
                if (localEditable.getSpanEnd(spans[i]) == position)
                    return spans[i];
            }
        }
        return null;
    }

    public class RichTextWatcher implements TextWatcher {
        private boolean nToBeMarkStrike;

        private RichTextWatcher() {
        }

        private <T> void deleteSpans(int start, int length, Class<T> paramClass) {
            Editable localEditable = RichEditView.this.getText();
            Object[] spans = localEditable.getSpans(start, start + length, paramClass);
            for (int i = 0; i < spans.length; i++)
                localEditable.removeSpan(spans[i]);
        }

        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            String str = s.toString();
            int startP = 1 + str.lastIndexOf('\n', start - 1);// 找到前面换行符的位置,找不到，则为0，即起始位置
            int endP = str.indexOf('\n', startP);// 从前面的换行符开始，找到后面一个回车符的位置,找不到，则为最后的位置
            if (endP < 0)
                endP = str.length();
            if ((((StrikethroughSpan[]) getText().getSpans(startP, endP, StrikethroughSpan.class)).length > 0))
                nToBeMarkStrike = true;
            else
                nToBeMarkStrike = false;
            deleteSpans(start, count, StrikethroughSpan.class);

        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (nToBeMarkStrike) {
                String str = s.toString();
                // 更改后重新查找一次
                int startP = 1 + str.lastIndexOf('\n', start - 1);
                int endP = str.indexOf('\n', startP);
                if (endP < 0)
                    endP = str.length();
                getText().setSpan(new StrikethroughSpan(), startP, endP,
                        Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            }
        }

        public void afterTextChanged(Editable s) {

        }

    }

    public class MediaHandler implements IMediaHandler {

        public void handleContact(SpannableStringBuilder builder, int start, int end, String name,
                String phone) {
            Bitmap localBitmap = RichEditView.this.getContactImage(name);
            TouchableContactSpan span = new TouchableContactSpan(mContext, localBitmap,
                    RichEditView.this, name,
                    phone);
//+Bug 274475 , zhoupengfei.wt, MODIFY, 20140514
            String objStr = "\uFFFC";
            builder.replace(start, end, objStr);
            builder.setSpan(span, start, start + objStr.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
//-Bug 274475 , zhoupengfei.wt, MODIFY, 20140514
        }

        public void handleImage(SpannableStringBuilder builder, int start, int end, String name) {
            TouchableImageSpan span = new TouchableImageSpan(mContext, RichEditView.this, name);
            span.initialize();
//+Bug 274475 , zhoupengfei.wt, MODIFY, 20140514
            String objStr = "\uFFFC";
            builder.replace(start, end, objStr);
            builder.setSpan(span, start, start + objStr.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
//-Bug 274475 , zhoupengfei.wt, MODIFY, 20140514
        }

        public void handleStrike(SpannableStringBuilder builder, int start, int end) {
            // TODO Auto-generated method stub
            builder.setSpan(new StrikethroughSpan(), start, end, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        }
    }

    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_action_select:
                int i = computeTextOffset(this.mLastTouchDownX, this.mLastTouchDownY);
                if ((i >= 0) && (((URLSpan[]) getText().getSpans(i, i, URLSpan.class)).length > 0)) {
                    analogClickEvent(mLastTouchDownX, mLastTouchDownY);
                    analogClickEvent(mLastTouchDownX, mLastTouchDownY);
                    return true;
                }
            default:
                return false;
        }

    }

    public void setVisibility(boolean b) {
        // TODO Auto-generated method stub
        setVisibility(b);
    }
}
