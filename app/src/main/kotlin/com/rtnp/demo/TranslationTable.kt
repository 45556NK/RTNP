// app/src/main/kotlin/com/rtnp/demo/TranslationTable.kt
package com.rtnp.demo

/**
 * 全局翻译表
 * 独立于 UI，外部可追加/覆盖
 */
object TranslationTable {

    private val table = mutableMapOf<String, String>()

    /**
     * 获取翻译
     * @param key 属性内部名
     * @return 翻译后的文本，未找到返回空字符串
     */
    fun get(key: String): String {
        return table[key] ?: ""
    }

    /**
     * 添加/覆盖翻译
     */
    fun put(key: String, value: String) {
        table[key] = value
    }

    /**
     * 批量添加翻译
     */
    fun putAll(map: Map<String, String>) {
        table.putAll(map)
    }

    // ==================== 基础翻译 ====================

    init {
        // 属性名
        table["health"] = "血量"
        table["speed"] = "速度"
        table["maxSpeed"] = "最大速度"
        table["acceleration"] = "加速度"
        table["deceleration"] = "减速度"
        table["range"] = "射程"
        table["damage"] = "伤害"
        table["cooldown"] = "冷却"
        table["chargeTime"] = "充能时间"
        table["bulletSpeed"] = "子弹速度"

        // 按钮标签
        table["btn_details"] = "详"
        table["btn_exit"] = "退出"
        table["btn_system"] = "系统管理"
        table["btn_action"] = "行动"
        table["btn_action_undock"] = "出动"
        table["btn_stop"] = "停止"

        // 武器类型
        table["weapon_railgun"] = "轨道炮"
        table["weapon_laser"] = "激光"
        table["weapon_missile"] = "导弹"
        table["weapon_pulse"] = "脉冲"

        // 状态
        table["state_on"] = "开"
        table["state_off"] = "关"

        // 阵营
        table["faction_neutral"] = "中立"
        table["faction_enemy"] = "敌对"
        table["faction_player"] = "我方"
        
        table["btn_back"] = "返回"
        table["empty_weapon_slot"] = "空"
        table["empty_strategy_slot"] = "空槽"
        table["system_panel_title"] = "系统管理"
        table["change_component"] = "更改组件"
        table["confirm_replace"] = "确定更换？"
        table["attr_damage"] = "伤害"
        table["attr_charge_time"] = "充能时间"
        table["attr_burst_time"] = "爆发时间"
        table["attr_cooldown"] = "冷却"
        table["attr_energy_regen_time"] = "能量恢复时间"
        table["attr_max_targets"] = "最大目标"
        table["attr_min_range"] = "最小射程"
        table["attr_explosion_damage"] = "爆炸伤害"
        table["attr_explosion_range"] = "爆炸范围"
        table["attr_range"] = "射程"
        table["attr_fire_rate"] = "射速"
        table["hex_selector_confirm"] = "确定？"
        
        // 船坞
        table["shipyard_quantity"] = "生产数量"
        table["shipyard_resource"] = "资源"
        table["shipyard_btn_minus"] = "减少"
        table["shipyard_btn_plus"] = "增加"
        table["shipyard_btn_back"] = "返回"
        table["shipyard_warn_no_resource"] = "仓储资源不足"
        table["shipyard_warn_dock_full"] = "停泊位已满"
        
        // 立场
        table["shield_cooling"] = "立场冷却中"
        table["shield_hp"] = "维持值"
        table["shield_duration"] = "效果时长"
        table["shield_cooldown"] = "冷却"
        table["empty_shield_slot"] = "空立场"
    }
}