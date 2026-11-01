package com.rtnp.demo.strategy

import android.graphics.Canvas
import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.gpu.render.tools.StrategyRenderSystem

abstract class StrategyTemplate {

    abstract val id: String
    abstract val displayName: String
    abstract val description: String

    open val cooldown: Float = 0f
    open val cooldownMode: CooldownMode = CooldownMode.CLICK
    open val blacklistTypes: List<String> = emptyList()

    /** ★ 策略图标路径（assets 相对路径），为 null 则使用默认文字 */
    open val iconPath: String? = null

    /** 是否需要 Canvas 渲染（旧版） */
    open val needsRender: Boolean = false

    /** ★ 是否需要 GPU 渲染（新版） */
    open val needsGpuRender: Boolean = false
    
    open val hiddenFromPlayer: Boolean = false

    fun canUseOn(unit: PlacedObject): Boolean {
        if (blacklistTypes.isEmpty()) return true
        return !blacklistTypes.contains(unit.type) && !blacklistTypes.contains(unit.category)
    }
    
    /**
 * 使用前条件检查。
 * 返回 false 时：不会执行 onClicked，也不会进入冷却。
 * 必须无副作用（只读状态，不改数据）。
 * 默认返回 true（不做检查）。
    */
    open fun canUse(host: PlacedObject, tools: StrategyTools, slotIndex: Int): Boolean = true

    abstract fun onUpdate(host: PlacedObject, deltaTime: Float, tools: StrategyTools)

    open fun onClicked(host: PlacedObject, tools: StrategyTools, slotIndex: Int) {}

    /** 旧版 Canvas 渲染 */
    open fun onRender(canvas: Canvas, host: PlacedObject, tools: StrategyTools) {}

    /** ★ 新版 GPU 渲染回调，在渲染线程中调用 */
    open fun onGpuRender(host: PlacedObject, tools: StrategyTools, renderSystem: StrategyRenderSystem) {}
}

enum class CooldownMode {
    CLICK,
    MANUAL
}