package com.rtnp.demo.ui.panel.tool

import android.content.Context
import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.ui.panel.data.InteractiveButtonComponent
import com.rtnp.demo.ui.panel.data.PanelComponent

object ButtonBindingTool {

    fun createWeaponButtons(unit: PlacedObject, vw: Int, context: Context): List<InteractiveButtonComponent> {
        return unit.weaponSlots.mapIndexed { index, slot ->
            WeaponButtonTool.create(slot, index, vw, context)
        }
    }

    fun createStrategyButtons(unit: PlacedObject, vw: Int, context: Context): List<InteractiveButtonComponent> {
        return (0 until unit.strategySlots).mapIndexed { index, _ ->
            val strategyId = unit.strategyIds.getOrElse(index) { "" }
            StrategyButtonTool.create(strategyId, index, vw, context, unit)
        }
    }

    fun createFactoryButtons(unit: PlacedObject, vw: Int, context: Context): List<InteractiveButtonComponent> {
        if (unit.factorySlots <= 0) return emptyList()
        return (0 until unit.factorySlots).map { index ->
            FactoryButtonTool.create(index, vw, context, unit)
        }
    }

    /**
     * ★ 新增：创建立场按钮。
     */
    fun createShieldButtons(unit: PlacedObject, vw: Int, context: Context): List<InteractiveButtonComponent> {
        if (unit.shieldSlots <= 0) return emptyList()
        return (0 until unit.shieldSlots).map { index ->
            ShieldButtonTool.create(index, vw, context, unit)
        }
    }

    fun addToComponents(components: MutableList<PanelComponent>, buttons: List<InteractiveButtonComponent>) {
        components.addAll(buttons)
    }
}