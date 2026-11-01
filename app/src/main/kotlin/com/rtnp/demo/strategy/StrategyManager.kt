package com.rtnp.demo.strategy

import android.content.Context
import android.graphics.Canvas
import com.rtnp.demo.UnitSystem
import com.rtnp.demo.core.PlacedObject

object StrategyManager {

    private val templates = mutableMapOf<String, StrategyTemplate>()
    private var tools: StrategyTools? = null
    private var templatesLoaded = false

    /** ★ 需要渲染的活跃策略：单位ID -> 策略ID列表 */
    private val activeRenderUnits = mutableMapOf<Long, List<String>>()

    fun init(context: Context, unitSystem: UnitSystem) {
        tools = StrategyTools(unitSystem)
        if (!templatesLoaded) {
            // ★ 显式注册策略模板（不再使用 DexFile 扫描）
            StrategyTemplateRegistration.registerStrategyTemplates()
            templatesLoaded = true
        }
    }

    fun reinit(unitSystem: UnitSystem) {
        tools = StrategyTools(unitSystem)
    }

    fun getTools(): StrategyTools? = tools

    fun register(template: StrategyTemplate) {
        templates[template.id] = template
    }

    fun getTemplate(id: String): StrategyTemplate? = templates[id]
    fun getAllIds(): Set<String> = templates.keys

    fun updateStrategies(unit: PlacedObject, deltaTime: Float, strategyIds: List<String>) {
        val t = tools ?: return
        for (i in strategyIds.indices) {
            val id = strategyIds[i]
            templates[id]?.onUpdate(unit, deltaTime, t)
        }
        // ★ 更新渲染注册
        if (strategyIds.any { templates[it]?.needsRender == true }) {
            activeRenderUnits[unit.uniqueId] = strategyIds.filter { templates[it]?.needsRender == true }
        } else {
            activeRenderUnits.remove(unit.uniqueId)
        }
    }

    /** ★ 注销单位渲染 */
    fun unregisterRenderUnit(unit: PlacedObject) {
        activeRenderUnits.remove(unit.uniqueId)
    }

    /** ★ 渲染所有活跃策略 */
    fun renderStrategies(canvas: Canvas, allUnits: List<PlacedObject>) {
        val t = tools ?: return
        for (unit in allUnits) {
            for (id in unit.strategyIds) {
                if (id.isEmpty()) continue
                val template = templates[id] ?: continue
                if (template.needsRender) {
                    template.onRender(canvas, unit, t)
                }
            }
        }
    }
    
    fun renderGpuStrategies(units: List<PlacedObject>, renderSystem: com.rtnp.demo.gpu.render.tools.StrategyRenderSystem) {
        val t = tools ?: return
        for (unit in units) {
            for (id in unit.strategyIds) {
                if (id.isEmpty()) continue
                val template = templates[id] ?: continue
                if (template.needsGpuRender) {
                    template.onGpuRender(unit, t, renderSystem)
                }
            }
        }
    }

    fun onClickStrategy(unit: PlacedObject, strategyId: String, slotIndex: Int) {
        val t = tools ?: return
        val template = templates[strategyId] ?: return

        // ★ 检查冷却条是否在冷却中
        if (template.cooldown > 0f && slotIndex < unit.strategyCooldownBars.size) {
            val bar = unit.strategyCooldownBars[slotIndex]
            if (bar != null && !bar.isFinished) return
        }

        // ★ 新增：条件检查。不满足时直接返回，不点击、不冷却
        if (!template.canUse(unit, t, slotIndex)) return

        if (template.cooldownMode == CooldownMode.CLICK && template.cooldown > 0f) {
            val bar = unit.getOrCreateStrategyCooldownBar(slotIndex, template.cooldown)
            bar.reset()
        }

        template.onClicked(unit, t, slotIndex)
    }
}