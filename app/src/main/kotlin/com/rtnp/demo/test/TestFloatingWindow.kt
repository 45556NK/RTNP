package com.rtnp.demo.ui.test

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.rtnp.demo.ui.FloatingWindowTool
import com.rtnp.demo.ui.WarningMessage
import com.rtnp.demo.ui.tools.clear.selector.CommonSliderTool

/**
 * 测试用悬浮窗（内含文本 / 按钮 / 两个滑块）。
 * 元素之间垂直间隙固定 20px。
 */
object TestFloatingWindow {

    private var content: Content? = null

    fun show(context: Context, screenHeight: Int) {
        if (content != null) return
        val c = Content(context, screenHeight)
        content = c
        FloatingWindowTool.register(c)
        FloatingWindowTool.show()
    }

    fun hide() {
        content?.let {
            FloatingWindowTool.unregister(it)
            content = null
        }
        FloatingWindowTool.hide()
    }

    fun isShowing(): Boolean = FloatingWindowTool.isShowing() && content != null

    private class Content(
        private val context: Context,
        private val screenHeight: Int
    ) : FloatingWindowTool.FloatingWindowComponent {

        companion object {
            private const val GAP_PX = 20f
        }

        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
        }
        private val buttonBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        private val buttonBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.WHITE
        }
        private val buttonTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textAlign = Paint.Align.CENTER
        }

        private var slider1: CommonSliderTool? = null
        private var slider2: CommonSliderTool? = null
        private var layoutDone = false

        private var buttonPressed = false

        // 布局缓存
        private var textBaseline = 0f
        private var buttonRect = RectF()

        override fun draw(canvas: Canvas, windowRect: RectF) {
            if (!layoutDone) {
                layoutDone = true
                doLayout(windowRect)
            }

            canvas.drawText("测试悬浮窗", windowRect.centerX(), textBaseline, textPaint)

            buttonBgPaint.color = if (buttonPressed) 0xFFAAAAAA.toInt() else 0xC8C8C8C8.toInt()
            canvas.drawRoundRect(buttonRect, buttonRect.height() * 0.2f, buttonRect.height() * 0.2f, buttonBgPaint)
            buttonBorderPaint.strokeWidth = windowRect.width() * 0.006f
            canvas.drawRoundRect(buttonRect, buttonRect.height() * 0.2f, buttonRect.height() * 0.2f, buttonBorderPaint)
            buttonTextPaint.textSize = buttonRect.height() * 0.5f
            val btnTextBaseline = buttonRect.centerY() - (buttonTextPaint.descent() + buttonTextPaint.ascent()) / 2f
            canvas.drawText("测试按钮", buttonRect.centerX(), btnTextBaseline, buttonTextPaint)

            slider1?.draw(canvas)
            slider2?.draw(canvas)
        }

        override fun onTouchDown(x: Float, y: Float) {
            val wr = FloatingWindowTool.getWindowRect()
            val sx = wr.left + x
            val sy = wr.top + y

            if (buttonRect.contains(sx, sy)) {
                buttonPressed = true
                return
            }

            slider1?.let { if (it.onTouchDown(sx, sy)) return }
            slider2?.let { if (it.onTouchDown(sx, sy)) return }
        }

        override fun onTouchMove(x: Float, y: Float) {
            if (buttonPressed) return
            val wr = FloatingWindowTool.getWindowRect()
            val sx = wr.left + x
            val sy = wr.top + y
            slider1?.onTouchMove(sx, sy)
            slider2?.onTouchMove(sx, sy)
        }

        override fun onTouchUp(x: Float, y: Float) {
            val wr = FloatingWindowTool.getWindowRect()
            val sx = wr.left + x
            val sy = wr.top + y

            if (buttonPressed) {
                buttonPressed = false
                if (buttonRect.contains(sx, sy)) {
                    WarningMessage.show("测试按钮被点击")
                }
                return
            }

            slider1?.onTouchUp(sx, sy)
            slider2?.onTouchUp(sx, sy)
        }

        private fun doLayout(windowRect: RectF) {
            val contentLeft = windowRect.left + GAP_PX
            val contentRight = windowRect.right - GAP_PX
            val contentWidth = contentRight - contentLeft

            // 文本
            val textSize = windowRect.height() * 0.08f
            textPaint.textSize = textSize
            val textHeight = textPaint.descent() - textPaint.ascent()

            // 按钮高度
            val btnHeight = windowRect.height() * 0.1f

            // 滑块长杆高度 = screenHeight / 80（与 CommonSliderTool 内部一致）
            val sliderHeight = screenHeight / 80f

            // 从顶部开始，按 20px 间隙依次排列
            var cursorY = windowRect.top + GAP_PX

            // 1. 文本
            textBaseline = cursorY + (-textPaint.ascent())
            cursorY += textHeight + GAP_PX

            // 2. 按钮
            val btnWidth = contentWidth * 0.5f
            val btnCx = windowRect.centerX()
            buttonRect.set(
                btnCx - btnWidth / 2f, cursorY,
                btnCx + btnWidth / 2f, cursorY + btnHeight
            )
            cursorY += btnHeight + GAP_PX

            // 3. 滑块 1
            val sliderX = contentLeft + contentWidth * 0.1f
            val sliderWidth = contentWidth * 0.8f
            val sliderY1 = cursorY + sliderHeight / 2f
            slider1 = CommonSliderTool(
                x = sliderX,
                y = sliderY1,
                width = sliderWidth,
                screenHeight = screenHeight,
                initialProgress = 0.3f
            ) { p -> android.util.Log.d("TestSlider", "滑块1: ${(p * 100).toInt()}%") }
            cursorY += sliderHeight + GAP_PX

            // 4. 滑块 2
            val sliderY2 = cursorY + sliderHeight / 2f
            slider2 = CommonSliderTool(
                x = sliderX,
                y = sliderY2,
                width = sliderWidth,
                screenHeight = screenHeight,
                initialProgress = 0.7f
            ) { p -> android.util.Log.d("TestSlider", "滑块2: ${(p * 100).toInt()}%") }
        }
    }
}