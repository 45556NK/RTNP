// app/src/main/kotlin/com/rtnp/demo/data/units/B1CarrierRocketTemplate.kt
package com.rtnp.demo.data.units

import android.graphics.Color
import com.rtnp.demo.core.InventoryItem
import com.rtnp.demo.core.UnitTemplateProvider

class B1CarrierRocketTemplate : UnitTemplateProvider {
    override fun provide(): InventoryItem {
        return InventoryItem().apply {
            type = "unit"
            name = "B1运载火箭"
            shape = "rectangle"
            width = 60
            height = 20
            health = 1000
            faction = 2
            category = "火箭"
            color = Color.GREEN
            description = "重型运载火箭，部署了特殊装备"
            allowWeaponChange = false
            allowStrategyChange = false
            playerControllable = false
            lifetime = 120f
            weaponSlotCount = 0
            strategySlots = 0
            maxDockingSlots = 0
            dockRadius = 0f
            speed = 80
            acceleration = 20f
            maxSpeed = 160f
            traitIds.add("carrier")
            refVolumeShape = "circle"
            refVolumeRadius = 40f  // 根据需要设置
        }
    }
}