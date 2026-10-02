// app/src/main/kotlin/com/rtnp/demo/ui/panel/template/EnvironmentUnitTemplate.kt
package com.rtnp.demo.ui.panel.template

import android.content.Context
import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.ui.panel.data.PanelComponent
import com.rtnp.demo.ui.panel.data.UnitNameComponent
import com.rtnp.demo.ui.panel.RightPanel
import com.rtnp.demo.ui.panel.tool.*

object EnvironmentUnitTemplate {

    fun build(unit: PlacedObject, vw: Int, context: Context): List<PanelComponent> {
        val components = mutableListOf<PanelComponent>()

        UnitNameComponent.create(unit)?.let { components.add(it) }
        LifetimeBarTool.create(unit)?.let { components.add(it) }
        components.add(HealthBarTool.create(unit))
        StorageBarTool.create(unit)?.let { components.add(it) }
        components.add(PanelComponent.Spacer(60f))  // 20像素间隔
        ButtonBindingTool.addToComponents(components,
            ButtonBindingTool.createShieldButtons(unit, vw, context))
        components.add(ExitButtonTool.create(vw, context) {
            RightPanel.unlock()
        })

        return components
    }
}