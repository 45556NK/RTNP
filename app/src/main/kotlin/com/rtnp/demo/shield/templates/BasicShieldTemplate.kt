package com.rtnp.demo.shield.templates

import android.graphics.Color
import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.combat.DamageType
import com.rtnp.demo.gpu.render.tools.ShieldRenderSystem
import com.rtnp.demo.shield.ShieldSlotData
import com.rtnp.demo.shield.ShieldTemplate
import com.rtnp.demo.shield.ShieldTools

/**
 * 初代立场（测试用）
 * 维持值 3000，激活时长 30 秒，冷却 10 秒
 */
class BasicShieldTemplate : ShieldTemplate() {

    override val id = "basic_shield"
    override val displayName = "初代立场"
    override val description = "基础护盾，拥有 3000 维持值，激活 30 秒"

    override val requiresActivation: Boolean = true

    override val defaultParams: Map<String, Any> = mapOf(
        "maxHp" to 3000,
        "currentHp" to 3000
    )

    override val needsGpuRender: Boolean = true

    override val activationDuration = 30f
    override val cooldownDuration = 10f

    override fun onActivate(
        host: PlacedObject,
        slotIndex: Int,
        tools: ShieldTools,
        slotData: ShieldSlotData
    ) {
        // 激活时重置维持值为满
        val maxHp = slotData.getFloat("maxHp") ?: 3000f
        slotData.put("currentHp", maxHp)

        // 恢复渲染
        slotData.renderEnabled = true

        // 声明激活参数：30 秒
        tools.activate(
            host = host,
            slotIndex = slotIndex,
            duration = activationDuration,
            progressValue = 1f,
            progressColor = Color.argb(200, 0, 60, 180)
        )
    }

    override fun onUpdate(
        host: PlacedObject,
        slotIndex: Int,
        deltaTime: Float,
        tools: ShieldTools,
        slotData: ShieldSlotData
    ) {
        val maxHp = slotData.getFloat("maxHp") ?: 3000f
        val currentHp = slotData.getFloat("currentHp") ?: 3000f

        // 右侧栏唯一的进度条：维持值，只显示数值
        val ratio = if (maxHp > 0f) (currentHp / maxHp).coerceIn(0f, 1f) else 0f
        val label = "${currentHp.toInt()}/${maxHp.toInt()}"
        val detailLayout = tools.buildShieldDetailLayout(
            displayName = displayName,
            currentHp = currentHp,
            maxHp = maxHp,
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
        // 停止渲染
        slotData.renderEnabled = false

        // 清除维持值进度条（并清零维持值）
        slotData.put("currentHp", 0f)
        tools.clearButtonState(host, slotIndex)

        // 进入冷却：10 秒
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
        val refRadius = host.referenceVolume?.radius?.takeIf { it > 0f }
            ?: (maxOf(host.width.toFloat(), host.height.toFloat()) / 2f)

        val ringRadius = refRadius * 1.5f

        // 只渲染一个圆环
        renderSystem.addRing(
            host.worldX, host.worldY,
            ringRadius, ringRadius - 8f,
            floatArrayOf(0.4f, 0.7f, 1f, 0.8f)
        )
    }

    override fun onAbsorb(
        host: PlacedObject,
        slotIndex: Int,
        tools: ShieldTools,
        attacker: PlacedObject?,
        weapon: PlacedObject.WeaponSlot?,
        damageType: DamageType,
        damage: Float,
        slotData: ShieldSlotData
    ): Float {
        val currentHp = slotData.getFloat("currentHp") ?: 3000f
        if (currentHp <= 0f) return 0f

        val absorbed = if (damage <= currentHp) damage else currentHp
        val newHp = currentHp - absorbed
        slotData.put("currentHp", newHp)

        // 维持值耗尽 → 请求立即失效
        if (newHp <= 0f) {
            tools.requestExpire(host, slotIndex)
        }

        return absorbed
    }
}