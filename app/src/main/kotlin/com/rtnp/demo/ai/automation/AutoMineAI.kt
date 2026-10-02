// app/src/main/kotlin/com/rtnp/demo/ai/automation/AutoMineAI.kt
package com.rtnp.demo.ai.automation

import com.rtnp.demo.ai.AiTemplate
import com.rtnp.demo.ai.AiTools
import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.logic.HexGridManager
import kotlin.math.sqrt

class AutoMineAI : AiTemplate {

    override val id = "auto_mine"
    override val displayName = "自动采矿"
    override val description = "放置0.8秒后，在本星区内寻找最近的金属矿物停泊采矿"
    override val enabledByDefault = true

    override fun onUpdate(host: PlacedObject, deltaTime: Float, tools: AiTools) {
        if (host.miningSpeed <= 0) return

        // 延迟0.8秒
        if (host.lifetime < 0.8f) return

        // 已经停泊就不再动
        if (host.isDocked) {
            tools.stopMoving(host)
            return
        }

        // 已经移动中就不再重复下指令
        if (host.isMoving) return

        val target = findNearestMetalMineralInTile(host, tools) ?: return

        tools.dockTo(host, target)
    }

    private fun findNearestMetalMineralInTile(host: PlacedObject, tools: AiTools): PlacedObject? {
        var tileId = HexGridManager.getTileIdForUnit(host)
        if (tileId == null) {
            HexGridManager.registerUnit(host)
            tileId = HexGridManager.getTileIdForUnit(host)
        }
        if (tileId == null) return null

        val unitsInTile = HexGridManager.getUnitsInTile(tileId)

        var best: PlacedObject? = null
        var bestDist = Float.MAX_VALUE

        for (unit in unitsInTile) {
            if (unit == host) continue
            if (unit.type != "environment") continue
            if (unit.storedItemName != "金属") continue
            if (unit.storedItemCount <= 0) continue
            if (unit.maxMiners > 0) {
                val minerCount = unitsInTile.count { it.isMining && it.miningTarget == unit }
                if (minerCount >= unit.maxMiners) continue
            }

            val dx = host.worldX - unit.worldX
            val dy = host.worldY - unit.worldY
            val dist = sqrt(dx * dx + dy * dy)

            if (dist < bestDist) {
                bestDist = dist
                best = unit
            }
        }
        return best
    }
}