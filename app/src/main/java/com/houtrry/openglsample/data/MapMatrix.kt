package com.houtrry.openglsample.data

import android.graphics.PointF
import android.opengl.Matrix
import android.util.Log
import com.houtrry.openglsample.utils.OpenglUtils

/**
 * @author: houtrry
 * @time: 2024-11-19
 * @desc:
 */
class MapMatrix {
    companion object {
        private const val TAG = "MapMatrix"
    }

    private val transformMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16) // 用于变换的矩阵
    private val viewMatrix = FloatArray(16)

    private var zoom: Float = 1.0f

    init {
        Matrix.setIdentityM(transformMatrix, 0)
        Matrix.setLookAtM(viewMatrix, 0,
            0f, 0f, 1f,
            0f, 0f, 0f,
            0f, 1f, 0f)
    }

    @Synchronized
    fun translate(translateX: Float, translateY: Float) {
        val vMatrix = floatArrayOf(translateX, translateY, 0f, 0f)
        val translateInverse = FloatArray(16)
        val translateMatrix = FloatArray(4)
        Matrix.invertM(translateInverse, 0, transformMatrix, 0)
        Matrix.multiplyMV(translateMatrix, 0, translateInverse, 0, vMatrix, 0)

        Matrix.translateM(transformMatrix, 0, translateMatrix[0], translateMatrix[1], 0f)
        Log.d(TAG, "translate, zoom: $zoom, transformMatrix: ${transformMatrix.contentToString()}")
    }

    @Synchronized
    fun zoom(zoom: Float, focusX: Float = 0f, focusY: Float = 0f) {
        Matrix.translateM(transformMatrix, 0, focusX, focusY, 0f)
        Matrix.scaleM(transformMatrix, 0, zoom, zoom, 1f)
        Matrix.translateM(transformMatrix, 0, -focusX, -focusY, 0f)
        this.zoom *= zoom
    }

    @Synchronized
    fun scale(scaleX: Float, scaleY: Float, focusX: Float = 0f, focusY: Float = 0f) {
        if (scaleX == 0f && scaleY == 0f) {
            Matrix.scaleM(transformMatrix, 0, scaleX, scaleY, 1f)
        } else {
            Matrix.translateM(transformMatrix, 0, focusX, focusY, 0f)
            Matrix.scaleM(transformMatrix, 0, scaleX, scaleY, 1f)
            Matrix.translateM(transformMatrix, 0, -focusX, -focusY, 0f)
        }
    }

    @Synchronized
    fun rotate(rotate: Float, focusX: Float = 0f, focusY: Float = 0f) {
        Matrix.translateM(transformMatrix, 0, focusX, focusY, 0f)
        Matrix.rotateM(transformMatrix, 0, rotate, 0f, 0f, 1f)
        Matrix.translateM(transformMatrix, 0, -focusX, -focusY, 0f)
    }

    @Synchronized
    fun rotateWithZoom(scale: Float, rotate: Float, focusX: Float = 0f, focusY: Float = 0f) {
        Matrix.translateM(transformMatrix, 0, focusX, focusY, 0f)
        Matrix.rotateM(transformMatrix, 0, rotate, 0f, 0f, 1f)
        Matrix.scaleM(transformMatrix, 0, scale, scale, 1f)
        Matrix.translateM(transformMatrix, 0, -focusX, -focusY, 0f)
        this.zoom *= scale
//        zoom(focusX, focusY, zoom)
//        rotate(focusX, focusY, rotate)
    }

    fun getTransformMatrix() = transformMatrix

    fun getTranslateX() = transformMatrix[12]
    fun getTranslateY() = transformMatrix[13]
    fun getTranslateZ() = transformMatrix[14]

    fun testAutoRotate() {
        Matrix.rotateM(transformMatrix, 0, 0.5f, 0f, 0f, 1f)
    }

    fun frustumM(width: Int, height: Int) {
        val aspectRatio = width * 1f / height
        Matrix.frustumM(projectionMatrix, 0,
            -aspectRatio, aspectRatio,
            -1f, 1f,
            1f, 100.0f)
    }

    fun getViewMatrix() = viewMatrix

    fun getProjectionMatrix() = projectionMatrix
}