package com.rtnp.demo.shield

import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.gpu.render.tools.ShieldRenderSystem
import com.rtnp.demo.strategy.CooldownMode

/**
 * 立场槽模板基类。
 */
abstract class ShieldTemplate {

    // ============ 原有字段 ============
    abstract val id: String
    abstract val displayName: String
    abstract val description: String
    open val defaultParams: Map<String, Any> = emptyMap()
    open val iconPath: String? = null
    open val cooldown: Float = 0f
    open val cooldownMode: CooldownMode = CooldownMode.MANUAL
    open val needsGpuRender: Boolean = false
    open val requiresActivation: Boolean = false
    open val activationDuration: Float = 0f
    open val cooldownDuration: Float = 0f

    // ============ 区域立场相关 ============

    /** 是否为区域立场 */
    open val isAreaShield: Boolean = false

    /** 区域立场半径（仅 isAreaShield = true 有效） */
    open val areaRadius: Float = 0f

    /** 是否只保护同阵营（默认 true） */
    open val protectsFriendlyOnly: Boolean = true

    /**
     * 区域立场替其他单位吸收伤害。
     *
     * 与 [onAbsorb] 的区别：
     *  - protectedTarget = 被保护的单位（受到伤害的目标 A）
     *  - host            = 区域立场的宿主（B，站在 A 附近）
     *  - slotIndex       = 区域立场在 host 里的槽位索引
     *  - slotData        = B 的该槽位的立场参数（维持值扣这里）
     *
     * 返回语义：**吸收后仍需造成的伤害**（0 ~ damage）。
     * 即：吸收了 x 点伤害，返回 damage - x。
     *
     * 默认实现：不吸收，返回原伤害。
     */
    open fun onAbsorbArea(
        protectedTarget: PlacedObject,
        host: PlacedObject,
        slotIndex: Int,
        tools: ShieldTools,
        attacker: PlacedObject?,
        weapon: PlacedObject.WeaponSlot?,
        damageType: com.rtnp.demo.combat.DamageType,
        damage: Float,
        slotData: ShieldSlotData
    ): Float {
        return damage
    }

    /**
     * 每帧更新（仅激活状态下调用）。
     */
    abstract fun onUpdate(
        host: PlacedObject,
        slotIndex: Int,
        deltaTime: Float,
        tools: ShieldTools,
        slotData: ShieldSlotData
    )

    /**
     * 激活时调用。
     */
    open fun onActivate(
        host: PlacedObject,
        slotIndex: Int,
        tools: ShieldTools,
        slotData: ShieldSlotData
    ) {}

    /**
     * 失效时调用（善后函数）。
     */
    open fun onExpire(
        host: PlacedObject,
        slotIndex: Int,
        tools: ShieldTools,
        slotData: ShieldSlotData
    ) {}

    /**
     * GPU 渲染回调。
     */
    open fun onGpuRender(
        host: PlacedObject,
        slotIndex: Int,
        tools: ShieldTools,
        renderSystem: ShieldRenderSystem
    ) {}

    /**
     * 伤害拦截（签名已统一：包含 slotIndex 和 tools）。
     */
    open fun onAbsorb(
        host: PlacedObject,
        slotIndex: Int,
        tools: ShieldTools,
        attacker: PlacedObject?,
        weapon: PlacedObject.WeaponSlot?,
        damageType: com.rtnp.demo.combat.DamageType,
        damage: Float,
        slotData: ShieldSlotData
    ): Float = 0f

    /**
     * 宿主死亡 / 立场被销毁时调用。
     */
    open fun onDestroy(
        host: PlacedObject,
        slotIndex: Int,
        tools: ShieldTools,
        slotData: ShieldSlotData
    ) {}
}