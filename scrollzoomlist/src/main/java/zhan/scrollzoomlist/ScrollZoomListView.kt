package zhan.scrollzoomlist

import android.animation.Animator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.PointF
import android.support.v4.view.GestureDetectorCompat
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.animation.DecelerateInterpolator
import android.widget.ListView
import java.util.*

class ScrollZoomListView @JvmOverloads constructor(context: Context, attr: AttributeSet? = null) :
        ListView(context, attr) {

    companion object {
        private const val INVALID_POINTER_ID = -1

        private const val DEFAULT_MIN_ZOOM_SCALE = 0.4f
        private const val DEFAULT_MAX_ZOOM_SCALE = 2.0f

        private const val DEFAULT_NORMAL_SCALE = 1.0f
        private const val DEFAULT_ZOOM_SCALE = 2.0f

        private const val DEFAULT_ZOOM_TO_SMALL_TIMES = 6
        private const val DEFAULT_ZOOM_SCALE_DURATION = 300
        private const val DEFAULT_ZOOM_TO_SMALL_SCALE_DURATION = 500

        private const val UN_LOADED_POINT = 10000
        private const val LOADED_POINT = 10001

        private var mActivePointerId = INVALID_POINTER_ID
    }

    private var mScaleFactor = DEFAULT_NORMAL_SCALE
    private var mLastScale = DEFAULT_NORMAL_SCALE

    private var mLoadedPointFlag = UN_LOADED_POINT

    var minZoomScale: Float = 0.toFloat()
    var maxZoomScale: Float = 0.toFloat()

    var normalScale: Float = 0.toFloat()
    var zoomScale: Float = 0.toFloat()

    var zoomToSmallTimes: Int = 0
    var zoomScaleDuration: Int = 0
    private var mZoomToSmallScaleDuration: Int = 0

    private var mScaleDetector: ScaleGestureDetector? = null
    private var mGestureDetectorCompat: GestureDetectorCompat? = null

    private var maxWidth = 0.0f
    private var maxHeight = 0.0f

    private var mLastTouchX: Float = 0.toFloat()
    private var mLastTouchY: Float = 0.toFloat()

    private var mTranslateX: Float = 0.toFloat()
    private var mTranslateY: Float = 0.toFloat()

    private var mListViewWidth: Float = 0.toFloat()
    private var mListViewHeight: Float = 0.toFloat()

    private var mCenterX: Float = 0.toFloat()
    private var mCenterY: Float = 0.toFloat()

    private var isScaling = false
    private var isPointerDown = false

    private var mZoomValueAnimator: ValueAnimator? = null

    //synchronous ListView Zoom ScaleGestureDetector
    private val mOnScaleGestureListeners = ArrayList<ScaleGestureDetector.SimpleOnScaleGestureListener>()

    //synchronous ListView Zoom GestureDetector
    private val mSimpleOnGestureListeners = ArrayList<GestureDetector.SimpleOnGestureListener>()

    //synchronous ListView Zoom Animation
    private val mOnListViewZoomListeners = ArrayList<OnListViewZoomListener>()

    private val mLinkPoints = LinkedList<PointF>()

    init {
        init(attr)
    }

    private fun init(attr: AttributeSet?) {
        mScaleDetector = ScaleGestureDetector(context, ScaleListener())
        mGestureDetectorCompat = GestureDetectorCompat(context, ScrollReaderViewGestureListener())

        val a = context.obtainStyledAttributes(attr, R.styleable.ScrollZoomListView, 0, 0)

        minZoomScale = a.getFloat(R.styleable.ScrollZoomListView_min_zoom_scale, DEFAULT_MIN_ZOOM_SCALE)
        maxZoomScale = a.getFloat(R.styleable.ScrollZoomListView_max_zoom_scale, DEFAULT_MAX_ZOOM_SCALE)
        normalScale = a.getFloat(R.styleable.ScrollZoomListView_normal_scale, DEFAULT_NORMAL_SCALE)
        zoomScale = a.getFloat(R.styleable.ScrollZoomListView_zoom_scale, DEFAULT_ZOOM_SCALE)

        zoomToSmallTimes = a.getInteger(R.styleable.ScrollZoomListView_zoom_to_small_times,
                DEFAULT_ZOOM_TO_SMALL_TIMES)
        zoomScaleDuration = a.getInteger(R.styleable.ScrollZoomListView_zoom_scale_duration,
                DEFAULT_ZOOM_SCALE_DURATION)
        mZoomToSmallScaleDuration = a.getInteger(R.styleable.ScrollZoomListView_zoom_to_small_scale_duration,
                DEFAULT_ZOOM_TO_SMALL_SCALE_DURATION)

        a.recycle()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (mZoomValueAnimator != null) {
            mZoomValueAnimator!!.cancel()
        }
        //remove all listener
        removeOnScaleGestureListeners()
        removeOnSimpleOnGestureListeners()
        removeOnListViewZoomListeners()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        mListViewWidth = MeasureSpec.getSize(widthMeasureSpec).toFloat()
        mListViewHeight = MeasureSpec.getSize(heightMeasureSpec).toFloat()
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent): Boolean {
        mScaleDetector!!.onTouchEvent(ev)
        mGestureDetectorCompat!!.onTouchEvent(ev)
        val action = ev.action
        when (action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                isPointerDown = false
                mLastTouchX = ev.x
                mLastTouchY = ev.y

                mActivePointerId = ev.getPointerId(0)
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                isPointerDown = true
            }
            MotionEvent.ACTION_MOVE -> {
                val pointerIndex = ev.findPointerIndex(mActivePointerId)
                val x: Float
                val y: Float
                try {
                    x = ev.getX(pointerIndex)
                    y = ev.getY(pointerIndex)
                } catch (ex: IllegalArgumentException) {
                    ex.printStackTrace()
                    return super.onTouchEvent(ev)
                }
                var dx = x - mLastTouchX
                var dy = y - mLastTouchY

                //ACTION_POINTER_DOWN ListView more distance * 6
                if (isPointerDown) {
                    dx *= zoomToSmallTimes
                    dy *= zoomToSmallTimes
                }
                if (isScaling) {  //ListView status is scaling
                    //val offsetX = mCenterX * (mLastScale - mScaleFactor)
                    //val offsetY = mCenterY * (mLastScale - mScaleFactor)

                    //mTranslateX += offsetX;
                    //mTranslateY += offsetY;

                    //checkPointF(UN_LOADED_POINT, offsetX, offsetY);

                    //mLastScale = mScaleFactor;
                } else if (mScaleFactor > normalScale) {   //ListView not scaling, move ...
                    mTranslateX += dx
                    mTranslateY += dy
                    checkPointF(UN_LOADED_POINT, dx, dy)
                    correctTranslateValue()
                }
                mLastTouchX = x
                mLastTouchY = y
                invalidate()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                mActivePointerId = INVALID_POINTER_ID
            }
            MotionEvent.ACTION_POINTER_UP -> {
                val pointerIndex = action and MotionEvent.ACTION_POINTER_INDEX_MASK shr MotionEvent.ACTION_POINTER_INDEX_SHIFT
                val pointerId = ev.getPointerId(pointerIndex)
                if (pointerId == mActivePointerId) {
                    val newPointerIndex = if (pointerIndex == 0) 1 else 0
                    mLastTouchX = ev.getX(newPointerIndex)
                    mLastTouchY = ev.getY(newPointerIndex)
                    mActivePointerId = ev.getPointerId(newPointerIndex)
                }
            }
        }
        return super.onTouchEvent(ev)
    }

    private fun correctTranslateValue() {
        if (mTranslateX > 0.0f) {
            mTranslateX = 0.0f
        } else if (mTranslateX < maxWidth) {
            mTranslateX = maxWidth
        }

        if (mTranslateY > 0.0f) {
            mTranslateY = 0.0f
        } else if (mTranslateY < maxHeight) {
            mTranslateY = maxHeight
        }
    }

    override fun dispatchDraw(canvas: Canvas) {
        canvas.save()
        canvas.translate(mTranslateX, mTranslateY)
        canvas.scale(mScaleFactor, mScaleFactor)
        super.dispatchDraw(canvas)
        canvas.restore()
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            for (listener in mOnScaleGestureListeners) {
                listener.onScaleBegin(detector)
            }
            return super.onScaleBegin(detector)
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            mScaleFactor *= detector.scaleFactor

            val minFactor = mScaleFactor.coerceAtMost(maxZoomScale)
            mScaleFactor = minZoomScale.coerceAtLeast(minFactor)

            maxWidth = mListViewWidth - mListViewWidth * mScaleFactor
            maxHeight = mListViewHeight - mListViewHeight * mScaleFactor

            mCenterX = detector.focusX
            mCenterY = detector.focusY

            val offsetX = mCenterX * (mLastScale - mScaleFactor)
            val offsetY = mCenterY * (mLastScale - mScaleFactor)

            mTranslateX += offsetX
            mTranslateY += offsetY

            checkPointF(UN_LOADED_POINT, offsetX, offsetY)
            mLastScale = mScaleFactor
            isScaling = true

            invalidate()
            for (listener in mOnScaleGestureListeners) {
                listener.onScale(detector)
            }
            return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector) {
            if (mScaleFactor < normalScale) {
                zoomList(mScaleFactor, normalScale, mZoomToSmallScaleDuration, LOADED_POINT)
            }
            isScaling = false
            for (listener in mOnScaleGestureListeners) {
                listener.onScaleEnd(detector)
            }
        }
    }

    private inner class ScrollReaderViewGestureListener : GestureDetector.SimpleOnGestureListener() {

        override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            //list view scroll call back... to outside
            for (listener in mSimpleOnGestureListeners) {
                listener.onScroll(e1, e2, distanceX, distanceY)
            }

            return super.onScroll(e1, e2, distanceX, distanceY)
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {  // single click event,double call no call single

            for (listener in mSimpleOnGestureListeners) {
                listener.onSingleTapConfirmed(e)
            }
            return super.onSingleTapConfirmed(e)
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {  //double click event
            if (normalScale < mScaleFactor) {
                zoomList(mScaleFactor, normalScale, zoomScaleDuration, LOADED_POINT)

            } else if (mScaleFactor == normalScale) {
                mCenterX = e.x
                mCenterY = e.y
                zoomList(mScaleFactor, zoomScale, zoomScaleDuration, UN_LOADED_POINT)
            }
            for (listener in mSimpleOnGestureListeners) {
                listener.onDoubleTap(e)
            }
            return super.onDoubleTap(e)
        }
    }

    //let ListView zoom func
    private fun zoomList(startValue: Float, endValue: Float, duration: Int, loadedPointFlag: Int) {
        if (mZoomValueAnimator == null) {
            mZoomValueAnimator = ValueAnimator()
            mZoomValueAnimator!!.interpolator = DecelerateInterpolator()

            mZoomValueAnimator!!.addUpdateListener { animation ->
                mScaleFactor = animation.animatedValue as Float

                var dx = mCenterX * (mLastScale - mScaleFactor)
                var dy = mCenterY * (mLastScale - mScaleFactor)

                val pointF = checkPointF(loadedPointFlag, dx, dy)

                if (pointF != null) {
                    dx = -pointF.x
                    dy = -pointF.y
                }

                mTranslateX += dx
                mTranslateY += dy


                maxWidth = mListViewWidth - mListViewWidth * mScaleFactor
                maxHeight = mListViewHeight - mListViewHeight * mScaleFactor

                correctZoomTranslateValue()

                invalidate()

                mLastScale = mScaleFactor

                for (listener in mOnListViewZoomListeners) {
                    listener.onListViewZoomUpdate(animation, mTranslateX, mTranslateY,
                            mScaleFactor, mScaleFactor)
                }
            }

            mZoomValueAnimator!!.addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {
                    isScaling = true

                    for (listener in mOnListViewZoomListeners) {
                        listener.onListViewStart()
                    }
                }

                override fun onAnimationEnd(animation: Animator) {
                    isScaling = false

                    mLoadedPointFlag = if (loadedPointFlag == UN_LOADED_POINT) LOADED_POINT else UN_LOADED_POINT

                    for (listener in mOnListViewZoomListeners) {
                        listener.onListViewCancel()
                    }
                }

                override fun onAnimationCancel(animation: Animator) {
                    isScaling = false

                    for (listener in mOnListViewZoomListeners) {
                        listener.onListViewCancel()
                    }
                }

                override fun onAnimationRepeat(animation: Animator) {

                }
            })
        }

        if (!mZoomValueAnimator!!.isRunning) {
            mZoomValueAnimator!!.setFloatValues(startValue, endValue)
            mZoomValueAnimator!!.duration = duration.toLong()
            mZoomValueAnimator!!.start()
        }
    }

    private fun correctZoomTranslateValue() {
        if (mTranslateX > 0.0f) { //zoom +

            if (mScaleFactor >= normalScale) {  //params correct
                mTranslateX = 0.0f
            }
        } else if (mTranslateX < maxWidth) { //zoom -

            if (mScaleFactor >= normalScale) { //params correct
                mTranslateX = maxWidth
            }
        }

        if (mTranslateY > 0.0f) { //zoom +

            if (mScaleFactor >= normalScale) {  //params correct
                mTranslateY = 0.0f
            }
        } else if (mTranslateY < maxHeight) { //zoom -

            if (mScaleFactor >= normalScale) { //params correct
                mTranslateY = maxHeight
            }
        }
    }

    private fun putPointF(dx: Float, dy: Float) {
        if (mLoadedPointFlag == UN_LOADED_POINT) {
            val pointF = PointF(dx, dy)
            mLinkPoints.addFirst(pointF)
        }
    }

    private fun checkPointF(loadedPointFlag: Int, dx: Float, dy: Float): PointF? {
        val pointF: PointF? = null
        when (loadedPointFlag) {
            UN_LOADED_POINT -> putPointF(dx, dy)
            else -> throw RuntimeException("ZoomListView loaded points error ! ! !")
        }
        return pointF
    }

    fun addOnScaleGestureListener(listener: ScaleGestureDetector.SimpleOnScaleGestureListener?) {
        if (listener != null) {
            if (!mOnScaleGestureListeners.contains(listener)) {
                mOnScaleGestureListeners.add(listener)
            }
        }
    }

    fun removeOnScaleGestureListener(listener: ScaleGestureDetector.SimpleOnScaleGestureListener?) {
        if (listener != null) {
            if (mOnScaleGestureListeners.contains(listener)) {
                mOnScaleGestureListeners.remove(listener)
            }
        }
    }

    fun removeOnScaleGestureListeners() {
        while (mOnScaleGestureListeners.isNotEmpty()) {
            mOnScaleGestureListeners.removeAt(0)
        }
    }

    fun setSimpleOnGestureListener(listener: GestureDetector.SimpleOnGestureListener?) {
        if (listener != null) {
            if (!mSimpleOnGestureListeners.contains(listener)) {
                mSimpleOnGestureListeners.add(listener)
            }
        }
    }

    fun removeOnSimpleOnGestureListener(listener: GestureDetector.SimpleOnGestureListener?) {
        if (listener != null) {
            if (mSimpleOnGestureListeners.contains(listener)) {
                mSimpleOnGestureListeners.remove(listener)
            }
        }
    }

    fun removeOnSimpleOnGestureListeners() {
        while (mSimpleOnGestureListeners.isNotEmpty()) {
            mSimpleOnGestureListeners.removeAt(0)
        }
    }

    fun setOnListViewZoomListener(listener: OnListViewZoomListener?) {
        if (listener != null) {
            if (!mOnListViewZoomListeners.contains(listener)) {
                mOnListViewZoomListeners.add(listener)
            }
        }
    }

    fun removeOnListViewZoomListener(listener: OnListViewZoomListener?) {
        if (listener != null) {
            if (mOnListViewZoomListeners.contains(listener)) {
                mOnListViewZoomListeners.remove(listener)
            }
        }
    }

    fun removeOnListViewZoomListeners() {
        while (mOnListViewZoomListeners.isNotEmpty()) {
            mOnListViewZoomListeners.removeAt(0)
        }
    }

    interface OnListViewZoomListener {

        fun onListViewZoomUpdate(animation: ValueAnimator, translateX: Float, translateY: Float,
                                 scaleX: Float, scaleY: Float)

        fun onListViewStart()

        fun onListViewCancel()
    }
}