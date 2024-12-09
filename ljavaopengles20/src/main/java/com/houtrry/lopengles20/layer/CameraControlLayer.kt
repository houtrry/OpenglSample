package com.houtrry.lopengles20.layer

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener
import androidx.core.view.GestureDetectorCompat
import com.houtrry.lopengles20.gesture.RotateGestureDetector
import com.houtrry.lopengles20.gesture.ZoomRotateGestureDetector
import com.houtrry.lopengles20.render.MapRender

class CameraControlLayer(val mapRender: MapRender) : BaseLayer() {

    companion object {
        private const val TAG = "CameraControlLayer"
    }

    private var translateGesture: GestureDetectorCompat? = null
    private var zoomGesture: ScaleGestureDetector? = null
    private var rotateGestureDetector: RotateGestureDetector? = null
    private var zoomRotateGestureDetector: ZoomRotateGestureDetector? = null
    private val mainHandler: Handler by lazy { Handler(Looper.getMainLooper()) }

    init {
        Log.d(TAG, "init start")
    }

    override fun onCreate() {
        mainHandler.post {
            translateGesture = GestureDetectorCompat(context, object : SimpleOnGestureListener() {
                override fun onDown(e: MotionEvent): Boolean {
                    // This must return true in order for onScroll() to trigger.
                    return true
                }

                override fun onScroll(
                    event1: MotionEvent, event2: MotionEvent,
                    distanceX: Float, distanceY: Float
                ): Boolean {
                    mapRender.getMapMatrix()
                        .translate(-distanceX / (viewHeight * 0.5f), distanceY / (viewHeight * 0.5f))
                    mapRender.requestRender()
                    return true
                }
            })
            zoomGesture = ScaleGestureDetector(context,
                object : SimpleOnScaleGestureListener() {
                    override fun onScale(detector: ScaleGestureDetector): Boolean {
                        if (!detector.isInProgress) {
                            return false
                        }
                        val focusX = detector.focusX
                        val focusY = detector.focusY
                        val factor = detector.scaleFactor
                        mapRender.getMapMatrix().zoom(
                            factor,
                            focusX / (viewHeight * 0.5f) - 1f,
                            1f - focusY / (viewHeight * 0.5f)
                        )
                        mapRender.requestRender()
                        return true
                    }
                })
            rotateGestureDetector = RotateGestureDetector{ focusX, focusY, rotate ->
                mapRender.getMapMatrix().rotate(
                    rotate,
                    focusX / (viewHeight * 0.5f) - 1f,
                    1f - focusY / (viewHeight * 0.5f),
                )
                mapRender.requestRender()
                true
            }
            zoomRotateGestureDetector = ZoomRotateGestureDetector { focusX, focusY, scale, rotate ->
                mapRender.getMapMatrix().rotateWithZoom(
                    scale,
                    rotate,
                    focusX / (viewHeight * 0.5f) - 1f,
                    1f - focusY / (viewHeight * 0.5f),
                )
                mapRender.requestRender()
                true
            }
        }
    }

    override fun onDraw() {

    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val translateGestureResult = translateGesture?.onTouchEvent(event) ?: false
        val zoomGestureResult = /*zoomGesture?.onTouchEvent(event) ?:*/ false
        val rotateGestureResult = /*rotateGestureDetector?.onTouchEvent(event) ?:*/ false
        val zoomRotateGestureResult = zoomRotateGestureDetector?.onTouchEvent(event) ?: false
        Log.d(
            TAG,
            "event: $event, translateGestureResult: $translateGestureResult, zoomGestureResult: $zoomGestureResult, rotateGestureResult： $rotateGestureResult"
        )
        return translateGestureResult
//                || zoomGestureResult
    //                || rotateGestureResult
                || zoomRotateGestureResult
    }

    override fun onSizeChange(width: Int, height: Int) {
        super.onSizeChange(width, height)
        mapMatrix.frustumM(width, height)
    }
}