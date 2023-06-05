package com.houtrry.openglsample.shaper

import android.graphics.Bitmap
import android.opengl.GLES20
import com.houtrry.openglsample.utils.OpenglUtils
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class BitmapShaper(private val bitmap: Bitmap) : BaseShaper {

    /**
     * 顶点着色器代码;
     */
    private val vertexShaderCode =
        // 等待输入的纹理顶点坐标
        "attribute vec4 inputTextureCoordinate;" +
                " varying vec2 textureCoordinate;" +
                "attribute vec4 vPosition;" +
                "void main() {" +
                "  gl_Position = vPosition;" +
                "textureCoordinate = inputTextureCoordinate.xy;" +
                "}"

    /**
     * 片段着色器代码
     */
    private val fragmentShaderCode =
        "varying highp vec2 textureCoordinate;" +
                "uniform sampler2D inputImageTexture;" +
                "void main() {" +
                // 将2D纹理inputImageTexture和纹理顶点坐标通过texture2D计算后传给片段着色器
                "  gl_FragColor = texture2D(inputImageTexture, textureCoordinate);" +
                "}"

    private var glTextureId:Int = 0
    private var texVertex = floatArrayOf(
        0f, 0f,
        0f, 1f,
        1f, 1f,
        1f, 0f
    )
    private val texVertexBuffer:FloatBuffer = ByteBuffer.allocateDirect(texVertex.size * 4)
        .order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
            put(texVertex)
            position(0)
        }


    override fun onCreate() {
        glTextureId = OpenglUtils.createTexture(
            bitmap,
            GLES20.GL_NEAREST, GLES20.GL_LINEAR,
            GLES20.GL_CLAMP_TO_EDGE, GLES20.GL_CLAMP_TO_EDGE
        )

    }

    override fun onSizeChange() {

    }

    override fun onDraw() {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

    }
}