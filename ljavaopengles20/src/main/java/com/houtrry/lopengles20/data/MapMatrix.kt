package com.houtrry.lopengles20.data

import android.opengl.Matrix
import android.util.Log
import com.houtrry.common_map.utils.formatMatrixString
import kotlin.math.sqrt

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

    init {
        Matrix.setIdentityM(transformMatrix, 0)
        Matrix.setLookAtM(
            viewMatrix, 0,
            0f, 0f, 1f,
            0f, 0f, 0f,
            0f, 1f, 0f
        )
    }

    @Synchronized
    fun translate(translateX: Float, translateY: Float) {
        val vMatrix = floatArrayOf(translateX, translateY, 0f, 0f)
        val translateInverse = FloatArray(16)
        val translateMatrix = FloatArray(4)
        Matrix.invertM(translateInverse, 0, transformMatrix, 0)
        Matrix.multiplyMV(translateMatrix, 0, translateInverse, 0, vMatrix, 0)

        Matrix.translateM(transformMatrix, 0, translateMatrix[0], translateMatrix[1], 0f)
        Log.d(TAG, "translate, transformMatrix: ${transformMatrix.contentToString()}")
    }

    @Synchronized
    fun zoom(zoom: Float, focusX: Float = 0f, focusY: Float = 0f) {
        Matrix.translateM(transformMatrix, 0, focusX, focusY, 0f)
        Matrix.scaleM(transformMatrix, 0, zoom, zoom, 1f)
        Matrix.translateM(transformMatrix, 0, -focusX, -focusY, 0f)
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
        Matrix.frustumM(
            projectionMatrix, 0,
            -aspectRatio, aspectRatio,
            -1f, 1f,
            1f, 100.0f
        )
    }

    fun getViewMatrix() = viewMatrix

    fun getProjectionMatrix() = projectionMatrix

    fun getTransformMatrixWithoutScale(scale: Float, matrix: FloatArray) {
        Log.d(
            TAG,
            "scale: $scale, transformMatrix: ${transformMatrix.formatMatrixString()}"
        )

//        Matrix.setIdentityM(matrix, 0)
        transformMatrix.copyInto(matrix)
        val sx = calcFloatArraySqrt(matrix[0], matrix[4])
        val sy = calcFloatArraySqrt(matrix[1], matrix[5])
        matrix[0] *= scale / sx
        matrix[4] *= scale / sx
        matrix[1] *= scale / sy
        matrix[5] *= scale / sy
        Log.d(TAG, "sx:$sx, sy: $sy, transformMatrix: ${matrix.formatMatrixString()}")

    }

    //变长数组的每项平方求和后，取其开根值
    private fun calcFloatArraySqrt(vararg args: Float): Float {
        return sqrt(args.sumOf { it.toDouble() * it }).toFloat()
    }
}