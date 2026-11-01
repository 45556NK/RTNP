package com.rtnp.demo.core

import android.content.Context
import android.graphics.Color
import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.movement.ModuleManager
import com.rtnp.demo.shield.ShieldSlotConfig
import com.rtnp.demo.ui.InventoryPanel
import com.rtnp.demo.ai.AiManager
import com.rtnp.demo.factory.FactorySlotConfig
import java.util.*

object TemplateRegistry {

    val templates = mutableListOf<InventoryItem>()

    fun init(context: Context) {
        // 标准导弹类型（只需注册一次）
        if (MissileTypeRegistry.get("标准导弹") == null) {
            MissileTypeRegistry.register(MissileType().apply {
                name = "标准导弹"
                health = 250
                damage = 3200
                startSpeed = 10
                maxSpeed = 400
                acceleration = 30f
                lifetime = 12
                explosionRange = 200
                effectSize = 80
                cooldownMax = 8
                minRange = 180
                refVolumeShape = "circle"
                refVolumeRadius = 20f
            })
        }

        // 硬编码模板（兼容旧版）
        registerHardcodedTemplates()

        // ★ 显式注册普通单位模板（不再使用 DexFile 扫描）
        UnitTemplateRegistration.registerNormalUnitTemplates(this)

        // ★ 显式注册导弹类型模板（不再使用 DexFile 扫描）
        MissileTemplateRegistration.registerMissileTemplates()

        // 统一注册所有模板到 InventoryPanel
        for (t in templates) {
            InventoryPanel.registerTemplate(t)
        }

        // 兼容旧版：保留硬编码的建造列表项
        BuildListRegistry.addBuildId("测试我方基地")
    }

    private fun registerHardcodedTemplates() {
        addTemplate(InventoryItem().apply {
            type = "environment"; name = "测试环境"
            shape = "circle"; size = 60; health = 80000
            faction = 0; category = "背景"; color = Color.GREEN
            autoControl = true; description = "一个无害的环境装饰，可放置在地图上。"
            maxDockingSlots = 12
            dockRadius = 80f
            lifetime = 300f
            physicsMass = 20f
            refVolumeShape = "circle"
            refVolumeRadius = 60f  // 根据需要设置
        })

        addTemplate(InventoryItem().apply {
            type = "environment"; name = "测试矿物"
            shape = "circle"; size = 60; health = 50000
            faction = 0; category = "道具"; color = Color.YELLOW
            autoControl = true; storedItemName = "金属"; storedItemCount = 800
            maxMiners = 3; description = "一个储存金属的矿物，可被工程船采集。"
            maxDockingSlots = 12
            dockRadius = 80f
            physicsMass = 30f
            refVolumeShape = "circle"
            refVolumeRadius = 60f  // 根据需要设置
        })

        addTemplate(InventoryItem().apply {
            type = "unit"; name = "测试工程"
            shape = "rectangle"; width = 90; height = 30; health = 6000
            speed = 80; acceleration = 20f; maxSpeed = 160f; deceleration = 40f
            turnRate = 180f; faction = 2; category = "工程"; color = Color.CYAN
            autoControl = false; storageCapacity = 1200; miningSpeed = 4f
            miningEnabled = true; description = "工程船，可采集矿物，无默认武器但可改装。"
            costItem = "金属"; costAmount = 3
            dockRadius = 80f
            strategySlots = 1
            physicsMass = 5f
            buildSpeed = 100f
            refVolumeShape = "circle"
            refVolumeRadius = 50f  // 根据需要设置
            weaponSlots.add(PlacedObject.WeaponSlot().apply {
                type = "?"; active = false; templateId = null
            })
        })

        addTemplate(InventoryItem().apply {
            type = "unit"; name = "测试单位"
            shape = "rectangle"; width = 90; height = 30; health = 8000
            speed = 80; acceleration = 20f; maxSpeed = 160f; deceleration = 40f
            turnRate = 180f; faction = 2; category = ""; color = Color.WHITE
            autoControl = false; description = "标准战斗舰船，装备轨道炮。"
            costItem = "金属"; costAmount = 10
            weaponSlots.add(ModuleManager.createSlotFromTemplate("railgun_a2") ?: PlacedObject.WeaponSlot())
            effectId = "tail_flame_unit"
            effectOffsetX = -40f; effectOffsetY = 0f
            effectAngleOffset = 270f; effectTriggerSpeed = 20f
            dockRadius = 80f
            strategySlots = 1
            buildAmount = 300f
            physicsMass = 6f
            refVolumeShape = "circle"
            refVolumeRadius = 50f  // 根据需要设置
            strategyIds.add("focus_fire")
        })

        addTemplate(InventoryItem().apply {
            type = "unit"; name = "测试敌方单位"
            shape = "rectangle"; width = 90; height = 30; health = 8000
            speed = 60; acceleration = 15f; maxSpeed = 140f; deceleration = 30f
            turnRate = 160f; faction = 1; category = ""; color = Color.RED
            autoControl = false; description = "敌方激光舰船，伤害随充能提升。"
            costItem = "金属"; costAmount = 8
            dockRadius = 80f
            strategySlots = 1
            physicsMass = 6f
            refVolumeShape = "circle"
            refVolumeRadius = 50f  // 根据需要设置
            shieldSlots = 1
            shieldSlotConfigs.add(ShieldSlotConfig().apply {
                shieldId = "anti_explosion_shield"
            })
            aiIds.add("zone_defense")
            weaponSlots.add(ModuleManager.createSlotFromTemplate("laser_default") ?: PlacedObject.WeaponSlot())
        })

        addTemplate(InventoryItem().apply {
            type = "base"; name = "测试敌对"
            shape = "circle"; size = 60; health = 30000
            faction = 1; category = ""; color = Color.RED
            autoControl = true; description = "敌方基地，具有远程轨道炮。"
            costItem = "金属"; costAmount = 15
            maxDockingSlots = 12
            dockRadius = 80f
            physicsMass = 20f
            refVolumeShape = "circle"
            refVolumeRadius = 80f  // 根据需要设置
            weaponSlots.add(ModuleManager.createSlotFromTemplate("railgun_b1") ?: PlacedObject.WeaponSlot())
        })

        addTemplate(InventoryItem().apply {
            type = "base"; name = "测试我方基地"
            shape = "circle"; size = 60; health = 30000
            faction = 2; category = "港口"; color = Color.BLUE
            autoControl = true
            description = "我方港口基地。"
            costItem = "金属"; costAmount = 30 ;strategySlots = 1
            storedItemName = "金属"; storedItemCount = 200; storageCapacity = 1500 ;physicsMass = 20f
            weaponSlots.add(ModuleManager.createSlotFromTemplate("railgun_b2") ?: PlacedObject.WeaponSlot())
            buildAmount = 2000f
            refVolumeRadius = 40f
            buildDockSlots = 3
            maxDockingSlots = 12
            dockRadius = 100f
            refVolumeShape = "circle"
            refVolumeRadius = 80f  // 根据需要设置
            buildDockRadius = 90f
            factorySlots = 3
            factorySlotConfigs.add(FactorySlotConfig().apply {
                factoryId = "repair_factory"
            })
            factorySlotConfigs.add(FactorySlotConfig().apply {
                factoryId = "shipyard"
                // 可选覆盖参数
                params.put("productionRate", 150f)
            })
            factorySlotConfigs.add(FactorySlotConfig().apply {
                factoryId = "load_factory"
            })
        })

        addTemplate(InventoryItem().apply {
            type = "unit"; name = "测试我方导弹"
            shape = "square"; size = 50; health = 10000
            speed = 80; acceleration = 20f; maxSpeed = 160f; deceleration = 40f
            turnRate = 360f; faction = 2; category = ""; color = Color.CYAN
            autoControl = false; description = "导弹发射车，远距离范围打击。"
            dockRadius = 80f
            physicsMass = 7f
            refVolumeShape = "circle"
            refVolumeRadius = 50f  // 根据需要设置
            costItem = "金属"; costAmount = 12
            weaponSlots.add(ModuleManager.createSlotFromTemplate("missile_default") ?: PlacedObject.WeaponSlot())
            weaponSlots.add(ModuleManager.createSlotFromTemplate("railgun_a1") ?: PlacedObject.WeaponSlot())
        })
    }

    fun addTemplate(item: InventoryItem) {
        // 移除旧的同名模板，确保新值生效
        templates.removeAll { it.name == item.name }
        templates.add(item)
    }
}