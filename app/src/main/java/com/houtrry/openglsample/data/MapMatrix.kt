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
class MapMatrix(
    var zoom: Float = 0.05f,
    var rotate: Float = 0f,
    var translate: PointF = PointF(0.0f, 0.0f)
) {

    companion object {
        private const val TAG = "MapMatrix"
    }

    private val transformMatrix = FloatArray(16)

    init {
        Matrix.setIdentityM(transformMatrix, 0)
    }

    fun translate(translateX: Float, translateY: Float) {
        this.translate.x += translateX
        this.translate.y += translateY
        Matrix.translateM(transformMatrix, 0, translateX, translateY, 0f)
        Log.d(TAG, "transformMatrix: $transformMatrix")
    }

    fun zoom(focusX: Float, focusY: Float, zoom: Float) {
        Matrix.translateM(transformMatrix, 0, focusX, focusY, 0f)
        Matrix.scaleM(transformMatrix, 0, zoom, zoom, 1f)
        Matrix.translateM(transformMatrix, 0, -focusX, -focusY, 0f)
        this.zoom *= zoom
    }

    fun rotate(focusX: Float, focusY: Float, rotate: Float) {
        Matrix.translateM(transformMatrix, 0, focusX, focusY, 0f)
        Matrix.rotateM(transformMatrix, 0, rotate, 0f, 0f, 1f)
        Matrix.translateM(transformMatrix, 0, -focusX, -focusY, 0f)
        this.rotate += rotate
    }

    fun getTransformMatrix() = transformMatrix

    fun getTranslateX() = transformMatrix[12]
    fun getTranslateY() = transformMatrix[13]
    fun getTranslateZ() = transformMatrix[14]
}