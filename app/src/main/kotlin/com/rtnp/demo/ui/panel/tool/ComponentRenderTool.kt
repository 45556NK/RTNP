package com.rtnp.demo.ui.panel.tool

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.rtnp.demo.logger.Logger
import com.rtnp.demo.ui.panel.data.HealthBarComponent
import com.rtnp.demo.ui.panel.data.InteractiveButtonComponent
import com.rtnp.demo.ui.panel.data.LifetimeBarComponent
import com.rtnp.demo.ui.panel.data.PanelComponent
import com.rtnp.demo.ui.panel.data.SpeedBarComponent
import com.rtnp.demo.ui.panel.data.StorageBarComponent
import com.rtnp.demo.ui.panel.data.BuildProgressComponent
import com.rtnp.demo.ui.panel.data.DynamicProgressBarComponent
import com.rtnp.demo.ui.panel.data.BuildReserveComponent
import com.rtnp.demo.ui.panel.tool.DeployButtonTool
import com.rtnp.demo.ui.panel.tool.BuildReserveRenderTool
import kotlin.math.min

object ComponentRenderTool {

    private const val PADDING_HORIZONTAL = 16f
    private const val PRESS_ANIM_DURATION = 0.1f

    private val pressAnimStartTimes = mutableMapOf<String, Long>()
    private val releaseAnimStartTimes = mutableMapOf<String, Long>()

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 28f
    }
    private val buttonBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val buttonBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    private val buttonTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 24f
        textAlign = Paint.Align.CENTER
    }
    private val animLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.RIGHT
    }
    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isFilterBitmap = true
    }

    fun drawComponents(canvas: Canvas, components: List<PanelComponent>) {
        val panelRect = PanelBox.rect()
        val buttons = components.filterIsInstance<InteractiveButtonComponent>()

        for (comp in components) {
            when (comp) {
                is PanelComponent.Text -> drawText(canvas, comp, panelRect)
                is PanelComponent.Image -> drawImage(canvas, comp, panelRect)
                is InteractiveButtonComponent -> {
                    val idx = buttons.indexOf(comp)
                    drawInteractiveButton(canvas, comp, panelRect, idx)
                }
                is HealthBarComponent -> drawProgressWithScale(canvas, comp, panelRect) {
                    HealthBarRenderTool.draw(canvas, comp, panelRect)
                }
                is SpeedBarComponent -> drawProgressWithScale(canvas, comp, panelRect) {
                    SpeedBarRenderTool.draw(canvas, comp, panelRect)
                }
                is LifetimeBarComponent -> drawProgressWithScale(canvas, comp, panelRect) {
                    LifetimeBarRenderTool.draw(canvas, comp, panelRect)
                }
                is StorageBarComponent -> drawProgressWithScale(canvas, comp, panelRect) {
                    StorageBarRenderTool.draw(canvas, comp, panelRect)
                }
                is BuildProgressComponent -> drawProgressWithScale(canvas, comp, panelRect) {
                    BuildProgressRenderTool.draw(canvas, comp, panelRect)
                }
                is BuildReserveComponent -> drawProgressWithScale(canvas, comp, panelRect) {
                    BuildReserveRenderTool.draw(canvas, comp, panelRect)
                }
                is DynamicProgressBarComponent -> drawProgressWithScale(canvas, comp, panelRect) {
                    DynamicProgressBarRenderTool.draw(canvas, comp, panelRect)
                }
                is PanelComponent.Placeholder -> { /* 不渲染 */ }
                is PanelComponent.Spacer -> {}
                else -> {}
            }
        }

        for (comp in components) {
            TooltipRenderTool.draw(canvas, comp, panelRect)
            if (comp is DynamicProgressBarComponent) {
                DetailPanelRenderTool.draw(canvas, comp, panelRect)
            }
        }
    }

    private fun drawProgressWithScale(canvas: Canvas, comp: PanelComponent, panelRect: RectF, drawFn: () -> Unit) {
        val key = when (comp) {
            is HealthBarComponent -> "H_${comp.layoutY}"
            is SpeedBarComponent -> "S_${comp.layoutY}"
            is LifetimeBarComponent -> "L_${comp.layoutY}"
            is StorageBarComponent -> "ST_${comp.layoutY}"
            is BuildProgressComponent -> "B_${comp.layoutY}"
            is BuildReserveComponent -> "BR_${comp.layoutY}"
            is DynamicProgressBarComponent -> "D_${comp.slotKey}"  // ★ 唯一 key
            else -> return
        }

        val isPressed = when (comp) {
            is HealthBarComponent -> comp.isPressed
            is SpeedBarComponent -> comp.isPressed
            is LifetimeBarComponent -> comp.isPressed
            is StorageBarComponent -> comp.isPressed
            is BuildProgressComponent -> comp.isPressed
            is BuildReserveComponent -> comp.isPressed
            is DynamicProgressBarComponent -> comp.isPressed
            else -> false
        }

        val now = System.currentTimeMillis()
        val scale: Float

        if (isPressed) {
            if (!pressAnimStartTimes.containsKey(key)) {
                pressAnimStartTimes[key] = now
            }
            releaseAnimStartTimes.remove(key)
            val elapsed = (now - (pressAnimStartTimes[key] ?: now)) / 1000f
            val progress = (elapsed / PRESS_ANIM_DURATION).coerceIn(0f, 1f)
            scale = 1f + (0.8f - 1f) * progress
        } else {
            if (pressAnimStartTimes.containsKey(key)) {
                if (!releaseAnimStartTimes.containsKey(key)) {
                    releaseAnimStartTimes[key] = now
                }
                val elapsed = (now - (releaseAnimStartTimes[key] ?: now)) / 1000f
                val progress = (elapsed / PRESS_ANIM_DURATION).coerceIn(0f, 1f)
                scale = 0.8f + (1f - 0.8f) * progress
                if (progress >= 1f) {
                    pressAnimStartTimes.remove(key)
                    releaseAnimStartTimes.remove(key)
                }
            } else {
                scale = 1f
            }
        }

        val barLeft = panelRect.left + ProgressBarRenderTool.MARGIN_H
        val barRight = panelRect.right - ProgressBarRenderTool.MARGIN_H
        val barTop = panelRect.top + comp.layoutY
        val barBottom = barTop + comp.computedHeight
        val cx = (barLeft + barRight) / 2f
        val cy = (barTop + barBottom) / 2f

        if (scale != 1f) {
            canvas.save()
            canvas.scale(scale, scale, cx, cy)
            drawFn()
            canvas.restore()
        } else {
            drawFn()
        }
    }

    private fun drawText(canvas: Canvas, comp: PanelComponent.Text, panelRect: RectF) {
        textPaint.textSize = comp.textSize
        val x = panelRect.left + PADDING_HORIZONTAL
        val fm = textPaint.fontMetrics
        val y = panelRect.top + comp.layoutY + comp.computedHeight - fm.descent
        canvas.drawText(comp.content, x, y, textPaint)
    }

    private fun drawImage(canvas: Canvas, comp: PanelComponent.Image, panelRect: RectF) {
        val bmp = comp.bitmap ?: return
        val left = panelRect.centerX() - comp.width / 2f
        val top = panelRect.top + comp.layoutY
        canvas.drawBitmap(bmp, null, RectF(left, top, left + comp.width, top + comp.height), null)
    }

    private fun drawInteractiveButton(canvas: Canvas, comp: InteractiveButtonComponent, panelRect: RectF, index: Int) {
        val halfWidth = comp.width / 2f
        val centerX = panelRect.centerX()
        val left = centerX - halfWidth
        val right = centerX + halfWidth
        val top = panelRect.top + comp.layoutY
        val bottom = top + comp.computedHeight

        val rect = RectF(left, top, right, bottom)
        val scale = comp.animScale
        val scaleCenterX = rect.centerX()
        val scaleCenterY = rect.centerY()
        val rotation = ButtonAnimTool.getRotation(index)

        val deployData = comp.customData as? DeployButtonTool.DeployButtonData
        val (bgAlpha, borderAlpha) = if (deployData != null) {
            DeployButtonTool.getAlpha(comp)
        } else {
            WeaponButtonTool.getAlpha(comp)
        }

        buttonBgPaint.alpha = bgAlpha
        buttonBorderPaint.alpha = borderAlpha

        canvas.save()
        canvas.scale(scale, scale, scaleCenterX, scaleCenterY)

        buttonBgPaint.color = comp.backgroundColor
        canvas.drawRoundRect(rect, InteractiveButtonTool.CORNER_RADIUS, InteractiveButtonTool.CORNER_RADIUS, buttonBgPaint)

        comp.borderBitmap?.let { bitmap ->
            val bmpWidth = bitmap.width.toFloat()
            val bmpHeight = bitmap.height.toFloat()
            val dstWidth = rect.width()
            val dstHeight = rect.height()
            val bitmapScale = minOf(dstWidth / bmpWidth, dstHeight / bmpHeight)
            val drawWidth = bmpWidth * bitmapScale
            val drawHeight = bmpHeight * bitmapScale
            val drawLeft = rect.centerX() - drawWidth / 2f
            val drawTop = rect.centerY() - drawHeight / 2f
            val drawRect = RectF(drawLeft, drawTop, drawLeft + drawWidth, drawTop + drawHeight)
            bitmapPaint.alpha = borderAlpha
            canvas.drawBitmap(bitmap, null, drawRect, bitmapPaint)
        }

        canvas.drawRoundRect(rect, InteractiveButtonTool.CORNER_RADIUS, InteractiveButtonTool.CORNER_RADIUS, buttonBorderPaint)

        comp.iconBitmap?.let { icon ->
            canvas.save()
            val iconMargin = 6f
            val iconRect = RectF(
                rect.left + iconMargin,
                rect.top + iconMargin,
                rect.right - iconMargin,
                rect.bottom - iconMargin
            )
            val iconScale = minOf(iconRect.width() / icon.width, iconRect.height() / icon.height)
            val drawWidth = icon.width * iconScale
            val drawHeight = icon.height * iconScale
            val drawLeft = iconRect.centerX() - drawWidth / 2f
            val drawTop = iconRect.centerY() - drawHeight / 2f

            if (rotation != 0f) {
                canvas.rotate(rotation, iconRect.centerX(), iconRect.centerY())
            }

            bitmapPaint.alpha = borderAlpha
            canvas.drawBitmap(icon, null, RectF(drawLeft, drawTop, drawLeft + drawWidth, drawTop + drawHeight), bitmapPaint)
            canvas.restore()
        } ?: run {
            if (comp.label.isNotEmpty()) {
                val textY = rect.centerY() - (buttonTextPaint.descent() + buttonTextPaint.ascent()) / 2f
                canvas.drawText(comp.label, rect.centerX(), textY, buttonTextPaint)
            }
        }
        CooldownRenderTool.draw(canvas, comp, rect)
        EnergyRingRenderTool.draw(canvas, comp, rect)

        canvas.restore()

        // 部署按钮覆盖层
        if (deployData != null) {
            val overlay = DeployButtonTool.getOverlay(comp)
            val overlayAlpha = DeployButtonTool.getOverlayAlpha(comp)
            if (overlay != null && overlayAlpha > 0) {
                val overlayRect = RectF(
                    rect.left + 6f,
                    rect.top + 6f,
                    rect.right - 6f,
                    rect.bottom - 6f
                )
                bitmapPaint.alpha = overlayAlpha
                canvas.drawBitmap(overlay, null, overlayRect, bitmapPaint)
                bitmapPaint.alpha = 255
            }
        }

        // 武器按钮覆盖层
        val extra = WeaponButtonTool.getExtra(comp)
        if (extra != null && WeaponButtonTool.isWeaponOff(comp) && extra.offOverlay != null) {
            val overlayRect = RectF(
                rect.left + 6f,
                rect.top + 6f,
                rect.right - 6f,
                rect.bottom - 6f
            )
            bitmapPaint.alpha = 255
            canvas.drawBitmap(extra.offOverlay, null, overlayRect, bitmapPaint)
        }

        if (comp.animLabel.isNotEmpty() && index >= 0) {
            val alpha = ButtonAnimTool.getLabelAlpha(index)
            val labelScale = ButtonAnimTool.getLabelScale(index)
            if (alpha > 0f && labelScale > 0f) {
                animLabelPaint.textSize = buttonTextPaint.textSize * labelScale * 3f
                animLabelPaint.alpha = (alpha * 255).toInt().coerceIn(0, 255)
                val textX = panelRect.left
                val textY = rect.centerY() - (animLabelPaint.descent() + animLabelPaint.ascent()) / 2f
                canvas.drawText(comp.animLabel, textX, textY, animLabelPaint)
            }
        }

        buttonBgPaint.alpha = 255
        buttonBorderPaint.alpha = 255
        buttonBorderPaint.color = Color.WHITE
        bitmapPaint.alpha = 255
    }
}