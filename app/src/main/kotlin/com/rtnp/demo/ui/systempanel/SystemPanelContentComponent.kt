package com.rtnp.demo.ui.systempanel

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.ui.FloatingWindowTool
import com.rtnp.demo.ui.HexButton
import com.rtnp.demo.ui.HexGridUtil
import com.rtnp.demo.ui.WarningMessage
import com.rtnp.demo.ui.systempanel.tools.SystemPanelEntriesBuilder
import com.rtnp.demo.ui.systempanel.tools.SystemPanelHexRenderer
import com.rtnp.demo.ui.systempanel.tools.SystemPanelInfoBox
import com.rtnp.demo.ui.systempanel.tools.SystemPanelSlotEntry
import kotlin.math.max
import kotlin.math.sqrt

class SystemPanelContentComponent : FloatingWindowTool.FloatingWindowComponent {

    var unit: PlacedObject? = null
    var screenWidth: Int = 0
    var screenHeight: Int = 0

    private val hexButtons = mutableListOf<HexButton>()
    private val entries = mutableListOf<SystemPanelSlotEntry>()
    private var backButton: HexButton? = null
    private var pressedButton: HexButton? = null

    private val lastWindowRect = RectF()
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
    }

    private var baseScale = 1f
    private var multiTouchStartDist = 0f
    private var multiTouchStartScale = 1f
    private var isMultiTouchActive = false

    private var selectedSlotIndex = -1
    private var isEmptyUnit = false

    override fun draw(canvas: Canvas, windowRect: RectF) {
        buildButtonsIfNeeded(windowRect)

        val titleText = SystemPanelConstants.getText("system_panel_title")
        val titleSize = windowRect.width() / 6f
        titlePaint.textSize = titleSize
        val titleY = windowRect.top + titleSize * 1.5f
        canvas.drawText(titleText, windowRect.centerX(), titleY, titlePaint)

        for (i in hexButtons.indices) {
            SystemPanelHexRenderer.drawHexButton(canvas, hexButtons[i], entries.getOrNull(i)?.bitmap)
        }
        backButton?.draw(canvas)
    }

    override fun onTouchDown(x: Float, y: Float) {
        if (isEmptyUnit) {
            closeInfoBox()
            SystemPanelEntry.hide()
            return
        }

        val screenX = x + lastWindowRect.left
        val screenY = y + lastWindowRect.top
        pressedButton = null

        backButton?.let {
            if (it.isPointInside(screenX, screenY)) {
                it.onTouchDown()
                pressedButton = it
                return
            }
        }

        for (btn in hexButtons) {
            if (btn.isPointInside(screenX, screenY)) {
                btn.onTouchDown()
                pressedButton = btn
                return
            }
        }
    }

    override fun onTouchMove(x: Float, y: Float) {
        if (isEmptyUnit) return
        val screenX = x + lastWindowRect.left
        val screenY = y + lastWindowRect.top
        pressedButton?.let {
            if (!it.isPointInside(screenX, screenY)) {
                it.cancel()
                pressedButton = null
            }
        }
    }

    override fun onTouchUp(x: Float, y: Float) {
        if (isEmptyUnit) return
        val screenX = x + lastWindowRect.left
        val screenY = y + lastWindowRect.top
        pressedButton?.let { btn ->
            if (btn.isPointInside(screenX, screenY)) {
                btn.onTouchUp()
                when {
                    btn === backButton -> {
                        closeInfoBox()
                        SystemPanelEntry.hide()
                    }
                    else -> {
                        val index = hexButtons.indexOf(btn)
                        if (index >= 0 && index < entries.size) {
                            selectSlot(index)
                        }
                    }
                }
            } else {
                btn.cancel()
            }
            pressedButton = null
        }
    }

    override fun onMultiTouchStart(firstX: Float, firstY: Float, secondX: Float, secondY: Float) {
        if (isEmptyUnit) return
        multiTouchStartDist = distance(firstX, firstY, secondX, secondY)
        multiTouchStartScale = baseScale
        isMultiTouchActive = true
    }

    override fun onMultiTouchMove(firstX: Float, firstY: Float, secondX: Float, secondY: Float) {
        if (isEmptyUnit) return
        if (!isMultiTouchActive) return
        val currentDist = distance(firstX, firstY, secondX, secondY)
        if (multiTouchStartDist <= 0f) return
        val scaleFactor = currentDist / multiTouchStartDist
        val newScale = (multiTouchStartScale * scaleFactor).coerceIn(0.5f, 1f)
        baseScale = newScale
        lastWindowRect.setEmpty()
    }

    override fun onMultiTouchEnd() {
        if (isEmptyUnit) return
        isMultiTouchActive = false
    }

    fun cleanup() {
        closeInfoBox()
    }

    private fun selectSlot(index: Int) {
        selectedSlotIndex = index
        lastWindowRect.setEmpty()
        updateInfoBox()
    }

    private fun resetToInitial() {
        baseScale = 1f
        selectedSlotIndex = -1
        lastWindowRect.setEmpty()
        closeInfoBox()
    }

    private fun updateInfoBox() {
        val entry = entries.getOrNull(selectedSlotIndex) ?: return
        val windowRect = FloatingWindowTool.getWindowRect()

        SystemPanelInfoBox.show(entry, windowRect, object : SystemPanelInfoBox.OnActionListener {
            override fun onCancel() {
                resetToInitial()
            }

            override fun onConfirm(buttonCenterX: Float, buttonCenterY: Float) {
                handleConfirm(entry, buttonCenterX, buttonCenterY)
            }
        })
    }

    private fun handleConfirm(entry: SystemPanelSlotEntry, centerX: Float, centerY: Float) {
        val us = SystemPanelLogic.getUnitSystem() ?: return
        val u = unit ?: return

        val allowed = when (entry.type) {
            "weapon" -> u.allowWeaponChange
            "strategy" -> u.allowStrategyChange
            "shield" -> u.allowShieldChange
            else -> true
        }

        if (!allowed) {
            WarningMessage.show("目标槽位已被固定！")
            return
        }

        SystemPanelInfoBox.hide()
        SystemPanelEntry.hide()

        when (entry.type) {
            "weapon" -> WeaponChangeController.show(
                unitSystem = us,
                slotIndex = entry.index,
                startX = centerX,
                startY = centerY,
                onFinished = { SystemPanelEntry.show(centerX, centerY, us) }
            )
            "strategy" -> StrategyChangeController.show(
                unitSystem = us,
                slotIndex = entry.index,
                startX = centerX,
                startY = centerY,
                onFinished = { SystemPanelEntry.show(centerX, centerY, us) }
            )
            "shield" -> ShieldChangeController.show(
                unitSystem = us,
                slotIndex = entry.index,
                startX = centerX,
                startY = centerY,
                onFinished = { SystemPanelEntry.show(centerX, centerY, us) }
            )
        }
    }

    private fun closeInfoBox() {
        SystemPanelInfoBox.hide()
    }

    private fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x1 - x2
        val dy = y1 - y2
        return sqrt(dx * dx + dy * dy)
    }

    private fun buildButtonsIfNeeded(windowRect: RectF) {
        val sameRect = hexButtons.isNotEmpty() && backButton != null &&
            lastWindowRect.left == windowRect.left &&
            lastWindowRect.top == windowRect.top &&
            lastWindowRect.right == windowRect.right &&
            lastWindowRect.bottom == windowRect.bottom
        if (sameRect) return

        lastWindowRect.set(windowRect)

        hexButtons.clear()
        entries.clear()
        backButton = null
        isEmptyUnit = false

        val u = unit ?: return
        val context = FloatingWindowTool.getContext() ?: return

        entries.addAll(SystemPanelEntriesBuilder.build(u, context))

        val hexSize = maxOf(SystemPanelConstants.getHexSize(screenWidth), windowRect.width() / 12f) * baseScale
        val fontSize = maxOf(SystemPanelConstants.getSmallFontSize(screenHeight), 24f) * baseScale
        val borderWidth = 8f * baseScale

        if (entries.isEmpty()) {
            isEmptyUnit = true
            backButton = buildBackButton(windowRect.centerX(), windowRect.centerY(), hexSize, fontSize)
            return
        }

        val total = entries.size + 1
        val coords = HexGridUtil.generateHexCoordsWithBorder(
            total, hexSize, borderWidth,
            windowRect.centerX(), windowRect.centerY()
        )

        if (selectedSlotIndex >= 0 && selectedSlotIndex < entries.size) {
            val selectedCoord = coords[selectedSlotIndex + 1]
            val dx = windowRect.centerX() - selectedCoord[0]
            val dy = windowRect.centerY() - selectedCoord[1]
            for (coord in coords) {
                coord[0] += dx
                coord[1] += dy
            }
        }

        backButton = buildBackButton(coords[0][0], coords[0][1], hexSize, fontSize)

        for (i in entries.indices) {
            val entry = entries[i]
            val coord = coords[i + 1]
            val btn = HexButton(
                coord[0], coord[1], hexSize,
                if (entry.bitmap == null) entry.label else ""
            )
            btn.fillColor = SystemPanelHexRenderer.getFillColor(entry.type)
            btn.strokeColor = Color.WHITE
            btn.pressedStrokeColor = Color.YELLOW
            btn.textColor = Color.BLACK
            btn.fontSize = fontSize
            btn.buildPaths()
            hexButtons.add(btn)
        }
    }

    private fun buildBackButton(cx: Float, cy: Float, hexSize: Float, fontSize: Float): HexButton {
        return HexButton(cx, cy, hexSize, SystemPanelConstants.getText("btn_back")).apply {
            fillColor = Color.rgb(255, 165, 0)
            strokeColor = Color.WHITE
            pressedStrokeColor = Color.YELLOW
            textColor = Color.BLACK
            this.fontSize = fontSize
            buildPaths()
        }
    }
}