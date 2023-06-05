package com.houtrry.openglsample.shaper

import android.graphics.Bitmap
import android.opengl.GLES20
import android.util.Log
import com.houtrry.openglsample.utils.OpenglUtils
import com.houtrry.openglsample.utils.toBuffer
import java.nio.FloatBuffer
import java.nio.ShortBuffer

class MapShaper(
    private val bitmap: Bitmap,
    private val vertexShaderCode: String,
    private val fragmentShaderCode: String
) : BaseShaper {

    companion object {
        private const val TAG = "MapShaper"

        //每个顶点的坐标数
        private const val COORDS_PRE_VERTEX = 3

        //每个纹理顶点的坐标数
        private const val COORDS_PRE_TEXTURE_VERTEX = 2
    }

    private var glTextureId: Int = 0

    //顶点坐标
    private var squareCoords = floatArrayOf(
        -1.0f, 1.0f, 0.0f,//top left
        -1.0f, -1.0f, 0.0f,//bottom left
        1.0f, -1.0f, 0.0f,//bottom right
        1.0f, 1.0f, 0.0f,//top right
    )

    //顶点对应的纹理坐标
    private var texVertex = floatArrayOf(
        0f, 0f,
        0f, 1f,
        1f, 1f,
        1f, 0f
    )

    //四个顶点的绘制顺序数组
    private val drawOrder = shortArrayOf(
        0, 1, 2,
        0, 2, 3
    )

    //四个顶点的缓冲数组
    private val vertexBuffer: FloatBuffer = squareCoords.toBuffer()

    private val texVertexBuffer: FloatBuffer = texVertex.toBuffer()

    //四个顶点的绘制顺序数组的缓冲数组
    private val drawListBuffer: ShortBuffer = drawOrder.toBuffer()

    private var vPMatrixHande: Int = 0
    private var mPrograms: Int = 0
    private val vertexStride: Int = COORDS_PRE_VERTEX * 4
    private val textVertexStride: Int = COORDS_PRE_TEXTURE_VERTEX * 4

    init {
        Log.d(TAG, "init start")
    }

    override fun onCreate() {
        //编译顶点着色器
        val vertexShaper: Int = OpenglUtils.loadShaper(
            GLES20.GL_VERTEX_SHADER, vertexShaderCode
        )
        Log.d(TAG, "vertexShaper: $vertexShaper")
        //编译片源着色器
        val fragmentShaper: Int = OpenglUtils.loadShaper(
            GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode
        )
        Log.d(TAG, "fragmentShaper: $fragmentShaper")
        //创建着色器程序
        mPrograms = GLES20.glCreateProgram().also {
            //为着色器程序添加顶点着色器
            GLES20.glAttachShader(it, vertexShaper)
            //为着色器程序添加片源着色器
            GLES20.glAttachShader(it, fragmentShaper)
            //链接并创建一个可执行的opengl es程序对象
            GLES20.glLinkProgram(it)
        }
        Log.d(TAG, "mPrograms: $mPrograms")
        glTextureId = OpenglUtils.createTexture(
            bitmap,
            GLES20.GL_NEAREST, GLES20.GL_LINEAR,
            GLES20.GL_CLAMP_TO_EDGE, GLES20.GL_CLAMP_TO_EDGE
        )
        Log.d(TAG, "glTextureId: $glTextureId")
    }

    override fun onSizeChange() {

    }

    override fun onDraw() {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glUseProgram(mPrograms)
        val position = GLES20.glGetAttribLocation(mPrograms, "vPosition")
        GLES20.glEnableVertexAttribArray(position)
        GLES20.glVertexAttribPointer(
            position, COORDS_PRE_VERTEX, GLES20.GL_FLOAT,
            false, vertexStride, vertexBuffer
        )
        GLES20.glActiveTexture(glTextureId)

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, glTextureId)

        val textureCoordinate = GLES20.glGetAttribLocation(mPrograms, "inputTextureCoordinate")

        GLES20.glEnableVertexAttribArray(textureCoordinate)

        GLES20.glVertexAttribPointer(
            textureCoordinate, COORDS_PRE_TEXTURE_VERTEX, GLES20.GL_FLOAT,
            false, textVertexStride, texVertexBuffer
        )

        GLES20.glDrawElements(
            GLES20.GL_TRIANGLE_STRIP, drawOrder.size,
            GLES20.GL_UNSIGNED_SHORT, drawListBuffer
        )

        GLES20.glDisableVertexAttribArray(position)
        GLES20.glDisableVertexAttribArray(textureCoordinate)
    }
}