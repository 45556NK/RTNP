package com.rtnp.demo.ai.tools

import com.rtnp.demo.core.PlacedObject
import kotlin.random.Random

/**
 * AI 随机反应时间工具。
 * 用于给 AI 一种拟人的延迟感：AI 做出决策前需要等待一个随机时长。
 *
 * 精度 0.1 秒。
 */
object AiReactionTimeTool {

    private val elapsedMap = mutableMapOf<Long, Float>()
    private val durationMap = mutableMapOf<Long, Float>()

    /**
     * 每帧调用。返回 true 表示反应时间已到，可以行动。
     * 返回 true 后，内部计时器会自动清空；下一次调用将开启新的随机倒计时。
     */
    @JvmStatic
    fun tick(host: PlacedObject, deltaTime: Float, minSec: Float, maxSec: Float): Boolean {
        val id = host.uniqueId
        if (!durationMap.containsKey(id)) {
            durationMap[id] = randomReaction(minSec, maxSec)
            elapsedMap[id] = 0f
        }
        val newElapsed = (elapsedMap[id] ?: 0f) + deltaTime
        elapsedMap[id] = newElapsed
        if (newElapsed >= (durationMap[id] ?: 0f)) {
            durationMap.remove(id)
            elapsedMap.remove(id)
            return true
        }
        return false
    }

    /**
     * 取消当前倒计时，下次调用 tick 会重新随机。
     */
    @JvmStatic
    fun reset(host: PlacedObject) {
        durationMap.remove(host.uniqueId)
        elapsedMap.remove(host.uniqueId)
    }

    /**
     * 强制重新随机一个倒计时。
     */
    @JvmStatic
    fun reRoll(host: PlacedObject, minSec: Float, maxSec: Float) {
        durationMap[host.uniqueId] = randomReaction(minSec, maxSec)
        elapsedMap[host.uniqueId] = 0f
    }

    private fun randomReaction(minSec: Float, maxSec: Float): Float {
        val range = maxSec - minSec
        if (range <= 0f) return minSec
        val steps = (range * 10).toInt()
        val step = if (steps <= 0) 0 else Random.nextInt(steps + 1)
        return minSec + step * 0.1f
    }
}