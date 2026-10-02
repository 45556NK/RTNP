package com.rtnp.demo.combat

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.rtnp.demo.UnitSystem
import com.rtnp.demo.core.PlacedObject
import kotlin.math.sqrt

object ExplosionDamageTool {

    const val DEFAULT_EFFECT_ID = "explosion_basic"

    private val suppressionUnits = mutableListOf<PlacedObject>()

    private val rangePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(60, 255, 100, 100)
        style = Paint.Style.FILL
    }

    fun registerSuppression(unit: PlacedObject) {
        if (unit.suppressionRange > 0 && unit.suppressionDamage > 0) {
            if (!suppressionUnits.contains(unit)) {
                suppressionUnits.add(unit)
            }
        }
    }

    fun unregisterSuppression(unit: PlacedObject) {
        suppressionUnits.remove(unit)
    }

    @JvmOverloads
    fun applyExplosion(
        host: UnitSystem,
        centerX: Float,
        centerY: Float,
        range: Float,
        damage: Float,
        sourceFaction: Int,
        effectId: String? = null,
        attacker: PlacedObject? = null
    ) {
        for (unit in host.placedObjects.toList()) {
            if (unit.faction == sourceFaction) continue
            if (unit.category == "missile") continue

            val dx = unit.worldX - centerX
            val dy = unit.worldY - centerY
            if (sqrt((dx * dx + dy * dy).toDouble()).toFloat() <= range) {
                host.applyDamage(unit, attacker, null, com.rtnp.demo.combat.DamageType.EXPLOSION, damage)
            }
        }
    }

    fun applySuppression(
        host: UnitSystem,
        unit: PlacedObject
    ) {
        if (unit.suppressionRange <= 0 || unit.suppressionDamage <= 0) return

        for (t in host.placedObjects.toList()) {
            if (t.faction == unit.faction) continue
            if (t.category == "missile") continue

            val dx = t.worldX - unit.worldX
            val dy = t.worldY - unit.worldY
            if (sqrt((dx * dx + dy * dy).toDouble()).toFloat() <= unit.suppressionRange) {
                host.applyDamage(t, unit, null, com.rtnp.demo.combat.DamageType.EXPLOSION, unit.suppressionDamage.toFloat())
            }
        }
    }

    fun update(host: UnitSystem, deltaTime: Float) {
        val iterator = suppressionUnits.iterator()
        while (iterator.hasNext()) {
            val unit = iterator.next()
            try {
                val hp = host.healthMap[unit]
                if (hp == null || hp <= 0f || !host.placedObjects.contains(unit)) {
                    if (unit.suppressionRange > 0 && unit.suppressionDamage > 0) {
                        applySuppression(host, unit)
                    }
                    iterator.remove()
                }
            } catch (e: Exception) {
                iterator.remove()
            }
        }
    }

    fun draw(canvas: Canvas) {
        for (unit in suppressionUnits) {
            if (unit.suppressionRange > 0) {
                canvas.drawCircle(unit.worldX, unit.worldY, unit.suppressionRange.toFloat(), rangePaint)
            }
        }
    }
}