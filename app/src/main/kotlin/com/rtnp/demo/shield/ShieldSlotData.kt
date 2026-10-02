package com.rtnp.demo.shield

/**
 * 立场槽参数容器。
 * 只提供基础的查找与修改服务。
 * 额外提供 renderEnabled、激活/冷却等运行时状态。
 */
class ShieldSlotData {
    private val params = mutableMapOf<String, Any>()

    /** 运行时渲染开关 */
    @JvmField var renderEnabled: Boolean = true

    // ==================== 激活状态 ====================
    /** 是否处于激活状态 */
    @JvmField var isActive: Boolean = false
    /** 激活剩余时间（秒）。仅当 activationDuration > 0 时有意义 */
    @JvmField var remainingTime: Float = 0f
    /** 激活总时长。> 0 有时效；-1 永久 */
    @JvmField var activationDuration: Float = 0f
    /** 激活进度条颜色，0 表示不显示进度条 */
    @JvmField var activationBarColor: Int = 0

    // ==================== 冷却状态 ====================
    /** 冷却剩余时间（秒） */
    @JvmField var cooldownRemaining: Float = 0f
    /** 冷却总时长 */
    @JvmField var cooldownDuration: Float = 0f
    /** 冷却进度条颜色，0 表示不显示进度条 */
    @JvmField var cooldownBarColor: Int = 0

    /** 模板请求立即失效 */
    @JvmField var requestedExpire: Boolean = false

    // ==================== 参数 ====================

    fun put(key: String, value: Any) {
        params[key] = value
    }

    fun putAll(map: Map<String, Any>) {
        params.putAll(map)
    }

    fun getInt(key: String): Int? = params[key] as? Int
    fun getFloat(key: String): Float? = params[key] as? Float
    fun getString(key: String): String? = params[key] as? String
    fun getBoolean(key: String): Boolean? = params[key] as? Boolean

    fun hasKey(key: String): Boolean = params.containsKey(key)
    fun getAll(): Map<String, Any> = params.toMap()

    fun copy(): ShieldSlotData {
        val newData = ShieldSlotData()
        newData.params.putAll(params)
        newData.renderEnabled = renderEnabled
        newData.isActive = isActive
        newData.remainingTime = remainingTime
        newData.activationDuration = activationDuration
        newData.activationBarColor = activationBarColor
        newData.cooldownRemaining = cooldownRemaining
        newData.cooldownDuration = cooldownDuration
        newData.cooldownBarColor = cooldownBarColor
        newData.requestedExpire = requestedExpire
        return newData
    }
}