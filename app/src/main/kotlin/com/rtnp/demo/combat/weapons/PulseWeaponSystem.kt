package com.rtnp.demo.combat.weapons

import android.graphics.Color
import android.opengl.GLES30
import com.rtnp.demo.UnitSystem
import com.rtnp.demo.combat.PulseWeapon
import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.gpu.CameraMatrix
import com.rtnp.demo.gpu.ShaderProgram
import com.rtnp.demo.ui.panel.tool.CooldownRenderTool
import com.rtnp.demo.ui.panel.tool.EnergyRingRenderTool
import com.rtnp.demo.ui.panel.tool.EnergyRingTool
import com.rtnp.demo.ui.panel.tool.WeaponButtonTool
import java.nio.ByteBuffer
import java.nio.ByteOrder

class PulseWeaponSystem : WeaponSystem {

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
        if (ws.pulseWeapon == null) {
            ws.pulseWeapon = PulseWeapon(ws, attacker)
            ws.pulseWeapon!!.maxTargets = ws.maxTargets
        }
        ws.pulseWeapon!!.update(fixedDt, host, range)

        val pw = ws.pulseWeapon!!

        // ★ 能量消耗阶段：逆时针减少
        if (pw.energyBar.progress < 1f && pw.energyBar.progress > 0f && !pw.cooldownBar.isFinished) {
            EnergyRingTool.startFromSlot(ws, Color.argb(200, 100, 200, 255), false)
        }
        // ★ 冷却阶段：能量耗尽，走冷却
        else if (pw.energyBar.progress <= 0f && !pw.cooldownBar.isFinished) {
            EnergyRingTool.stop(ws)
            WeaponButtonTool.setCooldownConfig(ws, Color.argb(89, 255, 130, 130), CooldownRenderTool.Direction.BOTTOM_UP)
        }
        // ★ 能量恢复阶段：顺时针增加
        else if (pw.energyBar.progress > 0f && pw.cooldownBar.isFinished) {
            EnergyRingTool.startFromSlot(ws, Color.argb(200, 100, 200, 255), true)
        }
        else {
            EnergyRingTool.stop(ws)
        }
    }

    override fun getRenderData(attacker: PlacedObject, ws: PlacedObject.WeaponSlot): WeaponRenderData? {
        val pw = ws.pulseWeapon ?: return null
        if (pw.energyBar.progress <= 0f) return null
        val targets = pw.lockedTargets.toList()
        if (targets.isEmpty()) return null
        val t = targets.first()
        return WeaponRenderData(
            type = "pulse",
            startX = attacker.worldX, startY = attacker.worldY,
            endX = t.worldX, endY = t.worldY,
            color = 0xFF64C8FF.toInt(),
            extra = mapOf("targetCount" to targets.size)
        )
    }

    override fun render(cameraMatrix: CameraMatrix, units: List<PlacedObject>, host: UnitSystem) {
        val s = shader ?: return
        s.use()
        s.setMat4("uMVPMatrix", cameraMatrix.getMVPMatrix())
        for (attacker in units.toList()) {
            for (ws in attacker.weaponSlots.toList()) {
                if (ws.type != "pulse" || !ws.active) continue
                val pw = ws.pulseWeapon ?: continue
                for (target in pw.lockedTargets.toList()) {
                    drawLine(s, attacker.worldX, attacker.worldY, target.worldX, target.worldY, 0xFF64C8FF.toInt())
                }
            }
        }
    }

    private fun drawLine(s: ShaderProgram, x1: Float, y1: Float, x2: Float, y2: Float, color: Int) {
        val vertices = floatArrayOf(x1, y1, 0f, x2, y2, 0f)
        draw(s, vertices, GLES30.GL_LINES, color)
    }

    private fun draw(s: ShaderProgram, vertices: FloatArray, mode: Int, color: Int) {
        val buf = ByteBuffer.allocateDirect(vertices.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().put(vertices).apply { position(0) }
        val pos = s.getAttribLocation("aPosition")
        GLES30.glVertexAttribPointer(pos, 3, GLES30.GL_FLOAT, false, 0, buf)
        GLES30.glEnableVertexAttribArray(pos)
        val r = ((color shr 16) and 0xFF) / 255f
        val g = ((color shr 8) and 0xFF) / 255f
        val b = (color and 0xFF) / 255f
        val a = ((color shr 24) and 0xFF) / 255f
        s.setVec4("uColor", floatArrayOf(r, g, b, a))
        GLES30.glDrawArrays(mode, 0, vertices.size / 3)
        GLES30.glDisableVertexAttribArray(pos)
    }
}