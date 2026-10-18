package com.rtnp.demo.ui.bottombar.tools

/**
 * 底部栏按钮动画状态机（工具，静态）。
 *
 * 管理：
 *  - 每个按钮的 scale（按压 + 激活）
 *  - 每个按钮的整体透明度（变暗）
 *  - 每个按钮的名称标签动画
 *  - 顶排整体抬升（由外部每帧 setUpperRowLiftTarget 驱动）
 */
object BottomBarAnimTool {

    private const val PRESS_DURATION = 0.15f
    private const val RELEASE_DURATION = 0.2f
    private const val ACTIVE_DURATION = 0.3f
    private const val DEACTIVE_DURATION = 0.3f
    private const val ALPHA_DURATION = 0.2f
    private const val LABEL_FADE_IN = 0.12f
    private const val LABEL_FADE_OUT = 0.2f
    private const val UPPER_LIFT_DURATION = 0.2f

    private const val PRESSED_SCALE = 0.9f
    private const val ACTIVE_SCALE = 1.5f
    private const val DIM_ALPHA = 0.6f

    private class AnimState {
        var currentScale = 1.0f
        var targetScale = 1.0f
        var scaleStart = 1.0f
        var scaleStartTime = 0L
        var scaleDuration = RELEASE_DURATION

        var currentActive = 0f
        var targetActive = 0f
        var activeStart = 0f
        var activeStartTime = 0L

        var currentAlpha = 1.0f
        var targetAlpha = 1.0f
        var alphaStart = 1.0f
        var alphaStartTime = 0L

        var pressed = false
    }

    private class LabelState {
        var currentAlpha = 0f
        var targetAlpha = 0f
        var alphaStart = 0f
        var alphaStartTime = 0L
        var alphaDuration = LABEL_FADE_IN

        var currentOffset = 1f
        var targetOffset = 1f
        var offsetStart = 1f
        var offsetStartTime = 0L
        var offsetDuration = LABEL_FADE_IN
    }

    private val states = mutableMapOf<String, AnimState>()
    private val labels = mutableMapOf<String, LabelState>()

    // 顶排抬升状态
    private var upperRowLiftTarget = 0f
    private var upperRowLiftCurrent = 0f
    private var upperRowLiftStart = 0f
    private var upperRowLiftStartTime = 0L

    // ==================== 事件入口 ====================

    @JvmStatic
    fun onPress(id: String) {
        val s = states.getOrPut(id) { AnimState() }
        s.pressed = true
        if (s.currentActive < 0.5f) {
            retargetScale(s, PRESSED_SCALE, PRESS_DURATION)
        }
        retargetLabel(labels.getOrPut(id) { LabelState() }, alpha = 1f, offset = 0f, LABEL_FADE_IN)
    }

    @JvmStatic
    fun onRelease(id: String) {
        val s = states[id] ?: return
        s.pressed = false
        retargetLabel(labels[id] ?: return, alpha = 0f, offset = -1f, LABEL_FADE_OUT)
    }

    @JvmStatic
    fun onCancel(id: String) {
        val s = states[id] ?: return
        s.pressed = false
        if (s.currentActive < 0.5f) {
            retargetScale(s, 1.0f, RELEASE_DURATION)
        }
        retargetLabel(labels[id] ?: return, alpha = 0f, offset = -1f, LABEL_FADE_OUT)
    }

    @JvmStatic
    fun setActive(id: String, active: Boolean) {
        val s = states.getOrPut(id) { AnimState() }
        val target = if (active) 1f else 0f
        if (s.targetActive == target) return

        s.activeStart = s.currentActive
        s.targetActive = target
        s.activeStartTime = System.currentTimeMillis()

        if (active) {
            retargetScale(s, ACTIVE_SCALE, ACTIVE_DURATION)
        } else {
            val back = if (s.pressed) PRESSED_SCALE else 1.0f
            retargetScale(s, back, DEACTIVE_DURATION)
        }
    }

    @JvmStatic
    fun setDimmed(id: String, dimmed: Boolean) {
        val s = states.getOrPut(id) { AnimState() }
        val target = if (dimmed) DIM_ALPHA else 1.0f
        if (s.targetAlpha == target) return
        s.alphaStart = s.currentAlpha
        s.targetAlpha = target
        s.alphaStartTime = System.currentTimeMillis()
    }

    /** 设置顶排整体抬升目标（0 ~ 0.8） */
    @JvmStatic
    fun setUpperRowLiftTarget(target: Float) {
        val t = target.coerceIn(0f, 1f)
        if (t == upperRowLiftTarget) return
        upperRowLiftTarget = t
        upperRowLiftStart = upperRowLiftCurrent
        upperRowLiftStartTime = System.currentTimeMillis()
    }

    @JvmStatic
    fun getUpperRowLiftProgress(): Float = upperRowLiftCurrent

    @JvmStatic
    fun clear() {
        states.clear()
        labels.clear()
        upperRowLiftTarget = 0f
        upperRowLiftCurrent = 0f
        upperRowLiftStart = 0f
    }

    // ==================== 每帧更新 ====================

    @JvmStatic
    fun update() {
        val now = System.currentTimeMillis()

        for (s in states.values) {
            val ts = ((now - s.scaleStartTime) / 1000f / s.scaleDuration).coerceIn(0f, 1f)
            s.currentScale = s.scaleStart + (s.targetScale - s.scaleStart) * easeOutCubic(ts)

            val ta = ((now - s.activeStartTime) / 1000f / ACTIVE_DURATION).coerceIn(0f, 1f)
            s.currentActive = s.activeStart + (s.targetActive - s.activeStart) * easeOutCubic(ta)

            val tl = ((now - s.alphaStartTime) / 1000f / ALPHA_DURATION).coerceIn(0f, 1f)
            s.currentAlpha = s.alphaStart + (s.targetAlpha - s.alphaStart) * easeOutCubic(tl)
        }

        for (l in labels.values) {
            val ta = ((now - l.alphaStartTime) / 1000f / l.alphaDuration).coerceIn(0f, 1f)
            l.currentAlpha = l.alphaStart + (l.targetAlpha - l.alphaStart) * easeOutCubic(ta)

            val to = ((now - l.offsetStartTime) / 1000f / l.offsetDuration).coerceIn(0f, 1f)
            l.currentOffset = l.offsetStart + (l.targetOffset - l.offsetStart) * easeOutCubic(to)
        }

        // 顶排抬升
        if (upperRowLiftCurrent != upperRowLiftTarget) {
            val t = ((now - upperRowLiftStartTime) / 1000f / UPPER_LIFT_DURATION).coerceIn(0f, 1f)
            upperRowLiftCurrent = upperRowLiftStart + (upperRowLiftTarget - upperRowLiftStart) * easeOutCubic(t)
            if (t >= 1f) upperRowLiftCurrent = upperRowLiftTarget
        }
    }

    // ==================== 查询 ====================

    @JvmStatic fun getScale(id: String): Float = states[id]?.currentScale ?: 1f
    @JvmStatic fun getAlpha(id: String): Float = states[id]?.currentAlpha ?: 1f
    @JvmStatic fun getActiveProgress(id: String): Float = states[id]?.currentActive ?: 0f
    @JvmStatic fun getLabelAlpha(id: String): Float = labels[id]?.currentAlpha ?: 0f
    @JvmStatic fun getLabelOffsetProgress(id: String): Float = labels[id]?.currentOffset ?: 1f

    // ==================== 内部 ====================

    private fun retargetScale(s: AnimState, target: Float, duration: Float) {
        if (s.targetScale == target) return
        s.scaleStart = s.currentScale
        s.targetScale = target
        s.scaleStartTime = System.currentTimeMillis()
        s.scaleDuration = duration
    }

    private fun retargetLabel(l: LabelState, alpha: Float, offset: Float, duration: Float) {
        if (l.targetAlpha != alpha) {
            l.alphaStart = l.currentAlpha
            l.targetAlpha = alpha
            l.alphaStartTime = System.currentTimeMillis()
            l.alphaDuration = duration
        }
        if (l.targetOffset != offset) {
            l.offsetStart = l.currentOffset
            l.targetOffset = offset
            l.offsetStartTime = System.currentTimeMillis()
            l.offsetDuration = duration
        }
    }

    private fun easeOutCubic(t: Float): Float {
        val u = 1f - t
        return 1f - u * u * u
    }
}