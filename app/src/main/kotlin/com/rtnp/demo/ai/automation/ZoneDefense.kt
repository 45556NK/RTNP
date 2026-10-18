package com.rtnp.demo.ai.automation

import com.rtnp.demo.ai.AiTemplate
import com.rtnp.demo.ai.AiTools
import com.rtnp.demo.ai.tools.AiMovementTools
import com.rtnp.demo.ai.tools.AiReactionTimeTool
import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.logic.DockingSystem
import com.rtnp.demo.logic.HexGridManager
import com.rtnp.demo.shield.ShieldManager

class ZoneDefense : AiTemplate {

    override val id = "zone_defense"
    override val displayName = "区域防守"
    override val description = "在本区块内搜索并攻击敌方单位，不跨区块行动"
    override val enabledByDefault = false

    companion object {
        private const val MIN_REACTION = 3f
        private const val MAX_REACTION = 7f
        private const val PATH_SAMPLE_STEPS = 100
    }

    /**
     * 每个单位的独立状态。
     */
    private data class State(
        var homeTileId: Int = -1,
        var currentTarget: PlacedObject? = null,
        var decisionMade: Boolean = false
    )

    private val states = mutableMapOf<Long, State>()

    override fun onUpdate(host: PlacedObject, deltaTime: Float, tools: AiTools) {
        val state = states.getOrPut(host.uniqueId) { State() }
        val hostTileId = HexGridManager.getTileIdForUnit(host) ?: run {
            state.currentTarget = null
            state.decisionMade = false
            AiReactionTimeTool.reset(host)
            return
        }

        // 首次记录驻地
        if (state.homeTileId == -1) {
            state.homeTileId = hostTileId
        }

        // ==================== 脱离驻地 → 归队 ====================
        if (hostTileId != state.homeTileId) {
            if (!state.decisionMade) {
                if (!AiReactionTimeTool.tick(host, deltaTime, MIN_REACTION, MAX_REACTION)) return
                state.decisionMade = true
            }
            val homeTarget = findHomeDockTarget(host, state.homeTileId)
            if (homeTarget != null) {
                AiMovementTools.dockToRandomSlot(host, homeTarget)
            }
            return
        }

        // 回到驻地 → 重置决策状态
        if (state.decisionMade) {
            AiReactionTimeTool.reset(host)
            state.decisionMade = false
        }

        // ==================== 检测区块内敌人 ====================
        val unitsInTile = HexGridManager.getUnitsInTile(hostTileId)
        val enemies = unitsInTile.filter {
            it.faction != 0 && it.faction != host.faction && it.health > 0
        }

        if (enemies.isEmpty()) {
            state.currentTarget = null
            return
        }

        // 有敌人：激活立场和武器
        activateShields(host)
        activateWeapons(host)

        // 宿主已在攻击：跳过（防止频繁更新指令）
        if (isHostAttacking(host)) return

        // ==================== 反应时间 & 目标锁定 ====================
        val curValid = isValidTarget(state.currentTarget, hostTileId, tools)
        if (!curValid) {
            state.currentTarget = null
            state.decisionMade = false
        }

        if (!state.decisionMade) {
            // 等待反应时间
            if (!AiReactionTimeTool.tick(host, deltaTime, MIN_REACTION, MAX_REACTION)) return
            // 反应结束：选择目标
            state.currentTarget = pickNearestEnemy(host, enemies, tools)
            state.decisionMade = true
        }

        val target = state.currentTarget ?: return

        // ==================== 分支决策 ====================
        executeDecision(host, hostTileId, target, tools)
    }

    // ==================== 分支决策 ====================

    private fun executeDecision(
        host: PlacedObject,
        hostTileId: Int,
        target: PlacedObject,
        tools: AiTools
    ) {
        // 导弹特殊处理：逃往本区块内离它最远的可停泊单位
        if ("missile" == target.category) {
            val farthest = findFarthestDockableInTile(host, hostTileId, target)
            if (farthest != null) {
                AiMovementTools.dockToRandomSlot(host, farthest)
            }
            return
        }

        // 分支 4：对方可停泊
        if (DockingSystem.getSlotCount(target) > 0) {
            AiMovementTools.dockToRandomSlot(host, target)
            return
        }

        // 分支 1：对方正在移动
        if (target.isMoving) {
            // 计算目标路线在宿主区块内的最远点
            val furthest = findFurthestPointInTile(
                target.worldX, target.worldY,
                target.targetX, target.targetY,
                hostTileId
            )
            if (furthest == null) return
            // 在区块内找离这个点最近的可停泊单位
            val nearest = findNearestDockableInTile(
                host, hostTileId, furthest.first, furthest.second
            )
            if (nearest != null) {
                AiMovementTools.dockToRandomSlot(host, nearest)
            }
            return
        }

        // 分支 2：对方静止 → 停泊到离它最近的可停泊单位
        val nearest = findNearestDockableInTile(
            host, hostTileId, target.worldX, target.worldY
        )
        if (nearest != null) {
            AiMovementTools.dockToRandomSlot(host, nearest)
        }
    }

    // ==================== 目标校验 / 选择 ====================

    private fun isValidTarget(
        target: PlacedObject?,
        hostTileId: Int,
        tools: AiTools
    ): Boolean {
        if (target == null) return false
        val tileId = HexGridManager.getTileIdForUnit(target) ?: return false
        if (tileId != hostTileId) return false
        return tools.getHealth(target) > 0f
    }

    private fun pickNearestEnemy(
        host: PlacedObject,
        enemies: List<PlacedObject>,
        tools: AiTools
    ): PlacedObject? {
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

    // ==================== 路线 / 区域查询 ====================

    /**
     * 沿线段 (x1,y1)→(x2,y2) 采样，返回最后一个仍在指定区块内的点。
     */
    private fun findFurthestPointInTile(
        x1: Float, y1: Float,
        x2: Float, y2: Float,
        tileId: Int
    ): Pair<Float, Float>? {
        var lastInTile: Pair<Float, Float>? = null
        var entered = false
        for (i in 0..PATH_SAMPLE_STEPS) {
            val t = i.toFloat() / PATH_SAMPLE_STEPS
            val x = x1 + (x2 - x1) * t
            val y = y1 + (y2 - y1) * t
            val tile = HexGridManager.getTileAt(x, y)
            if (tile?.id == tileId) {
                lastInTile = x to y
                entered = true
            } else if (entered) {
                break
            }
        }
        return lastInTile
    }

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

    /**
     * 归队：优先本区块同阵营基地，其次任意可停泊单位。
     */
    private fun findHomeDockTarget(host: PlacedObject, homeTileId: Int): PlacedObject? {
        val unitsInTile = HexGridManager.getUnitsInTile(homeTileId)
        // 优先同阵营基地
        for (u in unitsInTile) {
            if (u === host) continue
            if (u.health <= 0) continue
            if (u.faction != host.faction) continue
            if (u.type != "base") continue
            if (DockingSystem.getSlotCount(u) <= 0) continue
            return u
        }
        // 任意可停泊单位
        for (u in unitsInTile) {
            if (u === host) continue
            if (u.health <= 0) continue
            if (DockingSystem.getSlotCount(u) <= 0) continue
            return u
        }
        return null
    }

    // ==================== 立场 / 武器 ====================

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

    private fun isHostAttacking(host: PlacedObject): Boolean {
        for (ws in host.weaponSlots) {
            if (ws.lockedTarget != null) return true
        }
        return false
    }
}