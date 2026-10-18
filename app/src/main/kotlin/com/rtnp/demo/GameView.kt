// app/src/main/kotlin/com/rtnp/demo/GameView.kt
package com.rtnp.demo

import android.app.Activity
import android.content.Context
import android.graphics.*
import android.os.Handler
import android.view.*
import com.rtnp.demo.camera.CameraAnimator
import com.rtnp.demo.combat.WeaponConstants
import com.rtnp.demo.image.ImageManager
import com.rtnp.demo.logic.*
import com.rtnp.demo.render.*
import com.rtnp.demo.strategy.StrategyManager
import com.rtnp.demo.strategy.StrategyToolLayer
import com.rtnp.demo.strategy.TileSelectorTool
import com.rtnp.demo.testing.*
import com.rtnp.demo.ui.*
import com.rtnp.demo.ui.systempanel.*
import com.rtnp.demo.ui.action.ActionPlatform
import com.rtnp.demo.camera.CameraController
import com.rtnp.demo.ui.panel.RightPanel
import com.rtnp.demo.ui.action.ActionFlowController
import com.rtnp.demo.ui.panel.tool.DeployButtonTool
import com.rtnp.demo.ui.bottombar.BottomBarV2
import com.rtnp.demo.ui.bottombar.BottomBarGlobal
import com.rtnp.demo.ui.bottombar.tools.BottomBarItemPanelProxy
import com.rtnp.demo.ui.bottombar.tools.BottomBarUnitInfoPanelProxy

class GameView(context: Context) : View(context), InventoryPanel.AnimationHost {

    var cameraX: Float = WORLD_WIDTH / 2f
    var cameraY: Float = WORLD_HEIGHT / 2f
    var zoom: Float = 1f
    var isPaused: Boolean = false
    var devPaused: Boolean = false
    var itemSystem: ItemSystem? = null
        private set
    var unitSystem: UnitSystem? = null
        private set
    var multiSelectManager: MultiSelectManager? = null
        private set
    var pauseMenu: PauseMenu? = null
        private set
    var actionConfirmButtons: ActionConfirmButtons? = null
        private set
    var gestureDetector: GestureDetector? = null
        private set
    var scaleDetector: ScaleGestureDetector? = null
        private set
    var toastTool: ToastTool = ToastTool()
    // 字段
    var bottomBarV2: BottomBarV2? = null
        private set

    private var paint = Paint()
    private var itemTouchActive = false

    val touchHandler: GameTouchHandler = GameTouchHandler(this)

    private var gameRenderer: com.rtnp.demo.gpu.GameRenderer? = null

    private var lastFrameTime = 0L

    companion object {
        const val WORLD_WIDTH = 3000f
        const val WORLD_HEIGHT = 5000f
        const val HEX_SIDE = 1000f
        val HEX_HORIZ: Float = (Math.sqrt(3.0) * HEX_SIDE).toFloat()
        val HEX_VERT: Float = HEX_SIDE * 1.5f
    }

    override fun requestRedraw() {
        postInvalidateOnAnimation()
    }

    init {
        paint.isAntiAlias = true
        setBackgroundColor(Color.TRANSPARENT)

        toastTool = ToastTool()
        ToastTool.globalInstance = toastTool

        unitSystem = UnitSystem(context).also {
            it.setOnRedrawListener { postInvalidateOnAnimation() }
        }

        multiSelectManager = MultiSelectManager(unitSystem!!)

        itemSystem = ItemSystem(this, unitSystem!!, context).also {
            it.setMultiSelectManager(multiSelectManager!!)
            it.setOnFleetSelectListener { unit ->
                cameraX = unit.worldX
                cameraY = unit.worldY
                postInvalidateOnAnimation()
            }
            it.setPauseListener {
                isPaused = true
                it.closeInventoryIfOpen()
                unitSystem?.deselectUnit()
                unitSystem?.setShowDetails(false)
                invalidate()
            }
        }

        actionConfirmButtons = ActionConfirmButtons().apply {
            setOnActionListener(object : ActionConfirmButtons.OnActionListener {
                override fun onCancel() {
                    BuildManager.cancelBuild()
                    postInvalidateOnAnimation()
                }
                override fun onConfirm() {
                    BuildManager.confirmBuild()
                    postInvalidateOnAnimation()
                }
            })
        }

        unitSystem?.setMultiSelectManager(multiSelectManager!!)

        pauseMenu = PauseMenu(context, object : PauseMenu.OnMenuActionListener {
            override fun onResume() { isPaused = false }
            override fun onQuitToMain() {
                isPaused = true
                Handler().postDelayed({
                    (context as? MainActivity)?.showMainMenu()
                }, 200)
            }
            override fun onSettings() {
                (context as? MainActivity)?.showSettings(this@GameView)
            }
        })

        gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            @Suppress("OVERRIDE_DEPRECATION")
            override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
                if (isPaused || scaleDetector?.isInProgress == true || itemSystem?.isPlacementMode == true || itemTouchActive) return false
                if (touchHandler.isMultiSelectMode && (multiSelectManager?.isSelecting() == true || touchHandler.blockGestureScroll)) return false

                val (newX, newY) = CameraController.scroll(cameraX, cameraY, distanceX, distanceY, zoom)
                cameraX = newX
                cameraY = newY

                multiSelectManager?.updateCamera(cameraX, cameraY, zoom, width, height)
                syncCameraToGpu()
                postInvalidateOnAnimation()
                return true
            }

            override fun onSingleTapUp(e: MotionEvent): Boolean {
                if (!isPaused && unitSystem?.isShowDetails == false) {
                    touchHandler.handleMapTap(e.x, e.y)
                }
                return true
            }
        })

        scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                if (isPaused || unitSystem?.isShowDetails == true || itemTouchActive) return true

                val (newX, newY, newZoom) = CameraController.scale(
                    cameraX, cameraY,
                    detector.focusX, detector.focusY,
                    detector.scaleFactor, zoom,
                    width, height
                )
                cameraX = newX
                cameraY = newY
                zoom = newZoom

                multiSelectManager?.updateCamera(cameraX, cameraY, zoom, width, height)
                syncCameraToGpu()
                return true
            }
        })

        CameraAnimator.init(object : CameraAnimator.CameraState {
            override fun getCameraX() = cameraX
            override fun setCameraX(value: Float) { cameraX = value }
            override fun getCameraY() = cameraY
            override fun setCameraY(value: Float) { cameraY = value }
            override fun getZoom() = zoom
            override fun setZoom(value: Float) { zoom = value }
        }, { postInvalidateOnAnimation() })

        val us = unitSystem!!
        StrategyManager.init(context, us)
        StrategyManager.getTools()?.showToast = { text ->
            toastTool.show(text)
            postInvalidateOnAnimation()
        }

        HexGridManager.init(WORLD_WIDTH / 2f, WORLD_HEIGHT / 2f, HexGridManager.maxLayer, HEX_SIDE)

        DevTools.init(us, context as Activity)
        DevTools.onPauseToggleListener = object : DevTools.OnPauseToggleListener {
            override fun onPauseToggle(paused: Boolean) { devPaused = paused }
        }
        StrategyToolLayer.setCoordinateConverter { wx, wy ->
            val screenX = (wx - cameraX) * zoom + width / 2f
            val screenY = (wy - cameraY) * zoom + height / 2f
            Pair(screenX, screenY)
        }
        TileSelectorTool.setCoordinateConverter { wx, wy ->
            val screenX = (wx - cameraX) * zoom + width / 2f
            val screenY = (wy - cameraY) * zoom + height / 2f
            Pair(screenX, screenY)
        }
        bottomBarV2 = BottomBarV2(context)
        BottomBarGlobal.onPauseRequested = {
            isPaused = true
            /*BottomBarItemPanelProxy.hide()
            BottomBarUnitInfoPanelProxy.hide()*/
            unitSystem?.deselectUnit()
            unitSystem?.setShowDetails(false)
            invalidate()
        }
        RightPanel.init(context, unitSystem!!)
        ActionFlowController.init(unitSystem!!)
        RightPanel.unlock()
        DeployButtonTool.resetAll()
        InfoBoxTool.init(context)
        FloatingWindowTool.init(context)
        
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        ActionFlowController.updateScreenSize(w, h)
        itemSystem?.updateScreenSize(w, h)

        pauseMenu?.setScreenSize(w, h)
        multiSelectManager?.updateCamera(cameraX, cameraY, zoom, w, h)
        val bottomBarLeft = w - (w / 2)
        actionConfirmButtons?.layout(w, h, bottomBarLeft)
        DevTools.updateScreenSize(w, h)
        toastTool.updateScreenSize(w, h)
        unitSystem?.screenWidth = w
        unitSystem?.screenHeight = h

        FloatingWindowTool.onSizeChanged(w, h)
        SystemPanelEntry.onSizeChanged(w, h)
        
        bottomBarV2?.setScreenSize(w, h)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return touchHandler.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        syncCameraToGpu()

        val now = System.currentTimeMillis()
        if (lastFrameTime > 0) {
            val diff = now - lastFrameTime
            (diff / 1000f).coerceAtMost(0.1f)
        }
        ActionFlowController.update()
        lastFrameTime = now

        if (!isPaused && !devPaused) {
            unitSystem?.updateMovement()
            if (itemSystem?.isFleetOpen == true) {
                itemSystem?.refreshFleetPanel()
            }
        }

        gameRenderer?.updateUnits(unitSystem?.placedObjects ?: emptyList())
        gameRenderer?.unitSystem = unitSystem
        // 新底部栏（常态 UI，位于弹层之下）
        if (RenderControl.showBottomBar && RenderControl.useNewBottomBar) {
            bottomBarV2?.draw(canvas)
        }

        ActionPlatform.draw(canvas, width)
        StrategyToolLayer.drawButtons(canvas)
        if (TileSelectorTool.isActive) {
            TileSelectorTool.drawButtons(canvas)
        }
        if (RenderControl.showInventoryPanel) itemSystem?.draw(canvas)

        if (RenderControl.showUnitInfoPanel) {
            RightPanel.draw(canvas, width, height)
        }

        if (RenderControl.showActionConfirmButtons) {
            actionConfirmButtons?.visible = !isPaused && BuildManager.getPreviews().isNotEmpty()
            actionConfirmButtons?.draw(canvas)
        }
        toastTool.draw(canvas)

        WarningMessage.draw(canvas)
        FloatingWindowTool.draw(canvas)
        InfoBoxTool.draw(canvas)
        if (RenderControl.showDebugOverlay) unitSystem?.let { DebugOverlay.draw(canvas, zoom, it.objectCount, it) }
        if (RenderControl.showPauseMenu && isPaused) pauseMenu?.draw(canvas)
        if (RenderControl.showDevTools) DevTools.draw(canvas)

        postInvalidateOnAnimation()
    }

    fun showToast(text: String) {
        toastTool.show(text)
        postInvalidateOnAnimation()
    }

    fun hideToast() {
        toastTool.hide()
        postInvalidateOnAnimation()
    }

    fun screenToWorldX(sx: Float): Float {
        return CameraController.screenToWorldX(sx, cameraX, zoom, width)
    }

    fun screenToWorldY(sy: Float): Float {
        return CameraController.screenToWorldY(sy, cameraY, zoom, height)
    }

    fun setGpuRenderer(renderer: com.rtnp.demo.gpu.GameRenderer) {
        this.gameRenderer = renderer
        syncCameraToGpu()
    }

    private fun syncCameraToGpu() {
        gameRenderer?.updateCamera(cameraX, cameraY, zoom)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
    }
}