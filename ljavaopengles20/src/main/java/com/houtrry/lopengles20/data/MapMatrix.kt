package com.houtrry.lopengles20.data

import android.graphics.PointF
import android.opengl.Matrix
import android.util.Log
import com.houtrry.common_map.utils.formatMatrixString
import com.houtrry.lopengles20.layer.MapLayer
import com.houtrry.lopengles20.utils.identityM
import kotlin.math.sqrt

/**
 * @author: houtrry
 * @time: 2024-11-19
 * @desc: [屏幕显示] = [投影矩阵] × [视图矩阵] × [分辨率矩阵] × [世界矩阵] × [对象矩阵]
 * mvpMatrix = projectionMatrix * viewMatrix * resolutionMatrix * worldMatrix * modelMatrix
 *
 */
class MapMatrix {
    companion object {
        private const val TAG = "MapMatrix"
    }

    private val modelMatrix = FloatArray(16).identityM()
    private val projectionMatrix = FloatArray(16).identityM() // 用于变换的矩阵
    private val projectionViewMatrix = FloatArray(16).identityM() // 用于变换的矩阵
    private val viewMatrix = FloatArray(16).identityM() //用户手势操作的变换

    init {
        Matrix.setLookAtM(
            viewMatrix, 0,
            0f, 0f, 1f,
            0f, 0f, 0f,
            0f, 1f, 0f
        )
    }

    fun translate(translateX: Float, translateY: Float, viewWidth: Int, viewHeight: Int) {
        synchronized(modelMatrix) {
            Log.d(TAG, "translate start, ($translateX, $translateY), modelMatrix: ${modelMatrix.formatMatrixString()}")
            val p0 = convertScreenToGL(0f, 0f, viewWidth, viewHeight)
            val pxy = convertScreenToGL(translateX, translateY, viewWidth, viewHeight)
            Matrix.translateM(modelMatrix, 0, pxy.x - p0.x, pxy.y - p0.y, 0f)
            Log.d(TAG, "translate end, modelMatrix: ${modelMatrix.formatMatrixString()}")
        }
    }

    private var currentScale = 1.0f
    private val minScale = 0.5f
    private val maxScale = 4.0f

    fun rotateWithZoom(scale: Float, rotate: Float, focusX: Float = 0f, focusY: Float = 0f) {
        synchronized(modelMatrix) {
            // 计算缩放后的 scale
            val newScale = (currentScale * scale).coerceIn(minScale, maxScale)
            currentScale = newScale

            // 重置 modelMatrix
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, focusX, focusY, 0f)
            Matrix.rotateM(modelMatrix, 0, rotate, 0f, 0f, 1f)
            Matrix.scaleM(modelMatrix, 0, currentScale, currentScale, 1f)
            Matrix.translateM(modelMatrix, 0, -focusX, -focusY, 0f)
        }
    }

    fun getModelMatrix() = modelMatrix

    fun getTranslateX() = modelMatrix[12]
    fun getTranslateY() = modelMatrix[13]
    fun getTranslateZ() = modelMatrix[14]

    fun testAutoRotate() {
        Matrix.rotateM(modelMatrix, 0, 0.5f, 0f, 0f, 1f)
    }

    fun orthoM(width: Int, height: Int) {
        Matrix.orthoM(
            projectionMatrix, 0,
            -width * 0.5f, width * 0.5f,
            -height * 0.5f, height * 0.5f,
            1f, 100.0f
        )
        projectionViewMatrix.identityM()
        Matrix.multiplyMM(projectionViewMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
    }

    fun getViewMatrix() = viewMatrix

    fun getProjectionMatrix() = projectionMatrix

    fun getProjectionViewMatrix() = projectionViewMatrix

    fun getTransformMatrixWithoutScale(scale: Float, matrix: FloatArray) {
        Log.d(
            TAG,
            "scale: $scale, transformMatrix: ${modelMatrix.formatMatrixString()}"
        )

//        Matrix.setIdentityM(matrix, 0)
        modelMatrix.copyInto(matrix)
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

    /**
     * 屏幕坐标转成GL坐标
     */
    fun convertScreenToGL(screenX: Float, screenY: Float, viewWidth: Int, viewHeight: Int): PointF {
        val tempMatrix = FloatArray(16).identityM()
        val invertedMatrix = FloatArray(16).identityM()
        val ndcX = screenX / (viewWidth * 0.5f) - 1.0f
        val ndcY = 1.0f - screenY / (viewHeight * 0.5f)

        Matrix.multiplyMM(tempMatrix, 0, projectionMatrix, 0, modelMatrix, 0)
        Matrix.invertM(invertedMatrix, 0, tempMatrix, 0)

        val inVec = floatArrayOf(ndcX, ndcY, 0f, 1f)
        val outVec = FloatArray(4)
        Matrix.multiplyMV(outVec, 0, invertedMatrix, 0, inVec, 0)

        if (outVec[3] != 0f) {
            outVec[0] /= outVec[3]
            outVec[1] /= outVec[3]
        }
        return PointF(outVec[0], outVec[1])
    }

    /**
     * GL坐标转成屏幕坐标
     */
    fun convertGlToScreen(glX: Float, glY: Float, viewWidth: Int, viewHeight: Int): PointF {
        val tempMatrix = floatArrayOf(glX, glY, 0f, 0f)
        Matrix.multiplyMM(tempMatrix, 0, modelMatrix, 0, tempMatrix, 0)
        return PointF(viewWidth * 0.5f + tempMatrix[0], viewHeight * 0.5f - tempMatrix[1])
    }

    /**
     * 定位坐标转成GL坐标
     */
    fun worldToGl(bitmapInfo: BitmapInfo, x: Float, y: Float): PointF {
        return PointF(
            (x - bitmapInfo.resolution * bitmapInfo.width * 0.5f - bitmapInfo.originX) / bitmapInfo.resolution,
            (y - bitmapInfo.resolution * bitmapInfo.height * 0.5f - bitmapInfo.originY) / bitmapInfo.resolution,
        )
    }
}