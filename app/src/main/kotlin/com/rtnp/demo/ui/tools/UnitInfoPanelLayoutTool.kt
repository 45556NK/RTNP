package com.rtnp.demo.ui.tools

import android.graphics.RectF
import kotlin.math.max
import kotlin.math.min

/**
 * 单位信息面板布局工具（纯静态）。
 *
 * 面板宽度 W = max(S, 文字自然宽度最大值 + 2×padding)，上限屏宽 90%
 * 上半正方形边长 S（由贴图/形状处理后的长边决定），在 W 内水平居中
 * 下半文字区宽度 = W - 2×padding
 */
object UnitInfoPanelLayoutTool {

    const val MIN_SIDE_RATIO = 1f / 5f
    const val MAX_SIDE_RATIO = 1f / 3f
    const val PANEL_CORNER_RATIO = 1f / 20f
    const val PADDING_RATIO_OF_SIDE = 1f / 15f
    const val DASH_WIDTH_RATIO_OF_SCREEN = 1f / 150f
    const val LINE_HEIGHT_RATIO = 1.35f
    const val MAX_PANEL_WIDTH_RATIO_OF_SCREEN = 0.9f

    data class PanelLayout(
        val panelRect: RectF,
        val topSquare: RectF,
        val squareSide: Float,
        val dashY: Float,
        val textAreaLeft: Float,
        val textAreaTop: Float,
        val textAreaWidth: Float,
        val textPadding: Float,
        val lineHeight: Float
    )

    /**
     * 计算面板布局。
     *
     * @param x,y 面板左上角屏幕坐标
     * @param rawLongSide 贴图/形状原始长边（未 clamp）
     * @param naturalTextMaxWidth 所有行"自然宽度"（不换行时）的最大值
     * @param textLineCount 换行后的总行数
     * @param textSize 小字体大小
     */
    @JvmStatic
    fun compute(
        x: Float, y: Float,
        screenWidth: Int, screenHeight: Int,
        rawLongSide: Float,
        naturalTextMaxWidth: Float,
        textLineCount: Int,
        textSize: Float
    ): PanelLayout {
        val minSide = screenWidth * MIN_SIDE_RATIO
        val maxSide = screenWidth * MAX_SIDE_RATIO
        val side = rawLongSide.coerceIn(minSide, maxSide)

        val padding = side * PADDING_RATIO_OF_SIDE
        val lineHeight = textSize * LINE_HEIGHT_RATIO

        // 面板宽度
        val maxAllowedW = screenWidth * MAX_PANEL_WIDTH_RATIO_OF_SCREEN
        var panelWidth = max(side, naturalTextMaxWidth + 2f * padding)
        if (panelWidth > maxAllowedW) panelWidth = maxAllowedW

        // 上半正方形：水平居中
        val squareLeft = x + (panelWidth - side) / 2f
        val topSquare = RectF(squareLeft, y, squareLeft + side, y + side)
        val dashY = y + side

        // 下半
        val textAreaLeft = x + padding
        val textAreaTop = dashY + padding
        val textAreaWidth = panelWidth - 2f * padding

        val textBlockHeight = textLineCount * lineHeight
        val bottomHeight = padding + textBlockHeight + padding

        val totalHeight = side + bottomHeight
        val panelRect = RectF(x, y, x + panelWidth, y + totalHeight)

        return PanelLayout(
            panelRect = panelRect,
            topSquare = topSquare,
            squareSide = side,
            dashY = dashY,
            textAreaLeft = textAreaLeft,
            textAreaTop = textAreaTop,
            textAreaWidth = textAreaWidth,
            textPadding = padding,
            lineHeight = lineHeight
        )
    }

    @JvmStatic
    fun computeProcessedSize(
        rawW: Float, rawH: Float,
        screenWidth: Int
    ): Pair<Float, Float> {
        if (rawW <= 0f || rawH <= 0f) return Pair(0f, 0f)
        val longSide = max(rawW, rawH)
        val minSide = screenWidth * MIN_SIDE_RATIO
        val maxSide = screenWidth * MAX_SIDE_RATIO

        val targetLong = longSide.coerceIn(minSide, maxSide)
        val ratio = targetLong / longSide
        return Pair(rawW * ratio, rawH * ratio)
    }

    @JvmStatic
    fun wrapText(
        text: String,
        measure: (String) -> Float,
        maxWidth: Float
    ): List<String> {
        if (text.isEmpty()) return listOf("")
        if (maxWidth <= 0f) return listOf(text)

        val result = mutableListOf<String>()
        val chars = text.toCharArray()
        val sb = StringBuilder()
        var i = 0
        while (i < chars.size) {
            val c = chars[i]
            if (c == '\n') {
                result.add(sb.toString())
                sb.clear()
                i++
                continue
            }
            sb.append(c)
            if (measure(sb.toString()) > maxWidth && sb.length > 1) {
                sb.deleteCharAt(sb.length - 1)
                result.add(sb.toString())
                sb.clear()
            } else {
                i++
            }
        }
        if (sb.isNotEmpty()) result.add(sb.toString())
        return if (result.isEmpty()) listOf("") else result
    }
}