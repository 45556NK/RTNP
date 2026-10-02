package com.rtnp.demo.core

import android.graphics.Color
import com.rtnp.demo.ui.render.button.ProgressBarTool
import com.rtnp.demo.combat.PulseWeapon
import com.rtnp.demo.factory.FactorySlotConfig

class PlacedObject {

    @JvmField var worldX: Float = 0f
    @JvmField var worldY: Float = 0f
    @JvmField var shape: String = "rectangle"
    @JvmField var size: Int = 50
    @JvmField var width: Int = 90
    @JvmField var height: Int = 30
    @JvmField var health: Int = 1000
    @JvmField var originalHealth: Int = 1000
    @JvmField var name: String = ""
    @JvmField var type: String = "unit"
    @JvmField var targetX: Float = 0f
    @JvmField var targetY: Float = 0f
    @JvmField var isMoving: Boolean = false
    @JvmField var speed: Int = 80
    @JvmField var acceleration: Float = 20f
    @JvmField var maxSpeed: Float = 160f
    @JvmField var currentSpeed: Float = 0f
    @JvmField var deceleration: Float = 40f
    @JvmField var turnRate: Float = 180f
    @JvmField var heading: Float = -90f
    @JvmField var canMove: Boolean = true
    @JvmField var faction: Int = 2
    @JvmField var category: String = ""
    @JvmField var color: Int = Color.WHITE
    @JvmField var textureDisplayHeight: Float = 0f
    @JvmField var textureBitmap: android.graphics.Bitmap? = null
    @JvmField var isRotating: Boolean = false
    @JvmField var playerControllable: Boolean = true

    @JvmField var previewTargetX: Float = 0f
    @JvmField var previewTargetY: Float = 0f
    @JvmField var hasPreviewTarget: Boolean = false

    @JvmField var weaponSlots: MutableList<WeaponSlot> = mutableListOf()
    @JvmField var currentWeaponIndex: Int = 0
    @JvmField var currentTarget: PlacedObject? = null

    @JvmField var weaponType: String = "?"
    @JvmField var weaponActive: Boolean = true
    @JvmField var autoControl: Boolean = false
    @JvmField var damage: Float = 0f
    @JvmField var range: Float = 0f
    @JvmField var bulletSpeed: Float = 0f
    @JvmField var chargeTime: Float = 0f
    @JvmField var minDamage: Float = 0f
    @JvmField var maxDamage: Float = 0f

    @JvmField var laserTarget: PlacedObject? = null
    @JvmField var laserCharge: Float = 0f

    @JvmField var missileTypeName: String = ""
    @JvmField var missileCooldown: Float = 0f
    @JvmField var missileCooldownMax: Float = 1f
    @JvmField var missileMinRange: Int = 0

    @JvmField var missileStartSpeed: Int = 0
    @JvmField var missileMaxSpeed: Int = 0
    @JvmField var missileAcceleration: Float = 0f
    @JvmField var missileHealth: Int = 0
    @JvmField var missileDamage: Int = 0
    @JvmField var missileLifetime: Int = 0
    @JvmField var explosionRange: Int = 0
    @JvmField var effectSize: Int = 0
    @JvmField var missileCurrentSpeed: Float = 0f
    @JvmField var lifetime: Float = 0f
    @JvmField var hasSuppression: Boolean = false
    @JvmField var suppressionRange: Int = 0
    @JvmField var suppressionDamage: Int = 0
    @JvmField var suppressionDuration: Float = 0f
    @JvmField var suppressionEffectId: String? = null

    @JvmField var explosionRangeDisplayId: Long = -1L
    @JvmField var suppressionRangeDisplayId: Long = -1L

    @JvmField var explosionEffectId: String = "explosion"

    @JvmField var dockedAt: PlacedObject? = null
    @JvmField var dockSlotIndex: Int = -1
    @JvmField var isDocked: Boolean = false
    @JvmField var isUndocking: Boolean = false
    @JvmField var undockProgress: Float = 0f
    @JvmField var dockingTarget: PlacedObject? = null
    @JvmField var dockTarget: PlacedObject? = null
    @JvmField var dockOffsetX: Float = 0f
    @JvmField var dockOffsetY: Float = 0f
    @JvmField var description: String = ""
    @JvmField var isStopping: Boolean = false
    @JvmField var storedItemName: String = ""
    @JvmField var storedItemCount: Int = 0
    @JvmField var dockedUnits: MutableList<PlacedObject> = mutableListOf()
    @JvmField var actionDock: Boolean = false

    @JvmField var dockingSlots: MutableList<PlacedObject?>? = null
    @JvmField var maxDockingSlots: Int = 0
    @JvmField var dockRadius: Float = 60f

    @JvmField var dockedList: MutableList<DockEntry> = mutableListOf()
    @JvmField var dockingList: MutableList<DockEntry> = mutableListOf()

    class DockEntry {
        @JvmField var unitId: Long = 0L
        @JvmField var slotIndex: Int = -1

        constructor()
        constructor(unitId: Long, slotIndex: Int) {
            this.unitId = unitId
            this.slotIndex = slotIndex
        }
    }

    @JvmField var displayName: String? = null
    @JvmField var isSelected: Boolean = false
    @JvmField var autoLaunchTimer: Float = -1f

    @JvmField var miningSpeed: Float = 0f
    @JvmField var storageCapacity: Int = 0
    @JvmField var collectedItems: MutableMap<String, Int>? = null
    @JvmField var miningEnabled: Boolean = true
    @JvmField var miningTarget: PlacedObject? = null
    @JvmField var isMining: Boolean = false

    @JvmField var maxMiners: Int = 0

    @JvmField var costItem: String? = null
    @JvmField var costAmount: Int = 0
    @JvmField var buildSpeed: Float = 0f

    @JvmField var referenceVolume: ReferenceVolume? = null

    @JvmField var effectId: String = ""
    @JvmField var effectOffsetX: Float = 0f
    @JvmField var effectOffsetY: Float = 0f
    @JvmField var effectAngleOffset: Float = 180f
    @JvmField var effectTriggerSpeed: Float = 20f

    @JvmField var nextTargetSearchTime: Long = 0L

    @JvmField var traitIds: MutableList<String> = mutableListOf()
    @JvmField var strategyIds: MutableList<String> = mutableListOf()
    @JvmField var strategySlots: Int = 0

    @JvmField var maxLifetime: Float = 0f
    @JvmField var buildDockSlots: Int = 0
    @JvmField var buildDockRadius: Float = 0f

    @JvmField var allowWeaponChange: Boolean = true
    @JvmField var allowStrategyChange: Boolean = true
    @JvmField var strategyCooldownBars: MutableList<ProgressBarTool?> = mutableListOf()
    @JvmField var physicsMass: Float? = null

    @JvmField var aiIds: MutableList<String> = mutableListOf()
    @JvmField var missileLauncher: PlacedObject? = null

    @JvmField var uniqueId: Long = 0L

    // ★ 工厂槽字段（使用组合体模式）
    @JvmField var factorySlots: Int = 0
    @JvmField var factorySlotConfigs: MutableList<FactorySlotConfig> = mutableListOf()
    @JvmField var factoryCooldownBars: MutableList<ProgressBarTool?> = mutableListOf()
    @JvmField var allowFactoryChange: Boolean = true
    
    // ★ 立场槽字段
    @JvmField var shieldSlots: Int = 0
    @JvmField var shieldSlotConfigs: MutableList<com.rtnp.demo.shield.ShieldSlotConfig> = mutableListOf()
    @JvmField var allowShieldChange: Boolean = true
    
    
    @JvmField var dynamicPanelComponents: MutableList<com.rtnp.demo.ui.panel.data.DynamicProgressBarComponent> = mutableListOf()

    companion object {
        private var nextId: Long = 1L
        @JvmStatic
        fun generateUniqueId(): Long = nextId++
    }

    fun getOrCreateFactoryCooldownBar(slotIndex: Int, cooldownTime: Float): ProgressBarTool {
        while (factoryCooldownBars.size <= slotIndex) {
            factoryCooldownBars.add(null)
        }
        val existing = factoryCooldownBars[slotIndex]
        if (existing != null) return existing
        val newBar = ProgressBarTool(totalDuration = cooldownTime.coerceAtLeast(0.01f))
        newBar.autoAdvance = false
        factoryCooldownBars[slotIndex] = newBar
        return newBar
    }

    fun getOrCreateStrategyCooldownBar(slotIndex: Int, cooldownTime: Float): ProgressBarTool {
        while (strategyCooldownBars.size <= slotIndex) {
            strategyCooldownBars.add(null)
        }
        val existing = strategyCooldownBars[slotIndex]
        if (existing != null) return existing
        val newBar = ProgressBarTool(totalDuration = cooldownTime.coerceAtLeast(0.01f))
        newBar.autoAdvance = false
        strategyCooldownBars[slotIndex] = newBar
        return newBar
    }

    class ReferenceVolume {
        @JvmField var shape: String = "circle"
        @JvmField var radius: Float = 0f
    }

    fun syncCurrentWeapon() {
        if (weaponSlots.isEmpty()) {
            weaponType = "?"
            weaponActive = false
            damage = 0f
            range = 0f
            bulletSpeed = 0f
            chargeTime = 0f
            minDamage = 0f
            maxDamage = 0f
            return
        }
        for (slot in weaponSlots) {
            if (slot.active) {
                weaponType = slot.type
                weaponActive = true
                range = slot.range
                damage = slot.damage
                bulletSpeed = slot.bulletSpeed
                chargeTime = slot.chargeTime
                minDamage = slot.minDamage
                maxDamage = slot.maxDamage
                return
            }
        }
        weaponType = "?"
        weaponActive = false
        damage = 0f
        range = 0f
        bulletSpeed = 0f
        chargeTime = 0f
        minDamage = 0f
        maxDamage = 0f
    }

    class WeaponSlot {
        @JvmField var type: String = "?"
        @JvmField var range: Float = 0f
        @JvmField var damage: Float = 0f
        @JvmField var bulletSpeed: Float = 0f
        @JvmField var chargeTime: Float = 0f
        @JvmField var minDamage: Float = 0f
        @JvmField var maxDamage: Float = 0f
        @JvmField var cooldown: Float = 0f
        @JvmField var cooldownRemaining: Float = 0f
        @JvmField var laserCharge: Float = 0f
        @JvmField var laserTarget: PlacedObject? = null
        @JvmField var active: Boolean = true
        @JvmField var templateId: String? = null
        @JvmField var minRange: Float = 0f
        @JvmField var nextSearchTime: Long = 0L
        @JvmField var lockedTarget: PlacedObject? = null
        @JvmField var aimAngleOffset: Float = 0f
        @JvmField var cooldownBar: ProgressBarTool? = null
        @JvmField var pulseWeapon: PulseWeapon? = null
        @JvmField var maxTargets: Int = 1
        @JvmField var energyDuration: Float = 5f
        @JvmField var energyRegenDelay: Float = 6f
        @JvmField var energyRegenSpeed: Float = 0.05f
        @JvmField var missileTypeName: String = ""

        @JvmField var traitIds: MutableList<String> = mutableListOf()
        @JvmField var filterWhitelist: MutableMap<String, MutableList<String>> = mutableMapOf()

        fun getOrCreateCooldownBar(): ProgressBarTool {
            val existing = cooldownBar
            if (existing != null) return existing
            val newBar = ProgressBarTool(totalDuration = cooldown.coerceAtLeast(0.01f))
            newBar.autoAdvance = false
            cooldownBar = newBar
            return newBar
        }

        fun copy(): WeaponSlot {
            return WeaponSlot().apply {
                type = this@WeaponSlot.type
                range = this@WeaponSlot.range
                damage = this@WeaponSlot.damage
                bulletSpeed = this@WeaponSlot.bulletSpeed
                chargeTime = this@WeaponSlot.chargeTime
                minDamage = this@WeaponSlot.minDamage
                maxDamage = this@WeaponSlot.maxDamage
                cooldown = this@WeaponSlot.cooldown
                cooldownRemaining = this@WeaponSlot.cooldown
                laserCharge = 0f
                laserTarget = null
                active = this@WeaponSlot.active
                templateId = this@WeaponSlot.templateId
                minRange = this@WeaponSlot.minRange
                lockedTarget = null
                nextSearchTime = 0L
                aimAngleOffset = 0f
                maxTargets = this@WeaponSlot.maxTargets
                energyDuration = this@WeaponSlot.energyDuration
                energyRegenDelay = this@WeaponSlot.energyRegenDelay
                energyRegenSpeed = this@WeaponSlot.energyRegenSpeed
                traitIds = this@WeaponSlot.traitIds.toMutableList()
                missileTypeName = this@WeaponSlot.missileTypeName
                this@WeaponSlot.filterWhitelist.forEach { (k, v) ->
                    filterWhitelist[k] = v.toMutableList()
                }
            }
        }
    }
}