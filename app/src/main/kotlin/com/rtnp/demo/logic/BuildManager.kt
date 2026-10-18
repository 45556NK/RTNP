// app/src/main/kotlin/com/rtnp/demo/logic/BuildManager.kt
package com.rtnp.demo.logic

import com.rtnp.demo.UnitSystem
import com.rtnp.demo.core.InventoryItem
import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.logger.Logger
import com.rtnp.demo.core.TempUnitData

object BuildManager {

    data class BuildProcess(
        val worldX: Float,
        val worldY: Float,
        val item: InventoryItem,
        var progress: Float = 0f,
        var invisibleUnit: PlacedObject? = null,
        var submittedMaterials: Int = 0,
        val submittedBuilders: MutableSet<Long> = mutableSetOf()
    )

    private val previewUnits = mutableListOf<TempUnitData>()
    private val buildProcesses = mutableListOf<BuildProcess>()

    const val BUILD_SPEED = 20f

    @JvmField
    var onCreateRealUnit: ((Float, Float, InventoryItem) -> PlacedObject?)? = null

    @JvmField
    var unitSystem: UnitSystem? = null

    fun addPreview(x: Float, y: Float, item: InventoryItem) {
        previewUnits.add(TempUnitData(x, y, item))
    }

    fun getPreviews(): List<TempUnitData> = previewUnits.toList()

    fun isEmpty(): Boolean = previewUnits.isEmpty() && buildProcesses.isEmpty()

    fun clearPreviews() {
        previewUnits.clear()
    }

    fun getBuildProcesses(): List<BuildProcess> = buildProcesses.toList()

    fun updateProcesses(deltaTime: Float) {
        val us = unitSystem ?: return
        val iterator = buildProcesses.iterator()
        while (iterator.hasNext()) {
            val process = iterator.next()
            val inv = process.invisibleUnit ?: continue
            if (!us.placedObjects.contains(inv)) {
                iterator.remove()
                continue
            }

            val item = process.item
            val costItem = item.costItem
            val costAmount = item.costAmount

            if (!costItem.isNullOrEmpty() && costAmount > 0) {
                for (unit in us.placedObjects) {
                    if (!unit.isDocked || unit.dockedAt != inv) continue
                    //if (unit.uniqueId in process.submittedBuilders) continue
                    val materials = unit.collectedItems ?: continue
                    val has = materials[costItem] ?: 0
                    if (has <= 0) continue
                    val needed = costAmount - process.submittedMaterials
                    if (needed <= 0) break
                    val take = minOf(has, needed)
                    materials[costItem] = has - take
                    process.submittedMaterials += take
                    process.submittedBuilders.add(unit.uniqueId)
                }
            }

            val materialRatio = if (!costItem.isNullOrEmpty() && costAmount > 0) {
                (process.submittedMaterials.toFloat() / costAmount).coerceIn(0f, 1f)
            } else {
                1f
            }
            val maxProgress = materialRatio * item.buildAmount

            if (process.progress < maxProgress) {
                var totalBuildSpeed = 0f
                for (unit in us.placedObjects) {
                    if (unit.isDocked && unit.dockedAt == inv && unit.buildSpeed > 0f) {
                        totalBuildSpeed += unit.buildSpeed
                    }
                }
                if (totalBuildSpeed > 0f) {
                    process.progress += totalBuildSpeed * deltaTime
                    if (process.progress > maxProgress) {
                        process.progress = maxProgress
                    }
                }
            }

            syncInvisibleHealth(process, inv)

            if (process.progress >= item.buildAmount) {
                val builders = us.placedObjects.filter {
                    it.isDocked && it.dockedAt == inv && it.buildSpeed > 0f
                }
                spawnRealUnitAndTransferBuilders(process, builders)
                iterator.remove()
            }
        }
    }

    fun damageBuildProcess(inv: PlacedObject, damage: Float) {
        val index = buildProcesses.indexOfFirst { it.invisibleUnit == inv }
        if (index < 0) return
        val process = buildProcesses[index]
        process.progress -= damage
        if (process.progress < 0f) process.progress = 0f

        syncInvisibleHealth(process, inv)

        if (process.progress <= 0f) {
            removeInvisibleUnit(inv)
            buildProcesses.removeAt(index)
        }
    }

    private fun syncInvisibleHealth(process: BuildProcess, inv: PlacedObject) {
        inv.originalHealth = process.item.buildAmount.toInt().coerceAtLeast(1)
        inv.health = process.progress.toInt().coerceAtLeast(1)
        unitSystem?.healthMap?.put(inv, inv.health.toFloat())
    }

    fun confirmBuild() {
        val us = unitSystem ?: run {
            previewUnits.clear()
            return
        }
        for (preview in previewUnits) {
            if (preview.item.buildAmount <= 0f) {
                onCreateRealUnit?.invoke(preview.worldX, preview.worldY, preview.item)
            } else {
                val inv = createInvisibleUnit(preview.worldX, preview.worldY, preview.item) ?: continue
                val process = BuildProcess(preview.worldX, preview.worldY, preview.item, 0f, inv, 0)
                buildProcesses.add(process)
            }
        }
        previewUnits.clear()
    }

    fun cancelBuild() {
        previewUnits.clear()
    }

    fun cancelAllProcesses() {
        for (process in buildProcesses) {
            removeInvisibleUnit(process.invisibleUnit)
        }
        buildProcesses.clear()
    }

    private fun createInvisibleUnit(worldX: Float, worldY: Float, item: InventoryItem): PlacedObject? {
        val us = unitSystem ?: return null
        val inv = PlacedObject()
        inv.worldX = worldX
        inv.worldY = worldY
        inv.shape = item.shape
        inv.size = item.size
        inv.width = item.width
        inv.height = item.height
        inv.name = item.name + "_建造中"
        inv.type = "base"
        inv.category = "建造中单位"
        inv.faction = item.faction.takeIf { it != 0 } ?: 2
        inv.color = item.color
        inv.autoControl = true
        inv.canMove = false
        inv.playerControllable = false
        inv.buildDockSlots = item.buildDockSlots
        inv.buildDockRadius = item.buildDockRadius
        inv.maxDockingSlots = item.maxDockingSlots
        inv.dockRadius = item.dockRadius
        // 不再设置 port

        inv.originalHealth = item.buildAmount.toInt().coerceAtLeast(1)
        inv.health = inv.originalHealth

        inv.referenceVolume = if (item.refVolumeShape.isNotEmpty()) {
            PlacedObject.ReferenceVolume().apply {
                shape = item.refVolumeShape
                radius = item.refVolumeRadius
            }
        } else null

        inv.displayName = inv.name
        inv.uniqueId = PlacedObject.generateUniqueId()

        us.placedObjects.add(inv)
        us.healthMap[inv] = inv.health.toFloat()
        HexGridManager.registerUnit(inv)
        us.getPhysicsWorld()?.addUnit(inv)

        return inv
    }

    private fun removeInvisibleUnit(inv: PlacedObject?) {
        if (inv == null) return
        val us = unitSystem ?: return
        if (us.selectedUnit == inv) {
            us.deselectUnit()
            com.rtnp.demo.ui.panel.RightPanel.unlock()
        }
        us.placedObjects.remove(inv)
        us.healthMap.remove(inv)
        us.unitsWithLifetime.remove(inv)
        HexGridManager.unregisterUnit(inv)
        us.getPhysicsWorld()?.removeUnit(inv)
    }

    private fun spawnRealUnit(process: BuildProcess) {
        val us = unitSystem ?: return
        val realUnit = onCreateRealUnit?.invoke(process.worldX, process.worldY, process.item)
        removeInvisibleUnit(process.invisibleUnit)
        if (realUnit != null) {
            transferDockedBuilders(process.invisibleUnit, realUnit)
        }
    }

    private fun spawnRealUnitAndTransferBuilders(process: BuildProcess, builders: List<PlacedObject>) {
        val us = unitSystem ?: return
        val realUnit = onCreateRealUnit?.invoke(process.worldX, process.worldY, process.item)
        if (realUnit != null) {
            for (builder in builders) {
                transferBuilder(builder, realUnit)
            }
        }
        removeInvisibleUnit(process.invisibleUnit)
    }

    private fun transferDockedBuilders(inv: PlacedObject?, realUnit: PlacedObject) {
        if (inv == null) return
        val us = unitSystem ?: return
        for (unit in us.placedObjects.toList()) {
            if (unit.isDocked && unit.dockedAt == inv && unit.buildSpeed > 0f) {
                transferBuilder(unit, realUnit)
            }
        }
    }

    private fun transferBuilder(builder: PlacedObject, realUnit: PlacedObject) {
        val us = unitSystem ?: return
        if (!us.placedObjects.contains(builder)) return
        DockingSystem.releaseSlot(builder)
        builder.dockedAt = null
        builder.isDocked = false
        builder.isUndocking = false
        builder.actionDock = false
        builder.dockTarget = null
        DockingSystem.requestDock(builder, realUnit)
    }
}