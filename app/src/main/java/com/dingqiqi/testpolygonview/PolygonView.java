package com.dingqiqi.testpolygonview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by dingqiqi on 2016/9/13.
 */
public class PolygonView extends View {
    /**
     * 几边形
     */
    private float mPolygonCount = 5;
    /**
     * 初始角度
     */
    private double mInitAngle = 0;
    /**
     * 默认半径个数
     */
    private float mRadiusCount = 5;
    /**
     * 半径
     */
    private int mRadius;
    private int mInitRadius;
    /**
     * 平均角度
     */
    private double mAvgAngle;
    /**
     * 多边形画线画笔
     */
    private Paint mPaint;
    /**
     * 多边形路径
     */
    private Path mPath;
    /**
     * 顶点数据
     */
    private List<StopXY> mList;

    private String[] mText = new String[]{"A", "B", "C", "D", "E", "F", "", "", ""};
    /**
     * 文字画笔
     */
    private Paint mPaintText;
    /**
     * 文字大小
     */
    private int mTextSize = 12;
    /**
     * 上下文
     */
    private Context mContext;
    /**
     * 是否画值
     */
    private boolean mIsValue = false;
    /**
     * 值画笔
     */
    private Paint mPaintValue;
    /**
     * 值百分比
     */
    private float[] mValue = new float[]{0.2f, 0.8f, 0.5f, 0.9f, 0.6f};
    /**
     * 计算文字大小
     */
    private Rect mTextBounds;
    /**
     * 手势动作类型常量
     */
    private int NONE = 0;
    private int DRAG = 1;
    private int ZOOM = 2;
    /**
     * 模式（拖拽还是放大）
     */
    private int mMode = NONE;
    /**
     * 屏幕宽度
     */
    private int mWidth;
    /**
     * 两点按下距离(用于放大倍数)
     */
    private float mOrderDris;
    private float mNewDris;
    /**
     * 记录按下滑动位置，用于滑动
     */
    private float mStartX, mStartY;
    private float mCurX, mCurY;
    /**
     * 滑动边界值
     */
    private float mPaddingLeft, mPaddingRight, mPaddingTop, mPaddingBottom;

    public PolygonView(Context context) {
        super(context, null);
    }

    public PolygonView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        manager.getDefaultDisplay().getMetrics(metrics);
        mWidth = metrics.widthPixels;

        initView();
    }

    /**
     * 初始化控件
     */
    private void initView() {
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStrokeWidth(4);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(Color.BLACK);

        mPaintText = new Paint();
        mPaintText.setAntiAlias(true);
        mPaintText.setStyle(Paint.Style.FILL);
        mPaintText.setTextSize(dp2px(mTextSize));
        mPaintText.setColor(Color.RED);

        mPaintValue = new Paint(mPaintText);
        mPaintValue.setColor(Color.parseColor("#663F51B5"));

        //计算平均弧度
        mAvgAngle = 2 * Math.PI / mPolygonCount;
        if (mPolygonCount % 2 != 0) {
            mInitAngle = 180 / mPolygonCount;
        }

        mPath = new Path();
        mList = new ArrayList<>();
        mTextBounds = new Rect();
    }

    /**
     * 是否画值
     *
     * @param isValue
     */
    public void setIsValue(boolean isValue) {
        this.mIsValue = isValue;
        invalidate();
    }

    /**
     * 设置值百分比(0-1)
     *
     * @param value
     */
    public void setValue(float[] value) {
        if (value == null) {
            throw new IllegalArgumentException("不能为空");
        }

        for (int i = 0; i < value.length; i++) {
            if (value[i] < 0f || value[i] > 1f) {
                throw new IllegalArgumentException("百分比只能0-1");
            }
        }

        this.mValue = value;
        mIsValue = true;
        invalidate();
    }

    /**
     * 设置多边形边数机文字信息
     *
     * @param count 边数
     * @param text  多边文字（个数与多边形对应）
     */
    public void setPolygonCountText(int count, String[] text) {
        if (mText == null || text.length < count) {
            throw new IllegalArgumentException("文字数组长度要与边数对应");
        }
        this.mPolygonCount = count;
        mText = text;

        mAvgAngle = 2 * Math.PI / mPolygonCount;
        if (mPolygonCount % 2 != 0) {
            mInitAngle = 180 / mPolygonCount;
        }

        invalidate();
    }

    /**
     * 多边形内圈数
     *
     * @param count 圈数
     */
    public void setRadiusCount(float count) {
        if (count < 0) {
            throw new IllegalArgumentException("圈数要大于等于0");
        }
        this.mRadiusCount = count;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(getSpec(widthMeasureSpec, 1), getSpec(heightMeasureSpec, 2));
        //获取半径
        mRadius = getMeasuredHeight() > getMeasuredWidth() ? getMeasuredWidth() / 2 : getMeasuredHeight() / 2;
        //防止画出去
        mRadius = mRadius - dp2px(50);
        if (mInitRadius == 0) {
            mInitRadius = mRadius;
        }
    }

    /**
     * 设置布局宽高
     *
     * @param spec
     * @param flag
     */
    private int getSpec(int spec, int flag) {
        int value;
        if (flag == 1) {
            value = getMeasuredWidth() + getPaddingLeft() + getPaddingRight();
        } else {
            value = getMeasuredHeight() + getPaddingTop() + getPaddingBottom();
        }
        int mode = MeasureSpec.getMode(spec);
        int size = MeasureSpec.getSize(spec);

        if (mode == MeasureSpec.EXACTLY) {
            value = size;
        }

        return value;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.save();
        //以多边形中心为原点
        canvas.translate(getMeasuredWidth() / 2, getMeasuredHeight() / 2);

        //画线
        mList.clear();
        mPaddingLeft = mPaddingRight = mPaddingTop = mPaddingBottom = 0;
        for (int i = 0; i < mPolygonCount; i++) {
            //计算弧度
            double angle = mInitAngle * Math.PI / 180 + mAvgAngle * i;
            float stopX = (float) (mRadius * Math.sin(angle));
            float stopY = (float) (mRadius * Math.cos(angle));

            //计算上下左右能移动距离
            if (stopX < mPaddingLeft) {
                mPaddingLeft = stopX;
            }

            if (stopX > mPaddingRight) {
                mPaddingRight = stopX;
            }

            if (stopY < mPaddingTop) {
                mPaddingTop = stopY;
            }

            if (stopY > mPaddingBottom) {
                mPaddingBottom = stopY;
            }

            StopXY stopXY = new StopXY(stopX, stopY);
            mList.add(stopXY);

            canvas.drawLine(0, 0, stopX, stopY, mPaint);
            //获取文字宽高
            mPaintText.getTextBounds(mText[i], 0, mText[i].length(), mTextBounds);

            //计算文字距离边间距
            stopX = (float) (1.05 * stopX);
            stopY = (float) (1.05 * stopY);

            if (stopX < 0) {
                //左边文字
                stopX = stopX - mTextBounds.width();
            } else if (stopX < mRadius / 4) {
                //中间文字，会有偏差，小与1/4就算0
                stopX = stopX - mTextBounds.width() / 2;
            }

            //上边上移文字高度
            if (stopY > 0.0f) {
                stopY = stopY + mTextBounds.height();
            }
            //画文字
            canvas.drawText(mText[i], stopX, stopY, mPaintText);
        }

        //画边
        for (int i = 1; i <= mRadiusCount; i++) {
            //半径百分比
            float count = i / mRadiusCount;
            mPath.reset();
            //移动到初始点
            mPath.moveTo(mList.get(0).getStopX() * count, mList.get(0).getStopY() * count);
            //循环连线
            for (int j = 1; j < mList.size(); j++) {
                mPath.lineTo(mList.get(j).getStopX() * count, mList.get(j).getStopY() * count);
            }
            //连接到初始点
            mPath.lineTo(mList.get(0).getStopX() * count, mList.get(0).getStopY() * count);
            //画线
            canvas.drawPath(mPath, mPaint);
        }

        if (mIsValue) {
            mPath.reset();
            //移动到初始点
            mPath.moveTo(mList.get(0).getStopX() * mValue[0], mList.get(0).getStopY() * mValue[0]);
            //循环连线
            for (int j = 1; j < mList.size(); j++) {
                mPath.lineTo(mList.get(j).getStopX() * mValue[j], mList.get(j).getStopY() * mValue[j]);
            }
            //连接到初始点
            mPath.lineTo(mList.get(0).getStopX() * mValue[0], mList.get(0).getStopY() * mValue[0]);
            //画线
            canvas.drawPath(mPath, mPaintValue);
        }

        canvas.restore();
    }

    /**
     * 存储点实体类
     */
    private class StopXY {
        private float stopX;
        private float stopY;

        public StopXY(float stopX, float stopY) {
            this.stopX = stopX;
            this.stopY = stopY;
        }

        public float getStopX() {
            return stopX;
        }

        public float getStopY() {
            return stopY;
        }
    }

    /**
     * dp 转px
     *
     * @param value
     * @return
     */
    private int dp2px(int value) {
        return (int) (mContext.getResources().getDisplayMetrics().density * value + 0.5);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                //按下一个点算作滑动
                mMode = DRAG;
                mStartX = event.getX();
                mStartY = event.getY();

                break;
            case MotionEvent.ACTION_MOVE:
                //移动
                mCurX = event.getX();
                mCurY = event.getY();
                if (mMode == DRAG) {
                    // Log.i("aaa", getScrollX() + " " + getScrollY() + " " + getLeft() + " " + getRight());
                    //Log.i("aaa", mPaddingLeft + " " + mPaddingRight + " " + mPaddingTop + " " + mPaddingBottom);
                    //移动距离
                    int moveX = (int) (mStartX - mCurX);
                    int moveY = (int) (mStartY - mCurY);

                    //如果超过允许滑动距离，那个方向滑动值为0
                    if (getScrollX() < mPaddingLeft) {
                        if (mStartX - mCurX < 0) {
                            moveX = 0;
                        }
                    }
                    if (getScrollX() > mPaddingRight) {
                        if (mStartX - mCurX > 0) {
                            moveX = 0;
                        }
                    }
                    if (getScrollY() < mPaddingTop) {
                        if (mStartY - mCurY < 0) {
                            moveY = 0;
                        }
                    }
                    if (getScrollY() > mPaddingBottom) {
                        if (mStartY - mCurY > 0) {
                            moveY = 0;
                        }
                    }
                    //在当前点上移动
                    scrollBy(moveX, moveY);

                    mStartX = mCurX;
                    mStartY = mCurY;
                } else if (mMode == ZOOM) {
                    //放大
                    mNewDris = distance(event);
                    //达到一定的范围才放大缩小
                    if (mNewDris > 10) {
                        //放大倍数
                        float scale = mNewDris / mOrderDris;
                        //Log.i("aaa", "zoom" + (scale - 1));
                        //(scale - 1)  小于1就缩小，大于1放大
                        //按照半径的2/3进行放大缩小（这个自己调整，看效果）
                        mRadius = (int) (mRadius + (scale - 1) * mRadius * 2 / 3);
                        //控制放大限制
                        if (mRadius > mWidth) {
                            mRadius = mWidth;
                            //控制缩小限制
                        } else if (mRadius < mInitRadius) {
                            //缩小到边界值时，图形初始化到中心
                            scrollTo(0, 0);
                            mRadius = mInitRadius;
                        }
                        invalidate();
                        mOrderDris = mNewDris;
                    }
                }
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                //按下两个点算作放大缩小
                mOrderDris = distance(event);
                mMode = ZOOM;
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                //抬起
                mMode = NONE;
                break;
        }

        return true;
    }

    /**
     * 计算两个触摸点之间的距离
     * @param event
     * @return
     */
    private float distance(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    /**
     * 计算两个触摸点的中点
     * @param event
     * @return
     */
    private PointF middle(MotionEvent event) {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        return new PointF(x / 2, y / 2);
    }

}
