package com.houtrry.lopengles20.gesture

import android.util.Log
import android.view.MotionEvent
import com.houtrry.common_map.utils.clamp
import kotlin.math.atan2

class RotateGestureDetector(private val listener: (Float, Float, Float) -> Boolean) {

    companion object {
        private const val MAX_DELTA_ANGLE = 1e-1
        private const val TAG = "RotateGestureDetector"
    }

    private var previousMotionEvent: MotionEvent? = null
    private var focusX : Float = 0f
    private var focusY : Float = 0f

    fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.pointerCount != 2) {
            return false
        }
        if (previousMotionEvent == null) {
            previousMotionEvent = MotionEvent.obtain(event)
            focusX = (event.getX(0) + event.getX(1)) * 0.5f
            focusY = (event.getY(0) + event.getY(1)) * 0.5f
            return false
        }
        val deltaAngle: Double =
            Math.toDegrees((angle(previousMotionEvent!!) - angle(event)).clamp(-MAX_DELTA_ANGLE, MAX_DELTA_ANGLE))
        Log.d(TAG, "focusX: $focusX, focusY: $focusX, deltaAngle: $deltaAngle")
        val result = listener.invoke(focusX, focusY, deltaAngle.toFloat())
        previousMotionEvent!!.recycle()
        previousMotionEvent = MotionEvent.obtain(event)
        focusX = (event.getX(0) + event.getX(1)) * 0.5f
        focusY = (event.getY(0) + event.getY(1)) * 0.5f
        return result
    }

    private fun angle(event: MotionEvent): Double {
        val deltaX = (event.getX(0) - event.getX(1)).toDouble()
        val deltaY = (event.getY(0) - event.getY(1)).toDouble()
        return atan2(deltaY, deltaX)
    }
}