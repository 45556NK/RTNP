package com.rtnp.demo.combat

import com.rtnp.demo.UnitSystem
import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.logic.BuildManager
import com.rtnp.demo.shield.ShieldManager
import com.rtnp.demo.trait.TargetFilter

class CombatManager(private val host: UnitSystem) {

    fun hasMovingUnits(): Boolean {
        for (u in host.placedObjects) if (u.isMoving) return true
        return host.bullets.isNotEmpty() || host.missileSystem.getEffects().isNotEmpty()
    }

    fun applyExplosionDamage(missile: PlacedObject) {
        val nearby = ArrayList<PlacedObject>()
        host.spatialGrid.queryCircle(
            missile.targetX, missile.targetY,
            missile.explosionRange.toFloat(), nearby
        )
        val launcher = missile.missileLauncher
        for (t in nearby) {
            if (t.faction == missile.faction) continue
            host.applyDamage(t, launcher, null, DamageType.EXPLOSION, missile.missileDamage.toFloat())
        }
    }

    fun checkAndStartServiceForBase(base: PlacedObject) {
    }

    /**
    * 主入口：伤害经过立场拦截。
    * 增加 allUnits 传给 absorbDamage，让区域立场可以查找。
    */
    fun applyDamage(
        t: PlacedObject,
        attacker: PlacedObject?,
        weapon: PlacedObject.WeaponSlot?,
        damageType: DamageType,
        dmg: Float
    ) {
        val remaining = ShieldManager.absorbDamage(
            t, host.placedObjects, attacker, weapon, damageType, dmg
        )
        if (remaining <= 0f) return
        applyDamageDirect(t, remaining)
    }

    /**
     * 兼容旧调用：无 attacker / weapon 信息，类型为 ENVIRONMENT。
     */
    fun applyDamage(t: PlacedObject, dmg: Float) {
        applyDamage(t, null, null, DamageType.ENVIRONMENT, dmg)
    }

    /**
     * 纯伤害：绕过立场，用于自毁、环境伤害等。
     */
    fun applyDamageDirect(t: PlacedObject, dmg: Float) {
        if ("建造中单位" == t.category) {
            BuildManager.damageBuildProcess(t, dmg)
            return
        }
        val hp = host.healthMap[t]
        if (hp != null) {
            val nh = hp - dmg
            host.healthMap[t] = nh
            t.health = nh.toInt()
        }
    }

    fun checkMissileMinRange(a: PlacedObject, t: PlacedObject): Boolean {
        val dx = t.worldX - a.worldX
        val dy = t.worldY - a.worldY
        return (dx * dx + dy * dy) >= a.missileMinRange * a.missileMinRange
    }

    fun findTarget(att: PlacedObject, range: Float): PlacedObject? {
        if (att.faction == 0) {
            att.currentTarget = null
            return null
        }
        val locked = att.currentTarget
        if (locked != null) {
            if (locked.faction == 0) {
                att.currentTarget = null
            } else {
                val hp = host.healthMap[locked]
                if (hp != null && hp > 0) {
                    val dx = locked.worldX - att.worldX
                    val dy = locked.worldY - att.worldY
                    if (dx * dx + dy * dy <= range * range) {
                        return locked
                    }
                }
                att.currentTarget = null
            }
        }
        var best: PlacedObject? = null
        var bestDist2 = Float.MAX_VALUE
        val range2 = range * range
        for (t in host.placedObjects) {
            if (t.faction == att.faction) continue
            if (t.faction == 0) continue
            val hp = host.healthMap[t]
            if (hp == null || hp <= 0) continue
            val dx = t.worldX - att.worldX
            val dy = t.worldY - att.worldY
            val dist2 = dx * dx + dy * dy
            if (dist2 <= range2 && dist2 < bestDist2) {
                bestDist2 = dist2
                best = t
            }
        }
        att.currentTarget = best
        return best
    }

    fun findTargetForWeaponSlot(
        att: PlacedObject,
        ws: PlacedObject.WeaponSlot,
        range: Float
    ): PlacedObject? {
        if (att.faction == 0) {
            ws.lockedTarget = null
            return null
        }
        val locked = ws.lockedTarget
        if (locked != null) {
            if (!host.placedObjects.contains(locked)) {
                ws.lockedTarget = null
            } else if (locked.faction == 0 || locked.faction == att.faction) {
                ws.lockedTarget = null
            } else {
                val hp = host.healthMap[locked]
                if (hp != null && hp > 0) {
                    val dx = locked.worldX - att.worldX
                    val dy = locked.worldY - att.worldY
                    if (dx * dx + dy * dy <= range * range) {
                        if (!TargetFilter.shouldFilter(ws, locked)) {
                            return locked
                        }
                    }
                }
                ws.lockedTarget = null
            }
        }
        var best: PlacedObject? = null
        var bestDist2 = Float.MAX_VALUE
        val range2 = range * range
        for (t in host.placedObjects) {
            if (t.faction == att.faction) continue
            if (t.faction == 0) continue
            if (TargetFilter.shouldFilter(ws, t)) continue
            val hp = host.healthMap[t]
            if (hp == null || hp <= 0) continue
            val dx = t.worldX - att.worldX
            val dy = t.worldY - att.worldY
            val dist2 = dx * dx + dy * dy
            if (dist2 <= range2 && dist2 < bestDist2) {
                bestDist2 = dist2
                best = t
            }
        }
        ws.lockedTarget = best
        return best
    }
}