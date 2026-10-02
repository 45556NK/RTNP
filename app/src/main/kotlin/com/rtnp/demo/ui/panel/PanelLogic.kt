package com.rtnp.demo.ui.panel

import android.content.Context
import android.graphics.RectF
import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.logger.Logger
import com.rtnp.demo.ui.panel.data.*
import com.rtnp.demo.ui.panel.tool.PanelBox
import com.rtnp.demo.ui.panel.tool.PanelLayoutTool
import com.rtnp.demo.ui.panel.tool.ProgressBarRenderTool
import com.rtnp.demo.ui.panel.tool.TemplateSelector

object PanelLogic {

    private val components = mutableListOf<PanelComponent>()
    private val progressBarStates = mutableMapOf<String, Boolean>()

    fun process(state: PanelState, vw: Int, vh: Int, context: Context): List<PanelComponent> {
        PanelBox.update(vw, vh)
        buildComponents(state, vw, context)
        PanelLayoutTool.layout(components)
        restoreProgressBarStates(components)
        return components.toList()
    }

    fun clear() {
        components.clear()
        progressBarStates.clear()
    }

    fun inferTemplate(state: PanelState): PanelTemplate {
        val unit = state.lockedUnits.firstOrNull()
        if (unit == null) return PanelTemplate.NORMAL_UNIT
        return when {
            unit.category == "missile" -> PanelTemplate.MISSILE
            unit.category == "建造中单位" -> PanelTemplate.BUILDING_UNIT
            unit.type == "base" -> PanelTemplate.BASE
            unit.type == "environment" -> PanelTemplate.ENVIRONMENT
            !unit.playerControllable -> PanelTemplate.NON_CONTROLLABLE_UNIT
            else -> PanelTemplate.NORMAL_UNIT
        }
    }

    fun resolveMultiSelect(state: PanelState, multiSelectActive: Boolean, selectedUnits: List<PlacedObject>): PanelState {
        if (multiSelectActive) {
            return PanelState(
                mode = PanelMode.MULTI,
                lockedUnits = selectedUnits,
                template = PanelTemplate.MULTI_SELECT
            )
        }
        return state
    }

    fun isTouchInPanel(state: PanelState, x: Float, y: Float): Boolean {
        if (state.mode == PanelMode.HIDDEN) return false
        return PanelBox.contains(x, y)
    }

    fun handleProgressBarTouch(components: List<PanelComponent>, panelRect: RectF, x: Float, y: Float, action: Int): Boolean {
        var handled = false
        for (comp in components) {
            val hit = when (comp) {
                is HealthBarComponent -> isProgressBarHit(comp, panelRect, x, y)
                is SpeedBarComponent -> isProgressBarHit(comp, panelRect, x, y)
                is LifetimeBarComponent -> isProgressBarHit(comp, panelRect, x, y)
                is StorageBarComponent -> isProgressBarHit(comp, panelRect, x, y)
                is BuildProgressComponent -> isProgressBarHit(comp, panelRect, x, y)
                is BuildReserveComponent -> isProgressBarHit(comp, panelRect, x, y)
                is DynamicProgressBarComponent -> isProgressBarHit(comp, panelRect, x, y)
                else -> false
            }
            when (action) {
                0 -> {
                    if (hit) {
                        when (comp) {
                            is HealthBarComponent -> comp.isPressed = !comp.isPressed
                            is SpeedBarComponent -> comp.isPressed = !comp.isPressed
                            is LifetimeBarComponent -> comp.isPressed = !comp.isPressed
                            is StorageBarComponent -> comp.isPressed = !comp.isPressed
                            is BuildProgressComponent -> comp.isPressed = !comp.isPressed
                            is BuildReserveComponent -> comp.isPressed = !comp.isPressed
                            is DynamicProgressBarComponent -> comp.isPressed = !comp.isPressed
                            else -> {}
                        }
                        handled = true
                    } else {
                        when (comp) {
                            is HealthBarComponent -> comp.isPressed = false
                            is SpeedBarComponent -> comp.isPressed = false
                            is LifetimeBarComponent -> comp.isPressed = false
                            is StorageBarComponent -> comp.isPressed = false
                            is BuildProgressComponent -> comp.isPressed = false
                            is BuildReserveComponent -> comp.isPressed = false
                            is DynamicProgressBarComponent -> comp.isPressed = false
                            else -> {}
                        }
                    }
                }
                1 -> {}
                else -> {
                    when (comp) {
                        is HealthBarComponent -> comp.isPressed = false
                        is SpeedBarComponent -> comp.isPressed = false
                        is LifetimeBarComponent -> comp.isPressed = false
                        is StorageBarComponent -> comp.isPressed = false
                        is BuildProgressComponent -> comp.isPressed = false
                        is BuildReserveComponent -> comp.isPressed = false
                        is DynamicProgressBarComponent -> comp.isPressed = false
                        else -> {}
                    }
                }
            }
        }
        saveProgressBarStates(components)
        return handled
    }

    private fun saveProgressBarStates(components: List<PanelComponent>) {
        progressBarStates.clear()
        for (comp in components) {
            when (comp) {
                is HealthBarComponent -> progressBarStates["H"] = comp.isPressed
                is SpeedBarComponent -> progressBarStates["S"] = comp.isPressed
                is LifetimeBarComponent -> progressBarStates["L"] = comp.isPressed
                is StorageBarComponent -> progressBarStates["ST"] = comp.isPressed
                is BuildProgressComponent -> progressBarStates["B"] = comp.isPressed
                is BuildReserveComponent -> progressBarStates["BR"] = comp.isPressed
                else -> {}
            }
        }
    }

    private fun restoreProgressBarStates(components: List<PanelComponent>) {
        for (comp in components) {
            when (comp) {
                is HealthBarComponent -> comp.isPressed = progressBarStates["H"] ?: false
                is SpeedBarComponent -> comp.isPressed = progressBarStates["S"] ?: false
                is LifetimeBarComponent -> comp.isPressed = progressBarStates["L"] ?: false
                is StorageBarComponent -> comp.isPressed = progressBarStates["ST"] ?: false
                is BuildProgressComponent -> comp.isPressed = progressBarStates["B"] ?: false
                is BuildReserveComponent -> comp.isPressed = progressBarStates["BR"] ?: false
                else -> {}
            }
        }
    }

    private fun isProgressBarHit(comp: PanelComponent, panelRect: RectF, x: Float, y: Float): Boolean {
        val left = panelRect.left + ProgressBarRenderTool.MARGIN_H
        val right = panelRect.right - ProgressBarRenderTool.MARGIN_H
        val top = panelRect.top + comp.layoutY
        val bottom = top + comp.computedHeight
        return x >= left && x <= right && y >= top && y <= bottom
    }

    private fun buildComponents(state: PanelState, vw: Int, context: Context) {
        components.clear()
        val template = state.template ?: inferTemplate(state)
        val templateComponents = TemplateSelector.selectByTemplate(template, state, vw, context)

        val unit = state.lockedUnits.firstOrNull()
        if (unit == null) {
            components.addAll(templateComponents)
            return
        }

        val dynamicComponents = unit.dynamicPanelComponents

        for (comp in templateComponents) {
            components.add(comp)
            if (comp is PanelComponent.Placeholder) {
                // 找到该占位符下所有动态组件
                val matches = dynamicComponents.filter { it.placeholderId == comp.id }
                if (matches.isNotEmpty()) {
                    // 后进来的靠近占位符 → 逆序插入
                    for (i in matches.indices.reversed()) {
                        components.add(matches[i])
                    }
                }
            }
        }
    }

}