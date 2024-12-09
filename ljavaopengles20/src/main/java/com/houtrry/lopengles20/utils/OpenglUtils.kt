package com.houtrry.lopengles20.utils

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLUtils
import android.opengl.Matrix
import android.util.Log
import com.houtrry.common_map.utils.safeRecycle

object OpenglUtils {

    fun loadShaper(type: Int, shaderCode: String): Int? {
        Log.d("loadShaper", "type: $type, shaderCode: \n$shaderCode")
        //创建顶点着色器、片源着色器
        //并返回着色器id
        val shaper = GLES20.glCreateShader(type)
        //关联着色器代码
        GLES20.glShaderSource(shaper, shaderCode)
        //编辑着色器代码
        GLES20.glCompileShader(shaper)

        //检查编译状态
        val statusArray = intArrayOf(0)
        GLES20.glGetShaderiv(shaper, GLES20.GL_COMPILE_STATUS, statusArray, 0)

        Log.d(
            "loadShaper",
            "load shaper result is ${statusArray[0]}, and message is ${GLES20.glGetShaderInfoLog(shaper)}"
        )
        if (statusArray[0] == 0) {
            //如果编译失败
            GLES20.glDeleteShader(shaper)
            Log.e("loadShaper", "compile shaper failure")
            return null
        }

        return shaper
    }

    fun linkProgram(vertexShape: Int, fragmentShape: Int):Int? {
        //创建着色器程序
        val program = GLES20.glCreateProgram()
        if (program == 0) {
            Log.e("linkProgram", "could`t create program")
            return null
        }
        //为着色器程序添加顶点着色器
        GLES20.glAttachShader(program, vertexShape)
        //为着色器程序添加片源着色器
        GLES20.glAttachShader(program, fragmentShape)
        //链接并创建一个可执行的opengl es程序对象
        GLES20.glLinkProgram(program)

        //检查链接状态
        val linkStatus = intArrayOf(0)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
        Log.d(
            "linkProgram",
            "link program result is ${linkStatus[0]}, and message is ${GLES20.glGetProgramInfoLog(program)}"
        )
        if (linkStatus[0] == 0) {
            //链接失败就删除program
            GLES20.glDeleteProgram(program)
            Log.e("linkProgram", "link program failure")
            return null
        }
        return program
    }

    fun isValidateProgram(program:Int): Boolean {
        GLES20.glValidateProgram(program)
        val validateStatus = intArrayOf(0)
        GLES20.glGetProgramiv(program, GLES20.GL_VALIDATE_STATUS, validateStatus, 0)
        Log.d("isValidateProgram", "result of validate program status is ${validateStatus[0]}, and message is ${GLES20.glGetProgramInfoLog(program)}")
        return validateStatus[0] != 0
    }

    fun createTexture(
        bitmap: Bitmap,
        minFilter: Int, magFilter: Int,
        wrapS: Int, wrapT: Int
    ): Int {
        val textureHandle = createTextures(
            GLES20.GL_TEXTURE_2D, 1, minFilter, magFilter, wrapS, wrapT
        )
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
        bitmap.safeRecycle()
        return textureHandle[0]
    }

    private fun createTextures(
        textureTarget: Int,
        count: Int,
        minFilter: Int,
        magFilter: Int,
        wrapS: Int,
        wrapT: Int
    ): IntArray {
        val textureHandle = IntArray(count)
        for (index in 0 until count) {
            //1. 生成纹理
            GLES20.glGenTextures(1, textureHandle, index)
            //2. 绑定纹理
            GLES20.glBindTexture(textureTarget, textureHandle[index])
            //3. 设置纹理属性
            //3.1 设置纹理的缩小过滤类型
            GLES20.glTexParameteri(textureTarget, GLES20.GL_TEXTURE_MIN_FILTER, minFilter)
            //3.1.2 设置纹理的放大过滤类型
            GLES20.glTexParameteri(textureTarget, GLES20.GL_TEXTURE_MAG_FILTER, magFilter)
            //3.1.3 设置纹理的x方向环绕
            GLES20.glTexParameteri(textureTarget, GLES20.GL_TEXTURE_WRAP_S, wrapS)
            //3.1.4 设置纹理的y方向环绕
            GLES20.glTexParameteri(textureTarget, GLES20.GL_TEXTURE_WRAP_T, wrapT)
        }
        return textureHandle
    }

    fun getTargetMatrix(
        translateX: Float = 0.0f, translateY: Float = 0f,
        scaleX: Float = 1f, scaleY: Float = 1f,
        rotate: Float = 0f,
    ): FloatArray {
        val transformMatrix = FloatArray(16)
        Matrix.setIdentityM(transformMatrix, 0)
        Matrix.translateM(transformMatrix, 0, translateX, translateY, 0f)
        Matrix.rotateM(transformMatrix, 0, rotate, 0f, 0f,1f)
        Matrix.scaleM(transformMatrix, 0, scaleX, scaleY, 1f)
        return transformMatrix
    }
}