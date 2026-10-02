package com.rtnp.demo.ui.panel.template

import android.content.Context
import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.ui.panel.RightPanel
import com.rtnp.demo.ui.panel.data.PanelComponent
import com.rtnp.demo.ui.panel.data.UnitNameComponent
import com.rtnp.demo.ui.panel.tool.ActionButtonTool
import com.rtnp.demo.ui.panel.tool.ButtonBindingTool
import com.rtnp.demo.ui.panel.tool.ExitButtonTool
import com.rtnp.demo.ui.panel.tool.HealthBarTool
import com.rtnp.demo.ui.panel.tool.SystemButtonTool
import com.rtnp.demo.ui.action.ActionFlowController
import com.rtnp.demo.ui.panel.tool.LifetimeBarTool
import com.rtnp.demo.ui.panel.tool.SpeedBarTool
import com.rtnp.demo.ui.panel.tool.StorageBarTool
import com.rtnp.demo.ui.panel.tool.DeployButtonTool

object NormalUnitTemplate {

    fun build(unit: PlacedObject, vw: Int, context: Context): List<PanelComponent> {
        val components = mutableListOf<PanelComponent>()
        val us = RightPanel.getUnitSystem() ?: return@build emptyList()

        UnitNameComponent.create(unit)?.let { components.add(it) }

        // 血量条
        components.add(HealthBarTool.create(unit))
        // 寿命条
        LifetimeBarTool.create(unit)?.let { components.add(it) }
        // 速度条
        components.add(SpeedBarTool.create(unit))
        StorageBarTool.create(unit)?.let { components.add(it) }
        
        components.add(PanelComponent.Placeholder("shield_slot"))

        components.add(PanelComponent.Spacer(60f))

        // 系统管理按钮
        components.add(SystemButtonTool.create(vw, context, us))

        // 退出按钮
        components.add(ExitButtonTool.create(vw, context) {
            RightPanel.unlock()
        })

        // ★ 工程部署按钮：仅工程船显示，逻辑内置在按钮中
        if (unit.miningSpeed > 0f) {
            components.add(DeployButtonTool.create(vw, context, unit))
        }

        // 行动按钮
        components.add(ActionButtonTool.create(vw, context) {
            ActionFlowController.startAction()
        })

        ButtonBindingTool.addToComponents(components,
            ButtonBindingTool.createWeaponButtons(unit, vw, context))
            
        ButtonBindingTool.addToComponents(components,
            ButtonBindingTool.createShieldButtons(unit, vw, context))

        ButtonBindingTool.addToComponents(components,
            ButtonBindingTool.createStrategyButtons(unit, vw, context))
            
        ButtonBindingTool.addToComponents(components,
            ButtonBindingTool.createFactoryButtons(unit, vw, context))

        return components
    }
}