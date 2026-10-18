package com.rtnp.demo.ui.tools

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF

/**
 * 单位信息面板绘制工具（纯静态）。
 */
object UnitInfoPanelDrawTool {

    private const val PANEL_BG_COLOR = 0xC8202020.toInt()
    private const val PANEL_BORDER_COLOR = 0xFFFFFFFF.toInt()
    private const val PANEL_BORDER_RATIO = 1f / 200f
    private const val DASH_COLOR = 0xFF808080.toInt()
    private const val DASH_LEN_RATIO = 1f / 30f
    private const val DASH_GAP_RATIO = 1f / 60f
    private const val SHAPE_EXTRA_SCALE = 0.8f

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val dashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = DASH_COLOR
        style = Paint.Style.STROKE
    }
    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }
    private val shapePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isAntiAlias = true }
    private val clipPath = Path()

    fun drawPanelBackground(
        canvas: Canvas,
        layout: UnitInfoPanelLayoutTool.PanelLayout,
        screenWidth: Int
    ) {
        val r = layout.panelRect
        val corner = screenWidth * UnitInfoPanelLayoutTool.PANEL_CORNER_RATIO

        bgPaint.color = PANEL_BG_COLOR
        canvas.drawRoundRect(r, corner, corner, bgPaint)

        borderPaint.color = PANEL_BORDER_COLOR
        borderPaint.strokeWidth = screenWidth * PANEL_BORDER_RATIO
        canvas.drawRoundRect(r, corner, corner, borderPaint)

        // 虚线
        dashPaint.strokeWidth = screenWidth * UnitInfoPanelLayoutTool.DASH_WIDTH_RATIO_OF_SCREEN
        val dashLen = screenWidth * DASH_LEN_RATIO
        val dashGap = screenWidth * DASH_GAP_RATIO
        val y = layout.dashY
        var x = r.left + layout.textPadding
        val endX = r.right - layout.textPadding
        while (x < endX) {
            val nx = (x + dashLen).coerceAtMost(endX)
            canvas.drawLine(x, y, nx, y, dashPaint)
            x = nx + dashGap
        }
    }

    /**
     * 裁剪到上半正方形。
     * 上两角带圆角，下方保持直角（与下半部分衔接）。
     */
    fun clipToTopSquare(
        canvas: Canvas,
        layout: UnitInfoPanelLayoutTool.PanelLayout,
        screenWidth: Int
    ) {
        val r = layout.topSquare
        val corner = screenWidth * UnitInfoPanelLayoutTool.PANEL_CORNER_RATIO

        clipPath.reset()
        clipPath.moveTo(r.left + corner, r.top)
        clipPath.lineTo(r.right - corner, r.top)
        clipPath.quadTo(r.right, r.top, r.right, r.top + corner)
        clipPath.lineTo(r.right, r.bottom)
        clipPath.lineTo(r.left, r.bottom)
        clipPath.lineTo(r.left, r.top + corner)
        clipPath.quadTo(r.left, r.top, r.left + corner, r.top)
        clipPath.close()
        canvas.clipPath(clipPath)
    }

    /**
     * 绘制贴图（带旋转缩放动画）。
     * @param processedW/H 处理后的尺寸（内切正方形）
     */
    fun drawTexture(
        canvas: Canvas,
        layout: UnitInfoPanelLayoutTool.PanelLayout,
        bitmap: Bitmap,
        processedW: Float,
        processedH: Float,
        rotationDeg: Float,
        scale: Float
    ) {
        val cx = layout.topSquare.centerX()
        val cy = layout.topSquare.centerY()

        canvas.save()
        canvas.translate(cx, cy)
        canvas.rotate(rotationDeg)
        canvas.scale(scale, scale)

        val left = -processedW / 2f
        val top = -processedH / 2f
        canvas.drawBitmap(bitmap, null, RectF(left, top, left + processedW, top + processedH), iconPaint)

        canvas.restore()
    }

    /** 绘制形状（静态无动画） */
    fun drawShapeStatic(
        canvas: Canvas,
        layout: UnitInfoPanelLayoutTool.PanelLayout,
        shape: String,
        color: Int,
        processedW: Float,
        processedH: Float
    ) {
        val cx = layout.topSquare.centerX()
        val cy = layout.topSquare.centerY()
        shapePaint.color = color

        // 形状在正方形内额外缩小到 0.8 倍，避免贴边
        val w = processedW * SHAPE_EXTRA_SCALE
        val h = processedH * SHAPE_EXTRA_SCALE

        when (shape) {
            "circle" -> canvas.drawCircle(cx, cy, minOf(w, h) / 2f, shapePaint)
            "square" -> {
                val half = minOf(w, h) / 2f
                canvas.drawRect(cx - half, cy - half, cx + half, cy + half, shapePaint)
            }
            else -> canvas.drawRect(cx - w / 2f, cy - h / 2f,
                                    cx + w / 2f, cy + h / 2f, shapePaint)
        }
    }

    /** 绘制一行文字 */
    fun drawTextLine(
        canvas: Canvas,
        x: Float,
        baselineY: Float,
        text: String,
        color: Int,
        textSize: Float
    ) {
        textPaint.textSize = textSize
        textPaint.color = color
        textPaint.textAlign = Paint.Align.LEFT
        canvas.drawText(text, x, baselineY, textPaint)
    }
}