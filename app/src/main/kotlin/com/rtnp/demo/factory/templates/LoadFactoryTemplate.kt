package com.rtnp.demo.factory.templates

import android.graphics.Color
import android.graphics.RectF
import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.factory.FactorySlotData
import com.rtnp.demo.factory.FactoryTemplate
import com.rtnp.demo.factory.FactoryTools
import com.rtnp.demo.factory.templates.tools.LoadOperationPanel
import com.rtnp.demo.strategy.CooldownMode
import com.rtnp.demo.ui.hexselector.HexSelectorTool
import com.rtnp.demo.ui.InfoBoxTool

/**
 * 荷载工厂
 *
 * 介绍：用于在停泊位上装载或卸载货物。
 *
 * 逻辑：
 *  - 点击工厂按钮，打开六边形蜂窝选择器（停泊单位列表）
 *  - 中心第一个六边形为返回按钮，按下即关闭悬浮窗
 *  - 其余六边形：每个停泊单位生成一个（有贴图显示贴图，无贴图显示名称）
 *  - 选中一个单位后，下方显示信息框（单位名称 + 取消/获取/取出）
 *  - 获取：把被操作方的货物转移到操作方
 *  - 取出：把操作方的货物转移到被操作方
 */
class LoadFactoryTemplate : FactoryTemplate() {

    override val id = "load_factory"
    override val displayName = "荷载"
    override val description = "用于在停泊位上装载或卸载货物"

    override val cooldown = 0f
    override val cooldownMode = CooldownMode.MANUAL

    companion object {
        private val HEX_BG_COLOR = Color.rgb(255, 200, 0)
        private val HEX_UNSELECTED_BORDER = Color.argb(102, 173, 216, 230)
        private val HEX_SELECTED_BORDER = Color.argb(255, 173, 216, 230)

        private const val CANCEL_ICON = "images/ui/panel_cancel_btn.png"
        private const val GET_ICON = "images/ui/panel_back_btn.png"
        private const val TAKE_ICON = "images/ui/action_btn.png"

        private const val CHARS_PER_LINE = 4
    }

    override fun onUpdate(
        host: PlacedObject,
        slotIndex: Int,
        deltaTime: Float,
        tools: FactoryTools,
        slotData: FactorySlotData
    ) {
        // 无持续逻辑
    }

    override fun onClicked(
        host: PlacedObject,
        slotIndex: Int,
        tools: FactoryTools,
        slotData: FactorySlotData
    ) {
        openHexSelector(host, slotIndex, tools, slotData)
    }

    private fun openHexSelector(
        host: PlacedObject,
        slotIndex: Int,
        tools: FactoryTools,
        slotData: FactorySlotData
    ) {
        // 1. 收集停泊单位
        val allUnits = tools.getAllUnits()
        val dockedUnitIds = host.dockedList.map { it.unitId }.toSet()
        val dockedUnits = allUnits.filter { it.uniqueId in dockedUnitIds }

        // 2. 构建 hex 列表
        val hexItems = mutableListOf<HexSelectorTool.HexItem>()

        // 2.1 返回按钮
        hexItems.add(
            HexSelectorTool.HexItem(
                backgroundColor = HEX_BG_COLOR,
                infoText = "返回",
                hexText = "返回",
                unselectedBorderColor = HEX_UNSELECTED_BORDER,
                selectedBorderColor = HEX_SELECTED_BORDER
            )
        )

        // 2.2 每个停泊单位一个 hex
        for (unit in dockedUnits) {
            val name = unit.displayName ?: unit.name
            val bitmap = unit.textureBitmap
            hexItems.add(
                HexSelectorTool.HexItem(
                    backgroundColor = HEX_BG_COLOR,
                    infoText = name,
                    hexText = if (bitmap != null) "" else wrapHexText(name),
                    iconPath = null,
                    iconBitmap = bitmap,
                    unselectedBorderColor = HEX_UNSELECTED_BORDER,
                    selectedBorderColor = HEX_SELECTED_BORDER
                )
            )
        }

        val screenW = tools.getScreenWidth()
        val screenH = tools.getScreenHeight()

        // 3. InfoBoxProvider
        val provider = object : HexSelectorTool.InfoBoxProvider {
            override fun buildInfoBox(infoBoxId: Int, selectedIndex: Int, infoBoxRect: RectF) {
                // 返回按钮：立即关闭
                if (selectedIndex == 0) {
                    HexSelectorTool.hide()
                    return
                }

                // 第一行显示选中单位名称
                val unitName = hexItems[selectedIndex].infoText
                InfoBoxTool.setText(infoBoxId, unitName)

                // 底部三按钮：取消 / 获取 / 取出
                val W = infoBoxRect.width()
                val H = infoBoxRect.height()
                val btnSize = H / 4f
                val gap = H / 20f
                val btnY = H - btnSize - gap

                val totalWidth = btnSize * 3f + gap * 2f
                val startX = (W - totalWidth) / 2f

                InfoBoxTool.addButton(
                    taskId = infoBoxId,
                    x = startX,
                    y = btnY,
                    size = btnSize,
                    bgColor = Color.rgb(255, 182, 193),
                    label = "取消",
                    texturePath = CANCEL_ICON,
                    iconScale = 0.8f
                )
                InfoBoxTool.addButton(
                    taskId = infoBoxId,
                    x = startX + btnSize + gap,
                    y = btnY,
                    size = btnSize,
                    bgColor = Color.rgb(173, 216, 230),
                    label = "获取",
                    texturePath = GET_ICON,
                    iconScale = 0.8f
                )
                InfoBoxTool.addButton(
                    taskId = infoBoxId,
                    x = startX + (btnSize + gap) * 2f,
                    y = btnY,
                    size = btnSize,
                    bgColor = Color.rgb(200, 230, 200),
                    label = "取出",
                    texturePath = TAKE_ICON,
                    iconScale = 0.8f
                )
            }

            override fun onButtonClick(infoBoxId: Int, buttonIndex: Int, selectedIndex: Int) {
                when (buttonIndex) {
                    0 -> HexSelectorTool.cancelSelection()
                    1 -> {
                        val unit = dockedUnits.getOrNull(selectedIndex - 1) ?: return
                        HexSelectorTool.hide()
                        LoadOperationPanel.show(
                            mode = LoadOperationPanel.Mode.GET,
                            operator = host,
                            target = unit,
                            screenWidth = screenW,
                            screenHeight = screenH,
                            onBack = { openHexSelector(host, slotIndex, tools, slotData) }
                        )
                    }
                    2 -> {
                        val unit = dockedUnits.getOrNull(selectedIndex - 1) ?: return
                        HexSelectorTool.hide()
                        LoadOperationPanel.show(
                            mode = LoadOperationPanel.Mode.TAKE,
                            operator = host,
                            target = unit,
                            screenWidth = screenW,
                            screenHeight = screenH,
                            onBack = { openHexSelector(host, slotIndex, tools, slotData) }
                        )
                    }
                }
            }
        }

        // 4. 打开选择器
        HexSelectorTool.show(
            startX = screenW / 2f,
            startY = screenH / 2f,
            hexItems = hexItems,
            listener = object : HexSelectorTool.SelectorListener {
                override fun onConfirmed(index: Int) {}
                override fun onCancelled() {}
            },
            infoBoxProvider = provider
        )
    }

    /**
     * 把名称按每 CHARS_PER_LINE 个字符插一个换行，让 HexButton 自动多行绘制。
     */
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
}