package com.houtrry.lopengles20.layer

import android.content.Context
import android.view.MotionEvent
import com.houtrry.lopengles20.data.MapMatrix

abstract class BaseLayer: ILayer {

    protected var viewWidth: Int = 0
    protected var viewHeight: Int = 0
    protected var program = 0
    protected lateinit var context: Context
    protected lateinit var mapMatrix: MapMatrix
    protected var autoOverviewThresholdMeters: Float = 1.0f
    protected var lastZoomGestureEndedPoseXMeters: Float? = null
    protected var lastZoomGestureEndedPoseYMeters: Float? = null
    protected var isInZoomGesture: Boolean = false

    override fun onCreate(context: Context, program: Int, mapMatrix: MapMatrix) {
        this.program = program
        this.context = context
        this.mapMatrix = mapMatrix
        onCreate()
    }

    abstract fun onCreate()

    override fun onSizeChange(width: Int, height: Int) {
        viewWidth = width
        viewHeight = height
    }

    override fun doBeforeDraw() {

    }

    override fun doAfterDraw() {

    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return false
    }

    /**
     * Call when zoom gesture begins.
     */
    fun notifyZoomGestureBegin(currentRobotPoseXMeters: Float, currentRobotPoseYMeters: Float) {
        isInZoomGesture = true
        lastZoomGestureEndedPoseXMeters = null
        lastZoomGestureEndedPoseYMeters = null
    }

    /**
     * Call when zoom gesture ends; record the pose at end-of-zoom to start threshold tracking.
     */
    fun notifyZoomGestureEnd(currentRobotPoseXMeters: Float, currentRobotPoseYMeters: Float) {
        isInZoomGesture = false
        lastZoomGestureEndedPoseXMeters = currentRobotPoseXMeters
        lastZoomGestureEndedPoseYMeters = currentRobotPoseYMeters
    }

    /**
     * Feed robot pose during normal run; when exceeded threshold since last zoom, caller can restore overview.
     */
    fun updateRobotPoseAndCheckAutoOverview(
        currentRobotPoseXMeters: Float,
        currentRobotPoseYMeters: Float,
        onExceeded: () -> Unit
    ) {
        val ax = lastZoomGestureEndedPoseXMeters
        val ay = lastZoomGestureEndedPoseYMeters
        if (ax == null || ay == null) return
        val dx = currentRobotPoseXMeters - ax
        val dy = currentRobotPoseYMeters - ay
        val dist = kotlin.math.sqrt(dx * dx + dy * dy)
        if (dist >= autoOverviewThresholdMeters) {
            // reset anchor to avoid repeated triggers
            lastZoomGestureEndedPoseXMeters = null
            lastZoomGestureEndedPoseYMeters = null
            onExceeded.invoke()
        }
    }
}