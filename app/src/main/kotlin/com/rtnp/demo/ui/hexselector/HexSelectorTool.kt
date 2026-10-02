package com.rtnp.demo.ui.hexselector

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import com.rtnp.demo.core.GameConstants
import com.rtnp.demo.image.ImageManager
import com.rtnp.demo.ui.FloatingWindowTool
import com.rtnp.demo.ui.HexButton
import com.rtnp.demo.ui.HexGridUtil
import com.rtnp.demo.ui.InfoBoxTool
import com.rtnp.demo.ui.systempanel.SystemPanelConstants
import kotlin.math.sqrt

/**
 * 通用六边形选择器（独立UI工具）。
 *
 * 工具只做绘制，不做任何业务判断。
 * - hexText 是什么就画什么
 * - iconPath 非 null 就加载并绘制图标
 * 二者独立，可以同时存在、单独存在或都不存在。
 *
 * 信息框支持两种模式：
 * - 默认模式：显示"确定？" + 选中名称，底部取消/确定按钮
 * - 自定义模式：调用者传入 InfoBoxProvider，自行构建文本与按钮
 */
object HexSelectorTool {

    data class HexItem(
        val backgroundColor: Int,
        val infoText: String,
        val hexText: String,
        val iconPath: String? = null
    )

    interface SelectorListener {
        fun onConfirmed(index: Int)
        fun onCancelled()
    }

    /**
     * 信息框自定义接口。
     * 传入此接口时，信息框内容完全由调用者定义。
     */
    interface InfoBoxProvider {
        /**
         * 首次构建信息框。
         * 调用者在此方法中使用 InfoBoxTool 填充文本和按钮。
         * @param infoBoxId InfoBoxTool 分配的 id
         * @param selectedIndex 当前选中的六边形索引
         * @param infoBoxRect 信息框在屏幕上的矩形
         */
        fun buildInfoBox(infoBoxId: Int, selectedIndex: Int, infoBoxRect: RectF)

        /**
         * 数据变化时更新信息框。
         * 默认实现回退到 buildInfoBox（整体重建）。
         * 调用者可以覆盖此方法，只更新变化的部分。
         */
        fun updateInfoBox(infoBoxId: Int, selectedIndex: Int) {
            // 默认：不处理，由 refreshInfoBox 决定是否重建
        }

        /**
         * 按钮点击回调。
         * 调用者可以通过以下方法控制选择器：
         * - HexSelectorTool.confirmSelection() 触发 onConfirmed
         * - HexSelectorTool.cancelSelection() 清空选中
         * - HexSelectorTool.hide() 关闭选择器
         */
        fun onButtonClick(infoBoxId: Int, buttonIndex: Int, selectedIndex: Int)
    }

    private var contentComponent: HexSelectorContentComponent? = null
    private var bottomInfoBoxId: Int = -1
    private var listener: SelectorListener? = null
    private var infoBoxProvider: InfoBoxProvider? = null
    private var currentSelectedIndex: Int = -1
    private var currentHexItems: List<HexItem> = emptyList()

    @JvmStatic
    @JvmOverloads
    fun show(
        startX: Float,
        startY: Float,
        hexItems: List<HexItem>,
        listener: SelectorListener,
        infoBoxProvider: InfoBoxProvider? = null
    ) {
        this.listener = listener
        this.infoBoxProvider = infoBoxProvider
        this.currentSelectedIndex = -1
        this.currentHexItems = hexItems

        FloatingWindowTool.show(startX = startX, startY = startY)
        FloatingWindowTool.onOutsideClick = { hide(true) }

        val component = HexSelectorContentComponent(
            hexItems = hexItems,
            onHexSelected = { index, _ ->
                currentSelectedIndex = index
                if (index >= 0 && index < hexItems.size) {
                    showBottomInfoBox(hexItems[index], index)
                }
            }
        )
        contentComponent = component
        FloatingWindowTool.register(component)
    }

    @JvmStatic
    @JvmOverloads
    fun hide(notifyCancel: Boolean = false) {
        closeBottomInfoBox()
        contentComponent?.let { FloatingWindowTool.unregister(it) }
        contentComponent = null
        FloatingWindowTool.hide()
        FloatingWindowTool.onOutsideClick = null
        val l = listener
        listener = null
        infoBoxProvider = null
        currentSelectedIndex = -1
        currentHexItems = emptyList()
        if (notifyCancel && l != null) {
            l.onCancelled()
        }
    }

    @JvmStatic
    fun confirmSelection() {
        val idx = currentSelectedIndex
        val l = listener
        closeBottomInfoBox()
        if (idx >= 0 && l != null) {
            hide(false)
            l.onConfirmed(idx)
        }
    }

    @JvmStatic
    fun cancelSelection() {
        closeBottomInfoBox()
        contentComponent?.resetSelection()
        currentSelectedIndex = -1
    }

    /**
     * 刷新当前信息框内容。
     * - 有 provider：优先调用 provider.updateInfoBox（局部更新）
     *   若 provider 使用默认空实现（无覆盖），则回退到重建
     * - 无 provider：重建默认信息框
     *
     * 如果信息框当前不存在（玩家未选中），直接返回。
     */
    @JvmStatic
    fun refreshInfoBox() {
        if (bottomInfoBoxId == -1) return
        val idx = currentSelectedIndex
        if (idx < 0 || idx >= currentHexItems.size) return

        val provider = infoBoxProvider
        if (provider != null) {
            // 调用者自行决定如何处理局部更新
            // 若调用者未覆盖此方法，则不会有任何变化，此时由调用者
            // 显式调用 rebuildInfoBox() 强制重建
            provider.updateInfoBox(bottomInfoBoxId, idx)
        } else {
            // 默认模式：直接重建
            showBottomInfoBox(currentHexItems[idx], idx)
        }
    }

    /**
     * 强制重建信息框（供调用者在需要整体刷新时使用）。
     */
    @JvmStatic
    fun rebuildInfoBox() {
        val idx = currentSelectedIndex
        if (idx < 0 || idx >= currentHexItems.size) return
        showBottomInfoBox(currentHexItems[idx], idx)
    }

    // ==================== 信息框 ====================

    private fun showBottomInfoBox(item: HexItem, index: Int) {
        closeBottomInfoBox()

        val windowRect = FloatingWindowTool.getWindowRect()
        val gap = windowRect.width() / 20f
        val infoWidth = windowRect.width() - gap * 2f
        val infoHeight = windowRect.height() / 4f
        val infoLeft = windowRect.left + gap
        val infoTop = windowRect.bottom - infoHeight - gap

        bottomInfoBoxId = InfoBoxTool.show(infoLeft, infoTop, infoWidth, infoHeight)
        val infoBoxRect = RectF(infoLeft, infoTop, infoLeft + infoWidth, infoTop + infoHeight)

        val provider = infoBoxProvider
        if (provider != null) {
            provider.buildInfoBox(bottomInfoBoxId, index, infoBoxRect)
            InfoBoxTool.onButtonClick = { taskId, buttonIndex ->
                if (taskId == bottomInfoBoxId) {
                    provider.onButtonClick(bottomInfoBoxId, buttonIndex, index)
                }
            }
        } else {
            buildDefaultInfoBox(item, infoBoxRect)
        }
    }

    private fun buildDefaultInfoBox(item: HexItem, infoBoxRect: RectF) {
        val confirmText = SystemPanelConstants.getText("hex_selector_confirm")
        InfoBoxTool.setText(bottomInfoBoxId, "$confirmText\n${item.infoText}")

        val buttonDiameter = infoBoxRect.height() / 3f
        val buttonGap = infoBoxRect.height() / 20f
        val totalButtonsWidth = buttonDiameter * 2 + buttonGap
        val buttonsStartX = (infoBoxRect.width() - totalButtonsWidth) / 2f
        val buttonTopY = infoBoxRect.height() - buttonDiameter - buttonGap

        InfoBoxTool.addButton(
            taskId = bottomInfoBoxId,
            x = buttonsStartX,
            y = buttonTopY,
            size = buttonDiameter,
            bgColor = Color.rgb(255, 182, 193),
            label = "取消",
            texturePath = "images/ui/panel_cancel_btn.png",
            iconScale = 0.8f
        )

        InfoBoxTool.addButton(
            taskId = bottomInfoBoxId,
            x = buttonsStartX + buttonDiameter + buttonGap,
            y = buttonTopY,
            size = buttonDiameter,
            bgColor = Color.rgb(173, 216, 230),
            label = "确定",
            texturePath = "images/ui/panel_change_btn.png",
            iconScale = 0.8f
        )

        InfoBoxTool.onButtonClick = { taskId, buttonIndex ->
            if (taskId == bottomInfoBoxId) {
                when (buttonIndex) {
                    0 -> cancelSelection()
                    1 -> confirmSelection()
                }
            }
        }
    }

    private fun closeBottomInfoBox() {
        if (bottomInfoBoxId != -1) {
            InfoBoxTool.hide(bottomInfoBoxId)
            bottomInfoBoxId = -1
        }
        InfoBoxTool.onButtonClick = null
    }

    // ==================== 内部内容组件 ====================

    private class HexSelectorContentComponent(
        private var hexItems: List<HexItem>,
        private val onHexSelected: (Int, RectF) -> Unit
    ) : FloatingWindowTool.FloatingWindowComponent {

        private val hexButtons = mutableListOf<HexButton>()
        private val hexButtonData = mutableListOf<HexButtonData>()

        private var selectedIndex = -1
        private var pressedButton: HexButton? = null
        private val lastWindowRect = RectF()

        private var baseScale = 1f
        private var multiTouchStartDist = 0f
        private var multiTouchStartScale = 1f
        private var isMultiTouchActive = false

        private data class HexButtonData(
            val backgroundColor: Int,
            val infoText: String,
            val hexText: String,
            val iconPath: String?,
            var bitmap: Bitmap? = null
        )

        fun getSelectedIndex(): Int = selectedIndex

        fun resetSelection() {
            selectedIndex = -1
            updateSelectedAppearance()
            lastWindowRect.setEmpty()
        }

        override fun draw(canvas: Canvas, windowRect: RectF) {
            buildButtonsIfNeeded(windowRect)

            for (i in hexButtons.indices) {
                val btn = hexButtons[i]
                btn.draw(canvas)
                val data = hexButtonData.getOrNull(i) ?: continue
                data.bitmap?.let { bmp ->
                    val half = btn.size * 1.6f * 0.9f / 2f
                    val left = btn.centerX - half
                    val top = btn.centerY - half
                    canvas.drawBitmap(bmp, null, RectF(left, top, left + half * 2, top + half * 2), null)
                }
            }
        }

        override fun onTouchDown(x: Float, y: Float) {
            val screenX = x + lastWindowRect.left
            val screenY = y + lastWindowRect.top
            pressedButton = null
            for (btn in hexButtons) {
                if (btn.isPointInside(screenX, screenY)) {
                    btn.onTouchDown()
                    pressedButton = btn
                    return
                }
            }
        }

        override fun onTouchMove(x: Float, y: Float) {
            val screenX = x + lastWindowRect.left
            val screenY = y + lastWindowRect.top
            pressedButton?.let {
                if (!it.isPointInside(screenX, screenY)) {
                    it.cancel()
                    pressedButton = null
                }
            }
        }

        override fun onTouchUp(x: Float, y: Float) {
            val screenX = x + lastWindowRect.left
            val screenY = y + lastWindowRect.top
            pressedButton?.let { btn ->
                if (btn.isPointInside(screenX, screenY)) {
                    btn.onTouchUp()
                    val index = hexButtons.indexOf(btn)
                    if (index >= 0) {
                        selectedIndex = index
                        updateSelectedAppearance()
                        lastWindowRect.setEmpty()
                        val centerRect = RectF(
                            btn.centerX - btn.size, btn.centerY - btn.size,
                            btn.centerX + btn.size, btn.centerY + btn.size
                        )
                        onHexSelected(index, centerRect)
                    }
                } else {
                    btn.cancel()
                }
                pressedButton = null
            }
        }

        override fun onMultiTouchStart(firstX: Float, firstY: Float, secondX: Float, secondY: Float) {
            multiTouchStartDist = distance(firstX, firstY, secondX, secondY)
            multiTouchStartScale = baseScale
            isMultiTouchActive = true
        }

        override fun onMultiTouchMove(firstX: Float, firstY: Float, secondX: Float, secondY: Float) {
            if (!isMultiTouchActive) return
            val currentDist = distance(firstX, firstY, secondX, secondY)
            if (multiTouchStartDist <= 0f) return
            val scaleFactor = currentDist / multiTouchStartDist
            val newScale = (multiTouchStartScale * scaleFactor).coerceIn(0.4f, 1.4f)
            baseScale = newScale
            lastWindowRect.setEmpty()
        }

        override fun onMultiTouchEnd() {
            isMultiTouchActive = false
        }

        private fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
            val dx = x1 - x2
            val dy = y1 - y2
            return sqrt(dx * dx + dy * dy)
        }

        private fun updateSelectedAppearance() {
            for (i in hexButtons.indices) {
                val btn = hexButtons[i]
                btn.strokeColor = if (i == selectedIndex) Color.YELLOW else Color.WHITE
                btn.buildPaths()
            }
        }

        private fun buildButtonsIfNeeded(windowRect: RectF) {
            val sameRect = hexButtons.isNotEmpty() &&
                lastWindowRect.left == windowRect.left &&
                lastWindowRect.top == windowRect.top &&
                lastWindowRect.right == windowRect.right &&
                lastWindowRect.bottom == windowRect.bottom
            if (sameRect) return

            lastWindowRect.set(windowRect)

            hexButtons.clear()
            hexButtonData.clear()

            if (hexItems.isEmpty()) return

            val dynamicHexSize = windowRect.width() / 10f * baseScale
            val fontSize = GameConstants.getSmallFontSize(windowRect.height().toInt()) * baseScale
            val borderWidth = 8f * baseScale

            val coords = HexGridUtil.generateHexCoordsWithBorder(
                hexItems.size, dynamicHexSize, borderWidth,
                windowRect.centerX(), windowRect.centerY() + windowRect.height() * 0.1f
            )

            if (selectedIndex >= 0 && selectedIndex < coords.size) {
                val selectedCoord = coords[selectedIndex]
                val dx = windowRect.centerX() - selectedCoord[0]
                val dy = (windowRect.centerY() + windowRect.height() * 0.1f) - selectedCoord[1]
                for (coord in coords) {
                    coord[0] += dx
                    coord[1] += dy
                }
            }

            for (i in hexItems.indices) {
                val item = hexItems[i]
                val coord = coords[i]

                val btn = HexButton(coord[0], coord[1], dynamicHexSize, item.hexText)
                btn.fillColor = item.backgroundColor
                btn.strokeColor = if (i == selectedIndex) Color.YELLOW else Color.WHITE
                btn.pressedStrokeColor = Color.YELLOW
                btn.textColor = Color.BLACK
                btn.fontSize = fontSize
                btn.buildPaths()
                hexButtons.add(btn)

                val data = HexButtonData(item.backgroundColor, item.infoText, item.hexText, item.iconPath)
                if (item.iconPath != null) {
                    val context = FloatingWindowTool.getContext()
                    if (context != null) {
                        data.bitmap = ImageManager.getInstance(context).getBitmap(item.iconPath)
                    }
                }
                hexButtonData.add(data)
            }
        }
    }
}