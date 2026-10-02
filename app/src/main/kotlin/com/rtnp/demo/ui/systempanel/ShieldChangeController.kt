package com.rtnp.demo.ui.systempanel

import android.graphics.Color
import com.rtnp.demo.TranslationTable
import com.rtnp.demo.UnitSystem
import com.rtnp.demo.shield.ShieldManager
import com.rtnp.demo.ui.systempanel.tools.SystemPanelChangeTool

object ShieldChangeController {

    fun show(
        unitSystem: UnitSystem?,
        slotIndex: Int,
        startX: Float,
        startY: Float,
        onFinished: () -> Unit
    ) {
        val unit = unitSystem?.selectedUnit
        if (unit == null) {
            onFinished()
            return
        }

        val hexItems = mutableListOf<SystemPanelChangeTool.ChangeHexItem>()
        hexItems.add(
            SystemPanelChangeTool.ChangeHexItem(
                backgroundColor = Color.rgb(255, 165, 0),
                label = "返回"
            )
        )

        val shieldIds = ShieldManager.getAllIds().toList()
        for (shieldId in shieldIds) {
            val template = ShieldManager.getTemplate(shieldId) ?: continue
            if (template.iconPath != null) {
                hexItems.add(
                    SystemPanelChangeTool.ChangeHexItem(
                        backgroundColor = Color.rgb(100, 180, 240),
                        label = "",
                        iconPath = template.iconPath
                    )
                )
            } else {
                hexItems.add(
                    SystemPanelChangeTool.ChangeHexItem(
                        backgroundColor = Color.rgb(100, 180, 240),
                        label = template.displayName,
                        iconPath = null
                    )
                )
            }
        }

        SystemPanelChangeTool.show(
            startX = startX,
            startY = startY,
            topText = "",
            hexItems = hexItems,
            listener = object : SystemPanelChangeTool.ChangeToolListener {
                private var selectedShieldIndex = -1

                override fun onHexSelected(index: Int, centerX: Float, centerY: Float) {
                    if (index == 0) {
                        SystemPanelChangeTool.hide()
                        onFinished()
                    } else {
                        selectedShieldIndex = index - 1
                        val shieldId = shieldIds.getOrNull(selectedShieldIndex)
                        val template = shieldId?.let { ShieldManager.getTemplate(it) }
                        if (template != null) {
                            val maxHp = (template.defaultParams["maxHp"] as? Int) ?: 0
                            val hpLabel = TranslationTable.get("shield_hp")
                            val durationLabel = TranslationTable.get("shield_duration")
                            val cooldownLabel = TranslationTable.get("shield_cooldown")

                            val sb = StringBuilder()
                            sb.appendLine(template.displayName)
                            if (template.description.isNotEmpty()) {
                                sb.appendLine(template.description)
                            }
                            sb.appendLine("$hpLabel: $maxHp")
                            sb.appendLine("$durationLabel: ${template.activationDuration.toInt()}秒")
                            sb.append("$cooldownLabel: ${template.cooldownDuration.toInt()}秒")

                            SystemPanelChangeTool.setTopText(sb.toString())
                        }
                        SystemPanelChangeTool.showBottomConfirm()
                    }
                }

                override fun onBottomButtonClicked(buttonType: SystemPanelChangeTool.BottomButtonType) {
                    when (buttonType) {
                        SystemPanelChangeTool.BottomButtonType.CANCEL -> {
                            SystemPanelChangeTool.hideBottomConfirm()
                            SystemPanelChangeTool.resetSelection()
                        }
                        SystemPanelChangeTool.BottomButtonType.CONFIRM -> {
                            if (selectedShieldIndex >= 0 && selectedShieldIndex < shieldIds.size) {
                                val shieldId = shieldIds[selectedShieldIndex]
                                ShieldManager.replaceSlot(unit, slotIndex, shieldId)
                            }
                            SystemPanelChangeTool.hide()
                            onFinished()
                        }
                    }
                }
            }
        )
    }
}