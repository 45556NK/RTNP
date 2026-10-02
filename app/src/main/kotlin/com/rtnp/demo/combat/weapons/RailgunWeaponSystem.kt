package com.rtnp.demo.combat.weapons

import android.graphics.Color
import android.opengl.GLES30
import com.rtnp.demo.combat.Bullet
import com.rtnp.demo.UnitSystem
import com.rtnp.demo.combat.WeaponConstants
import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.gpu.CameraMatrix
import com.rtnp.demo.gpu.ShaderProgram
import com.rtnp.demo.gpu.effect.ShaderToolRegistry
import com.rtnp.demo.ui.panel.tool.CooldownRenderTool
import com.rtnp.demo.ui.panel.tool.WeaponButtonTool
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.*

class RailgunWeaponSystem : WeaponSystem {

    private var shader: ShaderProgram? = null
    private var bulletShader: ShaderProgram? = null

    override fun initShader(vertSource: String, fragSource: String) {
        shader = ShaderProgram.create(vertSource, fragSource)
        val bulletFrag = ShaderToolRegistry.getFragmentShader("bullet") ?: return
        val bulletVert = ShaderToolRegistry.getVertexShader("bullet") ?: """
            #version 300 es
            layout(location = 0) in vec4 aPosition;
            layout(location = 1) in vec2 aTexCoord;
            out vec2 textureCoordinate;
            uniform mat4 uMVPMatrix;
            void main() {
                gl_Position = uMVPMatrix * aPosition;
                textureCoordinate = aTexCoord;
            }
        """.trimIndent()
        bulletShader = ShaderProgram.create(bulletVert, bulletFrag)
    }

    override fun update(
        attacker: PlacedObject, ws: PlacedObject.WeaponSlot,
        host: UnitSystem, fixedDt: Float, range: Float, target: PlacedObject?
    ) {
        if (ws.cooldown > 0) {
            val bar = ws.getOrCreateCooldownBar()
            bar.totalDuration = ws.cooldown
            bar.speedMultiplier = WeaponConstants.cooldownMultiplier
            bar.addProgress(fixedDt / ws.cooldown * WeaponConstants.cooldownMultiplier)
        }
        if (ws.damage <= 0 || target == null) return

        val railBar = ws.getOrCreateCooldownBar()
        if (!railBar.isFinished || !host.placedObjects.contains(target)) return
        railBar.reset()

        val dx = target.worldX - attacker.worldX
        val dy = target.worldY - attacker.worldY
        val dist = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
        if (dist <= 0) return

        val baseAngle = atan2(dy.toDouble(), dx.toDouble()).toFloat()
        val finalAngle = baseAngle + ws.aimAngleOffset +
                Math.toRadians(WeaponConstants.launchAngleOffset.toDouble()).toFloat()

        val vx = cos(finalAngle) * ws.bulletSpeed * WeaponConstants.bulletSpeedMultiplier
        val vy = sin(finalAngle) * ws.bulletSpeed * WeaponConstants.bulletSpeedMultiplier

        host.bullets.add(Bullet(
            attacker.worldX, attacker.worldY,
            vx, vy,
            attacker.color,
            ws.damage.toInt(),
            attacker.faction,
            range,
            attacker.worldX, attacker.worldY,
            attacker,
            ws,
            com.rtnp.demo.combat.DamageType.RAILGUN
        ))
        WeaponButtonTool.setCooldownConfig(ws, Color.argb(89, 255, 130, 130), CooldownRenderTool.Direction.BOTTOM_UP)

    }

    override fun getRenderData(attacker: PlacedObject, ws: PlacedObject.WeaponSlot): WeaponRenderData? = null

    override fun render(cameraMatrix: CameraMatrix, units: List<PlacedObject>, host: UnitSystem) {
        val bs = bulletShader ?: return
        bs.use()
        val mvp = cameraMatrix.getMVPMatrix()

        val safeBullets = host.bullets.toList()

        for (b in safeBullets) {
            val angle = Math.toDegrees(atan2(b.vy.toDouble(), b.vx.toDouble())).toFloat()
            val dirX = cos(Math.toRadians(angle.toDouble())).toFloat()
            val dirY = sin(Math.toRadians(angle.toDouble())).toFloat()

            val radius = 30f
            val vertices = floatArrayOf(
                b.x - radius, b.y - radius, 0f, 0f, 0f,
                b.x - radius, b.y + radius, 0f, 0f, 1f,
                b.x + radius, b.y - radius, 0f, 1f, 0f,
                b.x + radius, b.y + radius, 0f, 1f, 1f
            )
            val buf = ByteBuffer.allocateDirect(vertices.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().put(vertices).apply { position(0) }

            bs.setMat4("uMVPMatrix", mvp)
            bs.setFloat("u_progress", 0.5f)
            bs.setVec2("u_direction", floatArrayOf(dirX, dirY))

            val stride = 5 * 4
            val posHandle = bs.getAttribLocation("aPosition")
            if (posHandle != -1) {
                GLES30.glVertexAttribPointer(posHandle, 3, GLES30.GL_FLOAT, false, stride, buf)
                GLES30.glEnableVertexAttribArray(posHandle)
            }
            val texHandle = bs.getAttribLocation("aTexCoord")
            if (texHandle != -1) {
                buf.position(3)
                GLES30.glVertexAttribPointer(texHandle, 2, GLES30.GL_FLOAT, false, stride, buf)
                GLES30.glEnableVertexAttribArray(texHandle)
            }
            GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
            if (posHandle != -1) GLES30.glDisableVertexAttribArray(posHandle)
            if (texHandle != -1) GLES30.glDisableVertexAttribArray(texHandle)
        }
    }
}