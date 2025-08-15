package com.houtrry.lopengles20.shaper

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.Matrix
import android.util.Log
import com.houtrry.common_map.data.BitmapSize
import com.houtrry.common_map.data.Size
import com.houtrry.common_map.data.Vector3
import com.houtrry.common_map.utils.formatMatrixString
import com.houtrry.common_map.utils.glGetUniformLocation
import com.houtrry.common_map.utils.toBuffer
import com.houtrry.lopengles20.data.*
import com.houtrry.lopengles20.utils.MatrixUtils
import com.houtrry.lopengles20.utils.OpenglUtils
import com.houtrry.lopengles20.utils.identityM
import java.nio.ShortBuffer

class BubbleTextShape {

    companion object {
        private const val TAG = "BubbleTextShape"
    }

    private var glArrowTextureId: Int = 0

    //四个顶点的绘制顺序数组
    private val drawOrder = shortArrayOf(
        0, 1, 2,
        0, 2, 3
    )
    private val mMVPMatrix = FloatArray(16) // MVP 矩阵
    private val transformMatrix: FloatArray = FloatArray(16)
    private val scaleMatrix: FloatArray = FloatArray(16)
    private val offsetMatrix: FloatArray = FloatArray(16).identityM().apply {
        // 添加向上偏移，让气泡底部对准目标位置
        // 在标准化纹理坐标空间中，向上偏移0.5个单位（即半个高度）
        Matrix.translateM(this, 0, 0f, 0.5f, 0f)
    }
    //四个顶点的绘制顺序数组的缓冲数组
    private val drawListBuffer: ShortBuffer = drawOrder.toBuffer()
    private lateinit var arrowBitmapSize: BitmapSize

    private val bubbleTextBitmapMap = mutableMapOf<BubbleText, Bitmap>()

    fun updateText(vector3: Vector3 ?= null, text: String? = null, color: Int? = null) {
//        bubbleText = bubbleText.update(vector3, text, color)
    }

    private var scaleFactor = 0.08f
    private var size: Size = Size(1024, 1024)

    fun setSize(width: Int, height: Int) {
        size = size.copy(width, height)
    }

    fun load(bubbleTextList: MutableList<BubbleText>) {
        bubbleTextBitmapMap.clear()
        bubbleTextList.forEach {
            val bitmap = it.generateBitmap()
            bubbleTextBitmapMap[it] = bitmap
        }
    }


    fun draw(program: Int, mapMatrix: MapMatrix) {
        bubbleTextBitmapMap.forEach { (bubbleText, bitmap) ->
            drawTextShape(program, mapMatrix, bubbleText.vector3, bitmap)
        }
    }

    private fun drawTextShape(program: Int, mapMatrix: MapMatrix,
                              position: Vector3, textBitmap: Bitmap) {
        val glArrowTextureId = OpenglUtils.createTexture(
            textBitmap,
            GLES20.GL_NEAREST, GLES20.GL_LINEAR,
            GLES20.GL_CLAMP_TO_EDGE, GLES20.GL_CLAMP_TO_EDGE
        )
        val vector3 = mapMatrix.worldToGl(position.x.toFloat(), position.y.toFloat())
        Log.d(TAG, "drawTextShape, $textBitmap, vector3: $vector3 -> $position")
        transformMatrix.identityM()
        Matrix.translateM(transformMatrix, 0, vector3.x, vector3.y, 0f)
        scaleMatrix.identityM()
        Matrix.scaleM(scaleMatrix, 0, textBitmap.width.toFloat(), textBitmap.height.toFloat(), 1f)

        mMVPMatrix.identityM()

        //注意，获取分量这个操作，应该是modelMatrix与transformMatrix计算后的结果获取分量
        //而不是先获取modelMatrix的分量，再与transformMatrix计算
        //不然结果就是错的
        Matrix.multiplyMM(transformMatrix, 0, mapMatrix.getModelMatrix(), 0, transformMatrix, 0)
        MatrixUtils.multiplyMatrices(mMVPMatrix,
            mapMatrix.getProjectionViewMatrix(),
            MatrixUtils.getComponentOfMatrix(transformMatrix, FloatArray(16).identityM(), true, false, false),
            scaleMatrix,
            offsetMatrix
        )

        val transformMatrixLocation = program.glGetUniformLocation("u_TransformMatrix")

        GLES20.glActiveTexture(glArrowTextureId)
        GLES20.glUniformMatrix4fv(transformMatrixLocation, 1, false, mMVPMatrix, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, glArrowTextureId)
        GLES20.glDrawElements(
            GLES20.GL_TRIANGLE_STRIP, drawOrder.size,
            GLES20.GL_UNSIGNED_SHORT, drawListBuffer
        )
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
    }
}