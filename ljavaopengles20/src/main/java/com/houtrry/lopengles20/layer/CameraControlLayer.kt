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
                       .translate(-distanceX, -distanceY, viewWidth, viewHeight)
                   mapRender.requestRender()
                    return true
                }
            })
            zoomRotateGestureDetector = ZoomRotateGestureDetector { focusX, focusY, scale, rotate ->
                synchronized(mapMatrix.getModelMatrix()) {
                    val poivt = mapRender.getMapMatrix().convertScreenToGL(
                        focusX,
                        focusY,
                        viewWidth,
                        viewHeight
                    )
                    Log.d(TAG, "rotateWithZoom-> (${poivt.x}, ${poivt.y}), (${focusX/viewWidth}, ${focusY/viewHeight}), $focusX/$viewWidth, $focusY/$viewHeight")
                    mapRender.getMapMatrix().rotateWithZoom(
                        scale,
                        rotate,
                        poivt.x,
                        poivt.y,
                    )
                }
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
        mapMatrix.orthoM(width, height)
    }
}
