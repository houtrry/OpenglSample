package com.houtrry.lopengles20.layer

import android.content.Context
import android.view.MotionEvent
import com.houtrry.lopengles20.data.MapMatrix

interface ILayer {

    fun onCreate(context: Context, program: Int, mapMatrix: MapMatrix)

    fun onSizeChange(width: Int, height: Int)

    /**
     * 已弃用：请在 `onDraw()` 内自行管理每次绘制所需的 GL 状态（启用/恢复）。
     */
    @Deprecated("Use onDraw to manage per-draw GL state; will be removed in a future release.")
    fun doBeforeDraw() { }

    fun onDraw()

    /**
     * 已弃用：请在 `onDraw()` 末尾自行恢复 GL 状态。
     */
    @Deprecated("Use onDraw to manage per-draw GL state; will be removed in a future release.")
    fun doAfterDraw() { }

    fun onTouchEvent(event: MotionEvent): Boolean

    /**
     * 资源释放：在移除 Layer 或退出时调用，释放纹理、缓冲等 GL 资源。
     */
    fun onDestroy() { }
}