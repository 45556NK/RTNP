package com.rtnp.demo.gpu.render.tools

import android.opengl.GLES30
import com.rtnp.demo.gpu.CameraMatrix
import com.rtnp.demo.gpu.ShaderProgram
import com.rtnp.demo.gpu.effect.ShaderToolRegistry
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * 立场渲染命令收集与执行系统。
 *
 * 支持两类渲染命令：
 * 1. 基础渲染（圆、环、线、三角）——走内置 shader
 * 2. 自定义着色器渲染——调用者指定 shaderId 与 uniforms
 *
 * 立场模板在 onGpuRender 中通过 addCircle / addRing / addLine / addTriangle
 * 或 addShaderCommand 推送渲染命令；HighDisplayPass 每帧调用 render 逐条执行。
 */
object ShieldRenderSystem {

    // ==================== 基础渲染 ====================

    private var baseShader: ShaderProgram? = null

    private data class CircleCmd(val cx: Float, val cy: Float, val r: Float, val color: FloatArray)
    private data class RingCmd(val cx: Float, val cy: Float, val outerR: Float, val innerR: Float, val color: FloatArray)
    private data class LineCmd(val x1: Float, val y1: Float, val x2: Float, val y2: Float, val color: FloatArray, val width: Float)
    private data class TriangleCmd(val x1: Float, val y1: Float, val x2: Float, val y2: Float, val x3: Float, val y3: Float, val color: FloatArray)

    private val circles = mutableListOf<CircleCmd>()
    private val rings = mutableListOf<RingCmd>()
    private val lines = mutableListOf<LineCmd>()
    private val triangles = mutableListOf<TriangleCmd>()

    // ==================== 自定义着色器渲染 ====================

    data class ShaderCmd(
        val cx: Float,
        val cy: Float,
        val radius: Float,
        val shaderId: String,
        val vertexShaderId: String?,
        val uniforms: Map<String, Any>
    )

    private val shaderCmds = mutableListOf<ShaderCmd>()

    /** 自定义着色器程序缓存：key = vertexShaderId ?: "<default>" + ":" + shaderId */
    private val shaderProgramCache = mutableMapOf<String, ShaderProgram?>()

    // 默认顶点着色器（系统内置）
    private const val DEFAULT_VERTEX_SHADER = """
        #version 300 es
        layout(location = 0) in vec4 aPosition;
        layout(location = 1) in vec2 aTexCoord;
        out vec2 textureCoordinate;
        uniform mat4 uMVPMatrix;
        void main() {
            gl_Position = uMVPMatrix * aPosition;
            textureCoordinate = aTexCoord;
        }
    """

    fun init() {
        val vert = """
            #version 300 es
            in vec2 aPosition;
            uniform mat4 uMVPMatrix;
            uniform vec2 uTranslation;
            uniform float uScale;
            void main() {
                vec2 pos = aPosition * uScale + uTranslation;
                gl_Position = uMVPMatrix * vec4(pos, 0.0, 1.0);
            }
        """.trimIndent()
        val frag = """
            #version 300 es
            precision mediump float;
            out vec4 fragColor;
            uniform vec4 uColor;
            void main() {
                fragColor = uColor;
            }
        """.trimIndent()
        baseShader = ShaderProgram.create(vert, frag)
    }

    // ==================== 命令推送接口 ====================

    fun addCircle(cx: Float, cy: Float, r: Float, color: FloatArray) {
        circles.add(CircleCmd(cx, cy, r, color))
    }

    fun addRing(cx: Float, cy: Float, outerR: Float, innerR: Float, color: FloatArray) {
        rings.add(RingCmd(cx, cy, outerR, innerR, color))
    }

    fun addLine(x1: Float, y1: Float, x2: Float, y2: Float, color: FloatArray, width: Float = 2f) {
        lines.add(LineCmd(x1, y1, x2, y2, color, width))
    }

    fun addTriangle(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float, color: FloatArray) {
        triangles.add(TriangleCmd(x1, y1, x2, y2, x3, y3, color))
    }

    /**
     * 推送一条自定义着色器渲染命令。
     *
     * @param cx 世界坐标中心X
     * @param cy 世界坐标中心Y
     * @param radius 渲染半径（正方形范围 [-radius, radius]）
     * @param shaderId 片段着色器ID（不含扩展名，如 "shield_pulse"），从 ShaderToolRegistry 查找
     * @param vertexShaderId 可选顶点着色器ID。为 null 时使用系统内置顶点着色器
     * @param uniforms uniform 键值对，支持 Float / Int / FloatArray（长度 2/3/4/16）
     */
    @JvmOverloads
    fun addShaderCommand(
        cx: Float,
        cy: Float,
        radius: Float,
        shaderId: String,
        vertexShaderId: String? = null,
        uniforms: Map<String, Any> = emptyMap()
    ) {
        shaderCmds.add(ShaderCmd(cx, cy, radius, shaderId, vertexShaderId, uniforms))
    }

    // ==================== 渲染 ====================

    fun hasCommands(): Boolean =
        circles.isNotEmpty() || rings.isNotEmpty() || lines.isNotEmpty() ||
        triangles.isNotEmpty() || shaderCmds.isNotEmpty()

    fun clearCommands() {
        circles.clear()
        rings.clear()
        lines.clear()
        triangles.clear()
        shaderCmds.clear()
    }

    fun render(cameraMatrix: CameraMatrix) {
        if (!hasCommands()) return

        val blendEnabled = GLES30.glIsEnabled(GLES30.GL_BLEND)
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)

        // 1. 基础渲染
        renderBasicCommands(cameraMatrix)

        // 2. 自定义着色器渲染
        renderShaderCommands(cameraMatrix)

        if (!blendEnabled) {
            GLES30.glDisable(GLES30.GL_BLEND)
        }
    }

    private fun renderBasicCommands(cameraMatrix: CameraMatrix) {
        if (circles.isEmpty() && rings.isEmpty() && lines.isEmpty() && triangles.isEmpty()) return

        val s = baseShader ?: return
        s.use()
        s.setMat4("uMVPMatrix", cameraMatrix.getMVPMatrix())

        val circleIter = circles.iterator()
        while (circleIter.hasNext()) {
            val c = circleIter.next()
            RenderToolkit.drawCircle(s, c.cx, c.cy, c.r, c.color)
            circleIter.remove()
        }

        val ringIter = rings.iterator()
        while (ringIter.hasNext()) {
            val r = ringIter.next()
            RenderToolkit.drawCircleOutline(s, r.cx, r.cy, r.outerR, r.color)
            RenderToolkit.drawCircleOutline(s, r.cx, r.cy, r.innerR, r.color)
            ringIter.remove()
        }

        val lineIter = lines.iterator()
        while (lineIter.hasNext()) {
            val l = lineIter.next()
            RenderToolkit.drawLine(s, l.x1, l.y1, l.x2, l.y2, l.color, l.width)
            lineIter.remove()
        }

        val triIter = triangles.iterator()
        while (triIter.hasNext()) {
            val t = triIter.next()
            RenderToolkit.drawTriangle(s, t.x1, t.y1, t.x2, t.y2, t.x3, t.y3, t.color)
            triIter.remove()
        }
    }

    private fun renderShaderCommands(cameraMatrix: CameraMatrix) {
        if (shaderCmds.isEmpty()) return

        val mvp = cameraMatrix.getMVPMatrix()

        val iter = shaderCmds.iterator()
        while (iter.hasNext()) {
            val cmd = iter.next()
            renderShaderCommand(cmd, mvp)
            iter.remove()
        }
    }

    private fun renderShaderCommand(cmd: ShaderCmd, mvp: FloatArray) {
        // 获取或创建着色器程序
        val program = getOrCreateProgram(cmd.vertexShaderId, cmd.shaderId) ?: return
        program.use()
        program.setMat4("uMVPMatrix", mvp)

        // 应用 uniform
        applyUniforms(program, cmd.uniforms)

        // 构建四边形顶点：[-radius, radius]，带纹理坐标
        val cx = cmd.cx
        val cy = cmd.cy
        val r = cmd.radius
        val vertices = floatArrayOf(
            cx - r, cy - r, 0f, 0f, 0f,
            cx - r, cy + r, 0f, 0f, 1f,
            cx + r, cy - r, 0f, 1f, 0f,
            cx + r, cy + r, 0f, 1f, 1f
        )
        val buf = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(vertices)
            .apply { position(0) }

        val stride = 5 * 4
        val posHandle = program.getAttribLocation("aPosition")
        if (posHandle != -1) {
            GLES30.glVertexAttribPointer(posHandle, 3, GLES30.GL_FLOAT, false, stride, buf)
            GLES30.glEnableVertexAttribArray(posHandle)
        }

        val texHandle = program.getAttribLocation("aTexCoord")
        if (texHandle != -1) {
            buf.position(3)
            GLES30.glVertexAttribPointer(texHandle, 2, GLES30.GL_FLOAT, false, stride, buf)
            GLES30.glEnableVertexAttribArray(texHandle)
        }

        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)

        if (posHandle != -1) GLES30.glDisableVertexAttribArray(posHandle)
        if (texHandle != -1) GLES30.glDisableVertexAttribArray(texHandle)
    }

    private fun getOrCreateProgram(vertexShaderId: String?, fragmentShaderId: String): ShaderProgram? {
        val cacheKey = "${vertexShaderId ?: "<default>"}:$fragmentShaderId"
        if (shaderProgramCache.containsKey(cacheKey)) {
            return shaderProgramCache[cacheKey]
        }

        val fragSource = ShaderToolRegistry.getFragmentShader(fragmentShaderId)
        if (fragSource == null) {
            shaderProgramCache[cacheKey] = null
            return null
        }

        val vertSource = if (vertexShaderId != null) {
            ShaderToolRegistry.getVertexShader(vertexShaderId) ?: DEFAULT_VERTEX_SHADER
        } else {
            DEFAULT_VERTEX_SHADER
        }

        val program = ShaderProgram.create(vertSource, fragSource)
        shaderProgramCache[cacheKey] = program
        return program
    }

    private fun applyUniforms(program: ShaderProgram, uniforms: Map<String, Any>) {
        for ((key, value) in uniforms) {
            when (value) {
                is Float -> program.setFloat(key, value)
                is Int -> program.setInt(key, value)
                is FloatArray -> {
                    when (value.size) {
                        2 -> GLES30.glUniform2fv(program.getUniformLocation(key), 1, value, 0)
                        3 -> GLES30.glUniform3fv(program.getUniformLocation(key), 1, value, 0)
                        4 -> program.setVec4(key, value)
                        16 -> program.setMat4(key, value)
                    }
                }
            }
        }
    }
}