package com.houtrry.lopengles20.layer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.opengl.GLES20
import android.opengl.Matrix
import android.util.Log
import com.houtrry.common_map.data.Size
import com.houtrry.lopengles20.data.BubbleText
import com.houtrry.lopengles20.data.draw
import com.houtrry.lopengles20.data.getBubbleSize
import com.houtrry.common_map.utils.formatMatrixString
import com.houtrry.common_map.utils.glGetUniformLocation
import com.houtrry.common_map.utils.isLTR
import com.houtrry.common_map.utils.toBuffer
import java.nio.ShortBuffer

class BubbleTextLayer: BaseLayer() {

    companion object {
        private const val TAG = "BubbleTextLayer"
    }

    private var glArrowTextureId: Int = 0

    //四个顶点的绘制顺序数组
    private val drawOrder = shortArrayOf(
        0, 1, 2,
        0, 2, 3
    )

    //四个顶点的绘制顺序数组的缓冲数组
    private val drawListBuffer: ShortBuffer = drawOrder.toBuffer()

//    private lateinit var arrowBitmapSize: BitmapSize
    private val transformMatrix: FloatArray = FloatArray(16)

    init {
        Log.d(TAG, "init start")
    }

    override fun onCreate() {
//        arrowBitmapSize = BitmapSize(arrowBitmap.width, arrowBitmap.height)
//        glArrowTextureId = OpenglUtils.createTexture(
//            arrowBitmap,
//            GLES20.GL_NEAREST, GLES20.GL_LINEAR,
//            GLES20.GL_CLAMP_TO_EDGE, GLES20.GL_CLAMP_TO_EDGE
//        )
//
//        Log.d(TAG, "glArrowTextureId: $glArrowTextureId, ${arrowBitmapSize.width} * ${arrowBitmapSize.height}")
    }
    private val mMVPMatrix = FloatArray(16) // MVP 矩阵

    override fun onDraw() {
//        val transformMatrix = OpenglUtils.getTargetMatrix(
//            mapMatrix.getTranslateX(),
//            mapMatrix.getTranslateY(),
//            240f.toFloat() / viewHeight,
//            240f.toFloat() / viewHeight,
//            0f
//        )
        mapMatrix.getTransformMatrixWithoutScale(300f.toFloat() / viewHeight, transformMatrix)
        Log.d(TAG, "transformMatrix: ${mapMatrix.getTransformMatrix().formatMatrixString()}")
        Log.d(TAG, "translateX: ${mapMatrix.getTransformMatrix()[3]}, translateY: ${mapMatrix.getTransformMatrix()[7]}, matrix: ${mapMatrix.getTransformMatrix().contentToString()}")
        Matrix.setIdentityM(mMVPMatrix, 0)
        Matrix.multiplyMM(mMVPMatrix, 0, mapMatrix.getViewMatrix(), 0, transformMatrix, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, mapMatrix.getProjectionMatrix(), 0, mMVPMatrix, 0);
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

    fun generateBubbleTextSpan(bubbleTextList: MutableList<BubbleText>?): Bitmap? {
        if (bubbleTextList.isNullOrEmpty()) {
            return null
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rect = Rect()
        val bubbleTextSizeList = bubbleTextList.map { it.getBubbleSize(paint) }.toMutableList()
        val bitmapWidth = bubbleTextSizeList.sumOf { it.width }
        val bitmapHeight = bubbleTextSizeList.maxOf { it.height }
        val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        var offset = 0
        val path = Path()
        var bubbleSize: Size
        val isLTR = isLTR()
        bubbleTextList.forEachIndexed { index, bubbleText ->
            bubbleSize = bubbleTextSizeList[index]
            bubbleText.draw(canvas, paint, path, bubbleSize, offset, isLTR)
            offset += bubbleSize.width
        }
        return bitmap
    }
}