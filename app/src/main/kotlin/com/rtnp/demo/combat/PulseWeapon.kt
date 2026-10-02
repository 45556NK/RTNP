// app/src/main/kotlin/com/rtnp/demo/combat/PulseWeapon.kt
package com.rtnp.demo.combat

import com.rtnp.demo.UnitSystem
import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.ui.render.button.ProgressBarTool
import com.rtnp.demo.trait.TargetFilter

class PulseWeapon(
    val slot: PlacedObject.WeaponSlot,
    val attacker: PlacedObject
) {
    val energyBar: ProgressBarTool
    val cooldownBar: ProgressBarTool
    var maxTargets: Int = 1
    val lockedTargets = mutableListOf<PlacedObject>()
    var noTargetStartTime = 0L
    private var lastSearchTime = 0L

    /** 是否禁止能量恢复 */
    var disableEnergyRegen: Boolean = false

    /** 是否禁止冷却恢复 */
    var disableCooldownRecovery: Boolean = false

    init {
        energyBar = ProgressBarTool(totalDuration = 1f).apply {
            setProgress(1f); autoAdvance = false
        }
        cooldownBar = ProgressBarTool(totalDuration = slot.cooldown.coerceAtLeast(0.01f)).apply {
            autoAdvance = false
        }
    }

    fun update(fixedDt: Float, host: UnitSystem, range: Float) {
        val now = System.currentTimeMillis()
        val dt = fixedDt.coerceIn(0f, 0.1f)
        val cd = slot.cooldown.coerceAtLeast(0.01f)
        val ed = slot.energyDuration.coerceAtLeast(0.01f)
        val rs = slot.energyRegenSpeed
        val rd = slot.energyRegenDelay

        // 1. 定期搜索目标
        if (now - lastSearchTime > 500) {
            lastSearchTime = now
            lockedTargets.clear()
            val r2 = range * range
            for (t in host.placedObjects) {
                if (t.faction == attacker.faction || t.faction == 0) continue
                val hp = host.healthMap[t] ?: continue
                if (hp <= 0) continue
                if (TargetFilter.shouldFilter(slot, t)) continue
                val dx = t.worldX - attacker.worldX
                val dy = t.worldY - attacker.worldY
                if (dx * dx + dy * dy <= r2) {
                    lockedTargets.add(t)
                    if (lockedTargets.size >= maxTargets) break
                }
            }
        }

        // 2. 每帧强制清除无效目标
        lockedTargets.removeAll { t ->
            val dx = t.worldX - attacker.worldX
            val dy = t.worldY - attacker.worldY
            dx * dx + dy * dy > range * range
            || TargetFilter.shouldFilter(slot, t)
        }

        // 3. 能量=0 → 冷却
        if (energyBar.progress <= 0f) {
            lockedTargets.clear()
            // ★ 如果禁止冷却恢复，不推进冷却
            if (!disableCooldownRecovery) {
                cooldownBar.addProgress(dt / cd * WeaponConstants.cooldownMultiplier)
            }
            if (cooldownBar.isFinished) {
                energyBar.setProgress(1f)
                cooldownBar.reset()
            }
            return
        }

        // 4. 有目标 → 攻击
        if (lockedTargets.isNotEmpty()) {
            var dmg = slot.damage * dt * WeaponConstants.cooldownMultiplier
            if (lockedTargets.size > 1) dmg /= lockedTargets.size.toFloat()
            for (t in lockedTargets.toList()) {
                val hp = host.healthMap[t] ?: continue
                if (hp <= 0) { lockedTargets.remove(t); continue }
                host.applyDamage(t, attacker, slot, com.rtnp.demo.combat.DamageType.PULSE, dmg)
            }
            energyBar.reduceProgress(dt / ed * WeaponConstants.cooldownMultiplier)
            noTargetStartTime = 0L
            return
        }

        // 5. 无目标 → 恢复
        if (noTargetStartTime == 0L) noTargetStartTime = now
        if (now - noTargetStartTime >= rd * 1000L) {
            // ★ 如果禁止能量恢复，不推进能量
            if (!disableEnergyRegen) {
                energyBar.addProgress(dt * rs * WeaponConstants.cooldownMultiplier)
            }
        }
    }

    /** 获取当前能量值 (0.0 ~ 1.0) */
    fun getEnergyProgress(): Float = energyBar.progress

    /** 获取当前冷却值 (0.0 ~ 1.0) */
    fun getCooldownProgress(): Float = cooldownBar.progress
}