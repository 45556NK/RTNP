package com.rtnp.demo.gpu.render.tools

import android.opengl.GLES30
import com.rtnp.demo.gpu.CameraMatrix
import com.rtnp.demo.gpu.ShaderProgram
import com.rtnp.demo.strategy.StrategyToolLayer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.cos
import kotlin.math.sin

class RangeLimitRenderSystem {

    private var discShader: ShaderProgram? = null
    private var ringShader: ShaderProgram? = null
    private var discVertexBuffer: java.nio.FloatBuffer? = null
    private var ringVertexBuffer: java.nio.FloatBuffer? = null
    private val circleSegments = 128

    private val ringColor = floatArrayOf(100f / 255f, 150f / 255f, 1f, 0.7f)
    private val outOfRangeColor = floatArrayOf(1f, 0.588f, 0.588f, 0.3f)

    fun init() {
        // 通用圆盘着色器
        val discVert = """
            #version 300 es
            in vec2 aPosition;
            uniform mat4 uMVPMatrix;
            uniform vec2 uCenter;
            uniform float uRadius;
            void main() {
                vec2 pos = aPosition * uRadius + uCenter;
                gl_Position = uMVPMatrix * vec4(pos, 0.0, 1.0);
            }
        """.trimIndent()
        val discFrag = """
            #version 300 es
            precision mediump float;
            out vec4 fragColor;
            uniform vec4 uColor;
            void main() {
                fragColor = uColor;
            }
        """.trimIndent()
        discShader = ShaderProgram.create(discVert, discFrag)
            ?: throw RuntimeException("disc shader failed")

        // 圆环着色器
        val ringVert = """
            #version 300 es
            in vec2 aPosition;
            in float aRadiusScale;
            uniform mat4 uMVPMatrix;
            uniform vec2 uCenter;
            uniform float uOuterRadius;
            uniform float uInnerRadius;
            void main() {
                float r = mix(uInnerRadius, uOuterRadius, aRadiusScale);
                vec2 pos = aPosition * r + uCenter;
                gl_Position = uMVPMatrix * vec4(pos, 0.0, 1.0);
            }
        """.trimIndent()
        val ringFrag = """
            #version 300 es
            precision mediump float;
            out vec4 fragColor;
            uniform vec4 uColor;
            void main() {
                fragColor = uColor;
            }
        """.trimIndent()
        ringShader = ShaderProgram.create(ringVert, ringFrag)
            ?: throw RuntimeException("ring shader failed")

        // 圆盘顶点（TRIANGLE_FAN）
        val discVerts = mutableListOf<Float>()
        discVerts.add(0f); discVerts.add(0f)
        for (i in 0..circleSegments) {
            val angle = 2.0 * Math.PI * i / circleSegments
            discVerts.add(cos(angle).toFloat())
            discVerts.add(sin(angle).toFloat())
        }
        discVertexBuffer = ByteBuffer.allocateDirect(discVerts.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()
            .put(discVerts.toFloatArray()).apply { position(0) }

        // 圆环顶点（TRIANGLE_STRIP，内外圈交替）
        val ringVerts = mutableListOf<Float>()
        for (i in 0..circleSegments) {
            val angle = 2.0 * Math.PI * i / circleSegments
            val x = cos(angle).toFloat()
            val y = sin(angle).toFloat()
            // 外圈顶点
            ringVerts.add(x); ringVerts.add(y); ringVerts.add(1.0f)
            // 内圈顶点
            ringVerts.add(x); ringVerts.add(y); ringVerts.add(0.0f)
        }
        ringVertexBuffer = ByteBuffer.allocateDirect(ringVerts.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()
            .put(ringVerts.toFloatArray()).apply { position(0) }
    }

    fun render(cameraMatrix: CameraMatrix, worldWidth: Float, worldHeight: Float) {
        val state = StrategyToolLayer
        val center = state.getCenterIfActive() ?: return
        val radius = state.radius

        val blendEnabledBefore = GLES30.glIsEnabled(GLES30.GL_BLEND)

        val mvp = cameraMatrix.getMVPMatrix()
        val outerRing = radius + 5f
        val innerRing = radius - 5f
        val holeRadius = radius + 5f

        // ===== 第一步：模板缓冲标记空洞 =====
        GLES30.glEnable(GLES30.GL_STENCIL_TEST)
        GLES30.glClear(GLES30.GL_STENCIL_BUFFER_BIT)
        GLES30.glStencilFunc(GLES30.GL_ALWAYS, 1, 0xFF)
        GLES30.glStencilOp(GLES30.GL_REPLACE, GLES30.GL_REPLACE, GLES30.GL_REPLACE)
        GLES30.glColorMask(false, false, false, false)
        GLES30.glDepthMask(false)

        discShader?.let { s ->
            s.use()
            val buf = discVertexBuffer ?: return@let
            buf.position(0)
            val posHandle = s.getAttribLocation("aPosition")
            if (posHandle == -1) return@let

            GLES30.glVertexAttribPointer(posHandle, 2, GLES30.GL_FLOAT, false, 0, buf)
            GLES30.glEnableVertexAttribArray(posHandle)

            s.setMat4("uMVPMatrix", mvp)
            s.setVec2("uCenter", floatArrayOf(center.first, center.second))
            s.setFloat("uRadius", holeRadius)

            GLES30.glDrawArrays(GLES30.GL_TRIANGLE_FAN, 0, 2 + circleSegments)

            GLES30.glDisableVertexAttribArray(posHandle)
        }

        GLES30.glColorMask(true, true, true, true)

        // ===== 第二步：红色遮罩（模板值=0的区域） =====
        GLES30.glStencilFunc(GLES30.GL_EQUAL, 0, 0xFF)
        GLES30.glStencilOp(GLES30.GL_KEEP, GLES30.GL_KEEP, GLES30.GL_KEEP)
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)

        discShader?.let { s ->
            s.use()
            val buf = discVertexBuffer ?: return@let
            buf.position(0)
            val posHandle = s.getAttribLocation("aPosition")
            if (posHandle == -1) return@let

            GLES30.glVertexAttribPointer(posHandle, 2, GLES30.GL_FLOAT, false, 0, buf)
            GLES30.glEnableVertexAttribArray(posHandle)

            s.setMat4("uMVPMatrix", mvp)
            s.setVec4("uColor", outOfRangeColor)
            s.setVec2("uCenter", floatArrayOf(center.first, center.second))
            s.setFloat("uRadius", 10000000f)

            GLES30.glDrawArrays(GLES30.GL_TRIANGLE_FAN, 0, 2 + circleSegments)

            GLES30.glDisableVertexAttribArray(posHandle)
        }

        // ===== 第三步：蓝色圆环（模板值=1的区域，即空洞内） =====
        GLES30.glStencilFunc(GLES30.GL_EQUAL, 1, 0xFF)

        ringShader?.let { s ->
            s.use()
            val buf = ringVertexBuffer ?: return@let
            buf.position(0)

            val stride = 3 * 4
            val posHandle = s.getAttribLocation("aPosition")
            val scaleHandle = s.getAttribLocation("aRadiusScale")
            if (posHandle == -1) return@let

            GLES30.glVertexAttribPointer(posHandle, 2, GLES30.GL_FLOAT, false, stride, buf)
            GLES30.glEnableVertexAttribArray(posHandle)

            if (scaleHandle != -1) {
                buf.position(2)
                GLES30.glVertexAttribPointer(scaleHandle, 1, GLES30.GL_FLOAT, false, stride, buf)
                GLES30.glEnableVertexAttribArray(scaleHandle)
            }

            // 确保混合模式正常
            GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)

            s.setMat4("uMVPMatrix", mvp)
            s.setVec4("uColor", ringColor)
            s.setVec2("uCenter", floatArrayOf(center.first, center.second))
            s.setFloat("uOuterRadius", outerRing)
            s.setFloat("uInnerRadius", innerRing)

            GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, (circleSegments + 1) * 2)

            GLES30.glDisableVertexAttribArray(posHandle)
            if (scaleHandle != -1) GLES30.glDisableVertexAttribArray(scaleHandle)
        }

        // ===== 恢复状态 =====
        GLES30.glDisable(GLES30.GL_STENCIL_TEST)
        if (blendEnabledBefore) {
            GLES30.glEnable(GLES30.GL_BLEND)
        } else {
            GLES30.glDisable(GLES30.GL_BLEND)
        }
        GLES30.glDepthMask(true)
    }
}