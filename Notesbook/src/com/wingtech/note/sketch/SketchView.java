/*===================================================================================================*	
 *  when  |      who     |    keyword           |        why         |         what                  *	
 *===================================================================================================*	
 *20160407|mengzhiming.wt|porting A1s enjoynotes| Port_EnjoyNotes | porting A1s enjoynotes to CMCC N2*	
*====================================================================================================*/

package com.wingtech.note.sketch;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.PorterDuff.Mode;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.widget.FrameLayout.LayoutParams;

import com.wingtech.note.Utils;
import com.wingtech.note.editor.RichEditor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Vector;

public class SketchView extends SurfaceView implements Callback, Runnable {
    private static final int BACKGROUND_COLOR = Color.WHITE;
    private Context mContex;
    private float mX;
    private float mY;

    private SurfaceHolder sfh;
    private Canvas canvas;
    private final Paint mGesturePaint = new Paint();
    private final Paint mCircle = new Paint();
    private final RichPathInfo mPathInfo = new RichPathInfo();
    private Path mPath = new Path();
    // 这两个数组需要保持一致
    private List<Path> paths = new LinkedList<Path>();
    private List<RichPathInfo> infos = new LinkedList<RichPathInfo>();
    private volatile boolean isDrawing;
    private volatile boolean drawOnce = false;
    private boolean runFlag;
    private int mColor = DEFAULT_COLOR;
    private int mWidth = DEFAULT_WIDTH;
    private static int DEFAULT_COLOR = Color.BLACK;
    private static int DEFAULT_WIDTH = 10;
    private static int DEFALUT_ALPHA = 255;
    private boolean mSketchState = false;// 当前是否开启涂鸦模式
    private byte[] data;
    private boolean mDeleting = false;
    private float mDX = -1;
    private float mDY = -1;
    private volatile boolean mDrawFingerFlag = false;// 是否需要画当前手指，用于在删除线时的指示
    private volatile boolean mDeletePathFlag = false;// 需要在绘图线程中删除线段，否则会有线程安全问题
    // Vector类是线程安全的
    // 这两个数组，一个是记录删除时手指滑过的点，另一个记录删除点转化的对应的线段，以供在绘图线程中删除
    private Vector<PointF> mDeletePoints = new Vector<PointF>();
    private Vector<RichPathInfo> mDeleteInfos = new Vector<RichPathInfo>();
    // 记录删除点在线段中的位置，以供断开较长的线段
    private Vector<Integer> mDeletePointPos = new Vector<Integer>();

    // 储存撤销操作时移除的线条
    private LinkedList<RichPathInfo> mCachedInfos = new LinkedList<RichPathInfo>();
    // 储存删掉的线段以供恢复用
    private Vector<RichPathInfo> bufferedDeletedInfos = new Vector<RichPathInfo>();


    public void setSketchState(boolean b) {
        mSketchState = b;
        if (mSketchState)
            mDeleting = false;
        else
            isDrawing = false;
            
    }

    public boolean getSketchState() {
        return mSketchState;
    }

    public SketchView(Context context) {
        this(context, null, 0);

    }

    public SketchView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SketchView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContex = context;
        sfh = getHolder();
        sfh.addCallback(this);
        sfh.setFormat(PixelFormat.TRANSPARENT);
        setBackgroundColor(BACKGROUND_COLOR);
        setZOrderOnTop(true);
        // setZOrderMediaOverlay(false);
        mGesturePaint.setAntiAlias(true);
        mGesturePaint.setStyle(Style.STROKE);
        mCircle.setAntiAlias(true);
        mCircle.setStyle(Style.STROKE);
        mCircle.setStrokeWidth(3);
        mCircle.setColor(Color.RED);
    }

    @Override
    protected boolean fitSystemWindows(Rect insets) {
        // TODO Auto-generated method stub
        return super.fitSystemWindows(insets);
    }

    @Override
    public boolean fitsSystemWindows() {
        // TODO Auto-generated method stub
        return super.fitsSystemWindows();
    }

    @Override
    public void forceLayout() {
        // TODO Auto-generated method stub
        super.forceLayout();
    }

    public void setPaintColor(int color) {
        mColor = color;
    }

    public void setLineWidth(int width) {
        mWidth = width;
    }

    public int getPaintColor() {
        return mColor;
    }

    public int getLineWidth() {
        return mWidth;
    }

    // 在滚动时这两个标志量需要外部设置下
    public void setDrawFinger(boolean b) {
        mDrawFingerFlag = b;
    }

    public void setDrawSketchFlag(boolean b) {
        isDrawing = b;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // TODO Auto-generated method stub
        if (!mSketchState)
            return false;
        if (mDeleting) {
            mDX = event.getX();
            mDY = event.getY();

            deleteLineByPoint();
            if (event.getAction() == MotionEvent.ACTION_UP) {
                mDrawFingerFlag = false;
            } else if (event.getAction() == MotionEvent.ACTION_DOWN) {
                mDrawFingerFlag = true;
            }
            return true;
        } else {
            mDrawFingerFlag = false;
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchDown(event);
                return true;

            case MotionEvent.ACTION_MOVE:
                if (isDrawing) {
                    touchMove(event);
                    return true;
                }
                break;
            case MotionEvent.ACTION_UP:
                if (isDrawing) {
                    touchUp(event);
                    return true;
                }
                break;
        }
        return super.onTouchEvent(event);
    }

    // 改进删除机制，改为只删除一部分，不删除整条线段
    private void deleteLineByPoint() {
        // TODO Auto-generated method stub
        // new Thread(new SearchingLineTask()).start();
        PointF point = new PointF(mDX, mDY);
        mDeletePoints.add(point);
    }

    /*
     * 该进程不但担负查找点对应的线，并删除之，并且负责删除模式下的绘图刷新
     */
    private class SearchingLineTask implements Runnable {
        public void run() {
            // 从后向前查找
            while (mDeleting) {
                int pos = -1;
                PointF point;
                RichPathInfo theInfo = null;
                if (mDeletePoints.size() > 0) {
                    point = mDeletePoints.get(0);
                    // 此处需注意线程安全问题，不能使用迭代方法遍历infos
                    for (int i = 0; i < infos.size(); i++) {
                        RichPathInfo info = infos.get(i);
                        if (info != null) {
                            pos = info.judgeLinePositionByPoint(point);
                            if (pos >= 0) {
                                theInfo = info;
                                break;
                            }
                        } else
                            break;
                    }
                    if (pos >= 0) {
                        mDeletePathFlag = true;
                        mDeleteInfos.add(theInfo);
                        mDeletePointPos.add(pos);
                        deleteSelectedLines();
                    }
                    mDeletePoints.remove(0);
                }
                refreshSketch();
            }
        }
    }

    private void touchDown(MotionEvent event) {
        isDrawing = true;
        float x = event.getX();
        float y = event.getY();

        mX = x;
        mY = y;
        mPathInfo.reset();
        mPath.reset();
        mPathInfo.setColor(mColor);
        mPathInfo.setWidth(mWidth);
        mPathInfo.addPoint(x, y);
        mPath.moveTo(x, y);
        // 开始画，清空缓存的线条
        mCachedInfos.clear();
        bufferedDeletedInfos.clear();
    }

    private void touchMove(MotionEvent event) {

        final float x = event.getX();
        final float y = event.getY();

        final float previousX = mX;
        final float previousY = mY;

        final float dx = Math.abs(x - previousX);
        final float dy = Math.abs(y - previousY);

        if (dx >= 3 || dy >= 3) {
            mPathInfo.addPoint(x, y);
            mPath.quadTo(previousX, previousY, (x + previousX) / 2, (y + previousY) / 2);
            mX = x;
            mY = y;
        }
    }

    private void touchUp(MotionEvent event) {
        isDrawing = false;
        // 当记录两个以上的点时，才保存下来，否则忽略。for 节省数据量
        if (mPathInfo.getPoints().size() > 1) {
            RichPathInfo bufferedPathInfo = (RichPathInfo) mPathInfo.clone();
            Path bufferedPath = new Path(mPath);
            infos.add(bufferedPathInfo);
            paths.add(bufferedPath);
            mPath.reset();
            mPathInfo.reset();
        }
        // 此时绘图线程已经停止，应当在主线程中调用一次
        drawOnce = true;
    }

    public void surfaceCreated(SurfaceHolder holder) {
        // TODO Auto-generated method stub
        runFlag = true;
        // if (!mThread.isAlive())
        // mThread.start();
        new Thread(this).start();
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // TODO Auto-generated method stub

    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // TODO Auto-generated method stub
        runFlag = false;
    }

    public void run() {
        // TODO Auto-generated method stub

        while (runFlag) {
            long start = System.currentTimeMillis();
            if(drawOnce){
                refreshSketch();
                drawOnce = false;
            }
            if (isDrawing) {
                try {
                    canvas = sfh.lockCanvas();
                    if (canvas != null) {
                        canvas.drawColor(BACKGROUND_COLOR, Mode.CLEAR);
                        for (int i = 0; i < infos.size(); i++) {
                            mGesturePaint.setStrokeWidth(infos.get(i).getWidth());
                            mGesturePaint.setColor(infos.get(i).getColor());
                            canvas.drawPath(paths.get(i), mGesturePaint);
                        }

                        mGesturePaint.setStrokeWidth(mPathInfo.getWidth());
                        mGesturePaint.setColor(mPathInfo.getColor());
                        canvas.drawPath(mPath, mGesturePaint);
                    }
                } catch (Exception e) {
                    // TODO: handle exception
                    e.printStackTrace();
                } finally {

                    if (canvas != null)
                        sfh.unlockCanvasAndPost(canvas);
                }
            }
            long end = System.currentTimeMillis();
            if (end - start < 30)
                try {
                    Thread.sleep(30 - (end - start));
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
        }
    }

    public synchronized void setSketch(byte[] bytes) {
        data = bytes;
        // 此处运行开销较大，开线程处理
        new Thread(new Runnable() {
            public void run() {
                // TODO Auto-generated method stub
                @SuppressWarnings("unchecked")
                List<RichPathInfo> sinfo = (LinkedList<RichPathInfo>) Utils.unSerialize(data);
                if (sinfo != null) {
                    for (RichPathInfo info : sinfo) {
                        // mLines.put(info, info.transferPath());
                        infos.add(info);
                        paths.add(info.transferPath());
                    }
                } else {
                    // mLines.clear();
                    infos.clear();
                    paths.clear();
                }
                refreshSketch();
            }
        }).start();
    }

    public synchronized byte[] getSketch() {
        // ArrayList<RichPathInfo> sketch = new
        // ArrayList<RichPathInfo>(mLines.keySet());
        return Utils.serialize(infos);
    }
// +bug_268944 , zhoupengfei.wt, ADD, 20140408	
    @Override
	protected void onDraw(Canvas canvas) {
		// TODO Auto-generated method stub
		super.onDraw(canvas);
        drawOnce = true;
	}
// -bug_268944 , zhoupengfei.wt, ADD, 20140408	
    public void setSketchDeleteMode() {
        // TODO Auto-generated method stub
        if (mDeleting)
            mDeleting = false;
        else {
            mDeleting = true;
            new Thread(new SearchingLineTask()).start();
        }
    }

    public boolean getSketchDeleteMode() {
        // TODO Auto-generated method stub
        return mDeleting;
    }

    // 供保存为图片时调用
    public void drawPathes(Canvas canvas) {
        // TODO Auto-generated method stub
        for (int i = 0; i < paths.size(); i++) {
            mGesturePaint.setStrokeWidth(infos.get(i).getWidth());
            mGesturePaint.setColor(infos.get(i).getColor());
            canvas.drawPath(paths.get(i), mGesturePaint);
        }
    }

    // 该函数在操作栏调用，不需要考虑线程安全问题
    public void cancelPath() {
        if (bufferedDeletedInfos.size() > 0) {
            infos.add(bufferedDeletedInfos.get(bufferedDeletedInfos.size() - 1));
            paths.add(bufferedDeletedInfos.get(bufferedDeletedInfos.size() - 1).transferPath());
            bufferedDeletedInfos.remove(bufferedDeletedInfos.size() - 1);
        } else if (infos.size() > 0) {
            mCachedInfos.addFirst(infos.get(infos.size() - 1));
            infos.remove(infos.size() - 1);
            paths.remove(paths.size() - 1);
        }
        refreshSketch();
    }

    public void recoverPath() {
        try {
            RichPathInfo info = mCachedInfos.removeFirst();
            infos.add(info);
            paths.add(info.transferPath());
            refreshSketch();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void deleteSelectedLines() {
        // 删除
        if (mDeletePathFlag) {
            mDeletePathFlag = false;
            RichPathInfo info;
            RichPathInfo[] resultInfos;
            int index;
            for (int i = 0; i < mDeleteInfos.size(); i++) {
                info = mDeleteInfos.get(i);
                index = infos.indexOf(info);
                if (index < 0) {
                    continue;
                }
                resultInfos = info.divideLineByPoint(mDeletePointPos.get(i));
                if (resultInfos == null) {
                    paths.remove(index);
                    infos.remove(index);
                    bufferedDeletedInfos.add(info);
                } else {
                    bufferedDeletedInfos.add(resultInfos[2]);
                    if (resultInfos[0].getPoints().size() > 2
                            && resultInfos[1].getPoints().size() > 2) {
                        paths.set(index, resultInfos[0].transferPath());
                        infos.set(index, resultInfos[0]);

                        paths.add(index, resultInfos[1].transferPath());
                        infos.add(index, resultInfos[1]);
                    } else if (resultInfos[0].getPoints().size() > 2) {
                        paths.set(index, resultInfos[0].transferPath());
                        infos.set(index, resultInfos[0]);
                    } else if (resultInfos[1].getPoints().size() > 2) {
                        paths.set(index, resultInfos[1].transferPath());
                        infos.set(index, resultInfos[1]);
                    } else {
                        paths.remove(index);
                        infos.remove(index);
                    }

                }

            }
            mDeleteInfos.clear();
            mDeletePointPos.clear();
        }
    }

    public void refreshSketch() {
        try {
            canvas = sfh.lockCanvas();
            if (canvas != null) {
                canvas.drawColor(BACKGROUND_COLOR, Mode.CLEAR);

                for (int i = 0; i < infos.size(); i++) {
                    mGesturePaint.setStrokeWidth(infos.get(i).getWidth());
                    mGesturePaint.setColor(infos.get(i).getColor());
                    canvas.drawPath(paths.get(i), mGesturePaint);
                }
                mGesturePaint.setStrokeWidth(mPathInfo.getWidth());
                mGesturePaint.setColor(mPathInfo.getColor());
                canvas.drawPath(mPath, mGesturePaint);
                // 画上删除标志
                if (mDrawFingerFlag) {
                    canvas.drawCircle(mDX, mDY, 10, mCircle);
                }
            }
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        } finally {
            if (canvas != null)
                sfh.unlockCanvasAndPost(canvas);
        }
    }

    public void clearBoard() {
        infos.clear();
        paths.clear();
        mPath.reset();
        mPathInfo.reset();
        mCachedInfos.clear();
        bufferedDeletedInfos.clear();
    }
}

