package com.rtnp.demo.ui.systempanel.tools

import android.content.Context
import android.graphics.Bitmap
import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.image.ImageManager
import com.rtnp.demo.movement.weapon.WeaponManager
import com.rtnp.demo.shield.ShieldManager
import com.rtnp.demo.strategy.StrategyManager
import com.rtnp.demo.ui.systempanel.SystemPanelConstants

/**
 * 单个系统管理槽位的条目数据。
 */
data class SystemPanelSlotEntry(
    val type: String,
    val index: Int,
    val label: String,
    val bitmap: Bitmap?
)

object SystemPanelEntriesBuilder {

    fun build(unit: PlacedObject, context: Context): List<SystemPanelSlotEntry> {
        val entries = mutableListOf<SystemPanelSlotEntry>()

        // 武器槽
        unit.weaponSlots.forEachIndexed { index, slot ->
            val template = slot.templateId?.let { WeaponManager.getTemplate(it) }
            val label = if (slot.active) {
                template?.displayName ?: slot.type
            } else {
                SystemPanelConstants.getText("empty_weapon_slot")
            }
            val bitmap = template?.iconPath?.let { loadIcon(it, context) }
            entries.add(SystemPanelSlotEntry("weapon", index, label, bitmap))
        }

        // 策略槽
        for (i in 0 until unit.strategySlots) {
            val sid = unit.strategyIds.getOrNull(i) ?: ""
            val template = if (sid.isNotEmpty()) StrategyManager.getTemplate(sid) else null
            val label = if (sid.isEmpty()) {
                SystemPanelConstants.getText("empty_strategy_slot")
            } else {
                template?.displayName ?: sid
            }
            val bitmap = template?.iconPath?.let { loadIcon(it, context) }
            entries.add(SystemPanelSlotEntry("strategy", i, label, bitmap))
        }

        // 立场槽
        for (i in 0 until unit.shieldSlots) {
            val config = unit.shieldSlotConfigs.getOrNull(i)
            val shieldId = config?.shieldId ?: ""
            val template = if (shieldId.isNotEmpty()) ShieldManager.getTemplate(shieldId) else null
            val label = if (shieldId.isEmpty()) {
                SystemPanelConstants.getText("empty_shield_slot")
            } else {
                template?.displayName ?: shieldId
            }
            val bitmap = template?.iconPath?.let { loadIcon(it, context) }
            entries.add(SystemPanelSlotEntry("shield", i, label, bitmap))
        }

        return entries
    }

    private fun loadIcon(iconPath: String, context: Context): Bitmap? {
        return ImageManager.getInstance(context).getBitmap(iconPath)
    }
}