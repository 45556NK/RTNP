// app/src/main/kotlin/com/rtnp/demo/ui/panel/template/NonControllableUnitTemplate.kt
package com.rtnp.demo.ui.panel.template

import android.content.Context
import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.ui.panel.data.PanelComponent
import com.rtnp.demo.ui.panel.data.UnitNameComponent
import com.rtnp.demo.ui.panel.tool.*
import com.rtnp.demo.ui.panel.RightPanel

object NonControllableUnitTemplate {

    fun build(unit: PlacedObject, vw: Int, context: Context): List<PanelComponent> {
        val components = mutableListOf<PanelComponent>()

        UnitNameComponent.create(unit)?.let { components.add(it) }

        LifetimeBarTool.create(unit)?.let { components.add(it) }

        components.add(HealthBarTool.create(unit))

        components.add(SpeedBarTool.create(unit))
        
        StorageBarTool.create(unit)?.let { components.add(it) }
        
        components.add(PanelComponent.Placeholder("shield_slot"))
        components.add(PanelComponent.Placeholder("strategy_slot"))
        
        components.add(PanelComponent.Spacer(60f))  // 20像素间隔

        components.add(SystemButtonTool.create(vw, context, RightPanel.getUnitSystem() ?: return emptyList()))

        components.add(ExitButtonTool.create(vw, context) {
            RightPanel.unlock()
        })

        ButtonBindingTool.addToComponents(components,
            ButtonBindingTool.createWeaponButtons(unit, vw, context))
            
        ButtonBindingTool.addToComponents(components,
            ButtonBindingTool.createShieldButtons(unit, vw, context))
            
        ButtonBindingTool.addToComponents(components,
            ButtonBindingTool.createShieldButtons(unit, vw, context))

        ButtonBindingTool.addToComponents(components,
            ButtonBindingTool.createStrategyButtons(unit, vw, context))
            
        ButtonBindingTool.addToComponents(components,
            ButtonBindingTool.createFactoryButtons(unit, vw, context))

        return components
    }
}