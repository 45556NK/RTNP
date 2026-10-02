package com.rtnp.demo.factory

import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.strategy.CooldownMode

/**
 * 工厂槽模板基类。
 * 工厂槽行为与策略槽一致，但独立系统。
 */
abstract class FactoryTemplate {

    abstract val id: String
    abstract val displayName: String
    abstract val description: String

    open val cooldown: Float = 0f
    open val cooldownMode: CooldownMode = CooldownMode.CLICK

    /** 默认参数，创建槽位时由系统自动填充到 FactorySlotData 中 */
    open val defaultParams: Map<String, Any> = emptyMap()

    /** 默认图标路径（UI用） */
    open val iconPath: String? = null

    /**
     * 每帧更新
     * @param host 宿主单位
     * @param slotIndex 当前工厂槽索引（供设置进度条使用）
     * @param deltaTime 帧间隔
     * @param tools 工具集
     * @param slotData 当前槽位的独立参数容器
     */
    abstract fun onUpdate(
        host: PlacedObject,
        slotIndex: Int,
        deltaTime: Float,
        tools: FactoryTools,
        slotData: FactorySlotData
    )

    /**
     * 玩家点击工厂槽时触发
     */
    open fun onClicked(
        host: PlacedObject,
        slotIndex: Int,
        tools: FactoryTools,
        slotData: FactorySlotData
    ) {}
}