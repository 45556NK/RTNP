package com.rtnp.demo.ui.systempanel.tools

import android.graphics.Color
import android.graphics.RectF
import com.rtnp.demo.ui.InfoBoxTool
import com.rtnp.demo.ui.systempanel.SystemPanelConstants

object SystemPanelInfoBox {

    interface OnActionListener {
        fun onCancel()
        fun onConfirm(buttonCenterX: Float, buttonCenterY: Float)
    }

    private var infoBoxId = -1

    fun isShowing(): Boolean = infoBoxId != -1

    fun show(entry: SystemPanelSlotEntry, windowRect: RectF, listener: OnActionListener) {
        hide()

        val gap = windowRect.height() / 20f
        val infoWidth = windowRect.width() - gap * 2f
        val infoHeight = windowRect.height() / 4f

        val infoLeft = windowRect.left + gap
        val infoTop = windowRect.bottom - infoHeight - gap

        infoBoxId = InfoBoxTool.show(infoLeft, infoTop, infoWidth, infoHeight)
        val title = SystemPanelConstants.getText("change_component")
        InfoBoxTool.setText(infoBoxId, "$title\n${entry.label}")

        val buttonDiameter = infoHeight / 3f
        val buttonGap = infoHeight / 20f
        val totalButtonsWidth = buttonDiameter * 2 + buttonGap
        val buttonsStartX = infoLeft + (infoWidth - totalButtonsWidth) / 2f
        val buttonsBottomY = infoTop + infoHeight - gap
        val buttonTopY = buttonsBottomY - buttonDiameter

        InfoBoxTool.addButton(
            taskId = infoBoxId,
            x = buttonsStartX - infoLeft,
            y = buttonTopY - infoTop,
            size = buttonDiameter,
            bgColor = Color.rgb(255, 182, 193),
            label = "取消",
            texturePath = "images/ui/panel_cancel_btn.png",
            iconScale = 0.8f
        )

        val confirmX = buttonsStartX + buttonDiameter + buttonGap
        InfoBoxTool.addButton(
            taskId = infoBoxId,
            x = confirmX - infoLeft,
            y = buttonTopY - infoTop,
            size = buttonDiameter,
            bgColor = Color.rgb(173, 216, 230),
            label = "更改",
            texturePath = "images/ui/panel_change_btn.png",
            iconScale = 0.8f
        )

        val changeButtonCenterX = infoLeft + (confirmX - infoLeft) + buttonDiameter / 2f
        val changeButtonCenterY = infoTop + (buttonTopY - infoTop) + buttonDiameter / 2f

        InfoBoxTool.onButtonClick = { taskId, buttonIndex ->
            if (taskId == infoBoxId) {
                when (buttonIndex) {
                    0 -> listener.onCancel()
                    1 -> listener.onConfirm(changeButtonCenterX, changeButtonCenterY)
                }
            }
        }
    }

    fun hide() {
        if (infoBoxId != -1) {
            InfoBoxTool.hide(infoBoxId)
            infoBoxId = -1
            InfoBoxTool.onButtonClick = null
        }
    }
}