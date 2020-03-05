package com.load.big.bitmap;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Scroller;

import androidx.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;

/**
 * 自定义加载大图控件
 */
public class BigImageView extends View implements GestureDetector.OnGestureListener, View.OnTouchListener, GestureDetector.OnDoubleTapListener {


    /**
     * 图片的宽和高
     */
    private int mImageWidth,mImageHeight;
    /**
     * 当前View的宽和高
     */
    private int mViewWidth,mViewHeight;
    /**
     * 图片的缩放比
     */
    private float mOriginalScale,mCurrentScale;
    /**
     * 缩放矩阵
     */
    private Matrix mMatrix;
    /**
     * 滑动器
     */
    private Scroller mScroller;
    /**
     * 绘制区域
     */
    private Rect mRect;
    /**
     * 区域解码器
     */
    private BitmapRegionDecoder mDecoder;
    /**
     * 手势识别
     */
    private GestureDetector mGestureDetector;
    /**
     * 缩放监听
     */
    private final ScaleGestureDetector mCurrentScaleGestureDetector;
    private BitmapFactory.Options mOptions;
    private Bitmap mBitmap;

    public BigImageView(Context context) {
        this(context, null);
    }

    public BigImageView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BigImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        //第1步：设置bigImageView需要的成员变量
        //加载显示的区域
        mRect = new Rect();
        mOptions = new BitmapFactory.Options();
        //缩放器
        mMatrix = new Matrix();
        //手勢识别
        mGestureDetector = new GestureDetector(context, this);
        //滚动类
        mScroller = new Scroller(context);
        //缩放手势识别
        mCurrentScaleGestureDetector = new ScaleGestureDetector(context, new ScaleGesture() );
        //设置触摸监听
        setOnTouchListener(this);
    }

    //第2步：设置图片
    public void setImage(InputStream is) {
        //获取图片的信息，不会将整张图片加载到内存
        mOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(is, null, mOptions);
        //获取图片的宽高信息
        mImageWidth = mOptions.outWidth;
        mImageHeight = mOptions.outHeight;
        //开启内存复用
        mOptions.inMutable = true;
        //设置像素格式
        mOptions.inPreferredConfig = Bitmap.Config.RGB_565;
        //真正意义加载图片
        mOptions.inJustDecodeBounds = false;

        //创建一个区域解码器
        try {
            mDecoder = BitmapRegionDecoder.newInstance(is, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //刷新页面，与invalidate方法相反，只会触发onMeasure和onLayout方法，不会触发onDraw
        requestLayout();
    }

    //第3步：测量
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mViewWidth = getMeasuredWidth();
        mViewHeight = getMeasuredHeight();

        //确定加载图片的区域
        /*mRect.left = 0;
        mRect.top = 0;
        mRect.right = mImageWidth;
        //得到图片的宽度，就能根据view的宽度计算缩放比例
        mCurrentScale = mViewWidth/(float)mImageWidth;
        mRect.bottom = (int)(mViewHeight/mCurrentScale);*/

        //加了缩放手势之后的逻辑
        mRect.left = 0;
        mRect.top = 0;
        mRect.right = Math.min(mImageWidth, mViewWidth);
        mRect.bottom = Math.min(mImageHeight, mViewHeight);

        //再定义一个缩放因子
        mOriginalScale = mViewWidth / (float)mImageWidth;
        mCurrentScale = mOriginalScale;

    }

    //第4步：画出具体的内容
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mDecoder == null) {
            return;
        }
        //复用内存
        mBitmap = mDecoder.decodeRegion(mRect, mOptions);
        mOptions.inBitmap = mBitmap;
        //没有双击放大缩小的时候
        //mMatrix.setScale(mCurrentScale,mCurrentScale);
        //需要双击放大或缩小的时候
        mMatrix.setScale(mViewWidth/(float)mRect.width(),mViewWidth/(float)mRect.width());
        canvas.drawBitmap(mBitmap, mMatrix, null);
    }

    //第5步：处理点击事件
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        /*//直接将事件传递给手势事件处理
        return mGestureDetector.onTouchEvent(event);*/

        //直接将事件传递给手势事件
        mGestureDetector.onTouchEvent(event);
        //传递给双击事件
        mCurrentScaleGestureDetector.onTouchEvent(event);
        return true;
    }

    //第6步：手按下去，处理事件
    @Override
    public boolean onDown(MotionEvent e) {
        //如果移动没有停止，就强行停止
        if (!mScroller.isFinished()) {
            mScroller.forceFinished(true);
        }
        return true;
    }

    /**
     * 第7步：处理滑动事件
     * @param e1 处理开始事件，手指按下去，获取坐标
     * @param e2 当前事件
     * @param distanceX
     * @param distanceY
     * @return
     */
    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        //上下移动的时候，mRect需要改变现实区域
        mRect.offset((int)distanceX, (int)distanceY);

        handleBorder();

        //请求重绘View树，即onDraw方法，不会触发onMeasure和onLayout方法
        invalidate();
        return false;
    }

    /**
     * 处理滑动的边界情形
     */
    private void handleBorder() {
        //向上滑动边界处理
        if (mRect.bottom > mImageHeight) {
            mRect.bottom = mImageHeight;
            mRect.top = mImageHeight - (int)(mViewHeight/mCurrentScale);
        }
        //向下滑动边界处理
        if (mRect.top < 0) {
            mRect.top = 0;
            mRect.bottom = (int) (mViewHeight / mCurrentScale);
        }
        //向左滑动边界处理
        if (mRect.right > mImageWidth) {
            mRect.right = mImageWidth;
            mRect.left = mImageWidth - (int) (mViewWidth / mCurrentScale);
        }

        //向右滑动边界处理
        if (mRect.left < 0) {
            mRect.left = 0;
            mRect.right = (int) (mViewWidth / mCurrentScale);
        }
    }

    /**
     * 第8步：处理惯性问题
     * @param e1
     * @param e2
     * @param velocityX
     * @param velocityY
     * @return
     */
    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {

        mScroller.fling(mRect.left, mRect.top, (int)-velocityX, (int)-velocityY, 0, mImageWidth- (int)(mViewWidth/mCurrentScale),
                0, mImageHeight - (int)(mViewHeight/mCurrentScale));
        return false;
    }

    //处理结果
    @Override
    public void computeScroll() {
        if (mScroller.isFinished()) {
            return;
        }
        if (mScroller.computeScrollOffset()) {
            mRect.top = mScroller.getCurrY();
            mRect.bottom = mRect.top + (int)(mViewHeight/mCurrentScale);
        }
        if(mRect.bottom>mImageHeight) {
            mRect.top = mImageHeight - (int)(mViewHeight/mCurrentScale);
            mRect.bottom = mImageHeight;
        }
        invalidate();
    }

    @Override
    public void onShowPress(MotionEvent e) {}

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {}

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onDoubleTap(MotionEvent e) {
        //双击事件
        //双击是放大还是缩小，自己决定，此处定义为图片放大倍数小于1.5倍的时候，双击放大
        if (mCurrentScale < mOriginalScale * 1.5) {
            mCurrentScale = mOriginalScale * 2;
        } else {
            mCurrentScale = mOriginalScale;
        }
        mRect.right = mRect.left + (int)(mViewWidth/mCurrentScale);
        mRect.bottom = mRect.top + (int)(mViewHeight/mCurrentScale);

        //处理滑动的边界情形
        handleBorder();
        invalidate();
        return false;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent e) {
        return false;
    }

    class ScaleGesture extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scale = mCurrentScale;
            scale += detector.getScaleFactor() - 1;
            if (scale <= mOriginalScale) {
                scale = mOriginalScale;
            } else if (scale > mOriginalScale * 2) {//设置最大的放大倍数，自行设定
                scale = mOriginalScale * 2;
            }
            mRect.right = mRect.left + (int)(mViewWidth/scale);
            mRect.bottom = mRect.top + (int)(mViewHeight/scale);
            mCurrentScale = scale;
            invalidate();
            return true;
        }
    }
}
