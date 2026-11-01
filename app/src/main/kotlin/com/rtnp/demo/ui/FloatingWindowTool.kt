package com.rtnp.demo.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import com.rtnp.demo.ui.tools.clear.selector.CommonSliderTool

object FloatingWindowTool {

    interface FloatingWindowComponent {
        fun draw(canvas: Canvas, windowRect: RectF)

        fun onTouchDown(x: Float, y: Float) {}
        fun onTouchMove(x: Float, y: Float) {}
        fun onTouchUp(x: Float, y: Float) {}

        fun onMultiTouchStart(firstX: Float, firstY: Float, secondX: Float, secondY: Float) {}
        fun onMultiTouchMove(firstX: Float, firstY: Float, secondX: Float, secondY: Float) {}
        fun onMultiTouchEnd() {}
    }

    /**
     * 滑块代理：把悬浮窗的"相对窗口坐标"转成"屏幕坐标"转发给 slider。
     *
     * 使用方式：
     * ```
     * val wr = FloatingWindowTool.getWindowRect()
     * val slider = CommonSliderTool(
     *     x = wr.left + wr.width() * 0.2f,     // 屏幕坐标，落在 windowRect 内
     *     y = wr.centerY(),
     *     width = wr.width() * 0.6f,
     *     screenHeight = screenHeight,
     *     initialProgress = 0.5f
     * ) { p -> ... }
     * val proxy = FloatingWindowTool.SliderProxy(slider)
     * FloatingWindowTool.register(proxy)
     * ```
     */
    class SliderProxy(
        private val slider: CommonSliderTool
    ) : FloatingWindowComponent {

        /** 本次触摸序列是否由 slider 接管（DOWN 是否命中按钮） */
        private var active = false

        override fun draw(canvas: Canvas, windowRect: RectF) {
            // slider 内部用屏幕坐标绘制，动画完成后 canvas 恒等变换，直接画即可
            slider.draw(canvas)
        }

        override fun onTouchDown(x: Float, y: Float) {
            val wr = getWindowRect()
            active = slider.onTouchDown(wr.left + x, wr.top + y)
        }

        override fun onTouchMove(x: Float, y: Float) {
            if (!active) return
            val wr = getWindowRect()
            slider.onTouchMove(wr.left + x, wr.top + y)
        }

        override fun onTouchUp(x: Float, y: Float) {
            if (!active) return
            val wr = getWindowRect()
            slider.onTouchUp(wr.left + x, wr.top + y)
            active = false
        }

        // 多指：滑块不支持，忽略
    }

    private var isVisible = false
    private var windowRect = RectF()

    private val components = mutableListOf<FloatingWindowComponent>()

    private val cornerRadius = 16f

    private var isTouching = false
    private var isMultiTouch = false

    var onOutsideClick: (() -> Unit)? = null

    private var isAnimating = false
    private var animStartTime = 0L
    private var animStartX = 0f
    private var animStartY = 0f
    private var animStartWidth = 0f
    private var animStartHeight = 0f
    private var targetX = 0f
    private var targetY = 0f
    private var targetWidth = 0f
    private var targetHeight = 0f

    private const val ANIM_DURATION = 0.1f
    private const val ANIM_START_SCALE = 0.005f

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(102, 173, 216, 230)
        style = Paint.Style.FILL
    }

    private var context: Context? = null

    fun init(context: Context) {
        this.context = context.applicationContext
    }

    fun getContext(): Context? = context

    fun onSizeChanged(w: Int, h: Int) {
        val marginHorizontal = w / 8f
        val marginVertical = w / 3f
        windowRect.set(
            marginHorizontal,
            marginVertical,
            w - marginHorizontal,
            h - marginVertical
        )
    }

    fun show(startX: Float? = null, startY: Float? = null) {
        isVisible = true

        if (startX != null && startY != null) {
            targetX = windowRect.left
            targetY = windowRect.top
            targetWidth = windowRect.width()
            targetHeight = windowRect.height()

            animStartWidth = targetWidth * ANIM_START_SCALE
            animStartHeight = targetHeight * ANIM_START_SCALE

            animStartX = startX - animStartWidth / 2f
            animStartY = startY - animStartHeight / 2f

            isAnimating = true
            animStartTime = System.currentTimeMillis()
        }
    }

    fun hide() {
        isVisible = false
        isAnimating = false
        isTouching = false
        isMultiTouch = false
    }

    fun isShowing(): Boolean = isVisible

    fun register(component: FloatingWindowComponent) {
        if (!components.contains(component)) {
            components.add(component)
        }
    }

    fun unregister(component: FloatingWindowComponent) {
        components.remove(component)
    }

    fun getWindowRect(): RectF = windowRect

    fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isVisible) return false
        if (isAnimating) return true

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val x = event.x
                val y = event.y
                if (!windowRect.contains(x, y)) {
                    isTouching = false
                    isMultiTouch = false
                    onOutsideClick?.invoke()
                    return true
                }
                isTouching = true
                isMultiTouch = false
                dispatchTouchDown(relativeX(x), relativeY(y))
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (!isTouching) return true
                if (event.pointerCount >= 2) {
                    val secondX = event.getX(event.actionIndex)
                    val secondY = event.getY(event.actionIndex)
                    if (!windowRect.contains(secondX, secondY)) return true
                    if (!isMultiTouch) {
                        isMultiTouch = true
                        dispatchMultiTouchStart(
                            relativeX(event.getX(0)), relativeY(event.getY(0)),
                            relativeX(event.getX(1)), relativeY(event.getY(1))
                        )
                    } else {
                        dispatchMultiTouchMove(
                            relativeX(event.getX(0)), relativeY(event.getY(0)),
                            relativeX(event.getX(1)), relativeY(event.getY(1))
                        )
                    }
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (!isTouching) return true
                if (event.pointerCount >= 2) {
                    if (!isMultiTouch) {
                        isMultiTouch = true
                        dispatchMultiTouchStart(
                            relativeX(event.getX(0)), relativeY(event.getY(0)),
                            relativeX(event.getX(1)), relativeY(event.getY(1))
                        )
                    } else {
                        dispatchMultiTouchMove(
                            relativeX(event.getX(0)), relativeY(event.getY(0)),
                            relativeX(event.getX(1)), relativeY(event.getY(1))
                        )
                    }
                } else if (event.pointerCount == 1) {
                    if (isMultiTouch) {
                        dispatchMultiTouchEnd()
                        isMultiTouch = false
                    }
                    dispatchTouchMove(relativeX(event.x), relativeY(event.y))
                }
            }
            MotionEvent.ACTION_UP -> {
                if (!isTouching) return true
                if (!isMultiTouch) {
                    dispatchTouchUp(relativeX(event.x), relativeY(event.y))
                } else {
                    dispatchMultiTouchEnd()
                }
                isTouching = false
                isMultiTouch = false
            }
            MotionEvent.ACTION_POINTER_UP -> {
                if (!isTouching) return true
                if (event.pointerCount <= 1) {
                    if (isMultiTouch) {
                        dispatchMultiTouchEnd()
                        isMultiTouch = false
                    }
                    val remainingIndex = if (event.actionIndex == 0) 1 else 0
                    val x = event.getX(remainingIndex)
                    val y = event.getY(remainingIndex)
                    dispatchTouchDown(relativeX(x), relativeY(y))
                } else {
                    if (isMultiTouch) {
                        dispatchMultiTouchMove(
                            relativeX(event.getX(0)), relativeY(event.getY(0)),
                            relativeX(event.getX(1)), relativeY(event.getY(1))
                        )
                    }
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                if (isMultiTouch) {
                    dispatchMultiTouchEnd()
                } else if (isTouching) {
                    dispatchTouchUp(event.x, event.y)
                }
                isTouching = false
                isMultiTouch = false
            }
        }
        return true
    }

    private fun relativeX(screenX: Float): Float = screenX - windowRect.left
    private fun relativeY(screenY: Float): Float = screenY - windowRect.top

    private fun dispatchTouchDown(x: Float, y: Float) {
        for (component in components) component.onTouchDown(x, y)
    }

    private fun dispatchTouchMove(x: Float, y: Float) {
        for (component in components) component.onTouchMove(x, y)
    }

    private fun dispatchTouchUp(x: Float, y: Float) {
        for (component in components) component.onTouchUp(x, y)
    }

    private fun dispatchMultiTouchStart(firstX: Float, firstY: Float, secondX: Float, secondY: Float) {
        for (component in components) component.onMultiTouchStart(firstX, firstY, secondX, secondY)
    }

    private fun dispatchMultiTouchMove(firstX: Float, firstY: Float, secondX: Float, secondY: Float) {
        for (component in components) component.onMultiTouchMove(firstX, firstY, secondX, secondY)
    }

    private fun dispatchMultiTouchEnd() {
        for (component in components) component.onMultiTouchEnd()
    }

    fun draw(canvas: Canvas) {
        if (!isVisible) return
        if (windowRect.isEmpty) return

        var currentRect = windowRect
        var currentCornerRadius = cornerRadius

        if (isAnimating) {
            val now = System.currentTimeMillis()
            val progress = ((now - animStartTime) / 1000f / ANIM_DURATION).coerceIn(0f, 1f)

            if (progress >= 1f) {
                isAnimating = false
                currentRect = windowRect
            } else {
                val currentLeft = animStartX + (targetX - animStartX) * progress
                val currentTop = animStartY + (targetY - animStartY) * progress
                val currentWidth = animStartWidth + (targetWidth - animStartWidth) * progress
                val currentHeight = animStartHeight + (targetHeight - animStartHeight) * progress
                currentRect = RectF(currentLeft, currentTop, currentLeft + currentWidth, currentTop + currentHeight)
                currentCornerRadius = cornerRadius * progress
            }
        }

        canvas.drawRoundRect(currentRect, currentCornerRadius, currentCornerRadius, bgPaint)

        val scaleX = currentRect.width() / windowRect.width()
        val scaleY = currentRect.height() / windowRect.height()
        val scale = minOf(scaleX, scaleY)
        val offsetX = currentRect.centerX() - windowRect.centerX() * scale
        val offsetY = currentRect.centerY() - windowRect.centerY() * scale

        canvas.save()
        canvas.translate(offsetX, offsetY)
        canvas.scale(scale, scale)
        canvas.clipRect(windowRect)

        for (component in components) {
            component.draw(canvas, windowRect)
        }

        canvas.restore()
    }
}