package com.houtrry.lopengles20.tile

import android.opengl.GLES20

/**
 * 纹理池（最小实现）：
 * - 目的：复用 GL_TEXTURE_2D，减少频繁 glGenTextures 带来的开销；
 * - 策略：限制容量，空闲纹理进入队列，超出容量则及时删除；
 * - 参数：min/mag 过滤器默认使用 GL_LINEAR，可按需调整。
 */
class TexturePool(
    private val capacity: Int,
    private val minFilter: Int = GLES20.GL_LINEAR,
    private val magFilter: Int = GLES20.GL_LINEAR
) {
    private val freeList = ArrayDeque<Int>()
    private var totalCreated = 0

    /**
     * 获取一个可用纹理：优先复用空闲队列；否则在容量内创建新纹理；
     * 超出容量时退化为直接创建一次性纹理（不进入复用队列）。
     */
    @Synchronized
    fun acquire(): Int {
        val tex = if (freeList.isNotEmpty()) {
            freeList.removeFirst()
        } else if (totalCreated < capacity) {
            val out = IntArray(1)
            GLES20.glGenTextures(1, out, 0)
            val id = out[0]
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, id)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, minFilter)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, magFilter)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
            totalCreated++
            id
        } else {
            // 超出容量：退化为一次性创建纹理（调用方用完后不要放回池中）
            val out = IntArray(1)
            GLES20.glGenTextures(1, out, 0)
            out[0]
        }
        return tex
    }

    /**
     * 释放纹理：若池未满加入空闲队列，否则直接删除。
     */
    @Synchronized
    fun release(textureId: Int) {
        if (freeList.size < capacity) {
            freeList.addLast(textureId)
        } else {
            val tmp = intArrayOf(textureId)
            GLES20.glDeleteTextures(1, tmp, 0)
        }
    }

    /**
     * 全局销毁：删除空闲队列中的所有纹理。
     * 注意：需在拥有有效 GL 上下文的 GL 线程中调用。
     */
    @Synchronized
    fun destroy() {
        while (freeList.isNotEmpty()) {
            val id = freeList.removeFirst()
            val tmp = intArrayOf(id)
            GLES20.glDeleteTextures(1, tmp, 0)
        }
        totalCreated = 0
    }
}


