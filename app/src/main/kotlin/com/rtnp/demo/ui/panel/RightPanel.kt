// app/src/main/kotlin/com/rtnp/demo/ui/panel/RightPanel.kt
package com.rtnp.demo.ui.panel

import android.content.Context
import android.graphics.Canvas
import com.rtnp.demo.UnitSystem
import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.ui.panel.data.InteractiveButtonComponent
import com.rtnp.demo.ui.panel.data.PanelComponent
import com.rtnp.demo.logger.Logger
import com.rtnp.demo.render.RenderControl
import com.rtnp.demo.ui.panel.tool.*

object RightPanel {

    private var state = PanelState()
    private var customLayout: List<LayoutItem>? = null
    private var forcedTemplate: PanelTemplate? = null
    private var context: Context? = null
    private var unitSystem: UnitSystem? = null

    private var currentComponents: List<PanelComponent> = emptyList()
    private var pressedButtonIndex: Int = -1

    fun init(context: Context, us: UnitSystem) {
        this.context = context
        this.unitSystem = us
    }

    fun lockUnit(unit: PlacedObject?) {
        if (unitSystem?.multiSelectManager?.isActive() == true) return
        if (unit == null) {
            unlock()
            return
        }
        val wasHidden = state.mode == PanelMode.HIDDEN
        state = PanelState(
            mode = PanelMode.NORMAL,
            lockedUnits = listOf(unit),
            customLayout = customLayout,
            showDetails = false,
            template = forcedTemplate
        )
        if (wasHidden) {
            PanelAnimTool.start()
        }
        RenderControl.showBottomBar = false
        RenderControl.touchDisabledBottomBar = true
    }

    fun lockMultiUnits(units: Set<PlacedObject>) {
        val wasHidden = state.mode == PanelMode.HIDDEN
        state = PanelState(
            mode = PanelMode.MULTI,
            lockedUnits = units.toList()
        )
        if (wasHidden) {
            PanelAnimTool.start()
        }
    }

    fun showDetails() {
        state = state.copy(showDetails = true)
    }

    fun hideDetails() {
        state = state.copy(showDetails = false)
    }

    fun useCustomLayout(layout: List<LayoutItem>) {
        customLayout = layout
    }

    fun useTemplate(template: PanelTemplate?) {
        forcedTemplate = template
    }

    fun getUnitSystem(): UnitSystem? = unitSystem

    fun draw(canvas: Canvas, vw: Int, vh: Int) {
        if (state.mode == PanelMode.HIDDEN) return
        val ctx = context ?: return

        val components = PanelLogic.process(state, vw, vh, ctx)
        currentComponents = components

        val buttons = components.filterIsInstance<InteractiveButtonComponent>()
        EnergyRingTool.update(UnitSystem.FIXED_DT)
        WeaponButtonTool.updateCooldowns(buttons)
        ButtonAnimTool.update(buttons.size)
        for (i in buttons.indices) {
            buttons[i].animScale = ButtonAnimTool.getScale(i)
        }
        // RightPanel.draw 中
        val unit = unitSystem?.selectedUnit
        if (unit != null) {
            StrategyButtonTool.updateCooldowns(buttons, unit)
            StrategyButtonTool.updateEnergy(buttons, unit)
            FactoryButtonTool.updateFactoryButtonData(buttons, unit)
            ShieldButtonTool.updateShieldButtonData(buttons, unit)   // ★ 新增
        }

        PanelAnimTool.draw(canvas, components, vw, vh) { c, comps ->
            PanelRenderer.render(c, comps)
        }
        
    }

    fun unlock() {
        state = PanelState()
        PanelLogic.clear()
        PanelAnimTool.stop()
        unitSystem?.deselectUnit()
        RenderControl.showBottomBar = true
        RenderControl.touchDisabledBottomBar = false
    }

    fun onTouch(x: Float, y: Float, action: Int): Boolean {
        if (state.mode == PanelMode.HIDDEN) return false

        val buttons = currentComponents.filterIsInstance<InteractiveButtonComponent>()
        val panelRect = PanelBox.rect()

        when (action) {
            0 -> {
                for (i in buttons.indices) {
                    val btn = buttons[i]
                    if (isButtonHit(btn, panelRect, x, y)) {
                        pressedButtonIndex = i
                        btn.onPressed?.invoke(i)
                        return true
                    }
                }
                if (PanelLogic.handleProgressBarTouch(currentComponents, panelRect, x, y, action)) {
                    return true
                }
                if (panelRect.contains(x, y)) {
                    return true  // 面板内的空白区域也消费事件
                }
            }
            1 -> {
                if (pressedButtonIndex >= 0 && pressedButtonIndex < buttons.size) {
                    val btn = buttons[pressedButtonIndex]
                    val hit = isButtonHit(btn, panelRect, x, y)
                    val shouldFire = btn.onReleased?.invoke(pressedButtonIndex) ?: false
                    if (hit && shouldFire) {
                        btn.onClick?.invoke()
                    }
                    pressedButtonIndex = -1
                    return true
                }
                PanelLogic.handleProgressBarTouch(currentComponents, panelRect, x, y, action)
                if (panelRect.contains(x, y)) {
                    return true  // 面板内的空白区域也消费事件
                }
            }
            3 -> {
                if (pressedButtonIndex >= 0 && pressedButtonIndex < buttons.size) {
                    val btn = buttons[pressedButtonIndex]
                    btn.onCancelled?.invoke(pressedButtonIndex)
                    pressedButtonIndex = -1
                }
                PanelLogic.handleProgressBarTouch(currentComponents, panelRect, x, y, action)
                if (panelRect.contains(x, y)) {
                    return true  // 面板内的空白区域也消费事件
                }
            }
        }
        return false
    }

    private fun isButtonHit(btn: InteractiveButtonComponent, panelRect: android.graphics.RectF, x: Float, y: Float): Boolean {
        val halfWidth = btn.width / 2f
        val centerX = panelRect.centerX()
        val left = centerX - halfWidth
        val right = centerX + halfWidth
        val top = panelRect.top + btn.layoutY
        val bottom = top + btn.computedHeight
        return x >= left && x <= right && y >= top && y <= bottom
    }

    fun getCurrentState(): PanelState = state

    fun applyState(newState: PanelState) {
        state = newState
    }

    fun isInPanel(x: Float, y: Float): Boolean {
        return PanelLogic.isTouchInPanel(state, x, y)
    }
}