package com.rtnp.demo.ui.panel.tool

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.rtnp.demo.ui.panel.data.DynamicProgressBarComponent

object DynamicProgressBarRenderTool {

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 22f
        textAlign = Paint.Align.CENTER
    }

    fun draw(canvas: Canvas, comp: DynamicProgressBarComponent, panelRect: RectF) {
        val left = panelRect.left + ProgressBarRenderTool.MARGIN_H
        val right = panelRect.right - ProgressBarRenderTool.MARGIN_H
        val top = panelRect.top + comp.layoutY
        val bottom = top + comp.computedHeight
        val barRect = RectF(left, top, right, bottom)

        // 背景
        bgPaint.color = comp.bgColor
        canvas.drawRoundRect(barRect, ProgressBarRenderTool.CORNER_RADIUS, ProgressBarRenderTool.CORNER_RADIUS, bgPaint)

        // 填充
        if (comp.progress > 0f) {
            val progressRight = left + (right - left) * comp.progress.coerceIn(0f, 1f)
            val progressRect = RectF(left, top, progressRight, bottom)
            progressPaint.color = comp.fillColor
            canvas.drawRoundRect(progressRect, ProgressBarRenderTool.CORNER_RADIUS, ProgressBarRenderTool.CORNER_RADIUS, progressPaint)
        }

        // 边框
        canvas.drawRoundRect(barRect, ProgressBarRenderTool.CORNER_RADIUS, ProgressBarRenderTool.CORNER_RADIUS, borderPaint)

        // 文字
        if (comp.label.isNotEmpty()) {
            val textY = barRect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2f
            canvas.drawText(comp.label, barRect.centerX(), textY, textPaint)
        }
    }
}