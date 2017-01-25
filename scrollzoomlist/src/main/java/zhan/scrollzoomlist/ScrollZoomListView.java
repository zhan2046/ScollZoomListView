package zhan.scrollzoomlist;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.support.annotation.NonNull;
import android.support.v4.view.GestureDetectorCompat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.animation.DecelerateInterpolator;
import android.widget.ListView;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by ruzhan on 17/1/9.
 */
public class ScrollZoomListView extends ListView {

  private static final int INVALID_POINTER_ID = -1;

  private static final float DEFAULT_MIN_ZOOM_SCALE = 0.4f;
  private static final float DEFAULT_MAX_ZOOM_SCALE = 2.0f;

  private static final float DEFAULT_NORMAL_SCALE = 1.0f;
  private static final float DEFAULT_ZOOM_SCALE = 2.0f;

  private static final int DEFAULT_ZOOM_TO_SMALL_TIMES = 6;
  private static final int DEFAULT_ZOOM_SCALE_DURATION = 300;
  private static final int DEFAULT_ZOOM_TO_SMALL_SCALE_DURATION = 500;

  private static final int UN_LOADED_POINT = 10000;
  private static final int LOADED_POINT = 10001;

  private static int mActivePointerId = INVALID_POINTER_ID;

  private float mScaleFactor = DEFAULT_NORMAL_SCALE;
  private float mLastScale = DEFAULT_NORMAL_SCALE;

  private int mLoadedPointFlag = UN_LOADED_POINT;

  private float mMinZoomScale;
  private float mMaxZoomScale;

  private float mNormalScale;
  private float mZoomScale;

  private int mZoomToSmallTimes;
  private int mZoomScaleDuration;
  private int mZoomToSmallScaleDuration;

  private ScaleGestureDetector mScaleDetector;
  private GestureDetectorCompat mGestureDetectorCompat;

  private float maxWidth = 0.0f;
  private float maxHeight = 0.0f;

  private float mLastTouchX;
  private float mLastTouchY;

  private float mTranslateX;
  private float mTranslateY;

  private float mListViewWidth;
  private float mListViewHeight;

  private float mCenterX;
  private float mCenterY;

  private boolean isScaling = false;
  private boolean isPointerDown = false;

  private ValueAnimator mZoomValueAnimator;

  //synchronous ListView Zoom ScaleGestureDetector
  private List<ScaleGestureDetector.SimpleOnScaleGestureListener> mOnScaleGestureListeners = new ArrayList<>();

  //synchronous ListView Zoom GestureDetector
  private List<GestureDetector.SimpleOnGestureListener> mSimpleOnGestureListeners = new ArrayList<>();

  //synchronous ListView Zoom Animation
  private List<OnListViewZoomListener> mOnListViewZoomListeners = new ArrayList<>();

  private LinkedList<PointF> mLinkPoints = new LinkedList<>();

  public ScrollZoomListView(Context context) {
    this(context, null);
  }

  public ScrollZoomListView(Context context, AttributeSet attr) {
    super(context, attr);
    init(attr);
  }

  private void init(AttributeSet attr) {
    mScaleDetector = new ScaleGestureDetector(getContext(), new ScaleListener());
    mGestureDetectorCompat =
        new GestureDetectorCompat(getContext(), new ScrollReaderViewGestureListener());

    TypedArray a = getContext().obtainStyledAttributes(attr, R.styleable.ScrollZoomListView, 0, 0);

    mMinZoomScale =
        a.getFloat(R.styleable.ScrollZoomListView_min_zoom_scale, DEFAULT_MIN_ZOOM_SCALE);
    mMaxZoomScale =
        a.getFloat(R.styleable.ScrollZoomListView_max_zoom_scale, DEFAULT_MAX_ZOOM_SCALE);
    mNormalScale = a.getFloat(R.styleable.ScrollZoomListView_normal_scale, DEFAULT_NORMAL_SCALE);
    mZoomScale = a.getFloat(R.styleable.ScrollZoomListView_zoom_scale, DEFAULT_ZOOM_SCALE);

    mZoomToSmallTimes = a.getInteger(R.styleable.ScrollZoomListView_zoom_to_small_times,
        DEFAULT_ZOOM_TO_SMALL_TIMES);
    mZoomScaleDuration = a.getInteger(R.styleable.ScrollZoomListView_zoom_scale_duration,
        DEFAULT_ZOOM_SCALE_DURATION);
    mZoomToSmallScaleDuration =
        a.getInteger(R.styleable.ScrollZoomListView_zoom_to_small_scale_duration,
            DEFAULT_ZOOM_TO_SMALL_SCALE_DURATION);

    a.recycle();
  }

  @Override protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();

    if (mZoomValueAnimator != null) {
      mZoomValueAnimator.cancel();
    }

    //remove all listener
    removeOnScaleGestureListeners();
    removeOnSimpleOnGestureListeners();
    removeOnListViewZoomListeners();
  }

  @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

    mListViewWidth = MeasureSpec.getSize(widthMeasureSpec);
    mListViewHeight = MeasureSpec.getSize(heightMeasureSpec);

    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
  }

  @Override public boolean onTouchEvent(@NonNull MotionEvent ev) {

    mScaleDetector.onTouchEvent(ev);

    mGestureDetectorCompat.onTouchEvent(ev);

    int action = ev.getAction();
    switch (action & MotionEvent.ACTION_MASK) {
      case MotionEvent.ACTION_DOWN: {

        isPointerDown = false;

        mLastTouchX = ev.getX();
        mLastTouchY = ev.getY();

        mActivePointerId = ev.getPointerId(0);
        break;
      }

      case MotionEvent.ACTION_POINTER_DOWN: {

        isPointerDown = true;
        break;
      }

      case MotionEvent.ACTION_MOVE: {

        int pointerIndex = ev.findPointerIndex(mActivePointerId);

        float x, y;

        try {

          x = ev.getX(pointerIndex);
          y = ev.getY(pointerIndex);
        } catch (IllegalArgumentException ex) {
          ex.printStackTrace();

          return super.onTouchEvent(ev);
        }

        float dx = (x - mLastTouchX);
        float dy = (y - mLastTouchY);

        //ACTION_POINTER_DOWN ListView more distance * 6
        if (isPointerDown) {
          dx = dx * mZoomToSmallTimes;
          dy = dy * mZoomToSmallTimes;
        }

        if (isScaling) {  //ListView status is scaling
          float offsetX = mCenterX * (mLastScale - mScaleFactor);
          float offsetY = mCenterY * (mLastScale - mScaleFactor);

          //mTranslateX += offsetX;
          //mTranslateY += offsetY;

          //checkPointF(UN_LOADED_POINT, offsetX, offsetY);

          //mLastScale = mScaleFactor;
        } else if (mScaleFactor > mNormalScale) {   //ListView not scaling, move ...
          mTranslateX += dx;
          mTranslateY += dy;

          checkPointF(UN_LOADED_POINT, dx, dy);

          correctTranslateValue();
        }

        mLastTouchX = x;
        mLastTouchY = y;

        invalidate();
        break;
      }

      case MotionEvent.ACTION_UP:
      case MotionEvent.ACTION_CANCEL: {

        mActivePointerId = INVALID_POINTER_ID;

        break;
      }

      case MotionEvent.ACTION_POINTER_UP: {

        int pointerIndex = (action & MotionEvent.ACTION_POINTER_INDEX_MASK)
            >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
        int pointerId = ev.getPointerId(pointerIndex);

        if (pointerId == mActivePointerId) {
          int newPointerIndex = pointerIndex == 0 ? 1 : 0;
          mLastTouchX = ev.getX(newPointerIndex);
          mLastTouchY = ev.getY(newPointerIndex);
          mActivePointerId = ev.getPointerId(newPointerIndex);
        }

        break;
      }
    }

    return super.onTouchEvent(ev);
  }

  private void correctTranslateValue() {
    if (mTranslateX > 0.0f) {
      mTranslateX = 0.0f;
    } else if (mTranslateX < maxWidth) {
      mTranslateX = maxWidth;
    }

    if (mTranslateY > 0.0f) {
      mTranslateY = 0.0f;
    } else if (mTranslateY < maxHeight) {
      mTranslateY = maxHeight;
    }
  }

  @Override protected void dispatchDraw(@NonNull Canvas canvas) {

    canvas.save(Canvas.MATRIX_SAVE_FLAG);
    canvas.translate(mTranslateX, mTranslateY);
    canvas.scale(mScaleFactor, mScaleFactor);

    super.dispatchDraw(canvas);

    canvas.restore();
  }

  private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
    @Override public boolean onScaleBegin(ScaleGestureDetector detector) {

      for (ScaleGestureDetector.OnScaleGestureListener listener : mOnScaleGestureListeners) {
        listener.onScaleBegin(detector);
      }

      return super.onScaleBegin(detector);
    }

    @Override public boolean onScale(ScaleGestureDetector detector) {

      mScaleFactor *= detector.getScaleFactor();

      float minFactor = Math.min(mScaleFactor, mMaxZoomScale);
      mScaleFactor = Math.max(mMinZoomScale, minFactor);

      maxWidth = mListViewWidth - (mListViewWidth * mScaleFactor);
      maxHeight = mListViewHeight - (mListViewHeight * mScaleFactor);

      mCenterX = detector.getFocusX();
      mCenterY = detector.getFocusY();

      float offsetX = mCenterX * (mLastScale - mScaleFactor);
      float offsetY = mCenterY * (mLastScale - mScaleFactor);

      mTranslateX += offsetX;
      mTranslateY += offsetY;

      checkPointF(UN_LOADED_POINT, offsetX, offsetY);

      mLastScale = mScaleFactor;

      isScaling = true;

      invalidate();

      for (ScaleGestureDetector.OnScaleGestureListener listener : mOnScaleGestureListeners) {
        listener.onScale(detector);
      }
      return true;
    }

    @Override public void onScaleEnd(ScaleGestureDetector detector) {

      if (mScaleFactor < mNormalScale) {
        zoomList(mScaleFactor, mNormalScale, mZoomToSmallScaleDuration, LOADED_POINT);
      }

      isScaling = false;

      for (ScaleGestureDetector.OnScaleGestureListener listener : mOnScaleGestureListeners) {
        listener.onScaleEnd(detector);
      }
    }
  }

  private class ScrollReaderViewGestureListener extends GestureDetector.SimpleOnGestureListener {

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {

      //list view scroll call back... to outside
      for (GestureDetector.SimpleOnGestureListener listener : mSimpleOnGestureListeners) {
        listener.onScroll(e1, e2, distanceX, distanceY);
      }

      return super.onScroll(e1, e2, distanceX, distanceY);
    }

    @Override public boolean onSingleTapConfirmed(MotionEvent e) {  // single click event,double call no call single

      for (GestureDetector.SimpleOnGestureListener listener : mSimpleOnGestureListeners) {
        listener.onSingleTapConfirmed(e);
      }
      return super.onSingleTapConfirmed(e);
    }

    @Override public boolean onDoubleTap(MotionEvent e) {  //double click event

      if (mNormalScale < mScaleFactor) {
        zoomList(mScaleFactor, mNormalScale, mZoomScaleDuration, LOADED_POINT);

      } else if (mScaleFactor == mNormalScale) {

        mCenterX = e.getX();
        mCenterY = e.getY();

        zoomList(mScaleFactor, mZoomScale, mZoomScaleDuration, UN_LOADED_POINT);
      }

      for (GestureDetector.SimpleOnGestureListener listener : mSimpleOnGestureListeners) {
        listener.onDoubleTap(e);
      }
      return super.onDoubleTap(e);
    }
  }

  //let ListView zoom func
  private void zoomList(float startValue, float endValue, int duration, final int loadedPointFlag) {
    if (mZoomValueAnimator == null) {
      mZoomValueAnimator = new ValueAnimator();
      mZoomValueAnimator.setInterpolator(new DecelerateInterpolator());

      mZoomValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
        @Override public void onAnimationUpdate(ValueAnimator animation) {

          mScaleFactor = (Float) animation.getAnimatedValue();

          float dx = mCenterX * (mLastScale - mScaleFactor);
          float dy = mCenterY * (mLastScale - mScaleFactor);

          PointF pointF = checkPointF(loadedPointFlag, dx, dy);

          if(pointF != null) {
            dx = -pointF.x;
            dy = -pointF.y;
          }

          mTranslateX += dx;
          mTranslateY += dy;


          maxWidth = mListViewWidth - (mListViewWidth * mScaleFactor);
          maxHeight = mListViewHeight - (mListViewHeight * mScaleFactor);

          correctZoomTranslateValue();

          invalidate();

          mLastScale = mScaleFactor;

          for (OnListViewZoomListener listener : mOnListViewZoomListeners) {
            listener.onListViewZoomUpdate(animation, mTranslateX, mTranslateY,
                mScaleFactor, mScaleFactor);
          }
        }
      });

      mZoomValueAnimator.addListener(new Animator.AnimatorListener() {
        @Override public void onAnimationStart(Animator animation) {
          isScaling = true;

          for (OnListViewZoomListener listener : mOnListViewZoomListeners) {
            listener.onListViewStart();
          }
        }

        @Override public void onAnimationEnd(Animator animation) {
          isScaling = false;

          mLoadedPointFlag = loadedPointFlag == UN_LOADED_POINT ? LOADED_POINT : UN_LOADED_POINT;

          for (OnListViewZoomListener listener : mOnListViewZoomListeners) {
            listener.onListViewCancel();
          }
        }

        @Override public void onAnimationCancel(Animator animation) {
          isScaling = false;

          for (OnListViewZoomListener listener : mOnListViewZoomListeners) {
            listener.onListViewCancel();
          }
        }

        @Override public void onAnimationRepeat(Animator animation) {

        }
      });
    }

    if (!mZoomValueAnimator.isRunning()) {
      mZoomValueAnimator.setFloatValues(startValue, endValue);
      mZoomValueAnimator.setDuration(duration);
      mZoomValueAnimator.start();
    }
  }

  private void correctZoomTranslateValue() {
    if (mTranslateX > 0.0f) { //zoom +

      if (mScaleFactor >= mNormalScale) {  //params correct
        mTranslateX = 0.0f;
      }
    } else if (mTranslateX < maxWidth) { //zoom -

      if (mScaleFactor >= mNormalScale) { //params correct
        mTranslateX = maxWidth;
      }
    }

    if (mTranslateY > 0.0f) { //zoom +

      if (mScaleFactor >= mNormalScale) {  //params correct
        mTranslateY = 0.0f;
      }
    } else if (mTranslateY < maxHeight) { //zoom -

      if (mScaleFactor >= mNormalScale) { //params correct
        mTranslateY = maxHeight;
      }
    }
  }

  private void putPointF(float dx, float dy) {
    if(mLoadedPointFlag == UN_LOADED_POINT) {
      PointF pointF = new PointF(dx, dy);
      mLinkPoints.addFirst(pointF);
    }
  }

  private PointF getPointF() {
    if(mLoadedPointFlag == LOADED_POINT) {
      return mLinkPoints.getLast();
    }

    return null;
  }

  private PointF checkPointF(int loadedPointFlag, float dx, float dy) {
    PointF pointF = null;

    if(loadedPointFlag == UN_LOADED_POINT) {
      putPointF(dx, dy);

    }else if(loadedPointFlag == LOADED_POINT) {
      pointF = getPointF();

    }else {
      throw new RuntimeException("ZoomListView loaded points error ! ! !");
    }
    return pointF;
  }

  public float getMinZoomScale() {
    return mMinZoomScale;
  }

  public void setMinZoomScale(float mMinZoomScale) {
    this.mMinZoomScale = mMinZoomScale;
  }

  public float getMaxZoomScale() {
    return mMaxZoomScale;
  }

  public void setMaxZoomScale(float mMaxZoomScale) {
    this.mMaxZoomScale = mMaxZoomScale;
  }

  public float getNormalScale() {
    return mNormalScale;
  }

  public void setNormalScale(float mNormalScale) {
    this.mNormalScale = mNormalScale;
  }

  public float getZoomScale() {
    return mZoomScale;
  }

  public void setZoomScale(float mZoomScale) {
    this.mZoomScale = mZoomScale;
  }

  public int getZoomToSmallTimes() {
    return mZoomToSmallTimes;
  }

  public void setZoomToSmallTimes(int mZoomToSmallTimes) {
    this.mZoomToSmallTimes = mZoomToSmallTimes;
  }

  public int getZoomScaleDuration() {
    return mZoomScaleDuration;
  }

  public void setZoomScaleDuration(int mZoomScaleDuration) {
    this.mZoomScaleDuration = mZoomScaleDuration;
  }

  public void addOnScaleGestureListener(ScaleGestureDetector.SimpleOnScaleGestureListener listener) {
    if(listener != null) {
      if(!mOnScaleGestureListeners.contains(listener)) {
        mOnScaleGestureListeners.add(listener);
      }
    }
  }

  public void removeOnScaleGestureListener(ScaleGestureDetector.SimpleOnScaleGestureListener listener) {
    if(listener != null) {
      if(mOnScaleGestureListeners.contains(listener)) {
        mOnScaleGestureListeners.remove(listener);
      }
    }
  }

  public void removeOnScaleGestureListeners() {
    while (!mOnScaleGestureListeners.isEmpty()) {
      mOnScaleGestureListeners.remove(0);
    }
  }

  public void setSimpleOnGestureListener(GestureDetector.SimpleOnGestureListener listener) {
    if(listener != null) {
      if(!mSimpleOnGestureListeners.contains(listener)) {
        mSimpleOnGestureListeners.add(listener);
      }
    }
  }

  public void removeOnSimpleOnGestureListener(GestureDetector.SimpleOnGestureListener listener) {
    if(listener != null) {
      if(mSimpleOnGestureListeners.contains(listener)) {
        mSimpleOnGestureListeners.remove(listener);
      }
    }
  }

  public void removeOnSimpleOnGestureListeners() {
    while (!mSimpleOnGestureListeners.isEmpty()) {
      mSimpleOnGestureListeners.remove(0);
    }
  }

  public void setOnListViewZoomListener(OnListViewZoomListener listener) {
    if(listener != null) {
      if(!mOnListViewZoomListeners.contains(listener)) {
        mOnListViewZoomListeners.add(listener);
      }
    }
  }

  public void removeOnListViewZoomListener(OnListViewZoomListener listener) {
    if(listener != null) {
      if(mOnListViewZoomListeners.contains(listener)) {
        mOnListViewZoomListeners.remove(listener);
      }
    }
  }

  public void removeOnListViewZoomListeners() {
    while (!mOnListViewZoomListeners.isEmpty()) {
      mOnListViewZoomListeners.remove(0);
    }
  }

  public interface OnListViewZoomListener {

    void onListViewZoomUpdate(ValueAnimator animation, float translateX, float translateY,
        float scaleX, float scaleY);

    void onListViewStart();

    void onListViewCancel();
  }
}