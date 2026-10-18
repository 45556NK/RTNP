package com.rtnp.demo.ui.bottombar.tools

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF

/**
 * 底部栏按钮绘制工具（工具，纯静态）。
 *
 * 激活态：
 *  - 浅红半透明底（替换浅灰）
 *  - 红色叉（替换贴图）
 *  - 边框变红
 *
 * 非激活态：
 *  - 浅灰底 + 边框 + 贴图
 *
 * 整体透明度：调用者通过 [alpha] 参数传入，作用于整个按钮
 */
object BottomBarDrawTool {

    private const val ICON_FILL_RATIO = 0.75f
    private const val BORDER_WIDTH_RATIO = 0.06f
    private const val LABEL_PAD_RATIO = 0.25f
    private const val LABEL_GAP_RATIO = 0.9f
    private const val LABEL_OFFSET_BASE_RATIO = 0.5f

    private const val BG_NORMAL = 0xC8C8C8C8.toInt()
    private const val BG_ACTIVE = 0x80FF9696.toInt()
    private const val BORDER_NORMAL = Color.WHITE
    private const val BORDER_ACTIVE = 0xFFFF6464.toInt()

    private const val CROSS_COLOR = 0xFFDC1E1E.toInt()
    private const val CROSS_LEN_RATIO = 0.6f
    private const val CROSS_WIDTH_RATIO = 0.15f

    private const val LABEL_BG_ALPHA = 200
    private const val LABEL_FG_ALPHA = 255

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }
    private val crossPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
    }
    private val labelBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(LABEL_BG_ALPHA, 0, 0, 0)
        style = Paint.Style.FILL
    }

    /**
     * 绘制一个按钮。
     *
     * @param rect 按钮实际布局矩形（含缩放/lift）
     * @param alpha 整体透明度（0~1）
     */
    @JvmStatic
    fun drawButton(
        canvas: Canvas,
        rect: RectF,
        icon: Bitmap?,
        alpha: Float,
        active: Boolean,
        displayName: String,
        labelTextSize: Float,
        labelAlpha: Float,
        labelOffsetProgress: Float
    ) {
        if (alpha <= 0.01f) return

        val cx = rect.centerX()
        val cy = rect.centerY()
        val r = minOf(rect.width(), rect.height()) / 2f

        canvas.saveLayerAlpha(null, (alpha * 255).toInt().coerceIn(0, 255))

        if (active) {
            // 浅红底
            bgPaint.color = BG_ACTIVE
            canvas.drawCircle(cx, cy, r, bgPaint)

            // 红边框
            borderPaint.color = BORDER_ACTIVE
            borderPaint.strokeWidth = r * BORDER_WIDTH_RATIO
            canvas.drawCircle(cx, cy, r, borderPaint)

            // 红叉
            crossPaint.color = CROSS_COLOR
            crossPaint.strokeWidth = r * CROSS_WIDTH_RATIO
            val len = r * CROSS_LEN_RATIO
            canvas.drawLine(cx - len, cy - len, cx + len, cy + len, crossPaint)
            canvas.drawLine(cx - len, cy + len, cx + len, cy - len, crossPaint)
        } else {
            // 浅灰底
            bgPaint.color = BG_NORMAL
            canvas.drawCircle(cx, cy, r, bgPaint)

            // 白边框
            borderPaint.color = BORDER_NORMAL
            borderPaint.strokeWidth = r * BORDER_WIDTH_RATIO
            canvas.drawCircle(cx, cy, r, borderPaint)

            // 贴图
            if (icon != null && !icon.isRecycled) {
                val iconSize = r * 2f * ICON_FILL_RATIO
                val ratio = icon.width.toFloat() / icon.height
                val dw = if (ratio >= 1f) iconSize else iconSize * ratio
                val dh = if (ratio >= 1f) iconSize / ratio else iconSize
                canvas.drawBitmap(
                    icon, null,
                    RectF(cx - dw / 2f, cy - dh / 2f, cx + dw / 2f, cy + dh / 2f),
                    iconPaint
                )
            }
        }

        // 名称
        if (labelAlpha > 0.01f && displayName.isNotEmpty()) {
            labelPaint.textSize = labelTextSize
            labelPaint.alpha = (labelAlpha * LABEL_FG_ALPHA).toInt().coerceIn(0, 255)

            val textW = labelPaint.measureText(displayName)
            val textH = labelPaint.textSize
            val pad = textH * LABEL_PAD_RATIO
            val offsetBase = textH * LABEL_OFFSET_BASE_RATIO

            val labelCx = cx
            val labelCy = cy - r - textH * LABEL_GAP_RATIO + labelOffsetProgress * offsetBase

            labelBgPaint.alpha = (labelAlpha * LABEL_BG_ALPHA).toInt().coerceIn(0, 255)
            canvas.drawRoundRect(
                RectF(labelCx - textW / 2f - pad, labelCy - textH / 2f - pad,
                      labelCx + textW / 2f + pad, labelCy + textH / 2f + pad),
                pad * 2f, pad * 2f, labelBgPaint
            )

            val textY = labelCy - (labelPaint.descent() + labelPaint.ascent()) / 2f
            canvas.drawText(displayName, labelCx, textY, labelPaint)
        }

        canvas.restore()
    }
}