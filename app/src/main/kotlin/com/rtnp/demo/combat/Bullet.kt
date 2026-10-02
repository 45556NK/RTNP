package com.rtnp.demo.combat

import com.rtnp.demo.core.PlacedObject

class Bullet(
    @JvmField var x: Float,
    @JvmField var y: Float,
    @JvmField var vx: Float,
    @JvmField var vy: Float,
    @JvmField var color: Int,
    @JvmField var damage: Int,
    @JvmField var faction: Int,
    @JvmField var maxRange: Float,
    @JvmField var startX: Float,
    @JvmField var startY: Float,
    @JvmField var attacker: PlacedObject? = null,
    @JvmField var weapon: PlacedObject.WeaponSlot? = null,
    @JvmField var damageType: DamageType = DamageType.RAILGUN
)