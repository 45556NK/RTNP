package com.rtnp.demo.strategy

import com.rtnp.demo.UnitSystem
import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.core.InventoryItem
import com.rtnp.demo.core.TemplateRegistry
import com.rtnp.demo.core.GameConstants
import com.rtnp.demo.gpu.render.tools.StrategyRenderSystem
import com.rtnp.demo.gpu.render.tools.ProgressCircleSystem
import com.rtnp.demo.strategy.tools.StrategyProgressBarTool

class StrategyTools(private val unitSystem: UnitSystem) {

    /** 策略进度条工具（右侧栏显示） */
    val progressBar: StrategyProgressBarTool = StrategyProgressBarTool

    /** Toast 回调，由外部注入 */
    var showToast: ((String) -> Unit)? = null

    // 上一帧存活单位快照（使用 uniqueId 映射以便查找对象）
    private var previousAliveMap: Map<Long, PlacedObject> = emptyMap()
    // 本帧死亡单位（自动覆盖，不需要消费）
    private val recentlyDeadUnits = mutableListOf<PlacedObject>()

    /**
     * 每帧开始时调用，自动对比上一帧与当前帧，统计死亡单位。
     */
    fun updateFrame() {
        val currentAlive = unitSystem.placedObjects
        val currentAliveMap = currentAlive.associateBy { it.uniqueId }

        recentlyDeadUnits.clear()
        if (previousAliveMap.isNotEmpty()) {
            for ((id, unit) in previousAliveMap) {
                if (!currentAliveMap.containsKey(id)) {
                    recentlyDeadUnits.add(unit)
                }
            }
        }

        previousAliveMap = currentAliveMap
    }

    fun getRecentlyDeadUnits(): List<PlacedObject> = recentlyDeadUnits

    fun getAllUnits(): List<PlacedObject> = unitSystem.placedObjects.toList()

    fun getUnitsByFaction(faction: Int): List<PlacedObject> =
        unitSystem.placedObjects.filter { it.faction == faction }

    fun getHealth(unit: PlacedObject): Float =
        unitSystem.healthMap[unit] ?: 0f

    fun distance(a: PlacedObject, b: PlacedObject): Float {
        val dx = a.worldX - b.worldX
        val dy = a.worldY - b.worldY
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }

    fun isValidTarget(attacker: PlacedObject, target: PlacedObject): Boolean {
        if (target.faction == attacker.faction || target.faction == 0) return false
        val hp = unitSystem.healthMap[target] ?: return false
        return hp > 0
    }

    fun getLockedTarget(unit: PlacedObject, slotIndex: Int): PlacedObject? {
        if (slotIndex < 0 || slotIndex >= unit.weaponSlots.size) return null
        return unit.weaponSlots[slotIndex].lockedTarget
    }

    fun setLockedTarget(unit: PlacedObject, slotIndex: Int, target: PlacedObject?) {
        if (slotIndex < 0 || slotIndex >= unit.weaponSlots.size) return
        unit.weaponSlots[slotIndex].lockedTarget = target
    }

    fun setHealth(unit: PlacedObject, health: Float) {
        unitSystem.healthMap[unit] = health
        unit.health = health.toInt()
    }

    fun spawnUnit(templateName: String, worldX: Float, worldY: Float): PlacedObject? {
        val template = TemplateRegistry.templates.firstOrNull { it.name == templateName }
            ?: return null
        unitSystem.addObjectFromItem(template, worldX, worldY)
        return unitSystem.placedObjects.lastOrNull()
    }

    fun spawnUnit(item: InventoryItem, worldX: Float, worldY: Float): PlacedObject? {
        unitSystem.addObjectFromItem(item, worldX, worldY)
        return unitSystem.placedObjects.lastOrNull()
    }

    fun activateRangeSelector(host: PlacedObject, range: Float,
                            onInside: (Float, Float) -> Unit,
                            onOutside: () -> Unit) {
        StrategyToolLayer.activate(host, range)
        StrategyToolLayer.onTapInside = onInside
        StrategyToolLayer.onTapOutside = onOutside
    }

    fun deactivateRangeSelector() {
        StrategyToolLayer.deactivate()
    }

    fun launchMissileAtCoord(launcher: PlacedObject, targetWorldX: Float, targetWorldY: Float, missileTypeName: String): PlacedObject? {
        return unitSystem.missileSystem?.launchMissileAtCoord(launcher, targetWorldX, targetWorldY, missileTypeName)
    }

    fun launchMissileAtPosition(launcher: PlacedObject, targetWorldX: Float, targetWorldY: Float, missileTypeName: String) {
        val type = com.rtnp.demo.core.MissileTypeRegistry.get(missileTypeName) ?: return

        val missile = PlacedObject().apply {
            this.type = "unit"
            category = "missile"
            name = type.name
            this.missileTypeName = missileTypeName
            missileLauncher = launcher
            shape = type.shape
            size = type.size
            worldX = launcher.worldX
            worldY = launcher.worldY
            targetX = targetWorldX
            targetY = targetWorldY
            faction = launcher.faction
            color = type.color
            missileDamage = type.damage
            explosionRange = type.explosionRange
            effectSize = type.effectSize
            missileLifetime = type.lifetime
            missileStartSpeed = type.startSpeed
            missileMaxSpeed = type.maxSpeed
            missileAcceleration = type.acceleration
            missileCurrentSpeed = type.startSpeed.toFloat()
            health = type.health
            originalHealth = type.health
            isMoving = true
            lifetime = 0f
            maxLifetime = type.lifetime.toFloat()
            heading = Math.toDegrees(Math.atan2(
                (targetWorldY - launcher.worldY).toDouble(),
                (targetWorldX - launcher.worldX).toDouble()
            )).toFloat()
            canMove = false
            weaponSlots.clear()
            displayName = "${type.name}(${GameConstants.factionToName(launcher.faction)})"
            currentSpeed = type.startSpeed.toFloat()
        }

        unitSystem.placedObjects.add(missile)
        unitSystem.healthMap[missile] = missile.health.toFloat()
    }

    fun startCooldown(unit: PlacedObject, slotIndex: Int, cooldownTime: Float) {
        val bar = unit.getOrCreateStrategyCooldownBar(slotIndex, cooldownTime)
        bar.reset()
    }

    fun createTask(data: MutableMap<String, Any?>): Long {
        return StrategyDataBus.createTask(data)
    }

    fun bindToTask(unit: PlacedObject, taskId: Long) {
        StrategyDataBus.bindUnitToTask(unit, taskId)
    }

    fun getTaskData(unit: PlacedObject): StrategyDataBus.TaskData? {
        return StrategyDataBus.getTaskData(unit)
    }

    fun updateTaskData(taskId: Long, key: String, value: Any?) {
        StrategyDataBus.updateTaskData(taskId, key, value)
    }

    fun completeTask(taskId: Long) {
        StrategyDataBus.completeTask(taskId)
    }

    fun removeUnit(unit: PlacedObject) {
        unitSystem.placedObjects.remove(unit)
        unitSystem.healthMap.remove(unit)
        if (unit == unitSystem.selectedUnit) {
            unitSystem.deselectUnit()
        }
    }

    fun getRenderSystem(): StrategyRenderSystem = StrategyRenderSystem
    fun getProgressCircleSystem(): ProgressCircleSystem = ProgressCircleSystem
    fun getScreenWidth(): Int = unitSystem.screenWidth
    fun getScreenHeight(): Int = unitSystem.screenHeight
}