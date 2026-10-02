package com.rtnp.demo.ui.systempanel.tools

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import com.rtnp.demo.ui.HexButton

object SystemPanelHexRenderer {

    /**
     * 根据槽位类型返回六边形填充色。
     */
    fun getFillColor(type: String): Int = when (type) {
        "weapon" -> Color.rgb(255, 130, 130)
        "strategy" -> Color.rgb(190, 160, 255)
        "shield" -> Color.rgb(100, 180, 240)
        else -> Color.rgb(190, 160, 255)
    }

    /**
     * 绘制单个六边形按钮（含可选图标）。
     */
    fun drawHexButton(canvas: Canvas, btn: HexButton, bitmap: Bitmap?) {
        btn.draw(canvas)
        bitmap?.let { bmp ->
            val half = btn.size * 1.6f * 0.9f / 2f
            val left = btn.centerX - half
            val top = btn.centerY - half
            canvas.drawBitmap(bmp, null, RectF(left, top, left + half * 2, top + half * 2), null)
        }
    }
}