// app/src/main/kotlin/com/rtnp/demo/logic/DeathManager.kt
package com.rtnp.demo.logic

import com.rtnp.demo.UnitSystem
import com.rtnp.demo.core.PlacedObject

class DeathManager(private val host: UnitSystem) {

    fun removeDeadUnits() {
        val deadUnits = mutableListOf<PlacedObject>()

        for (obj in host.placedObjects) {
            if (obj.category == "missile") continue

            val hp = host.healthMap[obj]
            if (hp != null && hp <= 0) {
                deadUnits.add(obj)
            }
        }

        for (obj in deadUnits) {
            handleDeath(obj)
        }
    }

    fun updateLifetimes(deltaTime: Float) {
        val expiredUnits = mutableListOf<PlacedObject>()
        val snapshot = ArrayList(host.unitsWithLifetime)

        for (unit in snapshot) {
            if (unit.category == "missile") {
                host.unitsWithLifetime.remove(unit)
                continue
            }
            if (!host.placedObjects.contains(unit)) {
                host.unitsWithLifetime.remove(unit)
                continue
            }
            unit.lifetime += deltaTime
            if (unit.lifetime >= unit.maxLifetime) {
                expiredUnits.add(unit)
            }
        }

        for (obj in expiredUnits) {
            handleDeath(obj)
        }
    }

    fun handleDeath(obj: PlacedObject) {
        if (obj == host.selectedUnit) {
            com.rtnp.demo.ui.panel.RightPanel.unlock()
        }
        // ★ 安全销毁立场
        com.rtnp.demo.shield.ShieldManager.destroyAllShields(obj)

        // 释放停泊在此单位的船只（不再依赖 port）
        for (s in ArrayList(host.placedObjects)) {
            if (s.dockedAt == obj) {
                DockingSystem.releaseSlot(s)
                s.dockedAt = null
                s.isDocked = false
                s.isUndocking = false
            }
        }

        if (obj == host.selectedUnit) {
            host.deselectUnit()
        }

        host.multiSelectManager?.removeUnit(obj)
        

        host.placedObjects.remove(obj)
        host.healthMap.remove(obj)
        host.unitsWithLifetime.remove(obj)
    }
}