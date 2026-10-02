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
 * 信息框工具
 * 职责：显示临时信息说明，位于UI层，覆盖在提示框之上
 * 支持多任务，可指定关闭，文字支持换行
 * 支持为信息框添加圆形按钮（多按钮）
 * 按钮支持按压缩放动画，并显示名称标签动画
 * 按钮贴图缩放由外部通过 iconScale 参数决定
 * 信息框底色可在创建时由外部传入
 * 文字颜色可在设置时由外部传入
 */
object InfoBoxTool {

    private val tasks = LinkedHashMap<Int, InfoBoxData>()
    private var nextId = 0

    private var context: Context? = null

    private val cornerRadius = 16f
    private val borderWidth = 3f

    private const val PRESS_ANIM_DURATION = 0.1f
    private const val PRESS_SCALE = 0.9f
    private const val LABEL_ANIM_DURATION = 0.1f
    private const val LABEL_START_SCALE = 0.2f
    private const val LABEL_OFFSET_Y = 20f

    /** 按钮点击回调：参数为 (taskId, buttonIndex) */
    var onButtonClick: ((taskId: Int, buttonIndex: Int) -> Unit)? = null

    private var pressedTaskId: Int = -1
    private var pressedButtonIndex: Int = -1

    private data class InfoButtonData(
        val rect: RectF,
        val bgColor: Int,
        val label: String,
        val texturePath: String?,
        val iconScale: Float = 1f,
        var bitmap: Bitmap? = null,
        var isPressed: Boolean = false,
        var pressAnimStart: Long = 0L,
        var releaseAnimStart: Long = 0L,
        var labelVisible: Boolean = false,
        var labelAnimStart: Long = 0L
    )

    private data class InfoBoxData(
        val rect: RectF,
        var text: String = "",
        var textColor: Int = Color.WHITE,   // ★ 新增：文字颜色
        var visible: Boolean = true,
        val bgColor: Int = Color.argb(204, 50, 50, 50), // 默认浅灰色，20%透明度
        val buttons: MutableList<InfoButtonData> = mutableListOf()
    )

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(128, 0, 0, 139)
        style = Paint.Style.STROKE
        strokeWidth = borderWidth
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.LEFT
    }

    private val buttonBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val buttonBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(76, 173, 216, 230)
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private val buttonTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textAlign = Paint.Align.CENTER
    }

    private val labelTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(178, 173, 216, 230) // 70%透明度浅青色
        textAlign = Paint.Align.CENTER
    }

    @JvmStatic
    fun init(context: Context) {
        this.context = context.applicationContext
    }

    fun isVisible(): Boolean = tasks.isNotEmpty()

    @JvmStatic
    fun show(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        bgColor: Int = Color.argb(204, 50, 50, 50) // 默认浅灰色
    ): Int {
        val id = ++nextId
        tasks[id] = InfoBoxData(
            rect = RectF(x, y, x + width, y + height),
            bgColor = bgColor
        )
        return id
    }

    @JvmStatic
    fun hide(id: Int) {
        if (pressedTaskId == id) {
            pressedTaskId = -1
            pressedButtonIndex = -1
        }
        tasks.remove(id)
    }

    /**
     * 设置文本
     * @param textColor 文字颜色，默认白色
     */
    @JvmStatic
    @JvmOverloads
    fun setText(id: Int, text: String, textColor: Int = Color.WHITE) {
        tasks[id]?.text = text
        tasks[id]?.textColor = textColor
    }

    @JvmStatic
    fun addButton(
        taskId: Int,
        x: Float,
        y: Float,
        size: Float,
        bgColor: Int,
        label: String,
        texturePath: String? = null,
        iconScale: Float = 1f
    ): Int {
        val data = tasks[taskId] ?: return -1
        val button = InfoButtonData(
            rect = RectF(x, y, x + size, y + size),
            bgColor = bgColor,
            label = label,
            texturePath = texturePath,
            iconScale = iconScale
        )
        data.buttons.add(button)
        return data.buttons.size - 1
    }

    fun onTouchEvent(event: MotionEvent): Boolean {
        if (tasks.isEmpty()) return false

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                for ((taskId, data) in tasks) {
                    if (!data.visible) continue
                    for (i in data.buttons.indices) {
                        if (isButtonHit(data, i, event.x, event.y)) {
                            val button = data.buttons[i]
                            button.isPressed = true
                            button.pressAnimStart = System.currentTimeMillis()
                            button.labelVisible = true
                            button.labelAnimStart = System.currentTimeMillis()

                            pressedTaskId = taskId
                            pressedButtonIndex = i
                            return true
                        }
                    }
                }
                return false
            }
            MotionEvent.ACTION_MOVE -> {
                return pressedTaskId >= 0 && pressedButtonIndex >= 0
            }
            MotionEvent.ACTION_UP -> {
                if (pressedTaskId >= 0 && pressedButtonIndex >= 0) {
                    val taskId = pressedTaskId
                    val buttonIndex = pressedButtonIndex
                    val data = tasks[taskId]
                    if (data != null) {
                        val button = data.buttons[buttonIndex]
                        button.isPressed = false
                        button.releaseAnimStart = System.currentTimeMillis()
                        button.labelVisible = false
                        button.labelAnimStart = System.currentTimeMillis()

                        if (isButtonHit(data, buttonIndex, event.x, event.y)) {
                            onButtonClick?.invoke(taskId, buttonIndex)
                        }
                    }
                    pressedTaskId = -1
                    pressedButtonIndex = -1
                    return true
                }
                return false
            }
            MotionEvent.ACTION_CANCEL -> {
                if (pressedTaskId >= 0 || pressedButtonIndex >= 0) {
                    val taskId = pressedTaskId
                    val buttonIndex = pressedButtonIndex
                    tasks[taskId]?.let { data ->
                        data.buttons.getOrNull(buttonIndex)?.let { button ->
                            button.isPressed = false
                            button.releaseAnimStart = System.currentTimeMillis()
                            button.labelVisible = false
                            button.labelAnimStart = System.currentTimeMillis()
                        }
                    }
                    pressedTaskId = -1
                    pressedButtonIndex = -1
                    return true
                }
                return false
            }
        }
        return false
    }

    private fun isButtonHit(data: InfoBoxData, buttonIndex: Int, x: Float, y: Float): Boolean {
        if (buttonIndex < 0 || buttonIndex >= data.buttons.size) return false
        val button = data.buttons[buttonIndex]
        val absLeft = data.rect.left + button.rect.left
        val absTop = data.rect.top + button.rect.top
        val centerX = absLeft + button.rect.width() / 2f
        val centerY = absTop + button.rect.height() / 2f
        val radius = button.rect.width() / 2f
        val dx = x - centerX
        val dy = y - centerY
        return dx * dx + dy * dy <= radius * radius
    }

    fun draw(canvas: Canvas) {
        if (tasks.isEmpty()) return

        val textSize = GameConstants.getSmallFontSize(canvas.height)
        textPaint.textSize = textSize
        buttonTextPaint.textSize = textSize
        labelTextPaint.textSize = textSize

        val padding = 8f
        val lineHeight = textSize * 1.4f

        for ((_, data) in tasks) {
            if (!data.visible) continue
            val rect = data.rect

            // 使用信息框自己的背景色
            bgPaint.color = data.bgColor
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, bgPaint)
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)

            if (data.text.isNotEmpty()) {
                // ★ 使用信息框自己的文字颜色
                textPaint.color = data.textColor
                val lines = data.text.split("\n")
                var y = rect.top + padding + textSize
                for (line in lines) {
                    if (y + textSize > rect.bottom - padding) break
                    canvas.drawText(line, rect.left + padding, y, textPaint)
                    y += lineHeight
                }
            }

            drawButtons(canvas, data, textSize)
        }
    }

    private fun drawButtons(canvas: Canvas, data: InfoBoxData, textSize: Float) {
        for (i in data.buttons.indices) {
            val button = data.buttons[i]
            val now = System.currentTimeMillis()

            val scale = if (button.isPressed) {
                val progress = ((now - button.pressAnimStart) / 1000f / PRESS_ANIM_DURATION).coerceIn(0f, 1f)
                1f + (PRESS_SCALE - 1f) * progress
            } else if (button.releaseAnimStart > 0L) {
                val progress = ((now - button.releaseAnimStart) / 1000f / PRESS_ANIM_DURATION).coerceIn(0f, 1f)
                PRESS_SCALE + (1f - PRESS_SCALE) * progress
            } else {
                1f
            }

            var labelAlpha = 0f
            var labelScale = 0f
            var labelOffsetY = 0f
            var shouldDrawLabel = false

            if (button.labelVisible) {
                val progress = ((now - button.labelAnimStart) / 1000f / LABEL_ANIM_DURATION).coerceIn(0f, 1f)
                labelAlpha = progress
                labelScale = LABEL_START_SCALE + (1f - LABEL_START_SCALE) * progress
                val fromY = 0f
                val toY = -(button.rect.height() / 2f + LABEL_OFFSET_Y)
                labelOffsetY = fromY + (toY - fromY) * progress
                shouldDrawLabel = progress > 0f
            } else if (button.labelAnimStart > 0L) {
                val progress = ((now - button.labelAnimStart) / 1000f / LABEL_ANIM_DURATION).coerceIn(0f, 1f)
                labelAlpha = 1f - progress
                labelScale = 1f - (1f - LABEL_START_SCALE) * progress
                val fromY = -(button.rect.height() / 2f + LABEL_OFFSET_Y)
                val toY = 0f
                labelOffsetY = fromY + (toY - fromY) * progress
                shouldDrawLabel = labelAlpha > 0f
            }

            val centerX = data.rect.left + button.rect.left + button.rect.width() / 2f
            val centerY = data.rect.top + button.rect.top + button.rect.height() / 2f
            val radius = (button.rect.width() / 2f) * scale

            buttonBgPaint.color = button.bgColor
            canvas.drawCircle(centerX, centerY, radius, buttonBgPaint)
            canvas.drawCircle(centerX, centerY, radius, buttonBorderPaint)

            val bitmap = getButtonBitmap(button)
            if (bitmap != null) {
                val src = android.graphics.Rect(0, 0, bitmap.width, bitmap.height)
                val scaledRadius = radius * button.iconScale
                val dst = RectF(
                    centerX - scaledRadius,
                    centerY - scaledRadius,
                    centerX + scaledRadius,
                    centerY + scaledRadius
                )
                canvas.drawBitmap(bitmap, src, dst, null)
            } else if (button.label.isNotEmpty()) {
                val scaledTextSize = textSize * scale
                buttonTextPaint.textSize = scaledTextSize
                val textY = centerY - (buttonTextPaint.descent() + buttonTextPaint.ascent()) / 2f
                canvas.drawText(button.label, centerX, textY, buttonTextPaint)
                buttonTextPaint.textSize = textSize
            }

            if (shouldDrawLabel && button.label.isNotEmpty()) {
                labelTextPaint.textSize = textSize * labelScale
                labelTextPaint.alpha = (labelAlpha * 178).toInt().coerceIn(0, 255)
                val labelY = centerY + labelOffsetY - (labelTextPaint.descent() + labelTextPaint.ascent()) / 2f
                canvas.drawText(button.label, centerX, labelY, labelTextPaint)
                labelTextPaint.alpha = 178
                labelTextPaint.textSize = textSize
            }
        }
    }

    private fun getButtonBitmap(button: InfoButtonData): Bitmap? {
        button.bitmap?.let { return it }
        val path = button.texturePath ?: return null
        val ctx = context ?: return null
        val bmp = ImageManager.getInstance(ctx).getBitmap(path)
        if (bmp != null) {
            button.bitmap = bmp
        }
        return bmp
    }
}