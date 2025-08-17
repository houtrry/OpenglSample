package com.houtrry.lopengles20.layer

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.Matrix
import android.util.Log
import com.houtrry.common_map.data.BitmapSize
import com.houtrry.lopengles20.utils.OpenglUtils
import com.houtrry.common_map.utils.formatMatrixString
import com.houtrry.common_map.utils.glGetAttribLocation
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
    private var positionLocation: Int = -1
    private var textureCoordinateLocation: Int = -1
    private var transformMatrixLocation: Int = -1

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

        // 缓存位置
        positionLocation = program.glGetAttribLocation("vPosition")
        textureCoordinateLocation = program.glGetAttribLocation("inputTextureCoordinate")
        transformMatrixLocation = program.glGetUniformLocation("u_TransformMatrix")
    }
    private val mMVPMatrix = FloatArray(16).identityM() // MVP 矩阵

    override fun onDraw() {
        // 计算 MVP：PV * (model without scale) * textureSize

        synchronized(mMVPMatrix) {
            mMVPMatrix.identityM()
            MatrixUtils.multiplyMatrices(mMVPMatrix,
                mapMatrix.getProjectionViewMatrix(),
                mapMatrix.getTransformMatrixWithoutScale(mMVPMatrix),
                textureSizeMatrix
            )
        }
        if (transformMatrixLocation >= 0) {
            GLES20.glUniformMatrix4fv(transformMatrixLocation, 1, false, mMVPMatrix, 0)
        }

        // 顶点/纹理坐标 attribute 指针
        if (positionLocation >= 0) {
            GLES20.glEnableVertexAttribArray(positionLocation)
        }
        if (textureCoordinateLocation >= 0) {
            GLES20.glEnableVertexAttribArray(textureCoordinateLocation)
        }

        // 纹理绑定与绘制
        GLES20.glActiveTexture(glArrowTextureId)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, glArrowTextureId)
        GLES20.glDrawElements(
            GLES20.GL_TRIANGLE_STRIP, drawOrder.size,
            GLES20.GL_UNSIGNED_SHORT, drawListBuffer
        )
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)

        // 收尾：关闭 attribute
        if (positionLocation >= 0) GLES20.glDisableVertexAttribArray(positionLocation)
        if (textureCoordinateLocation >= 0) GLES20.glDisableVertexAttribArray(textureCoordinateLocation)
    }

    override fun onDestroy() {
        if (glArrowTextureId != 0) {
            val tmp = intArrayOf(glArrowTextureId)
            GLES20.glDeleteTextures(1, tmp, 0)
            glArrowTextureId = 0
        }
    }
}