package com.houtrry.openglsample.utils

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

fun FloatArray.toBuffer(): FloatBuffer = ByteBuffer.allocateDirect(this.size * 4)
    .order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
        put(this@toBuffer)
        position(0)
    }

fun ShortArray.toBuffer(): ShortBuffer = ByteBuffer.allocateDirect(this.size * 2)
    .order(ByteOrder.nativeOrder()).asShortBuffer().apply {
        put(this@toBuffer)
        position(0)
    }

fun Context.readRawText(rewResId: Int): String {
    val sb = StringBuilder()
    BufferedReader(InputStreamReader(resources.openRawResource(rewResId))).use {
        sb.append(it.readText())
        sb.append("\n")
    }
    return sb.toString()
}