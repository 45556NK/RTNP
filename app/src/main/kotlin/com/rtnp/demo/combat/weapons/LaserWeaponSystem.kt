package com.rtnp.demo.combat.weapons

import android.graphics.Color
import android.opengl.GLES30
import com.rtnp.demo.UnitSystem
import com.rtnp.demo.combat.WeaponConstants
import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.gpu.CameraMatrix
import com.rtnp.demo.gpu.ShaderProgram
import com.rtnp.demo.ui.panel.tool.EnergyRingRenderTool
import com.rtnp.demo.ui.panel.tool.EnergyRingTool
import java.nio.ByteBuffer
import java.nio.ByteOrder

class LaserWeaponSystem : WeaponSystem {

    private var shader: ShaderProgram? = null

    override fun initShader(vertSource: String, fragSource: String) {
        shader = ShaderProgram.create(vertSource, fragSource)
    }

    override fun update(
        attacker: PlacedObject,
        ws: PlacedObject.WeaponSlot,
        host: UnitSystem,
        fixedDt: Float,
        range: Float,
        target: PlacedObject?
    ) {
        if (target == null) {
            ws.laserTarget = null
            ws.laserCharge = 0f
            EnergyRingTool.stop(ws)
            return
        }
        if (!host.placedObjects.contains(target)) return

        if (ws.laserTarget != target) {
            ws.laserTarget = target
            ws.laserCharge = 0f
            EnergyRingTool.startFromSlot(ws, Color.argb(200, 255, 130, 130), true)
        } else {
            if (ws.laserCharge < ws.chargeTime) {
                ws.laserCharge += fixedDt * WeaponConstants.cooldownMultiplier
                if (ws.laserCharge > ws.chargeTime) ws.laserCharge = ws.chargeTime
            }
        }
        val dps = ws.minDamage + (ws.maxDamage - ws.minDamage) * (ws.laserCharge / ws.chargeTime).coerceIn(0f, 1f)
        // ★ 传入 attacker 和 ws
        host.applyDamage(target, attacker, ws, com.rtnp.demo.combat.DamageType.LASER,
            dps * fixedDt * WeaponConstants.cooldownMultiplier)
    }

    override fun getRenderData(attacker: PlacedObject, ws: PlacedObject.WeaponSlot): WeaponRenderData? {
        val t = ws.laserTarget ?: return null
        return WeaponRenderData(
            type = "laser",
            startX = attacker.worldX, startY = attacker.worldY,
            endX = t.worldX, endY = t.worldY,
            color = attacker.color
        )
    }

    override fun render(cameraMatrix: CameraMatrix, units: List<PlacedObject>, host: UnitSystem) {
        val s = shader ?: return
        s.use()
        s.setMat4("uMVPMatrix", cameraMatrix.getMVPMatrix())
        for (attacker in units.toList()) {
            for (ws in attacker.weaponSlots.toList()) {
                if (ws.type != "laser" || !ws.active) continue
                val t = ws.laserTarget ?: continue
                drawLine(s, attacker.worldX, attacker.worldY, t.worldX, t.worldY, attacker.color)
            }
        }
    }

    private fun drawLine(s: ShaderProgram, x1: Float, y1: Float, x2: Float, y2: Float, color: Int) {
        val v = floatArrayOf(x1, y1, 0f, x2, y2, 0f)
        draw(s, v, GLES30.GL_LINES, color)
    }

    private fun draw(s: ShaderProgram, vertices: FloatArray, mode: Int, color: Int) {
        val buf = ByteBuffer.allocateDirect(vertices.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().put(vertices).apply { position(0) }
        val pos = s.getAttribLocation("aPosition")
        GLES30.glVertexAttribPointer(pos, 3, GLES30.GL_FLOAT, false, 0, buf)
        GLES30.glEnableVertexAttribArray(pos)
        val r = ((color shr 16) and 0xFF) / 255f
        val g = ((color shr 8) and 0xFF) / 255f
        val b = (color and 0xFF) / 255f
        s.setVec4("uColor", floatArrayOf(r, g, b, 1f))
        GLES30.glDrawArrays(mode, 0, vertices.size / 3)
        GLES30.glDisableVertexAttribArray(pos)
    }
}