package com.rtnp.demo.ui.bottombar.templates

import com.rtnp.demo.core.TemplateRegistry
import com.rtnp.demo.ui.bottombar.BottomBarButtonTemplate
import com.rtnp.demo.ui.bottombar.tools.BottomBarItemPanelProxy
import com.rtnp.demo.ui.bottombar.tools.BottomBarUnitInfoPanelProxy
import com.rtnp.demo.ui.tools.ItemGridPanelTool

class InventoryButtonTemplate : BottomBarButtonTemplate() {

    override val id = "inventory"
    override val displayName = "物品"
    override val iconPath = "images/inventory_btn.png"
    override val mutexGroup: String? = "panel"

    override fun onClick() {
        // 打开（只有从非激活→激活时调用）
        BottomBarItemPanelProxy.show(
            items = TemplateRegistry.templates,
            multiSelect = false,
            mode = ItemGridPanelTool.PanelMode.ITEM
        ) { selected ->
            if (selected != null) BottomBarUnitInfoPanelProxy.show(selected)
            else BottomBarUnitInfoPanelProxy.hide()
        }
    }

    override fun onDeactivate() {
        // 关闭（只有从激活→非激活时调用）
        BottomBarItemPanelProxy.hide()
        BottomBarUnitInfoPanelProxy.hide()
    }
}