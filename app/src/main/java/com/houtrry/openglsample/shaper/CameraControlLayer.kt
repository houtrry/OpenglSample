package com.houtrry.openglsample.shaper

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener
import androidx.core.view.GestureDetectorCompat
import com.houtrry.openglsample.data.MapMatrix
import com.houtrry.openglsample.gesture.RotateGestureDetector
import com.houtrry.openglsample.gesture.ZoomRotateGestureDetector
import com.houtrry.openglsample.render.MapRender

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
                    Log.d(TAG, "onDown ")
                    return true
                }

                override fun onScroll(
                    event1: MotionEvent, event2: MotionEvent,
                    distanceX: Float, distanceY: Float
                ): Boolean {
                    mapRender.getMapMatrix()
                        .translate(-distanceX / (viewWidth * 0.5f), distanceY / (viewHeight * 0.5f))
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
                            focusX / (viewWidth * 0.5f) - 1f,
                            1f - focusY / (viewHeight * 0.5f),
                            factor
                        )
                        return true
                    }
                })
            rotateGestureDetector = RotateGestureDetector{ focusX, focusY, rotate ->
                mapRender.getMapMatrix().rotate(
                    focusX / (viewWidth * 0.5f) - 1f,
                    1f - focusY / (viewHeight * 0.5f),
                    rotate
                )
                true
            }
            zoomRotateGestureDetector = ZoomRotateGestureDetector { focusX, focusY, scale, rotate ->
                mapRender.getMapMatrix().rotateWithZoom(
                    focusX / (viewWidth * 0.5f) - 1f,
                    1f - focusY / (viewHeight * 0.5f),
                    scale,
                    rotate
                )
                true
            }
        }
    }

    override fun onDraw(mapMatrix: MapMatrix) {

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

    private fun handleTouchEvent(event: MotionEvent) {

    }
}