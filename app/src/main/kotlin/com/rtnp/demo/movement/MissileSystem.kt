package com.rtnp.demo.movement

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.rtnp.demo.UnitSystem
import com.rtnp.demo.combat.ExplosionDamageTool
import com.rtnp.demo.core.MissileType
import com.rtnp.demo.core.MissileTypeRegistry
import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.core.GameConstants
import com.rtnp.demo.trait.TargetFilter
import com.rtnp.demo.combat.WeaponConstants
import com.rtnp.demo.gpu.effect.GpuEffectSystem
import com.rtnp.demo.gpu.render.tools.ExplosionRangeDisplaySystem
import com.rtnp.demo.gpu.render.tools.SuppressionRangeDisplaySystem
import kotlin.math.sqrt
import kotlin.math.abs

class MissileSystem {

    private var host: UnitSystem? = null
    private val missiles = mutableListOf<PlacedObject>()
    private val effects = mutableListOf<ExplosionEffect>()
    private val watchedMissiles = mutableSetOf<PlacedObject>()

    fun setHost(host: UnitSystem) {
        this.host = host
    }

    fun setExplosionRange(missile: PlacedObject, range: Int) {
        if (missile.category != "missile") return
        missile.explosionRange = range
    }

    fun setExplosionDamage(missile: PlacedObject, damage: Int) {
        if (missile.category != "missile") return
        missile.missileDamage = damage
    }

    fun setMissileSpeed(missile: PlacedObject, speed: Int) {
        if (missile.category != "missile") return
        missile.missileMaxSpeed = speed
    }

    fun setMissileCurrentSpeed(missile: PlacedObject, speed: Float) {
        if (missile.category != "missile") return
        missile.missileCurrentSpeed = speed
    }

    fun setMissileAcceleration(missile: PlacedObject, accel: Float) {
        if (missile.category != "missile") return
        missile.missileAcceleration = accel
    }

    fun setMissileHealth(missile: PlacedObject, health: Int) {
        if (missile.category != "missile") return
        missile.health = health
        missile.originalHealth = health
        host?.healthMap?.put(missile, health.toFloat())
    }

    fun setMissileLifetime(missile: PlacedObject, lifetime: Int) {
        if (missile.category != "missile") return
        missile.missileLifetime = lifetime
        missile.maxLifetime = lifetime.toFloat()
    }

    fun setMissileTarget(missile: PlacedObject, targetX: Float, targetY: Float) {
        if (missile.category != "missile") return
        missile.targetX = targetX
        missile.targetY = targetY
    }

    fun getLauncher(missile: PlacedObject): PlacedObject? = missile.missileLauncher

    fun getAllMissiles(): List<PlacedObject> = missiles.toList()

    fun getMissilesByLauncher(launcher: PlacedObject): List<PlacedObject> {
        return missiles.filter { it.missileLauncher == launcher }
    }

    fun detonateAllByLauncher(launcher: PlacedObject) {
        val list = missiles.filter { it.missileLauncher == launcher }
        for (m in list) {
            missiles.remove(m)
            directExplode(m)
        }
    }

    fun removeAllByLauncher(launcher: PlacedObject) {
        val list = missiles.filter { it.missileLauncher == launcher }
        for (m in list) removeFromWorld(m)
    }

    fun setAllExplosionRange(range: Int) {
        for (m in missiles) m.explosionRange = range
    }

    fun setAllExplosionDamage(damage: Int) {
        for (m in missiles) m.missileDamage = damage
    }

    fun selfDestructAll() {
        val list = missiles.toList()
        missiles.clear()
        for (m in list) directExplode(m)
    }

    fun watch(missile: PlacedObject) {
        if (missile.category == "missile") watchedMissiles.add(missile)
    }

    fun unwatch(missile: PlacedObject) { watchedMissiles.remove(missile) }
    fun unwatchAll() { watchedMissiles.clear() }

    fun isWatched(missile: PlacedObject): Boolean = watchedMissiles.contains(missile)

    fun selfDestructWatched() {
        if (watchedMissiles.isEmpty()) return
        val list = mutableListOf<PlacedObject>()
        val watched = watchedMissiles.toList()
        for (m in watched) {
            if (!missiles.contains(m)) { watchedMissiles.remove(m); continue }
            val h = host ?: continue
            if (!h.placedObjects.contains(m)) { missiles.remove(m); watchedMissiles.remove(m); continue }
            missiles.remove(m)
            list.add(m)
        }
        for (m in list) {
            try { directExplode(m) } catch (e: Exception) { removeFromWorldSilent(m) }
        }
        watchedMissiles.clear()
    }

    private fun removeFromWorldSilent(missile: PlacedObject) {
        cleanupRangeDisplays(missile)
        val h = host ?: return
        try { h.placedObjects.remove(missile) } catch (e: Exception) {}
        try { h.healthMap.remove(missile) } catch (e: Exception) {}
        try { h.unitsWithLifetime.remove(missile) } catch (e: Exception) {}
        try { if (missile == h.selectedUnit) h.deselectUnit() } catch (e: Exception) {}
    }

    fun launchMissile(launcher: PlacedObject, target: PlacedObject) {
        val h = host ?: return
        var activeMissileSlot: PlacedObject.WeaponSlot? = null
        for (ws in launcher.weaponSlots) {
            if (ws.type == "missile" && ws.active) {
                activeMissileSlot = ws
                if (TargetFilter.shouldFilter(ws, target)) return
                break
            }
        }
        val typeName = when {
            launcher.missileTypeName.isNotEmpty() -> launcher.missileTypeName
            activeMissileSlot?.missileTypeName?.isNotEmpty() == true -> activeMissileSlot!!.missileTypeName
            else -> return
        }
        val type = MissileTypeRegistry.get(typeName) ?: return
        val missile = createMissile(type, typeName, launcher, target.worldX, target.worldY)
        h.placedObjects.add(missile)
        h.healthMap[missile] = missile.health.toFloat()
        missiles.add(missile)
    }

    fun launchMissileAtCoord(launcher: PlacedObject, tx: Float, ty: Float, typeName: String): PlacedObject? {
        val h = host ?: return null
        val type = MissileTypeRegistry.get(typeName) ?: return null
        val missile = createMissile(type, typeName, launcher, tx, ty)
        h.placedObjects.add(missile)
        h.healthMap[missile] = missile.health.toFloat()
        missiles.add(missile)
        return missile
    }

    private fun createMissile(type: MissileType, typeName: String, launcher: PlacedObject, tx: Float, ty: Float): PlacedObject {
        return PlacedObject().apply {
            this.type = "unit"; category = "missile"; name = type.name; missileTypeName = typeName
            missileLauncher = launcher; shape = type.shape; size = type.size
            worldX = launcher.worldX; worldY = launcher.worldY; targetX = tx; targetY = ty
            faction = launcher.faction; color = type.color
            missileDamage = type.damage; explosionRange = type.explosionRange; effectSize = type.effectSize
            missileLifetime = type.lifetime; missileStartSpeed = type.startSpeed
            missileMaxSpeed = type.maxSpeed; missileAcceleration = type.acceleration
            missileCurrentSpeed = type.startSpeed.toFloat(); health = type.health; originalHealth = type.health
            isMoving = true; lifetime = 0f; maxLifetime = type.lifetime.toFloat()
            heading = Math.toDegrees(Math.atan2((ty - launcher.worldY).toDouble(), (tx - launcher.worldX).toDouble())).toFloat()
            canMove = false; weaponSlots.clear()
            displayName = "${type.name}(${GameConstants.factionToName(launcher.faction)})"
            currentSpeed = type.startSpeed.toFloat(); effectAngleOffset = -1f; traitIds = mutableListOf()
            hasSuppression = type.hasSuppression; suppressionRange = type.suppressionRange
            suppressionDamage = type.suppressionDamage; suppressionDuration = type.suppressionDuration
            suppressionEffectId = type.suppressionEffectId; explosionEffectId = type.explosionEffectId
            explosionRangeDisplayId = ExplosionRangeDisplaySystem.addExplosionRange(tx, ty, explosionRange.toFloat())
            if (type.hasSuppression) {
                ExplosionDamageTool.registerSuppression(this)
                suppressionRangeDisplayId = SuppressionRangeDisplaySystem.addSuppressionRange(worldX, worldY, suppressionRange.toFloat())
            }
        }
    }

    fun update(deltaTime: Float) {
        val sp = WeaponConstants.cooldownMultiplier
        val directList = mutableListOf<PlacedObject>()
        val suppressionList = mutableListOf<PlacedObject>()
        val h = host
        if (h != null) ExplosionDamageTool.update(h, deltaTime * sp)
        val it = missiles.iterator()
        while (it.hasNext()) {
            val m = it.next()
            if (m.traitIds.isNotEmpty()) com.rtnp.demo.trait.TraitManager.updateTraits(m, deltaTime, m.traitIds, host?.healthMap ?: emptyMap())
            m.lifetime += deltaTime * sp
            if (m.lifetime >= m.maxLifetime) { suppressionList.add(m); it.remove(); continue }
            if (m.lifetime > 0.1f) {
                val hp = host?.healthMap?.get(m) ?: m.health.toFloat()
                if (hp <= 0f) { it.remove(); suppressionList.add(m); continue }
            }
            if (!m.isMoving) continue
            if (m.missileCurrentSpeed < m.missileMaxSpeed) {
                m.missileCurrentSpeed += m.missileAcceleration * deltaTime * sp
                if (m.missileCurrentSpeed > m.missileMaxSpeed) m.missileCurrentSpeed = m.missileMaxSpeed.toFloat()
            }
            m.currentSpeed = m.missileCurrentSpeed
            val dx = m.targetX - m.worldX; val dy = m.targetY - m.worldY
            val dist = sqrt(dx * dx + dy * dy); val step = m.missileCurrentSpeed * deltaTime * sp
            if (dist <= 2f || step >= dist) {
                m.worldX = m.targetX; m.worldY = m.targetY; m.isMoving = false
                updateSuppressionRangePosition(m); directList.add(m); it.remove()
            } else {
                val nx = m.worldX + dx / dist * step; val ny = m.worldY + dy / dist * step
                m.heading = Math.toDegrees(Math.atan2((ny - m.worldY).toDouble(), (nx - m.worldX).toDouble())).toFloat()
                m.worldX = nx; m.worldY = ny; updateSuppressionRangePosition(m)
            }
        }
        for (m in directList) directExplode(m)
        for (m in suppressionList) suppressionExplode(m)
        watchedMissiles.removeAll { !missiles.contains(it) && host?.placedObjects?.contains(it) != true }
        val effIt = effects.iterator()
        while (effIt.hasNext()) { val e = effIt.next(); e.update(deltaTime * sp); if (e.isFinished()) effIt.remove() }
    }

    private fun updateSuppressionRangePosition(m: PlacedObject) {
        if (m.suppressionRangeDisplayId >= 0) SuppressionRangeDisplaySystem.updateSuppressionRange(m.suppressionRangeDisplayId, m.worldX, m.worldY)
    }

    fun clearAll() {
        for (m in missiles.toList()) { cleanupRangeDisplays(m); removeFromWorld(m) }
        missiles.clear(); effects.clear(); watchedMissiles.clear()
        ExplosionRangeDisplaySystem.clear(); SuppressionRangeDisplaySystem.clear()
    }

    fun handleMissileDeath(m: PlacedObject) {
        if (m.category != "missile") return
        missiles.remove(m); watchedMissiles.remove(m); cleanupRangeDisplays(m)
    }

    private fun directExplode(m: PlacedObject) {
        val h = host ?: return
        cleanupRangeDisplays(m)
        ExplosionDamageTool.applyExplosion(
            h,
            m.worldX, m.worldY,
            m.explosionRange.toFloat(),
            m.missileDamage.toFloat(),
            m.faction,
            null,
            m.missileLauncher      // ★ 新增
        )
        effects.add(ExplosionEffect(m.worldX, m.worldY, m.explosionRange, m.effectSize))
        removeFromWorld(m)
        val eff = m.explosionEffectId.ifEmpty { "explosion" }
        GpuEffectSystem.spawn(eff, m.worldX, m.worldY, m)
    }

    private fun suppressionExplode(m: PlacedObject) {
        val h = host ?: return; cleanupRangeDisplays(m); removeFromWorld(m)
        if (m.hasSuppression) {
            ExplosionDamageTool.applySuppression(h, m)
            if (!m.suppressionEffectId.isNullOrEmpty()) GpuEffectSystem.spawn(m.suppressionEffectId!!, m.worldX, m.worldY, m)
        }
    }

    private fun removeFromWorld(m: PlacedObject) {
        cleanupRangeDisplays(m); val h = host ?: return
        h.placedObjects.remove(m); h.healthMap.remove(m); h.unitsWithLifetime.remove(m)
        missiles.remove(m); watchedMissiles.remove(m)
        if (m == h.selectedUnit) {
            h.deselectUnit()
            com.rtnp.demo.ui.panel.RightPanel.unlock()
        }
    }

    private fun removeFromWorldSilentNoMissiles(m: PlacedObject) {
        cleanupRangeDisplays(m); val h = host ?: return
        h.placedObjects.remove(m); h.healthMap.remove(m); h.unitsWithLifetime.remove(m)
        watchedMissiles.remove(m)
        if (m == h.selectedUnit) h.deselectUnit()
    }

    private fun cleanupRangeDisplays(m: PlacedObject) {
        if (m.explosionRangeDisplayId >= 0) { ExplosionRangeDisplaySystem.removeExplosionRange(m.explosionRangeDisplayId); m.explosionRangeDisplayId = -1L }
        if (m.suppressionRangeDisplayId >= 0) { SuppressionRangeDisplaySystem.removeSuppressionRange(m.suppressionRangeDisplayId); m.suppressionRangeDisplayId = -1L }
    }

    fun getMissiles(): List<PlacedObject> = missiles.toList()
    fun getEffects(): List<ExplosionEffect> = effects
    fun removeMissile(m: PlacedObject) { if (missiles.remove(m)) removeFromWorld(m) }

    fun draw(canvas: Canvas, zoom: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        ExplosionDamageTool.draw(canvas)
        for (m in missiles) {
            if (m.isMoving) {
                paint.apply { color = Color.WHITE; strokeWidth = 2f / zoom; style = Paint.Style.STROKE }
                canvas.drawLine(m.worldX, m.worldY, m.targetX, m.targetY, paint)
            }
        }
        for (m in missiles) {
            canvas.save(); canvas.translate(m.worldX, m.worldY); canvas.rotate(m.heading)
            paint.apply { color = m.color; style = Paint.Style.FILL }
            val half = m.size / 2f; canvas.drawRect(-half, -half, half, half, paint); canvas.restore()
        }
        for (eff in effects) eff.draw(canvas, zoom)
    }

    class ExplosionEffect(private val x: Float, private val y: Float, private val maxRadius: Int, private val effectSize: Int) {
        private var time = 0f
        private val expandDuration = 0.5f; private val ringDuration = 3f
        fun update(dt: Float) { time += dt }
        fun isFinished(): Boolean = time > ringDuration * 2
        fun draw(canvas: Canvas, zoom: Float) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            if (time < expandDuration) {
                val r = effectSize * (time / expandDuration); val a = (255 * (1 - time / expandDuration)).toInt()
                paint.apply { color = Color.argb(a, 255, 0, 0); style = Paint.Style.FILL }
                canvas.drawCircle(x, y, r, paint)
            }
            if (time > 0.1f) {
                val rt = time - 0.1f
                if (rt < ringDuration) {
                    val sr = maxRadius / 3f; val er = maxRadius.toFloat()
                    val cr = sr + (er - sr) * (rt / ringDuration); val a = (255 * (1 - rt / ringDuration)).toInt()
                    paint.apply { color = Color.argb(a, 255, 165, 0); style = Paint.Style.STROKE; strokeWidth = 20f }
                    canvas.drawCircle(x, y, cr, paint)
                }
            }
        }
    }
}