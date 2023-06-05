package com.houtrry.openglsample.utils

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLUtils

object OpenglUtils {

    fun loadShaper(type: Int, shaderCode: String): Int {
        //创建顶点着色器、片源着色器
        //并返回着色器id
        val shaper = GLES20.glCreateShader(type)
        //关联着色器代码
        GLES20.glShaderSource(shaper, shaderCode)
        //编辑着色器代码
        GLES20.glCompileShader(shaper)
        return shaper
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
        bitmap.recycle()
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


}