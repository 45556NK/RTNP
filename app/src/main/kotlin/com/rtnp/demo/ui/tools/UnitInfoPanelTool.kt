package com.rtnp.demo.ui.tools

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.rtnp.demo.core.GameConstants
import com.rtnp.demo.core.InventoryItem
import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.core.TemplateRegistry
import com.rtnp.demo.image.ImageManager
import com.rtnp.demo.ui.tools.UnitInfoPanelLayoutTool
import com.rtnp.demo.ui.tools.UnitInfoPanelAnimTool
import com.rtnp.demo.ui.tools.UnitInfoPanelDrawTool

/**
 * 通用单位信息面板（主工具，object 单例，有状态）。
 *
 * 显示内容：
 *  - 上半：贴图（带动画）或形状（静态），正方形内切
 *  - 中间：灰色虚线
 *  - 下半：自动加 3 行（名称/血量/造价）+ 调用者自定义行
 */
object UnitInfoPanelTool {

    data class InfoLine(val text: String, val color: Int)

    private var context: Context? = null
    private var screenWidth = 0
    private var screenHeight = 0

    private var isVisible = false
    private var panelX = 0f
    private var panelY = 0f
    private var currentItem: Any? = null
    private var currentLines: List<InfoLine> = emptyList()

    private var lastDrawTime = 0L

    // 缓存贴图
    private var cachedIcon: Bitmap? = null
    private var cachedProcessedW = 0f
    private var cachedProcessedH = 0f
    private var cachedIsShape = false
    private var cachedShape = ""
    private var cachedShapeColor = 0

    private val measurePaint = Paint(Paint.ANTI_ALIAS_FLAG)

    // ==================== 系统 ====================

    fun init(ctx: Context) {
        context = ctx.applicationContext
    }

    fun setScreenSize(w: Int, h: Int) {
        screenWidth = w
        screenHeight = h
    }

    fun isShowing(): Boolean = isVisible

    // ==================== 显示控制 ====================

    fun show(item: Any, x: Float, y: Float, lines: List<InfoLine>) {
        if (item !is PlacedObject && item !is InventoryItem) return
        currentItem = item
        currentLines = lines
        panelX = x
        panelY = y
        isVisible = true
        lastDrawTime = 0L
        UnitInfoPanelAnimTool.start()
        prepareGraphics(item)
    }

    fun hide() {
        isVisible = false
        currentItem = null
        currentLines = emptyList()
        cachedIcon = null
        UnitInfoPanelAnimTool.stop()
    }

    // ==================== 绘制 ====================

    fun draw(canvas: Canvas) {
        if (!isVisible) return
        val item = currentItem ?: return
        if (screenWidth <= 0 || screenHeight <= 0) return

        val now = System.currentTimeMillis()
        val dt = if (lastDrawTime == 0L) 0f else (now - lastDrawTime) / 1000f
        lastDrawTime = now
        UnitInfoPanelAnimTool.update(dt)

        val textSize = GameConstants.getSmallFontSize(screenHeight)
        measurePaint.textSize = textSize

        val rawLongSide = maxOf(cachedProcessedW, cachedProcessedH)

        // 1. 所有行自然宽度的最大值
        val naturalMaxW = if (currentLines.isEmpty()) 0f
            else currentLines.maxOf { measurePaint.measureText(it.text) }

        // 2. 探测布局（仅取 textAreaWidth 用于换行）
        val probe = UnitInfoPanelLayoutTool.compute(
            panelX, panelY, screenWidth, screenHeight,
            rawLongSide, naturalMaxW, 0, textSize
        )

        // 3. 换行
        val wrappedLines = mutableListOf<String>()
        val wrappedColors = mutableListOf<Int>()
        for (line in currentLines) {
            val parts = UnitInfoPanelLayoutTool.wrapText(
                line.text,
                { s -> measurePaint.measureText(s) },
                probe.textAreaWidth
            )
            for (p in parts) {
                wrappedLines.add(p)
                wrappedColors.add(line.color)
            }
        }

        // 4. 用真实行数重算
        val layout = UnitInfoPanelLayoutTool.compute(
            panelX, panelY, screenWidth, screenHeight,
            rawLongSide, naturalMaxW, wrappedLines.size, textSize
        )

        // 背景
        UnitInfoPanelDrawTool.drawPanelBackground(canvas, layout, screenWidth)

        // 上半
        canvas.save()
        UnitInfoPanelDrawTool.clipToTopSquare(canvas, layout, screenWidth)
        if (cachedIsShape) {
            UnitInfoPanelDrawTool.drawShapeStatic(
                canvas, layout, cachedShape, cachedShapeColor,
                cachedProcessedW, cachedProcessedH
            )
        } else {
            cachedIcon?.let { bmp ->
                UnitInfoPanelDrawTool.drawTexture(
                    canvas, layout, bmp,
                    cachedProcessedW, cachedProcessedH,
                    UnitInfoPanelAnimTool.getRotation(),
                    UnitInfoPanelAnimTool.getScale()
                )
            }
        }
        canvas.restore()

        // 下半文字
        var baselineY = layout.textAreaTop + textSize
        for (i in wrappedLines.indices) {
            UnitInfoPanelDrawTool.drawTextLine(
                canvas, layout.textAreaLeft, baselineY,
                wrappedLines[i], wrappedColors[i], textSize
            )
            baselineY += layout.lineHeight
        }
    }

    // ==================== 内部 ====================
    
    /** 获取处理后的贴图/形状长边（用于代理计算面板尺寸） */
    fun getProcessedSide(item: Any, screenWidth: Int): Float {
        val rawW: Float
        val rawH: Float
        when (item) {
            is PlacedObject -> {
                val bmp = item.textureBitmap
                if (bmp != null && !bmp.isRecycled) {
                    rawW = bmp.width.toFloat()
                    rawH = bmp.height.toFloat()
                } else {
                    val (w, h) = when (item.shape) {
                        "circle", "square" -> Pair(item.size.toFloat(), item.size.toFloat())
                        else -> Pair(item.width.toFloat(), item.height.toFloat())
                    }
                    rawW = w
                    rawH = h
                }
            }
            is InventoryItem -> {
                val bmp = tryLoadIcon(item)
                if (bmp != null) {
                    rawW = bmp.width.toFloat()
                    rawH = bmp.height.toFloat()
                } else {
                    val (w, h) = when (item.shape) {
                        "circle", "square" -> Pair(item.size.toFloat(), item.size.toFloat())
                        else -> Pair(item.width.toFloat(), item.height.toFloat())
                    }
                    rawW = w
                    rawH = h
                }
            }
            else -> return 0f
        }
        val (w, h) = UnitInfoPanelLayoutTool.computeProcessedSize(rawW, rawH, screenWidth)
        return maxOf(w, h)
    }

    private fun tryLoadIcon(item: InventoryItem): Bitmap? {
        if (!item.useTexture || item.texturePath.isNullOrEmpty()) return null
        val ctx = context ?: return null
        return try {
            ImageManager.getInstance(ctx).getBitmap(item.texturePath!!)
        } catch (_: Exception) { null }
    }

    private fun prepareGraphics(item: Any) {
        cachedIcon = null
        cachedIsShape = false

        when (item) {
            is PlacedObject -> {
                val bmp = item.textureBitmap
                if (bmp != null && !bmp.isRecycled) {
                    val (w, h) = UnitInfoPanelLayoutTool.computeProcessedSize(
                        bmp.width.toFloat(), bmp.height.toFloat(), screenWidth
                    )
                    cachedIcon = bmp
                    cachedProcessedW = w
                    cachedProcessedH = h
                } else {
                    // 形状降级
                    cachedIsShape = true
                    cachedShape = item.shape
                    cachedShapeColor = item.color
                    val rawW: Float
                    val rawH: Float
                    when (item.shape) {
                        "circle", "square" -> {
                            rawW = item.size.toFloat()
                            rawH = item.size.toFloat()
                        }
                        else -> {
                            rawW = item.width.toFloat()
                            rawH = item.height.toFloat()
                        }
                    }
                    val (w, h) = UnitInfoPanelLayoutTool.computeProcessedSize(rawW, rawH, screenWidth)
                    cachedProcessedW = w
                    cachedProcessedH = h
                }
            }
            is InventoryItem -> {
                if (item.useTexture && !item.texturePath.isNullOrEmpty()) {
                    val ctx = context
                    if (ctx != null) {
                        try {
                            val bmp = ImageManager.getInstance(ctx).getBitmap(item.texturePath!!)
                            if (bmp != null) {
                                val (w, h) = UnitInfoPanelLayoutTool.computeProcessedSize(
                                    bmp.width.toFloat(), bmp.height.toFloat(), screenWidth
                                )
                                cachedIcon = bmp
                                cachedProcessedW = w
                                cachedProcessedH = h
                                return
                            }
                        } catch (_: Exception) {}
                    }
                }
                // 形状降级
                cachedIsShape = true
                cachedShape = item.shape
                cachedShapeColor = item.color
                val rawW: Float
                val rawH: Float
                when (item.shape) {
                    "circle", "square" -> {
                        rawW = item.size.toFloat()
                        rawH = item.size.toFloat()
                    }
                    else -> {
                        rawW = item.width.toFloat()
                        rawH = item.height.toFloat()
                    }
                }
                val (w, h) = UnitInfoPanelLayoutTool.computeProcessedSize(rawW, rawH, screenWidth)
                cachedProcessedW = w
                cachedProcessedH = h
            }
        }
    }

    /** 供代理构造自动 3 行 */
    fun buildAutoLines(item: Any): List<InfoLine> {
        val lines = mutableListOf<InfoLine>()

        val name: String
        val hpCur: Float
        val hpMax: Float
        var costItem: String? = null
        var costAmount: Int = 0

        when (item) {
            is PlacedObject -> {
                name = item.displayName ?: item.name
                hpCur = item.health.toFloat()
                hpMax = item.originalHealth.toFloat()
                val tpl = TemplateRegistry.templates.firstOrNull { it.name == item.name }
                costItem = tpl?.costItem
                costAmount = tpl?.costAmount ?: 0
            }
            is InventoryItem -> {
                name = item.name
                hpCur = item.health.toFloat()
                hpMax = item.health.toFloat()
                costItem = item.costItem
                costAmount = item.costAmount
            }
            else -> return lines
        }

        if (name.isNotEmpty()) lines.add(InfoLine(name, Color.WHITE))
        lines.add(InfoLine("血量: ${hpCur.toInt()}/${hpMax.toInt()}", Color.RED))
        if (!costItem.isNullOrEmpty() && costAmount > 0) {
            lines.add(InfoLine("造价: $costItem x$costAmount", Color.rgb(255, 215, 0)))
        }
        return lines
    }
}