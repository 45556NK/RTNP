package com.rtnp.demo.ui.panel.tool

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.rtnp.demo.ui.panel.data.HealthBarComponent
import com.rtnp.demo.ui.panel.data.LifetimeBarComponent
import com.rtnp.demo.ui.panel.data.PanelComponent
import com.rtnp.demo.ui.panel.data.SpeedBarComponent
import com.rtnp.demo.ui.panel.data.StorageBarComponent
import com.rtnp.demo.ui.panel.data.BuildProgressComponent
import com.rtnp.demo.ui.panel.data.BuildReserveComponent
import com.rtnp.demo.ui.panel.data.DynamicProgressBarComponent

object TooltipRenderTool {

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(230, 240, 240, 240)
        style = Paint.Style.FILL
    }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 55f
    }

    fun draw(canvas: Canvas, comp: PanelComponent, panelRect: RectF) {
        val tuple: Tuple5? = when (comp) {
            is HealthBarComponent -> {
                if (comp.tooltipText == null) null
                else Tuple5(comp.isPressed, comp.tooltipText, comp.tooltipBitmap, comp.tooltipWidth, comp.tooltipHeight, listOf(comp.tooltipText))
            }
            is SpeedBarComponent -> {
                if (comp.tooltipText == null) null
                else Tuple5(comp.isPressed, comp.tooltipText, comp.tooltipBitmap, comp.tooltipWidth, comp.tooltipHeight, listOf(comp.tooltipText))
            }
            is LifetimeBarComponent -> {
                if (comp.tooltipText == null) null
                else Tuple5(comp.isPressed, comp.tooltipText, comp.tooltipBitmap, comp.tooltipWidth, comp.tooltipHeight, listOf(comp.tooltipText))
            }
            is StorageBarComponent -> {
                if (comp.tooltipText == null) null
                else Tuple5(comp.isPressed, comp.tooltipText, comp.tooltipBitmap, comp.tooltipWidth, comp.tooltipHeight, comp.tooltipLines)
            }
            is BuildProgressComponent -> {
                if (comp.tooltipText == null) null
                else Tuple5(comp.isPressed, comp.tooltipText, comp.tooltipBitmap, comp.tooltipWidth, comp.tooltipHeight, comp.tooltipLines)
            }
            is BuildReserveComponent -> {
                if (comp.tooltipText == null) null
                else Tuple5(comp.isPressed, comp.tooltipText, comp.tooltipBitmap, comp.tooltipWidth, comp.tooltipHeight, comp.tooltipLines)
            }
            is DynamicProgressBarComponent -> null
            else -> null
        }
        if (tuple == null || !tuple.isPressed) return

        val tooltipLeft = panelRect.left - tuple.width - 5f
        val tooltipTop = panelRect.top + comp.layoutY
        val tooltipRect = RectF(tooltipLeft, tooltipTop, tooltipLeft + tuple.width, tooltipTop + tuple.height)

        canvas.drawRoundRect(tooltipRect, 8f, 8f, bgPaint)
        canvas.drawRoundRect(tooltipRect, 8f, 8f, borderPaint)

        val lineHeight = textPaint.textSize * 1.5f
        tuple.lines.forEachIndexed { index, line ->
            val textY = tooltipTop + lineHeight * (index + 0.5f) - (textPaint.descent() + textPaint.ascent()) / 2f
            val textX = tooltipLeft + 8f
            canvas.drawText(line, textX, textY, textPaint)
        }
    }

    private class Tuple5(
        val isPressed: Boolean,
        val text: String?,
        val bitmap: Bitmap?,
        val width: Float,
        val height: Float,
        val lines: List<String> = emptyList()
    )
}