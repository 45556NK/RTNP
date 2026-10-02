package com.rtnp.demo.core

import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.factory.FactorySlotConfig

/**
 * 物品/单位模板数据类（用于物品栏和建筑面板）
 * 使用 @JvmField 保证与现有 Java 代码的字段直接访问完全兼容
 */
class InventoryItem {

    @JvmField var type: String = ""
    @JvmField var name: String = ""
    @JvmField var useTexture: Boolean = false
    @JvmField var texturePath: String? = null

    @JvmField var shape: String = "rectangle"
    @JvmField var size: Int = 50
    @JvmField var width: Int = 90
    @JvmField var height: Int = 30

    @JvmField var health: Int = 1000
    @JvmField var speed: Int = 80
    @JvmField var acceleration: Float = 0f
    @JvmField var maxSpeed: Float = 0f
    @JvmField var deceleration: Float = 0f
    @JvmField var turnRate: Float = 0f
    @JvmField var faction: Int = 2
    @JvmField var category: String = ""
    @JvmField var color: Int = 0
    @JvmField var textureDisplayHeight: Float = 0f

    @JvmField var weaponType: String = "?"
    @JvmField var weaponActive: Boolean = true
    @JvmField var autoControl: Boolean = false
    @JvmField var damage: Float = 0f
    @JvmField var range: Float = 0f
    @JvmField var bulletSpeed: Float = 0f
    @JvmField var chargeTime: Float = 0f
    @JvmField var minDamage: Float = 0f
    @JvmField var maxDamage: Float = 0f

    @JvmField var missileTypeName: String = ""
    @JvmField var missileCooldownMax: Float = 1f
    @JvmField var missileMinRange: Int = 0

    @JvmField var weaponSlots: MutableList<PlacedObject.WeaponSlot> = mutableListOf()

    @JvmField var maxDockingSlots: Int = 0
    @JvmField var dockRadius: Float = 60f

    @JvmField var description: String = ""

    @JvmField var storedItemName: String? = null
    @JvmField var storedItemCount: Int = 0
    @JvmField var miningSpeed: Float = 0f
    @JvmField var storageCapacity: Int = 0
    @JvmField var miningEnabled: Boolean = true
    @JvmField var maxMiners: Int = 0

    @JvmField var costItem: String? = null
    @JvmField var costAmount: Int = 0

    @JvmField var refVolumeShape: String = ""
    @JvmField var refVolumeRadius: Float = 0f

    @JvmField var effectId: String = ""
    @JvmField var effectOffsetX: Float = 0f
    @JvmField var effectOffsetY: Float = 0f
    @JvmField var effectAngleOffset: Float = 180f
    @JvmField var effectTriggerSpeed: Float = 20f

    @JvmField var traitIds: MutableList<String> = mutableListOf()

    @JvmField var strategySlots: Int = 0
    @JvmField var strategyIds: MutableList<String> = mutableListOf()

    @JvmField var lifetime: Float = 0f

    @JvmField var allowWeaponChange: Boolean = true
    @JvmField var allowStrategyChange: Boolean = true

    @JvmField var physicsMass: Float? = null
    @JvmField var aiIds: MutableList<String> = mutableListOf()

    @JvmField var weaponSlotCount: Int = -1
    @JvmField var playerControllable: Boolean = true

    @JvmField var buildAmount: Float = 0f
    @JvmField var buildSpeed: Float = 0f
    @JvmField var buildDockSlots: Int = 0
    @JvmField var buildDockRadius: Float = 0f

    @JvmField var suppressionEffectId: String? = null

    // ★ 工厂槽配置（使用组合体模式）
    @JvmField var factorySlots: Int = 0
    @JvmField var factorySlotConfigs: MutableList<FactorySlotConfig> = mutableListOf()
    @JvmField var allowFactoryChange: Boolean = true
    
    // ★ 立场槽配置
    @JvmField var shieldSlots: Int = 0
    @JvmField var shieldSlotConfigs: MutableList<com.rtnp.demo.shield.ShieldSlotConfig> = mutableListOf()
    @JvmField var allowShieldChange: Boolean = true
}