package com.rtnp.demo.combat

import com.rtnp.demo.UnitSystem
import com.rtnp.demo.core.PlacedObject

object MissileLoop {

    const val MISSILE_HITBOX_EXTRA = 20f

    @JvmStatic
    fun updateBullets(host: UnitSystem, fixedDt: Float) {
        val bit = host.bullets.iterator()
        while (bit.hasNext()) {
            val b = bit.next()
            val prevX = b.x
            val prevY = b.y
            b.x += b.vx * fixedDt
            b.y += b.vy * fixedDt

            val dxStart = b.x - b.startX
            val dyStart = b.y - b.startY
            val distFromStart = Math.sqrt((dxStart * dxStart + dyStart * dyStart).toDouble())
            if (distFromStart >= b.maxRange) {
                bit.remove()
                continue
            }

            var hit = false
            val targetCopy = ArrayList(host.placedObjects)

            for (target in targetCopy) {
                if (target.faction == b.faction) continue
                if (isBulletHitTarget(b, prevX, prevY, target)) {
                    host.applyDamage(target, b.attacker, b.weapon, b.damageType, b.damage.toFloat())
                    hit = true
                    break
                }
            }

            if (hit) bit.remove()
        }
    }

    private fun isBulletHitTarget(b: Bullet, prevX: Float, prevY: Float, target: PlacedObject): Boolean {
        val extra = if ("missile" == target.category) MISSILE_HITBOX_EXTRA else 0f

        if (isPointInObjectExpanded(b.x, b.y, target, extra)) return true

        val dx = b.x - prevX
        val dy = b.y - prevY
        val distSq = dx * dx + dy * dy
        if (distSq < 1f) return false

        val dist = Math.sqrt(distSq.toDouble()).toFloat()
        val steps = (dist / 10f).toInt() + 1
        var i = 1
        while (i <= steps) {
            val t = i.toFloat() / steps.toFloat()
            val cx = prevX + dx * t
            val cy = prevY + dy * t
            if (isPointInObjectExpanded(cx, cy, target, extra)) return true
            i++
        }

        return false
    }

    private fun isPointInObjectExpanded(px: Float, py: Float, o: PlacedObject, extra: Float): Boolean {
        val dx = px - o.worldX
        val dy = py - o.worldY

        val angleOffset = if (o.textureBitmap != null) 90f else 0f
        val angleRad = Math.toRadians((-o.heading + angleOffset).toDouble())
        val cos = Math.cos(angleRad).toFloat()
        val sin = Math.sin(angleRad).toFloat()
        val localX = dx * cos - dy * sin
        val localY = dx * sin + dy * cos

        return when (o.shape) {
            "circle" -> {
                val r = o.size / 2f + extra
                localX * localX + localY * localY <= r * r
            }
            "square" -> {
                val half = o.size / 2f + extra
                Math.abs(localX) <= half && Math.abs(localY) <= half
            }
            else -> {
                val hw = o.width / 2f + extra
                val hh = o.height / 2f + extra
                Math.abs(localX) <= hw && Math.abs(localY) <= hh
            }
        }
    }
}