package com.rtnp.demo.ui.tools

import kotlin.math.min

/**
 * 单位信息面板贴图动画（纯静态）。
 *
 * 周期（共 11.333s）：
 *  阶段 1a  [0, 2s)      : 30°/s 匀速，scale = 1.0
 *  阶段 1b  [2, 5s)      : 30→90°/s 加速，scale 1.0→1.5
 *  阶段 1c  [5, 6.333s)  : 90°/s 匀速，scale = 1.5（累计到 360°）
 *  阶段 2   [6.333, 8.333s): 60°/s 匀速，scale 1.5→1.0（累计到 480°，即 120°）
 *  阶段 3   [8.333, 11.333s): 短加速长减速，从 120° 精确回到 0°
 *
 * 阶段 3 速度曲线：
 *  0 ~ 0.45s（τ_p = 0.15）线性升到峰值
 *  0.45s ~ 3s 线性降到 0
 *  总积分保证 3s 内精准转 240°
 */
object UnitInfoPanelAnimTool {

    private const val CYCLE_DURATION = 11.333f
    private const val TAU_P = 0.15f
    private const val STAGE3_DURATION = 3f

    private var elapsed = 0f
    private var running = false

    fun start() {
        elapsed = 0f
        running = true
    }

    fun stop() {
        running = false
        elapsed = 0f
    }

    fun update(dt: Float) {
        if (!running) return
        elapsed += dt.coerceIn(0f, 0.1f)
        if (elapsed >= CYCLE_DURATION) elapsed -= CYCLE_DURATION
    }

    /** 当前旋转角度（度） */
    fun getRotation(): Float {
        val t = elapsed
        return when {
            t < 2f -> 30f * t
            t < 5f -> {
                val u = t - 2f
                60f + 30f * u + 10f * u * u
            }
            t < 6.333f -> {
                240f + 90f * (t - 5f)
            }
            t < 8.333f -> {
                360f + 60f * (t - 6.333f)
            }
            else -> {
                val tau = (t - 8.333f) / STAGE3_DURATION
                val f = if (tau <= TAU_P) {
                    tau * tau / TAU_P
                } else {
                    val u = tau - TAU_P
                    TAU_P + 2f * u - u * u / (1f - TAU_P)
                }
                480f + 240f * f
            }
        }
    }

    /** 当前缩放 */
    fun getScale(): Float {
        val t = elapsed
        return when {
            t < 2f -> 1.0f
            t < 5f -> 1.0f + 0.5f * (t - 2f) / 3f
            t < 6.333f -> 1.5f
            t < 8.333f -> 1.5f - 0.5f * (t - 6.333f) / 2f
            else -> 1.0f
        }
    }
}