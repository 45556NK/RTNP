package com.rtnp.demo.factory

import android.content.Context
import com.rtnp.demo.UnitSystem
import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.logger.Logger

/**
 * 工厂槽管理器（单例）。
 * 负责注册模板、更新工厂槽逻辑、处理点击、更换槽位。
 */
object FactoryManager {

    private val templates = mutableMapOf<String, FactoryTemplate>()
    private var tools: FactoryTools? = null
    private var templatesLoaded = false

    @JvmStatic
    fun init(context: Context, unitSystem: UnitSystem) {
        tools = FactoryTools(unitSystem)
        if (!templatesLoaded) {
            Logger.d("FactoryManager", "开始注册工厂模板")
            FactoryTemplateRegistration.registerFactoryTemplates()
            templatesLoaded = true
            Logger.d("FactoryManager", "工厂模板注册完成，共 ${templates.size} 个")
        }
    }

    @JvmStatic
    fun reinit(unitSystem: UnitSystem) {
        tools = FactoryTools(unitSystem)
    }

    @JvmStatic
    fun getTools(): FactoryTools? = tools

    @JvmStatic
    fun register(template: FactoryTemplate) {
        templates[template.id] = template
    }

    @JvmStatic
    fun getTemplate(id: String): FactoryTemplate? = templates[id]

    @JvmStatic
    fun getAllIds(): Set<String> = templates.keys

    /**
     * 每帧更新所有工厂槽（传入 slotIndex 供模板设置进度条）
     */
    @JvmStatic
    fun updateFactories(unit: PlacedObject, deltaTime: Float) {
        val t = tools ?: return
        for (i in unit.factorySlotConfigs.indices) {
            val config = unit.factorySlotConfigs[i]
            if (config.factoryId.isEmpty()) continue
            val template = templates[config.factoryId] ?: continue
            template.onUpdate(unit, i, deltaTime, t, config.params)
        }
    }

    @JvmStatic
    fun onClickFactory(unit: PlacedObject, slotIndex: Int) {
        val t = tools ?: return
        if (slotIndex < 0 || slotIndex >= unit.factorySlotConfigs.size) return
        val config = unit.factorySlotConfigs[slotIndex]
        val template = templates[config.factoryId] ?: return
        template.onClicked(unit, slotIndex, t, config.params)
    }

    /**
     * 外部修改指定槽位的参数
     */
    @JvmStatic
    fun updateSlotParam(unit: PlacedObject, slotIndex: Int, key: String, value: Any) {
        if (slotIndex < 0 || slotIndex >= unit.factorySlotConfigs.size) return
        unit.factorySlotConfigs[slotIndex].params.put(key, value)
    }

    /**
     * 外部获取指定槽位的参数容器
     */
    @JvmStatic
    fun getSlotData(unit: PlacedObject, slotIndex: Int): FactorySlotData? {
        if (slotIndex < 0 || slotIndex >= unit.factorySlotConfigs.size) return null
        return unit.factorySlotConfigs[slotIndex].params
    }

    /**
     * ★ 更换指定槽位的工厂
     * 会将槽位重置为新的工厂，并填充默认参数
     */
    @JvmStatic
    fun replaceSlot(unit: PlacedObject, slotIndex: Int, factoryId: String) {
        if (slotIndex < 0 || slotIndex >= unit.factorySlots) return
        while (unit.factorySlotConfigs.size <= slotIndex) {
            unit.factorySlotConfigs.add(FactorySlotConfig())
        }
        val config = FactorySlotConfig()
        config.factoryId = factoryId
        // 填充默认参数
        templates[factoryId]?.defaultParams?.forEach { (key, value) ->
            config.params.put(key, value)
        }
        unit.factorySlotConfigs[slotIndex] = config
        // 清空冷却条
        while (unit.factoryCooldownBars.size <= slotIndex) {
            unit.factoryCooldownBars.add(null)
        }
        unit.factoryCooldownBars[slotIndex] = null
    }

    /**
     * ★ 清空指定槽位的工厂
     */
    @JvmStatic
    fun clearSlot(unit: PlacedObject, slotIndex: Int) {
        if (slotIndex < 0 || slotIndex >= unit.factorySlots) return
        replaceSlot(unit, slotIndex, "")
    }
}