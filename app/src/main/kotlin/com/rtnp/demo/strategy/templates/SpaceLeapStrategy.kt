package com.rtnp.demo.strategy.templates

import android.graphics.Color
import com.rtnp.demo.camera.CameraAnimator
import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.gpu.effect.GpuEffectSystem
import com.rtnp.demo.gpu.render.tools.TeleportPathSystem
import com.rtnp.demo.logic.DockingSystem
import com.rtnp.demo.render.RenderControl
import com.rtnp.demo.strategy.CooldownMode
import com.rtnp.demo.strategy.StrategyTemplate
import com.rtnp.demo.strategy.StrategyToolLayer
import com.rtnp.demo.strategy.StrategyTools
import com.rtnp.demo.ui.ToastTool
import com.rtnp.demo.ui.WarningMessage
import com.rtnp.demo.ui.panel.data.PanelComponent
import kotlin.random.Random

class SpaceLeapStrategy : StrategyTemplate() {

    override val id = "space_leap"
    override val displayName = "空间跨越"
    override val description = "可以进行短距离空间跨越\n跨越距离大约2500"
    override val iconPath: String = "images/icons/space_leap_icon.png"
    override val cooldown = 10f
    override val cooldownMode = CooldownMode.MANUAL
    override val blacklistTypes: List<String> = listOf("environment", "missile")

    companion object {
        private const val MAX_RANGE = 2500f
        private const val CHARGE_DURATION_MS = 4000L
        private const val EXPLOSION_REF_RATIO = 5f
        private val PROGRESS_COLOR = Color.rgb(120, 100, 255)
    }

    private data class State(
        val targetUnit: PlacedObject,
        val chargeStartTime: Long,
        val pathId: Long
    )

    private val states = mutableMapOf<Long, State>()

    // ==================== 条件检查 ====================

    override fun canUse(host: PlacedObject, tools: StrategyTools, slotIndex: Int): Boolean {
        if (host.isMoving || host.isStopping || host.isUndocking || host.currentSpeed > 0f) {
            WarningMessage.show("不具备跨越条件")
            return false
        }
        if (states.containsKey(host.uniqueId)) {
            WarningMessage.show("正在跨越中")
            return false
        }
        return true
    }

    // ==================== 每帧更新 ====================

    override fun onUpdate(host: PlacedObject, deltaTime: Float, tools: StrategyTools) {
        val state = states[host.uniqueId] ?: return

        // 宿主开始移动 → 打断
        if (host.currentSpeed > 0f || host.isMoving || host.isStopping || host.isUndocking) {
            cancelCharge(host, tools)
            WarningMessage.show("跨越被打断")
            return
        }

        // 目标消失 / 死亡
        val target = state.targetUnit
        if (!tools.getAllUnits().contains(target) || tools.getHealth(target) <= 0f) {
            cancelCharge(host, tools)
            WarningMessage.show("目标已消失")
            return
        }

        // 目标停泊点被占用 → 打断
        val slots = DockingSystem.getSlotCount(target)
        val occupied = target.dockedList.size + target.dockingList.size
        if (slots <= 0 || occupied >= slots) {
            cancelCharge(host, tools)
            WarningMessage.show("目标停泊点已满")
            return
        }

        // ==================== 更新进度条 ====================
        val elapsed = System.currentTimeMillis() - state.chargeStartTime
        val ratio = (elapsed.toFloat() / CHARGE_DURATION_MS).coerceIn(0f, 1f)
        val totalSec = (CHARGE_DURATION_MS / 1000L).toInt()          // 4
        val remainSec = (((CHARGE_DURATION_MS - elapsed).coerceAtLeast(0L) + 999L) / 1000L).toInt()  // 向上取整，1~0
        val label = "${remainSec}/${totalSec}"
        val detailLayout = listOf(
            PanelComponent.Text(content = displayName, textSize = 32f),
            PanelComponent.Spacer(10f),
            PanelComponent.Text(content = "剩余 ${remainSec}秒", textSize = 28f),
            PanelComponent.Text(content = "总计 ${totalSec}秒", textSize = 28f)
        )
        tools.progressBar.setProgressBarRaw(
            host = host,
            strategyId = id,
            progress = ratio,
            color = PROGRESS_COLOR,
            label = label,
            detailLayout = detailLayout
        )

        // 充能完成 → 传送
        if (elapsed >= CHARGE_DURATION_MS) {
            executeTeleport(host, target)
            cancelCharge(host, tools)
        }
    }

    // ==================== 点击触发 ====================

    override fun onClicked(host: PlacedObject, tools: StrategyTools, slotIndex: Int) {
        CameraAnimator.animateZoomAndMove(host.worldX, host.worldY, 0.5f, 800L)

        StrategyToolLayer.activate(host, MAX_RANGE)

        RenderControl.touchDisabledUnitInfoPanel = true
        RenderControl.touchDisabledBottomBar = true
        RenderControl.touchDisabledMultiSelectButton = true
        RenderControl.showUnitInfoPanel = false

        ToastTool.showGlobal("请选择跨越目标") {
            cleanupUi()
        }

        StrategyToolLayer.onTapInside = { wx, wy ->
            val targetUnit = findTargetBase(wx, wy, host, tools)
            if (targetUnit != null) {
                val pathId = TeleportPathSystem.addTeleportPath(
                    startX = host.worldX, startY = host.worldY,
                    endX = targetUnit.worldX, endY = targetUnit.worldY,
                    color = floatArrayOf(0.5f, 0.8f, 1f),
                    pulseDuration = 0.062f,
                    startMarkerRadius = host.referenceVolume?.radius ?: 20f
                )
                states[host.uniqueId] = State(
                    targetUnit = targetUnit,
                    chargeStartTime = System.currentTimeMillis(),
                    pathId = pathId
                )
                tools.startCooldown(host, slotIndex, cooldown)
                cleanupUi()
            } else {
                WarningMessage.show("请选择有效目标")
            }
        }

        StrategyToolLayer.onTapOutside = {
            WarningMessage.show("请选择有效目标")
        }
    }

    // ==================== 目标查找 ====================

    private fun findTargetBase(
        wx: Float, wy: Float,
        host: PlacedObject,
        tools: StrategyTools
    ): PlacedObject? {
        var best: PlacedObject? = null
        var bestDist = Float.MAX_VALUE

        for (unit in tools.getAllUnits()) {
            if (unit == host) continue
            if (unit.type != "base") continue
            if (unit.category == "临时建筑") continue
            // 阵营不限：我方 / 敌方 / 中立均可
            if (tools.getHealth(unit) <= 0f) continue
            // 必须有空闲停泊点
            val slots = DockingSystem.getSlotCount(unit)
            if (slots <= 0) continue
            val occupied = unit.dockedList.size + unit.dockingList.size
            if (occupied >= slots) continue

            val hit: Boolean = if (unit.shape == "circle") {
                val r = unit.size / 2f + 20f
                val dx = wx - unit.worldX
                val dy = wy - unit.worldY
                dx * dx + dy * dy <= r * r
            } else {
                val hw = unit.width / 2f + 20f
                val hh = unit.height / 2f + 20f
                Math.abs(wx - unit.worldX) <= hw && Math.abs(wy - unit.worldY) <= hh
            }
            if (!hit) continue

            val dx = wx - unit.worldX
            val dy = wy - unit.worldY
            val dist = dx * dx + dy * dy
            if (dist < bestDist) {
                bestDist = dist
                best = unit
            }
        }
        return best
    }

    // ==================== 执行传送 ====================

    private fun executeTeleport(host: PlacedObject, target: PlacedObject) {
        // 记录宿主参考半径（爆炸大小用）
        val refRadius = host.referenceVolume?.radius?.takeIf { it > 0f }
            ?: (maxOf(host.width.toFloat(), host.height.toFloat()) / 2f)

        if (host.isDocked) {
            DockingSystem.releaseSlot(host)
            host.dockedAt = null
            host.isDocked = false
        }
        host.isMoving = false
        host.isStopping = false
        host.isUndocking = false
        host.currentSpeed = 0f

        val sectors = DockingSystem.getDockingSectors(target)
        val freeSectors = sectors.filter { !it.isOccupied }
        if (freeSectors.isEmpty()) {
            WarningMessage.show("目标无可用停泊点")
            return
        }
        val chosen = freeSectors[Random.nextInt(freeSectors.size)]

        val success = DockingSystem.requestDock(host, target, chosen.index)
        if (!success) {
            WarningMessage.show("目标无可用停泊点")
            return
        }

        val sector = DockingSystem.getDockingSectors(target)
            .firstOrNull { it.index == host.dockSlotIndex }
        if (sector != null) {
            host.worldX = sector.centerX
            host.worldY = sector.centerY
            host.targetX = sector.centerX
            host.targetY = sector.centerY
        } else {
            host.worldX = target.worldX
            host.worldY = target.worldY
            host.targetX = target.worldX
            host.targetY = target.worldY
        }

        host.isDocked = true
        host.dockedAt = target
        host.dockTarget = null
        host.actionDock = false
        host.isMoving = false
        host.isStopping = false
        host.isUndocking = false
        host.currentSpeed = 0f
        target.dockingList.removeAll { it.unitId == host.uniqueId }
        target.dockedList.add(PlacedObject.DockEntry(host.uniqueId, host.dockSlotIndex))

        // 传送后位置释放爆炸
        spawnLeapExplosion(host.worldX, host.worldY, refRadius)
    }

    // ==================== 爆炸特效 ====================

    /**
     * 在指定位置释放爆炸特效。
     * ExplosionEffect 的 getWorldBounds 使用 `host.explosionRange × 2` 作为世界包围半径，
     * 视觉半径约等于 explosionRange。
     */
    private fun spawnLeapExplosion(x: Float, y: Float, refRadius: Float) {
        val visualRadius = (refRadius * EXPLOSION_REF_RATIO).toInt().coerceAtLeast(1)
        val tempHost = PlacedObject()
        tempHost.explosionRange = visualRadius
        GpuEffectSystem.spawn("explosion", x, y, tempHost)
    }

    // ==================== 清理 ====================

    private fun cancelCharge(host: PlacedObject, tools: StrategyTools) {
        val state = states.remove(host.uniqueId) ?: return
        if (state.pathId >= 0) {
            TeleportPathSystem.removeTeleportPath(state.pathId)
        }
        tools.progressBar.clearButtonState(host, id)
    }

    private fun cleanupUi() {
        StrategyToolLayer.deactivate()
        RenderControl.touchDisabledUnitInfoPanel = false
        RenderControl.touchDisabledBottomBar = false
        RenderControl.touchDisabledMultiSelectButton = false
        RenderControl.showUnitInfoPanel = true
        ToastTool.hideGlobal()
    }
}