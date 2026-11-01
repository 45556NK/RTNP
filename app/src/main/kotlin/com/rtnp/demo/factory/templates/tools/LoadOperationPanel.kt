package com.rtnp.demo.factory.templates.tools

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.rtnp.demo.core.GameConstants
import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.movement.ItemManager
import com.rtnp.demo.ui.FloatingWindowTool
import com.rtnp.demo.ui.HexButton
import com.rtnp.demo.ui.HexGridUtil
import com.rtnp.demo.ui.WarningMessage
import com.rtnp.demo.ui.tools.clear.selector.CommonSliderTool
import kotlin.math.sqrt

/**
 * 荷载操作面板。
 *
 * 使用方式：
 *   LoadOperationPanel.show(
 *       mode = LoadOperationPanel.Mode.GET,
 *       operator = host,
 *       target = selectedUnit,
 *       screenWidth = tools.getScreenWidth(),
 *       screenHeight = tools.getScreenHeight()
 *   )
 *
 * 转移方向：
 *   取出模式 TAKE：target → operator，六边形显示 operator 拥有的货物
 *   获取模式 GET ：operator → target，六边形显示 target 拥有的货物
 */
object LoadOperationPanel {

    enum class Mode { TAKE, GET }

    private var current: Content? = null

    fun show(
        mode: Mode,
        operator: PlacedObject,
        target: PlacedObject,
        screenWidth: Int,
        screenHeight: Int,
        onBack: () -> Unit
    ) {
        hide()
        val c = Content(mode, operator, target, screenHeight) {
            hide()
            onBack()
        }
        current = c
        FloatingWindowTool.register(c)
        FloatingWindowTool.onOutsideClick = { hide() }
        FloatingWindowTool.show(
            startX = screenWidth / 2f,
            startY = screenHeight / 2f
        )
    }

    fun hide() {
        current?.let { FloatingWindowTool.unregister(it) }
        current = null
        FloatingWindowTool.onOutsideClick = null
        FloatingWindowTool.hide()
    }

    fun isShowing(): Boolean = current != null && FloatingWindowTool.isShowing()

    // ============================================================
    //  Content：悬浮窗内容组件
    // ============================================================

    private class Content(
        val mode: Mode,
        val operator: PlacedObject,
        val target: PlacedObject,
        val screenHeight: Int,
        private val onBack: () -> Unit
    ) : FloatingWindowTool.FloatingWindowComponent {

        companion object {
            private val HEX_BG_COLOR = Color.rgb(255, 200, 0)
            private val HEX_UNSELECTED_BORDER = Color.argb(102, 173, 216, 230)
            private val HEX_SELECTED_BORDER = Color.argb(255, 173, 216, 230)
            private const val CHARS_PER_LINE = 4
            private const val BOTTOM_HEIGHT_RATIO = 0.38f
        }

        // ============ 字号 ============

        private val titleSize = GameConstants.getSmallFontSize(screenHeight) * 1.2f
        private val labelSize = GameConstants.getSmallFontSize(screenHeight)
        private val buttonTextSize = GameConstants.getSmallFontSize(screenHeight)

        // ============ 数据 ============

        private data class HexEntry(
            val key: String,
            val label: String,
            val total: Int
        )

        private val entries = mutableListOf<HexEntry>()
        private val hexButtons = mutableListOf<HexButton>()
        private var selectedIndex = -1

        // ============ 布局缓存 ============

        private var hexSize = 0f
        private var hexFontSize = 0f
        private var hexBorderWidth = 0f

        private var slider: CommonSliderTool? = null
        private val confirmRect = RectF()

        private var baseScale = 1f
        private var multiTouchStartDist = 0f
        private var multiTouchStartScale = 1f
        private var multiTouchActive = false

        private var pressedHex: HexButton? = null
        private var confirmPressed = false

        private var lastWindowRect = RectF()
        private var layoutDirty = true
        private var hexLayoutDirty = true
        private var resetSliderOnNextLayout = false

        // ============ 画笔 ============

        private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            textSize = titleSize
        }
        private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            textSize = labelSize
        }
        private val confirmBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = 0xFF66BB6A.toInt()
        }
        private val confirmPressedBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = 0xFF4CAF50.toInt()
        }
        private val confirmBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.WHITE
        }
        private val confirmTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            textSize = buttonTextSize
        }

        init {
            buildEntries()
        }

        // ============ 数据构建 ============

        private fun buildEntries() {
            entries.clear()
            entries.add(HexEntry("__back__", "返回", 0))

            val source = when (mode) {
                Mode.TAKE -> operator
                Mode.GET -> target
            }
            val items = source.collectedItems ?: return
            for ((name, count) in items) {
                if (count <= 0) continue
                entries.add(HexEntry(name, wrapHexText(name), count))
            }
        }

        private fun wrapHexText(text: String): String {
            if (text.length <= CHARS_PER_LINE) return text
            val sb = StringBuilder()
            var n = 0
            for (c in text) {
                if (n > 0 && n % CHARS_PER_LINE == 0) sb.append('\n')
                sb.append(c)
                n++
            }
            return sb.toString()
        }

        // ============ 绘制入口 ============

        override fun draw(canvas: Canvas, windowRect: RectF) {
            if (lastWindowRect != windowRect) {
                lastWindowRect.set(windowRect)
                layoutDirty = true
                hexLayoutDirty = true
            }
            if (layoutDirty) {
                layoutDirty = false
                hexLayoutDirty = false
                layoutAll(windowRect)
            } else if (hexLayoutDirty) {
                hexLayoutDirty = false
                layoutHexButtons(windowRect)
            }

            drawTitle(canvas, windowRect)
            for (btn in hexButtons) btn.draw(canvas)
            if (selectedIndex in 1 until entries.size) {
                drawBottom(canvas, windowRect)
            }
        }

        private fun drawTitle(canvas: Canvas, wr: RectF) {
            val name = target.displayName ?: target.name
            val cy = wr.top + titleSize * 1.6f
            val baseline = cy - (titlePaint.descent() + titlePaint.ascent()) / 2f
            canvas.drawText(name, wr.centerX(), baseline, titlePaint)
        }

        private fun drawBottom(canvas: Canvas, wr: RectF) {
            val entry = entries.getOrNull(selectedIndex) ?: return
            val sl = slider ?: return

            // 文本："获取 X / N" 或 "取出 X / N"
            val cur = (sl.progress * entry.total).toInt()
            val prefix = if (mode == Mode.TAKE) "取出" else "获取"
            val text = "$prefix $cur / ${entry.total}"
            val labelY = getLabelY(wr)
            val labelBaseline = labelY - (labelPaint.descent() + labelPaint.ascent()) / 2f
            canvas.drawText(text, wr.centerX(), labelBaseline, labelPaint)

            // 滑动条
            sl.draw(canvas)

            // 确定按钮
            val r = confirmRect.height() * 0.2f
            val bg = if (confirmPressed) confirmPressedBgPaint else confirmBgPaint
            canvas.drawRoundRect(confirmRect, r, r, bg)
            confirmBorderPaint.strokeWidth = confirmRect.height() * 0.05f
            canvas.drawRoundRect(confirmRect, r, r, confirmBorderPaint)
            val btnBaseline = confirmRect.centerY() - (confirmTextPaint.descent() + confirmTextPaint.ascent()) / 2f
            canvas.drawText("确定", confirmRect.centerX(), btnBaseline, confirmTextPaint)
        }

        // ============ 布局 ============

        private fun layoutAll(wr: RectF) {
            layoutHexButtons(wr)
            layoutBottom(wr)
        }

        private fun layoutHexButtons(wr: RectF) {
            hexButtons.clear()
            if (entries.isEmpty()) return

            hexSize = wr.width() / 10f * baseScale
            hexFontSize = GameConstants.getSmallFontSize(wr.height().toInt()) * baseScale
            hexBorderWidth = 8f * baseScale

            val hasBottom = selectedIndex in 1 until entries.size
            val titleAreaH = titleSize * 2.4f
            val bottomAreaH = if (hasBottom) wr.height() * BOTTOM_HEIGHT_RATIO else 0f
            val usableTop = wr.top + titleAreaH
            val usableBottom = wr.bottom - bottomAreaH
            val centerY = (usableTop + usableBottom) / 2f

            val coords = HexGridUtil.generateHexCoordsWithBorder(
                entries.size, hexSize, hexBorderWidth,
                wr.centerX(), centerY
            )

            for (i in entries.indices) {
                val e = entries[i]
                val c = coords[i]
                val btn = HexButton(c[0], c[1], hexSize, e.label)
                btn.fillColor = HEX_BG_COLOR
                btn.strokeColor = if (i == selectedIndex) HEX_SELECTED_BORDER else HEX_UNSELECTED_BORDER
                btn.pressedStrokeColor = HEX_SELECTED_BORDER
                btn.textColor = Color.BLACK
                btn.fontSize = hexFontSize
                btn.strokeWidth = hexBorderWidth
                btn.buildPaths()
                hexButtons.add(btn)
            }
        }

        private fun layoutBottom(wr: RectF) {
            if (selectedIndex !in 1 until entries.size) {
                slider = null
                return
            }

            val oldProgress = if (resetSliderOnNextLayout) 0f else (slider?.progress ?: 0f)
            resetSliderOnNextLayout = false

            val sliderW = wr.width() * 0.7f
            val sliderX = wr.left + (wr.width() - sliderW) / 2f
            val sliderY = getSliderY(wr)
            slider = CommonSliderTool(
                x = sliderX,
                y = sliderY,
                width = sliderW,
                screenHeight = screenHeight,
                initialProgress = oldProgress
            ) { }

            val btnW = wr.width() * 0.35f
            val btnH = wr.height() * 0.1f
            val btnCx = wr.centerX()
            val btnCy = wr.bottom - wr.height() * 0.06f - btnH / 2f
            confirmRect.set(
                btnCx - btnW / 2f, btnCy - btnH / 2f,
                btnCx + btnW / 2f, btnCy + btnH / 2f
            )
        }

        private fun getLabelY(wr: RectF): Float {
            val bottomTop = wr.bottom - wr.height() * BOTTOM_HEIGHT_RATIO
            return bottomTop + labelSize * 1.6f
        }

        private fun getSliderY(wr: RectF): Float {
            val bottomTop = wr.bottom - wr.height() * BOTTOM_HEIGHT_RATIO
            val bottomH = wr.height() * BOTTOM_HEIGHT_RATIO
            return bottomTop + bottomH * 0.45f
        }

        // ============ 触摸 ============

        override fun onTouchDown(x: Float, y: Float) {
            val wr = FloatingWindowTool.getWindowRect()
            val sx = wr.left + x
            val sy = wr.top + y
            pressedHex = null
            confirmPressed = false

            if (selectedIndex in 1 until entries.size) {
                if (confirmRect.contains(sx, sy)) {
                    confirmPressed = true
                    return
                }
                if (slider?.onTouchDown(sx, sy) == true) return
            }

            for (btn in hexButtons) {
                if (btn.isPointInside(sx, sy)) {
                    btn.onTouchDown()
                    pressedHex = btn
                    return
                }
            }
        }

        override fun onTouchMove(x: Float, y: Float) {
            val wr = FloatingWindowTool.getWindowRect()
            val sx = wr.left + x
            val sy = wr.top + y

            if (confirmPressed) return
            if (slider?.isDragging() == true) {
                slider?.onTouchMove(sx, sy)
                return
            }
            pressedHex?.let {
                if (!it.isPointInside(sx, sy)) {
                    it.cancel()
                    pressedHex = null
                }
            }
        }

        override fun onTouchUp(x: Float, y: Float) {
            val wr = FloatingWindowTool.getWindowRect()
            val sx = wr.left + x
            val sy = wr.top + y

            if (confirmPressed) {
                confirmPressed = false
                if (confirmRect.contains(sx, sy)) performTransfer()
                return
            }

            if (slider?.isDragging() == true) {
                slider?.onTouchUp(sx, sy)
                return
            }

            pressedHex?.let { btn ->
                if (btn.isPointInside(sx, sy)) {
                    btn.onTouchUp()
                    val idx = hexButtons.indexOf(btn)
                    if (idx >= 0) handleHexClick(idx)
                } else {
                    btn.cancel()
                }
                pressedHex = null
            }
        }

        private fun handleHexClick(index: Int) {
            if (index == 0) {
                if (selectedIndex in 1 until entries.size) {
                    // 有选中 → 清除选中，回到网格全屏
                    selectedIndex = -1
                    layoutDirty = true
                } else {
                    // 无选中 → 返回上一级（LoadFactoryTemplate 的 hex 选择）
                    onBack()
                }
                return
            }
            if (selectedIndex != index) {
                resetSliderOnNextLayout = true
            }
            selectedIndex = index
            layoutDirty = true
        }

        // ============ 转移 ============

        private fun performTransfer() {
            val entry = entries.getOrNull(selectedIndex) ?: return
            val sl = slider ?: return
            val requested = (sl.progress * entry.total).toInt()
            if (requested <= 0) {
                WarningMessage.show("请选择有效数量")
                return
            }

            val source = if (mode == Mode.TAKE) operator else target
            val dest = if (mode == Mode.TAKE) target else operator

            val sourceMap = source.collectedItems
            val have = sourceMap?.get(entry.key) ?: 0
            if (have <= 0) {
                WarningMessage.show("来源方无该货物")
                return
            }

            // 目标方剩余容量可容纳的最大数量
            val itemDef = ItemManager.getInstance().getItemDef(entry.key)
            val spacePerItem = (itemDef?.space ?: 1).coerceAtLeast(1)
            val destStored = getTotalStored(dest)
            val destSpaceLeft = (dest.storageCapacity - destStored).coerceAtLeast(0)
            val destCanTake = destSpaceLeft / spacePerItem

            if (destCanTake <= 0) {
                WarningMessage.show("目标方仓储已满")
                return
            }

            // 实际转移量：min(请求量, 来源拥有量, 目标可容纳量)
            val actual = minOf(requested, have, destCanTake)

            sourceMap?.set(entry.key, have - actual)
            if ((sourceMap?.get(entry.key) ?: 0) <= 0) sourceMap?.remove(entry.key)

            if (dest.collectedItems == null) dest.collectedItems = mutableMapOf()
            val destMap = dest.collectedItems!!
            destMap[entry.key] = (destMap[entry.key] ?: 0) + actual

            // 只有部分成功才警告；全部成功不警告
            if (actual < requested) {
                WarningMessage.show("目标方仓储容量不足，仅转移 $actual")
            }

            // 不关闭界面，刷新货物列表
            val currentKey = entry.key
            buildEntries()
            val newIdx = entries.indexOfFirst { it.key == currentKey }
            if (newIdx >= 1) {
                selectedIndex = newIdx
            } else {
                selectedIndex = -1
            }
            resetSliderOnNextLayout = true
            layoutDirty = true
        }

        /** 计算单位已占用的仓储空间 */
        private fun getTotalStored(unit: PlacedObject): Int {
            val items = unit.collectedItems ?: return 0
            var total = 0
            for ((name, count) in items) {
                val def = ItemManager.getInstance().getItemDef(name)
                val space = def?.space ?: 1
                total += count * space
            }
            return total
        }

        // ============ 多指缩放 ============

        override fun onMultiTouchStart(firstX: Float, firstY: Float, secondX: Float, secondY: Float) {
            multiTouchStartDist = dist(firstX, firstY, secondX, secondY)
            multiTouchStartScale = baseScale
            multiTouchActive = true
        }

        override fun onMultiTouchMove(firstX: Float, firstY: Float, secondX: Float, secondY: Float) {
            if (!multiTouchActive) return
            val d = dist(firstX, firstY, secondX, secondY)
            if (multiTouchStartDist <= 0f) return
            baseScale = (multiTouchStartScale * (d / multiTouchStartDist)).coerceIn(0.4f, 1.4f)
            hexLayoutDirty = true
        }

        override fun onMultiTouchEnd() {
            multiTouchActive = false
        }

        private fun dist(x1: Float, y1: Float, x2: Float, y2: Float): Float {
            val dx = x1 - x2
            val dy = y1 - y2
            return sqrt(dx * dx + dy * dy)
        }
    }
}