package com.rtnp.demo.shield

import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.gpu.render.tools.ShieldRenderSystem
import com.rtnp.demo.strategy.CooldownMode

/**
 * 立场槽模板基类。
 */
abstract class ShieldTemplate {

    abstract val id: String
    abstract val displayName: String
    abstract val description: String

    open val defaultParams: Map<String, Any> = emptyMap()
    open val iconPath: String? = null
    open val cooldown: Float = 0f
    open val cooldownMode: CooldownMode = CooldownMode.MANUAL
    open val needsGpuRender: Boolean = false

    /** 是否需要激活 */
    open val requiresActivation: Boolean = false
    
    /** 激活时长（秒）。0 表示永久或不需要激活 */
    open val activationDuration: Float = 0f

    /** 冷却时长（秒） */
    open val cooldownDuration: Float = 0f

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