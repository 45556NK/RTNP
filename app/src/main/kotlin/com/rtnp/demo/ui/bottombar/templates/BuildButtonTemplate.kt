package com.rtnp.demo.ui.bottombar.templates

import com.rtnp.demo.core.GameConstants
import com.rtnp.demo.core.TemplateRegistry
import com.rtnp.demo.ui.bottombar.BottomBarButtonTemplate
import com.rtnp.demo.ui.bottombar.tools.BottomBarItemPanelProxy
import com.rtnp.demo.ui.bottombar.tools.BottomBarUnitInfoPanelProxy
import com.rtnp.demo.ui.tools.ItemGridPanelTool

class BuildButtonTemplate : BottomBarButtonTemplate() {

    override val id = "build"
    override val displayName = "建筑"
    override val iconPath = "images/build_btn.png"
    override val mutexGroup: String? = "panel"

    override fun onClick() {
        val buildItems = TemplateRegistry.templates.filter {
            it.type == "base" &&
            it.faction == GameConstants.FACTION_PLAYER &&
            it.category != "临时建筑"
        }
        BottomBarItemPanelProxy.show(
            items = buildItems,
            multiSelect = false,
            mode = ItemGridPanelTool.PanelMode.BUILD
        ) { selected ->
            if (selected != null) BottomBarUnitInfoPanelProxy.show(selected)
            else BottomBarUnitInfoPanelProxy.hide()
        }
    }

    override fun onDeactivate() {
        BottomBarItemPanelProxy.hide()
        BottomBarUnitInfoPanelProxy.hide()
    }
}