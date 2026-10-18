// app/src/main/kotlin/com/rtnp/demo/render/RenderControl.kt
package com.rtnp.demo.render

object RenderControl {

    // ==================== 渲染开关 ====================
    var showUnitInfoPanel: Boolean = true
    var showFleetPanel: Boolean = true
    var showBottomBar: Boolean = true
    var showHexGrid: Boolean = true
    var showUnits: Boolean = true
    var showBullets: Boolean = true
    var showMissiles: Boolean = true
    var showEffects: Boolean = true
    var showHealthIndicators: Boolean = true
    var showSelectionIndicator: Boolean = true
    var showBackground: Boolean = true
    var showDebugOverlay: Boolean = true
    var showDevTools: Boolean = true
    var showInventoryPanel: Boolean = true
    var showActionConfirmButtons: Boolean = true
    var showPauseMenu: Boolean = true
    var showDialogs: Boolean = true

    // ==================== 触摸失效开关 ====================
    var touchDisabledUnitInfoPanel: Boolean = false
    var touchDisabledFleetPanel: Boolean = false
    var touchDisabledBottomBar: Boolean = false
    var touchDisabledInventoryPanel: Boolean = false
    var touchDisabledActionConfirmButtons: Boolean = false
    var touchDisabledPauseMenu: Boolean = false
    var touchDisabledDialogs: Boolean = false
    var touchDisabledDevTools: Boolean = false
    var touchDisabledMap: Boolean = false
    var touchDisabledMultiSelectButton: Boolean = false
    var hideStopButtonForAntimatter: Boolean = false
    
    
    // ==================== 新旧系统切换 ====================
    var useNewBottomBar: Boolean = true

    // ==================== 便捷方法 ====================
    fun hideAllUI() {
        showUnitInfoPanel = false
        showFleetPanel = false
        showBottomBar = false
        showDebugOverlay = false
        showDevTools = false
        showInventoryPanel = false
        showActionConfirmButtons = false
        showPauseMenu = false
        showDialogs = false
    }

    fun showAllUI() {
        showUnitInfoPanel = true
        showFleetPanel = true
        showBottomBar = true
        showDebugOverlay = true
        showDevTools = true
        showInventoryPanel = true
        showActionConfirmButtons = true
        showPauseMenu = true
        showDialogs = true
    }

    fun hideAllWorld() {
        showHexGrid = false
        showUnits = false
        showBullets = false
        showMissiles = false
        showEffects = false
        showHealthIndicators = false
        showSelectionIndicator = false
        showBackground = false
    }

    fun showAllWorld() {
        showHexGrid = true
        showUnits = true
        showBullets = true
        showMissiles = true
        showEffects = true
        showHealthIndicators = true
        showSelectionIndicator = true
        showBackground = true
    }

    fun disableAllUITouch() {
        touchDisabledUnitInfoPanel = true
        touchDisabledFleetPanel = true
        touchDisabledBottomBar = true
        touchDisabledInventoryPanel = true
        touchDisabledActionConfirmButtons = true
        touchDisabledPauseMenu = true
        touchDisabledDialogs = true
        touchDisabledDevTools = true
    }

    fun enableAllUITouch() {
        touchDisabledUnitInfoPanel = false
        touchDisabledFleetPanel = false
        touchDisabledBottomBar = false
        touchDisabledInventoryPanel = false
        touchDisabledActionConfirmButtons = false
        touchDisabledPauseMenu = false
        touchDisabledDialogs = false
        touchDisabledDevTools = false
    }
}