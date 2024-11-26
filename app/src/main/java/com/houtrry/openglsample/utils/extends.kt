package com.houtrry.openglsample.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.opengl.GLES20
import android.os.Build
import android.util.Log
import java.io.BufferedReader
import java.io.File
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

fun Int.glGetAttribLocation(name: String): Int {
    return GLES20.glGetAttribLocation(this, name)
}
fun Int.glDisableVertexAttribArray() {
    if (this >=0 ) {
        GLES20.glDisableVertexAttribArray(this)
    }
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
    val bitmap = Bitmap.createBitmap(
        drawable.intrinsicWidth,
        drawable.intrinsicHeight,
        Bitmap.Config.ARGB_8888
    )
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

fun Bitmap.toGrayFile(file: File) {
    Log.d("toGrayFile", "file: ${file.absolutePath}")
    if (file.exists()) {
        file.delete()
    }
    file.createNewFile()
    val width = this.width
    val height = this.height
    Log.d("toGrayFile", "width: $width, height: $height")

    /**
     * mapData.origin_x = ByteUtil.getDouble(byteArray1)//8
     * mapData.origin_y = ByteUtil.getDouble(byteArray2)//8
     * mapData.scale = ByteUtil.getDouble(byteArray3)//8
     * mapData.size_x = ByteUtil.getInt(byteArray4)//4
     * mapData.size_y = ByteUtil.getInt(byteArray5)//4
     */
    val originX = 0.0
    val originY = 0.0
    val scale = 0.05
    val offset = 32
    val byteArray = ByteArray(offset + width * height)
    System.arraycopy(ByteUtil.getBytes(originX), 0,  byteArray, 0, 8)
    System.arraycopy(ByteUtil.getBytes(originY), 0,  byteArray, 8, 8)
    System.arraycopy(ByteUtil.getBytes(scale), 0,  byteArray, 16, 8)
    System.arraycopy(ByteUtil.getBytes(width), 0,  byteArray, 24, 4)
    System.arraycopy(ByteUtil.getBytes(height), 0,  byteArray, 28, 4)
    for (i in 0 .. (offset + 8)) {
        Log.d("toGrayFile", "byteArray[$i] = ${byteArray[i]}")
    }
    for (y in 0 until height) {
        for (x in 0 until width) {
            val pixel = getPixel(x, y)
            byteArray[offset + y * width + x] = Color.red(pixel).toByte()
            if (height / 2 == y) {
                Log.d("toGrayFile", "g[$x, ${y}] = ${byteArray[offset + y * width + x]})")
            }
        }
    }

//    for (j in 0 until height) {
//        val color = getPixel(width / 2, j)
//        Log.d(
//            "toGrayFile",
//            "p[${width / 2}, $j] = $color(${Color.red(color)}, ${Color.green(color)}, ${
//                Color.blue(color)
//            })"
//        )
//    }
//    for (i in 0 until width) {
//        val color = getPixel(i, height / 2)
//        Log.d(
//            "toGrayFile",
//            "p[$i, ${height / 2}] = $color(${Color.red(color)}, ${Color.green(color)}, ${
//                Color.blue(color)
//            })"
//        )
//    }
    file.writeBytes(byteArray)
    Log.d("toGrayFile", "write end, ${file.length()}, ${file.absolutePath}")

//    val readBytes = file.readBytes()
//
//    val oxArray = ByteArray(8)
//    val oyArray = ByteArray(8)
//    val scArray = ByteArray(8)
//    val wArray = ByteArray(4)
//    val hArray = ByteArray(4)
//    System.arraycopy(readBytes, 0, oxArray, 0, oxArray.size)
//    System.arraycopy(readBytes, 8, oyArray, 0, oyArray.size)
//    System.arraycopy(readBytes, 16, scArray, 0, scArray.size)
//    System.arraycopy(readBytes, 24, wArray, 0, wArray.size)
//    System.arraycopy(readBytes, 28, hArray, 0, hArray.size)
//
//    val oX = ByteUtil.getDouble(oxArray)
//    val oY = ByteUtil.getDouble(oyArray)
//    val sc = ByteUtil.getDouble(scArray)
//    val w = ByteUtil.getInt(wArray)
//    val h = ByteUtil.getInt(hArray)
//
//    Log.d("toGrayFile", "oX: $oX, oY: $oY, sc: $sc, w: $w, h: $h")
//
//    for (i in 0 until w) {
//        val color = readBytes[offset + w * h / 2 + i]
//        Log.d("toGrayFile", "g[$i, ${h / 2}] = $color)")
//    }
}

fun Double.clamp(min: Double, max: Double): Double {
    return if (this < min) {
        min
    } else if (this > max) {
        max
    } else {
        this
    }
}

fun Context.getAssertBitmap(name: String): Bitmap {
    return assets.open(name).use {
        BitmapFactory.decodeStream(it)
    }
}

fun FloatArray.formatMatrixString(): String {
    return "\n[${get(0)}, ${get(4)}, ${get(8)}, ${get(12)}, \n" +
            " ${get(1)}, ${get(5)}, ${get(9)}, ${get(13)}, \n" +
            " ${get(2)}, ${get(6)}, ${get(10)}, ${get(14)}, \n" +
            " ${get(3)}, ${get(7)}, ${get(11)}, ${get(15)}]"
}
