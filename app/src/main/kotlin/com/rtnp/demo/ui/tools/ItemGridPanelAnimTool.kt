package com.rtnp.demo.ui.tools

/**
 * 物品展示面板选中动画（工具，静态）。
 *
 * 每个槽位独立维护一个 progress：
 *  0.0 = 原始大小
 *  1.0 = 缩小到 SELECTED_SCALE
 */
object ItemGridPanelAnimTool {

    private const val ANIM_DURATION = 0.2f
    private const val SELECTED_SCALE = 0.9f

    private class SlotState(
        var current: Float = 0f,
        var target: Float = 0f
    )

    private val states = mutableMapOf<String, SlotState>()
    private var lastFrameTime = 0L

    /** 设置某槽位的目标状态（1 = 选中缩到 0.8，0 = 恢复） */
    @JvmStatic
    fun setTarget(slotKey: String, target: Float) {
        val s = states.getOrPut(slotKey) { SlotState() }
        s.target = target.coerceIn(0f, 1f)
    }

    /** 读取当前 progress（0~1） */
    @JvmStatic
    fun getProgress(slotKey: String): Float = states[slotKey]?.current ?: 0f

    /** 获取缩放比例 */
    @JvmStatic
    fun getScale(slotKey: String): Float {
        val p = getProgress(slotKey)
        return 1f + (SELECTED_SCALE - 1f) * p
    }

    /** 每帧更新 */
    @JvmStatic
    fun update() {
        val now = System.currentTimeMillis()
        val dt = if (lastFrameTime == 0L) 0f else (now - lastFrameTime) / 1000f
        lastFrameTime = now

        if (dt <= 0f) return
        val step = dt / ANIM_DURATION

        for (s in states.values) {
            if (s.current == s.target) continue
            if (s.current < s.target) {
                s.current = (s.current + step).coerceAtMost(s.target)
            } else {
                s.current = (s.current - step).coerceAtLeast(s.target)
            }
            // 缓动曲线：smoothstep 近似
            // 注：为了让动画看起来自然，用时间线性驱动，绘制时再用缓动曲线映射
        }
    }

    /**
     * 缓动映射：线性 progress → 视觉 progress。
     * 使用 smoothstep 让动画在起止两端放缓。
     */
    @JvmStatic
    fun easeProgress(slotKey: String): Float {
        val p = getProgress(slotKey)
        return p * p * (3f - 2f * p)  // smoothstep
    }

    /** 清除所有状态 */
    @JvmStatic
    fun clear() {
        states.clear()
        lastFrameTime = 0L
    }
}