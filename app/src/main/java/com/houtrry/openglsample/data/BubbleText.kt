package com.houtrry.openglsample.data

import android.graphics.*
import android.view.Gravity
import com.houtrry.openglsample.utils.dp
import com.houtrry.openglsample.utils.sp
import kotlin.math.max

data class BubbleText(
    /**
     * 文本
     */
    val text: String,
    /**
     * 文本坐标
     */
    val vector3: Vector3,
    /**
     *
     * 文本的尺寸、位置等信息
     */
    val textParams: BubbleTextLayoutParam,
    /**
     * 文字的drawable
     */
    val drawable: BubbleTextDrawable? = null,
)

data class BubbleTextDrawable(
    val drawable: Bitmap,
    val width: Float = 24.dp,
    val height: Float = 24.dp,
    val drawablePadding: Float = 6.dp,
    val gravity: Int = Gravity.START,
)

data class BubbleTextLayoutParam(
    /**
     * 文字大小
     */
    val textSize: Float = 16.sp,
    val textStokeWidthSize: Float = 2f,
    /**
     * 文字颜色
     */
    val textColor: Int = Color.BLACK,
    /**
     * padding-左
     */
    val paddingStart: Int = 10.dp.toInt(),
    /**
     * padding-右
     */
    val paddingEnd: Int = 10.dp.toInt(),
    /**
     * padding-上
     */
    val paddingTop: Int= 6.dp.toInt(),
    /**
     * padding-下
     */
    val paddingBottom: Int= 6.dp.toInt(),
    /**
     * 箭头-三角形-底边宽度
     */
    val arrowWidth: Int= 8.dp.toInt(),
    /**
     * 箭头-三角形-底边到箭头顶点位置的高度
     */
    val arrowHeight: Int= 8.dp.toInt(),
    /**
     * 箭头-位于底边的位置，
     * - 为0：表示位于底边正中央
     * - <0：表示位于底边正中央左侧，距离中心的距离
     * - >0：表示位于底边正中央右侧，距离中心的距离
     */
    val arrowMargin: Int = 0,
    /**
     * 背景色
     */
    val backgroundColor: Int = Color.argb(120, 234, 234, 234),
//    val backgroundColor: Int = Color.argb(255, 255, 0, 0),
    /**
     * 圆角半径
     */
    val connerRadius: Float = 6.dp,

    /**
     * 边缘线的宽度
     */
    var borderStokeWidth: Float = 1.dp,

    /**
     * 边缘线的颜色
     */
    var borderStokeColor: Int = Color.BLACK,
)

fun BubbleText.getBubbleSize(paint: Paint): Size {
    paint.textSize = textParams.textSize
    paint.strokeWidth = textParams.textStokeWidthSize
    val textWidth = paint.measureText(text)
    val textHeight = paint.fontMetrics.let { it.bottom - it.top }
    var width: Float = (textParams.paddingStart + textParams.paddingEnd).toFloat()
    var height: Float =
        (textParams.paddingTop + textParams.paddingBottom + textParams.arrowHeight).toFloat()
    when (drawable?.gravity) {
        null -> {
            width += textWidth
            height += textHeight
        }
        Gravity.TOP, Gravity.BOTTOM -> {
            width += max(textWidth, drawable.width)
            height += drawable.height + drawable.drawablePadding + textHeight
        }
        else -> {
            width += drawable.width + drawable.drawablePadding + textWidth
            height += max(textHeight, drawable.height)
        }
    }
    return Size(width.toInt(), height.toInt())
}


/**
 * @param isLTR 是否是从左到右的布局
 */
fun BubbleText.draw(
    canvas: Canvas,
    paint: Paint,
    path: Path,
    size: Size,
    offsetX: Int,
    isLTR: Boolean
) {
    textParams.drawBubbleBackground(canvas, paint, path, size, offsetX)
    drawDrawable(canvas, paint, size, offsetX, isLTR)
    drawText(canvas, paint, size, offsetX, isLTR)
}

fun BubbleText.drawDrawable(
    canvas: Canvas,
    paint: Paint,
    size: Size,
    offsetX: Int,
    isLTR: Boolean
) {
    if (drawable == null) {

        return
    }
    val left: Int
    val top: Int
    when (drawable.gravity) {
        Gravity.TOP -> {
            left = (size.width * 0.5f - drawable.width * 0.5f).toInt()
            top = textParams.paddingTop
        }
        Gravity.BOTTOM -> {
            left = (size.width * 0.5f - drawable.width * 0.5f).toInt()
            top = (size.height - textParams.arrowHeight - drawable.height - textParams.paddingBottom).toInt()
        }
        else -> {
            val isLeft =
                (isLTR && (drawable.gravity == Gravity.START || drawable.gravity == Gravity.LEFT)) || (!isLTR && (drawable.gravity == Gravity.END || drawable.gravity == Gravity.RIGHT))
            if (isLeft) {
                left = textParams.paddingStart
                top = ((size.height - textParams.arrowHeight) * 0.5f - drawable.height * 0.5f).toInt()
            } else {
                left = (size.width - textParams.paddingEnd - drawable.width).toInt()
                top = ((size.height - textParams.arrowHeight) * 0.5f - drawable.height * 0.5f).toInt()
            }
        }
    }

    canvas.drawBitmap(
        drawable.drawable,
        Rect(0, 0, drawable.drawable.width, drawable.drawable.height),
        Rect(offsetX + left, top, (offsetX + left + drawable.width).toInt(), (top + drawable.height).toInt()),
        paint
    )
}

fun BubbleText.drawText(canvas: Canvas, paint: Paint, size: Size, offsetX: Int, isLTR: Boolean) {
    val left: Float
    val bottom: Float
    paint.textSize = textParams.textSize
    paint.color = textParams.textColor
    paint.strokeWidth = textParams.textStokeWidthSize
    paint.style = Paint.Style.FILL
    val textWidth = paint.measureText(text)
    val dyText = paint.fontMetrics.let {
        (it.bottom - it.top) * 0.5f - it.bottom
    }
    when (drawable?.gravity) {
        null -> {
            left = size.width * 0.5f - textWidth * 0.5f
            bottom = (size.height - textParams.arrowHeight) * 0.5f + dyText
        }
        Gravity.TOP -> {
            left = size.width * 0.5f - textWidth * 0.5f
//            bottom = (size.height - textParams.arrowHeight) - textParams.paddingBottom.toFloat()
            bottom = textParams.paddingTop + drawable.height + drawable.drawablePadding + paint.fontMetrics.let { it.bottom - it.top } * 0.5f + dyText
        }
        Gravity.BOTTOM -> {
            left = size.width * 0.5f - textWidth * 0.5f
            bottom = textParams.paddingTop.toFloat() + paint.fontMetrics.let { it.bottom - it.top } * 0.5f + dyText
        }
        else -> {
            val isLeft =
                (isLTR && (drawable.gravity == Gravity.START || drawable.gravity == Gravity.LEFT)) || (!isLTR && (drawable.gravity == Gravity.END || drawable.gravity == Gravity.RIGHT))
            if (isLeft) {
                left = (textParams.paddingStart + drawable.drawablePadding + drawable.width).toFloat()
                bottom = (size.height - textParams.arrowHeight) * 0.5f + dyText
            } else {
                left = textParams.paddingStart.toFloat()
                bottom =(size.height - textParams.arrowHeight) * 0.5f + dyText
            }
        }
    }
    canvas.drawText(
        text,
        offsetX + left,
        bottom,
        paint
    )
}

private fun BubbleTextLayoutParam.drawBubbleBackground(
    canvas: Canvas,
    paint: Paint,
    path: Path,
    size: Size,
    offsetX: Int
) {
    path.reset()
//    val offsetInner = borderStokeWidth * 0.5f
    val offsetInner = 0f
    path.moveTo(offsetX + connerRadius, offsetInner)
    path.rLineTo(size.width - connerRadius * 2, 0f)

    //右上圆角
    path.arcTo(
        offsetX + size.width - connerRadius * 2 + offsetInner,
        offsetInner,
        offsetX + size.width - offsetInner,
        connerRadius * 2 - offsetInner,
        270f,
        90f,
        false
    )
    path.rLineTo(0f, size.height - connerRadius * 2 - arrowHeight)

    //右下圆角
    path.arcTo(
        offsetX + size.width - connerRadius * 2 + offsetInner,
        size.height - connerRadius * 2 + offsetInner - arrowHeight,
        offsetX + size.width - offsetInner,
        size.height - offsetInner - arrowHeight,
        0f,
        90f,
        false
    )
//    path.rLineTo(connerRadius * 2 - size.width, 0f)
//
//    /**
//     * 画箭头
//     */
    path.rLineTo(-size.width * 0.5f + arrowMargin + connerRadius + arrowWidth * 0.5f, 0f)
    path.rLineTo(-arrowWidth * 0.5f, arrowHeight.toFloat())
    path.rLineTo(-arrowWidth * 0.5f, -arrowHeight.toFloat())
    path.rLineTo(-size.width * 0.5f - arrowMargin + connerRadius + arrowWidth * 0.5f, 0f)
//
//    //左下圆角
    path.arcTo(
        offsetX + offsetInner,
        size.height - connerRadius * 2 + offsetInner - arrowHeight,
        offsetX + connerRadius * 2 - offsetInner,
        size.height - offsetInner - arrowHeight,
        90f,
        90f,
        false
    )
//
//    path.rLineTo(0f, -size.height + connerRadius * 2 + arrowHeight)
//
//
//    //左上圆角
    path.arcTo(
        offsetX + offsetInner,
        offsetInner,
        offsetX + connerRadius * 2 - offsetInner,
        connerRadius * 2 - offsetInner,
        180f,
        90f,
        false
    )
    path.close()
    paint.style = Paint.Style.FILL
    paint.color = backgroundColor
    canvas.drawPath(path, paint)
    paint.style = Paint.Style.STROKE
    paint.color = borderStokeColor
    paint.strokeWidth = borderStokeWidth
    canvas.drawPath(path, paint)
}