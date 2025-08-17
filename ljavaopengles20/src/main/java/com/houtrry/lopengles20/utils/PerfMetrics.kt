package com.houtrry.lopengles20.utils

import android.content.Context
import android.os.Debug
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 轻量性能埋点（最小入侵）：
 * - 由渲染线程调用 onFrameStart/onFrameEnd 统计 FPS 与 CPU 时间；
 * - 提供可选的上传字节/可见Tile/更新Tile/绘制调用计数；
 * - 每 N 帧落盘 CSV 到 app 外部文件目录，便于 adb pull。
 */
object PerfMetrics {

    @Volatile
    var enabled: Boolean = true

    private const val FLUSH_EVERY_FRAMES = 60

    private var csvFile: File? = null
    private var fileWriter: FileWriter? = null

    private var lastFrameEndNs: Long = 0L
    private var frameCounter: Int = 0

    private var uploadBytesThisFrame: Long = 0L
    private var visibleTilesThisFrame: Int = 0
    private var updatedTilesThisFrame: Int = 0
    private var drawCallsThisFrame: Int = 0

    private val timeFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    @Synchronized
    fun init(context: Context) {
        if (!enabled) return
        if (csvFile != null) return
        val dir = context.getExternalFilesDir(null) ?: context.filesDir
        val out = File(dir, "metrics.csv")
        val exists = out.exists()
        csvFile = out
        fileWriter = FileWriter(out, true)
        if (!exists) {
            fileWriter?.apply {
                write("ts,fps,cpu_ms,upload_bytes,visible_tiles,updated_tiles,draw_calls,heap_used_mb\n")
                flush()
            }
        }
    }

    fun onFrameStart() {
        if (!enabled) return
        uploadBytesThisFrame = 0L
        visibleTilesThisFrame = 0
        updatedTilesThisFrame = 0
        drawCallsThisFrame = 0
    }

    fun onFrameEnd(frameStartNs: Long, cpuStartNs: Long) {
        if (!enabled) return
        val frameEndNs = System.nanoTime()
        val cpuEndNs = Debug.threadCpuTimeNanos()
        val dtMs = (frameEndNs - frameStartNs) / 1_000_000.0
        val cpuMs = (cpuEndNs - cpuStartNs) / 1_000_000.0
        val fps = if (dtMs > 0.0) 1000.0 / dtMs else 0.0
        val heapUsedMb = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024.0 * 1024.0)

        val ts = timeFormatter.format(Date())
        val line = String.format(
            Locale.US,
            "%s,%.2f,%.2f,%d,%d,%d,%d,%.2f\n",
            ts, fps, cpuMs, uploadBytesThisFrame, visibleTilesThisFrame, updatedTilesThisFrame, drawCallsThisFrame, heapUsedMb
        )
        appendLine(line)
    }

    fun addUploadBytes(bytes: Long) {
        if (!enabled) return
        uploadBytesThisFrame += bytes
    }

    fun setVisibleTiles(count: Int) {
        if (!enabled) return
        visibleTilesThisFrame = count
    }

    fun addUpdatedTiles(count: Int) {
        if (!enabled) return
        updatedTilesThisFrame += count
    }

    fun incrementDrawCalls(delta: Int = 1) {
        if (!enabled) return
        drawCallsThisFrame += delta
    }

    @Synchronized
    private fun appendLine(text: String) {
        if (!enabled) return
        val writer = fileWriter ?: return
        writer.write(text)
        frameCounter++
        if (frameCounter % FLUSH_EVERY_FRAMES == 0) {
            writer.flush()
        }
    }

    @Synchronized
    fun flush() {
        fileWriter?.flush()
    }
}


