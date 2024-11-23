package com.houtrry.openglsample.gesture

import android.util.Log
import android.view.MotionEvent
import com.houtrry.openglsample.utils.clamp
import kotlin.math.atan2
import kotlin.math.hypot

class ZoomRotateGestureDetector(private val listener: (Float, Float, Float, Float) -> Boolean) {

    companion object {
        private const val MAX_DELTA_ANGLE = 1e-1
        private const val TAG = "ZoomRotateGestureDetector"
    }

    private var previousMotionEvent: MotionEvent? = null
    private var focusX: Float = 0f
    private var focusY: Float = 0f
    private var preDistance = 0.0

    fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.pointerCount > 2) {
            return false
        }
        if (event.pointerCount <= 1) {
            previousMotionEvent?.recycle()
            previousMotionEvent = null
            focusX = 0f
            focusY = 0f
            preDistance = 0.0
            return false
        }
        if (previousMotionEvent == null) {
            previousMotionEvent = MotionEvent.obtain(event)
            focusX = (event.getX(0) + event.getX(1)) * 0.5f
            focusY = (event.getY(0) + event.getY(1)) * 0.5f
            preDistance = hypot(
                (event.getX(0) - event.getX(1)).toDouble(),
                (event.getY(0) - event.getY(1)).toDouble()
            )
            return false
        }
        val deltaAngle: Double =
            Math.toDegrees(
                (angle(previousMotionEvent!!) - angle(event)).clamp(
                    -MAX_DELTA_ANGLE,
                    MAX_DELTA_ANGLE
                )
            )

        val distance = hypot(
            (event.getX(0) - event.getX(1)).toDouble(),
            (event.getY(0) - event.getY(1)).toDouble()
        )
        val scale = (distance / preDistance).toFloat()
        Log.d(TAG, "focusX: $focusX, focusY: $focusX, deltaAngle: $deltaAngle, scale: $scale")

        val result = listener.invoke(focusX, focusY, scale, deltaAngle.toFloat())
        previousMotionEvent!!.recycle()
        previousMotionEvent = MotionEvent.obtain(event)
        focusX = (event.getX(0) + event.getX(1)) * 0.5f
        focusY = (event.getY(0) + event.getY(1)) * 0.5f
        preDistance = distance
        return result
    }

    private fun angle(event: MotionEvent): Double {
        val deltaX = (event.getX(0) - event.getX(1)).toDouble()
        val deltaY = (event.getY(0) - event.getY(1)).toDouble()
        return atan2(deltaY, deltaX)
    }
}