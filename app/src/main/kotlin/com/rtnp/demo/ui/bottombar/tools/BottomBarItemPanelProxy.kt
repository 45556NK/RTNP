package com.rtnp.demo.ui.bottombar.tools

import android.content.Context
import android.graphics.Canvas
import android.view.MotionEvent
import com.rtnp.demo.core.InventoryItem
import com.rtnp.demo.ui.tools.ItemGridPanelTool
import com.rtnp.demo.ui.tools.ItemGridPanelTool.PanelMode

/**
 * 物品展示面板代理。
 */
object BottomBarItemPanelProxy {

    // ==================== 系统接口 ====================

    fun init(context: Context) { ItemGridPanelTool.init(context) }

    fun setScreenSize(w: Int, h: Int) { ItemGridPanelTool.setScreenSize(w, h) }

    fun contains(x: Float, y: Float): Boolean {
        if (!ItemGridPanelTool.isShowing()) return false
        return ItemGridPanelTool.getPanelRect().contains(x, y)
    }

    fun onTouchEvent(event: MotionEvent): Boolean = ItemGridPanelTool.onTouchEvent(event)

    fun draw(canvas: Canvas) { ItemGridPanelTool.draw(canvas) }

    // ==================== 模板接口 ====================

    fun show(
        items: List<InventoryItem>,
        multiSelect: Boolean,
        mode: PanelMode,
        onSelectionChanged: (InventoryItem?) -> Unit
    ) {
        ItemGridPanelTool.show(items, multiSelect, mode, onSelectionChanged)
    }

    fun hide() { ItemGridPanelTool.hide() }

    fun isShowing(): Boolean = ItemGridPanelTool.isShowing()

    fun getSelectedItems(): List<InventoryItem> = ItemGridPanelTool.getSelectedItems()

    fun getMode(): PanelMode = ItemGridPanelTool.getMode()
}