/*===================================================================================================*	
 *  when  |      who     |    keyword           |        why         |         what                  *	
 *===================================================================================================*	
 *20160407|mengzhiming.wt|porting A1s enjoynotes| Port_EnjoyNotes | porting A1s enjoynotes to CMCC N2*	
*====================================================================================================*/

package com.wingtech.note.sketch;

import android.graphics.Color;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class RichPathInfo implements Serializable, Cloneable {
    private static final int MAX_DELETE_LEN = 50;
    public static final int PRECISION = 10;// 精度
    /**
     * 自动生成
     */
    private static final long serialVersionUID = 8637598861794054118L;
    private int mColor;
    private int mWidth;
    private static int DEFAULT_COLOR = Color.BLACK;
    private static int DEFAULT_WIDTH = 10;
    private List<PerPoint> mPoints;

    public class PerPoint implements Serializable, Cloneable {

        private static final long serialVersionUID = -5261415427144654761L;
        public float x;
        public float y;

        public PerPoint(float x, float y) {
            this.x = x;
            this.y = y;
        }

        protected PerPoint clone() {
            // TODO Auto-generated method stub

            return new PerPoint(this.x, this.y);
        }
    }

    @Override
    protected RichPathInfo clone() {
        // TODO Auto-generated method stub
        RichPathInfo newInfo = new RichPathInfo(mColor, mWidth);
        for (int i = 0; i < mPoints.size(); i++) {
            newInfo.addPoint(mPoints.get(i));
        }
        return newInfo;
    }

    public RichPathInfo(int color, int width) {
        mColor = color;
        mWidth = width;
        mPoints = new ArrayList<PerPoint>();
    }

    public RichPathInfo() {
        setColor(DEFAULT_COLOR);
        setWidth(DEFAULT_WIDTH);
        mPoints = new ArrayList<PerPoint>();
    }

    public void setColor(int color) {
        mColor = color;
    }

    public void setWidth(int width) {
        mWidth = width;
    }

    public int getColor() {
        return mColor;
    }

    public int getWidth() {
        return mWidth;
    }

    public void addPoint(float x, float y) {
        PerPoint newPoint = new PerPoint(x, y);
        mPoints.add(newPoint);
    }

    public void addPoint(PerPoint point) {
        if (point != null)
            mPoints.add(point);
    }

    public Path transferPath() {
        Path path = new Path();
        if (mPoints.size() > 0) {
            float curX, curY, lastX, lastY;
            PerPoint startPos = mPoints.get(0);
            lastX = startPos.x;
            lastY = startPos.y;
            path.moveTo(lastX, lastY);
            for (int i = 1; i < mPoints.size(); i++) {
                // 数组中可能有空值，很可能是反序列化数组导致的。
                if (mPoints.get(i) == null)
                    continue;
                curX = mPoints.get(i).x;
                curY = mPoints.get(i).y;
                path.quadTo(lastX, lastY, (curX + lastX) / 2, (curY + lastY) / 2);
                lastX = curX;
                lastY = curY;
            }
        }
        return path;
    }

    public void reset() {
        // TODO Auto-generated method stub
        mPoints.clear();
    }

    public List<PerPoint> getPoints() {
        return mPoints;
    }

    // 改进判断算法，使用向量数量积算法
    public int judgeLinePositionByPoint(float x, float y) {
        float curX, curY, lastX, lastY;
        curX = lastX = mPoints.get(0).x;
        curY = lastY = mPoints.get(0).y;
        if (Math.abs(x - curX) < PRECISION && Math.abs(y - curY) < PRECISION)
            return 0;
        // 判断点是否在当前点和上一个点之间
        for (int i = 1; i < mPoints.size(); i++) {
            // 数组中可能有空值，很可能是反序列化数组导致的。
            if (mPoints.get(i) == null)
                continue;
            curX = mPoints.get(i).x;
            curY = mPoints.get(i).y;
            lastX = mPoints.get(i - 1).x;
            lastY = mPoints.get(i - 1).y;
            if (Math.abs(x - curX) < PRECISION && Math.abs(y - curY) < PRECISION)
                return i;
            float left = Math.min(curX, lastX);
            float right = Math.max(curX, lastX);
            float top = Math.min(curY, lastY);
            float bottom = Math.max(curY, lastY);
            if (x < left || x > right || y < top || y > bottom)
                continue;
            float scalar = (curX - lastX) * (x - lastX) + (curY - lastY) * (y - lastY);// 计算向量乘积
            float a = (curX - lastX) * (curX - lastX) + (curY - lastY) * (curY - lastY);
            float b = ((x - lastX) * (x - lastX) + (y - lastY) * (y - lastY))
                    * a - scalar * scalar;
            int distance = (int) Math.sqrt(b / a);
            if (distance < PRECISION)
                return i;

        }
        return -1;
    }

    // 从Start开始，删除len个点
    public void deletePoints(int start, int end) {
        if (end >= mPoints.size())
            return;
        for (int i = end; i >= start; i--) {
            mPoints.remove(i);
        }
    }

    // 只删除线的一部分
    public RichPathInfo[] divideLineByPoint(int pos) {
        if (mPoints.size() <= MAX_DELETE_LEN) {
            return null;
        }
        RichPathInfo[] infos = new RichPathInfo[3];
        infos[0] = this.clone();
        infos[1] = this.clone();
        infos[2] = this.clone();
        int start = Math.max(0, pos - MAX_DELETE_LEN / 2);
        int end = Math.min(mPoints.size() - 1, pos + MAX_DELETE_LEN / 2);
        infos[0].deletePoints(start, mPoints.size() - 1);
        infos[1].deletePoints(0, end);
        infos[2].deletePoints(0, start - 1);
        infos[2].deletePoints(end + 1, mPoints.size() - 1);
        return infos;
    }

    public int judgeLinePositionByPoint(PointF point) {
        return judgeLinePositionByPoint(point.x, point.y);
    }
}

