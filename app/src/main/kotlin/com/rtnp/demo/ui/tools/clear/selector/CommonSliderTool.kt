package com.rtnp.demo.ui.tools.clear.selector

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import kotlin.math.sqrt

/**
 * 通用滑动选择器。
 *
 * 特点：
 *  - 长杆 + 已填充 + 45° 倾斜的正方形按钮
 *  - 按钮中心可在长杆上左右滑动（含超出杆两端）
 *  - 必须按住按钮才启动拖动；拖动中手指可以离开按钮区域继续拖动，直到松手
 *  - 进度变化实时回调（0.0 ~ 1.0）
 *  - 支持多实例
 *
 * 坐标定义：
 *  - (x, y) 为长杆最左侧中心点
 *  - 长杆从 (x, y) 向右延伸 width
 */
class CommonSliderTool(
    private val x: Float,
    private val y: Float,
    val width: Float,
    screenHeight: Int,
    initialProgress: Float = 0f,
    trackColor: Int = DEFAULT_TRACK_COLOR,
    fillColor: Int = DEFAULT_FILL_COLOR,
    buttonColor: Int = DEFAULT_BUTTON_COLOR,
    private val onProgressChanged: (Float) -> Unit
) {

    companion object {
        /** 默认长杆颜色（浅紫） */
        const val DEFAULT_TRACK_COLOR = 0xFFB19CD9.toInt()
        /** 默认已填充颜色（深紫） */
        const val DEFAULT_FILL_COLOR = 0xFF6A0DAD.toInt()
        /** 默认滑动按钮颜色（蓝紫） */
        const val DEFAULT_BUTTON_COLOR = 0xFF8A2BE2.toInt()

        private const val SQRT2 = 1.41421356f
    }

    // ==================== 尺寸（只读） ====================

    /** 长杆高度 = 屏幕高 / 80 */
    val height: Float = screenHeight / 80f

    /** 滑动按钮边长 = 屏幕高 / 35（原 1/70 的两倍） */
    val buttonSize: Float = screenHeight / 35f

    // ==================== 状态 ====================

    var progress: Float = initialProgress.coerceIn(0f, 1f)
        private set

    private var isDragging = false
    private var dragOffset: Float = 0f

    // ==================== 画笔 ====================

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = trackColor
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = fillColor
    }
    private val buttonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = buttonColor
    }

    // ==================== 对外方法 ====================

    /** 外部设置进度（不触发回调） */
    fun setProgressSilently(p: Float) {
        progress = p.coerceIn(0f, 1f)
    }

    /** 外部设置进度（触发回调） */
    fun setProgress(p: Float) {
        val np = p.coerceIn(0f, 1f)
        if (np == progress) return
        progress = np
        onProgressChanged(progress)
    }

    /** 是否正在拖动 */
    fun isDragging(): Boolean = isDragging

    // ==================== 触摸 ====================

    /**
     * 通用入口：把 MotionEvent 转发给分离的触摸接口。
     * 适用于直接接入 GameTouchHandler 的场景（屏幕坐标）。
     */
    fun onTouchEvent(event: MotionEvent): Boolean {
        return when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> onTouchDown(event.x, event.y)
            MotionEvent.ACTION_MOVE -> onTouchMove(event.x, event.y)
            MotionEvent.ACTION_UP -> onTouchUp(event.x, event.y)
            MotionEvent.ACTION_CANCEL -> onTouchCancel()
            else -> false
        }
    }

    /**
     * 分离的触摸接口：坐标为屏幕坐标。
     * 悬浮窗等场景通过代理调用这些方法。
     */
    fun onTouchDown(screenX: Float, screenY: Float): Boolean {
        if (!isHitButton(screenX, screenY)) return false
        isDragging = true
        dragOffset = screenX - getButtonCenterX()
        return true
    }

    fun onTouchMove(screenX: Float, screenY: Float): Boolean {
        if (!isDragging) return false
        val newCenterX = screenX - dragOffset
        val newProgress = ((newCenterX - x) / width).coerceIn(0f, 1f)
        if (newProgress != progress) {
            progress = newProgress
            onProgressChanged(progress)
        }
        return true
    }

    fun onTouchUp(screenX: Float, screenY: Float): Boolean {
        if (isDragging) {
            isDragging = false
            return true
        }
        return false
    }

    fun onTouchCancel(): Boolean {
        if (isDragging) {
            isDragging = false
            return true
        }
        return false
    }


    // ==================== 绘制 ====================

    fun draw(canvas: Canvas) {
        val cy = y
        val halfH = height / 2f
        val radius = halfH
        val buttonCenterX = getButtonCenterX()

        // 1. 长杆底色（两端圆头）
        val trackRect = RectF(x, cy - halfH, x + width, cy + halfH)
        canvas.drawRoundRect(trackRect, radius, radius, trackPaint)

        // 2. 已填充部分（两端圆头）
        if (buttonCenterX > x) {
            val fillRect = RectF(x, cy - halfH, buttonCenterX, cy + halfH)
            canvas.drawRoundRect(fillRect, radius, radius, fillPaint)
        }

        // 3. 滑动按钮（45° 倾斜正方形，中心对齐长杆中心线）
        val half = buttonSize / 2f
        canvas.save()
        canvas.translate(buttonCenterX, cy)
        canvas.rotate(45f)
        canvas.drawRect(-half, -half, half, half, buttonPaint)
        canvas.restore()
    }

    // ==================== 内部 ====================

    private fun getButtonCenterX(): Float = x + progress * width

    private fun isHitButton(tx: Float, ty: Float): Boolean {
        val cx = getButtonCenterX()
        val r = buttonSize * SQRT2 / 2f
        val dx = tx - cx
        val dy = ty - y
        return dx * dx + dy * dy <= r * r
    }
}