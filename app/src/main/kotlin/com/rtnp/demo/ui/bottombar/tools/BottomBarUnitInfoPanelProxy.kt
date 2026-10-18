package com.rtnp.demo.ui.bottombar.tools

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import com.rtnp.demo.core.GameConstants
import com.rtnp.demo.ui.tools.ItemGridPanelLayoutTool
import com.rtnp.demo.ui.tools.UnitInfoPanelLayoutTool
import com.rtnp.demo.ui.tools.UnitInfoPanelTool
import com.rtnp.demo.ui.tools.UnitInfoPanelTool.InfoLine

/**
 * 单位信息面板代理。
 *
 * 对外接口：
 *  - 系统：init / setScreenSize / isShowing / draw / hide
 *  - 模板：show(item, extraLines)  —— 坐标由系统自动计算
 *
 * 位置规则：
 *  - 底部对齐到"物品展示面板顶部 - 半个底部栏按钮直径"
 *  - 水平居中于物品展示面板
 */
object BottomBarUnitInfoPanelProxy {

    private var screenWidth = 0
    private var screenHeight = 0

    // ==================== 系统接口 ====================

    fun init(context: Context) {
        UnitInfoPanelTool.init(context)
    }

    fun setScreenSize(w: Int, h: Int) {
        screenWidth = w
        screenHeight = h
        UnitInfoPanelTool.setScreenSize(w, h)
    }

    fun isShowing(): Boolean = UnitInfoPanelTool.isShowing()

    fun draw(canvas: Canvas) {
        UnitInfoPanelTool.draw(canvas)
    }

    fun hide() {
        UnitInfoPanelTool.hide()
    }

    // ==================== 模板接口 ====================

    fun show(item: Any, extraLines: List<InfoLine> = emptyList()) {
        if (screenWidth <= 0 || screenHeight <= 0) return

        val allLines = UnitInfoPanelTool.buildAutoLines(item) + extraLines

        val textSize = GameConstants.getSmallFontSize(screenHeight)
        val side = UnitInfoPanelTool.getProcessedSide(item, screenWidth)
        if (side <= 0f) return

        val padding = side * UnitInfoPanelLayoutTool.PADDING_RATIO_OF_SIDE
        val lineHeight = textSize * UnitInfoPanelLayoutTool.LINE_HEIGHT_RATIO

        // 计算自然宽度
        val measurePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.textSize = textSize }
        val naturalMaxW = if (allLines.isEmpty()) 0f
            else allLines.maxOf { measurePaint.measureText(it.text) }

        // 面板宽度
        val maxAllowedW = screenWidth * UnitInfoPanelLayoutTool.MAX_PANEL_WIDTH_RATIO_OF_SCREEN
        var panelWidth = maxOf(side, naturalMaxW + 2f * padding)
        if (panelWidth > maxAllowedW) panelWidth = maxAllowedW

        val textAreaWidth = panelWidth - 2f * padding

        // 换行后总行数
        var totalLines = 0
        for (l in allLines) {
            totalLines += UnitInfoPanelLayoutTool.wrapText(
                l.text,
                { s -> measurePaint.measureText(s) },
                textAreaWidth
            ).size
        }

        val totalHeight = side + padding + totalLines * lineHeight + padding

        // 位置
        val itemPanel = ItemGridPanelLayoutTool.computePanelRect(screenWidth, screenHeight)
        val barDiameter = screenWidth * BottomBarLayoutTool.DIAMETER_RATIO
        val gap = barDiameter * 0.5f
        val y = itemPanel.top - gap - totalHeight
        val x = itemPanel.left + (itemPanel.width() - panelWidth) / 2f

        UnitInfoPanelTool.show(item, x, y, allLines)
    }

    /** 便捷构造行 */
    fun line(text: String, color: Int): InfoLine = InfoLine(text, color)
}