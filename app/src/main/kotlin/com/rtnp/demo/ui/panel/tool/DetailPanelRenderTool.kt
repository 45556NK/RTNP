package com.rtnp.demo.ui.panel.tool

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.rtnp.demo.ui.panel.data.DynamicProgressBarComponent
import com.rtnp.demo.ui.panel.data.PanelComponent

/**
 * 详情栏渲染工具。
 * 按住动态进度条时，在面板左侧弹出详情浮层。
 * 支持 Text / Spacer 组件。
 */
object DetailPanelRenderTool {

    private const val PADDING = 12f
    private const val LINE_SPACING = 8f
    private const val MIN_WIDTH = 200f
    private const val MAX_WIDTH = 500f

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
        textSize = 40f
    }

    private data class LayoutItem(val comp: PanelComponent, val height: Float)
    private data class LayoutResult(val width: Float, val height: Float, val items: List<LayoutItem>)

    fun draw(canvas: Canvas, comp: DynamicProgressBarComponent, panelRect: RectF) {
        if (!comp.isPressed) return
        if (comp.detailLayout.isEmpty()) return

        val layout = calculateLayout(comp.detailLayout)
        if (layout.items.isEmpty()) return

        val tooltipLeft = panelRect.left - layout.width - 5f
        val tooltipTop = panelRect.top + comp.layoutY
        val tooltipRect = RectF(tooltipLeft, tooltipTop, tooltipLeft + layout.width, tooltipTop + layout.height)

        canvas.drawRoundRect(tooltipRect, 8f, 8f, bgPaint)
        canvas.drawRoundRect(tooltipRect, 8f, 8f, borderPaint)

        var y = tooltipTop + PADDING
        for (item in layout.items) {
            when (val c = item.comp) {
                is PanelComponent.Text -> {
                    textPaint.textSize = c.textSize
                    val textY = y + item.height - textPaint.descent()
                    canvas.drawText(c.content, tooltipLeft + PADDING, textY, textPaint)
                }
                else -> { /* 其他类型暂不渲染 */ }
            }
            y += item.height + LINE_SPACING
        }
    }

    private fun calculateLayout(components: List<PanelComponent>): LayoutResult {
        val items = mutableListOf<LayoutItem>()
        var maxContentWidth = MIN_WIDTH - PADDING * 2  // 内容宽度（不含 padding）
        var totalHeight = PADDING * 2

        for (comp in components) {
            val h = when (comp) {
                is PanelComponent.Text -> {
                    textPaint.textSize = comp.textSize
                    val w = textPaint.measureText(comp.content)
                    if (w > maxContentWidth) maxContentWidth = w
                    val fm = textPaint.fontMetrics
                    (fm.bottom - fm.top) * 1.2f
                }
                is PanelComponent.Spacer -> comp.height
                else -> 0f
            }
            if (h > 0f) {
                items.add(LayoutItem(comp, h))
                totalHeight += h + LINE_SPACING
            }
        }
        if (items.isNotEmpty()) totalHeight -= LINE_SPACING

        val width = (maxContentWidth + PADDING * 2).coerceAtMost(MAX_WIDTH)
        return LayoutResult(width, totalHeight, items)
    }
}