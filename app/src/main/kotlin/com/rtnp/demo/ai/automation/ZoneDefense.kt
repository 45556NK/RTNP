package com.rtnp.demo.ai.automation

import com.rtnp.demo.ai.AiTemplate
import com.rtnp.demo.ai.AiTools
import com.rtnp.demo.ai.tools.AiMovementTools
import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.logic.DockingSystem
import com.rtnp.demo.logic.HexGridManager
import com.rtnp.demo.shield.ShieldManager

class ZoneDefense : AiTemplate {

    override val id = "zone_defense"
    override val displayName = "区域防守"
    override val description = "在本区块内搜索并攻击敌方单位，不跨区块行动"
    override val enabledByDefault = false

    /** 死咬目标，直到离开区块或死亡 */
    private var currentTarget: PlacedObject? = null

    override fun onUpdate(host: PlacedObject, deltaTime: Float, tools: AiTools) {
        val hostTileId = HexGridManager.getTileIdForUnit(host) ?: run {
            currentTarget = null
            return
        }

        // 获取本区块内的敌方单位（非中立、非我方）
        val unitsInTile = HexGridManager.getUnitsInTile(hostTileId)
        val enemies = unitsInTile.filter {
            it.faction != 0 && it.faction != host.faction && it.health > 0
        }

        // 激活立场和武器
        activateShields(host)
        activateWeapons(host)

        if (enemies.isEmpty()) {
            currentTarget = null
            return
        }

        // 宿主已经在攻击：跳过（防止频繁更新指令）
        if (isHostAttacking(host)) return

        // 校验当前目标，或寻找新目标
        val target = validateOrFindTarget(host, hostTileId, enemies, tools)
        if (target == null) {
            currentTarget = null
            return
        }
        currentTarget = target

        // ★ 导弹特殊处理：逃往本区块内离它最远的可停泊单位
        if ("missile" == target.category) {
            val farthest = findFarthestDockableInTile(host, hostTileId, target)
            if (farthest != null) {
                AiMovementTools.dockToRandomSlot(host, farthest)
            }
            return
        }

        // 分支 4：对方本身可停泊
        if (DockingSystem.getSlotCount(target) > 0) {
            AiMovementTools.dockToRandomSlot(host, target)
            return
        }

        // 分支 1：对方正在移动，且目的地是有效的可停泊单位
        val dockTarget = target.dockTarget
        if (target.isMoving && dockTarget != null && tools.getAllUnits().contains(dockTarget)
            && DockingSystem.getSlotCount(dockTarget) > 0) {
            AiMovementTools.dockToRandomSlot(host, dockTarget)
            return
        }

        // 分支 2：对方静止 → 停泊到离它最近的可停泊单位
        if (!target.isMoving) {
            val nearest = findNearestDockableInTile(host, hostTileId, target.worldX, target.worldY)
            if (nearest != null) {
                AiMovementTools.dockToRandomSlot(host, nearest)
            }
            return
        }

        // 分支 3：对方正在移动，目的地没有可停泊单位 → 去目的地最近的可停泊单位
        val nearestToDest = findNearestDockableInTile(host, hostTileId, target.targetX, target.targetY)
        if (nearestToDest != null) {
            AiMovementTools.dockToRandomSlot(host, nearestToDest)
        }
    }

    // ==================== 立场 / 武器 ====================

    /**
     * 激活所有需要激活的立场（跳过永久激活的）。
     */
    private fun activateShields(host: PlacedObject) {
        for (i in host.shieldSlotConfigs.indices) {
            val config = host.shieldSlotConfigs[i]
            if (config.shieldId.isEmpty()) continue
            val template = ShieldManager.getTemplate(config.shieldId) ?: continue
            if (!template.requiresActivation) continue
            if (config.params.isActive) continue
            if (config.params.cooldownRemaining > 0f) continue
            ShieldManager.activateShield(host, i)
        }
    }

    /**
     * 打开所有武器槽。
     */
    private fun activateWeapons(host: PlacedObject) {
        var changed = false
        for (ws in host.weaponSlots) {
            if (!ws.active) {
                ws.active = true
                changed = true
            }
        }
        if (changed) host.syncCurrentWeapon()
    }

    /**
     * 宿主是否已有武器槽在攻击。
     */
    private fun isHostAttacking(host: PlacedObject): Boolean {
        for (ws in host.weaponSlots) {
            if (ws.lockedTarget != null) return true
        }
        return false
    }

    // ==================== 目标锁定 ====================

    /**
     * 校验当前目标是否仍有效；无效则寻找最近的敌人。
     */
    private fun validateOrFindTarget(
        host: PlacedObject,
        hostTileId: Int,
        enemies: List<PlacedObject>,
        tools: AiTools
    ): PlacedObject? {
        // 校验当前目标
        val cur = currentTarget
        if (cur != null) {
            val curTile = HexGridManager.getTileIdForUnit(cur)
            if (curTile != null && curTile == hostTileId && tools.getHealth(cur) > 0f) {
                return cur
            }
            currentTarget = null
        }

        // 寻找最近的新目标
        var best: PlacedObject? = null
        var bestDist = Float.MAX_VALUE
        for (e in enemies) {
            val d = tools.distance(host, e)
            if (d < bestDist) {
                bestDist = d
                best = e
            }
        }
        return best
    }

    // ==================== 可停泊目标查找 ====================

    /**
     * 本区块内离 (targetX, targetY) 最近的可停泊单位。
     */
    private fun findNearestDockableInTile(
        host: PlacedObject,
        tileId: Int,
        targetX: Float,
        targetY: Float
    ): PlacedObject? {
        val unitsInTile = HexGridManager.getUnitsInTile(tileId)
        var best: PlacedObject? = null
        var bestDist = Float.MAX_VALUE
        for (u in unitsInTile) {
            if (u === host) continue
            if (u.health <= 0) continue
            if (DockingSystem.getSlotCount(u) <= 0) continue
            val dx = u.worldX - targetX
            val dy = u.worldY - targetY
            val d = dx * dx + dy * dy
            if (d < bestDist) {
                bestDist = d
                best = u
            }
        }
        return best
    }

    /**
     * 本区块内离 from 最远的可停泊单位。
     */
    private fun findFarthestDockableInTile(
        host: PlacedObject,
        tileId: Int,
        from: PlacedObject
    ): PlacedObject? {
        val unitsInTile = HexGridManager.getUnitsInTile(tileId)
        var best: PlacedObject? = null
        var bestDist = -1f
        for (u in unitsInTile) {
            if (u === host) continue
            if (u.health <= 0) continue
            if (DockingSystem.getSlotCount(u) <= 0) continue
            val dx = u.worldX - from.worldX
            val dy = u.worldY - from.worldY
            val d = dx * dx + dy * dy
            if (d > bestDist) {
                bestDist = d
                best = u
            }
        }
        return best
    }
}