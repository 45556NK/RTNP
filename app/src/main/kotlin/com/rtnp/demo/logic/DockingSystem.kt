package com.rtnp.demo.logic

import com.rtnp.demo.core.PlacedObject
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object DockingSystem {

    const val DEFAULT_SLOT_COUNT = 6
    const val DEFAULT_DOCK_RADIUS = 60f
    private const val ARRIVAL_THRESHOLD = 2f

    fun getSlotCount(target: PlacedObject): Int {
        if (target.category == "建造中单位" && target.buildDockSlots > 0) {
            return target.buildDockSlots
        }
        if (target.maxDockingSlots > 0) return target.maxDockingSlots
        return 0
    }

    fun getDockRadius(target: PlacedObject): Float {
        if (target.category == "建造中单位" && target.buildDockRadius > 0f) {
            return target.buildDockRadius
        }
        if (target.dockRadius > 0) return target.dockRadius
        return DEFAULT_DOCK_RADIUS
    }

    @JvmOverloads
    fun requestDock(unit: PlacedObject, target: PlacedObject, preferredSlot: Int? = null): Boolean {
        if (unit === target) return false
        if (unit.isDocked || unit.isMoving || unit.isStopping) return false

        val totalSlots = getSlotCount(target)
        if (totalSlots <= 0) return false

        if (target.dockingList.any { it.unitId == unit.uniqueId }) return true
        if (target.dockedList.any { it.unitId == unit.uniqueId }) return true

        val occupiedSlots = mutableSetOf<Int>()
        for (entry in target.dockedList) { occupiedSlots.add(entry.slotIndex) }
        for (entry in target.dockingList) { occupiedSlots.add(entry.slotIndex) }

        val freeIndex: Int? = if (preferredSlot != null && preferredSlot in 0 until totalSlots) {
            if (preferredSlot !in occupiedSlots) preferredSlot else null
        } else {
            (0 until totalSlots).firstOrNull { it !in occupiedSlots }
        }

        if (freeIndex == null) return false

        target.dockingList.add(PlacedObject.DockEntry(unit.uniqueId, freeIndex))

        val sectors = getDockingSectors(target)
        val sector = sectors.firstOrNull { it.index == freeIndex }

        unit.dockTarget = target
        unit.dockSlotIndex = freeIndex
        unit.targetX = sector?.centerX ?: target.worldX
        unit.targetY = sector?.centerY ?: target.worldY
        unit.actionDock = true
        unit.isMoving = true
        unit.isStopping = false
        unit.currentSpeed = 0f
        unit.dockOffsetX = 0f
        unit.dockOffsetY = 0f

        return true
    }

    fun getDockingSectors(target: PlacedObject): List<DockSector> {
        val slots = getSlotCount(target)
        if (slots <= 0) return emptyList()
        val radius = getDockRadius(target)
        val angleStep = 360f / slots
        val sectors = mutableListOf<DockSector>()

        val occupiedSlots = mutableSetOf<Int>()
        for (entry in target.dockedList) { occupiedSlots.add(entry.slotIndex) }
        for (entry in target.dockingList) { occupiedSlots.add(entry.slotIndex) }

        for (i in 0 until slots) {
            val startAngle = target.heading + angleStep * i
            val endAngle = startAngle + angleStep
            val midAngle = startAngle + angleStep / 2f

            val midRad = Math.toRadians(midAngle.toDouble())
            val slotX = target.worldX + radius * Math.cos(midRad).toFloat()
            val slotY = target.worldY + radius * Math.sin(midRad).toFloat()

            sectors.add(DockSector(i, startAngle, endAngle, slotX, slotY, target, i in occupiedSlots))
        }
        return sectors
    }

    data class DockSector(
        val index: Int,
        val startAngle: Float,
        val endAngle: Float,
        val centerX: Float,
        val centerY: Float,
        val target: PlacedObject,
        val isOccupied: Boolean
    )

    fun checkArrival(unit: PlacedObject, allUnits: List<PlacedObject>) {
        val target = unit.dockTarget ?: return
        if (!unit.actionDock) return
        if (unit.isDocked) return

        val dx = unit.worldX - unit.targetX
        val dy = unit.worldY - unit.targetY
        val distToSlot = sqrt(dx * dx + dy * dy)

        if (distToSlot > ARRIVAL_THRESHOLD) return

        val entry = target.dockingList.firstOrNull { it.unitId == unit.uniqueId } ?: return
        target.dockingList.remove(entry)
        target.dockedList.add(PlacedObject.DockEntry(unit.uniqueId, entry.slotIndex))

        unit.isMoving = false
        unit.isStopping = false
        unit.currentSpeed = 0f
        unit.isDocked = true
        unit.dockedAt = target
        unit.dockTarget = null
        unit.actionDock = false

        if (target.type == "environment" && target.maxMiners > 0 && unit.miningSpeed > 0) {
            unit.miningTarget = target
            MiningSystem.forceStartMining(unit, allUnits)
        }
    }

    fun releaseSlot(unit: PlacedObject) {
        val target = unit.dockedAt ?: return

        val dockedEntry = target.dockedList.find { it.unitId == unit.uniqueId }
        val dockingEntry = target.dockingList.find { it.unitId == unit.uniqueId }

        if (dockedEntry == null && dockingEntry == null) return

        target.dockedList.removeAll { it.unitId == unit.uniqueId }
        target.dockingList.removeAll { it.unitId == unit.uniqueId }

        unit.dockSlotIndex = -1
        unit.dockedAt = null
        unit.isDocked = false
    }

    fun cleanDeadFromLists(allUnits: List<PlacedObject>) {
        // ★ 一次构建 id → 单位映射
        val idToUnit = HashMap<Long, PlacedObject>(allUnits.size)
        for (u in allUnits) idToUnit[u.uniqueId] = u

        for (unit in allUnits) {
            if (getSlotCount(unit) <= 0) continue

            // ★ 内联 isUnitCancelledOrStopped，用 map O(1) 查找
            unit.dockingList.removeAll { entry ->
                val u = idToUnit[entry.unitId] ?: return@removeAll true  // 已死
                !u.actionDock || (u.isStopping && !u.isMoving)
            }
            unit.dockedList.removeAll { it.unitId !in idToUnit }
        }
    }

    private fun isUnitCancelledOrStopped(unitId: Long, allUnits: List<PlacedObject>): Boolean {
        val unit = allUnits.firstOrNull { it.uniqueId == unitId } ?: return true
        return !unit.actionDock || (unit.isStopping && !unit.isMoving)
    }

    fun findDockTarget(mover: PlacedObject, wx: Float, wy: Float, allUnits: List<PlacedObject>): PlacedObject? {
        for (obj in allUnits) {
            if (obj === mover) continue
            if (obj.isDocked) continue
            if (getSlotCount(obj) <= 0) continue

            val radius = getDockRadius(obj)
            val dx = wx - obj.worldX
            val dy = wy - obj.worldY
            if (dx * dx + dy * dy <= radius * radius) {
                return obj
            }
        }
        return null
    }
}