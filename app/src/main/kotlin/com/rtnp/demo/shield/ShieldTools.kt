package com.rtnp.demo.shield

import android.content.Context
import android.graphics.Color
import com.rtnp.demo.UnitSystem
import com.rtnp.demo.TranslationTable
import com.rtnp.demo.core.InventoryItem
import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.core.TemplateRegistry
import com.rtnp.demo.ui.panel.data.PanelComponent
import com.rtnp.demo.ui.panel.tool.TemplateProgressBarTool

/**
 * 立场槽工具集，提供给立场模板使用。
 */
class ShieldTools(private val unitSystem: UnitSystem) {

    companion object {
        const val SHIELD_PLACEHOLDER_ID = "shield_slot"
    }

    // ==================== 查询 ====================

    fun getAllUnits(): List<PlacedObject> = unitSystem.placedObjects.toList()

    fun getUnitsByFaction(faction: Int): List<PlacedObject> =
        unitSystem.placedObjects.filter { it.faction == faction }

    fun getHealth(unit: PlacedObject): Float = unitSystem.healthMap[unit] ?: 0f

    fun setHealth(unit: PlacedObject, health: Float) {
        unitSystem.healthMap[unit] = health
        unit.health = health.toInt()
    }

    // ==================== 单位生成 / 移除 ====================

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

    // ==================== 屏幕 / Context ====================

    fun getContext(): Context? = unitSystem.context
    fun getScreenWidth(): Int = unitSystem.screenWidth
    fun getScreenHeight(): Int = unitSystem.screenHeight

    // ==================== 激活 / 冷却 工具 ====================

    /**
     * 声明激活参数。
     * @param duration 激活时效（秒）。> 0 有时效；-1 永久
     * @param progressValue 进度条初始值。0 表示不显示进度条
     * @param progressColor 进度条颜色
     */
    fun activate(
        host: PlacedObject,
        slotIndex: Int,
        duration: Float,
        progressValue: Float,
        progressColor: Int
    ) {
        val data = ShieldManager.getSlotData(host, slotIndex) ?: return
        data.activationDuration = duration
        data.remainingTime = if (duration > 0f) duration else -1f
        data.activationBarColor = if (progressValue > 0f) progressColor else 0
    }

    /**
     * 声明冷却参数。
     * @param duration 冷却时长（秒）
     * @param progressValue 进度条初始值。0 表示不显示进度条
     * @param progressColor 进度条颜色
     */
    fun cooldown(
        host: PlacedObject,
        slotIndex: Int,
        duration: Float,
        progressValue: Float,
        progressColor: Int
    ) {
        val data = ShieldManager.getSlotData(host, slotIndex) ?: return
        data.cooldownDuration = duration
        data.cooldownRemaining = duration
        data.cooldownBarColor = if (progressValue > 0f) progressColor else 0
    }

    /**
     * 模板请求立即失效。
     * 系统会在下一帧调用 onExpire 并进入冷却流程。
     */
    fun requestExpire(host: PlacedObject, slotIndex: Int) {
        val data = ShieldManager.getSlotData(host, slotIndex) ?: return
        data.requestedExpire = true
    }

    // ==================== 进度条接口 ====================

    /**
     * 语义版：显示立场维持值进度条。
     */
    @JvmOverloads
    fun setProgressBar(
        host: PlacedObject,
        slotIndex: Int,
        displayName: String,
        currentHp: Float,
        maxHp: Float,
        color: Int,
        extraDescription: String? = null
    ) {
        val ratio = if (maxHp > 0f) (currentHp / maxHp).coerceIn(0f, 1f) else 0f
        val hpLabel = TranslationTable.get("shield_hp")
        val label = "$hpLabel: ${currentHp.toInt()}/${maxHp.toInt()}"
        val detailLayout = buildShieldDetailLayout(displayName, currentHp, maxHp, extraDescription)
        val slotKey = "shield_$slotIndex"
        val bgColor = Color.argb(100, 200, 200, 200)
        TemplateProgressBarTool.show(
            host = host,
            placeholderId = SHIELD_PLACEHOLDER_ID,
            slotKey = slotKey,
            bgColor = bgColor,
            progress = ratio,
            fillColor = color,
            label = label,
            detailLayout = detailLayout
        )
    }

    /**
     * 底层版：显示进度条（调用者自行提供 label 和详情栏）。
     */
    @JvmOverloads
    fun setProgressBarRaw(
        host: PlacedObject,
        slotIndex: Int,
        progress: Float,
        color: Int,
        label: String = "",
        detailLayout: List<PanelComponent> = emptyList()
    ) {
        val slotKey = "shield_$slotIndex"
        val bgColor = Color.argb(100, 200, 200, 200)
        TemplateProgressBarTool.show(
            host = host,
            placeholderId = SHIELD_PLACEHOLDER_ID,
            slotKey = slotKey,
            bgColor = bgColor,
            progress = progress,
            fillColor = color,
            label = label,
            detailLayout = detailLayout
        )
    }

    fun buildShieldDetailLayout(
        displayName: String,
        currentHp: Float,
        maxHp: Float,
        extraDescription: String? = null
    ): List<PanelComponent> {
        val list = mutableListOf<PanelComponent>()
        list.add(PanelComponent.Text(content = displayName, textSize = 32f))
        list.add(PanelComponent.Spacer(10f))
        val hpLabel = TranslationTable.get("shield_hp")
        list.add(PanelComponent.Text(
            content = "$hpLabel: ${currentHp.toInt()}/${maxHp.toInt()}",
            textSize = 28f
        ))
        if (!extraDescription.isNullOrEmpty()) {
            list.add(PanelComponent.Text(content = extraDescription, textSize = 24f))
        }
        return list
    }
    /**
    * 移除立场进度条。
    */
    fun clearButtonState(host: PlacedObject, slotIndex: Int) {
        val slotKey = "shield_$slotIndex"
        TemplateProgressBarTool.hide(host, slotKey)
    }
}