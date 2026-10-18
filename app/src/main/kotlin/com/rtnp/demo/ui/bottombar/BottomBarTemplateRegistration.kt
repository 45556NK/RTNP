package com.rtnp.demo.ui.bottombar

import com.rtnp.demo.ui.bottombar.templates.BuildButtonTemplate
import com.rtnp.demo.ui.bottombar.templates.FleetButtonTemplate
import com.rtnp.demo.ui.bottombar.templates.InventoryButtonTemplate
import com.rtnp.demo.ui.bottombar.templates.PauseButtonTemplate

/**
 * 底部栏模板注册表（显式注册）。
 *
 * 所有底部栏模板都在这里手动实例化并注册。
 * 由 [BottomBarV2] 构造时自动调用，外部无需参与。
 */
object BottomBarTemplateRegistration {

    fun registerAll(bar: BottomBarV2) {
        bar.registerTemplate(PauseButtonTemplate())
        bar.registerTemplate(BuildButtonTemplate())
        bar.registerTemplate(FleetButtonTemplate())
        bar.registerTemplate(InventoryButtonTemplate())
    }
}