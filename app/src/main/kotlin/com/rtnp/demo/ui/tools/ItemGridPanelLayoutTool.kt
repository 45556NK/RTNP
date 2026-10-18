package com.rtnp.demo.ui.tools

import android.graphics.RectF
import com.rtnp.demo.ui.bottombar.tools.BottomBarLayoutTool

/**
 * 物品展示面板布局工具（纯静态）。
 *
 * 面板位置：
 *  - 宽度 = 屏幕宽 × 2/5
 *  - 高度 = 屏幕高 × 3/7
 *  - 右边缘 = 屏幕右 - 屏幕宽 × 1/100
 *  - 底边 = 底部栏顶排按钮顶部 - 一个按钮间隙
 *
 * 槽位布局：
 *  - 2 列
 *  - 槽宽 = 面板宽 × 24/50
 *  - 列间隙 = 面板宽 × 2/50
 *  - 正方形槽位
 *  - 行间距 = 列间隙
 */
object ItemGridPanelLayoutTool {

    const val PANEL_WIDTH_RATIO = 2f / 5f
    const val PANEL_HEIGHT_RATIO = 3f / 7f
    const val PANEL_RIGHT_MARGIN_RATIO = 1f / 100f
    const val PANEL_CORNER_RATIO = 1f / 20f

    const val SLOT_WIDTH_RATIO_OF_PANEL = 24f / 50f
    const val SLOT_GAP_RATIO_OF_PANEL = 2f / 50f
    const val SLOT_CORNER_RATIO = 1f / 10f
    const val SLOT_BORDER_RATIO = 1f / 20f

    const val COLUMNS = 2

    /** 计算面板矩形 */
    @JvmStatic
    fun computePanelRect(screenWidth: Int, screenHeight: Int): RectF {
        val w = screenWidth * PANEL_WIDTH_RATIO
        val h = screenHeight * PANEL_HEIGHT_RATIO
        val marginRight = screenWidth * PANEL_RIGHT_MARGIN_RATIO
        val right = screenWidth - marginRight
        val left = right - w

        val diameter = screenWidth * BottomBarLayoutTool.DIAMETER_RATIO
        val gap = diameter * BottomBarLayoutTool.GAP_RATIO
        val margin = gap
        val radius = diameter / 2f
        val step = diameter + gap

        // 顶排按钮顶部
        val upperRowTop = screenHeight - margin - diameter - step
        // 面板底部 = 顶排按钮顶部 - 2 个按钮直径
        val bottom = upperRowTop - 2f * diameter
        val top = bottom - h

        return RectF(left, top, right, bottom)
    }

    /** 槽位宽 */
    @JvmStatic
    fun getSlotWidth(panelWidth: Float): Float = panelWidth * SLOT_WIDTH_RATIO_OF_PANEL

    /** 槽位间隙（列间隙 = 行间隙） */
    @JvmStatic
    fun getSlotGap(panelWidth: Float): Float = panelWidth * SLOT_GAP_RATIO_OF_PANEL

    /** 内容总高度 */
    @JvmStatic
    fun getContentHeight(panelRect: RectF, itemCount: Int): Float {
        if (itemCount <= 0) return 0f
        val rows = (itemCount + COLUMNS - 1) / COLUMNS
        val slotW = getSlotWidth(panelRect.width())
        val gap = getSlotGap(panelRect.width())
        return rows * slotW + (rows - 1) * gap
    }

    /** 最大滚动偏移 */
    @JvmStatic
    fun getMaxScroll(panelRect: RectF, itemCount: Int): Float {
        val content = getContentHeight(panelRect, itemCount)
        return (content - panelRect.height()).coerceAtLeast(0f)
    }

    /**
     * 获取指定索引槽位的屏幕矩形。
     * @param scrollY 当前滚动偏移（正值 = 内容向上滚）
     */
    @JvmStatic
    fun getSlotRect(panelRect: RectF, itemCount: Int, index: Int, scrollY: Float): RectF {
        val slotW = getSlotWidth(panelRect.width())
        val slotH = slotW
        val gap = getSlotGap(panelRect.width())

        val col = index % COLUMNS
        val row = index / COLUMNS

        val left = panelRect.left + col * (slotW + gap)
        val top = panelRect.top + row * (slotH + gap) - scrollY
        return RectF(left, top, left + slotW, top + slotH)
    }

    /**
     * 命中判定：返回被点击的槽位索引，未命中返回 -1。
     */
    @JvmStatic
    fun hitTestSlot(panelRect: RectF, itemCount: Int, scrollY: Float, x: Float, y: Float): Int {
        for (i in 0 until itemCount) {
            if (getSlotRect(panelRect, itemCount, i, scrollY).contains(x, y)) return i
        }
        return -1
    }
}