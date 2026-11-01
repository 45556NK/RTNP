package com.rtnp.demo.gpu

import android.content.Context
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import com.rtnp.demo.UnitSystem
import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.gpu.effect.GpuEffectSystem
import com.rtnp.demo.gpu.render.BackgroundRenderPass
import com.rtnp.demo.gpu.render.HexGridRenderPass
import com.rtnp.demo.gpu.render.HighDisplayPass
import com.rtnp.demo.gpu.render.LowDisplayPass
import com.rtnp.demo.gpu.render.StrategyDisplayPass
import com.rtnp.demo.gpu.render.UnitRenderPass
import com.rtnp.demo.gpu.render.WeaponDisplayPass
import com.rtnp.demo.texture.TextureManager
import com.rtnp.demo.logger.Logger
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class GameRenderer(private val context: Context) : GLSurfaceView.Renderer {

    private lateinit var textureProvider: GpuTextureProvider
    private lateinit var backgroundPass: BackgroundRenderPass
    private lateinit var hexGridPass: HexGridRenderPass
    private lateinit var unitRenderPass: UnitRenderPass
    private var lowDisplayPass: LowDisplayPass? = null
    private var weaponDisplayPass: WeaponDisplayPass? = null
    private var highDisplayPass: HighDisplayPass? = null
    private var strategyDisplayPass: StrategyDisplayPass? = null

    val cameraMatrix = CameraMatrix()

    @Volatile var units: List<PlacedObject> = emptyList()
    @Volatile var unitSystem: UnitSystem? = null

    @Volatile private var pendingCameraX = cameraMatrix.worldWidth / 2f
    @Volatile private var pendingCameraY = cameraMatrix.worldHeight / 2f
    @Volatile private var pendingZoom = 0.2f
    @Volatile private var cameraChanged = true

    private var currentZoom = 0.2f

    private var initStep = 0

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES30.glClearColor(0f, 0f, 0f, 1.0f)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
        cameraMatrix.updateSurfaceSize(width, height)
        if (::backgroundPass.isInitialized) backgroundPass.setScreenSize(width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        val fixedDt = UnitSystem.FIXED_DT

        when (initStep) {
            0 -> {
                TextureManager.init(context)
                initStep++
            }
            1 -> {
                textureProvider = GpuTextureProvider()
                backgroundPass = BackgroundRenderPass(textureProvider)
                backgroundPass.setContext(context)
                backgroundPass.init()
                backgroundPass.setScreenSize(cameraMatrix.surfaceWidth, cameraMatrix.surfaceHeight)
                initStep++
            }
            2 -> {
                hexGridPass = HexGridRenderPass()
                hexGridPass.init()
                initStep++
            }
            3 -> {
                GpuEffectSystem.init(context)
                initStep++
            }
            4 -> {
                unitRenderPass = UnitRenderPass()
                unitRenderPass.init()
                initStep++
            }
            5 -> {
                try {
                    val lp = LowDisplayPass(textureProvider)
                    lp.init()
                    lowDisplayPass = lp
                } catch (e: Exception) {
                    Logger.e("GameRenderer", "LowDisplayPass init failed", e)
                    lowDisplayPass = null
                }
                try {
                    val wp = WeaponDisplayPass()
                    wp.init()
                    weaponDisplayPass = wp
                } catch (e: Exception) {
                    Logger.e("GameRenderer", "WeaponDisplayPass init failed", e)
                    weaponDisplayPass = null
                }
                try {
                    val hp = HighDisplayPass()
                    hp.init()
                    highDisplayPass = hp
                } catch (e: Exception) {
                    Logger.e("GameRenderer", "HighDisplayPass init failed", e)
                    highDisplayPass = null
                }
                try {
                    val sp = StrategyDisplayPass()
                    sp.init()
                    strategyDisplayPass = sp
                } catch (e: Exception) {
                    Logger.e("GameRenderer", "StrategyDisplayPass init failed", e)
                    strategyDisplayPass = null
                }
                initStep++
            }
        }

        GpuEffectSystem.update(fixedDt)

        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_STENCIL_BUFFER_BIT)

        if (initStep < 2) return

        if (cameraChanged) {
            cameraMatrix.update(pendingCameraX, pendingCameraY, pendingZoom)
            currentZoom = pendingZoom
            cameraChanged = false
        }

        // 1. 背景
        backgroundPass.draw("bg")

        // 2. 六边形网格
        if (::hexGridPass.isInitialized) {
            hexGridPass.draw(cameraMatrix)
        }

        // 3. 底部显示层
        lowDisplayPass?.draw(units, unitSystem, cameraMatrix, currentZoom)

        // 4. 下特效层
        GpuEffectSystem.drawUnder(cameraMatrix)

        // 5. 单位
        if (::unitRenderPass.isInitialized) {
            unitRenderPass.draw(units, cameraMatrix)
        }

        // 6. 武器显示层
        val us = unitSystem
        if (us != null) weaponDisplayPass?.draw(units, us, cameraMatrix)

        // 7. 上特效层
        GpuEffectSystem.drawOver(cameraMatrix)

        // 8. 高层显示层
        highDisplayPass?.draw(units, unitSystem, cameraMatrix, currentZoom)

        // 9. 策略显示层（传入单位和系统引用，并生成 GPU 渲染命令）
        strategyDisplayPass?.draw(cameraMatrix, fixedDt, currentZoom, units, us)
    }

    fun updateCamera(cameraX: Float, cameraY: Float, zoom: Float) {
        pendingCameraX = cameraX
        pendingCameraY = cameraY
        pendingZoom = zoom
        cameraChanged = true
    }

    fun updateUnits(list: List<PlacedObject>) {
        // 快照：避免 GL 线程遍历时主线程修改原列表导致 ConcurrentModificationException
        units = ArrayList(list)
    }
}