package com.houtrry.common_map.text

import android.content.Context
import android.util.DisplayMetrics
import android.util.TypedValue
import com.houtrry.common_map.data.TextInfo
import kotlin.math.max

/**
 * 屏幕空间布局工具：负责把世界锚点近似映射到屏幕像素，并计算气泡/图标/文字的像素级布局。
 * 注意：为尽量小改动，本工具默认以屏幕尺寸作为视口，适用于全屏 GLSurfaceView 情况。
 */
object ScreenLayoutUtils {

	data class PaddingPx(val left: Float, val top: Float, val right: Float, val bottom: Float)

	data class ArrowConfig(
		val widthDp: Float = 12f,
		val heightDp: Float = 6f,
		val autoFlip: Boolean = true, // true: 根据屏幕边缘自动翻转；false: 固定在下方
	)

	data class LayoutParams(
		val iconTextGapDp: Float = 4f,
		val paddingDp: PaddingPx = PaddingPx(8f, 6f, 8f, 6f),
		val arrow: ArrowConfig = ArrowConfig(),
	)

	data class PxRect(val left: Float, val top: Float, val right: Float, val bottom: Float) {
		val width: Float get() = right - left
		val height: Float get() = bottom - top
	}

	data class BubbleLayout(
		val bubbleRectPx: PxRect,
		val arrowApexPx: Pair<Float, Float>,
		val arrowBaseLeftPx: Pair<Float, Float>,
		val arrowBaseRightPx: Pair<Float, Float>,
		val iconRectPx: PxRect?,
		val textOriginPx: Pair<Float, Float>, // 文本左上基线起点（首行）
		val isArrowOnTop: Boolean
	)

	fun dpToPx(context: Context, dp: Float): Float {
		return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.resources.displayMetrics)
	}

	fun getViewportSize(context: Context): Pair<Int, Int> {
		val dm: DisplayMetrics = context.resources.displayMetrics
		return Pair(dm.widthPixels, dm.heightPixels)
	}

	/**
	 * 近似测量文本像素尺寸（使用 Paint 度量会更准；此处尽量小改动，使用字号做保守估计）。
	 */
	fun estimateTextSizePx(context: Context, textInfo: TextInfo, lines: Int = 1): Pair<Float, Float> {
		val fontPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, textInfo.style.fontSize, context.resources.displayMetrics)
		val avgCharWidth = fontPx * 0.6f
		val width = textInfo.content.length.coerceAtLeast(1) * avgCharWidth
		val lineHeight = fontPx * textInfo.style.lineSpacing
		val height = max(fontPx, lineHeight) * lines
		return Pair(width, height)
	}

	/**
	 * 计算“气泡+图标+文字”的屏幕空间布局（像素单位）。
	 *
	 * worldToScreen 由调用方提供：将世界坐标近似映射到屏幕像素坐标（x, y）。
	 */
	fun layoutBubbleIconText(
		context: Context,
		textInfo: TextInfo,
		iconWidthDp: Float,
		iconHeightDp: Float,
		params: LayoutParams,
		measurer: ((Context, TextInfo) -> Pair<Float, Float>)? = null,
		worldToScreen: (x: Double, y: Double, z: Double) -> Pair<Float, Float>
	): BubbleLayout {
		val (vw, vh) = getViewportSize(context)
		val anchor = worldToScreen(textInfo.position.x, textInfo.position.y, textInfo.position.z)

		val padPx = PaddingPx(
			left = dpToPx(context, params.paddingDp.left),
			top = dpToPx(context, params.paddingDp.top),
			right = dpToPx(context, params.paddingDp.right),
			bottom = dpToPx(context, params.paddingDp.bottom)
		)
		val gapPx = dpToPx(context, params.iconTextGapDp)
		val iconW = dpToPx(context, iconWidthDp)
		val iconH = dpToPx(context, iconHeightDp)
		val (textW, textH) = (measurer?.invoke(context, textInfo) ?: estimateTextSizePx(context, textInfo))

		val bubbleW = padPx.left + (if (textInfo.iconInfo != null) iconW + gapPx else 0f) + textW + padPx.right
		val bubbleH = padPx.top + max(iconH, textH) + padPx.bottom

		val arrowW = dpToPx(context, params.arrow.widthDp)
		val arrowH = dpToPx(context, params.arrow.heightDp)

		// 初步放置为“箭头在下方”
		var isArrowOnTop = false
		var bubbleBottom = anchor.second - arrowH
		var bubbleTop = bubbleBottom - bubbleH
		var bubbleLeft = anchor.first - bubbleW / 2f
		var bubbleRight = anchor.first + bubbleW / 2f

		// 边缘调整与自动翻转
		if (params.arrow.autoFlip) {
			if (bubbleTop < 0f) {
				// 顶部越界：翻转到上方
				isArrowOnTop = true
				bubbleTop = anchor.second + arrowH
				bubbleBottom = bubbleTop + bubbleH
			}
		}
		// 水平边缘：整体平移到可见范围内
		val dx = when {
			bubbleLeft < 0f -> -bubbleLeft
			bubbleRight > vw -> vw - bubbleRight
			else -> 0f
		}
		bubbleLeft += dx
		bubbleRight += dx

		val iconRect = if (textInfo.iconInfo != null) PxRect(
			bubbleLeft + padPx.left,
			(bubbleTop + bubbleBottom - iconH) / 2f,
			bubbleLeft + padPx.left + iconW,
			(bubbleTop + bubbleBottom + iconH) / 2f,
		) else null

		val textLeft = (iconRect?.right ?: (bubbleLeft + padPx.left)) + (if (textInfo.iconInfo != null) gapPx else 0f)
		val textTopBaseline = bubbleTop + padPx.top + dpToPx(context, textInfo.style.fontSize)

		val bubbleRect = PxRect(bubbleLeft, bubbleTop, bubbleRight, bubbleBottom)
		val arrowApex = Pair(anchor.first, anchor.second)
		val arrowBaseY = if (!isArrowOnTop) bubbleBottom else bubbleTop
		val arrowBaseL = Pair(anchor.first - arrowW / 2f, arrowBaseY)
		val arrowBaseR = Pair(anchor.first + arrowW / 2f, arrowBaseY)

		return BubbleLayout(
			bubbleRectPx = bubbleRect,
			arrowApexPx = arrowApex,
			arrowBaseLeftPx = arrowBaseL,
			arrowBaseRightPx = arrowBaseR,
			iconRectPx = iconRect,
			textOriginPx = Pair(textLeft, textTopBaseline),
			isArrowOnTop = isArrowOnTop
		)
	}
}


