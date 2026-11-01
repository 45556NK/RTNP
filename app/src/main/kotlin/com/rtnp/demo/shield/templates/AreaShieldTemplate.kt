package com.rtnp.demo.shield.templates

import android.graphics.Color
import com.rtnp.demo.combat.DamageType
import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.gpu.render.tools.ShieldRenderSystem
import com.rtnp.demo.shield.ShieldSlotData
import com.rtnp.demo.shield.ShieldTemplate
import com.rtnp.demo.shield.ShieldTools

/**
 * 区域护盾（测试用）。
 *
 * 保护范围内所有同阵营单位，维持值 5000，激活时长 60 秒，冷却 20 秒，半径 300。
 * 被保护单位受到的伤害，由区域护盾平分吸收。
 * 宿主自己受到的伤害仅吸收 40%，其余 60% 由宿主承担。
 * 维持值耗尽时立即失效。
 */
class AreaShieldTemplate : ShieldTemplate() {

    override val id = "area_shield"
    override val displayName = "区域护盾"
    override val description = "保护范围内所有友方单位\n平分吸收伤害。宿主自身仅吸收40%"

    override val requiresActivation: Boolean = true
    override val isAreaShield: Boolean = true
    override val areaRadius: Float = 300f
    override val protectsFriendlyOnly: Boolean = true

    override val defaultParams: Map<String, Any> = mapOf(
        "maxHp" to 5000,
        "currentHp" to 5000
    )

    override val needsGpuRender: Boolean = true
    override val activationDuration = 60f
    override val cooldownDuration = 20f

    companion object {
        /** 对宿主自己的吸收比例 */
        private const val HOST_SELF_ABSORB_RATIO = 0.4f
    }

    override fun onActivate(
        host: PlacedObject,
        slotIndex: Int,
        tools: ShieldTools,
        slotData: ShieldSlotData
    ) {
        val maxHp = slotData.getFloat("maxHp") ?: 5000f
        slotData.put("currentHp", maxHp)
        slotData.renderEnabled = true

        tools.activate(
            host = host,
            slotIndex = slotIndex,
            duration = activationDuration,
            progressValue = 1f,
            progressColor = Color.argb(200, 80, 180, 255)
        )
    }

    override fun onUpdate(
        host: PlacedObject,
        slotIndex: Int,
        deltaTime: Float,
        tools: ShieldTools,
        slotData: ShieldSlotData
    ) {
        val maxHp = slotData.getFloat("maxHp") ?: 5000f
        val currentHp = slotData.getFloat("currentHp") ?: 5000f

        val ratio = if (maxHp > 0f) (currentHp / maxHp).coerceIn(0f, 1f) else 0f
        val label = "${currentHp.toInt()}/${maxHp.toInt()}"
        val detailLayout = tools.buildShieldDetailLayout(
            displayName = displayName,
            currentHp = currentHp,
            maxHp = maxHp,
            extraDescription = "半径 ${areaRadius.toInt()}"
        )
        tools.setProgressBarRaw(
            host = host,
            slotIndex = slotIndex,
            progress = ratio,
            color = Color.argb(200, 100, 200, 255),
            label = label,
            detailLayout = detailLayout
        )
    }

    override fun onExpire(
        host: PlacedObject,
        slotIndex: Int,
        tools: ShieldTools,
        slotData: ShieldSlotData
    ) {
        slotData.renderEnabled = false
        slotData.put("currentHp", 0f)
        tools.clearButtonState(host, slotIndex)
        tools.cooldown(
            host = host,
            slotIndex = slotIndex,
            duration = cooldownDuration,
            progressValue = 0f,
            progressColor = 0
        )
    }

    override fun onGpuRender(
        host: PlacedObject,
        slotIndex: Int,
        tools: ShieldTools,
        renderSystem: ShieldRenderSystem
    ) {
        val r = areaRadius
        renderSystem.addCircle(
            host.worldX, host.worldY, r,
            floatArrayOf(0.3f, 0.7f, 1f, 0.12f)
        )
        renderSystem.addRing(
            host.worldX, host.worldY, r, r - 8f,
            floatArrayOf(0.4f, 0.8f, 1f, 0.9f)
        )
        renderSystem.addCircle(
            host.worldX, host.worldY, 30f,
            floatArrayOf(0.6f, 0.9f, 1f, 0.4f)
        )
    }

    override fun onAbsorbArea(
        protectedTarget: PlacedObject,
        host: PlacedObject,
        slotIndex: Int,
        tools: ShieldTools,
        attacker: PlacedObject?,
        weapon: PlacedObject.WeaponSlot?,
        damageType: DamageType,
        damage: Float,
        slotData: ShieldSlotData
    ): Float {
        val currentHp = slotData.getFloat("currentHp") ?: 5000f
        if (currentHp <= 0f) return damage

        // 宿主自身：仅吸收 40%；其他单位：100% 吸收
        val absorbRatio = if (protectedTarget === host) HOST_SELF_ABSORB_RATIO else 1f
        val maxAbsorb = damage * absorbRatio
        val absorbed = minOf(maxAbsorb, currentHp)
        val newHp = currentHp - absorbed
        slotData.put("currentHp", newHp)

        if (newHp <= 0f) {
            tools.requestExpire(host, slotIndex)
        }

        return (damage - absorbed).coerceAtLeast(0f)
    }
}