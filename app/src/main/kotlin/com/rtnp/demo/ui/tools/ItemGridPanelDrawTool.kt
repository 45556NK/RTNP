package com.rtnp.demo.ui.tools

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.rtnp.demo.core.InventoryItem
import com.rtnp.demo.ui.tools.ItemGridPanelLayoutTool

/**
 * 物品展示面板绘制工具（工具，纯静态）。
 */
object ItemGridPanelDrawTool {

    private const val PANEL_BG_COLOR = 0xC8303030.toInt()
    private const val PANEL_BORDER_COLOR = 0xFFFFFFFF.toInt()
    private const val PANEL_BORDER_RATIO = 1f / 200f

    private const val SLOT_BG_NORMAL = 0xFF3A3A3A.toInt()
    private const val SLOT_BG_SELECTED = 0xFFFFD700.toInt()   // 亮黄
    private const val SLOT_BORDER_COLOR = 0xFFFFFFFF.toInt()

    private const val ICON_FILL_RATIO = 0.80f
    private const val SHAPE_FALLBACK_RATIO = 0.80f

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }
    private val clipPath = Path()

    /** 绘制面板背景 */
    @JvmStatic
    fun drawPanelBackground(canvas: Canvas, panelRect: RectF) {
        val corner = panelRect.width() * ItemGridPanelLayoutTool.PANEL_CORNER_RATIO
        bgPaint.color = PANEL_BG_COLOR
        canvas.drawRoundRect(panelRect, corner, corner, bgPaint)

        borderPaint.color = PANEL_BORDER_COLOR
        borderPaint.strokeWidth = panelRect.width() * PANEL_BORDER_RATIO
        canvas.drawRoundRect(panelRect, corner, corner, borderPaint)
    }

    /** 应用面板圆角裁剪 */
    @JvmStatic
    fun clipToPanel(canvas: Canvas, panelRect: RectF) {
        val corner = panelRect.width() * ItemGridPanelLayoutTool.PANEL_CORNER_RATIO
        clipPath.reset()
        clipPath.addRoundRect(panelRect, corner, corner, Path.Direction.CW)
        canvas.clipPath(clipPath)
    }

    /**
     * 绘制单个槽位。
     *
     * @param selected 是否处于选中状态（黄色底）
     * @param contentScale 内容缩放（贴图/形状），边框不缩放
     */
    @JvmStatic
    fun drawSlot(
        canvas: Canvas,
        slotRect: RectF,
        item: InventoryItem,
        selected: Boolean,
        contentScale: Float,
        icon: Bitmap?
    ) {
        val corner = slotRect.width() * ItemGridPanelLayoutTool.SLOT_CORNER_RATIO

        // 槽位底色（选中黄 / 常态深灰）
        bgPaint.color = if (selected) SLOT_BG_SELECTED else SLOT_BG_NORMAL
        canvas.drawRoundRect(slotRect, corner, corner, bgPaint)

        // 槽位边框（不随内容缩放）
        borderPaint.color = SLOT_BORDER_COLOR
        borderPaint.strokeWidth = slotRect.width() * ItemGridPanelLayoutTool.SLOT_BORDER_RATIO
        canvas.drawRoundRect(slotRect, corner, corner, borderPaint)

        // 内容区（按 contentScale 缩到中心）
        val innerPadding = slotRect.width() * (1f - ICON_FILL_RATIO) / 2f
        val innerLeft = slotRect.left + innerPadding
        val innerTop = slotRect.top + innerPadding
        val innerRight = slotRect.right - innerPadding
        val innerBottom = slotRect.bottom - innerPadding

        val cx = (innerLeft + innerRight) / 2f
        val cy = (innerTop + innerBottom) / 2f
        val innerW = (innerRight - innerLeft) * contentScale
        val innerH = (innerBottom - innerTop) * contentScale

        if (icon != null && !icon.isRecycled) {
            // 贴图：保持宽高比，内切居中
            val ratio = icon.width.toFloat() / icon.height
            val dw: Float
            val dh: Float
            if (ratio >= 1f) { dw = innerW; dh = innerW / ratio }
            else { dh = innerH; dw = innerH * ratio }
            val dl = cx - dw / 2f
            val dt = cy - dh / 2f
            canvas.drawBitmap(icon, null, RectF(dl, dt, dl + dw, dt + dh), iconPaint)
        } else {
            // 形状降级
            drawFallbackShape(canvas, item, cx, cy, innerW * SHAPE_FALLBACK_RATIO, innerH * SHAPE_FALLBACK_RATIO)
        }
    }

    private fun drawFallbackShape(
        canvas: Canvas,
        item: InventoryItem,
        cx: Float, cy: Float,
        w: Float, h: Float
    ) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = item.color
            style = Paint.Style.FILL
        }
        when (item.shape) {
            "circle" -> {
                val r = minOf(w, h) / 2f
                canvas.drawCircle(cx, cy, r, p)
            }
            "square" -> {
                val half = minOf(w, h) / 2f
                canvas.drawRect(cx - half, cy - half, cx + half, cy + half, p)
            }
            else -> {
                // rectangle：按 item 原始宽高比等比缩放，内切于 w × h
                val targetW = item.width.toFloat()
                val targetH = item.height.toFloat()
                if (targetW <= 0f || targetH <= 0f || w <= 0f || h <= 0f) return

                val ratio = targetW / targetH
                val availRatio = w / h
                val dw: Float
                val dh: Float
                if (ratio >= availRatio) {
                    // 宽是瓶颈
                    dw = w
                    dh = w / ratio
                } else {
                    // 高是瓶颈
                    dh = h
                    dw = h * ratio
                }
                canvas.drawRect(cx - dw / 2f, cy - dh / 2f, cx + dw / 2f, cy + dh / 2f, p)
            }
        }
    }

}