package com.houtrry.lopengles20.layer

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.Matrix
import android.util.Log
import com.houtrry.common_map.data.BitmapSize
import com.houtrry.lopengles20.utils.OpenglUtils
import com.houtrry.common_map.utils.formatMatrixString
import com.houtrry.common_map.utils.glGetUniformLocation
import com.houtrry.common_map.utils.toBuffer
import com.houtrry.lopengles20.utils.MatrixUtils
import com.houtrry.lopengles20.utils.identityM
import java.nio.ShortBuffer

class RobotLayer(
    private val arrowBitmap: Bitmap,
) : BaseLayer() {

    companion object {
        private const val TAG = "RobotLayer"
    }

    private var glArrowTextureId: Int = 0

    //四个顶点的绘制顺序数组
    private val drawOrder = shortArrayOf(
        0, 1, 2,
        0, 2, 3
    )

    //四个顶点的绘制顺序数组的缓冲数组
    private val drawListBuffer: ShortBuffer = drawOrder.toBuffer()

    private lateinit var arrowBitmapSize: BitmapSize
    private val transformMatrix: FloatArray = FloatArray(16)
    private val textureSizeMatrix = FloatArray(16).identityM()

    init {
        Log.d(TAG, "init start")
    }

    override fun onCreate() {
        arrowBitmapSize =
            BitmapSize(arrowBitmap.width, arrowBitmap.height)
        glArrowTextureId = OpenglUtils.createTexture(
            arrowBitmap,
            GLES20.GL_NEAREST, GLES20.GL_LINEAR,
            GLES20.GL_CLAMP_TO_EDGE, GLES20.GL_CLAMP_TO_EDGE
        )
        Matrix.scaleM(textureSizeMatrix, 0, arrowBitmapSize.width.toFloat(), arrowBitmapSize.height.toFloat(), 1f)

        Log.d(TAG, "glArrowTextureId: $glArrowTextureId, ${arrowBitmapSize.width} * ${arrowBitmapSize.height}")
    }
    private val mMVPMatrix = FloatArray(16).identityM() // MVP 矩阵

    override fun onDraw() {
//        val transformMatrix = OpenglUtils.getTargetMatrix(
//            mapMatrix.getTranslateX(),
//            mapMatrix.getTranslateY(),
//            240f.toFloat() / viewHeight,
//            240f.toFloat() / viewHeight,
//            0f
//        )
        val transformMatrixLocation = program.glGetUniformLocation("u_TransformMatrix")

//        mapMatrix.getTransformMatrixWithoutScale(300f, transformMatrix)
//        Log.d(TAG, "transformMatrix: ${mapMatrix.getModelMatrix().formatMatrixString()}")
//        Log.d(TAG, "translateX: ${mapMatrix.getModelMatrix()[3]}, translateY: ${mapMatrix.getModelMatrix()[7]}, matrix: ${mapMatrix.getModelMatrix().contentToString()}")
//        mMVPMatrix.identityM()
//        Matrix.multiplyMM(mMVPMatrix, 0, mapMatrix.getViewMatrix(), 0, transformMatrix, 0);
//        Matrix.multiplyMM(mMVPMatrix, 0, mapMatrix.getProjectionMatrix(), 0, mMVPMatrix, 0);
//        GLES20.glUniformMatrix4fv(transformMatrixLocation, 1, false, mMVPMatrix, 0)

        synchronized(mMVPMatrix) {
            mMVPMatrix.identityM()
            // 计算缩放因子
            //mvpMatrix = projectionMatrix * viewMatrix * modelMatrix * textureSizeMatrix
//            Log.d(TAG, "0-test-->${mapMatrix.getModelMatrix().formatMatrixString()}")
//            Log.d(TAG, "1-test-->${mapMatrix.getComponentOfMatrix(mMVPMatrix, true, false, false).formatMatrixString()}")
//            Log.d(TAG, "2-test-->${mapMatrix.getTransformMatrixWithoutScale(mMVPMatrix).formatMatrixString()}")
//            Matrix.multiplyMM(mMVPMatrix, 0, mapMatrix.getProjectionViewMatrix(), 0, mapMatrix.getComponentOfMatrix(mMVPMatrix, true, true, false), 0);
//            Log.d(TAG, "modelMatrix: ${mapMatrix.getModelMatrix().formatMatrixString()}")
//            Matrix.multiplyMM(mMVPMatrix, 0, mMVPMatrix, 0, textureSizeMatrix, 0)


            MatrixUtils.multiplyMatrices(mMVPMatrix,
                mapMatrix.getProjectionViewMatrix(),
//                mapMatrix.getComponentOfMatrix(mMVPMatrix, true, true, false),
                mapMatrix.getTransformMatrixWithoutScale(mMVPMatrix),
                textureSizeMatrix
            )
        }
        GLES20.glUniformMatrix4fv(transformMatrixLocation, 1, false, mMVPMatrix, 0)

        GLES20.glActiveTexture(glArrowTextureId)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, glArrowTextureId)
        GLES20.glDrawElements(
            GLES20.GL_TRIANGLE_STRIP, drawOrder.size,
            GLES20.GL_UNSIGNED_SHORT, drawListBuffer
        )
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
    }
}