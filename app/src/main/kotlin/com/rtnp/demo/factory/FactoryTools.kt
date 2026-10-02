package com.rtnp.demo.factory

import android.content.Context
import com.rtnp.demo.UnitSystem
import com.rtnp.demo.core.InventoryItem
import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.core.TemplateRegistry
import com.rtnp.demo.ui.panel.tool.CooldownRenderTool
import com.rtnp.demo.ui.panel.tool.EnergyRingRenderTool
import com.rtnp.demo.ui.panel.tool.FactoryButtonTool

class FactoryTools(private val unitSystem: UnitSystem) {

    fun getAllUnits(): List<PlacedObject> = unitSystem.placedObjects.toList()

    fun getUnitsByFaction(faction: Int): List<PlacedObject> =
        unitSystem.placedObjects.filter { it.faction == faction }

    fun getHealth(unit: PlacedObject): Float = unitSystem.healthMap[unit] ?: 0f

    fun setHealth(unit: PlacedObject, health: Float) {
        unitSystem.healthMap[unit] = health
        unit.health = health.toInt()
    }

    fun spawnUnit(templateName: String, worldX: Float, worldY: Float): PlacedObject? {
        val template = TemplateRegistry.templates.firstOrNull { it.name == templateName } ?: return null
        unitSystem.addObjectFromItem(template, worldX, worldY)
        return unitSystem.placedObjects.lastOrNull()
    }

    fun spawnUnit(item: InventoryItem, worldX: Float, worldY: Float): PlacedObject? {
        unitSystem.addObjectFromItem(item, worldX, worldY)
        return unitSystem.placedObjects.lastOrNull()
    }

    fun removeUnit(unit: PlacedObject) {
        unitSystem.placedObjects.remove(unit)
        unitSystem.healthMap.remove(unit)
        unitSystem.unitsWithLifetime.remove(unit)
        if (unit == unitSystem.selectedUnit) {
            unitSystem.deselectUnit()
        }
    }

    // ==================== 屏幕 / Context 访问 ====================

    fun getContext(): Context? = unitSystem.context

    fun getScreenWidth(): Int = unitSystem.screenWidth

    fun getScreenHeight(): Int = unitSystem.screenHeight

    // ==================== 工厂按钮进度接口 ====================

    fun setCooldownBar(
        host: PlacedObject, slotIndex: Int, progress: Float, color: Int,
        direction: CooldownRenderTool.Direction = CooldownRenderTool.Direction.BOTTOM_UP
    ) {
        FactoryButtonTool.setBar(
            host, slotIndex, FactoryButtonTool.BarType.COOLDOWN,
            progress, color, cooldownDirection = direction
        )
    }

    fun setProgressBar(
        host: PlacedObject, slotIndex: Int, progress: Float, color: Int,
        direction: EnergyRingRenderTool.Direction = EnergyRingRenderTool.Direction.CLOCKWISE
    ) {
        FactoryButtonTool.setBar(
            host, slotIndex, FactoryButtonTool.BarType.PROGRESS,
            progress, color, progressDirection = direction
        )
    }

    fun clearButtonState(host: PlacedObject, slotIndex: Int) {
        FactoryButtonTool.clear(host, slotIndex)
    }
}