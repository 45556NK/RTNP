package com.rtnp.demo.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.WindowManager

object WarningMessage {

    private enum class Phase {
        IDLE,
        APPEAR,
        FADE_OUT
    }

    private const val MAX_PANEL_WIDTH_RATIO = 1f / 3f
    private const val MIN_PANEL_WIDTH_RATIO = 1f / 8f
    private const val PADDING_RATIO_OF_TEXT_SIZE = 0.6f
    private const val LINE_HEIGHT_RATIO = 1.4f
    private const val CORNER_RADIUS = 10f

    private var phase = Phase.IDLE
    private var phaseTime = 0f
    private var message = ""
    private var screenWidth = 0
    private var screenHeight = 0

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 36f
        textAlign = Paint.Align.CENTER
    }

    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        initialized = true
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = android.util.DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(metrics)
        screenWidth = metrics.widthPixels
        screenHeight = metrics.heightPixels
    }

    fun show(text: String) {
        message = text
        phase = Phase.APPEAR
        phaseTime = 0f
    }

    fun isActive(): Boolean = phase != Phase.IDLE

    fun update(deltaTime: Float) {
        if (phase == Phase.IDLE) return
        phaseTime += deltaTime
        when (phase) {
            Phase.APPEAR -> if (phaseTime >= 0.1f) { phaseTime = 0f; phase = Phase.FADE_OUT }
            Phase.FADE_OUT -> if (phaseTime >= 0.8f) { phase = Phase.IDLE }
            Phase.IDLE -> {}
        }
    }

    // ==================== 布局计算 ====================

    private data class Layout(
        val panelWidth: Float,
        val panelHeight: Float,
        val lines: List<String>,
        val lineHeight: Float
    )

    private fun computeLayout(): Layout {
        val maxPanelWidth = screenWidth * MAX_PANEL_WIDTH_RATIO
        val minPanelWidth = screenWidth * MIN_PANEL_WIDTH_RATIO
        val padding = textPaint.textSize * PADDING_RATIO_OF_TEXT_SIZE
        val lineHeight = textPaint.textSize * LINE_HEIGHT_RATIO

        // 先处理显式 \n 拆行
        val explicitLines = message.split("\n")

        // 计算每行自然宽度
        val naturalMax = explicitLines.maxOfOrNull { textPaint.measureText(it) } ?: 0f

        val panelWidth: Float
        val finalLines: List<String>

        if (naturalMax + padding * 2f <= maxPanelWidth) {
            // 不超：宽度自适应文字（夹在 min~max 之间）
            panelWidth = (naturalMax + padding * 2f).coerceIn(minPanelWidth, maxPanelWidth)
            finalLines = explicitLines
        } else {
            // 超了：固定 maxPanelWidth，字符级换行
            panelWidth = maxPanelWidth
            val textAreaWidth = panelWidth - padding * 2f
            val wrapped = mutableListOf<String>()
            for (rawLine in explicitLines) {
                wrapped.addAll(wrapText(rawLine, textAreaWidth))
            }
            finalLines = wrapped
        }

        val lineCount = finalLines.size.coerceAtLeast(1)
        val panelHeight = lineHeight * lineCount + padding * 2f

        return Layout(panelWidth, panelHeight, finalLines, lineHeight)
    }

    /** 字符级换行（中文友好） */
    private fun wrapText(text: String, maxWidth: Float): List<String> {
        if (text.isEmpty()) return listOf("")
        if (maxWidth <= 0f) return listOf(text)

        val result = mutableListOf<String>()
        val chars = text.toCharArray()
        val sb = StringBuilder()
        var i = 0
        while (i < chars.size) {
            sb.append(chars[i])
            if (textPaint.measureText(sb.toString()) > maxWidth && sb.length > 1) {
                sb.deleteCharAt(sb.length - 1)
                result.add(sb.toString())
                sb.clear()
            } else {
                i++
            }
        }
        if (sb.isNotEmpty()) result.add(sb.toString())
        return if (result.isEmpty()) listOf("") else result
    }

    // ==================== 绘制 ====================

    fun draw(canvas: Canvas) {
        if (phase == Phase.IDLE || screenWidth <= 0 || screenHeight <= 0) return

        val layout = computeLayout()
        val panelWidth = layout.panelWidth
        val panelHeight = layout.panelHeight

        val startY = screenHeight * 1f / 4f
        val endY = -panelHeight

        val (scale, centerY, alpha) = when (phase) {
            Phase.APPEAR -> {
                val t = (phaseTime / 0.1f).coerceIn(0f, 1f)
                val s = 0.8f + 0.4f * t
                val y = startY - screenHeight * 0.1f * t
                Triple(s, y, 255)
            }
            Phase.FADE_OUT -> {
                val t = (phaseTime / 0.8f).coerceIn(0f, 1f)
                val s = 1.2f - 0.2f * t
                val y = startY - screenHeight * 0.1f + (endY - (startY - screenHeight * 0.1f)) * t
                val a = (255 * (1f - t)).toInt()
                Triple(s, y, a)
            }
            Phase.IDLE -> return
        }

        val centerX = screenWidth / 2f

        canvas.save()
        canvas.translate(centerX, centerY)
        canvas.scale(scale, scale)

        val left = -panelWidth / 2f
        val top = -panelHeight / 2f
        val right = panelWidth / 2f
        val bottom = panelHeight / 2f

        bgPaint.color = Color.argb(alpha, 139, 0, 0)
        canvas.drawRoundRect(RectF(left, top, right, bottom), CORNER_RADIUS, CORNER_RADIUS, bgPaint)

        borderPaint.color = Color.argb(alpha, 255, 100, 100)
        canvas.drawRoundRect(RectF(left, top, right, bottom), CORNER_RADIUS, CORNER_RADIUS, borderPaint)

        // 逐行绘制（垂直居中整块文字）
        textPaint.alpha = alpha
        val lineHeight = layout.lineHeight
        val lines = layout.lines
        val totalTextHeight = lineHeight * lines.size
        val firstLineCenterY = -totalTextHeight / 2f + lineHeight / 2f

        for (i in lines.indices) {
            val lineCenterY = firstLineCenterY + i * lineHeight
            val textY = lineCenterY - (textPaint.descent() + textPaint.ascent()) / 2f
            canvas.drawText(lines[i], 0f, textY, textPaint)
        }

        canvas.restore()
    }
}