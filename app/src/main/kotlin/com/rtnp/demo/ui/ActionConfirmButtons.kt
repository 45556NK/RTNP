package com.rtnp.demo.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import com.rtnp.demo.core.GameConstants
import com.rtnp.demo.image.ImageManager

/**
 * 建筑预览的确认/取消按钮。
 *
 * 风格与底部栏一致：
 *  - 圆角正方形，白色边框
 *  - 按压缩放至 0.9（0.1s easeOutCubic），边框变金黄
 *  - 按下时名称浮出（带黑色半透明底）
 *  - 位置紧邻底部栏左侧，与屏幕底边有间隔
 *  - 两个按钮的边框外缘之间间隔 2px
 */
class ActionConfirmButtons(private val context: Context) {

    interface OnActionListener {
        fun onCancel()
        fun onConfirm()
    }

    companion object {
        /** 两个按钮边框外缘之间的间隔（像素） */
        private const val BUTTON_GAP_PX = 2f

        /** 贴图路径（与 ActionPlatform 区分，避免重名） */
        private const val CONFIRM_ICON = "images/ui/preview_confirm_btn.png"
        private const val CANCEL_ICON = "images/ui/preview_cancel_btn.png"
    }

    // ==================== 布局参数 ====================

    private var buttonSize = 0f
    private var cornerRadius = 0f
    private var borderWidth = 0f
    private var gap = 0f
    private var screenHeightPx = 0
    private var isLayouted = false

    @JvmField
    var visible: Boolean = false

    private val cancelRect = RectF()
    private val confirmRect = RectF()

    private var cancelPressed = false
    private var confirmPressed = false

    // ==================== 动画状态 ====================

    private class AnimState {
        var currentScale = 1f
        var targetScale = 1f
        var scaleStart = 1f
        var scaleStartTime = 0L
        var scaleDuration = 0.1f

        var labelAlpha = 0f
        var labelTargetAlpha = 0f
        var labelAlphaStart = 0f
        var labelAlphaStartTime = 0L

        var labelOffset = 1f
        var labelTargetOffset = 1f
        var labelOffsetStart = 1f
        var labelOffsetStartTime = 0L
    }

    private val cancelAnim = AnimState()
    private val confirmAnim = AnimState()

    // ==================== 贴图 ====================

    private var confirmBitmap: Bitmap? = null
    private var cancelBitmap: Bitmap? = null

    // ==================== 画笔 ====================

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
    }
    private val labelBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(200, 0, 0, 0)
        style = Paint.Style.FILL
    }

    private var listener: OnActionListener? = null

    init {
        try {
            val img = ImageManager.getInstance(context)
            confirmBitmap = img.getBitmap(CONFIRM_ICON)
            cancelBitmap = img.getBitmap(CANCEL_ICON)
        } catch (_: Exception) {}
    }

    fun setOnActionListener(listener: OnActionListener) {
        this.listener = listener
    }

    // ==================== 布局 ====================

    fun layout(screenWidth: Int, screenHeight: Int, bottomBarLeft: Int) {
        val barWidth = screenWidth / 2f
        buttonSize = barWidth / 3f
        borderWidth = screenWidth / 40f
        gap = screenWidth / 50f          // 与底部栏间隔一致
        cornerRadius = buttonSize * 0.15f
        screenHeightPx = screenHeight

        val bottom = screenHeight - gap
        val right = bottomBarLeft - gap

        // 确定按钮贴底栏左侧
        confirmRect.set(right - buttonSize, bottom - buttonSize, right, bottom)

        // 取消按钮：其右边框外缘距确定按钮左边框外缘 2px
        // confirmRect.left 与 cancelRect.right 是边框中心线
        // 边框外缘距离 = (confirmRect.left - borderWidth/2) - (cancelRect.right + borderWidth/2)
        // 令其 = BUTTON_GAP_PX
        // → cancelRect.right = confirmRect.left - borderWidth - BUTTON_GAP_PX
        val cancelRight = confirmRect.left - borderWidth - BUTTON_GAP_PX
        cancelRect.set(cancelRight - buttonSize, bottom - buttonSize, cancelRight, bottom)

        isLayouted = true
    }

    // ==================== 触摸 ====================

    fun onTouchEvent(event: MotionEvent): Boolean {
        if (!visible || !isLayouted) return false
        val x = event.x
        val y = event.y

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (cancelRect.contains(x, y)) {
                    cancelPressed = true
                    onPress(cancelAnim)
                    return true
                }
                if (confirmRect.contains(x, y)) {
                    confirmPressed = true
                    onPress(confirmAnim)
                    return true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                // 按住后不因移出取消，仅在抬手时判断命中
                if (cancelPressed || confirmPressed) return true
            }
            MotionEvent.ACTION_UP -> {
                if (cancelPressed) {
                    val hit = cancelRect.contains(x, y)
                    cancelPressed = false
                    onRelease(cancelAnim)
                    if (hit) listener?.onCancel()
                    return true
                }
                if (confirmPressed) {
                    val hit = confirmRect.contains(x, y)
                    confirmPressed = false
                    onRelease(confirmAnim)
                    if (hit) listener?.onConfirm()
                    return true
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                if (cancelPressed) { cancelPressed = false; onRelease(cancelAnim) }
                if (confirmPressed) { confirmPressed = false; onRelease(confirmAnim) }
            }
        }
        return false
    }

    // ==================== 绘制 ====================

    fun draw(canvas: Canvas) {
        if (!visible || !isLayouted) return
        updateAnim(cancelAnim)
        updateAnim(confirmAnim)
        drawButton(canvas, cancelRect, cancelBitmap, cancelAnim, cancelPressed, "取消")
        drawButton(canvas, confirmRect, confirmBitmap, confirmAnim, confirmPressed, "确定")
    }

    private fun drawButton(
        canvas: Canvas,
        rect: RectF,
        icon: Bitmap?,
        anim: AnimState,
        pressed: Boolean,
        name: String
    ) {
        val cx = rect.centerX()
        val cy = rect.centerY()

        canvas.save()
        canvas.scale(anim.currentScale, anim.currentScale, cx, cy)

        // 底色
        bgPaint.color = 0xC8C8C8C8.toInt()
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, bgPaint)

        // 边框
        borderPaint.color = if (pressed) Color.rgb(255, 215, 0) else Color.WHITE
        borderPaint.strokeWidth = borderWidth
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)

        // 贴图
        if (icon != null && !icon.isRecycled) {
            val iconSize = buttonSize * 0.65f
            val ratio = icon.width.toFloat() / icon.height
            val dw: Float
            val dh: Float
            if (ratio >= 1f) { dw = iconSize; dh = iconSize / ratio }
            else { dh = iconSize; dw = iconSize * ratio }
            val dl = cx - dw / 2f
            val dt = cy - dh / 2f
            canvas.drawBitmap(icon, null, RectF(dl, dt, dl + dw, dt + dh), iconPaint)
        }

        canvas.restore()

        // 名称标签（不随按钮缩放）
        if (anim.labelAlpha > 0.01f) {
            val textSize = GameConstants.getSmallFontSize(screenHeightPx)
            labelPaint.textSize = textSize
            labelPaint.alpha = (anim.labelAlpha * 255).toInt().coerceIn(0, 255)

            val textW = labelPaint.measureText(name)
            val textH = labelPaint.textSize
            val pad = textH * 0.25f
            val offsetBase = textH * 0.5f
            val labelCx = cx
            val labelCy = rect.top - textH * 0.9f + anim.labelOffset * offsetBase

            labelBgPaint.alpha = (anim.labelAlpha * 200).toInt().coerceIn(0, 255)
            canvas.drawRoundRect(
                RectF(labelCx - textW / 2f - pad, labelCy - textH / 2f - pad,
                      labelCx + textW / 2f + pad, labelCy + textH / 2f + pad),
                pad * 2f, pad * 2f, labelBgPaint
            )
            val textY = labelCy - (labelPaint.descent() + labelPaint.ascent()) / 2f
            canvas.drawText(name, labelCx, textY, labelPaint)
        }
    }

    // ==================== 动画 ====================

    private fun onPress(s: AnimState) {
        val now = System.currentTimeMillis()
        s.scaleStart = s.currentScale
        s.targetScale = 0.9f
        s.scaleStartTime = now
        s.scaleDuration = 0.1f

        s.labelAlphaStart = s.labelAlpha
        s.labelTargetAlpha = 1f
        s.labelAlphaStartTime = now

        s.labelOffsetStart = s.labelOffset
        s.labelTargetOffset = 0f
        s.labelOffsetStartTime = now
    }

    private fun onRelease(s: AnimState) {
        val now = System.currentTimeMillis()
        s.scaleStart = s.currentScale
        s.targetScale = 1f
        s.scaleStartTime = now
        s.scaleDuration = 0.1f

        s.labelAlphaStart = s.labelAlpha
        s.labelTargetAlpha = 0f
        s.labelAlphaStartTime = now

        s.labelOffsetStart = s.labelOffset
        s.labelTargetOffset = -1f
        s.labelOffsetStartTime = now
    }

    private fun updateAnim(s: AnimState) {
        val now = System.currentTimeMillis()

        val ts = ((now - s.scaleStartTime) / 1000f / s.scaleDuration).coerceIn(0f, 1f)
        s.currentScale = s.scaleStart + (s.targetScale - s.scaleStart) * easeOutCubic(ts)

        val ta = ((now - s.labelAlphaStartTime) / 1000f / 0.15f).coerceIn(0f, 1f)
        s.labelAlpha = s.labelAlphaStart + (s.labelTargetAlpha - s.labelAlphaStart) * easeOutCubic(ta)

        val to = ((now - s.labelOffsetStartTime) / 1000f / 0.2f).coerceIn(0f, 1f)
        s.labelOffset = s.labelOffsetStart + (s.labelTargetOffset - s.labelOffsetStart) * easeOutCubic(to)
    }

    private fun easeOutCubic(t: Float): Float {
        val u = 1f - t
        return 1f - u * u * u
    }
}