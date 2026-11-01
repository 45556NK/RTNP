package com.rtnp.demo.shield

import android.content.Context
import com.rtnp.demo.UnitSystem
import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.gpu.render.tools.ShieldRenderSystem
import com.rtnp.demo.logger.Logger

/**
 * 立场槽管理器（单例）。
 * 负责注册模板、每帧更新、伤害拦截、销毁、更换槽位。
 */
object ShieldManager {

    private val templates = mutableMapOf<String, ShieldTemplate>()
    private var tools: ShieldTools? = null
    private var templatesLoaded = false

    @JvmStatic
    fun init(context: Context, unitSystem: UnitSystem) {
        tools = ShieldTools(unitSystem)
        if (!templatesLoaded) {
            Logger.d("ShieldManager", "开始注册立场模板")
            ShieldTemplateRegistration.registerShieldTemplates()
            templatesLoaded = true
            Logger.d("ShieldManager", "立场模板注册完成，共 ${templates.size} 个")
        }
    }

    @JvmStatic
    fun reinit(unitSystem: UnitSystem) {
        tools = ShieldTools(unitSystem)
    }

    @JvmStatic
    fun getTools(): ShieldTools? = tools

    @JvmStatic
    fun register(template: ShieldTemplate) {
        templates[template.id] = template
    }

    @JvmStatic
    fun getTemplate(id: String): ShieldTemplate? = templates[id]

    @JvmStatic
    fun getAllIds(): Set<String> = templates.keys

    // ==================== 每帧更新 ====================

    @JvmStatic
    fun updateShields(unit: PlacedObject, deltaTime: Float) {
        val t = tools ?: return
        for (i in unit.shieldSlotConfigs.indices) {
            val config = unit.shieldSlotConfigs[i]
            if (config.shieldId.isEmpty()) continue
            val template = templates[config.shieldId] ?: continue
            val data = config.params

            // 1. 处理请求失效
            if (data.requestedExpire) {
                data.requestedExpire = false
                expireShield(unit, i)
                continue
            }

            // 2. 永久激活模板：每帧调用 onUpdate
            if (!template.requiresActivation) {
                template.onUpdate(unit, i, deltaTime, t, data)
                continue
            }

            // 3. 需激活模板
            if (data.isActive) {
                // 运行逻辑
                template.onUpdate(unit, i, deltaTime, t, data)

                // 检测激活超时
                if (data.activationDuration > 0f && data.remainingTime > 0f) {
                    data.remainingTime -= deltaTime
                    if (data.remainingTime <= 0f) {
                        expireShield(unit, i)
                        continue
                    }
                }
            } else if (data.cooldownRemaining > 0f) {
                // 冷却中：只推进时间，UI 由 ShieldButtonTool 读取
                data.cooldownRemaining -= deltaTime
                if (data.cooldownRemaining <= 0f) {
                    data.cooldownRemaining = 0f
                    data.cooldownDuration = 0f
                    data.cooldownBarColor = 0
                }
            }
            // 否则：未激活未冷却，什么都不做
        }
    }

    // ==================== 激活 / 失效 ====================

    /**
     * 激活指定槽位的立场（供 UI 调用）。
     * 若立场已激活或处于冷却中，忽略。
     */
    @JvmStatic
    fun activateShield(host: PlacedObject, slotIndex: Int) {
        val t = tools ?: return
        if (slotIndex < 0 || slotIndex >= host.shieldSlotConfigs.size) return
        val config = host.shieldSlotConfigs[slotIndex]
        if (config.shieldId.isEmpty()) return
        val template = templates[config.shieldId] ?: return
        val data = config.params

        if (data.isActive) return
        if (data.cooldownRemaining > 0f) return

        data.isActive = true
        data.remainingTime = -1f
        data.activationDuration = 0f
        data.activationBarColor = 0

        template.onActivate(host, slotIndex, t, data)
    }

    /**
     * 触发指定槽位立场失效（内部调用）。
     * 只做状态清理和调用模板 onExpire，不操作右侧栏进度条。
     */
    private fun expireShield(host: PlacedObject, slotIndex: Int) {
        val t = tools ?: return
        if (slotIndex < 0 || slotIndex >= host.shieldSlotConfigs.size) return
        val config = host.shieldSlotConfigs[slotIndex]
        if (config.shieldId.isEmpty()) return
        val template = templates[config.shieldId] ?: return
        val data = config.params

        if (!data.isActive) return

        data.isActive = false
        data.remainingTime = 0f
        data.activationDuration = 0f
        data.activationBarColor = 0

        // 调用模板善后（模板内部负责声明冷却、停止渲染等）
        template.onExpire(host, slotIndex, t, data)
    }

    // ==================== 伤害拦截 ====================

    /**
     * 区域立场内部引用（收集后按距离排序用）
     */
    private data class AreaShieldRef(
        val owner: PlacedObject,
        val slotIndex: Int,
        val template: ShieldTemplate,
        val distSq: Float
    )

    @JvmStatic
    fun absorbDamage(
        host: PlacedObject,
        allUnits: List<PlacedObject>,
        attacker: PlacedObject?,
        weapon: PlacedObject.WeaponSlot?,
        damageType: com.rtnp.demo.combat.DamageType,
        damage: Float
    ): Float {
        if (damage <= 0f) return 0f
        val t = tools ?: return damage
        var remaining = damage

        // ============ 1. 区域立场（平分吸收） ============
        if (remaining > 0f && allUnits.isNotEmpty()) {
            val areaShields = mutableListOf<AreaShieldRef>()

            for (other in allUnits) {
                if (other.shieldSlotConfigs.isEmpty()) continue
                // 阵营过滤
                for (i in other.shieldSlotConfigs.indices) {
                    val cfg = other.shieldSlotConfigs[i]
                    if (cfg.shieldId.isEmpty()) continue
                    val tpl = templates[cfg.shieldId] ?: continue
                    if (!tpl.isAreaShield) continue
                    if (tpl.requiresActivation && !cfg.params.isActive) continue
                    if (tpl.protectsFriendlyOnly && other.faction != host.faction) continue

                    val dx = other.worldX - host.worldX
                    val dy = other.worldY - host.worldY
                    val distSq = dx * dx + dy * dy
                    if (distSq > tpl.areaRadius * tpl.areaRadius) continue

                    areaShields.add(AreaShieldRef(other, i, tpl, distSq))
                }
            }

            if (areaShields.isNotEmpty()) {
                areaShields.sortBy { it.distSq }
                val share = remaining / areaShields.size

                var totalAbsorbed = 0f
                for (ref in areaShields) {
                    val stillNeed = ref.template.onAbsorbArea(
                        protectedTarget = host,
                        host = ref.owner,
                        slotIndex = ref.slotIndex,
                        tools = t,
                        attacker = attacker,
                        weapon = weapon,
                        damageType = damageType,
                        damage = share,
                        slotData = ref.owner.shieldSlotConfigs[ref.slotIndex].params
                    )
                    val absorbed = (share - stillNeed.coerceIn(0f, share)).coerceAtLeast(0f)
                    totalAbsorbed += absorbed
                }
                remaining -= totalAbsorbed
                if (remaining < 0f) remaining = 0f
            }
        }

        // ============ 2. 自身普通立场 ============
        if (remaining > 0f) {
            for (i in host.shieldSlotConfigs.indices) {
                if (remaining <= 0f) break
                val config = host.shieldSlotConfigs[i]
                if (config.shieldId.isEmpty()) continue
                val template = templates[config.shieldId] ?: continue
                if (template.isAreaShield) continue  // 区域立场已在上段处理
                if (template.requiresActivation && !config.params.isActive) continue

                val absorbed = template.onAbsorb(
                    host, i, t, attacker, weapon, damageType, remaining, config.params
                ).coerceIn(0f, remaining)
                remaining -= absorbed
            }
        }

        return remaining
    }

    // ==================== 销毁 ====================

    /**
     * 安全销毁指定槽位的立场（触发 onDestroy）。
     * 仅在宿主死亡或特殊原因时调用。
     */
    @JvmStatic
    fun destroyShield(host: PlacedObject, slotIndex: Int) {
        val t = tools ?: return
        if (slotIndex < 0 || slotIndex >= host.shieldSlotConfigs.size) return
        val config = host.shieldSlotConfigs[slotIndex]
        if (config.shieldId.isEmpty()) return
        val template = templates[config.shieldId] ?: return
        template.onDestroy(host, slotIndex, t, config.params)
        // 清空右侧栏进度条
        t.clearButtonState(host, slotIndex)
        host.shieldSlotConfigs[slotIndex] = ShieldSlotConfig()
    }

    @JvmStatic
    fun destroyAllShields(host: PlacedObject) {
        for (i in host.shieldSlotConfigs.indices) {
            destroyShield(host, i)
        }
    }

    // ==================== 槽位操作 ====================

    @JvmStatic
    fun updateSlotParam(unit: PlacedObject, slotIndex: Int, key: String, value: Any) {
        if (slotIndex < 0 || slotIndex >= unit.shieldSlotConfigs.size) return
        unit.shieldSlotConfigs[slotIndex].params.put(key, value)
    }

    @JvmStatic
    fun getSlotData(unit: PlacedObject, slotIndex: Int): ShieldSlotData? {
        if (slotIndex < 0 || slotIndex >= unit.shieldSlotConfigs.size) return null
        return unit.shieldSlotConfigs[slotIndex].params
    }

    @JvmStatic
    fun replaceSlot(unit: PlacedObject, slotIndex: Int, shieldId: String) {
        if (slotIndex < 0 || slotIndex >= unit.shieldSlots) return
        while (unit.shieldSlotConfigs.size <= slotIndex) {
            unit.shieldSlotConfigs.add(ShieldSlotConfig())
        }
        val config = ShieldSlotConfig()
        config.shieldId = shieldId
        templates[shieldId]?.defaultParams?.forEach { (key, value) ->
            config.params.put(key, value)
        }
        unit.shieldSlotConfigs[slotIndex] = config
    }

    @JvmStatic
    fun clearSlot(unit: PlacedObject, slotIndex: Int) {
        if (slotIndex < 0 || slotIndex >= unit.shieldSlots) return
        unit.shieldSlotConfigs[slotIndex] = ShieldSlotConfig()
    }

    // ==================== 渲染 ====================

    @JvmStatic
    fun renderGpuShields(units: List<PlacedObject>, renderSystem: ShieldRenderSystem) {
        val t = tools ?: return
        for (unit in units) {
            if (unit.shieldSlotConfigs.isEmpty()) continue
            for (i in unit.shieldSlotConfigs.indices) {
                val config = unit.shieldSlotConfigs[i]
                if (config.shieldId.isEmpty()) continue
                val template = templates[config.shieldId] ?: continue
                if (!template.needsGpuRender) continue
                if (!config.params.renderEnabled) continue
                // 需激活模板未激活时不渲染
                if (template.requiresActivation && !config.params.isActive) continue
                template.onGpuRender(unit, i, t, renderSystem)
            }
        }
    }
}