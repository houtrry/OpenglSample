package com.houtrry.openglsample.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.opengl.GLES20
import android.os.Build
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

fun String.glGetUniformLocation(progressId: Int): Int {
    return GLES20.glGetUniformLocation(progressId, this)
}

fun Int.glGetUniformLocation(name: String): Int {
    return GLES20.glGetUniformLocation(this, name)
}

fun <E, T, R> notNull(e: E?, t: T?, callback: ((E, T) -> R)): R? {
    return if (e == null || t == null) {
        null
    } else {
        callback.invoke(e, t)
    }
}

fun Context.getVectorDrawable(resourceId: Int): Bitmap? {
     if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
        return BitmapFactory.decodeResource(resources, resourceId)
    }
    val drawable = getDrawable(resourceId) ?: return null
    val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}

fun Bitmap.safeRecycle() {
    if (isRecycled) {
        return
    }
    try {
        recycle()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
