package com.rtnp.demo.factory.templates

import android.graphics.Color
import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.factory.FactorySlotData
import com.rtnp.demo.factory.FactoryTemplate
import com.rtnp.demo.factory.FactoryTools
import com.rtnp.demo.strategy.CooldownMode
import com.rtnp.demo.ui.panel.tool.CooldownRenderTool

/**
 * 修复工厂
 * 拼装系统使用预制模块快速拼装战列舰使其更快的投入战场
 *
 * 每 1 秒尝试修复一次停泊单位：
 * - 消耗 1 金属
 * - 修复 repairAmount 点血量（不超过上限）
 *
 * 冷却仅在存在需要修复的单位时推进；无目标时清空冷却条。
 */
class RepairFactoryTemplate : FactoryTemplate() {

    override val id = "repair_factory"
    override val displayName = "修复工厂"
    override val description = "拼装系统使用预制模块快速拼装战列舰使其更快的投入战场"

    override val cooldown = 1f
    override val cooldownMode = CooldownMode.MANUAL

    override val defaultParams: Map<String, Any> = mapOf(
        "costAmount" to 1,
        "repairAmount" to 100
    )

    private val repairInterval = 1.0f

    override fun onUpdate(
        host: PlacedObject,
        slotIndex: Int,
        deltaTime: Float,
        tools: FactoryTools,
        slotData: FactorySlotData
    ) {
        val costAmount = slotData.getInt("costAmount") ?: 1
        val repairAmount = slotData.getInt("repairAmount") ?: 100

        // 1. 获取停泊单位
        val dockedUnits = host.dockedList.mapNotNull { entry ->
            tools.getAllUnits().firstOrNull { it.uniqueId == entry.unitId }
        }

        // 2. 找到第一个受伤单位
        val target = dockedUnits.firstOrNull { unit ->
            val hp = tools.getHealth(unit)
            hp < unit.originalHealth
        }

        // 3. 没有目标：清空冷却条、重置计时，直接返回
        if (target == null) {
            slotData.put("_timer", 0f)
            tools.clearButtonState(host, slotIndex)
            return
        }

        // 4. 有目标：推进冷却
        var timer = slotData.getFloat("_timer") ?: 0f
        timer += deltaTime

        val progressRatio = (timer / repairInterval).coerceIn(0f, 1f)
        tools.setCooldownBar(
            host, slotIndex, progressRatio,
            Color.argb(89, 255, 130, 130),
            CooldownRenderTool.Direction.BOTTOM_UP
        )

        if (timer < repairInterval) {
            slotData.put("_timer", timer)
            return
        }

        // 5. 冷却完成，尝试修复
        timer -= repairInterval
        slotData.put("_timer", timer)

        val metalCount = host.collectedItems?.get("金属") ?: 0
        if (metalCount < costAmount) return

        host.collectedItems?.put("金属", metalCount - costAmount)

        val currentHp = tools.getHealth(target)
        val newHp = (currentHp + repairAmount).coerceAtMost(target.originalHealth.toFloat())
        tools.setHealth(target, newHp)
    }

    override fun onClicked(
        host: PlacedObject,
        slotIndex: Int,
        tools: FactoryTools,
        slotData: FactorySlotData
    ) {
        // 持续运作型，无需点击触发
    }
}