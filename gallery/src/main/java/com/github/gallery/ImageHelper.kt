package com.github.gallery

import android.animation.FloatEvaluator
import android.graphics.Matrix
import android.graphics.RectF
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.widget.ImageView
import android.widget.OverScroller
import androidx.core.view.ViewCompat
import kotlin.math.max
import kotlin.math.min

/**
 * @author aLang
 * 图片缩放助手
 */
internal class ImageHelper(private val imageView: ImageView) {

    var listener: GestureDetector.SimpleOnGestureListener? = null
    private val matrix = Matrix()
    private val concatMatrix = Matrix()
    private val imageMatrix = Matrix()
    private val src = RectF()
    private val dst = RectF()
    private val rect: RectF = RectF()
    private val values = FloatArray(9)
    private var minScale = 1f
    private var maxScale = 1f
    private val scaleRunnable = object : Runnable {
        private val interpolator = AccelerateInterpolator()
        private val evaluator = FloatEvaluator()
        private var startTime: Long = 0L
        private var startValue: Number = 0f
        private var endValue: Number = 0f

        fun startScale(startValue: Number, endValue: Number) {
            imageView.drawable ?: return
            this.startValue = startValue
            this.endValue = endValue
            startTime = System.currentTimeMillis()
            ViewCompat.postOnAnimation(imageView, this)
        }

        override fun run() {
            val fraction = interpolator.getInterpolation(min(1f, (System.currentTimeMillis() - startTime) / 300f))
            val evaluate = evaluator.evaluate(fraction, startValue, endValue)
            imageMatrix.getValues(values)
            val sx = values[Matrix.MSCALE_X]
            val sy = values[Matrix.MSCALE_Y]
            val scale = when {
                sx.isFinite() -> sx
                sy.isFinite() -> sy
                else -> 1f
            }
            if (scale != 0f) {
                val scaleFactor = evaluate / scale
                concatMatrix.postScale(
                    scaleFactor,
                    scaleFactor,
                    scaleGestureDetector.focusX,
                    scaleGestureDetector.focusY
                )
                setImageMatrix()
                checkConcatMatrixBounds()
                setImageMatrix()
            }
            if (fraction < 1f) ViewCompat.postOnAnimation(imageView, this)
        }
    }
    private val flingRunnable = object : Runnable {
        val scroller = OverScroller(imageView.context)
        var startX = 0
        var startY = 0
        var minX = 0
        var maxX = 0
        var minY = 0
        var maxY = 0

        fun fling(velocityX: Int, velocityY: Int) {
            scroller.forceFinished(true)

            val drawable = imageView.drawable ?: return
            rect.set(0f, 0f, drawable.intrinsicWidth.toFloat(), drawable.intrinsicHeight.toFloat())
            imageMatrix.mapRect(rect)

            val dw = rect.width()
            val dh = rect.height()
            val vw = imageView.width.toFloat()
            val vh = imageView.height.toFloat()

            startX = (-rect.left).toInt()
            startY = (-rect.top).toInt()

            if (dw < vw) {
                minX = (dw - vw).toInt()
                maxX = 0
            } else {
                minX = 0
                maxX = (dw - vw).toInt()
            }

            if (dh < vh) {
                minY = (dh - vh).toInt()
                maxY = 0
            } else {
                minY = 0
                maxY = (dh - vh).toInt()
            }

            // If we actually can move, fling the scroller
            if (startX != minX || startX != maxX || startY != minY || startY != maxY) {
                scroller.fling(startX, startY, velocityX, velocityY, minX, maxX, minY, maxY, 0, 0)
                ViewCompat.postOnAnimation(imageView, this)
            }
        }

        override fun run() {
            if (scroller.computeScrollOffset()) {
                val currX = scroller.currX
                val currY = scroller.currY
                val dx = (startX - currX).toFloat()
                val dy = (startY - currY).toFloat()
                concatMatrix.postTranslate(dx, dy)
                setImageMatrix()
                checkConcatMatrixBounds()
                setImageMatrix()
                ViewCompat.postOnAnimation(imageView, this)
            }
        }
    }

    /*--------------------------------------------------------------------------------------------------------------------*/
    private val onLayoutChangeListener =
        View.OnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            val flag = left != oldLeft || top != oldTop || right != oldRight || bottom != oldBottom
            if (flag) reset()
        }
    private val onTouchListener = object : View.OnTouchListener {
        override fun onTouch(v: View, event: MotionEvent): Boolean {
            imageView.drawable ?: return false
            var consumed = false
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> v.parent?.requestDisallowInterceptTouchEvent(true)
                MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                    imageMatrix.getValues(values)
                    val sx = values[Matrix.MSCALE_X]
                    val sy = values[Matrix.MSCALE_Y]
                    val scale = when {
                        sx.isFinite() -> sx
                        sy.isFinite() -> sy
                        else -> 1f
                    }
                    if (scale < minScale || scale > maxScale) autoZoom()
                }
            }
            return scaleGestureDetector.onTouchEvent(event) or gestureDetector.onTouchEvent(event)
        }
    }
    private val onScaleGestureListener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val scaleFactor = detector.scaleFactor
            if (scaleFactor.isFinite() && scaleFactor != 1f) {
                concatMatrix.postScale(scaleFactor, scaleFactor, detector.focusX, detector.focusY)
                setImageMatrix()
                checkConcatMatrixBounds()
                setImageMatrix()
            }
            return true
        }
    }
    private val onGestureListener = object : GestureDetector.SimpleOnGestureListener() {

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            imageView.performClick()
            return false
        }

        override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
            listener?.onSingleTapConfirmed(e)
            return super.onSingleTapConfirmed(e)
        }

        override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            val drawable = imageView.drawable ?: return false

            imageView.parent?.apply {
                rect.set(0f, 0f, drawable.intrinsicWidth.toFloat(), drawable.intrinsicHeight.toFloat())
                imageMatrix.mapRect(rect)
                val dw = rect.width().toInt()
                val vw = imageView.width
                val disallowIntercept = when {
                    dw <= vw -> false
                    dw > vw && distanceX < 0f && rect.left.toInt() == 0 -> false
                    dw > vw && distanceX > 0f && rect.right.toInt() == vw -> false
                    else -> true
                }

                requestDisallowInterceptTouchEvent(e1.pointerCount > 1 || e2.pointerCount > 1 || disallowIntercept)
            }

            concatMatrix.postTranslate(-distanceX, -distanceY)
            setImageMatrix()
            checkConcatMatrixBounds()
            setImageMatrix()

            return false
        }

        override fun onLongPress(e: MotionEvent) {
            autoZoom()
        }

        override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            if (e1.pointerCount > 1 || e2.pointerCount > 1) return false
            flingRunnable.fling(-velocityX.toInt() / 10, -velocityY.toInt() / 10)
            return false
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            autoZoom()
            return false
        }
    }
    private val scaleGestureDetector = ScaleGestureDetector(imageView.context, onScaleGestureListener)
    private val gestureDetector = GestureDetector(imageView.context, onGestureListener)

    init {
        imageView.scaleType = ImageView.ScaleType.MATRIX
        imageView.addOnLayoutChangeListener(onLayoutChangeListener)
        imageView.setOnTouchListener(onTouchListener)
        gestureDetector.setOnDoubleTapListener(onGestureListener)
    }

    fun reset() {
        val drawable = imageView.drawable ?: return

        val intrinsicWidth = drawable.intrinsicWidth.toFloat()
        val intrinsicHeight = drawable.intrinsicHeight.toFloat()
        src.set(0f, 0f, intrinsicWidth, intrinsicHeight)
        dst.set(0f, 0f, imageView.width.toFloat(), imageView.height.toFloat())
        matrix.setRectToRect(src, dst, Matrix.ScaleToFit.FILL)

        matrix.getValues(values)
        minScale = min(values[Matrix.MSCALE_X], values[Matrix.MSCALE_Y])
        maxScale = max(values[Matrix.MSCALE_X], values[Matrix.MSCALE_Y])

        matrix.setRectToRect(src, dst, Matrix.ScaleToFit.CENTER)
        concatMatrix.reset()
        setImageMatrix()
        checkConcatMatrixBounds()
    }

    private fun setImageMatrix() {
        imageMatrix.set(matrix)
        imageMatrix.postConcat(concatMatrix)
        imageView.imageMatrix = imageMatrix
        ViewCompat.postInvalidateOnAnimation(imageView)
    }

    private fun checkConcatMatrixBounds(): Boolean {
        val drawable = imageView.drawable ?: return false
        rect.set(0f, 0f, drawable.intrinsicWidth.toFloat(), drawable.intrinsicHeight.toFloat())
        imageMatrix.mapRect(rect)

        val dh = rect.height()
        val dw = rect.width()
        val vh = imageView.height
        val vw = imageView.width

        val dy = when {
            dh <= vh -> 0.5f * (vh - dh) - rect.top
            rect.top > 0 -> -rect.top
            rect.bottom < vh -> vh - rect.bottom
            else -> 0f
        }

        val dx = when {
            dw <= vw -> 0.5f * (vw - dw) - rect.left
            rect.left > 0 -> -rect.left
            rect.right < vw -> vw - rect.right
            else -> 0f
        }

        // Finally actually translate the matrix
        concatMatrix.postTranslate(dx, dy)
        return true
    }

    private fun autoZoom() {
        imageMatrix.getValues(values)
        val sx = values[Matrix.MSCALE_X]
        val sy = values[Matrix.MSCALE_Y]
        val scale = when {
            sx.isFinite() -> sx
            sy.isFinite() -> sy
            else -> 1f
        }
        if (scale == maxScale || scale < minScale) scaleRunnable.startScale(scale, minScale)
        else scaleRunnable.startScale(scale, maxScale)
    }
}