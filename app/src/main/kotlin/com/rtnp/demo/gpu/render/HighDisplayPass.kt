package com.rtnp.demo.gpu.render

import com.rtnp.demo.UnitSystem
import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.gpu.CameraMatrix
import com.rtnp.demo.gpu.render.tools.*
import com.rtnp.demo.shield.ShieldManager

class HighDisplayPass {

    private val movePathRenderer = MovePathRenderer()
    private val healthIndicatorSystem = HealthIndicatorSystem()
    private val selectionBoxSystem = SelectionBoxSystem()
    private val selectionIndicatorSystem = SelectionIndicatorSystem()
    private val rangeLimitRenderSystem = RangeLimitRenderSystem()
    private val tileSelectorRenderSystem = TileSelectorRenderSystem()
    private val explosionRangeDisplaySystem = ExplosionRangeDisplaySystem
    private val suppressionRangeDisplaySystem = SuppressionRangeDisplaySystem

    // ★ 立场渲染系统
    private val shieldRenderSystem = ShieldRenderSystem

    fun init() {
        movePathRenderer.init()
        healthIndicatorSystem.init()
        selectionBoxSystem.init()
        selectionIndicatorSystem.init()
        rangeLimitRenderSystem.init()
        tileSelectorRenderSystem.init()
        explosionRangeDisplaySystem.init()
        suppressionRangeDisplaySystem.init()

        // ★ 初始化立场渲染
        shieldRenderSystem.init()
    }

    fun draw(units: List<PlacedObject>, unitSystem: UnitSystem?, cameraMatrix: CameraMatrix, zoom: Float) {

        val safeUnits = units.toList()
        // 移动路径
        movePathRenderer.build(units)
        movePathRenderer.drawLines(cameraMatrix, floatArrayOf(1f, 1f, 1f, 1f))
        movePathRenderer.drawDots(cameraMatrix, floatArrayOf(0.6f, 0.8f, 1f, 1f))

        // 血量指示灯
        healthIndicatorSystem.render(cameraMatrix, units, unitSystem, zoom)

        // 单位选中指示灯
        selectionIndicatorSystem.render(cameraMatrix, units, unitSystem, zoom)

        // 多选框
        unitSystem?.multiSelectManager?.let { mm ->
            if (mm.isSelecting()) {
                val rect = mm.getCurrentWorldRect()
                selectionBoxSystem.render(cameraMatrix, rect)
            }
        }

        // 限制范围渲染（策略工具使用）
        rangeLimitRenderSystem.render(cameraMatrix, 3000f, 5000f)

        // 区块选择指示器
        tileSelectorRenderSystem.render(cameraMatrix, zoom)

        // 爆炸范围显示
        explosionRangeDisplaySystem.render(cameraMatrix)

        // 压制范围显示
        suppressionRangeDisplaySystem.render(cameraMatrix)

        // ★ 立场渲染
        shieldRenderSystem.clearCommands()
        if (unitSystem != null) {
            ShieldManager.renderGpuShields(safeUnits, shieldRenderSystem)
        }
        shieldRenderSystem.render(cameraMatrix)
    }
}