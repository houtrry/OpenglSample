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

    @Synchronized
    fun translate(translateX: Float, translateY: Float) {
        this.translate.x += translateX
        this.translate.y += translateY
        Matrix.translateM(transformMatrix, 0, translateX, translateY, 0f)
        Log.d(TAG, "transformMatrix: $transformMatrix")
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
        this.rotate += rotate
    }

    @Synchronized
    fun rotateWithZoom(scale: Float, rotate: Float, focusX: Float = 0f, focusY: Float = 0f) {
        Matrix.translateM(transformMatrix, 0, focusX, focusY, 0f)
        Matrix.rotateM(transformMatrix, 0, rotate, 0f, 0f, 1f)
        Matrix.scaleM(transformMatrix, 0, scale, scale, 1f)
        Matrix.translateM(transformMatrix, 0, -focusX, -focusY, 0f)
        this.rotate += rotate
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
}