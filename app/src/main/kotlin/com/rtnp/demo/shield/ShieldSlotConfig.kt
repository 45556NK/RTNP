package com.rtnp.demo.shield

/**
 * 单个立场槽的完整配置。
 * 包含立场模板ID、该槽位的独立参数、以及图标路径（UI用）。
 */
class ShieldSlotConfig {
    @JvmField var shieldId: String = ""
    @JvmField var params: ShieldSlotData = ShieldSlotData()
    @JvmField var iconPath: String? = null

    fun copy(): ShieldSlotConfig {
        val newConfig = ShieldSlotConfig()
        newConfig.shieldId = shieldId
        newConfig.params = params.copy()
        newConfig.iconPath = iconPath
        return newConfig
    }
}