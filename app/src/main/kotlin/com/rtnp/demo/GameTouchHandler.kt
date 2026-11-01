// app/src/main/kotlin/com/rtnp/demo/GameTouchHandler.kt
package com.rtnp.demo

import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.logic.BuildManager
import com.rtnp.demo.logic.DockingSystem
import com.rtnp.demo.logic.HexGridManager
import com.rtnp.demo.logic.MultiMoveCoordinator
import com.rtnp.demo.render.RenderControl
import com.rtnp.demo.strategy.StrategyToolLayer
import com.rtnp.demo.testing.DevTools
import com.rtnp.demo.ui.*
import com.rtnp.demo.ui.action.ActionPlatform
import com.rtnp.demo.ui.panel.RightPanel
import com.rtnp.demo.strategy.TileSelectorTool
import com.rtnp.demo.ui.action.ActionFlowController
import com.rtnp.demo.ui.bottombar.tools.BottomBarItemPanelProxy
import com.rtnp.demo.ui.tools.ItemGridPanelTool
import com.rtnp.demo.ui.tools.clear.selector.CommonSliderTool

class GameTouchHandler(private val gameView: GameView) {

    var isMultiSelectMode = false
    private var longPressDetected = false
    private var downX = 0f
    private var downY = 0f
    private val longPressHandler = Handler(Looper.getMainLooper())
    private var longPressRunnable: Runnable? = null
    var blockGestureScroll = false
    private var hasMoved = false
    var blockMapTouch = false
    var ignoreMapTouchForMultiMove = false
    var firstClickTarget: PlacedObject? = null
    private var buttonTouched = false
    private var touchedButtonType = 0

    private var touchFocus: String? = null

    companion object {
        const val LONG_PRESS_TIME = 400L
        const val LONG_PRESS_MAX_MOVE = 80

        private const val FOCUS_ACTION_PLATFORM = "action_platform"
        private const val FOCUS_RIGHT_PANEL = "right_panel"
        private const val FOCUS_FLEET_PANEL = "fleet_panel"
        private const val FOCUS_BOTTOM_BAR = "bottom_bar"
        private const val FOCUS_INVENTORY = "inventory"
        private const val FOCUS_MULTI_SELECT_BUTTONS = "multi_select_buttons"
        private const val FOCUS_TOAST = "toast"
        private const val FOCUS_TILE_SELECTOR = "tile_selector"
        private const val FOCUS_INFO_BOX = "info_box"
    }

    fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        val action = event.actionMasked

        // 每次新的触摸序列开始时，重置手势检测器
        if (action == MotionEvent.ACTION_DOWN) {
            val cancelEvent = MotionEvent.obtain(
                event.downTime, event.eventTime, MotionEvent.ACTION_CANCEL,
                event.x, event.y, event.metaState
            )
            try {
                gameView.scaleDetector?.onTouchEvent(cancelEvent)
                gameView.gestureDetector?.onTouchEvent(cancelEvent)
            } catch (_: Exception) {}
            cancelEvent.recycle()
        }

        if (RenderControl.touchDisabledMap) {
            val scaleDetector = gameView.scaleDetector
            val gestureDetector = gameView.gestureDetector
            var result = scaleDetector?.onTouchEvent(event) ?: false
            try { result = gestureDetector?.onTouchEvent(event) ?: false || result } catch (_: Exception) {}
            return result || gameView.onTouchEvent(event)
        }

        if (gameView.isPaused) {
            if (!RenderControl.touchDisabledPauseMenu) {
                return gameView.pauseMenu?.onTouchEvent(event)?.also { if (it) gameView.invalidate() } ?: false
            }
            return true
        }

        // 通用滑块（测试用例）：拖动中 或 DOWN 命中按钮
        gameView.testSlider?.let { slider ->
            if (slider.isDragging() || action == MotionEvent.ACTION_DOWN) {
                if (slider.onTouchEvent(event)) {
                    gameView.invalidate()
                    return true
                }
            }
        }

        if (ActionPlatform.onTouch(x, y, action)) {
            if (action == MotionEvent.ACTION_DOWN) touchFocus = FOCUS_ACTION_PLATFORM
            gameView.invalidate()
            return true
        }

        // Toast 触摸
        if (action == MotionEvent.ACTION_DOWN) {
            if (ToastTool.globalInstance?.onTouchDown(x, y) == true) {
                touchFocus = FOCUS_TOAST
                return true
            }
        }

        // 信息框触摸
        if (action == MotionEvent.ACTION_DOWN && InfoBoxTool.onTouchEvent(event)) {
            touchFocus = FOCUS_INFO_BOX
            gameView.invalidate()
            return true
        }

        // 焦点优先
        if (touchFocus != null && action != MotionEvent.ACTION_DOWN) {
            return handleFocusTouch(event, x, y, action)
        }
        if (touchFocus != null && action == MotionEvent.ACTION_DOWN) {
            touchFocus = null
        }

        // 悬浮窗触摸
        if (FloatingWindowTool.isShowing()) {
            if (FloatingWindowTool.onTouchEvent(event)) {
                gameView.invalidate()
                return true
            }
        }

        if (!RenderControl.touchDisabledDevTools && DevTools.onTouchEvent(event)) return true

        val unitSystem = gameView.unitSystem ?: return false
        val itemSystem = gameView.itemSystem ?: return false

        if (StrategyToolLayer.isActive) {
            when (action) {
                MotionEvent.ACTION_DOWN -> StrategyToolLayer.onTouchDown(x, y)
                MotionEvent.ACTION_UP -> {
                    if (StrategyToolLayer.onTouchUp(x, y) { sx, sy ->
                            Pair(gameView.screenToWorldX(sx), gameView.screenToWorldY(sy))
                        }) {
                        gameView.invalidate()
                        return true
                    }
                }
            }
        }

        if (TileSelectorTool.isActive) {
            when (action) {
                MotionEvent.ACTION_DOWN -> {
                    gameView.gestureDetector?.onTouchEvent(event)
                    if (TileSelectorTool.onButtonTouch(x, y, action)) {
                        touchFocus = FOCUS_TILE_SELECTOR
                        gameView.invalidate()
                        return true
                    }
                    TileSelectorTool.onTouchDown(x, y)
                    touchFocus = FOCUS_TILE_SELECTOR
                    gameView.invalidate()
                }
            }
        }

        if (!RenderControl.touchDisabledUnitInfoPanel &&
            action == MotionEvent.ACTION_DOWN && RightPanel.onTouch(x, y, action)) {
            touchFocus = FOCUS_RIGHT_PANEL
            gameView.invalidate()
            return true
        }

        if (!RenderControl.touchDisabledFleetPanel && itemSystem.isFleetPanelContains(x, y)) {
            if (action == MotionEvent.ACTION_DOWN && itemSystem.onTouchEvent(event)) {
                touchFocus = FOCUS_FLEET_PANEL
                gameView.invalidate()
                return true
            }
            return true
        }
        
        if (RenderControl.useNewBottomBar) {
            if (!RenderControl.touchDisabledBottomBar) {
                val nb = gameView.bottomBarV2
                if (nb != null && action == MotionEvent.ACTION_DOWN && nb.contains(x, y)) {
                    touchFocus = FOCUS_BOTTOM_BAR
                    nb.onTouchEvent(event)
                    gameView.invalidate()
                    return true
                }
            }
        } else if (!RenderControl.touchDisabledBottomBar) {
            val bottomBarRect = itemSystem.bottomBarRect
            if (bottomBarRect != null && bottomBarRect.contains(x.toInt(), y.toInt())) {
                if (action == MotionEvent.ACTION_DOWN && itemSystem.onTouchEvent(event)) {
                    touchFocus = FOCUS_BOTTOM_BAR
                    gameView.invalidate()
                    return true
                }
                return true
            }
        }

        if (itemSystem.isBuildDragMode) {
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                itemSystem.resetBuildDragMode()
            }
        }

        if (!RenderControl.touchDisabledActionConfirmButtons &&
            gameView.actionConfirmButtons?.onTouchEvent(event) == true) {
            gameView.invalidate(); return true
        }

        if (isMultiSelectMode) {
            if (buttonTouched) {
                if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                    touchFocus = null
                    when (touchedButtonType) {
                        1 -> {
                            firstClickTarget = null; isMultiSelectMode = false
                            gameView.multiSelectManager?.setActive(false)
                            gameView.multiSelectManager?.clearSelection()
                            unitSystem.deselectUnit()
                            itemSystem.setMultiSelectMode(false)
                        }
                        2 -> {
                            gameView.multiSelectManager?.setFormationMode(!(gameView.multiSelectManager?.isFormationMode() ?: false))
                        }
                    }
                    buttonTouched = false; touchedButtonType = 0
                    gameView.invalidate()
                }
                return true
            }
            when (action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = x; downY = y; longPressDetected = false; hasMoved = false
                    val r = Runnable {
                        if (!longPressDetected) { longPressDetected = true; blockGestureScroll = true; gameView.multiSelectManager?.startSelection(downX, downY); gameView.invalidate() }
                    }
                    longPressRunnable = r; longPressHandler.postDelayed(r, LONG_PRESS_TIME)
                }
                MotionEvent.ACTION_MOVE -> {
                    if (longPressDetected) { gameView.multiSelectManager?.updateSelection(x, y); gameView.invalidate(); return true }
                    else { if (Math.hypot((x - downX).toDouble(), (y - downY).toDouble()) > LONG_PRESS_MAX_MOVE) { val r = longPressRunnable; if (r != null) longPressHandler.removeCallbacks(r); hasMoved = true } }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val r = longPressRunnable; if (r != null) longPressHandler.removeCallbacks(r)
                    if (longPressDetected) { gameView.multiSelectManager?.endSelection(false); gameView.postDelayed({ gameView.multiSelectManager?.cancelSelection(); blockGestureScroll = false; gameView.invalidate() }, 50); gameView.invalidate() }
                    longPressDetected = false; hasMoved = false
                }
            }
        }

        if (!RenderControl.touchDisabledInventoryPanel && itemSystem.onTouchEvent(event)) {
            if (action == MotionEvent.ACTION_DOWN) touchFocus = FOCUS_INVENTORY
            gameView.invalidate(); return true
        }
        if (isMultiSelectMode && longPressDetected) return true

        val scaleDetector = gameView.scaleDetector
        val gestureDetector = gameView.gestureDetector
        var result = scaleDetector?.onTouchEvent(event) ?: false
        try { result = gestureDetector?.onTouchEvent(event) ?: false || result } catch (_: Exception) {}
        return result || gameView.onTouchEvent(event)
    }

    private fun handleFocusTouch(event: MotionEvent, x: Float, y: Float, action: Int): Boolean {
        val focus = touchFocus ?: return false

        return when (focus) {
            FOCUS_ACTION_PLATFORM -> {
                if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) ActionPlatform.onTouch(x, y, action)
                gameView.invalidate(); true
            }
            FOCUS_INFO_BOX -> {
                if (InfoBoxTool.onTouchEvent(event)) {
                    if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                        touchFocus = null
                    }
                    gameView.invalidate()
                    true
                } else {
                    touchFocus = null
                    false
                }
            }
            FOCUS_RIGHT_PANEL -> {
                if (!RenderControl.touchDisabledUnitInfoPanel) RightPanel.onTouch(x, y, action)
                gameView.invalidate(); true
            }
            FOCUS_FLEET_PANEL -> gameView.itemSystem?.onTouchEvent(event)?.also { if (it) gameView.invalidate() } ?: false
            FOCUS_BOTTOM_BAR -> {
                if (RenderControl.useNewBottomBar) {
                    gameView.bottomBarV2?.onTouchEvent(event)
                    if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                        touchFocus = null
                    }
                    gameView.invalidate()
                    true
                } else {
                    gameView.itemSystem?.onTouchEvent(event)?.also { if (it) gameView.invalidate() } ?: false
                }
            }
            FOCUS_INVENTORY -> gameView.itemSystem?.onTouchEvent(event)?.also { if (it) gameView.invalidate() } ?: false
            FOCUS_MULTI_SELECT_BUTTONS -> true
            FOCUS_TOAST -> {
                when (action) {
                    MotionEvent.ACTION_MOVE -> ToastTool.globalInstance?.onTouchMove(x, y)
                    MotionEvent.ACTION_UP -> {
                        ToastTool.globalInstance?.onTouchUp(x, y)
                        touchFocus = null
                        gameView.invalidate()
                    }
                    MotionEvent.ACTION_CANCEL -> {
                        ToastTool.globalInstance?.cancelTouch()
                        touchFocus = null
                    }
                }
                true
            }
            FOCUS_TILE_SELECTOR -> {
                when (action) {
                    MotionEvent.ACTION_DOWN -> {
                        gameView.gestureDetector?.onTouchEvent(event)
                    }
                    MotionEvent.ACTION_POINTER_DOWN -> {
                        val cancelEvent = MotionEvent.obtain(
                            event.downTime, event.eventTime, MotionEvent.ACTION_CANCEL,
                            event.x, event.y, event.metaState
                        )
                        gameView.gestureDetector?.onTouchEvent(cancelEvent)
                        cancelEvent.recycle()
                        gameView.scaleDetector?.onTouchEvent(event)
                    }
                    MotionEvent.ACTION_POINTER_UP -> {
                        val cancelEvent = MotionEvent.obtain(
                            event.downTime, event.eventTime, MotionEvent.ACTION_CANCEL,
                            event.x, event.y, event.metaState
                        )
                        gameView.scaleDetector?.onTouchEvent(cancelEvent)
                        cancelEvent.recycle()
                        gameView.gestureDetector?.onTouchEvent(event)
                    }
                    MotionEvent.ACTION_UP -> {
                        touchFocus = null
                        if (TileSelectorTool.onButtonTouch(x, y, action)) {
                            gameView.invalidate()
                            return true
                        }
                        TileSelectorTool.onTouchUp(x, y, { sx, sy ->
                            Pair(gameView.screenToWorldX(sx), gameView.screenToWorldY(sy))
                        }, gameView.zoom)
                        gameView.invalidate()
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (event.pointerCount >= 2) {
                            gameView.scaleDetector?.onTouchEvent(event)
                        } else {
                            gameView.gestureDetector?.onTouchEvent(event)
                        }
                        gameView.invalidate()
                        return true
                    }
                }
                true
            }
            else -> false
        }
    }

    fun handleMapTap(screenX: Float, screenY: Float) {
        if (blockMapTouch || ignoreMapTouchForMultiMove) return

        val unitSystem = gameView.unitSystem ?: return
        val cameraX = gameView.cameraX; val cameraY = gameView.cameraY; val zoom = gameView.zoom
        val viewWidth = gameView.width; val viewHeight = gameView.height
        val worldX = (screenX - viewWidth / 2f) / zoom + cameraX
        val worldY = (screenY - viewHeight / 2f) / zoom + cameraY

        if (ActionFlowController.handleMapTap(worldX, worldY)) {
            gameView.postInvalidateOnAnimation(); return
        }

        if (isMultiSelectMode) {
            val mm = gameView.multiSelectManager ?: return
            if (mm.isFormationMode()) {
                if (HexGridManager.isPointInHexGrid(worldX, worldY)) {
                    var dockTarget: PlacedObject? = null
                    for (obj in unitSystem.placedObjects) {
                        if (obj.isDocked) continue
                        if (DockingSystem.getSlotCount(obj) <= 0) continue
                        val radius = DockingSystem.getDockRadius(obj)
                        val dx = worldX - obj.worldX; val dy = worldY - obj.worldY
                        if (dx * dx + dy * dy <= radius * radius) { dockTarget = obj; break }
                    }
                    if (dockTarget != null) {
                        val count = MultiMoveCoordinator.startBatchDock(unitSystem, dockTarget)
                        if (count > 0) unitSystem.showTargetIndicator(worldX, worldY, Color.CYAN)
                        else unitSystem.showActionDeniedIndicator(worldX, worldY)
                    } else unitSystem.showActionDeniedIndicator(worldX, worldY)
                } else unitSystem.showActionDeniedIndicator(worldX, worldY)
                gameView.postInvalidateOnAnimation(); return
            } else {
                mm.clearSelection(); unitSystem.deselectUnit()
                gameView.itemSystem?.updateSelectedUnitForDetail(null); firstClickTarget = null
                gameView.postInvalidateOnAnimation(); return
            }
        }

        if (unitSystem.isActionLocked && unitSystem.selectedUnit != null) {
            if (!unitSystem.isUnitMovingOrStopping) {
                if (HexGridManager.isPointInHexGrid(worldX, worldY)) {
                    // 已废弃，不再处理
                } else {
                    unitSystem.showActionDeniedIndicator(worldX, worldY)
                    gameView.postInvalidateOnAnimation(); return
                }
            } else {
                unitSystem.showActionDeniedIndicator(worldX, worldY)
                gameView.postInvalidateOnAnimation(); return
            }
        }

        val itemSys = gameView.itemSystem
        if (itemSys != null && itemSys.isPlacementMode) {
            if (HexGridManager.isPointInHexGrid(worldX, worldY)) {
                val item = itemSys.selectedItem
                if (item != null) {
                    if (itemSys.isBuildOpen) BuildManager.addPreview(worldX, worldY, item)
                    else unitSystem.addObjectFromItem(item, worldX, worldY)
                }
            }
            gameView.postInvalidateOnAnimation(); return
        }
        // 物品/建筑放置（面板打开且有选中时）
        if (BottomBarItemPanelProxy.isShowing()) {
            val selected = BottomBarItemPanelProxy.getSelectedItems().firstOrNull()
            if (selected != null) {
                if (HexGridManager.isPointInHexGrid(worldX, worldY)) {
                    when (BottomBarItemPanelProxy.getMode()) {
                        ItemGridPanelTool.PanelMode.ITEM ->
                            unitSystem.addObjectFromItem(selected, worldX, worldY)
                        ItemGridPanelTool.PanelMode.BUILD ->
                            BuildManager.addPreview(worldX, worldY, selected)
                    }
                }
                gameView.postInvalidateOnAnimation()
                return
            }
        }

        unitSystem.directSelectTap(worldX, worldY)
        gameView.itemSystem?.updateSelectedUnitForDetail(unitSystem.selectedUnit)
        if (unitSystem.selectedUnit != null) { gameView.itemSystem?.closeInventoryIfOpen(); gameView.cameraX = unitSystem.selectedUnit.worldX; gameView.cameraY = unitSystem.selectedUnit.worldY }
        gameView.postInvalidateOnAnimation()
    }
}