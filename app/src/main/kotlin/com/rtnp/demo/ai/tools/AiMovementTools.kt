package com.rtnp.demo.ai.tools

import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.logic.DockingSystem
import kotlin.random.Random

/**
 * AI 移动工具集。
 * 提供给 AI 模板调用的静态工具。
 */
object AiMovementTools {

    /**
     * 让宿主移动向目标的随机空闲停泊点。
     *
     * 状态处理：
     * - 已在目标上停泊 → 返回 true
     * - 已在前往同一目标的路上（isMoving && dockTarget === target）→ 返回 true，不打断
     * - 正在移动/停止（前往其他目标或自由移动）→ 释放旧占用 + 减速，返回 true
     * - 已停泊在别处 → 释放旧槽位 + 发起新请求
     * - 空闲 → 发起新请求
     *
     * @return true 表示已成功控制或正在控制宿主；false 表示无法操作
     */
    @JvmStatic
    fun dockToRandomSlot(host: PlacedObject, target: PlacedObject): Boolean {
        if (host === target) return false
        if (!host.canMove) return false

        // 已在目标上停泊
        if (host.isDocked && host.dockedAt === target) return true

        // 已在前往同一目标的路上
        if (host.isMoving && host.dockTarget === target) return true

        // 收集目标上的空闲停泊槽位
        val sectors = DockingSystem.getDockingSectors(target)
        if (sectors.isEmpty()) return false
        val freeSlots = sectors.filter { !it.isOccupied }
        if (freeSlots.isEmpty()) return false

        // 需要打断（正在移动/停止）：释放旧占用，然后减速
        if (host.isMoving || host.isStopping) {
            releaseOccupiedSlot(host)
            host.isMoving = false
            host.isStopping = true
            host.targetX = host.worldX
            host.targetY = host.worldY
            host.dockTarget = null
            host.dockOffsetX = 0f
            host.dockOffsetY = 0f
            return true
        }

        // 已停泊在别处：释放旧槽位
        if (host.isDocked) {
            releaseOccupiedSlot(host)
        }

        // 随机选择一个空闲槽位并请求停泊
        val slot = freeSlots[Random.nextInt(freeSlots.size)]
        return DockingSystem.requestDock(host, target, slot.index)
    }

    /**
     * 释放宿主当前占用的所有停泊记录。
     * 包括：
     * - 已停泊的目标（dockedAt）
     * - 正在前往的目标（dockTarget，可能与之不同）
     * 清理对应的 dockedList / dockingList 中的预留记录。
     */
    private fun releaseOccupiedSlot(host: PlacedObject) {
        val dockedAt = host.dockedAt
        if (dockedAt != null) {
            dockedAt.dockedList.removeAll { it.unitId == host.uniqueId }
            dockedAt.dockingList.removeAll { it.unitId == host.uniqueId }
        }
        val dockTarget = host.dockTarget
        if (dockTarget != null && dockTarget !== dockedAt) {
            dockTarget.dockedList.removeAll { it.unitId == host.uniqueId }
            dockTarget.dockingList.removeAll { it.unitId == host.uniqueId }
        }
        host.dockSlotIndex = -1
        host.dockedAt = null
        host.isDocked = false
        host.isUndocking = false
        host.dockTarget = null
    }
}