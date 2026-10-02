package com.rtnp.demo.data.units

import android.graphics.Color
import com.rtnp.demo.core.InventoryItem
import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.core.UnitTemplateProvider
import com.rtnp.demo.shield.ShieldSlotConfig

class TestTextureTemplate : UnitTemplateProvider {
    override fun provide(): InventoryItem {
        return InventoryItem().apply {
            type = "unit"
            name = "测试贴图"
            shape = "rectangle"
            width = 40
            height = 80
            textureDisplayHeight = 170f
            health = 9000
            speed = 80
            maxSpeed = 160f
            acceleration = 25f
            deceleration = 40f
            turnRate = 180f
            faction = 2
            category = ""
            color = Color.WHITE
            description = "标准战斗舰船，装备轨道炮。"
            autoControl = false
            maxDockingSlots = 0
            dockRadius = 80f
            useTexture = true
            texturePath = "images/units/pg.png"
            costItem = "金属"
            costAmount = 10
            storedItemName = null
            storedItemCount = 0
            miningSpeed = 0f
            storageCapacity = 0
            maxMiners = 0
            strategySlots = 2
            strategyIds = mutableListOf()
            traitIds = mutableListOf("predictive_aim")
            refVolumeShape = "circle"
            refVolumeRadius = 50f

            weaponSlotCount = 1
            weaponSlots = mutableListOf(
                PlacedObject.WeaponSlot().apply {
                    templateId = "railgun_a2"
                }
            )

            effectId = "tail_flame_unit"
            effectOffsetX = -84f
            effectOffsetY = 0f
            effectAngleOffset = 270f
            effectTriggerSpeed = 20f

            // ★ 立场槽：初代立场
            shieldSlots = 2
            shieldSlotConfigs.add(ShieldSlotConfig().apply {
                shieldId = "basic_shield"
            })
        }
    }
}