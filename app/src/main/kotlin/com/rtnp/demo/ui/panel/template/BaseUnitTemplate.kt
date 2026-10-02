// app/src/main/kotlin/com/rtnp/demo/ui/panel/template/BaseUnitTemplate.kt
package com.rtnp.demo.ui.panel.template

import android.content.Context
import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.ui.panel.RightPanel
import com.rtnp.demo.ui.panel.data.PanelComponent
import com.rtnp.demo.ui.panel.data.UnitNameComponent
import com.rtnp.demo.ui.panel.tool.*

object BaseUnitTemplate {

    fun build(unit: PlacedObject, vw: Int, context: Context): List<PanelComponent> {
        val components = mutableListOf<PanelComponent>()

        UnitNameComponent.create(unit)?.let { components.add(it) }

        LifetimeBarTool.create(unit)?.let { components.add(it) }

        components.add(HealthBarTool.create(unit))

        StorageBarTool.create(unit)?.let { components.add(it) }

        // 间隔 60 像素
        components.add(PanelComponent.Spacer(60f))

        val unitSystem = RightPanel.getUnitSystem() ?: return emptyList()
        components.add(SystemButtonTool.create(vw, context, unitSystem))

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